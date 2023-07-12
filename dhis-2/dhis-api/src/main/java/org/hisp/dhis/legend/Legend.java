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
package org.hisp.dhis.legend;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translation;

/**
 * @author Jan Henrik Overland
 */
@JacksonXmlRootElement(localName = "legend", namespace = DxfNamespaces.DXF_2_0)
@Entity
@Table(
    name = "maplegend",
    indexes = {
      @Index(name = "maplegend_startvalue", columnList = "startValue"),
      @Index(name = "maplegend_endvalue", columnList = "endvalue")
    })
public class Legend extends BaseIdentifiableObject implements EmbeddedObject {

  @Id
  @Column(name = "maplegendid")
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  @Column(name = "uid", length = 11, nullable = false, unique = true)
  protected String uid;

  @Column(name = "startValue", nullable = false)
  private Double startValue;

  @Column(name = "endvalue", nullable = false)
  private Double endValue;

  @Column(name = "name", length = 230, nullable = false)
  private String name;

  @Column private String color;

  @Column private String image;

  @ManyToOne
  @JoinColumn(
      name = "maplegendsetid",
      referencedColumnName = "maplegendsetid",
      foreignKey = @ForeignKey(name = "fk_maplegend_maplegendsetid"))
  private LegendSet legendSet;

  @Column(name = "translations")
  @Type(type = "jblTranslations")
  protected Set<Translation> translations = new HashSet<>();

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

  @Override
  @JsonProperty(value = "id")
  @JacksonXmlProperty(localName = "id", isAttribute = true)
  @Description("The Unique Identifier for this Object.")
  @Property(value = PropertyType.IDENTIFIER, required = Property.Value.FALSE)
  @PropertyRange(min = 11, max = 11)
  public String getUid() {
    return uid;
  }

  @Override
  public void setUid(String uid) {
    this.uid = uid;
  }

  @Override
  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The name of this Object. Required and unique.")
  @PropertyRange(min = 1)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
