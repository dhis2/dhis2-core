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
package org.hisp.dhis.analytics.event.data.stage;

import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.ColumnAndAlias;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.program.AnalyticsType;

/**
 * Builds stage-scoped org unit SQL fragments for selection and filtering.
 *
 * <p>This encapsulates {@code stage.ou} behavior and keeps org unit level handling out of manager
 * code.
 */
public interface StageOrgUnitSqlService {
  /**
   * Creates the SELECT/GROUP BY column expression for a stage org unit item.
   *
   * @param item the stage org unit query item
   * @param params query parameters
   * @param isGroupByClause true when the expression is for GROUP BY
   * @return stage org unit SQL column and alias information
   */
  ColumnAndAlias selectColumn(QueryItem item, EventQueryParams params, boolean isGroupByClause);

  /**
   * Creates the WHERE clause fragment for a stage org unit item.
   *
   * @param item the stage org unit query item
   * @param params query parameters
   * @param analyticsType analytics type used for org unit level columns
   * @return SQL fragment for stage org unit filtering
   */
  String whereClause(QueryItem item, EventQueryParams params, AnalyticsType analyticsType);
}
