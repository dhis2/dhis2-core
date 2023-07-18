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
package org.hisp.dhis.trackedentity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.Date;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;

/**
 * @author Ameen Mohamed
 */
@JacksonXmlRootElement(localName = "trackedEntityProgramOwner", namespace = DxfNamespaces.DXF_2_0)
public class TrackedEntityProgramOwner implements Serializable {
  private int id;

  private TrackedEntityInstance entityInstance;

  private Program program;

  private OrganisationUnit organisationUnit;

  private Date created;

  private Date lastUpdated;

  private String createdBy;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public TrackedEntityProgramOwner() {
    this.createdBy = "internal";
  }

  public TrackedEntityProgramOwner(
      TrackedEntityInstance trackedEntityInstance,
      Program program,
      OrganisationUnit organisationUnit) {
    this.entityInstance = trackedEntityInstance;
    this.program = program;
    this.organisationUnit = organisationUnit;
    this.createdBy = "internal";
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void changeOwner(OrganisationUnit newOwnerOu) {
    this.organisationUnit = newOwnerOu;
  }

  // -------------------------------------------------------------------------
  // equals and hashCode
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();

    result = prime * result + ((entityInstance == null) ? 0 : entityInstance.hashCode());
    result = prime * result + ((program == null) ? 0 : program.hashCode());

    return result;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null) {
      return false;
    }

    if (!getClass().isAssignableFrom(object.getClass())) {
      return false;
    }

    final TrackedEntityProgramOwner other = (TrackedEntityProgramOwner) object;

    if (entityInstance == null) {
      if (other.entityInstance != null) {
        return false;
      }
    } else if (!entityInstance.equals(other.entityInstance)) {
      return false;
    }

    if (program == null) {
      if (other.program != null) {
        return false;
      }
    } else if (!program.equals(other.program)) {
      return false;
    }

    return true;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OrganisationUnit getOrganisationUnit() {
    return organisationUnit;
  }

  public void setOrganisationUnit(OrganisationUnit organisationUnit) {
    this.organisationUnit = organisationUnit;
  }

  @JsonProperty("trackedEntityInstance")
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(localName = "trackedEntityInstance", namespace = DxfNamespaces.DXF_2_0)
  public TrackedEntityInstance getEntityInstance() {
    return entityInstance;
  }

  public void setEntityInstance(TrackedEntityInstance trackedEntityInstance) {
    this.entityInstance = trackedEntityInstance;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void updateDates() {
    Date now = new Date();
    if (this.created == null) {
      this.created = now;
    }
    this.lastUpdated = now;
  }
}
