/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.event.data.ou;

import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.AnalyticsConstants.ORG_UNIT_STRUCT_ALIAS;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.common.ColumnHeader;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;

/** Shared SQL identifiers for ENROLLMENT_OU query and aggregate handling. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrgUnitSqlConstants {

  public static final String ORG_UNIT_STRUCTURE_TABLE = "analytics_rs_orgunitstructure";
  public static final String EVENT_TABLE_ALIAS = ANALYTICS_TBL_ALIAS;
  public static final String ORG_UNIT_STRUCTURE_ALIAS = ORG_UNIT_STRUCT_ALIAS;
  public static final String EVENT_ENROLLMENT_OU_COLUMN =
      EventAnalyticsColumnName.ENROLLMENT_OU_COLUMN_NAME;
  public static final String ORG_UNIT_UID_COLUMN = "organisationunituid";
  public static final String ORG_UNIT_NAME_COLUMN = "name";
  public static final String ORG_UNIT_LEVEL_COLUMN = "level";
  public static final String ENROLLMENT_OU_RESULT_ALIAS = ColumnHeader.ENROLLMENT_OU.getItem();
  public static final String ENROLLMENT_OU_NAME_RESULT_ALIAS =
      ColumnHeader.ENROLLMENT_OU_NAME.getItem();
}
