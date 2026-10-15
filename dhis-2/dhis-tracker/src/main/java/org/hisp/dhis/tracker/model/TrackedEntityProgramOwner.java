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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;

/**
 * @author Ameen Mohamed
 */
@Entity
@Table(
    name = "trackedentityprogramowner",
    uniqueConstraints =
        @UniqueConstraint(
            name = "trackedentityprogramowner_trackedentityid_programid_key",
            columnNames = {"trackedentityid", "programid"}))
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TrackedEntityProgramOwner implements Serializable {

  @Id
  @Column(name = "trackedentityprogramownerid")
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "trackedentityprogramowner_sequence")
  @SequenceGenerator(
      name = "trackedentityprogramowner_sequence",
      sequenceName = "trackedentityprogramowner_sequence",
      allocationSize = 1)
  @Getter
  private int id;

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "trackedentityid",
      foreignKey = @ForeignKey(name = "fk_trackedentityprogramowner_trackedentityinstanceid"))
  private TrackedEntity trackedEntity;

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "programid",
      foreignKey = @ForeignKey(name = "fk_trackedentityprogramowner_programid"),
      nullable = false)
  private Program program;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "organisationunitid",
      foreignKey = @ForeignKey(name = "fk_trackedentityprogramowner_organisationunitid"))
  private OrganisationUnit organisationUnit;

  @Column(name = "created", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @Getter
  private Date created;

  @Column(name = "lastUpdated", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @Getter
  private Date lastUpdated;

  @Column(name = "createdBy", nullable = false)
  @Getter
  private String createdBy;

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
  public OrganisationUnit getOrganisationUnit() {
    return organisationUnit;
  }

  @JsonProperty("trackedEntityInstance")
  @JsonSerialize(as = IdentifiableObject.class)
  public TrackedEntity getTrackedEntity() {
    return trackedEntity;
  }

  @JsonProperty
  @JsonSerialize(as = IdentifiableObject.class)
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
