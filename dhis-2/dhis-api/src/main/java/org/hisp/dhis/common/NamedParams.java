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
package org.hisp.dhis.common;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Meant as a wrapper around query parameters to provide convenient read access to a mixed map with
 * simple values and lists of simple values as they occur in a HTTP request.
 *
 * <p>Internally it is based on accessor functions so that it can be bound to a {@link Map} or a
 * HTTP request object methods while allowing to be defined in a module that is not dependent on web
 * dependencies.
 *
 * @author Jan Bernitt
 */
public final class NamedParams {

  private final UnaryOperator<String> uni;

  private final Function<String, String[]> multi;

  public NamedParams(Map<String, String> params) {
    this(params::get);
  }

  public NamedParams(UnaryOperator<String> uni) {
    this(
        uni,
        name -> {
          String value = uni.apply(name);
          return value == null ? new String[0] : new String[] {value};
        });
  }

  public NamedParams(UnaryOperator<String> uni, Function<String, String[]> multi) {
    this.uni = uni;
    this.multi = multi;
  }

  public String getString(String name) {
    return uni.apply(name);
  }

  public String getString(String name, String defaultValue) {
    String value = getString(name);
    return value == null ? defaultValue : value;
  }

  public List<String> getStrings(String name) {
    return getStrings(name, ",");
  }

  public List<String> getStrings(String name, String splitRegex) {
    String[] value = multi.apply(name);
    if (value == null || value.length == 0) {
      return emptyList();
    }
    return value.length == 1 ? asList(value[0].split(splitRegex)) : asList(value);
  }

  public int getInt(String name, int defaultValue) {
    return parsedUni(name, defaultValue, Integer::parseInt);
  }

  public boolean getBoolean(String name, boolean defaultValue) {
    String value = uni.apply(name);
    return value == null ? defaultValue : getBoolean(name);
  }

  public boolean getBoolean(String name) {
    String value = uni.apply(name);
    return "true".equalsIgnoreCase(value) || value != null && value.isEmpty();
  }

  public <E extends Enum<E>> E getEnum(String name, E defaultValue) {
    return getEnum(name, defaultValue.getDeclaringClass(), defaultValue);
  }

  public <E extends Enum<E>> E getEnum(String name, Class<E> type, E defaultValue) {
    return parsedUni(
        name,
        defaultValue,
        constant -> Enum.valueOf(type, constant.toUpperCase().replace('-', '_')));
  }

  private <T> T parsedUni(String name, T defaultValue, Function<String, T> parser) {
    String value = getString(name);
    try {
      return value == null ? defaultValue : parser.apply(value);
    } catch (IllegalArgumentException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(ex);
    }
  }
}
