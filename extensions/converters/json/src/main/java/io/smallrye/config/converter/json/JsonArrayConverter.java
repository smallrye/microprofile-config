/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config.converter.json;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * Converts a json string to a JSonArray
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
public class JsonArrayConverter implements Converter<JsonArray> {

    @Override
    public JsonArray convert(String input) throws IllegalArgumentException {
        if(isNullOrEmpty(input))return null;
        
        try(JsonReader jsonReader = Json.createReader(new StringReader(input))){
            return jsonReader.readArray();
        }
    }
    
    private boolean isNullOrEmpty(String input){
        return input==null || input.isEmpty();
    }
    
}