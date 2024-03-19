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
package org.hisp.dhis.render;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Objects;
import java.util.Set;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.render.type.ValueTypeRenderingType;

/**
 * This class represents how a class (DataElement or TrackedEntityAttribute), a ValueType or
 * OptionSet can be rendered
 */
public class ObjectValueTypeRenderingOption {
  /** The class that should be rendered */
  private Class<?> clazz;

  /** The ValueType of the class to be rendered */
  private ValueType valueType;

  /** Does the object repreent an option set? */
  private boolean hasOptionSet;

  /** A set of renderingTypes available for the combination of clazz valueType and hasOptionSet */
  private Set<ValueTypeRenderingType> renderingTypes;

  public ObjectValueTypeRenderingOption(
      Class<?> clazz,
      ValueType valueType,
      boolean hasOptionSet,
      Set<ValueTypeRenderingType> renderingTypes) {
    this.clazz = clazz;
    this.valueType = valueType;
    this.hasOptionSet = hasOptionSet;
    this.renderingTypes = renderingTypes;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Set<ValueTypeRenderingType> getRenderingTypes() {
    return renderingTypes;
  }

  public void setRenderingTypes(Set<ValueTypeRenderingType> renderingTypes) {
    this.renderingTypes = renderingTypes;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isHasOptionSet() {
    return hasOptionSet;
  }

  public void setHasOptionSet(boolean hasOptionSet) {
    this.hasOptionSet = hasOptionSet;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ValueType getValueType() {
    return valueType;
  }

  public void setValueType(ValueType valueType) {
    this.valueType = valueType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Class<?> getClazz() {
    return clazz;
  }

  public void setClazz(Class<?> clazz) {
    this.clazz = clazz;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ObjectValueTypeRenderingOption that = (ObjectValueTypeRenderingOption) o;
    return hasOptionSet == that.hasOptionSet
        && Objects.equals(clazz, that.clazz)
        && valueType == that.valueType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(clazz, valueType, hasOptionSet);
  }
}
