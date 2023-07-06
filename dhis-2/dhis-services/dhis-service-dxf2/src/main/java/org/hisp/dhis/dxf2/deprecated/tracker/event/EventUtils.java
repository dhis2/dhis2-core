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
package org.hisp.dhis.dxf2.deprecated.tracker.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.user.User;
import org.postgresql.util.PGobject;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
public class EventUtils {
  public static final String FALLBACK_USERNAME = "[Unknown]";

  public static String getValidUsername(String userName, ImportOptions importOptions) {
    String validUsername = userName;
    String fallBack =
        importOptions.getUser() != null ? importOptions.getUser().getUsername() : FALLBACK_USERNAME;

    if (StringUtils.isEmpty(validUsername)) {
      validUsername = User.getSafeUsername(fallBack);
    } else if (validUsername.length() > User.USERNAME_MAX_LENGTH) {
      validUsername = User.getSafeUsername(fallBack);
    }

    return validUsername;
  }

  /**
   * Converts a Set of {@see EventDataValue} into a JSON string using the provided Jackson {@see
   * ObjectMapper} This method, before serializing to JSON, if first transforms the Set into a Map,
   * where the Map key is the EventDataValue DataElement UID and the Map value is the actual {@see
   * EventDataValue}.
   *
   * @param dataValues a Set of {@see EventDataValue}
   * @param mapper a configured Jackson {@see ObjectMapper}
   * @return a PGobject containing the serialized Set
   * @throws JsonProcessingException if the JSON serialization fails
   */
  public static PGobject eventDataValuesToJson(Set<EventDataValue> dataValues, ObjectMapper mapper)
      throws JsonProcessingException, SQLException {
    PGobject jsonbObj = new PGobject();
    jsonbObj.setType("json");
    jsonbObj.setValue(
        mapper.writeValueAsString(
            dataValues.stream()
                .collect(Collectors.toMap(EventDataValue::getDataElement, Function.identity()))));
    return jsonbObj;
  }

  /**
   * Converts a {@see DataValue} into a JSON string using the provided Jackson {@see ObjectMapper}.
   *
   * @param dataValue a {@see DataValue}
   * @param mapper a configured Jackson {@see ObjectMapper}
   * @return a PGobject containing the serialized object
   * @throws JsonProcessingException if the JSON serialization fails
   */
  public static PGobject eventDataValuesToJson(DataValue dataValue, ObjectMapper mapper)
      throws JsonProcessingException, SQLException {
    PGobject jsonbObj = new PGobject();
    jsonbObj.setType("json");
    jsonbObj.setValue(mapper.writeValueAsString(dataValue));
    return jsonbObj;
  }

  /**
   * Converts the Event Data Value json payload into a Set of EventDataValue
   *
   * <p>Note that the EventDataValue payload is stored as a map: {dataelementid:{ ...},
   * {dataelementid:{ ...} }
   *
   * <p>Therefore, the conversion is a bit convoluted, since the payload has to be converted into a
   * Map and then into a Set
   */
  public static Set<EventDataValue> jsonToEventDataValues(
      ObjectMapper jsonMapper, Object eventsDataValues) throws JsonProcessingException {
    final TypeFactory typeFactory = jsonMapper.getTypeFactory();
    MapType mapType =
        typeFactory.constructMapType(HashMap.class, String.class, EventDataValue.class);

    String content = null;
    if (eventsDataValues instanceof String) {
      content = (String) eventsDataValues;
    } else if (eventsDataValues instanceof PGobject) {
      content = ((PGobject) eventsDataValues).getValue();
    }

    Set<EventDataValue> dataValues = new HashSet<>();
    if (!StringUtils.isEmpty(content)) {
      Map<String, EventDataValue> parsed = jsonMapper.readValue(content, mapType);
      for (String dataElementId : parsed.keySet()) {
        EventDataValue edv = parsed.get(dataElementId);
        edv.setDataElement(dataElementId);
        dataValues.add(edv);
      }
    }

    return dataValues;
  }

  @SuppressWarnings("unchecked")
  public static Set<AttributeValue> getAttributeValues(
      ObjectMapper jsonMapper, Object attributeValues) throws JsonProcessingException {
    Set<AttributeValue> attributeValueSet = new HashSet<>();

    String content = null;
    if (attributeValues instanceof String) {
      content = (String) attributeValues;
    } else if (attributeValues instanceof PGobject) {
      content = ((PGobject) attributeValues).getValue();
    }

    Map<String, Map<String, String>> m = jsonMapper.readValue(content, Map.class);
    for (String key : m.keySet()) {
      Attribute attribute = new Attribute();
      attribute.setUid(key);
      String value = m.get(key).get("value");
      AttributeValue attributeValue = new AttributeValue(value, attribute);
      attributeValueSet.add(attributeValue);
    }
    return attributeValueSet;
  }

  @SneakyThrows
  public static PGobject userInfoToJson(UserInfoSnapshot userInfo, ObjectMapper mapper) {
    PGobject jsonbObj = new PGobject();
    jsonbObj.setType("json");
    jsonbObj.setValue(mapper.writeValueAsString(userInfo));
    return jsonbObj;
  }

  public static UserInfoSnapshot jsonToUserInfo(String userInfoAsString, ObjectMapper mapper) {
    try {
      if (StringUtils.isNotEmpty(userInfoAsString)) {
        return mapper.readValue(userInfoAsString, UserInfoSnapshot.class);
      }
      return null;
    } catch (IOException e) {
      log.error("Parsing UserInfoSnapshot json string failed. String value: " + userInfoAsString);
      throw new IllegalArgumentException(e);
    }
  }
}
