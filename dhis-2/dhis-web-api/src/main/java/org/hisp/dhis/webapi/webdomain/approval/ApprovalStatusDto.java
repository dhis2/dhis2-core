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
package org.hisp.dhis.webapi.webdomain.approval;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalPermissions;
import org.hisp.dhis.dataapproval.DataApprovalState;
import org.hisp.dhis.dataapproval.DataApprovalStatus;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JacksonXmlRootElement(localName = "approvalStatus", namespace = DxfNamespaces.DXF_2_0)
public class ApprovalStatusDto {
  @JsonProperty
  @OpenApi.Property({UID.class, DataApprovalWorkflow.class})
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  private final String wf;

  @JsonProperty
  @OpenApi.Property(Period.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  private final String pe;

  @JsonProperty
  @OpenApi.Property({UID.class, OrganisationUnit.class})
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  private final String ou;

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  private final String ouName;

  @JsonProperty
  @OpenApi.Property({UID.class, CategoryOptionCombo.class})
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  private final String aoc;

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  private final DataApprovalState state;

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  private final String level;

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  private final DataApprovalPermissions permissions;

  public static ApprovalStatusDto from(Map.Entry<DataApproval, DataApprovalStatus> entry) {
    return from(entry.getKey(), entry.getValue());
  }

  public static ApprovalStatusDto from(DataApproval approval, DataApprovalStatus status) {
    String level =
        status != null && status.getApprovedLevel() != null
            ? status.getApprovedLevel().getUid()
            : null;
    return builder()
        .wf(uid(approval.getWorkflow()))
        .pe(approval.getPeriod().getIsoDate())
        .ou(uid(approval.getOrganisationUnit()))
        .aoc(uid(approval.getAttributeOptionCombo()))
        .state(status != null ? status.getState() : null)
        .level(level)
        .permissions(status != null ? status.getPermissions() : null)
        .build();
  }

  private static String uid(IdentifiableObject object) {
    return object == null ? null : object.getUid();
  }
}
