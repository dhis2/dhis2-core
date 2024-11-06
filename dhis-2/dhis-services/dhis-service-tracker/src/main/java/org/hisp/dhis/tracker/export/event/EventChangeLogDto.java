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
package org.hisp.dhis.tracker.export.event;

import java.util.Date;
import org.hisp.dhis.changelog.ChangeLogType;

public class EventChangeLogDto {
  private final String dataElementUid;
  private final String currentValue;
  private final String previousValue;
  private final ChangeLogType changeLogType;
  private final Date created;
  private final String createdBy;
  private final String firstName;
  private final String surname;
  private final String username;
  private final String userUid;

  public EventChangeLogDto(
      String dataElementUid,
      String currentValue,
      String previousValue,
      ChangeLogType changeLogType,
      Date created,
      String createdBy,
      String firstName,
      String surname,
      String username,
      String userUid) {
    this.dataElementUid = dataElementUid;
    this.currentValue = currentValue;
    this.previousValue = previousValue;
    this.changeLogType = changeLogType;
    this.created = created;
    this.createdBy = createdBy;
    this.firstName = firstName;
    this.surname = surname;
    this.username = username;
    this.userUid = userUid;
  }

  public String getDataElementUid() {
    return dataElementUid;
  }

  public String getCurrentValue() {
    return currentValue;
  }

  public String getPreviousValue() {
    return previousValue;
  }

  public ChangeLogType getChangeLogType() {
    return changeLogType;
  }

  public Date getCreated() {
    return created;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getSurname() {
    return surname;
  }

  public String getUsername() {
    return username;
  }

  public String getUserUid() {
    return userUid;
  }
}
