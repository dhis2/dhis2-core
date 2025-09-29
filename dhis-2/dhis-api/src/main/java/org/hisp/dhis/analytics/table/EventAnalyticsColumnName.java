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
package org.hisp.dhis.analytics.table;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class EventAnalyticsColumnName {

  public static final String EVENT_COLUMN_NAME = "event";
  public static final String ENROLLMENT_COLUMN_NAME = "enrollment";
  public static final String TRACKED_ENTITY_COLUMN_NAME = "trackedentity";
  public static final String PS_COLUMN_NAME = "ps";
  public static final String AO_COLUMN_NAME = "ao";
  public static final String ENROLLMENT_DATE_COLUMN_NAME = "enrollmentdate";
  public static final String ENROLLMENT_OCCURRED_DATE_COLUMN_NAME = "enrollmentoccurreddate";
  public static final String OCCURRED_DATE_COLUMN_NAME = "occurreddate";
  public static final String SCHEDULED_DATE_COLUMN_NAME = "scheduleddate";
  public static final String COMPLETED_DATE_COLUMN_NAME = "completeddate";
  public static final String CREATED_COLUMN_NAME = "created";
  public static final String CREATED_DATE_COLUMN_NAME = "created";
  public static final String LAST_UPDATED_COLUMN_NAME = "lastupdated";
  public static final String STORED_BY_COLUMN_NAME = "storedby";
  public static final String CREATED_BY_USERNAME_COLUMN_NAME = "createdbyusername";
  public static final String CREATED_BY_NAME_COLUMN_NAME = "createdbyname";
  public static final String CREATED_BY_LASTNAME_COLUMN_NAME = "createdbylastname";
  public static final String CREATED_BY_DISPLAYNAME_COLUMN_NAME = "createdbydisplayname";
  public static final String LAST_UPDATED_BY_USERNAME_COLUMN_NAME = "lastupdatedbyusername";
  public static final String LAST_UPDATED_BY_NAME_COLUMN_NAME = "lastupdatedbyname";
  public static final String LAST_UPDATED_BY_LASTNAME_COLUMN_NAME = "lastupdatedbylastname";
  public static final String LAST_UPDATED_BY_DISPLAYNAME_COLUMN_NAME = "lastupdatedbydisplayname";
  public static final String EVENT_STATUS_COLUMN_NAME = "eventstatus";
  public static final String ENROLLMENT_STATUS_COLUMN_NAME = "enrollmentstatus";
  public static final String EVENT_GEOMETRY_COLUMN_NAME = "eventgeometry";
  public static final String LONGITUDE_COLUMN_NAME = "longitude";
  public static final String LATITUDE_COLUMN_NAME = "latitude";
  public static final String OU_COLUMN_NAME = "ou";
  public static final String OU_NAME_COLUMN_NAME = "ouname";
  public static final String OU_CODE_COLUMN_NAME = "oucode";
  public static final String OU_LEVEL_COLUMN_NAME = "oulevel";
  public static final String OU_GEOMETRY_COLUMN_NAME = "ougeometry";
  public static final String ENROLLMENT_GEOMETRY_COLUMN_NAME = "enrollmentgeometry";
  public static final String REGISTRATION_OU_COLUMN_NAME = "registrationou";
  public static final String ENROLLMENT_OU_COLUMN_NAME = "enrollmentou";
  public static final String TRACKED_ENTITY_GEOMETRY_COLUMN_NAME = "tegeometry";
}
