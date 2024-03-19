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
package org.hisp.dhis.utils;

import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A simple java objects to JSON output mapper for test data creation. The idea here is not to have
 * a full-blown Java-to-JSON converter but something that makes simple test value creation more
 * convenient.
 *
 * @author Jan Bernitt
 */
public final class JavaToJson {
  private JavaToJson() {
    throw new UnsupportedOperationException("util");
  }

  public static String singleToDoubleQuotes(String json) {
    return json.replace('\'', '"');
  }

  /** Creates a JSON number from a Java number */
  public static String toJson(double value) {
    return value % 1 == 0d ? String.valueOf((long) value) : String.valueOf(value);
  }

  /** Creates a JSON boolean from a Java boolean */
  public static String toJson(boolean value) {
    return value ? "true" : "false";
  }

  /** Creates a quoted JSON string from a Java string (not quoted) */
  public static String toJson(String value) {
    return value == null ? "null" : "\"" + value + "\"";
  }

  /** Creates JSON array from any Java array type */
  public static String toJson(Object[] value) {
    return value == null
        ? "null"
        : "[" + Arrays.stream(value).map(JavaToJson::toJson).collect(joining(",")) + "]";
  }

  /** Creates JSON arrays from a {@link Collection} */
  public static String toJson(Collection<?> value) {
    return value == null ? "null" : toJson(value.toArray());
  }

  /** Creates JSON arrays from a {@link Collection} */
  public static String toJson(Stream<?> value) {
    return value == null ? "null" : toJson(value.toArray());
  }

  /**
   * Creates JSON objects from a {@link Map}.
   *
   * <p>OBS!!! members will be sorted alphabetically for reproducibility. This is mostly to counter
   * the non-deterministic order of {@link Map#of()} methods.
   */
  public static String toJson(Map<String, ?> value) {
    return value == null
        ? "null"
        : "{"
            + value.entrySet().stream()
                .sorted(comparingByKey())
                .map(e -> toJson(e.getKey()) + ":" + toJson(e.getValue()))
                .collect(joining(","))
            + "}";
  }

  /**
   * Creates JSON based on the actual type of the provided Java object. Supported are {@link
   * String}, {@link Number}s, {@link Boolean}s, {@link Collection}s, {@link Stream}, {@link Map}s
   * and array types.
   */
  public static String toJson(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof String) {
      return toJson((String) value);
    }
    if (value instanceof Boolean) {
      return toJson(((Boolean) value).booleanValue());
    }
    if (value instanceof Number) {
      return toJson(((Number) value).doubleValue());
    }
    if (value instanceof Object[]) {
      return toJson((Object[]) value);
    }
    if (value instanceof Collection) {
      return toJson((Collection<?>) value);
    }
    if (value instanceof Stream) {
      return toJson((Stream<?>) value);
    }
    if (value instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, ?> map = (Map<String, ?>) value;
      return toJson(map);
    }
    throw new UnsupportedOperationException(
        String.format(
            "Mapping `%s` of type %s to JSON is not yet supported",
            value, value.getClass().getSimpleName()));
  }
}
