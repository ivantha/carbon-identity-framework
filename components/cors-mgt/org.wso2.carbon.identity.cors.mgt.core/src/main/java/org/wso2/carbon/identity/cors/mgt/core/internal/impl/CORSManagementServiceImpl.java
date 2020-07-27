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

package org.wso2.carbon.identity.cors.mgt.core.internal.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.cors.mgt.core.CORSManagementService;
import org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages;
import org.wso2.carbon.identity.cors.mgt.core.dao.CORSConfigurationDAO;
import org.wso2.carbon.identity.cors.mgt.core.dao.CORSOriginDAO;
import org.wso2.carbon.identity.cors.mgt.core.exception.CORSManagementServiceClientException;
import org.wso2.carbon.identity.cors.mgt.core.exception.CORSManagementServiceException;
import org.wso2.carbon.identity.cors.mgt.core.model.CORSConfiguration;
import org.wso2.carbon.identity.cors.mgt.core.model.CORSManagementServiceConfigurationHolder;
import org.wso2.carbon.identity.cors.mgt.core.model.CORSOrigin;
import org.wso2.carbon.identity.cors.mgt.core.model.ValidatedOrigin;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages.ERROR_CODE_CORS_GET_DAO;
import static org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages.ERROR_CODE_ORIGIN_NOT_PRESENT;
import static org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages.ERROR_CODE_ORIGIN_PRESENT;
import static org.wso2.carbon.identity.cors.mgt.core.constant.ErrorMessages.ERROR_CODE_VALIDATE_APP_ID;
import static org.wso2.carbon.identity.cors.mgt.core.internal.util.ErrorUtils.handleClientException;
import static org.wso2.carbon.identity.cors.mgt.core.internal.util.ErrorUtils.handleServerException;

/**
 * Implementation of the CORSService.
 */
public class CORSManagementServiceImpl implements CORSManagementService {

    private static final Log log = LogFactory.getLog(CORSManagementServiceImpl.class);

    private List<CORSOriginDAO> corsOriginDAOS;
    private List<CORSConfigurationDAO> corsConfigurationDAOS;

    public CORSManagementServiceImpl(
            CORSManagementServiceConfigurationHolder corsManagementServiceConfigurationHolder) {

        this.corsOriginDAOS = corsManagementServiceConfigurationHolder.getCorsOriginDAOS();
        this.corsConfigurationDAOS = corsManagementServiceConfigurationHolder.getCorsConfigurationDAOS();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CORSOrigin> getTenantCORSOrigins(String tenantDomain) throws CORSManagementServiceException {

        int tenantId = getTenantId(tenantDomain);

        return Collections.unmodifiableList(getCORSOriginDAO().getCORSOriginsByTenantId(tenantId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CORSOrigin> getApplicationCORSOrigins(String tenantDomain, String appId)
            throws CORSManagementServiceException {

        int tenantId = getTenantId(tenantDomain);
        ApplicationBasicInfo applicationBasicInfo = getApplicationBasicInfo(tenantDomain, appId);

        return Collections.unmodifiableList(getCORSOriginDAO().getCORSOriginsByApplicationId(tenantId,
                applicationBasicInfo.getApplicationId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTenantCORSOrigins(String tenantDomain, List<String> origins) throws CORSManagementServiceException {

        int tenantId = getTenantId(tenantDomain);

        // Convert Origins to ValidatedOrigins.
        List<ValidatedOrigin> validatedOrigins = originsToValidatedOrigins(origins);

        // Set the CORS origins.
        getCORSOriginDAO().setTenantCORSOrigins(tenantId, validatedOrigins.stream().map(validatedOrigin -> {
            // Create the CORS origin.
            CORSOrigin corsOrigin = new CORSOrigin();
            corsOrigin.setOrigin(validatedOrigin.getValue());
            return corsOrigin;
        }).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationCORSOrigins(String tenantDomain, String appId, List<String> origins)
            throws CORSManagementServiceException {

        int tenantId = getTenantId(tenantDomain);
        ApplicationBasicInfo applicationBasicInfo = getApplicationBasicInfo(tenantDomain, appId);

        // Convert Origins to ValidatedOrigins.
        List<ValidatedOrigin> validatedOrigins = originsToValidatedOrigins(origins);

        // Set the CORS origins.
        getCORSOriginDAO().setApplicationCORSOrigins(tenantId, applicationBasicInfo.getApplicationId(),
                validatedOrigins.stream().map(validatedOrigin -> {
                    // Create the CORS origin.
                    CORSOrigin corsOrigin = new CORSOrigin();
                    corsOrigin.setOrigin(validatedOrigin.getValue());
                    corsOrigin.setTenantLevel(false);
                    return corsOrigin;
                }).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTenantCORSOrigins(String tenantDomain, List<String> origins) throws CORSManagementServiceException {

        int tenantId = getTenantId(tenantDomain);

        // Convert Origins to ValidatedOrigins.
        List<ValidatedOrigin> validatedOrigins = originsToValidatedOrigins(origins);

        // Check if the CORS origins are already present.
        List<CORSOrigin> existingCORSOrigins = getCORSOriginDAO().getCORSOriginsByTenantId(tenantId);
        for (ValidatedOrigin validatedOrigin : validatedOrigins) {
            if (existingCORSOrigins.stream().map(CORSOrigin::getId).collect(Collectors.toList())
                    .contains(validatedOrigin.getValue())) {
                // CORS origin is already registered for the tenant.
                if (log.isDebugEnabled()) {
                    log.debug(String.format(ERROR_CODE_ORIGIN_PRESENT.getMessage(), tenantDomain, validatedOrigin));
                }
                throw handleClientException(ERROR_CODE_ORIGIN_PRESENT, tenantDomain, validatedOrigin.getValue());
            }
        }

        // Add the CORS origins.
        getCORSOriginDAO().addTenantCORSOrigins(tenantId, validatedOrigins.stream().map(validatedOrigin -> {
            // Create the CORS origin.
            CORSOrigin corsOrigin = new CORSOrigin();
            corsOrigin.setOrigin(validatedOrigin.getValue());
            return corsOrigin;
        }).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addApplicationCORSOrigins(String tenantDomain, String appId, List<String> origins)
            throws CORSManagementServiceException {

        int tenantId = getTenantId(tenantDomain);
        ApplicationBasicInfo applicationBasicInfo = getApplicationBasicInfo(tenantDomain, appId);

        // Convert Origins to ValidatedOrigins.
        List<ValidatedOrigin> validatedOrigins = originsToValidatedOrigins(origins);

        // Check if the CORS origins are already present.
        List<CORSOrigin> existingCORSOrigins = getCORSOriginDAO().getCORSOriginsByApplicationId(tenantId,
                applicationBasicInfo.getApplicationId());
        for (ValidatedOrigin validatedOrigin : validatedOrigins) {
            if (existingCORSOrigins.stream().map(CORSOrigin::getId).collect(Collectors.toList())
                    .contains(validatedOrigin.getValue())) {
                // CORS origin is already registered for the application.
                if (log.isDebugEnabled()) {
                    log.debug(String.format(ERROR_CODE_ORIGIN_PRESENT.getMessage(), tenantDomain, validatedOrigin));
                }
                throw handleClientException(ERROR_CODE_ORIGIN_PRESENT, tenantDomain, validatedOrigin.getValue());
            }
        }

        // Add the CORS origins.
        getCORSOriginDAO().addApplicationCORSOrigins(tenantId, applicationBasicInfo.getApplicationId(),
                validatedOrigins.stream().map(validatedOrigin -> {
                    // Create the CORS origin.
                    CORSOrigin corsOrigin = new CORSOrigin();
                    corsOrigin.setOrigin(validatedOrigin.getValue());
                    return corsOrigin;
                }).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteTenantCORSOrigins(String tenantDomain, List<String> originIds)
            throws CORSManagementServiceException {

        int tenantId = getTenantId(tenantDomain);

        // Check if the CORS origins are not in the system.
        List<CORSOrigin> existingCORSOrigins = getCORSOriginDAO().getCORSOriginsByTenantId(tenantId);
        for (String originId : originIds) {
            if (!existingCORSOrigins.stream().map(CORSOrigin::getId).collect(Collectors.toList()).contains(originId)) {
                // CORS origin is not registered for the tenant.
                if (log.isDebugEnabled()) {
                    log.debug(String.format(ERROR_CODE_ORIGIN_NOT_PRESENT.getMessage(), tenantDomain, originId));
                }
                throw handleClientException(ERROR_CODE_ORIGIN_NOT_PRESENT, tenantDomain, originId);
            }
        }

        // Delete the CORS origin tenant association.
        getCORSOriginDAO().deleteTenantCORSOriginAssociationsById(tenantId,
                originIds.stream().map(Integer::parseInt).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteApplicationCORSOrigins(String tenantDomain, String appId, List<String> originIds)
            throws CORSManagementServiceException {

        int tenantId = getTenantId(tenantDomain);
        ApplicationBasicInfo applicationBasicInfo = getApplicationBasicInfo(tenantDomain, appId);

        // Check if the CORS origins are not in the system.
        List<CORSOrigin> existingCORSOrigins = getCORSOriginDAO().getCORSOriginsByApplicationId(tenantId,
                applicationBasicInfo.getApplicationId());
        for (String originId : originIds) {
            if (!existingCORSOrigins.stream().map(CORSOrigin::getId).collect(Collectors.toList()).contains(originId)) {
                // CORS origin is not registered for the application.
                if (log.isDebugEnabled()) {
                    log.debug(String.format(ERROR_CODE_ORIGIN_NOT_PRESENT.getMessage(), tenantDomain, originId));
                }
                throw handleClientException(ERROR_CODE_ORIGIN_NOT_PRESENT, tenantDomain, originId);
            }
        }

        // Delete the CORS origin application associations.
        getCORSOriginDAO().deleteApplicationCORSOriginAssociationsById(tenantId,
                applicationBasicInfo.getApplicationId(), originIds.stream()
                        .map(Integer::parseInt).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CORSOrigin.Application> getCORSOriginApplications(String corsOriginId)
            throws CORSManagementServiceException {

        // DAO layer throws an exception if CORSApplications cannot be retrieved for the corsOriginId.
        // i.e The corsOriginId is invalid.
        return Collections.unmodifiableList(getCORSOriginDAO()
                .getCORSOriginApplications(Integer.parseInt(corsOriginId)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CORSConfiguration getCORSConfiguration(String tenantDomain) throws CORSManagementServiceException {

        getTenantId(tenantDomain);

        return getCORSConfigurationDAO().getCORSConfigurationByTenantDomain(tenantDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCORSConfiguration(String tenantDomain, CORSConfiguration corsConfiguration)
            throws CORSManagementServiceException {

        getTenantId(tenantDomain);

        getCORSConfigurationDAO().setCORSConfigurationByTenantDomain(tenantDomain, corsConfiguration);
    }

    /**
     * Select highest priority CORSOrigin DAO from an already sorted list of CORSOrigin DAOs.
     *
     * @return Highest priority CORSOrigin DAO.
     */
    private CORSOriginDAO getCORSOriginDAO() throws CORSManagementServiceException {

        if (!this.corsOriginDAOS.isEmpty()) {
            return corsOriginDAOS.get(corsOriginDAOS.size() - 1);
        } else {
            throw handleServerException(ERROR_CODE_CORS_GET_DAO, "corsOriginDAOs");
        }
    }

    /**
     * Select highest priority CORSConfiguration DAO from an already sorted list of CORSConfiguration DAOs.
     *
     * @return Highest priority CORSConfiguration DAO.
     */
    private CORSConfigurationDAO getCORSConfigurationDAO() throws CORSManagementServiceException {

        if (!this.corsConfigurationDAOS.isEmpty()) {
            return corsConfigurationDAOS.get(corsConfigurationDAOS.size() - 1);
        } else {
            throw handleServerException(ERROR_CODE_CORS_GET_DAO, "corsConfigurationDAOs");
        }
    }

    /**
     * Returns the tenant ID from the tenant domain.
     *
     * @param tenantDomain The tenant domain.
     * @return The tenant ID.
     * @throws CORSManagementServiceClientException
     */
    private int getTenantId(String tenantDomain) throws CORSManagementServiceClientException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        if (tenantId == MultitenantConstants.INVALID_TENANT_ID) {
            throw handleClientException(ErrorMessages.ERROR_CODE_INVALID_TENANT_DOMAIN, tenantDomain);
        } else {
            return tenantId;
        }
    }

    /**
     * Get an {code ApplicationBasicInfo} instance belonging to the application with the resource ID {@code appId}.
     *
     * @param tenantDomain The tenant domain.
     * @param appId        The application resource ID.
     * @return An {@code ApplicationBasicInfo} instance.
     * @throws CORSManagementServiceClientException
     */
    private ApplicationBasicInfo getApplicationBasicInfo(String tenantDomain, String appId)
            throws CORSManagementServiceClientException {

        // If the appId is blank then throw an exception.
        if (StringUtils.isBlank(appId)) {
            throw handleClientException(ErrorMessages.ERROR_CODE_INVALID_APP_ID, appId);
        }

        // Check whether the appId belongs to the tenant with the tenantDomain.
        try {
            ApplicationBasicInfo applicationBasicInfo = ApplicationManagementService.getInstance()
                    .getApplicationBasicInfoByResourceId(appId, tenantDomain);
            if (applicationBasicInfo == null) {
                throw handleClientException(ErrorMessages.ERROR_CODE_INVALID_APP_ID, appId);
            } else {
                return applicationBasicInfo;
            }
        } catch (IdentityApplicationManagementException e) {
            // Something else happened.
            log.error(String.format(ERROR_CODE_VALIDATE_APP_ID.getDescription(), appId), e);
        }
        return null;
    }

    /**
     * Convert origin strings to ValidatedOrigins.
     *
     * @param origins A list of origin strings.
     * @return A list of {@code ValidatedOrigin}s containing the initial origins.
     * @throws CORSManagementServiceClientException
     */
    private List<ValidatedOrigin> originsToValidatedOrigins(List<String> origins)
            throws CORSManagementServiceClientException {

        List<ValidatedOrigin> validatedOrigins = new ArrayList<>();
        for (String origin : origins) {
            validatedOrigins.add(new ValidatedOrigin(origin));
        }

        return validatedOrigins;
    }
}
