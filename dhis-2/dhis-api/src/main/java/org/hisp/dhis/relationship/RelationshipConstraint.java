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
package org.hisp.dhis.relationship;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackerdataview.TrackerDataView;

/**
 * @author Stian Sandvold
 */
@JacksonXmlRootElement(localName = "relationshipConstraint", namespace = DxfNamespaces.DXF_2_0)
public class RelationshipConstraint implements EmbeddedObject {
  private int id;

  private RelationshipEntity relationshipEntity;

  private TrackedEntityType trackedEntityType;

  private Program program;

  private ProgramStage programStage;

  private TrackerDataView trackerDataView;

  public RelationshipConstraint() {}

  @JsonIgnore
  public void setId(int id) {
    this.id = id;
  }

  public int getId() {
    return this.id;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public RelationshipEntity getRelationshipEntity() {
    return relationshipEntity;
  }

  public void setRelationshipEntity(RelationshipEntity relationshipEntity) {
    this.relationshipEntity = relationshipEntity;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public TrackedEntityType getTrackedEntityType() {
    return trackedEntityType;
  }

  public void setTrackedEntityType(TrackedEntityType trackedEntityType) {
    this.trackedEntityType = trackedEntityType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  @JsonProperty()
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramStage getProgramStage() {
    return programStage;
  }

  public void setProgramStage(ProgramStage programStage) {
    this.programStage = programStage;
  }

  @JsonProperty()
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public TrackerDataView getTrackerDataView() {
    return trackerDataView;
  }

  public void setTrackerDataView(TrackerDataView trackerDataView) {
    this.trackerDataView = trackerDataView;
  }

  @Override
  public String toString() {
    return "RelationshipConstraint{"
        + "id="
        + id
        + ", relationshipEntity="
        + relationshipEntity
        + ", trackedEntityType="
        + trackedEntityType
        + ", program="
        + program
        + ", programStage="
        + programStage
        + '}';
  }
}
