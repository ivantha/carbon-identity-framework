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

package org.wso2.carbon.identity.cors.mgt.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for a CORS origin.
 */
public class CORSOrigin {

    /**
     * ID of the origin.
     */
    private String id;

    /**
     * The origin of the CORSOrigin instance.
     */
    private String origin;

    /**
     * Whether the origin is associated with the tenant.
     */
    private boolean isTenantLevel;

    /**
     * Applications associated with the {@code CORSOrigin}.
     */
    private List<Application> associatedApplications;

    /**
     * Default constructor.
     */
    public CORSOrigin() {

        this.associatedApplications = new ArrayList<>();
    }

    /**
     * Get the {@code id}.
     *
     * @return Returns the {@code id}.
     */
    public String getId() {

        return id;
    }

    /**
     * Set the {@code id}.
     */
    public void setId(String id) {

        this.id = id;
    }

    /**
     * Get the {@code origin}.
     *
     * @return Returns the {@code origin}.
     */
    public String getOrigin() {

        return origin;
    }

    /**
     * Set the {@code origin}.
     *
     * @param origin The value to be set as the {@code origin}.
     */
    public void setOrigin(String origin) {

        this.origin = origin;
    }

    public boolean isTenantLevel() {

        return isTenantLevel;
    }

    public void setTenantLevel(boolean tenantLevel) {

        isTenantLevel = tenantLevel;
    }

    /**
     * Get the {@code associatedApplications}.
     *
     * @return Returns the {@code associatedApplications}.
     */
    public List<Application> getAssociatedApplications() {

        return associatedApplications;
    }

    /**
     * Set the {@code associatedApplications}.
     *
     * @param associatedApplications The list of {@code Application}s to be set as the
     *                               {@code associatedApplications}.
     */
    public void setAssociatedApplications(List<Application> associatedApplications) {

        this.associatedApplications = associatedApplications;
    }

    /**
     * Application which has an association with a particular CORS origin.
     */
    public static class Application {

        /**
         * ID of the application.
         */
        private String id;

        /**
         * Name of the application.
         */
        private String name;

        /**
         * Constructor for Application.
         *
         * @param id   ID of the application.
         */
        public Application(String id) {

            this.id = id;
        }

        /**
         * Constructor for Application.
         *
         * @param id   ID of the application.
         * @param name Name of the application.
         */
        public Application(String id, String name) {

            this.id = id;
            this.name = name;
        }

        /**
         * Get the {@code id}.
         *
         * @return Returns the {@code id}.
         */
        public String getId() {

            return id;
        }

        /**
         * Set the {@code id}.
         */
        public void setId(String id) {

            this.id = id;
        }

        /**
         * Get the {@code name}.
         *
         * @return Returns the {@code name}.
         */
        public String getName() {

            return name;
        }

        /**
         * Set the {@code name}.
         */
        public void setName(String name) {

            this.name = name;
        }
    }
}
