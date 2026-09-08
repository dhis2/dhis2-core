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
package org.hisp.dhis.tracker.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.common.EmbeddedObject;

/**
 * @author Stian Sandvold
 */
@Entity
@Table(name = "relationshipitem")
@Setter
@Getter
@NoArgsConstructor
public class RelationshipItem implements EmbeddedObject, Serializable {

  @Id
  @Column(name = "relationshipitemid")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "relationshipitem_sequence")
  @SequenceGenerator(
      name = "relationshipitem_sequence",
      sequenceName = "relationshipitem_sequence",
      allocationSize = 1)
  private int id;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @JoinColumn(
      name = "relationshipid",
      foreignKey = @ForeignKey(name = "fk_relationshipitem_relationshipid"))
  private Relationship relationship;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "trackedentityid",
      foreignKey = @ForeignKey(name = "fk_relationshipitem_trackedentityinstanceid"))
  private TrackedEntity trackedEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "enrollmentid",
      foreignKey = @ForeignKey(name = "fk_relationshipitem_programinstanceid"))
  private Enrollment enrollment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "trackereventid",
      foreignKey = @ForeignKey(name = "fk_relationshipitem_trackereventid"))
  private TrackerEvent trackerEvent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "singleeventid",
      foreignKey = @ForeignKey(name = "fk_relationshipitem_singleeventid"))
  private SingleEvent singleEvent;
}
