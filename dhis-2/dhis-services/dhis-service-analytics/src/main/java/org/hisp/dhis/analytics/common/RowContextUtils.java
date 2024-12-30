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

import org.hisp.dhis.analytics.common.CTEContext.CteDefinitionWithOffset;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.db.sql.SqlBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RowContextUtils {

    /**
     * Get where clauses for row context items
     * @param cteContext CTE context
     * @param params Event query parameters
     * @param sqlBuilder SQL builder
     * @return List of where clauses
     */
    public static List<String> getRowContextWhereClauses(CTEContext cteContext, EventQueryParams params, SqlBuilder sqlBuilder) {
        List<String> whereClauses = new ArrayList<>();
        Set<String> ctxNames = cteContext.getCTENames();

        List<String> filters = getItemsWithFilters(params);

        for (String ctxName : ctxNames) {
            CteDefinitionWithOffset cteDef = cteContext.getDefinitionByItemUid(ctxName);
                // only add where clause for row context items with filters
                if (cteDef.isRowContext() && filters.contains(cteDef.getItemId())) {
                    whereClauses.add("%s.%s IS NULL".formatted(cteDef.getAlias(),
                            sqlBuilder.quote(cteDef.getItemId())));
                }
                // only add where clause for "exists" row context items with filters
                if (cteDef.isExists() && filters.contains(cteDef.getItemId())) {
                    whereClauses.add("ee.enrollment IS NOT NULL");
                }
        }

        return whereClauses;
    }

    private static List<String> getItemsWithFilters(EventQueryParams params) {
        return params.getItems().stream()
                .filter(QueryItem::hasFilter)
                .map(QueryItem::getItemId)
                .toList();
    }
}
