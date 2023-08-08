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
package org.hisp.dhis.dataapproval;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Date;
import lombok.Setter;
import org.hisp.dhis.common.DxfNamespaces;

@Setter
@JacksonXmlRootElement(localName = "dataApprovalPermissions", namespace = DxfNamespaces.DXF_2_0)
public class DataApprovalPermissions {
  private boolean mayApprove;

  private boolean mayUnapprove;

  private boolean mayAccept;

  private boolean mayUnaccept;

  private boolean mayReadData;

  private boolean mayReadAcceptedBy;

  private String state;

  private String approvedBy;

  private Date approvedAt;

  private String acceptedBy;

  private Date acceptedAt;

  @JsonProperty
  public boolean isMayApprove() {
    return mayApprove;
  }

  @JsonProperty
  public boolean isMayUnapprove() {
    return mayUnapprove;
  }

  @JsonProperty
  public boolean isMayAccept() {
    return mayAccept;
  }

  @JsonProperty
  public boolean isMayUnaccept() {
    return mayUnaccept;
  }

  @JsonProperty
  public boolean isMayReadData() {
    return mayReadData;
  }

  /**
   * OBS! Note that this field is intentionally not serialised to JSON as the user should not know
   * if he can or can't read.
   *
   * @return whether the user can read who accepted the DA
   */
  public boolean isMayReadAcceptedBy() {
    return mayReadAcceptedBy;
  }

  @JsonProperty
  public String getState() {
    return state;
  }

  @JsonProperty
  public String getApprovedBy() {
    return approvedBy;
  }

  @JsonProperty
  public Date getApprovedAt() {
    return approvedAt;
  }

  @JsonProperty
  public String getAcceptedBy() {
    return acceptedBy;
  }

  @JsonProperty
  public Date getAcceptedAt() {
    return acceptedAt;
  }

  // ----------------------------------------------------------------------
  // toString
  // ----------------------------------------------------------------------

  @Override
  public String toString() {
    return "DataApprovalPermissions{"
        + "mayApprove="
        + mayApprove
        + ", mayUnapprove="
        + mayUnapprove
        + ", mayAccept="
        + mayAccept
        + ", mayUnaccept="
        + mayUnaccept
        + ", mayReadData="
        + mayReadData
        + '}';
  }
}
