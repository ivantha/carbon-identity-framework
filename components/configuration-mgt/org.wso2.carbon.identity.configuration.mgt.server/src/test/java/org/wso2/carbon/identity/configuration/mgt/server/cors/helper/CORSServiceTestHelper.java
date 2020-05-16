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

package org.wso2.carbon.identity.configuration.mgt.server.cors.helper;

import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceAdd;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceTypeAdd;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.constant.TestConstants.SAMPLE_URL_LIST_1;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.internal.Constants.CORS_URL_RESOURCE_NAME;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.internal.Constants.CORS_URL_RESOURCE_TYPE;

public class CORSServiceTestHelper {

    public static void mockCarbonContextForTenant(int tenantId, String tenantDomain) {

        mockStatic(PrivilegedCarbonContext.class);
        PrivilegedCarbonContext privilegedCarbonContext = mock(PrivilegedCarbonContext.class);

        when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);
        when(privilegedCarbonContext.getTenantDomain()).thenReturn(tenantDomain);
        when(privilegedCarbonContext.getTenantId()).thenReturn(tenantId);
        when(privilegedCarbonContext.getUsername()).thenReturn("admin");
    }

    public static void mockIdentityTenantUtility() {

        mockStatic(IdentityTenantUtil.class);
        IdentityTenantUtil identityTenantUtil = mock(IdentityTenantUtil.class);
        when(identityTenantUtil.getTenantDomain(any(Integer.class))).thenReturn(SUPER_TENANT_DOMAIN_NAME);
    }

    public static ResourceTypeAdd getSampleResourceTypeAdd() {

        ResourceTypeAdd resourceTypeAdd = new ResourceTypeAdd();
        resourceTypeAdd.setName(CORS_URL_RESOURCE_TYPE);

        return resourceTypeAdd;
    }

    public static ResourceAdd getSampleResourceAdd() {

        ResourceAdd resourceAdd = new ResourceAdd();
        resourceAdd.setName(CORS_URL_RESOURCE_NAME);
        List<Attribute> attributeList = new ArrayList<>();
        for (String url : SAMPLE_URL_LIST_1) {
            Attribute attribute = new Attribute(String.valueOf(url.hashCode()), url);
            attributeList.add(attribute);
        }
        resourceAdd.setAttributes(attributeList);

        return resourceAdd;
    }

}
