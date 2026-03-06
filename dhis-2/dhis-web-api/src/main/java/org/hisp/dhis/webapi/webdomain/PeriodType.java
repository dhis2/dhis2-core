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
package org.hisp.dhis.webapi.webdomain;

import static org.apache.commons.lang3.StringUtils.firstNonBlank;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;
import org.hisp.dhis.i18n.I18n;

/**
 * @author Morten Olav Hansen
 */
@Data
@JacksonXmlRootElement(localName = "periodType", namespace = DXF_2_0)
public class PeriodType {
  private String name;

  private String displayName;

  private String isoDuration;

  private String isoFormat;

  private int frequencyOrder;

  private String label;

  private String displayLabel;

  public PeriodType() {}

  public PeriodType(org.hisp.dhis.period.PeriodType periodType, I18n i18n) {
    this.name = periodType.getName();
    this.displayName = periodType.getDisplayName(i18n);
    this.frequencyOrder = periodType.getFrequencyOrder();
    this.isoDuration = periodType.getIso8601Duration();
    this.isoFormat = periodType.getIsoFormat();
    this.label = periodType.getLabel();
    this.displayLabel = firstNonBlank(periodType.getDisplayLabel(), periodType.getLabel());
  }

  @JsonProperty(namespace = DXF_2_0)
  public String getName() {
    return name;
  }

  @JsonProperty(namespace = DXF_2_0)
  public String getDisplayName() {
    return displayName;
  }

  @JsonProperty(namespace = DXF_2_0)
  public String getIsoDuration() {
    return isoDuration;
  }

  @JsonProperty(namespace = DXF_2_0)
  public String getIsoFormat() {
    return isoFormat;
  }

  @JsonProperty(namespace = DXF_2_0)
  public int getFrequencyOrder() {
    return frequencyOrder;
  }

  @JsonProperty(namespace = DXF_2_0)
  public String getLabel() {
    return label;
  }

  @JsonProperty(namespace = DXF_2_0)
  public String getDisplayLabel() {
    return displayLabel;
  }
}
