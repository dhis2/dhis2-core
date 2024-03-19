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
package org.hisp.dhis.dataitem;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.ValueType;

/**
 * This is a pure DTO class and basic constructors in order to make it simple. It's used only as
 * final output/response to the consumer.
 *
 * @author maikel arabori
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@JacksonXmlRootElement(localName = "dataItem", namespace = DXF_2_0)
public class DataItem implements Serializable {
  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private String name;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private String shortName;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private String displayName;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private String displayShortName;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private String id;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private String code;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private DimensionItemType dimensionItemType;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private String programId;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private ValueType valueType;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private ValueType simplifiedValueType;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private String expression;

  public ValueType getSimplifiedValueType() {
    return valueType != null ? valueType.toSimplifiedValueType() : null;
  }
}
