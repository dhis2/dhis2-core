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
package org.hisp.dhis.programstagefilter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.Date;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.period.RelativePeriodEnum;

/**
 * Filtering parameters for date type.
 *
 * @author Ameen Mohamed <ameen@dhis2.com>
 */
public class DateFilterPeriod implements Serializable {
  private static final long serialVersionUID = 1L;

  /** An integer referring to relative startDate based on the current date. */
  private int startBuffer;

  /** An integer referring to relative startDate based on the current date. */
  private int endBuffer;

  /** An absolute start date */
  private Date startDate;

  /** An absolute end date */
  private Date endDate;

  /** Relative period. */
  private RelativePeriodEnum period;

  /** Enum indicating whether this date filter is absolute or relative */
  private DatePeriodType type;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DateFilterPeriod() {}

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getEndBuffer() {
    return endBuffer;
  }

  public void setEndBuffer(int to) {
    this.endBuffer = to;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public RelativePeriodEnum getPeriod() {
    return period;
  }

  public void setPeriod(RelativePeriodEnum relativePeriod) {
    this.period = relativePeriod;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getStartBuffer() {
    return startBuffer;
  }

  public void setStartBuffer(int startBuffer) {
    this.startBuffer = startBuffer;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DatePeriodType getType() {
    return type;
  }

  public void setType(DatePeriodType type) {
    this.type = type;
  }
}
