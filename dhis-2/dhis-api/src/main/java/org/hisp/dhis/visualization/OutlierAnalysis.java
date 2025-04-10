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
package org.hisp.dhis.visualization;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import lombok.Data;

/** Class responsible for keeping the settings related to outlier analysis in Visualization. */
@Data
public class OutlierAnalysis implements Serializable {

  public static final int MAX_RESULTS_MIN_VALUE = 1;
  public static final int MAX_RESULTS_MAX_VALUE = 500;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private boolean enabled;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private OutlierMethod outlierMethod;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private NormalizedOutlierMethod normalizationMethod;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private Double thresholdFactor;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private OutlierLine extremeLines;

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  private Integer maxResults;

  /**
   * Checks basic rules o ensure this object is valid. The usual validation annotations do not work
   * for this object because this is used as a Json/nested object. This kind of object does not
   * trigger the usual validation through @PropertyRange.
   *
   * @return true if this object is valid, false otherwise.
   */
  @JsonIgnore
  public boolean isValid() {
    return maxResults == null
        || (maxResults >= MAX_RESULTS_MIN_VALUE && maxResults <= MAX_RESULTS_MAX_VALUE);
  }
}
