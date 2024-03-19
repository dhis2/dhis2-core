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
package org.hisp.dhis.translation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.Objects;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@JacksonXmlRootElement(localName = "translations", namespace = DxfNamespaces.DXF_2_0)
public class Translation implements Serializable {
  private String locale;

  private String property;

  private String value;

  public Translation() {}

  public Translation(String locale, String property, String value) {
    this.locale = locale;
    this.property = property;
    this.value = value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(locale, property, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final Translation other = (Translation) obj;

    return Objects.equals(this.locale, other.locale)
        && Objects.equals(this.property, other.property)
        && Objects.equals(this.value, other.value);
  }

  /**
   * Creates a cache key.
   *
   * @param locale the locale string, i.e. Locale.toString().
   * @param property the translation property.
   * @return a unique cache key valid for a given translated objects, or null if either locale or
   *     property is null.
   */
  public static String getCacheKey(String locale, String property) {
    return locale != null && property != null ? (locale + property) : null;
  }

  // -------------------------------------------------------------------------------
  // Accessors
  // -------------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("locale", locale)
        .add("property", property)
        .add("value", value)
        .toString();
  }
}
