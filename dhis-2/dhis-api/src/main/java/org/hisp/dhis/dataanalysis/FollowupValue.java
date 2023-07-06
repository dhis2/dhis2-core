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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

/**
 * A {@link FollowupValue} is a read-only set of fields extracted for a {@link
 * org.hisp.dhis.datavalue.DataValue} and references objects as part of the followup-data-analysis.
 *
 * @author Jan Bernitt
 */
@Getter
@AllArgsConstructor
public final class FollowupValue implements Serializable {
  // OBS! The order of fields is important as it becomes the order of the
  // constructor arguments that are mapped to database results.

  @JsonProperty private String de;

  @JsonProperty private String deName;

  @JsonProperty private String peType;

  @JsonProperty private Date peStartDate;

  @JsonProperty private Date peEndDate;

  @Setter @JsonProperty private String peName;

  @JsonProperty private String ou;

  @JsonProperty private String ouName;

  @JsonProperty private String ouPath;

  @JsonProperty private String coc;

  @JsonProperty private String cocName;

  @JsonProperty private String aoc;

  @JsonProperty private String aocName;

  @JsonProperty private String value;

  @JsonProperty private String storedBy;

  @JsonProperty private Date lastUpdated;

  @JsonProperty private Date created;

  @JsonProperty private String comment;

  @JsonProperty private Integer min;

  @JsonProperty private Integer max;

  @JsonProperty
  public String getPe() {
    return peType == null
        ? null
        : PeriodType.getIsoPeriod(PeriodType.getCalendar(), peType, peStartDate);
  }

  @JsonIgnore
  Period getPeAsPeriod() {
    return peType == null ? null : PeriodType.getPeriodFromIsoString(getPe());
  }
}
