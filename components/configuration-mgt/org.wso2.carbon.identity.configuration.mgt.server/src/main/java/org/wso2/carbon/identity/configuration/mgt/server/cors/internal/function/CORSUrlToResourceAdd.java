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

package org.wso2.carbon.identity.configuration.mgt.server.cors.internal.function;

import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceAdd;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.wso2.carbon.identity.configuration.mgt.server.cors.internal.Constants.CORS_URL_RESOURCE_NAME;

public class CORSUrlToResourceAdd implements Function<List<String>, ResourceAdd> {

    @Override
    public ResourceAdd apply(List<String> urls) {

        ResourceAdd resourceAdd = new ResourceAdd();
        resourceAdd.setName(CORS_URL_RESOURCE_NAME);

        List<Attribute> attributes = new ArrayList<>();
        for (String url : urls) {
            addAttribute(attributes, String.valueOf(url.hashCode()), url);
        }
        resourceAdd.setAttributes(attributes);

        return resourceAdd;
    }

    private void addAttribute(List<Attribute> attributeList, String key, String value) {

        if (value != null) {
            Attribute attribute = new Attribute();
            attribute.setKey(key);
            attribute.setValue(value);
            attributeList.add(attribute);
        }
    }

}
