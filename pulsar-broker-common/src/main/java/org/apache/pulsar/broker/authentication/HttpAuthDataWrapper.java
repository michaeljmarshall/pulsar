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

import lombok.Builder;

/**
 * A class to collect all the common fields used for authentication. Because the authentication data source is
 * not always consistent when using the Pulsar Protocol and the Pulsar Proxy
 * (see <a href="https://github.com/apache/pulsar/issues/19332">19332</a>), this class is currently restricted
 * to use only in HTTP authentication.
 */
@Builder
@lombok.Data
public class HttpAuthDataWrapper {

    /**
     * The original principal (or role) of the client. This is only supplied when the client is using the proxy.
     */
    private final String originalPrincipal;

    /**
     * The client role. When the client is using the proxy, this is the role of the proxy or the role of the client,
     * depending on the type of authentication the proxy uses with the broker.
     */
    private final String clientRole;

    /**
     * The authentication data source used to generate the {@link #clientRole}.
     */
    private final AuthenticationDataSource clientAuthenticationDataSource;
}
