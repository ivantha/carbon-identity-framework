/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.configuration.mgt.server.cors.internal.impl;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementClientException;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceAdd;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceTypeAdd;
import org.wso2.carbon.identity.configuration.mgt.server.cors.CORSService;
import org.wso2.carbon.identity.configuration.mgt.server.cors.exception.CORSServiceException;
import org.wso2.carbon.identity.configuration.mgt.server.cors.internal.CORSServiceHolder;
import org.wso2.carbon.identity.configuration.mgt.server.cors.internal.function.CORSUrlToAttribute;
import org.wso2.carbon.identity.configuration.mgt.server.cors.internal.function.CORSUrlToResourceAdd;
import org.wso2.carbon.identity.configuration.mgt.server.cors.internal.function.ResourceToCORSUrl;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.internal.Constants.CORS_URL_RESOURCE_NAME;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.internal.Constants.CORS_URL_RESOURCE_TYPE;

public class CORSServiceImpl implements CORSService {

    private static final Log log = LogFactory.getLog(CORSService.class);

    @Override
    public List<String> getCORSUrls(String tenantDomain) throws CORSServiceException {

        try {
            startTenantFlow(tenantDomain);

            Resource resource = getConfigurationManager().getResource(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME);
            if (resource == null) {
                throw new CORSServiceException(String.format("Tenant %s does not have any CORS URLs", tenantDomain));
            }
            List<String> urls = new ResourceToCORSUrl().apply(resource);

            return urls;
        } catch (ConfigurationManagementException e) {
            throw new CORSServiceException("Error while getting CORS URLs", e);
        } finally {
            endTenantFlow();
        }
    }

    @Override
    public void setCORSUrls(String tenantDomain, List<String> urls) throws CORSServiceException {

        try {
            startTenantFlow(tenantDomain);
            addCORSUrlResourceTypeIfNotExists();

            ResourceAdd resourceAdd = new CORSUrlToResourceAdd().apply(urls);
            getConfigurationManager().replaceResource(CORS_URL_RESOURCE_TYPE, resourceAdd);
        } catch (ConfigurationManagementException e) {
            throw new CORSServiceException("Error while updating CORS URLs", e);
        } finally {
            endTenantFlow();
        }
    }

    @Override
    public void addCORSUrl(String tenantDomain, String url) throws CORSServiceException {

        addCORSUrls(tenantDomain, Collections.singletonList(url));

        try {
            startTenantFlow(tenantDomain);
            addCORSUrlResourceTypeIfNotExists();

            if (tenantHasCORSUrl(url)) {
                throw new CORSServiceException(String.format("Tenant %s already has %s as a CORS URL", tenantDomain, url));
            }

            Attribute attribute = new CORSUrlToAttribute().apply(url);
            getConfigurationManager().addAttribute(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME, attribute);
        } catch (ConfigurationManagementException e) {
            throw new CORSServiceException("Error while adding CORS URL", e);
        } finally {
            endTenantFlow();
        }
    }

    @Override
    public void addCORSUrls(String tenantDomain, List<String> urls) throws CORSServiceException {

        try {
            startTenantFlow(tenantDomain);
            addCORSUrlResourceTypeIfNotExists();

            for(String url: urls) {
                if (tenantHasCORSUrl(url)) {
                    throw new CORSServiceException(String.format("Tenant %s already has %s as a CORS URL", tenantDomain, url));
                }
            }

            for (String url : urls) {
                Attribute attribute = new CORSUrlToAttribute().apply(url);
                getConfigurationManager().addAttribute(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME, attribute);
            }
        } catch (ConfigurationManagementException e) {
            throw new CORSServiceException("Error while adding CORS URL", e);
        } finally {
            endTenantFlow();
        }
    }

    @Override
    public void deleteCORSUrl(String tenantDomain, String url) throws CORSServiceException {

        try {
            startTenantFlow(tenantDomain);

            if (!tenantHasCORSUrl(url)) {
                throw new CORSServiceException(String.format("Tenant %s doesn't have %s as a CORS URL", tenantDomain, url));
            }

            Attribute attribute = new CORSUrlToAttribute().apply(url);
            getConfigurationManager().deleteAttribute(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME,
                    attribute.getKey());
        } catch (ConfigurationManagementException e) {
            throw new CORSServiceException("Error while deleting CORS URL", e);
        } finally {
            endTenantFlow();
        }
    }

    @Override
    public void deleteCORSUrls(String tenantDomain, List<String> urls) throws CORSServiceException {

        try {
            startTenantFlow(tenantDomain);

            for(String url: urls) {
                if (!tenantHasCORSUrl(url)) {
                    throw new CORSServiceException(String.format("Tenant %s doesn't have %s as a CORS URL", tenantDomain, url));
                }
            }

            for (String url : urls) {
                Attribute attribute = new CORSUrlToAttribute().apply(url);
                getConfigurationManager().deleteAttribute(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME,
                        attribute.getKey());
            }
        } catch (ConfigurationManagementException e) {
            throw new CORSServiceException("Error while deleting CORS URLs", e);
        } finally {
            endTenantFlow();
        }
    }

    private void startTenantFlow(String tenantDomain) {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
    }

    private void endTenantFlow() {

        PrivilegedCarbonContext.endTenantFlow();
    }

    private ResourceTypeAdd createCORSUrlResourceTypeToAdd() {

        ResourceTypeAdd resourceTypeAdd = new ResourceTypeAdd();
        resourceTypeAdd.setName(CORS_URL_RESOURCE_TYPE);
        resourceTypeAdd.setDescription("CORS URLs");
        return resourceTypeAdd;
    }

    private boolean isCORSUrlResourceTypeNotExists() throws ConfigurationManagementException {

        try {
            getConfigurationManager().getResourceType(CORS_URL_RESOURCE_TYPE);
        } catch (ConfigurationManagementClientException e) {
            if (ERROR_CODE_RESOURCE_TYPE_DOES_NOT_EXISTS.getCode().equals(e.getErrorCode())) {
                return true;
            }
            throw e;
        }
        return false;
    }

    private void addCORSUrlResourceTypeIfNotExists() throws ConfigurationManagementException {

        if (isCORSUrlResourceTypeNotExists()) {
            ResourceTypeAdd resourceTypeAdd = createCORSUrlResourceTypeToAdd();
            getConfigurationManager().addResourceType(resourceTypeAdd);
        }
    }

    private ConfigurationManager getConfigurationManager() {

        return CORSServiceHolder.getInstance().getConfigurationManager();
    }

    private boolean tenantHasCORSUrl(String url) throws ConfigurationManagementException {

        Resource resource = getConfigurationManager().getResource(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME);
        if (resource != null) {
            List<String> currentUrls = new ResourceToCORSUrl().apply(resource);

            return currentUrls.contains(url);
        }else {
            return false;
        }
    }

}
