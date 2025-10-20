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
package org.hisp.dhis.query;

import com.google.common.base.Enums;
import com.google.common.collect.Lists;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class QueryUtils {

  private static final String UNABLE_TO_PARSE = "Unable to parse `";

  private QueryUtils() {
    throw new UnsupportedOperationException("util");
  }

  public static <T> T parseValue(Class<?> klass, Object objectValue) {
    return parseValue(klass, null, objectValue);
  }

  @SuppressWarnings("unchecked")
  public static <T> T parseValue(Class<?> klass, Class<?> secondaryKlass, Object objectValue) {
    if (klass.isInstance(objectValue)) {
      return (T) objectValue;
    }

    if (!(objectValue instanceof String)) {
      return (T) objectValue;
    }

    String value = (String) objectValue;

    if (Integer.class.isAssignableFrom(klass)) {
      try {
        return (T) Integer.valueOf(value);
      } catch (Exception ex) {
        throw new QueryParserException(UNABLE_TO_PARSE + value + "` as `Integer`.");
      }
    }
    if (Boolean.class.isAssignableFrom(klass)) {
      try {
        return (T) Boolean.valueOf(value);
      } catch (Exception ex) {
        throw new QueryParserException(UNABLE_TO_PARSE + value + "` as `Boolean`.");
      }
    }
    if (Float.class.isAssignableFrom(klass)) {
      try {
        return (T) Float.valueOf(value);
      } catch (Exception ex) {
        throw new QueryParserException(UNABLE_TO_PARSE + value + "` as `Float`.");
      }
    }
    if (Double.class.isAssignableFrom(klass)) {
      try {
        return (T) Double.valueOf(value);
      } catch (Exception ex) {
        throw new QueryParserException(UNABLE_TO_PARSE + value + "` as `Double`.");
      }
    }
    if (Date.class.isAssignableFrom(klass)) {
      try {
        Date date = DateUtils.parseDate(value);
        return (T) date;
      } catch (Exception ex) {
        throw new QueryParserException(UNABLE_TO_PARSE + value + "` as `Date`.");
      }
    }
    if (Enum.class.isAssignableFrom(klass)) {
      T enumValue = getEnumValue(klass, value);

      if (enumValue != null) {
        return enumValue;
      }
    } else if (Collection.class.isAssignableFrom(klass) || Map.class.isAssignableFrom(klass)) {
      if (!value.startsWith("[") || !value.endsWith("]")) {
        try {
          return (T) Integer.valueOf(value);
        } catch (NumberFormatException e) {
          throw new QueryParserException("Collection size must be integer `" + value + "`");
        }
      }

      String[] split = value.substring(1, value.length() - 1).split(",");
      List<String> items = Lists.newArrayList(split);

      if (secondaryKlass != null) {
        List<Object> convertedList = new ArrayList<>();

        for (String item : items) {
          Object convertedValue = parseValue(secondaryKlass, null, item);

          if (convertedValue != null) {
            convertedList.add(convertedValue);
          }
        }

        return (T) convertedList;
      }

      return (T) items;
    }

    throw new QueryParserException(
        UNABLE_TO_PARSE + value + "` to `" + klass.getSimpleName() + "`.");
  }

  /**
   * Try and parse `value` as Enum. Throws `QueryException` if invalid value.
   *
   * @param klass the Enum class.
   * @param value the enum value.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> T getEnumValue(Class<?> klass, String value) {
    Optional<? extends Enum<?>> enumValue =
        Enums.getIfPresent((Class<? extends Enum>) klass, value).toJavaUtil();

    if (enumValue.isPresent()) {
      return (T) enumValue.get();
    }
    Object[] possibleValues = klass.getEnumConstants();
    throw new QueryParserException(
        UNABLE_TO_PARSE
            + value
            + "` as `"
            + klass
            + "`, available values are: "
            + Arrays.toString(possibleValues));
  }

  public static Object parseValue(String value) {
    if (value == null || StringUtils.isEmpty(value)) {
      return null;
    }
    if (NumberUtils.isCreatable(value)) {
      return value;
    }
    return "'" + value + "'";
  }

  /**
   * Converts a String with JSON format [x,y,z] into an SQL query collection format (x,y,z).
   *
   * @param value a string contains a collection with JSON format [x,y,z].
   * @return a string contains a collection with SQL query format (x,y,z).
   */
  public static String convertCollectionValue(String value) {
    if (StringUtils.isEmpty(value)) {
      throw new QueryParserException("Value is null");
    }

    if (!value.startsWith("[") || !value.endsWith("]")) {
      throw new QueryParserException("Invalid query value");
    }

    String[] split = value.substring(1, value.length() - 1).split(",");
    List<String> items = Lists.newArrayList(split);
    StringBuilder str = new StringBuilder("(");

    for (int i = 0; i < items.size(); i++) {
      Object item = QueryUtils.parseValue(items.get(i));
      if (item != null) {
        str.append(item);
        if (i < items.size() - 1) {
          str.append(",");
        }
      }
    }

    str.append(")");

    return str.toString();
  }

  /**
   * Returns a single result from the given {@link TypedQuery}. Returns null if no objects could be
   * found (without throwing an exception).
   *
   * @param query the query.
   * @return an object.
   */
  public static <T> T getSingleResult(TypedQuery<T> query) {
    query.setMaxResults(1);

    List<T> list = query.getResultList();

    if (list == null || list.isEmpty()) {
      return null;
    }

    return list.get(0);
  }
}
