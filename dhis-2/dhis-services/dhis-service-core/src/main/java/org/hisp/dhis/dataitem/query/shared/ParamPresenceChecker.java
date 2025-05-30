/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.dataitem.query.shared;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.VALUE_TYPES;

import java.util.Set;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.ValueType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Checker responsible to provide methods that ensure the existence and integrity of query params.
 *
 * @author maikel arabori
 */
@NoArgsConstructor(access = PRIVATE)
public class ParamPresenceChecker {
  /**
   * This will check if the the given param is inside the given paramsMap and check if the param is
   * a non-blank String.
   *
   * @param paramsMap the query params map
   * @param param the param to be validated, that lives inside the given paramsMap
   * @return true if the param is a String and is not blank, false otherwise
   */
  public static boolean hasNonBlankStringPresence(MapSqlParameterSource paramsMap, String param) {
    return paramsMap != null
        && paramsMap.hasValue(param)
        && paramsMap.getValue(param) instanceof String
        && isNotBlank((String) paramsMap.getValue(param));
  }

  /**
   * This will check if the the given param is inside the given paramsMap and check if the param is
   * a String. It allows the presence of blank and empty strings.
   *
   * @param paramsMap the query params map
   * @param param the param to be validated, that lives inside the given paramsMap
   * @return true if the param is a String, false otherwise
   */
  public static boolean hasStringPresence(MapSqlParameterSource paramsMap, String param) {
    return paramsMap != null
        && paramsMap.hasValue(param)
        && paramsMap.getValue(param) instanceof String;
  }

  /**
   * This will check if the the given param is inside the given paramsMap and check if the param is
   * a positive Integer.
   *
   * @param paramsMap the query params map
   * @param param the param to be validated, that lives inside the given paramsMap
   * @return true if the param is an Integer greater than ZERO, false otherwise
   */
  public static boolean hasIntegerPresence(MapSqlParameterSource paramsMap, String param) {
    return paramsMap != null
        && paramsMap.hasValue(param)
        && paramsMap.getValue(param) instanceof Integer
        && ((Integer) paramsMap.getValue(param) > 0);
  }

  /**
   * This will check if the the given param is inside the given paramsMap and check if the param is
   * a non-empty Set.
   *
   * @param paramsMap the query params map
   * @param param the param to be validated, that lives inside the given paramsMap
   * @return true if the param is a Set and is not empty, false otherwise
   */
  public static boolean hasSetPresence(MapSqlParameterSource paramsMap, String param) {
    return paramsMap != null
        && paramsMap.hasValue(param)
        && paramsMap.getValue(param) instanceof Set
        && isNotEmpty((Set) paramsMap.getValue(param));
  }

  /**
   * Checks if the given paramsMap has the presence of the valueType provided.
   *
   * @param paramsMap
   * @param valueType
   * @return true if valueType is present in paramsMap, false otherwise
   */
  @SuppressWarnings("unchecked")
  public static boolean hasValueTypePresence(MapSqlParameterSource paramsMap, ValueType valueType) {
    if (hasSetPresence(paramsMap, VALUE_TYPES)) {
      Set<String> valueTypeNames = (Set<String>) paramsMap.getValue(VALUE_TYPES);

      return valueTypeNames != null && valueTypeNames.contains(valueType.name());
    }

    return false;
  }
}
