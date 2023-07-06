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
package org.hisp.dhis.dxf2.events.importer.shared;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.Note;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.event.EventStatus;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Luciano Fiandesio
 */
public class ImmutableEvent {
  private Event event;

  public ImmutableEvent(Event event) {
    this.event = event;
  }

  public String getUid() {
    return event.getUid();
  }

  public String getEvent() {
    return event.getEvent();
  }

  public EnrollmentStatus getEnrollmentStatus() {
    return event.getEnrollmentStatus();
  }

  public EventStatus getStatus() {
    return event.getStatus();
  }

  public String getProgram() {
    return event.getProgram();
  }

  public String getProgramStage() {
    return event.getProgramStage();
  }

  public String getEnrollment() {
    return event.getEnrollment();
  }

  public String getOrgUnit() {
    return event.getOrgUnit();
  }

  public String getOrgUnitName() {
    return event.getOrgUnitName();
  }

  public String getTrackedEntityInstance() {
    return event.getTrackedEntityInstance();
  }

  public String getEventDate() {
    return event.getEventDate();
  }

  public String getDueDate() {
    return event.getDueDate();
  }

  public String getStoredBy() {
    return event.getStoredBy();
  }

  public Set<DataValue> getDataValues() {
    return event.getDataValues();
  }

  public List<Note> getNotes() {
    return event.getNotes();
  }

  public Boolean getFollowup() {
    return event.getFollowup();
  }

  public String getCreated() {
    return event.getCreated();
  }

  public String getLastUpdated() {
    return event.getLastUpdated();
  }

  public String getCreatedAtClient() {
    return event.getCreatedAtClient();
  }

  public String getLastUpdatedAtClient() {
    return event.getLastUpdatedAtClient();
  }

  public String getAttributeOptionCombo() {
    return event.getAttributeOptionCombo();
  }

  public String getAttributeCategoryOptions() {
    return event.getAttributeCategoryOptions();
  }

  public String getCompletedBy() {
    return event.getCompletedBy();
  }

  public String getCompletedDate() {
    return event.getCompletedDate();
  }

  public Boolean isDeleted() {
    return event.isDeleted();
  }

  public int getOptionSize() {
    return event.getOptionSize();
  }

  public Set<Relationship> getRelationships() {
    return event.getRelationships();
  }

  public Geometry getGeometry() {
    return event.getGeometry();
  }

  public String getAssignedUser() {
    return event.getAssignedUser();
  }

  public String getAssignedUserUsername() {
    return event.getAssignedUserUsername();
  }

  @Override
  public boolean equals(Object o) {
    return event.equals(o);
  }

  @Override
  public int hashCode() {
    return event.hashCode();
  }

  @Override
  public String toString() {
    return event.toString();
  }

  public String getHref() {
    return event.getHref();
  }
}
