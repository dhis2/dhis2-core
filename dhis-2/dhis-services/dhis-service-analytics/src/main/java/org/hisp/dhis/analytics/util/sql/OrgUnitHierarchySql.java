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
package org.hisp.dhis.analytics.util.sql;

import java.util.function.IntFunction;
import lombok.experimental.UtilityClass;

/** SQL fragments for navigating the organisation unit hierarchy columns of analytics tables. */
@UtilityClass
public class OrgUnitHierarchySql {

  /**
   * Returns a SQL expression resolving, for each analytics row, the UID of the parent organisation
   * unit of the row's own organisation unit. Grouping by this expression puts sibling organisation
   * units into the same group, which lets MIN or MAX be taken across the children of an
   * organisation unit before the resulting values are summed up the hierarchy.
   *
   * <p>The expression walks the {@code uidlevelN} columns from the deepest level upwards and yields
   * the level above the one matching the row's own organisation unit. Rows whose organisation unit
   * sits at level one have no parent and resolve to the organisation unit itself.
   *
   * @param maxLevel the deepest organisation unit level having a {@code uidlevelN} column.
   * @param ouColumn the qualified and quoted column holding the row's organisation unit UID.
   * @param levelColumn maps an organisation unit level to its qualified {@code uidlevelN} column.
   * @return a SQL {@code case} expression, or {@code ouColumn} when fewer than two levels exist.
   */
  public String getParentOrgUnitExpression(
      int maxLevel, String ouColumn, IntFunction<String> levelColumn) {
    if (maxLevel < 2) {
      return ouColumn;
    }

    StringBuilder sql = new StringBuilder("case");

    for (int level = maxLevel; level > 1; level--) {
      sql.append(" when ")
          .append(levelColumn.apply(level))
          .append(" = ")
          .append(ouColumn)
          .append(" then ")
          .append(levelColumn.apply(level - 1));
    }

    return sql.append(" else ").append(ouColumn).append(" end").toString();
  }
}
