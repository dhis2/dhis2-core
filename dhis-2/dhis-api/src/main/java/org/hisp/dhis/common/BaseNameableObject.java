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
package org.hisp.dhis.common;

import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Objects;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;

/**
 * @author Bob Jolliffe
 */
@JacksonXmlRootElement(localName = "nameableObject", namespace = DxfNamespaces.DXF_2_0)
public class BaseNameableObject extends BaseIdentifiableObject implements NameableObject {
  /** Short name representing this object. Optional and unique. */
  protected String shortName;

  /** Description of this object. */
  protected String description;

  protected String formName;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public BaseNameableObject() {}

  public BaseNameableObject(String uid, String code, String name) {
    this.uid = uid;
    this.code = code;
    this.name = name;
  }

  public BaseNameableObject(
      long id, String uid, String name, String shortName, String code, String description) {
    super(id, uid, name);
    this.shortName = shortName;
    this.code = code;
    this.description = description;
  }

  public BaseNameableObject(NameableObject object) {
    super(object.getId(), object.getUid(), object.getName());
    this.shortName = object.getShortName();
    this.code = object.getCode();
    this.description = object.getDescription();
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /**
   * Returns the display property indicated by the given display property. Falls back to display
   * name if display short name is null.
   *
   * @param displayProperty the display property.
   * @return the display property.
   */
  @Override
  @JsonIgnore
  public String getDisplayProperty(DisplayProperty displayProperty) {
    if (DisplayProperty.SHORTNAME == displayProperty && getDisplayShortName() != null) {
      return getDisplayShortName();
    } else {
      return getDisplayName();
    }
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (getShortName() != null ? getShortName().hashCode() : 0);
    result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
    return result;
  }

  /** Class check uses isAssignableFrom and get-methods to handle proxied objects. */
  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof BaseNameableObject other
            && getRealClass(this) == getRealClass(obj)
            && super.equals(obj)
            && objectEquals(other);
  }

  private boolean objectEquals(BaseNameableObject other) {
    return Objects.equals(getShortName(), other.getShortName())
        && Objects.equals(getDescription(), other.getDescription());
  }

  @Override
  public String toString() {
    return "{"
        + "\"class\":\""
        + getClass()
        + "\", "
        + "\"hashCode\":\""
        + hashCode()
        + "\", "
        + "\"id\":\""
        + getId()
        + "\", "
        + "\"uid\":\""
        + getUid()
        + "\", "
        + "\"code\":\""
        + getCode()
        + "\", "
        + "\"name\":\""
        + getName()
        + "\", "
        + "\"shortName\":\""
        + getShortName()
        + "\", "
        + "\"description\":\""
        + getDescription()
        + "\", "
        + "\"created\":\""
        + getCreated()
        + "\", "
        + "\"lastUpdated\":\""
        + getLastUpdated()
        + "\" "
        + "}";
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @Override
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @PropertyRange(min = 1, max = 50)
  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  @Override
  @Sortable(whenPersisted = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "shortName", key = "SHORT_NAME")
  public String getDisplayShortName() {
    return getTranslation("SHORT_NAME", getShortName());
  }

  @Override
  @Sortable
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = 1)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  @Sortable(value = false)
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return getTranslation("DESCRIPTION", getDescription());
  }

  /** Returns the form name, or the name if it does not exist. */
  public String getFormNameFallback() {
    return formName != null && !formName.isEmpty() ? getFormName() : getDisplayName();
  }

  @Sortable
  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFormName() {
    return formName;
  }

  public void setFormName(String formName) {
    this.formName = formName;
  }

  @JsonProperty
  @Sortable(whenPersisted = false)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "formName", key = "FORM_NAME")
  public String getDisplayFormName() {
    return getTranslation("FORM_NAME", getFormNameFallback());
  }
}
