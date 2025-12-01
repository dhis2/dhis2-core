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
import java.util.Calendar;
import java.util.Date;
import lombok.Data;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.user.User;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@Entity
@Data
@Table(name = "programtempowner")
public class ProgramTempOwner {
  @Id
  @GeneratedValue
  @Column(name = "programtempownerid")
  private long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "programid",
      foreignKey = @ForeignKey(name = "fk_programtempowner_programid"),
      nullable = false)
  private Program program;

  @Column(length = 50000)
  private String reason;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "validtill")
  private Date validTill;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "userid",
      foreignKey = @ForeignKey(name = "fk_programtempowner_userid"),
      nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "trackedentityid",
      foreignKey = @ForeignKey(name = "fk_programtempowner_trackedentityinstanceid"),
      nullable = false)
  private TrackedEntity trackedEntity;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public ProgramTempOwner() {}

  public ProgramTempOwner(
      Program program, TrackedEntity trackedEntity, String reason, User user, int hoursToExpire) {
    this.program = program;
    this.reason = reason;
    this.user = user;
    this.validTill = addHoursToJavaUtilDate(new Date(), hoursToExpire);
    this.trackedEntity = trackedEntity;
  }

  private Date addHoursToJavaUtilDate(Date date, int hours) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.HOUR_OF_DAY, hours);
    return calendar.getTime();
  }
}
