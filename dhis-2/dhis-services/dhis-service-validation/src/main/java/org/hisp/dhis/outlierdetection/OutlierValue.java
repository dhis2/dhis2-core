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
package org.hisp.dhis.outlierdetection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Date;
import lombok.Data;

/**
 * @author Lars Helge Overland
 */
@Data
@JsonPropertyOrder({
  "de",
  "deName",
  "pe",
  "ou",
  "ouName",
  "coc",
  "cocName",
  "aoc",
  "lastUpdated",
  "value",
  "mean",
  "stdDev",
  "absDev",
  "zScore",
  "lowerBound",
  "upperBound"
})
public class OutlierValue {
  @JsonProperty private String de;

  @JsonProperty private String deName;

  @JsonProperty private String pe;

  @JsonProperty private String ou;

  @JsonProperty private String ouName;

  @JsonProperty private String coc;

  @JsonProperty private String cocName;

  @JsonProperty private String aoc;

  @JsonProperty private String aocName;

  @JsonProperty private Date lastUpdated;

  @JsonProperty private Double value;

  @JsonProperty private Double mean;

  @JsonProperty private Double median;

  @JsonProperty private Double stdDev;

  @JsonProperty private Double absDev;

  @JsonProperty private Double zScore;

  @JsonProperty private Double lowerBound;

  @JsonProperty private Double upperBound;

  @JsonProperty private Boolean followup;
}
