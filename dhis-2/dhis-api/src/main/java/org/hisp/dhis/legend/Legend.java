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
package org.hisp.dhis.legend;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.persistence.*;
import java.util.Set;
import org.hibernate.annotations.Type;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.BaseMetadataObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.TranslationProperty;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Jan Henrik Overland
 */
@Entity
@Table(name = "maplegend")
@JacksonXmlRootElement(localName = "legend", namespace = DxfNamespaces.DXF_2_0)
public class Legend extends BaseMetadataObject implements IdentifiableObject, EmbeddedObject {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "maplegendid")
  private long id;

  @Column(name = "code", length = 50)
  private String code;

  @Column(name = "name", length = 255)
  private String name;

  @Embedded
  private TranslationProperty translations;

  @Transient
  private Sharing sharing = new Sharing();

  @Column(name = "startvalue")
  @org.hibernate.annotations.Index(name = "maplegend_startvalue")
  private Double startValue;

  @Column(name = "endvalue")
  @org.hibernate.annotations.Index(name = "maplegend_endvalue")
  private Double endValue;

  @Column(name = "color", length = 255)
  private String color;

  @Column(name = "image", length = 255)
  private String image;

  @ManyToOne
  @JoinColumn(name = "maplegendsetid")
  private LegendSet legendSet;

  public Legend() {}

  public Legend(String name, Double startValue, Double endValue, String color, String image) {
    this.name = name;
    this.startValue = startValue;
    this.endValue = endValue;
    this.color = color;
    this.image = image;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = Integer.MIN_VALUE)
  public Double getStartValue() {
    return startValue;
  }

  public void setStartValue(Double startValue) {
    this.startValue = startValue;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @PropertyRange(min = Integer.MIN_VALUE)
  public Double getEndValue() {
    return endValue;
  }

  public void setEndValue(Double endValue) {
    this.endValue = endValue;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public LegendSet getLegendSet() {
    return legendSet;
  }

  public void setLegendSet(LegendSet legendSet) {
    this.legendSet = legendSet;
  }

  // -------------------------------------------------------------------------
  // IdentifiableObject implementation
  // -------------------------------------------------------------------------

  @Override
  public long getId() {
    return id;
  }

  @Override
  public void setId(long id) {
    this.id = id;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getCode() {
    return code;
  }

  @Override
  public void setCode(String code) {
    this.code = code;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDisplayName() {
    return getName();
  }

  @Override
  public Set<Translation> getTranslations() {
    return translations != null ? translations.getTranslations() : Set.of();
  }

  @Override
  public void setTranslations(Set<Translation> translations) {
    if (this.translations == null) {
      this.translations = new TranslationProperty();
    }
    this.translations.setTranslations(translations);
  }

  @Override
  public Sharing getSharing() {
    if (sharing == null) {
      sharing = new Sharing();
    }
    return sharing;
  }

  @Override
  public void setSharing(Sharing sharing) {
    this.sharing = sharing;
  }

  // -------------------------------------------------------------------------
  // Unsupported IdentifiableObject methods
  // -------------------------------------------------------------------------

  /** This entity does not support attribute values. */
  @Override
  @Deprecated
  public AttributeValues getAttributeValues() {
    return AttributeValues.empty();
  }

  /** This entity does not support attribute values. */
  @Override
  @Deprecated
  public void setAttributeValues(AttributeValues attributeValues) {
    // Not supported
  }

  /** This entity does not support attribute values. */
  @Override
  @Deprecated
  public void addAttributeValue(String attributeUid, String value) {
    // Not supported
  }

  /** This entity does not support attribute values. */
  @Override
  @Deprecated
  public void removeAttributeValue(String attributeId) {
    // Not supported
  }

  /** @deprecated This method is replaced by {@link #getCreatedBy()} */
  @Override
  @Deprecated
  public User getUser() {
    return getCreatedBy();
  }

  /** @deprecated This method is replaced by {@link #setCreatedBy(User)} */
  @Override
  @Deprecated
  public void setUser(User user) {
    setCreatedBy(user);
  }

  @Override
  @Deprecated
  public void setOwner(String owner) {
    // Not supported
  }

  @Override
  public String getPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return getUid();
    }
    if (idScheme.is(IdentifiableProperty.CODE)) {
      return getCode();
    }
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getName();
    }
    return null;
  }

  @Override
  public String getDisplayPropertyValue(IdScheme idScheme) {
    if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
      return getDisplayName();
    }
    if (idScheme.is(IdentifiableProperty.CODE)) {
      return getCode();
    }
    if (idScheme.is(IdentifiableProperty.NAME)) {
      return getDisplayName();
    }
    return null;
  }
}
