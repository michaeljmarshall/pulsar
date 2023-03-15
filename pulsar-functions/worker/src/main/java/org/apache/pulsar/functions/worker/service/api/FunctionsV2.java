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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.pulsar.broker.authentication.HttpAuthDataWrapper;
import org.apache.pulsar.common.io.ConnectorDefinition;
import org.apache.pulsar.functions.worker.WorkerService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

/**
 * The v2 functions API to manage functions.
 */
public interface FunctionsV2<W extends WorkerService> {

    Response getFunctionInfo(String tenant,
                             String namespace,
                             String functionName,
                             HttpAuthDataWrapper authDataWrapper) throws IOException;

    @Deprecated
    default Response getFunctionInfo(String tenant,
                             String namespace,
                             String functionName,
                             String clientRole) throws IOException {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return getFunctionInfo(tenant, namespace, functionName, authDataWrapper);
    }

    Response getFunctionInstanceStatus(String tenant,
                                               String namespace,
                                               String functionName,
                                               String instanceId,
                                               URI uri,
                                               HttpAuthDataWrapper authDataWrapper) throws IOException;

    @Deprecated
    default Response getFunctionInstanceStatus(String tenant,
                                       String namespace,
                                       String functionName,
                                       String instanceId,
                                       URI uri,
                                       String clientRole) throws IOException {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return getFunctionInstanceStatus(tenant, namespace, functionName, instanceId, uri, authDataWrapper);
    }

    Response getFunctionStatusV2(String tenant,
                                 String namespace,
                                 String functionName,
                                 URI requestUri,
                                 HttpAuthDataWrapper authDataWrapper) throws IOException;

    @Deprecated
    default Response getFunctionStatusV2(String tenant,
                                 String namespace,
                                 String functionName,
                                 URI requestUri,
                                 String clientRole) throws IOException {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return getFunctionStatusV2(tenant, namespace, functionName, requestUri, authDataWrapper);
    }

    Response registerFunction(String tenant,
                              String namespace,
                              String functionName,
                              InputStream uploadedInputStream,
                              FormDataContentDisposition fileDetail,
                              String functionPkgUrl,
                              String functionDetailsJson,
                              HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response registerFunction(String tenant,
                              String namespace,
                              String functionName,
                              InputStream uploadedInputStream,
                              FormDataContentDisposition fileDetail,
                              String functionPkgUrl,
                              String functionDetailsJson,
                              String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return registerFunction(tenant, namespace, functionName, uploadedInputStream, fileDetail, functionPkgUrl,
                functionDetailsJson, authDataWrapper);
    }


    Response updateFunction(String tenant,
                            String namespace,
                            String functionName,
                            InputStream uploadedInputStream,
                            FormDataContentDisposition fileDetail,
                            String functionPkgUrl,
                            String functionDetailsJson,
                            HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response updateFunction(String tenant,
                            String namespace,
                            String functionName,
                            InputStream uploadedInputStream,
                            FormDataContentDisposition fileDetail,
                            String functionPkgUrl,
                            String functionDetailsJson,
                            String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return updateFunction(tenant, namespace, functionName, uploadedInputStream, fileDetail, functionPkgUrl,
                functionDetailsJson, authDataWrapper);
    }

    Response deregisterFunction(String tenant, String namespace, String functionName,
                                HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response deregisterFunction(String tenant,
                                String namespace,
                                String functionName,
                                String clientAppId) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientAppId).build();
        return deregisterFunction(tenant, namespace, functionName, authDataWrapper);
    }

    Response listFunctions(String tenant, String namespace, HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response listFunctions(String tenant, String namespace, String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return listFunctions(tenant, namespace, authDataWrapper);
    }

    Response triggerFunction(String tenant,
                             String namespace,
                             String functionName,
                             String triggerValue,
                             InputStream triggerStream,
                             String topic,
                             HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response triggerFunction(String tenant,
                             String namespace,
                             String functionName,
                             String triggerValue,
                             InputStream triggerStream,
                             String topic,
                             String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return triggerFunction(tenant, namespace, functionName, triggerValue, triggerStream, topic, authDataWrapper);
    }

    Response getFunctionState(String tenant,
                              String namespace,
                              String functionName,
                              String key,
                              HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response getFunctionState(String tenant,
                              String namespace,
                              String functionName,
                              String key,
                              String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return getFunctionState(tenant, namespace, functionName, key, authDataWrapper);
    }

    Response restartFunctionInstance(String tenant,
                                     String namespace,
                                     String functionName,
                                     String instanceId,
                                     URI uri,
                                     HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response restartFunctionInstance(String tenant,
                                     String namespace,
                                     String functionName,
                                     String instanceId,
                                     URI uri,
                                     String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return restartFunctionInstance(tenant, namespace, functionName, instanceId, uri, authDataWrapper);
    }


    Response restartFunctionInstances(String tenant,
                                      String namespace,
                                      String functionName,
                                      HttpAuthDataWrapper authDataWrapper);
    @Deprecated
    default Response restartFunctionInstances(String tenant,
                                      String namespace,
                                      String functionName,
                                      String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return restartFunctionInstances(tenant, namespace, functionName, authDataWrapper);
    }

    Response stopFunctionInstance(String tenant,
                                  String namespace,
                                  String functionName,
                                  String instanceId,
                                  URI uri,
                                  HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response stopFunctionInstance(String tenant,
                                  String namespace,
                                  String functionName,
                                  String instanceId,
                                  URI uri,
                                  String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return stopFunctionInstance(tenant, namespace, functionName, instanceId, uri, authDataWrapper);
    }

    Response stopFunctionInstances(String tenant,
                                   String namespace,
                                   String functionName,
                                   HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response stopFunctionInstances(String tenant,
                                   String namespace,
                                   String functionName,
                                   String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return stopFunctionInstances(tenant, namespace, functionName, authDataWrapper);
    }

    Response uploadFunction(InputStream uploadedInputStream,
                            String path,
                            HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response uploadFunction(InputStream uploadedInputStream,
                            String path,
                            String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return uploadFunction(uploadedInputStream, path, authDataWrapper);
    }

    Response downloadFunction(String path, HttpAuthDataWrapper authDataWrapper);

    @Deprecated
    default Response downloadFunction(String path, String clientRole) {
        HttpAuthDataWrapper authDataWrapper = HttpAuthDataWrapper.builder().clientRole(clientRole).build();
        return downloadFunction(path, authDataWrapper);
    }

    List<ConnectorDefinition> getListOfConnectors();

}
