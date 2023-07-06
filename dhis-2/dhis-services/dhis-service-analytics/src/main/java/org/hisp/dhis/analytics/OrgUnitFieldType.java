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
package org.hisp.dhis.analytics;

import static org.hisp.dhis.analytics.DataQueryParams.DEFAULT_ORG_UNIT_COL;
import static org.hisp.dhis.analytics.DataQueryParams.ENROLLMENT_OU_COL;
import static org.hisp.dhis.analytics.DataQueryParams.REGISTRATION_OU_COL;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The type of organisation unit field to use for an event (or enrollment) analytics query.
 *
 * @author Jim Grace
 */
@Getter
@AllArgsConstructor
public enum OrgUnitFieldType {
  DEFAULT(DEFAULT_ORG_UNIT_COL, DEFAULT_ORG_UNIT_COL),
  ATTRIBUTE(null, null),
  REGISTRATION(REGISTRATION_OU_COL, REGISTRATION_OU_COL),
  ENROLLMENT(ENROLLMENT_OU_COL, DEFAULT_ORG_UNIT_COL),
  OWNER_AT_START(ENROLLMENT_OU_COL, DEFAULT_ORG_UNIT_COL),
  OWNER_AT_END(ENROLLMENT_OU_COL, DEFAULT_ORG_UNIT_COL);

  /**
   * The event analytics column name containing the organisation unit UID of interest (or the backup
   * column name if an ownership type).
   */
  private final String eventColumn;

  /**
   * The enrollment analytics column name containing the organisation unit UID of interest (or the
   * backup column name if an ownership type).
   */
  private final String enrollmentColumn;

  /** Returns true if this is an ownership type. */
  public boolean isOwnership() {
    return this == OWNER_AT_START || this == OWNER_AT_END;
  }
}
