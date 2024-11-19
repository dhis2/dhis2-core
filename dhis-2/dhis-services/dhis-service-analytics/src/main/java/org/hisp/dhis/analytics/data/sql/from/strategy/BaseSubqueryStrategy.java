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
package org.hisp.dhis.analytics.data.sql.from.strategy;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.table.util.PartitionUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.db.sql.SqlBuilder;

/** Base class for subquery strategies. */
@RequiredArgsConstructor
public abstract class BaseSubqueryStrategy implements SubqueryStrategy {

  protected final DataQueryParams params;
  protected final SqlBuilder sqlBuilder;

  protected String getTablePartitionOrTableName() {
    if (!params.isSkipPartitioning() && params.hasPartitions()) {
      return params.getPartitions().hasOne()
          ? PartitionUtils.getPartitionName(params.getTableName(), params.getPartitions().getAny())
          : getUnionPartitionSourceClause();
    }
    return params.getTableName();
  }

  private String getUnionPartitionSourceClause() {
    StringBuilder sql = new StringBuilder("(");
    for (Integer partition : params.getPartitions().getPartitions()) {
      String partitionName = PartitionUtils.getPartitionName(params.getTableName(), partition);
      sql.append("select ap.* from ").append(partitionName).append(" as ap union all ");
    }
    return TextUtils.removeLast(sql.toString(), "union all") + ")";
  }
}
