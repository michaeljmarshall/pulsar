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
package org.apache.pulsar.functions.worker.service.api;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import org.apache.pulsar.broker.authentication.AuthenticationDataHttps;
import org.apache.pulsar.broker.authentication.AuthenticationDataSource;
import org.apache.pulsar.broker.authentication.HttpAuthDataWrapper;
import org.apache.pulsar.common.functions.UpdateOptionsImpl;
import org.apache.pulsar.common.io.ConfigFieldDefinition;
import org.apache.pulsar.common.io.ConnectorDefinition;
import org.apache.pulsar.common.io.SinkConfig;
import org.apache.pulsar.common.policies.data.SinkStatus;
import org.apache.pulsar.common.policies.data.SinkStatus.SinkInstanceStatus.SinkInstanceStatusData;
import org.apache.pulsar.functions.worker.WorkerService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

/**
 * The service to manage sinks.
 */
public interface Sinks<W extends WorkerService> extends Component<W> {

    void registerSink(String tenant,
                      String namespace,
                      String sinkName,
                      InputStream uploadedInputStream,
                      FormDataContentDisposition fileDetail,
                      String sinkPkgUrl,
                      SinkConfig sinkConfig,
                      HttpAuthDataWrapper authDataWrapper);

    /**
     * Update a function.
     * @param tenant The tenant of a Pulsar Sink
     * @param namespace The namespace of a Pulsar Sink
     * @param sinkName The name of a Pulsar Sink
     * @param uploadedInputStream Input stream of bytes
     * @param fileDetail A form-data content disposition header
     * @param sinkPkgUrl URL path of the Pulsar Sink package
     * @param sinkConfig Configuration of Pulsar Sink
     * @param clientRole Client role for running the Pulsar Sink
     * @param clientAuthenticationDataHttps Authentication status of the http client
     */
    @Deprecated
    default void registerSink(String tenant,
                      String namespace,
                      String sinkName,
                      InputStream uploadedInputStream,
                      FormDataContentDisposition fileDetail,
                      String sinkPkgUrl,
                      SinkConfig sinkConfig,
                      String clientRole,
                      AuthenticationDataSource clientAuthenticationDataHttps) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder()
                .clientRole(clientRole)
                .clientAuthenticationDataSource(clientAuthenticationDataHttps)
                .build();
        registerSink(tenant, namespace, sinkName, uploadedInputStream, fileDetail, sinkPkgUrl, sinkConfig,
                authDataWrapper);
    }

    /**
     * This method uses an incorrect signature 'AuthenticationDataHttps' that prevents the extension of auth status,
     * so it is marked as deprecated and kept here only for backward compatibility. Please use the method that accepts
     * the signature of the AuthenticationDataSource.
     */
    @Deprecated
    default void registerSink(String tenant,
                      String namespace,
                      String sinkName,
                      InputStream uploadedInputStream,
                      FormDataContentDisposition fileDetail,
                      String sinkPkgUrl,
                      SinkConfig sinkConfig,
                      String clientRole,
                      AuthenticationDataHttps clientAuthenticationDataHttps) {
        registerSink(
                tenant,
                namespace,
                sinkName,
                uploadedInputStream,
                fileDetail,
                sinkPkgUrl,
                sinkConfig,
                clientRole,
                (AuthenticationDataSource) clientAuthenticationDataHttps);
    }

    /**
     * Update a function.
     * @param tenant The tenant of a Pulsar Sink
     * @param namespace The namespace of a Pulsar Sink
     * @param sinkName The name of a Pulsar Sink
     * @param uploadedInputStream Input stream of bytes
     * @param fileDetail A form-data content disposition header
     * @param sinkPkgUrl URL path of the Pulsar Sink package
     * @param sinkConfig Configuration of Pulsar Sink
     * @param updateOptions Options while updating the sink
     * @param authDataWrapper auth data for http request
     */
    void updateSink(String tenant,
                    String namespace,
                    String sinkName,
                    InputStream uploadedInputStream,
                    FormDataContentDisposition fileDetail,
                    String sinkPkgUrl,
                    SinkConfig sinkConfig,
                    UpdateOptionsImpl updateOptions,
                    HttpAuthDataWrapper authDataWrapper);

    /**
     * Update a function.
     * @param tenant The tenant of a Pulsar Sink
     * @param namespace The namespace of a Pulsar Sink
     * @param sinkName The name of a Pulsar Sink
     * @param uploadedInputStream Input stream of bytes
     * @param fileDetail A form-data content disposition header
     * @param sinkPkgUrl URL path of the Pulsar Sink package
     * @param sinkConfig Configuration of Pulsar Sink
     * @param clientRole Client role for running the Pulsar Sink
     * @param clientAuthenticationDataHttps Authentication status of the http client
     * @param updateOptions Options while updating the sink
     */
    @Deprecated
    default void updateSink(String tenant,
                    String namespace,
                    String sinkName,
                    InputStream uploadedInputStream,
                    FormDataContentDisposition fileDetail,
                    String sinkPkgUrl,
                    SinkConfig sinkConfig,
                    String clientRole,
                    AuthenticationDataSource clientAuthenticationDataHttps,
                    UpdateOptionsImpl updateOptions) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder()
                .clientRole(clientRole)
                .clientAuthenticationDataSource(clientAuthenticationDataHttps)
                .build();
        updateSink(tenant, namespace, sinkName, uploadedInputStream, fileDetail, sinkPkgUrl, sinkConfig, updateOptions,
                authDataWrapper);
    }

    /**
     * This method uses an incorrect signature 'AuthenticationDataHttps' that prevents the extension of auth status,
     * so it is marked as deprecated and kept here only for backward compatibility. Please use the method that accepts
     * the signature of the AuthenticationDataSource.
     */
    @Deprecated
    default void updateSink(String tenant,
                    String namespace,
                    String sinkName,
                    InputStream uploadedInputStream,
                    FormDataContentDisposition fileDetail,
                    String sinkPkgUrl,
                    SinkConfig sinkConfig,
                    String clientRole,
                    AuthenticationDataHttps clientAuthenticationDataHttps,
                    UpdateOptionsImpl updateOptions) {
        updateSink(
                tenant,
                namespace,
                sinkName,
                uploadedInputStream,
                fileDetail,
                sinkPkgUrl,
                sinkConfig,
                clientRole,
                (AuthenticationDataSource) clientAuthenticationDataHttps,
                updateOptions);
    }

    SinkInstanceStatusData getSinkInstanceStatus(String tenant,
                                                 String namespace,
                                                 String sinkName,
                                                 String instanceId,
                                                 URI uri,
                                                 HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default SinkInstanceStatusData getSinkInstanceStatus(String tenant,
                                                         String namespace,
                                                         String sinkName,
                                                         String instanceId,
                                                         URI uri,
                                                         String clientRole,
                                                         AuthenticationDataSource clientAuthenticationDataHttps) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder()
                .clientRole(clientRole)
                .clientAuthenticationDataSource(clientAuthenticationDataHttps)
                .build();
        return getSinkInstanceStatus(tenant, namespace, sinkName, instanceId, uri, authDataWrapper);
    }

    SinkStatus getSinkStatus(String tenant,
                             String namespace,
                             String componentName,
                             URI uri,
                             HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default SinkStatus getSinkStatus(String tenant,
                             String namespace,
                             String componentName,
                             URI uri,
                             String clientRole,
                             AuthenticationDataSource clientAuthenticationDataHttps) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder()
                .clientRole(clientRole)
                .clientAuthenticationDataSource(clientAuthenticationDataHttps)
                .build();
        return getSinkStatus(tenant, namespace, componentName, uri, authDataWrapper);
    }

    SinkConfig getSinkInfo(String tenant,
                           String namespace,
                           String componentName,
                           HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default SinkConfig getSinkInfo(String tenant,
                                   String namespace,
                                   String componentName) {
        return getSinkInfo(tenant, namespace, componentName, HttpAuthDataWrapper.builder().build());
    }

    List<ConnectorDefinition> getSinkList();


    List<ConfigFieldDefinition> getSinkConfigDefinition(String name);

}
