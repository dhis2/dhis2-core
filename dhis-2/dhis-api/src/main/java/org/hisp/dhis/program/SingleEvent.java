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
package org.hisp.dhis.program;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.audit.AuditAttribute;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.Auditable;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.user.User;
import org.locationtech.jts.geom.Geometry;

@Auditable(scope = AuditScope.TRACKER)
@Setter
@Getter
@NoArgsConstructor
public class SingleEvent extends SoftDeletableObject {
  private Date createdAtClient;

  private Date lastUpdatedAtClient;

  @AuditAttribute private Enrollment enrollment;

  @AuditAttribute private ProgramStage programStage;

  private String storedBy;

  private UserInfoSnapshot createdByUserInfo;

  private UserInfoSnapshot lastUpdatedByUserInfo;

  private Date occurredDate;

  @AuditAttribute private OrganisationUnit organisationUnit;

  @AuditAttribute private CategoryOptionCombo attributeOptionCombo;

  private List<Note> notes = new ArrayList<>();

  @AuditAttribute private Set<EventDataValue> eventDataValues = new HashSet<>();

  private Set<RelationshipItem> relationshipItems = new HashSet<>();

  @AuditAttribute private EventStatus status = EventStatus.ACTIVE;

  private String completedBy;

  private Date completedDate;

  private Date lastSynchronized = new Date(0);

  private Geometry geometry;

  private User assignedUser;

  @Override
  public void setAutoFields() {
    super.setAutoFields();

    if (createdAtClient == null) {
      createdAtClient = created;
    }

    lastUpdatedAtClient = lastUpdated;
  }

  public boolean hasAttributeOptionCombo() {
    return attributeOptionCombo != null;
  }
}
