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
package org.hisp.dhis.tracker.acl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import lombok.Data;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@Entity
@Data
@Table(name = "programownershiphistory")
public class ProgramOwnershipHistory {
  @Id
  @GeneratedValue
  @Column(name = "programownershiphistoryid")
  private int id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "programid",
      foreignKey = @ForeignKey(name = "fk_programownershiphistory_programid"),
      nullable = false)
  private Program program;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "startdate")
  private Date startDate;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "enddate")
  private Date endDate;

  @Column(name = "createdby")
  private String createdBy;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "trackedentityid",
      foreignKey = @ForeignKey(name = "fk_programownershiphistory_trackedentityinstanceid"),
      nullable = false)
  private TrackedEntity trackedEntity;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "organisationunitid",
      foreignKey = @ForeignKey(name = "fk_programownershiphistory_organisationunitid"),
      nullable = false)
  private OrganisationUnit organisationUnit;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public ProgramOwnershipHistory() {}

  public ProgramOwnershipHistory(
      Program program,
      TrackedEntity trackedEntity,
      OrganisationUnit organisationUnit,
      Date startDate,
      String createdBy) {
    this.program = program;
    this.startDate = startDate;
    this.createdBy = createdBy;
    this.endDate = new Date();
    this.trackedEntity = trackedEntity;
    this.organisationUnit = organisationUnit;
  }

  public ProgramOwnershipHistory(
      Program program,
      TrackedEntity trackedEntity,
      OrganisationUnit organisationUnit,
      Date startDate,
      Date endDate,
      String createdBy) {
    this.program = program;
    this.startDate = startDate;
    this.createdBy = createdBy;
    this.endDate = endDate;
    this.trackedEntity = trackedEntity;
    this.organisationUnit = organisationUnit;
  }
}
