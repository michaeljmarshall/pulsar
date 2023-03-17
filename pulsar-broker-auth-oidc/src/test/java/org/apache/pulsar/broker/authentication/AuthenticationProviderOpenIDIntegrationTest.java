/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.broker.authentication;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.naming.AuthenticationException;
import org.apache.pulsar.broker.ServiceConfiguration;
import org.apache.pulsar.common.api.AuthData;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * An integration test relying on WireMock to simulate an OpenID Connect provider.
 */
public class AuthenticationProviderOpenIDIntegrationTest {

    AuthenticationProviderOpenID provider;
    PrivateKey privateKey;

    // These are the kid values for JWKs in the /keys endpoint
    String validJwk = "valid";
    String invalidJwk = "invalid";

    // The valid issuer
    String issuer;
    WireMockServer server;

    @BeforeTest
    void beforeClass() throws IOException {

        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        issuer = server.baseUrl();

        // Set up a correct openid-configuration
        server.stubFor(
                get(urlEqualTo("/.well-known/openid-configuration"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {
                                          "issuer": "%s",
                                          "jwks_uri": "%s/keys"
                                        }
                                        """.replace("%s", server.baseUrl()))));

        // Set up an incorrect openid-configuration where issuer does not match
        server.stubFor(
                get(urlEqualTo("/fail/.well-known/openid-configuration"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                        {
                                          "issuer": "https://wrong-issuer.com",
                                          "jwks_uri": "%s/keys"
                                        }
                                        """.replace("%s", server.baseUrl()))));

        // Create the token key pair
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        privateKey = keyPair.getPrivate();
        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        String n = Base64.getUrlEncoder().encodeToString(rsaPublicKey.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().encodeToString(rsaPublicKey.getPublicExponent().toByteArray());

        // Set up JWKS endpoint with a valid and an invalid public key
        server.stubFor(
                get(urlEqualTo( "/keys"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(
                                        """
                                        {
                                            "keys" : [
                                                {
                                                "kid":"%s",
                                                "kty":"RSA",
                                                "alg":"RS256",
                                                "n":"%s",
                                                "e":"%s"
                                                },
                                                {
                                                "kid": "%s",
                                                "kty":"RSA",
                                                "n":"invalid-key",
                                                "e":"AQAB"
                                                }
                                            ]
                                        }
                                        """.formatted(validJwk, n, e, invalidJwk))));

        ServiceConfiguration conf = new ServiceConfiguration();
        conf.setAuthenticationEnabled(true);
        conf.setAuthenticationProviders(Set.of(AuthenticationProviderOpenID.class.getName()));
        Properties props = conf.getProperties();
        props.setProperty(AuthenticationProviderOpenID.REQUIRE_HTTPS, "false");
        props.setProperty(AuthenticationProviderOpenID.ALLOWED_AUDIENCE, "allowed-audience");
        props.setProperty(AuthenticationProviderOpenID.ALLOWED_TOKEN_ISSUERS, issuer);
        provider = new AuthenticationProviderOpenID();
        provider.initialize(conf);
    }

    @AfterClass
    void afterClass() {
        server.stop();
    }

    @Test
    public void testTokenWithValidJWK() throws Exception {
        String role = "superuser";
        String token = generateToken(validJwk, issuer, role, "allowed-audience",
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() + 10000));
        assertEquals(role, provider.authenticateAsync(new AuthenticationDataCommand(token)).get());
    }

    @Test
    public void testTokenWithInvalidJWK() throws Exception {
        String role = "superuser";
        String token = generateToken(invalidJwk, issuer, role, "allowed-audience",
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() + 10000));
        try {
            provider.authenticateAsync(new AuthenticationDataCommand(token)).get();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof AuthenticationException, "Found exception: " + e.getCause());
        }
    }

    @Test
    public void testAuthorizationServerReturnsIncorrectIssuerInOpenidConnectConfiguration() throws Exception {
        // This issuer is configured to return an issuer in the openid-configuration
        // that does not match the issuer on the token
        String failIssuer = server.baseUrl() + "/fail";
        String role = "superuser";
        String token = generateToken(validJwk, failIssuer, role, "allowed-audience",
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() + 10000));
        try {
            provider.authenticateAsync(new AuthenticationDataCommand(token)).get();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof AuthenticationException, "Found exception: " + e.getCause());
        }
    }

    @Test
    public void testAuthenticationStateOpenIDForValidToken() throws Exception {
        String role = "superuser";
        String token = generateToken(validJwk, issuer, role, "allowed-audience",
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() + 10000));
        AuthenticationState state = provider.newAuthState(null, null, null);
        AuthData result = state.authenticateAsync(AuthData.of(token.getBytes())).get();
        assertNull(result);
        assertEquals(state.getAuthRole(), role);
        assertEquals(state.getAuthDataSource().getCommandData(), token);
        assertFalse(state.isExpired());
    }

    @Test
    public void testAuthenticationStateOpenIDForExpiredToken() throws Exception {
        String role = "superuser";
        String token = generateToken(validJwk, issuer, role, "allowed-audience",
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() - 10000));
        AuthenticationState state = provider.newAuthState(null, null, null);
        try {
            state.authenticateAsync(AuthData.of(token.getBytes())).get();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof AuthenticationException, "Found exception: " + e.getCause());
        }
    }

    @Test
    public void testAuthenticationStateOpenIDForValidTokenWithNoExp() throws Exception {
        String role = "superuser";
        String token = generateToken(validJwk, issuer, role, "allowed-audience",
                new Date(System.currentTimeMillis()), null);
        AuthenticationState state = provider.newAuthState(null, null, null);
        AuthData result = state.authenticateAsync(AuthData.of(token.getBytes())).get();
        assertNull(result);
        assertEquals(state.getAuthRole(), role);
        assertEquals(state.getAuthDataSource().getCommandData(), token);
        assertFalse(state.isExpired());
    }

    @Test
    public void testAuthenticationStateOpenIDForTokenExpiration() throws Exception {
        ServiceConfiguration conf = new ServiceConfiguration();
        conf.setAuthenticationEnabled(true);
        conf.setAuthenticationProviders(Set.of(AuthenticationProviderOpenID.class.getName()));
        Properties props = conf.getProperties();
        props.setProperty(AuthenticationProviderOpenID.REQUIRE_HTTPS, "false");
        props.setProperty(AuthenticationProviderOpenID.ALLOWED_AUDIENCE, "allowed-audience");
        props.setProperty(AuthenticationProviderOpenID.ALLOWED_TOKEN_ISSUERS, issuer);
        // Use the leeway to allow the token to pass validation and then fail expiration
        props.setProperty(AuthenticationProviderOpenID.ACCEPTED_TIME_LEEWAY_SECONDS, "10");
        provider = new AuthenticationProviderOpenID();
        provider.initialize(conf);

        String role = "superuser";
        String token = generateToken(validJwk, issuer, role, "allowed-audience",
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()));
        AuthenticationState state = provider.newAuthState(null, null, null);
        AuthData result = state.authenticateAsync(AuthData.of(token.getBytes())).get();
        assertNull(result);
        assertEquals(state.getAuthRole(), role);
        assertEquals(state.getAuthDataSource().getCommandData(), token);
        assertTrue(state.isExpired());
    }

    // This test is somewhat counterintuitive. We allow the state object to change roles, but then we fail it
    // in the ServerCnx handling of the state object. As such, it is essential that the state object allow
    // the role to change.
    @Test
    public void testAuthenticationStateOpenIDAllowsRoleChange() throws Exception {
        String role1 = "superuser";
        String token1 = generateToken(validJwk, issuer, role1, "allowed-audience",
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() + 10000));
        String role2 = "otheruser";
        String token2 = generateToken(validJwk, issuer, role2, "allowed-audience",
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() + 10000));
        AuthenticationState state = provider.newAuthState(null, null, null);
        AuthData result1 = state.authenticateAsync(AuthData.of(token1.getBytes())).get();
        assertNull(result1);
        assertEquals(state.getAuthRole(), role1);
        assertEquals(state.getAuthDataSource().getCommandData(), token1);
        assertFalse(state.isExpired());

        AuthData result2 = state.authenticateAsync(AuthData.of(token2.getBytes())).get();
        assertNull(result2);
        assertEquals(state.getAuthRole(), role2);
        assertEquals(state.getAuthDataSource().getCommandData(), token2);
        assertFalse(state.isExpired());
    }

    private String generateToken(String kid, String issuer, String subject, String audience, Date iat, Date exp) {
        DefaultJwtBuilder defaultJwtBuilder = new DefaultJwtBuilder();
        defaultJwtBuilder.setHeaderParam("kid", kid);
        defaultJwtBuilder.setHeaderParam("typ", "JWT");
        defaultJwtBuilder.setHeaderParam("alg", "RS256");
        defaultJwtBuilder.setIssuer(issuer);
        defaultJwtBuilder.setSubject(subject);
        defaultJwtBuilder.setAudience(audience);
        defaultJwtBuilder.setIssuedAt(iat);
        defaultJwtBuilder.setExpiration(exp);
        defaultJwtBuilder.signWith(privateKey);
        return defaultJwtBuilder.compact();
    }

}
