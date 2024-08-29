/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.hibernate.jsonb.type;

import static org.hisp.dhis.jsontree.JsonBuilder.createObject;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.HibernateException;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;

public class JsonAttributeValueBinaryType extends JsonBinaryType {
  public static final ObjectMapper MAPPER = JacksonObjectMapperConfig.staticJsonMapper();

  @Override
  protected JavaType getResultingJavaType(Class<?> returnedClass) {
    return MAPPER.getTypeFactory().constructMapLikeType(Map.class, String.class, returnedClass);
  }

  @Override
  public String convertObjectToJson(Object object) {
    if (object == null) return "{}";
    @SuppressWarnings("unchecked")
    Set<AttributeValue> attributeValues = (Set<AttributeValue>) object;
    // turn set into a JSON object: {<attribute-uid>:{"value":<value>}}
    JsonNode json =
        createObject(
            obj ->
                attributeValues.forEach(
                    attr ->
                        obj.addObject(
                            attr.getAttribute().getUid(),
                            attrObj -> attrObj.addString("value", attr.getValue()))));
    return json.getDeclaration();
  }

  @Override
  public Object deepCopy(Object value) throws HibernateException {
    String json = convertObjectToJson(value);
    return convertJsonToObject(json);
  }

  @Override
  public Set<AttributeValue> convertJsonToObject(String json) {
    JsonObject attrs = JsonMixed.of(json);
    if (attrs.isUndefined() || attrs.isEmpty()) return new HashSet<>(0);
    Set<AttributeValue> res = new HashSet<>(attrs.size());
    attrs.forEach(
        (uid, value) ->
            res.add(
                new AttributeValue(
                    new Attribute(uid), value.asObject().getString("value").string())));
    return res;
  }
}
