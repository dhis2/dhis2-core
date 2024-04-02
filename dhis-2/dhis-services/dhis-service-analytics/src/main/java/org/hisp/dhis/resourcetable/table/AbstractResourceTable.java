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
package org.hisp.dhis.resourcetable.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.resourcetable.ResourceTable;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public abstract class AbstractResourceTable implements ResourceTable {
  protected final SqlBuilder sqlBuilder;

  protected final Logged logged;

  /**
   * @param relation the relation to quote, e.g. a table or column name.
   * @return a double quoted relation.
   */
  protected String quote(String relation) {
    return sqlBuilder.quote(relation);
  }

  /**
   * @param name the table name.
   * @return a fully qualified and quoted table reference which specifies the catalog, database and
   *     table.
   */
  protected String qualify(String name) {
    return sqlBuilder.qualifyTable(name);
  }

  /**
   * Replaces variables in the given template string with the given variables to qualify and the
   * given map of variable keys and values.
   *
   * @param template the template string.
   * @param qualifyVariables the list of variables to qualify.
   * @param variables the map of variables and values.
   * @return a resolved string.
   */
  protected String replaceQualify(
      String template, List<String> qualifyVariables, Map<String, String> variables) {
    Map<String, String> map = new HashMap<>(variables);
    qualifyVariables.forEach(v -> map.put(v, qualify(v)));
    return TextUtils.replace(template, map);
  }
}
