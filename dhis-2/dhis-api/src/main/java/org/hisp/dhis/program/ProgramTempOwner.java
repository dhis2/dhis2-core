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
package org.hisp.dhis.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@JacksonXmlRootElement(localName = "programTempOwner", namespace = DxfNamespaces.DXF_2_0)
public class ProgramTempOwner implements Serializable {

  /** */
  private static final long serialVersionUID = -2030234810482111257L;

  private long id;

  private Program program;

  private String reason;

  private Date validTill;

  private User user;

  private TrackedEntityInstance entityInstance;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public ProgramTempOwner() {}

  public ProgramTempOwner(
      Program program,
      TrackedEntityInstance entityInstance,
      String reason,
      User user,
      int hoursToExpire) {
    this.program = program;
    this.reason = reason;
    this.user = user;
    this.validTill = addHoursToJavaUtilDate(new Date(), hoursToExpire);
    this.entityInstance = entityInstance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(program, entityInstance, reason, validTill, user);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ProgramTempOwner other = (ProgramTempOwner) obj;

    return Objects.equals(this.program, other.program)
        && Objects.equals(this.reason, other.reason)
        && Objects.equals(this.validTill, other.validTill)
        && Objects.equals(this.user, other.user)
        && Objects.equals(this.entityInstance, other.entityInstance);
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  public TrackedEntityInstance getEntityInstance() {
    return entityInstance;
  }

  public void setEntityInstance(TrackedEntityInstance entityInstance) {
    this.entityInstance = entityInstance;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Date getValidTill() {
    return validTill;
  }

  public void setValidTill(Date accessedAt) {
    this.validTill = accessedAt;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Date addHoursToJavaUtilDate(Date date, int hours) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.HOUR_OF_DAY, hours);
    return calendar.getTime();
  }
}
