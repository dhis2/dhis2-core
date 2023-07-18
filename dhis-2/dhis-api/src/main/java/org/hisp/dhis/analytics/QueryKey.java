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
package org.hisp.dhis.analytics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Lars Helge Overland
 */
public class QueryKey {
  // Null Value
  public static final String NV = "NV";

  private static final char VALUE_SEP = ':';

  private static final char COMPONENT_SEP = '-';

  List<String> keyComponents = new ArrayList<>();

  public QueryKey() {}

  /**
   * Adds a component to this key. Null values are included.
   *
   * @param property the key property.
   * @param value the key component value.
   */
  public QueryKey add(String property, Object value) {
    String keyComponent = property + VALUE_SEP + value;
    this.keyComponents.add(keyComponent);
    return this;
  }

  /**
   * Adds a component to this key. Null values are included.
   *
   * @param value the key component value.
   */
  public QueryKey add(Object value) {
    this.keyComponents.add(String.valueOf(value));
    return this;
  }

  /**
   * Adds a component to this key. Null values are omitted.
   *
   * @param property the key property.
   * @param value the key component value.
   */
  public QueryKey addIgnoreNull(String property, Object value) {
    if (value != null) {
      this.add(property, value);
    }

    return this;
  }

  /**
   * Adds a component value to this key if the given object is not null, supplied by the given value
   * supplier.
   *
   * @param property the key property.
   * @param object the object to check for null.
   * @param valueSupplier the supplier of the key component value.
   */
  public QueryKey addIgnoreNull(String property, Object object, Supplier<String> valueSupplier) {
    if (object != null) {
      this.addIgnoreNull(property, valueSupplier.get());
    }

    return this;
  }

  /**
   * Returns a plain text key based on the components of this key. Use {@link QueryKey#build()} to
   * obtain a shorter and more usable key.
   */
  public String asPlainKey() {
    return StringUtils.join(keyComponents, COMPONENT_SEP);
  }

  /** Returns a 40-character unique key. The key is a SHA-1 hash of the components of this key. */
  public String build() {
    return DigestUtils.sha1Hex(asPlainKey());
  }

  /** Equal to {@link QueryKey#build()}. */
  @Override
  public String toString() {
    return build();
  }
}
