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
package org.hisp.dhis.analytics.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.db.sql.SqlBuilder;

public class RowContextUtils {

  public static List<String> getRowContextColumns(CTEContext cteContext, SqlBuilder sqlBuilder) {
    List<String> columns = new ArrayList<>();
    Map<String, String> rowCtxRefs = cteContext.getRowContextReferences();
    for (String aliases : rowCtxRefs.keySet()) {
      columns.add(getStatusColumn(aliases, rowCtxRefs.get(aliases), sqlBuilder));
      columns.add(getExistsColumn(aliases, rowCtxRefs.get(aliases), sqlBuilder));
    }

    return columns;
  }

  public static List<String> getRowContextWhereClauses(CTEContext cteContext) {
    List<String> whereClauses = new ArrayList<>();
    Map<String, String> rowCtxRefs = cteContext.getRowContextReferences();
    for (String alias : rowCtxRefs.values()) {
      whereClauses.add("%s.value is null".formatted(alias));
      // whereClauses.add("%s.exists_flag = true".formatted(alias));
    }
    return whereClauses;
  }

  private static String getExistsColumn(
      String aliases, String cteReference, SqlBuilder sqlBuilder) {
    return "coalesce(%s.exists_flag, false) as %s"
        .formatted(cteReference, sqlBuilder.quote(aliases + ".status.exists"));
  }

  private static String getStatusColumn(String alias, String cteReference, SqlBuilder sqlBuilder) {
    return "%s_status.status as %s".formatted(cteReference, sqlBuilder.quote(alias));
  }
}
