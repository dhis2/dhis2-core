/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export.trackedentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.changelog.ChangeLogType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.model.TrackedEntity;

@Entity
@Table(name = "trackedentitychangelog")
@NoArgsConstructor
@Getter
@Setter
public class TrackedEntityChangeLog {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(sequenceName = "trackedentitychangelog_sequence")
  @Column(name = "trackedentitychangelogid")
  private long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "trackedentityid",
      foreignKey = @ForeignKey(name = "fk_trackedentitychangelog_trackedentityid"),
      nullable = false)
  private TrackedEntity trackedEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "trackedentityattributeid",
      foreignKey = @ForeignKey(name = "fk_trackedentitychangelog_trackedentityattributeid"),
      nullable = false)
  private TrackedEntityAttribute trackedEntityAttribute;

  @Column(name = "previousvalue", length = 50000)
  private String previousValue;

  @Column(name = "currentvalue", length = 50000)
  private String currentValue;

  @Enumerated(EnumType.STRING)
  @Column(name = "changelogtype", length = 100, nullable = false)
  private ChangeLogType changeLogType;

  @Column(name = "created", nullable = false)
  private Date created;

  @Column(name = "createdby", nullable = false)
  private String createdByUsername;

  @Transient private UserInfoSnapshot createdBy;

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
