/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.identity.cors.mgt.core.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.cors.mgt.core.constant.SQLConstants;
import org.wso2.carbon.identity.cors.mgt.core.dao.CORSOriginDAO;
import org.wso2.carbon.identity.cors.mgt.core.exception.CORSManagementServiceServerException;
import org.wso2.carbon.identity.cors.mgt.core.model.CORSOrigin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages.ERROR_CODE_CORS_ADD;
import static org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages.ERROR_CODE_CORS_APPLICATIONS_RETRIEVE;
import static org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages.ERROR_CODE_CORS_ORIGIN_DELETE;
import static org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages.ERROR_CODE_CORS_RETRIEVE;
import static org.wso2.carbon.identity.cors.mgt.core.internal.util.ErrorUtils.handleServerException;

/**
 * {@link CORSOriginDAO} implementation.
 */
public class CORSOriginDAOImpl implements CORSOriginDAO {

    private static final Log log = LogFactory.getLog(CORSOriginDAOImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {

        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CORSOrigin> getCORSOriginsByTenantId(int tenantId) throws CORSManagementServiceServerException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(false);
        try {
            preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGINS_BY_TENANT_ID);
            preparedStatement.setInt(1, tenantId);
            resultSet1 = preparedStatement.executeQuery();

            List<CORSOrigin> corsOrigins = new ArrayList<>();
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

                corsOrigins.add(corsOrigin);
            }
            return corsOrigins;
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_CORS_RETRIEVE, e, IdentityTenantUtil.getTenantDomain(tenantId));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet1, preparedStatement);
            IdentityDatabaseUtil.closeResultSet(resultSet2);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CORSOrigin> getCORSOriginsByApplicationId(int tenantId, int appId)
            throws CORSManagementServiceServerException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(false);
        try {
            preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGINS_BY_APPLICATION_ID);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.setInt(2, appId);
            resultSet1 = preparedStatement.executeQuery();

            List<CORSOrigin> corsOrigins = new ArrayList<>();
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

                corsOrigins.add(corsOrigin);
            }
            return corsOrigins;
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_CORS_RETRIEVE, e, IdentityTenantUtil.getTenantDomain(tenantId));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet1, preparedStatement);
            IdentityDatabaseUtil.closeResultSet(resultSet2);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTenantCORSOrigins(int tenantId, List<CORSOrigin> corsOrigins)
            throws CORSManagementServiceServerException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {
            // Delete existing tenant associations.
            preparedStatement = connection.prepareStatement(SQLConstants.DELETE_TENANT_ASSOCIATIONS);
            preparedStatement.setInt(1, tenantId);
            preparedStatement.executeUpdate();

            // Cleanup dangling origins without any associations.
            cleanupDanglingOrigins(connection, tenantId);

            for (CORSOrigin corsOrigin : corsOrigins) {
                // Check if the origins is there.
                preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGIN_ID);
                preparedStatement.setInt(1, tenantId);
                preparedStatement.setString(2, corsOrigin.getOrigin());
                resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    // Origin is already present. Therefore just set the tenant association.
                    preparedStatement = connection.prepareStatement(SQLConstants.UPDATE_TENANT_ASSOCIATIONS);
                    preparedStatement.setInt(1, 1);
                    preparedStatement.setInt(2, tenantId);
                    preparedStatement.setString(3, corsOrigin.getOrigin());
                    preparedStatement.executeUpdate();
                } else {
                    // Origin is not present. Therefore add an origin and set the tenant association.
                    preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ORIGIN);
                    preparedStatement.setInt(1, tenantId);
                    preparedStatement.setString(2, corsOrigin.getOrigin());
                    preparedStatement.setInt(3, 1);
                    preparedStatement.executeUpdate();
                }
            }
            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw handleServerException(ERROR_CODE_CORS_ADD, e, IdentityTenantUtil.getTenantDomain(tenantId));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationCORSOrigins(int tenantId, int appId, List<CORSOrigin> corsOrigins)
            throws CORSManagementServiceServerException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {
            // Delete existing application associations.
            preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGINS_BY_TENANT_ID);
            preparedStatement.setInt(1, tenantId);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                CORSOrigin corsOrigin = new CORSOrigin();
                corsOrigin.setId(resultSet.getString("ID"));

                preparedStatement = connection.prepareStatement(SQLConstants.DELETE_APPLICATION_ASSOCIATIONS);
                preparedStatement.setInt(1, Integer.parseInt(corsOrigin.getId()));
                preparedStatement.setInt(2, appId);
                preparedStatement.executeUpdate();
            }

            // Cleanup dangling origins without any associations.
            cleanupDanglingOrigins(connection, tenantId);

            for (CORSOrigin corsOrigin : corsOrigins) {
                // Check if the origins is there.
                preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGIN_ID);
                preparedStatement.setInt(1, tenantId);
                preparedStatement.setString(2, corsOrigin.getOrigin());
                resultSet = preparedStatement.executeQuery();
                if (!resultSet.next()) {
                    // Origin is not present. Therefore add an origin without the tenant association.
                    preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ORIGIN,
                            Statement.RETURN_GENERATED_KEYS);
                    preparedStatement.setInt(1, tenantId);
                    preparedStatement.setString(2, corsOrigin.getOrigin());
                    preparedStatement.setInt(3, 0);
                    preparedStatement.executeUpdate();

                    // Get origin id.
                    int corsOriginId = -1;
                    resultSet = preparedStatement.getGeneratedKeys();
                    if (resultSet.next()) {
                        corsOriginId = resultSet.getInt(1);
                    } else {
                        IdentityDatabaseUtil.rollbackTransaction(connection);
                        throw handleServerException(ERROR_CODE_CORS_ADD, IdentityTenantUtil.getTenantDomain(tenantId));
                    }

                    // Add application associations.
                    preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ASSOCIATION);
                    preparedStatement.setInt(1, corsOriginId);
                    preparedStatement.setInt(2, appId);
                    preparedStatement.executeUpdate();
                }
            }
            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw handleServerException(ERROR_CODE_CORS_ADD, e, IdentityTenantUtil.getTenantDomain(tenantId));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTenantCORSOrigins(int tenantId, List<CORSOrigin> corsOrigins)
            throws CORSManagementServiceServerException {

        PreparedStatement preparedStatement = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {
            for (CORSOrigin corsOrigin : corsOrigins) {
                // Insert cors origins.
                preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ORIGIN);
                preparedStatement.setInt(1, tenantId);
                preparedStatement.setString(2, corsOrigin.getOrigin());
                preparedStatement.setInt(3, 1);
                preparedStatement.executeUpdate();
            }
            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw handleServerException(ERROR_CODE_CORS_ADD, e, IdentityTenantUtil.getTenantDomain(tenantId));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addApplicationCORSOrigins(int tenantId, int appId, List<CORSOrigin> corsOrigins)
            throws CORSManagementServiceServerException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {
            for (CORSOrigin corsOrigin : corsOrigins) {
                // Check if the origins is there.
                preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGIN_ID);
                preparedStatement.setInt(1, tenantId);
                preparedStatement.setString(2, corsOrigin.getOrigin());
                resultSet = preparedStatement.executeQuery();
                if (!resultSet.next()) {
                    // Origin is not present. Therefore add an origin without the tenant association.
                    preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ORIGIN);
                    preparedStatement.setInt(1, tenantId);
                    preparedStatement.setString(2, corsOrigin.getOrigin());
                    preparedStatement.setInt(3, 0);
                    preparedStatement.executeUpdate();
                }

                // Get origin id.
                preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGIN_ID);
                preparedStatement.setInt(1, tenantId);
                preparedStatement.setString(2, corsOrigin.getOrigin());
                resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    int corsOriginId = resultSet.getInt("ID");

                    // Add application associations.
                    preparedStatement = connection.prepareStatement(SQLConstants.INSERT_CORS_ASSOCIATION);
                    preparedStatement.setInt(1, corsOriginId);
                    preparedStatement.setInt(2, appId);
                    preparedStatement.executeUpdate();
                } else {
                    IdentityDatabaseUtil.rollbackTransaction(connection);
                    throw handleServerException(ERROR_CODE_CORS_ADD, IdentityTenantUtil.getTenantDomain(tenantId));
                }
            }
            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw handleServerException(ERROR_CODE_CORS_ADD, e, IdentityTenantUtil.getTenantDomain(tenantId));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateCORSOrigins(List<CORSOrigin> corsOrigins) throws CORSManagementServiceServerException {

        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteCORSOriginsById(List<String> corsOriginIds) throws CORSManagementServiceServerException {

        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteTenantCORSOriginAssociationsById(int tenantId, List<Integer> corsOriginIds)
            throws CORSManagementServiceServerException {

        PreparedStatement preparedStatement = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        int currentId = -1;
        try {
            for (int corsOriginId : corsOriginIds) {
                currentId = corsOriginId;

                // Delete existing tenant associations.
                preparedStatement = connection.prepareStatement(SQLConstants.DELETE_TENANT_ASSOCIATION);
                preparedStatement.setInt(1, corsOriginId);
                preparedStatement.executeUpdate();
            }

            // Cleanup dangling origins without any associations.
            cleanupDanglingOrigins(connection, tenantId);

            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw handleServerException(ERROR_CODE_CORS_ORIGIN_DELETE, e, String.valueOf(currentId));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteApplicationCORSOriginAssociationsById(int tenantId, int appId, List<Integer> corsOriginIds)
            throws CORSManagementServiceServerException {

        PreparedStatement preparedStatement = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        int currentId = -1;
        try {
            for (int corsOriginId : corsOriginIds) {
                currentId = corsOriginId;

                // Delete application association.
                preparedStatement = connection.prepareStatement(SQLConstants.DELETE_APPLICATION_ASSOCIATION);
                preparedStatement.setInt(1, corsOriginId);
                preparedStatement.setInt(2, appId);
                preparedStatement.executeUpdate();
            }

            // Cleanup dangling origins without any associations.
            cleanupDanglingOrigins(connection, tenantId);

            IdentityDatabaseUtil.commitTransaction(connection);
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw handleServerException(ERROR_CODE_CORS_ORIGIN_DELETE, e, String.valueOf(currentId));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, preparedStatement);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CORSOrigin.Application> getCORSOriginApplications(int corsOriginId)
            throws CORSManagementServiceServerException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Connection connection = IdentityDatabaseUtil.getDBConnection(false);
        try {
            preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_APPLICATIONS_BY_CORS_ORIGIN_ID);
            preparedStatement.setInt(1, corsOriginId);
            resultSet = preparedStatement.executeQuery();

            List<CORSOrigin.Application> corsApplications = new ArrayList<>();
            while (resultSet.next()) {
                CORSOrigin.Application corsApplication = new CORSOrigin.Application(resultSet.getString("ID"),
                        resultSet.getString("APP_NAME"));
                corsApplications.add(corsApplication);
            }
            return corsApplications;
        } catch (SQLException e) {
            throw handleServerException(ERROR_CODE_CORS_APPLICATIONS_RETRIEVE, e, String.valueOf(corsOriginId));
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, preparedStatement);
        }
    }

    private void cleanupDanglingOrigins(Connection connection, int tenantId) throws SQLException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;

        try {
            preparedStatement = connection.prepareStatement(SQLConstants.GET_CORS_ORIGINS_BY_TENANT_ID);
            preparedStatement.setInt(1, tenantId);
            resultSet1 = preparedStatement.executeQuery();
            while (resultSet1.next()) {
                CORSOrigin corsOrigin = new CORSOrigin();
                corsOrigin.setId(resultSet1.getString("ID"));
                corsOrigin.setOrigin(resultSet1.getString("ORIGIN"));
                corsOrigin.setTenantLevel(resultSet1.getString("IS_TENANT_LEVEL").equals("1"));

                if (!corsOrigin.isTenantLevel()) {
                    // Get the associated applications.
                    preparedStatement = connection.prepareStatement(SQLConstants
                            .GET_CORS_APPLICATION_IDS_BY_CORS_ORIGIN_ID);
                    preparedStatement.setString(1, corsOrigin.getId());
                    resultSet2 = preparedStatement.executeQuery();

                    if (!resultSet2.next()) {
                        preparedStatement = connection.prepareStatement(SQLConstants.DELETE_ORIGIN);
                        preparedStatement.setInt(1, Integer.parseInt(corsOrigin.getId()));
                        preparedStatement.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw e;
        } finally {
            IdentityDatabaseUtil.closeStatement(preparedStatement);
            IdentityDatabaseUtil.closeResultSet(resultSet1);
            IdentityDatabaseUtil.closeResultSet(resultSet2);
        }
    }
}
