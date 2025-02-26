/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export.trackedentity;

import java.util.Date;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

@NoArgsConstructor
@Getter
@Setter
public class TrackedEntityChangeLog {
  private long id;

  private TrackedEntity trackedEntity;

  private TrackedEntityAttribute trackedEntityAttribute;

  private String previousValue;

  private String currentValue;

  private ChangeLogType changeLogType;

  private Date created;

  private String createdByUsername;

  private UserInfoSnapshot createdBy;

  @Getter(AccessLevel.NONE)
  private ProgramTrackedEntityAttribute programAttribute;

  public TrackedEntityChangeLog(
      TrackedEntity trackedEntity,
      TrackedEntityAttribute trackedEntityAttribute,
      String previousValue,
      String currentValue,
      ChangeLogType changeLogType,
      Date created,
      String createdByUsername) {
    this(
        trackedEntity, trackedEntityAttribute, previousValue, currentValue, changeLogType, created);
    this.createdByUsername = createdByUsername;
  }

  public TrackedEntityChangeLog(
      TrackedEntity trackedEntity,
      TrackedEntityAttribute trackedEntityAttribute,
      String previousValue,
      String currentValue,
      ChangeLogType changeLogType,
      Date created,
      UserInfoSnapshot createdBy) {
    this(
        trackedEntity, trackedEntityAttribute, previousValue, currentValue, changeLogType, created);
    this.createdBy = createdBy;
  }

  private TrackedEntityChangeLog(
      TrackedEntity trackedEntity,
      TrackedEntityAttribute trackedEntityAttribute,
      String previousValue,
      String currentValue,
      ChangeLogType changeLogType,
      Date created) {
    this.trackedEntity = trackedEntity;
    this.trackedEntityAttribute = trackedEntityAttribute;
    this.previousValue = previousValue;
    this.currentValue = currentValue;
    this.changeLogType = changeLogType;
    this.created = created;
  }
}
