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

package org.wso2.carbon.identity.cors.mgt.core.constant;

import java.util.Arrays;
import java.util.List;

/**
 * Constants for the tests.
 */
public class TestConstants {

    public static final List<String> SAMPLE_ORIGIN_LIST_1 = Arrays.asList(
            "http://foo.com",
            "http://bar.com",
            "https://foobar.com");
    public static final List<String> SAMPLE_ORIGIN_LIST_2 = Arrays.asList(
            "http://abc.com",
            "https://pqr.com",
            "http://xyz.com");
    public static final String SAMPLE_TENANT_DOMAIN_NAME = "abc.com";
    public static final int SAMPLE_TENANT_ID = 4;
    public static final int SAMPLE_APP_ID_1 = 11;
    public static final String SAMPLE_APP_RESOURCE_ID_1 = "c0881fad-fb6f-4d08-b4ad-2680364bd998";
    public static final int SAMPLE_APP_ID_2 = 22;
    public static final String SAMPLE_APP_RESOURCE_ID_2 = "4d34790b-8d16-4e03-b6c5-476c0bf31038";

    private TestConstants() {

    }
}
