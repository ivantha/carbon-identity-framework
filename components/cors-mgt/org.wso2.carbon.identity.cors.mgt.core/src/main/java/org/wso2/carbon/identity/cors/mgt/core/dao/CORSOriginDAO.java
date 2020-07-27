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

package org.wso2.carbon.identity.cors.mgt.core.dao;

import org.wso2.carbon.identity.cors.mgt.core.exception.CORSManagementServiceServerException;
import org.wso2.carbon.identity.cors.mgt.core.model.CORSOrigin;

import java.util.List;

/**
 * Perform CRUD operations for {@link CORSOrigin}.
 */
public interface CORSOriginDAO {

    /**
     * Get priority value for the {@link CORSOriginDAO}.
     *
     * @return Priority value for the DAO.
     */
    int getPriority();

    List<CORSOrigin> getCORSOriginsByTenantId(int tenantId)
            throws CORSManagementServiceServerException;

    List<CORSOrigin> getCORSOriginsByApplicationId(int tenantId, int appId)
            throws CORSManagementServiceServerException;

    void setTenantCORSOrigins(int tenantId, List<CORSOrigin> corsOrigins)
            throws CORSManagementServiceServerException;

    void setApplicationCORSOrigins(int tenantId, int appId, List<CORSOrigin> corsOrigins)
            throws CORSManagementServiceServerException;

    void addTenantCORSOrigins(int tenantId, List<CORSOrigin> corsOrigins)
            throws CORSManagementServiceServerException;

    void addApplicationCORSOrigins(int tenantId, int appId, List<CORSOrigin> corsOrigins)
            throws CORSManagementServiceServerException;

    void updateCORSOrigins(List<CORSOrigin> corsOrigins)
            throws CORSManagementServiceServerException;

    void deleteCORSOriginsById(List<String> corsOriginIds)
            throws CORSManagementServiceServerException;

    void deleteTenantCORSOriginAssociationsById(int tenantId, List<Integer> corsOriginIds)
            throws CORSManagementServiceServerException;

    void deleteApplicationCORSOriginAssociationsById(int tenantId, int appId, List<Integer> corsOriginIds)
            throws CORSManagementServiceServerException;

    List<CORSOrigin.Application> getCORSOriginApplications(int corsOriginId)
            throws CORSManagementServiceServerException;
}
