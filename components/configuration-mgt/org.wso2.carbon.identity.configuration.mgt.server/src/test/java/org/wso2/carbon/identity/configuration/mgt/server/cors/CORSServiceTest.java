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

package org.wso2.carbon.identity.configuration.mgt.server.cors;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManagerImpl;
import org.wso2.carbon.identity.configuration.mgt.core.dao.ConfigurationDAO;
import org.wso2.carbon.identity.configuration.mgt.core.dao.impl.ConfigurationDAOImpl;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.internal.ConfigurationManagerComponentDataHolder;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.ConfigurationManagerConfigurationHolder;
import org.wso2.carbon.identity.configuration.mgt.server.cors.exception.CORSServiceException;
import org.wso2.carbon.identity.configuration.mgt.server.cors.internal.CORSServiceHolder;
import org.wso2.carbon.identity.configuration.mgt.server.cors.internal.impl.CORSServiceImpl;
import org.wso2.carbon.identity.configuration.mgt.server.cors.util.TestUtils;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import javax.sql.DataSource;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.helper.CORSServiceTestHelper.getSampleResourceAdd;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.helper.CORSServiceTestHelper.getSampleResourceTypeAdd;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.helper.CORSServiceTestHelper.mockCarbonContextForTenant;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.helper.CORSServiceTestHelper.mockIdentityTenantUtility;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.constant.TestConstants.SAMPLE_URL;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.constant.TestConstants.SAMPLE_URL_LIST_1;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.constant.TestConstants.SAMPLE_URL_LIST_2;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.internal.Constants.CORS_URL_RESOURCE_NAME;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.internal.Constants.CORS_URL_RESOURCE_TYPE;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.util.TestUtils.closeH2Base;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.util.TestUtils.initiateH2Base;
import static org.wso2.carbon.identity.configuration.mgt.server.cors.util.TestUtils.spyConnection;

@PrepareForTest({PrivilegedCarbonContext.class, IdentityDatabaseUtil.class, IdentityUtil.class, IdentityTenantUtil.class})
public class CORSServiceTest extends PowerMockTestCase {

    private ConfigurationManager configurationManager;
    private Connection connection;

    private CORSService corsService;

    @BeforeMethod
    public void setUp() throws Exception {

        initiateH2Base();
        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());

        DataSource dataSource = mock(DataSource.class);
        mockStatic(IdentityDatabaseUtil.class);
        when(IdentityDatabaseUtil.getDataSource()).thenReturn(dataSource);

        connection = TestUtils.getConnection();
        Connection spyConnection = spyConnection(connection);
        when(dataSource.getConnection()).thenReturn(spyConnection);

        // Mock get maximum query length call.
        mockStatic(IdentityUtil.class);
        when(IdentityUtil.getProperty(any(String.class))).thenReturn("4194304");
        when(IdentityUtil.getEndpointURIPath(any(String.class), anyBoolean(), anyBoolean())).thenReturn(
                "/t/bob.com/api/identity/config-mgt/v1.0/resource/file/publisher/SMSPublisher/9e038218-8e99-4dae-bf83-a78f5dcd73a8");
        ConfigurationManagerComponentDataHolder.setUseCreatedTime(true);
        ConfigurationManagerConfigurationHolder configurationHolder = new ConfigurationManagerConfigurationHolder();
        ConfigurationDAO configurationDAO = new ConfigurationDAOImpl();
        configurationHolder.setConfigurationDAOS(Collections.singletonList(configurationDAO));
        mockCarbonContextForTenant(SUPER_TENANT_ID, SUPER_TENANT_DOMAIN_NAME);
        mockIdentityTenantUtility();
        configurationManager = new ConfigurationManagerImpl(configurationHolder);

        ConfigurationManagerComponentDataHolder.getInstance().setConfigurationManagementEnabled(true);

        corsService = new CORSServiceImpl();
        CORSServiceHolder.getInstance().setConfigurationManager(configurationManager);
    }

    @AfterMethod
    public void tearDown() throws Exception {

        connection.close();
        closeH2Base();
    }

    @Test
    public void testGetCORSUrls() {

        try {
            configurationManager.addResourceType(getSampleResourceTypeAdd());
            configurationManager.addResource(CORS_URL_RESOURCE_TYPE, getSampleResourceAdd());
            List<String> urls = corsService.getCORSUrls(SUPER_TENANT_DOMAIN_NAME);

            assertEquals(SAMPLE_URL_LIST_1, urls);
        } catch (CORSServiceException | ConfigurationManagementException throwables) {
            throwables.printStackTrace();
        }
    }

    @Test
    public void testSetCORSUrls() {

        try {
            corsService.setCORSUrls(SUPER_TENANT_DOMAIN_NAME, SAMPLE_URL_LIST_1);
            List<String> urls = configurationManager.getResource(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME)
                    .getAttributes()
                    .stream()
                    .map(Attribute::getValue)
                    .collect(Collectors.toList());

            assertEquals(SAMPLE_URL_LIST_1, urls);
        } catch (CORSServiceException | ConfigurationManagementException throwables) {
            throwables.printStackTrace();
        }
    }

    @Test
    public void testAddCORSUrl() {

        try {
            corsService.setCORSUrls(SUPER_TENANT_DOMAIN_NAME, SAMPLE_URL_LIST_1);
            corsService.addCORSUrl(SUPER_TENANT_DOMAIN_NAME, SAMPLE_URL);
            List<String> urls = configurationManager.getResource(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME)
                    .getAttributes()
                    .stream()
                    .map(Attribute::getValue)
                    .collect(Collectors.toList());

            assertEquals(Stream.concat(SAMPLE_URL_LIST_1.stream(), Stream.of(SAMPLE_URL)).collect(Collectors.toList()),
                    urls);
        } catch (CORSServiceException | ConfigurationManagementException throwables) {
            throwables.printStackTrace();
        }
    }

    @Test
    public void testAddCORSUrls() {

        try {
            corsService.setCORSUrls(SUPER_TENANT_DOMAIN_NAME, SAMPLE_URL_LIST_1);
            corsService.addCORSUrls(SUPER_TENANT_DOMAIN_NAME, SAMPLE_URL_LIST_2);
            List<String> urls = configurationManager.getResource(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME)
                    .getAttributes()
                    .stream()
                    .map(Attribute::getValue)
                    .collect(Collectors.toList());

            assertEquals(Stream.concat(SAMPLE_URL_LIST_1.stream(),
                    SAMPLE_URL_LIST_2.stream()).collect(Collectors.toList()), urls);
        } catch (CORSServiceException | ConfigurationManagementException throwables) {
            throwables.printStackTrace();
        }
    }

    @Test
    public void testDeleteCORSUrl() {

        try {
            corsService.setCORSUrls(SUPER_TENANT_DOMAIN_NAME, SAMPLE_URL_LIST_1);
            corsService.deleteCORSUrl(SUPER_TENANT_DOMAIN_NAME, SAMPLE_URL_LIST_1.get(0));
            List<String> urls = configurationManager.getResource(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME)
                    .getAttributes()
                    .stream()
                    .map(Attribute::getValue)
                    .collect(Collectors.toList());

            assertEquals(SAMPLE_URL_LIST_1.subList(1, SAMPLE_URL_LIST_1.size()), urls);
        } catch (CORSServiceException | ConfigurationManagementException throwables) {
            throwables.printStackTrace();
        }
    }

    @Test
    public void testDeleteCORSUrls() {

        try {
            corsService.setCORSUrls(SUPER_TENANT_DOMAIN_NAME, SAMPLE_URL_LIST_1);
            corsService.deleteCORSUrls(SUPER_TENANT_DOMAIN_NAME, SAMPLE_URL_LIST_1.subList(0, 2));
            List<String> urls = configurationManager.getResource(CORS_URL_RESOURCE_TYPE, CORS_URL_RESOURCE_NAME)
                    .getAttributes()
                    .stream()
                    .map(Attribute::getValue)
                    .collect(Collectors.toList());

            assertEquals(SAMPLE_URL_LIST_1.subList(2, SAMPLE_URL_LIST_1.size()), urls);
        } catch (CORSServiceException | ConfigurationManagementException throwables) {
            throwables.printStackTrace();
        }
    }

}
