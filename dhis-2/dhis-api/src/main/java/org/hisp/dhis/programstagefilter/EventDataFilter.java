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
import java.util.Set;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * Filter parameters to be used for filtering data element values.
 *
 * @author Ameen Mohamed <ameen@dhis2.com>
 */
public class EventDataFilter implements Serializable {
  private static final long serialVersionUID = 1L;

  /** The data element id or data item */
  private String dataItem;

  /** Less than or equal to */
  private String le;

  /** Greater than or equal to */
  private String ge;

  /** Greater than */
  private String gt;

  /** Lesser than */
  private String lt;

  /** Equal to */
  private String eq;

  /** In a list */
  private Set<String> in;

  /** Like */
  private String like;

  /** If the dataItem is of type date, then date filtering parameters are specified using this. */
  private DateFilterPeriod dateFilter;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public EventDataFilter() {}

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDataItem() {
    return dataItem;
  }

  public void setDataItem(String data) {
    this.dataItem = data;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getLe() {
    return le;
  }

  public void setLe(String le) {
    this.le = le;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getGe() {
    return ge;
  }

  public void setGe(String ge) {
    this.ge = ge;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getGt() {
    return gt;
  }

  public void setGt(String gt) {
    this.gt = gt;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getLt() {
    return lt;
  }

  public void setLt(String lt) {
    this.lt = lt;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getEq() {
    return eq;
  }

  public void setEq(String eq) {
    this.eq = eq;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Set<String> getIn() {
    return in;
  }

  public void setIn(Set<String> in) {
    this.in = in;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getLike() {
    return like;
  }

  public void setLike(String like) {
    this.like = like;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DateFilterPeriod getDateFilter() {
    return dateFilter;
  }

  public void setDateFilter(DateFilterPeriod dateFilter) {
    this.dateFilter = dateFilter;
  }
}
