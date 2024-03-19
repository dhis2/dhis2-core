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
package org.hisp.dhis.dataanalysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DataAnalysisParams {
  private String startDate;

  private String endDate;

  private List<String> ds;

  private Double standardDeviation;

  private String ou;

  public DataAnalysisParams() {}

  @JsonProperty
  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  @JsonProperty
  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  @JsonProperty
  public List<String> getDs() {
    return ds;
  }

  public void setDs(List<String> ds) {
    this.ds = ds;
  }

  @JsonProperty
  public Double getStandardDeviation() {
    return standardDeviation;
  }

  public void setStandardDeviation(Double standardDeviation) {
    this.standardDeviation = standardDeviation;
  }

  @JsonProperty
  public String getOu() {
    return ou;
  }

  public void setOu(String ou) {
    this.ou = ou;
  }

  @Override
  public String toString() {
    return "StdDevOutlierAnalysisParams{"
        + "startDate='"
        + startDate
        + '\''
        + ", endDate='"
        + endDate
        + '\''
        + ", ds="
        + ds
        + ", standardDeviation="
        + standardDeviation
        + ", ou='"
        + ou
        + '\''
        + '}';
  }
}
