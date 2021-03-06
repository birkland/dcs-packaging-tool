/*
 * Copyright 2015 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.packaging.tool.impl.support;

import org.dataconservancy.packaging.tool.model.dprofile.PropertyValueHint;

/**
 * A simple factory class which, when provided a validator type, provides an appropriate validator for property values
 */
public class ValidatorFactory {
    /**
     * Returns a validator for the given ValidatorType
     * @param type  the given ValidatorType
     * @return  the Validator
     */
    public static Validator getValidator(PropertyValueHint type){
        switch(type){
            case EMAIL:
                return new EmailValidator();
            case FILE_NAME:
                return new FilenameValidator();
            case PHONE_NUMBER:
                return new PhoneNumberValidator();
            case URL:
                return new UrlValidator();
            case URI:
                return new URIValidator();
            default:
                return null;
        }
    }
}
