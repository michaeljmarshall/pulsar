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
package org.apache.pulsar.functions.auth;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1KeyToPath;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1ProjectedVolumeSource;
import io.kubernetes.client.openapi.models.V1SecretVolumeSource;
import io.kubernetes.client.openapi.models.V1ServiceAccountTokenProjection;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import io.kubernetes.client.openapi.models.V1VolumeProjection;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.pulsar.broker.authentication.AuthenticationDataSource;
import org.apache.pulsar.client.impl.auth.AuthenticationToken;
import org.apache.pulsar.functions.instance.AuthenticationConfig;
import org.apache.pulsar.functions.proto.Function;
import org.apache.pulsar.functions.runtime.kubernetes.KubernetesRuntimeFactoryConfig;
import org.eclipse.jetty.util.StringUtil;

/**
 * Kubernetes Function Authentication Provider that uses adds a service account to a function pod's container
 * definition. This is used to authenticate the function instance via OpenId Connect.
 *
 * Note: the pod inherits the namespaces default service account.
 */
public class KubernetesServiceAccountAuthProvider implements KubernetesFunctionAuthProvider {

    private static final String SERVICE_ACCOUNT_VOLUME_NAME = "service-account-token";
    private static final String TRUST_CERT_VOLUME_NAME = "ca-cert";
    private static final String DEFAULT_MOUNT_DIR = "/etc/auth";
    private static final String FUNCTION_AUTH_TOKEN = "token";
    private static final String FUNCTION_CA_CERT = "ca.crt";
    private String brokerTrustCertsSecretName;
    private long serviceAccountTokenExpirationSeconds;

    @Override
    public void initialize(CoreV1Api coreClient, byte[] caBytes,
                           java.util.function.Function<Function.FunctionDetails, String> namespaceCustomizerFunc,
                           KubernetesRuntimeFactoryConfig config) {
        setNamespaceProviderFunc(namespaceCustomizerFunc);
        brokerTrustCertsSecretName = config.getBrokerClientTrustCertsSecretName();
        serviceAccountTokenExpirationSeconds = config.getServiceAccountTokenExpirationSeconds();
    }

    @Override
    public void configureAuthenticationConfig(AuthenticationConfig authConfig,
                                              Optional<FunctionAuthData> functionAuthData) {
        authConfig.setClientAuthenticationPlugin(AuthenticationToken.class.getName());
        authConfig.setClientAuthenticationParameters(Paths.get(DEFAULT_MOUNT_DIR, FUNCTION_AUTH_TOKEN)
                .toUri().toString());
        // if we have ca bytes, update the new path for the CA
        if (StringUtil.isNotBlank(brokerTrustCertsSecretName)) {
            authConfig.setTlsTrustCertsFilePath(String.format("%s/%s", DEFAULT_MOUNT_DIR, FUNCTION_CA_CERT));
        }
    }

    /**
     * No need to cache anything because we rely on Kubernetes to generate the token used for authentication.
     */
    @Override
    public Optional<FunctionAuthData> cacheAuthData(Function.FunctionDetails funcDetails,
                                                    AuthenticationDataSource authenticationDataSource)
            throws Exception {
        return Optional.empty();
    }

    /**
     * No need to update anything because we rely on Kubernetes to generate the token used for authentication.
     */
    @Override
    public Optional<FunctionAuthData> updateAuthData(Function.FunctionDetails funcDetails,
                                                     Optional<FunctionAuthData> existingFunctionAuthData,
                                                     AuthenticationDataSource authenticationDataSource)
            throws Exception {
        return Optional.empty();
    }

    /**
     * No need to clean up anything because we rely on Kubernetes to clean up the secret when the pod is deleted.
     */
    @Override
    public void cleanUpAuthData(Function.FunctionDetails funcDetails, Optional<FunctionAuthData> functionAuthData)
            throws Exception {

    }

    @Override
    public void initialize(CoreV1Api coreClient) {
    }

    @Override
    public void configureAuthDataStatefulSet(V1StatefulSet statefulSet, Optional<FunctionAuthData> functionAuthData) {
        V1PodSpec podSpec = statefulSet.getSpec().getTemplate().getSpec();
        // configure pod mount secret with auth token
        if (StringUtil.isNotBlank(brokerTrustCertsSecretName)) {
            podSpec.addVolumesItem(createTrustCertVolume());
        }
        podSpec.addVolumesItem(createServiceAccountVolume());
        podSpec.getContainers().forEach(this::addVolumeMountsToContainer);
    }

    private V1Volume createServiceAccountVolume() {
        V1ProjectedVolumeSource projectedVolumeSource = new V1ProjectedVolumeSource();
        V1VolumeProjection volumeProjection = new V1VolumeProjection();
        volumeProjection.serviceAccountToken(
                new V1ServiceAccountTokenProjection()
                        .expirationSeconds(serviceAccountTokenExpirationSeconds)
                        .path(FUNCTION_AUTH_TOKEN));
        projectedVolumeSource.addSourcesItem(volumeProjection);
        return new V1Volume()
                .name(SERVICE_ACCOUNT_VOLUME_NAME)
                .projected(projectedVolumeSource);
    }

    private V1Volume createTrustCertVolume() {
        return new V1Volume()
                .name(TRUST_CERT_VOLUME_NAME)
                .secret(new V1SecretVolumeSource()
                        .secretName(brokerTrustCertsSecretName)
                        .addItemsItem(new V1KeyToPath()
                                .key(FUNCTION_CA_CERT)
                                .path(FUNCTION_CA_CERT)));
    }

    private void addVolumeMountsToContainer(V1Container container) {
        container.addVolumeMountsItem(
                new V1VolumeMount()
                        .name(SERVICE_ACCOUNT_VOLUME_NAME)
                        .mountPath(DEFAULT_MOUNT_DIR)
                        .readOnly(true));
        if (StringUtil.isNotBlank(brokerTrustCertsSecretName)) {
            container.addVolumeMountsItem(
                    new V1VolumeMount()
                            .name(TRUST_CERT_VOLUME_NAME)
                            .mountPath(DEFAULT_MOUNT_DIR)
                            .readOnly(true));
        }
    }
}
