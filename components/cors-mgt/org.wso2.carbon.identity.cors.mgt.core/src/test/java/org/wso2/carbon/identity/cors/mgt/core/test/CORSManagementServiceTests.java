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

package org.wso2.carbon.identity.cors.mgt.core.test;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.cors.mgt.core.CORSManagementService;
import org.wso2.carbon.identity.cors.mgt.core.constant.SQLConstants;
import org.wso2.carbon.identity.cors.mgt.core.dao.CORSConfigurationDAO;
import org.wso2.carbon.identity.cors.mgt.core.dao.CORSOriginDAO;
import org.wso2.carbon.identity.cors.mgt.core.dao.impl.CORSConfigurationDAOImpl;
import org.wso2.carbon.identity.cors.mgt.core.dao.impl.CORSOriginDAOImpl;
import org.wso2.carbon.identity.cors.mgt.core.exception.CORSManagementServiceClientException;
import org.wso2.carbon.identity.cors.mgt.core.exception.CORSManagementServiceException;
import org.wso2.carbon.identity.cors.mgt.core.internal.CORSManagementServiceHolder;
import org.wso2.carbon.identity.cors.mgt.core.internal.impl.CORSManagementServiceImpl;
import org.wso2.carbon.identity.cors.mgt.core.model.CORSManagementServiceConfigurationHolder;
import org.wso2.carbon.identity.cors.mgt.core.model.CORSOrigin;
import org.wso2.carbon.identity.cors.mgt.core.util.CarbonUtils;
import org.wso2.carbon.identity.cors.mgt.core.util.ConfigurationManagementUtils;
import org.wso2.carbon.identity.cors.mgt.core.util.DatabaseUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;
import static org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages.ERROR_CODE_CORS_ADD;
import static org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages.ERROR_CODE_CORS_RETRIEVE;
import static org.wso2.carbon.identity.cors.mgt.core.constant.TestConstants.SAMPLE_APP_ID_1;
import static org.wso2.carbon.identity.cors.mgt.core.constant.TestConstants.SAMPLE_APP_RESOURCE_ID_1;
import static org.wso2.carbon.identity.cors.mgt.core.constant.TestConstants.SAMPLE_APP_RESOURCE_ID_2;
import static org.wso2.carbon.identity.cors.mgt.core.constant.TestConstants.SAMPLE_ORIGIN_LIST_1;
import static org.wso2.carbon.identity.cors.mgt.core.constant.TestConstants.SAMPLE_ORIGIN_LIST_2;
import static org.wso2.carbon.identity.cors.mgt.core.constant.TestConstants.SAMPLE_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.identity.cors.mgt.core.constant.TestConstants.SAMPLE_TENANT_ID;
import static org.wso2.carbon.identity.cors.mgt.core.internal.util.ErrorUtils.handleServerException;

/**
 * Unit test cases for CORSService.
 */
@PrepareForTest({PrivilegedCarbonContext.class,
        IdentityDatabaseUtil.class,
        IdentityUtil.class,
        IdentityTenantUtil.class,
        ApplicationManagementService.class})
public class CORSManagementServiceTests extends PowerMockTestCase {

    private ConfigurationManager configurationManager;
    private Connection connection;
    private CORSManagementService corsManagementService;

    @BeforeMethod
    public void setUp() throws Exception {

        DatabaseUtils.initiateH2Base();

        CarbonUtils.setCarbonHome();
        CarbonUtils.mockCarbonContextForTenant(SUPER_TENANT_ID, SUPER_TENANT_DOMAIN_NAME);
        CarbonUtils.mockIdentityTenantUtility();
        CarbonUtils.mockRealmService();
        CarbonUtils.mockApplicationManagementService();

        connection = DatabaseUtils.createDataSource();
        configurationManager = ConfigurationManagementUtils.getConfigurationManager();

        CORSManagementServiceConfigurationHolder corsManagementServiceConfigurationHolder =
                new CORSManagementServiceConfigurationHolder();
        CORSOriginDAO corsOriginDAO = new CORSOriginDAOImpl();
        CORSConfigurationDAO corsConfigurationDAO = new CORSConfigurationDAOImpl();
        corsManagementServiceConfigurationHolder
                .setCorsOriginDAOS(Collections.singletonList(corsOriginDAO));
        corsManagementServiceConfigurationHolder
                .setCorsConfigurationDAOS(Collections.singletonList(corsConfigurationDAO));

        corsManagementService = new CORSManagementServiceImpl(corsManagementServiceConfigurationHolder);
        CORSManagementServiceHolder.getInstance().setConfigurationManager(configurationManager);
    }

    @AfterMethod
    public void tearDown() throws Exception {

        connection.close();
        DatabaseUtils.closeH2Base();
    }

    @Test
    public void testGetCORSOriginsWithNonExisting() throws CORSManagementServiceException {

        List<CORSOrigin> corsOrigins = corsManagementService.getTenantCORSOrigins(SUPER_TENANT_DOMAIN_NAME);

        assertTrue(corsOrigins.isEmpty());
    }

    @Test
    public void testGetCORSOriginsWithSuperTenant() throws CORSManagementServiceException {

        PreparedStatement preparedStatement = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {
            for (String origin : SAMPLE_ORIGIN_LIST_1) {
                // Origin is not present. Therefore add an origin and set the tenant association.
                preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ORIGIN);
                preparedStatement.setInt(1, SUPER_TENANT_ID);
                preparedStatement.setString(2, origin);
                preparedStatement.setInt(3, 1);
                preparedStatement.executeUpdate();
            }
            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw handleServerException(ERROR_CODE_CORS_ADD, e, IdentityTenantUtil.getTenantDomain(SUPER_TENANT_ID));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }

        List<String> retrievedOrigins = corsManagementService.getTenantCORSOrigins(SUPER_TENANT_DOMAIN_NAME)
                .stream().map(CORSOrigin::getOrigin).collect(Collectors.toList());

        assertEquals(retrievedOrigins, SAMPLE_ORIGIN_LIST_1);
    }

    @Test
    public void testGetCORSOriginsWithApplication() throws CORSManagementServiceException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {
            for (String origin : SAMPLE_ORIGIN_LIST_1) {
                // Origin is not present. Therefore add an origin without the tenant association.
                preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ORIGIN,
                        Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setInt(1, SAMPLE_TENANT_ID);
                preparedStatement.setString(2, origin);
                preparedStatement.setInt(3, 0);
                preparedStatement.executeUpdate();

                // Get origin id.
                int corsOriginId = -1;
                resultSet = preparedStatement.getGeneratedKeys();
                if (resultSet.next()) {
                    corsOriginId = resultSet.getInt(1);
                } else {
                    IdentityDatabaseUtil.rollbackTransaction(connection);
                    throw handleServerException(ERROR_CODE_CORS_ADD, IdentityTenantUtil.getTenantDomain(
                            SAMPLE_TENANT_ID));
                }

                // Add application associations.
                preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ASSOCIATION);
                preparedStatement.setInt(1, corsOriginId);
                preparedStatement.setInt(2, SAMPLE_APP_ID_1);
                preparedStatement.executeUpdate();
            }
            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw handleServerException(ERROR_CODE_CORS_ADD, e, IdentityTenantUtil.getTenantDomain(SAMPLE_TENANT_ID));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
        }

        List<String> retrievedOrigins = corsManagementService.getApplicationCORSOrigins(SAMPLE_TENANT_DOMAIN_NAME,
                SAMPLE_APP_RESOURCE_ID_1).stream().map(CORSOrigin::getOrigin).collect(Collectors.toList());

        assertEquals(retrievedOrigins, SAMPLE_ORIGIN_LIST_1);
    }

    @Test
    public void testSetTenantCORSOrigins() throws CORSManagementServiceException {

        corsManagementService.setTenantCORSOrigins(SUPER_TENANT_DOMAIN_NAME, SAMPLE_ORIGIN_LIST_1);

        List<CORSOrigin> retrievedCORSOrigins = new ArrayList<>();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(false);
        try {
            preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGINS_BY_TENANT_ID);
            preparedStatement.setInt(1, SUPER_TENANT_ID);
            resultSet1 = preparedStatement.executeQuery();

            while (resultSet1.next()) {
                CORSOrigin corsOrigin = new CORSOrigin();
                corsOrigin.setId(resultSet1.getString("ID"));
                corsOrigin.setOrigin(resultSet1.getString("ORIGIN"));
                corsOrigin.setTenantLevel(resultSet1.getString("IS_TENANT_LEVEL").equals("1"));

                // Set the associated applications.
                preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_APPLICATIONS_BY_CORS_ORIGIN_ID);
                preparedStatement.setString(1, corsOrigin.getId());
                resultSet2 = preparedStatement.executeQuery();

                List<CORSOrigin.Application> corsApplications = new ArrayList<>();
                while (resultSet2.next()) {
                    CORSOrigin.Application corsApplication = new CORSOrigin.Application(resultSet2.getString("ID"),
                            resultSet2.getString("APP_NAME"));
                    corsApplications.add(corsApplication);
                }
                corsOrigin.setAssociatedApplications(corsApplications);

                if (corsOrigin.isTenantLevel()) {
                    retrievedCORSOrigins.add(corsOrigin);
                }
            }
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_CORS_RETRIEVE, e, SUPER_TENANT_DOMAIN_NAME);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet1, preparedStatement);
            IdentityDatabaseUtil.closeResultSet(resultSet2);
        }

        assertEquals(retrievedCORSOrigins.stream().map(CORSOrigin::getOrigin).collect(Collectors.toList()),
                SAMPLE_ORIGIN_LIST_1);
    }

    @Test
    public void testSetApplicationCORSOrigins() throws CORSManagementServiceException {

        corsManagementService.setApplicationCORSOrigins(SUPER_TENANT_DOMAIN_NAME, SAMPLE_APP_RESOURCE_ID_1,
                SAMPLE_ORIGIN_LIST_1);

        List<CORSOrigin> retrievedCORSOrigins = new ArrayList<>();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(false);
        try {
            preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGINS_BY_APPLICATION_ID);
            preparedStatement.setInt(1, SUPER_TENANT_ID);
            preparedStatement.setInt(2, SAMPLE_APP_ID_1);
            resultSet1 = preparedStatement.executeQuery();

            while (resultSet1.next()) {
                CORSOrigin corsOrigin = new CORSOrigin();
                corsOrigin.setId(resultSet1.getString("ID"));
                corsOrigin.setOrigin(resultSet1.getString("ORIGIN"));
                corsOrigin.setTenantLevel(resultSet1.getString("IS_TENANT_LEVEL").equals("1"));

                // Set the associated applications.
                preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_APPLICATIONS_BY_CORS_ORIGIN_ID);
                preparedStatement.setString(1, corsOrigin.getId());
                resultSet2 = preparedStatement.executeQuery();

                List<CORSOrigin.Application> corsApplications = new ArrayList<>();
                while (resultSet2.next()) {
                    CORSOrigin.Application corsApplication = new CORSOrigin.Application(resultSet2.getString("ID"),
                            resultSet2.getString("APP_NAME"));
                    corsApplications.add(corsApplication);
                }
                corsOrigin.setAssociatedApplications(corsApplications);

                retrievedCORSOrigins.add(corsOrigin);
            }
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_CORS_RETRIEVE, e, SUPER_TENANT_DOMAIN_NAME);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet1, preparedStatement);
            IdentityDatabaseUtil.closeResultSet(resultSet2);
        }

        assertEquals(retrievedCORSOrigins.stream().map(CORSOrigin::getOrigin).collect(Collectors.toList()),
                SAMPLE_ORIGIN_LIST_1);
    }

    @Test
    public void testAddTenantCORSOrigins() throws ConfigurationManagementException,
            CORSManagementServiceException {

        corsManagementService.addTenantCORSOrigins(SUPER_TENANT_DOMAIN_NAME, SAMPLE_ORIGIN_LIST_1);
        corsManagementService.addApplicationCORSOrigins(SUPER_TENANT_DOMAIN_NAME, SAMPLE_APP_RESOURCE_ID_1,
                SAMPLE_ORIGIN_LIST_2);

        List<CORSOrigin> retrievedCORSOrigins = new ArrayList<>();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(false);
        try {
            preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGINS_BY_TENANT_ID);
            preparedStatement.setInt(1, SUPER_TENANT_ID);
            resultSet1 = preparedStatement.executeQuery();

            while (resultSet1.next()) {
                CORSOrigin corsOrigin = new CORSOrigin();
                corsOrigin.setId(resultSet1.getString("ID"));
                corsOrigin.setOrigin(resultSet1.getString("ORIGIN"));
                corsOrigin.setTenantLevel(resultSet1.getString("IS_TENANT_LEVEL").equals("1"));

                // Set the associated applications.
                preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_APPLICATIONS_BY_CORS_ORIGIN_ID);
                preparedStatement.setString(1, corsOrigin.getId());
                resultSet2 = preparedStatement.executeQuery();

                List<CORSOrigin.Application> corsApplications = new ArrayList<>();
                while (resultSet2.next()) {
                    CORSOrigin.Application corsApplication = new CORSOrigin.Application(resultSet2.getString("ID"),
                            resultSet2.getString("APP_NAME"));
                    corsApplications.add(corsApplication);
                }
                corsOrigin.setAssociatedApplications(corsApplications);

                retrievedCORSOrigins.add(corsOrigin);
            }
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_CORS_RETRIEVE, e, SUPER_TENANT_DOMAIN_NAME);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet1, preparedStatement);
            IdentityDatabaseUtil.closeResultSet(resultSet2);
        }

        assertEquals(retrievedCORSOrigins.stream().map(CORSOrigin::getOrigin).collect(Collectors.toList()),
                Stream.concat(SAMPLE_ORIGIN_LIST_1.stream(), SAMPLE_ORIGIN_LIST_2.stream())
                        .collect(Collectors.toList()));
        assertEquals(retrievedCORSOrigins.stream().filter(CORSOrigin::isTenantLevel)
                .map(CORSOrigin::getOrigin).collect(Collectors.toList()), SAMPLE_ORIGIN_LIST_1);
        assertEquals(retrievedCORSOrigins.stream().filter(corsOrigin -> !corsOrigin.isTenantLevel())
                .map(CORSOrigin::getOrigin).collect(Collectors.toList()), SAMPLE_ORIGIN_LIST_2);
    }

    @Test
    public void testAddApplicationCORSOrigins() throws ConfigurationManagementException,
            CORSManagementServiceException {

        corsManagementService.addApplicationCORSOrigins(SUPER_TENANT_DOMAIN_NAME, SAMPLE_APP_RESOURCE_ID_1,
                SAMPLE_ORIGIN_LIST_1);

        List<CORSOrigin> retrievedCORSOrigins = new ArrayList<>();

        PreparedStatement preparedStatement = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(false);
        try {
            preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGINS_BY_TENANT_ID);
            preparedStatement.setInt(1, SUPER_TENANT_ID);
            resultSet1 = preparedStatement.executeQuery();

            while (resultSet1.next()) {
                CORSOrigin corsOrigin = new CORSOrigin();
                corsOrigin.setId(resultSet1.getString("ID"));
                corsOrigin.setOrigin(resultSet1.getString("ORIGIN"));
                corsOrigin.setTenantLevel(resultSet1.getString("IS_TENANT_LEVEL").equals("1"));

                // Set the associated applications.
                preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_APPLICATIONS_BY_CORS_ORIGIN_ID);
                preparedStatement.setString(1, corsOrigin.getId());
                resultSet2 = preparedStatement.executeQuery();

                List<CORSOrigin.Application> corsApplications = new ArrayList<>();
                while (resultSet2.next()) {
                    CORSOrigin.Application corsApplication = new CORSOrigin.Application(resultSet2.getString("ID"),
                            resultSet2.getString("APP_NAME"));
                    corsApplications.add(corsApplication);
                }
                corsOrigin.setAssociatedApplications(corsApplications);

                retrievedCORSOrigins.add(corsOrigin);
            }
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_CORS_RETRIEVE, e, SUPER_TENANT_DOMAIN_NAME);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet1, preparedStatement);
            IdentityDatabaseUtil.closeResultSet(resultSet2);
        }

        assertEquals(retrievedCORSOrigins.stream().map(CORSOrigin::getOrigin).collect(Collectors.toList()),
                SAMPLE_ORIGIN_LIST_1);
    }

    @Test
    public void testAddCORSOriginsWithInvalidApp() {

        assertThrows(CORSManagementServiceClientException.class, () -> corsManagementService
                .addApplicationCORSOrigins(SUPER_TENANT_DOMAIN_NAME, SAMPLE_APP_RESOURCE_ID_2,
                        SAMPLE_ORIGIN_LIST_2));
    }

    @Test
    public void testDeleteTenantCORSOrigins() throws CORSManagementServiceException, ConfigurationManagementException {

        PreparedStatement preparedStatement = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {
            for (String origin : SAMPLE_ORIGIN_LIST_1) {
                // Origin is not present. Therefore add an origin and set the tenant association.
                preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ORIGIN);
                preparedStatement.setInt(1, SUPER_TENANT_ID);
                preparedStatement.setString(2, origin);
                preparedStatement.setInt(3, 1);
                preparedStatement.executeUpdate();
            }
            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw handleServerException(ERROR_CODE_CORS_ADD, e, IdentityTenantUtil.getTenantDomain(SUPER_TENANT_ID));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }

        List<CORSOrigin> preRetrievedOrigins = corsManagementService.getTenantCORSOrigins(SUPER_TENANT_DOMAIN_NAME);

        corsManagementService.deleteTenantCORSOrigins(SUPER_TENANT_DOMAIN_NAME,
                preRetrievedOrigins.subList(0, 2).stream().map(CORSOrigin::getId).collect(Collectors.toList()));

        List<CORSOrigin> retrievedOrigins = corsManagementService.getTenantCORSOrigins(SUPER_TENANT_DOMAIN_NAME);

        assertEquals(retrievedOrigins.stream().map(CORSOrigin::getOrigin).collect(Collectors.toList()),
                SAMPLE_ORIGIN_LIST_1.subList(2, SAMPLE_ORIGIN_LIST_1.size()));
    }

    @Test
    public void testDeleteApplicationCORSOrigins() throws CORSManagementServiceException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {
            for (String origin : SAMPLE_ORIGIN_LIST_1) {
                // Origin is not present. Therefore add an origin without the tenant association.
                preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ORIGIN,
                        Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setInt(1, SUPER_TENANT_ID);
                preparedStatement.setString(2, origin);
                preparedStatement.setInt(3, 0);
                preparedStatement.executeUpdate();

                // Get origin id.
                int corsOriginId = -1;
                resultSet = preparedStatement.getGeneratedKeys();
                if (resultSet.next()) {
                    corsOriginId = resultSet.getInt(1);
                } else {
                    IdentityDatabaseUtil.rollbackTransaction(connection);
                    throw handleServerException(ERROR_CODE_CORS_ADD, IdentityTenantUtil.getTenantDomain(
                            SUPER_TENANT_ID));
                }

                // Add application associations.
                preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ASSOCIATION);
                preparedStatement.setInt(1, corsOriginId);
                preparedStatement.setInt(2, SAMPLE_APP_ID_1);
                preparedStatement.executeUpdate();
            }
            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw handleServerException(ERROR_CODE_CORS_ADD, e, IdentityTenantUtil.getTenantDomain(SAMPLE_TENANT_ID));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
        }

        List<CORSOrigin> preRetrievedOrigins = corsManagementService
                .getApplicationCORSOrigins(SUPER_TENANT_DOMAIN_NAME, SAMPLE_APP_RESOURCE_ID_1);

        corsManagementService.deleteApplicationCORSOrigins(SUPER_TENANT_DOMAIN_NAME, SAMPLE_APP_RESOURCE_ID_1,
                preRetrievedOrigins.subList(0, 2).stream().map(CORSOrigin::getId).collect(Collectors.toList()));

        List<CORSOrigin> retrievedOrigins = corsManagementService
                .getApplicationCORSOrigins(SUPER_TENANT_DOMAIN_NAME, SAMPLE_APP_RESOURCE_ID_1);

        assertEquals(retrievedOrigins.stream().map(CORSOrigin::getOrigin).collect(Collectors.toList()),
                SAMPLE_ORIGIN_LIST_1.subList(2, SAMPLE_ORIGIN_LIST_1.size()));
    }
}
