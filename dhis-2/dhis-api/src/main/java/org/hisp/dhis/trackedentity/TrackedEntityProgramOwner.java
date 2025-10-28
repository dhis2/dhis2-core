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
package org.hisp.dhis.trackedentity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;

/**
 * @author Ameen Mohamed
 */
@JacksonXmlRootElement(localName = "trackedEntityProgramOwner", namespace = DxfNamespaces.DXF_2_0)
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TrackedEntityProgramOwner implements Serializable {

  @Getter private int id;
  @EqualsAndHashCode.Include private TrackedEntity trackedEntity;
  @EqualsAndHashCode.Include private Program program;
  private OrganisationUnit organisationUnit;
  @Getter private Date created;
  @Getter private Date lastUpdated;
  @Getter private String createdBy;

  public TrackedEntityProgramOwner() {
    this.createdBy = "internal";
  }

  public TrackedEntityProgramOwner(
      TrackedEntity trackedEntity, Program program, OrganisationUnit organisationUnit) {
    this.trackedEntity = trackedEntity;
    this.program = program;
    this.organisationUnit = organisationUnit;
    this.createdBy = "internal";
  }

  public void changeOwner(OrganisationUnit newOwnerOu) {
    this.organisationUnit = newOwnerOu;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OrganisationUnit getOrganisationUnit() {
    return organisationUnit;
  }

  @JsonProperty("trackedEntityInstance")
  @JsonSerialize(as = IdentifiableObject.class)
  @JacksonXmlProperty(localName = "trackedEntityInstance", namespace = DxfNamespaces.DXF_2_0)
  public TrackedEntity getTrackedEntity() {
    return trackedEntity;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Program getProgram() {
    return program;
  }

  public void updateDates() {
    Date now = new Date();
    if (this.created == null) {
      this.created = now;
    }
    this.lastUpdated = now;
  }
}
