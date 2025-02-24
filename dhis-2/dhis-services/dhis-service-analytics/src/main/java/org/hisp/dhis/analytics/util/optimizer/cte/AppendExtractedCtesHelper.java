/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.optimizer.cte;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import org.hisp.dhis.analytics.util.optimizer.cte.data.GeneratedCte;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;

@UtilityClass
public class AppendExtractedCtesHelper {

  /**
   * Appends the list of CTE definitions (given as GeneratedCte records) to the main query's WITH
   * clause, placing the new CTEs before the existing ones.
   *
   * @param select the main query (as a Select AST node)
   * @param generatedCtes the collection of new CTE definitions.
   */
  public static void appendExtractedCtes(Select select, Map<String, GeneratedCte> generatedCtes) {
    List<WithItem> existingWithItems = select.getWithItemsList();
    if (existingWithItems == null) {
      existingWithItems = new ArrayList<>();
      select.setWithItemsList(existingWithItems);
    }
    List<WithItem> newCtes = new ArrayList<>();
    for (GeneratedCte genCte : generatedCtes.values()) {
      try {
        // select 1 is a dummy query to wrap the CTE definition
        String wrappedCte = "with " + genCte.name() + " as (" + genCte.cteString() + ") select 1";
        Statement stmt = CCJSqlParserUtil.parse(wrappedCte);
        if (stmt instanceof Select tempSelect) {
          List<WithItem> tempWithItems = tempSelect.getWithItemsList();
          if (tempWithItems != null && !tempWithItems.isEmpty()) {
            WithItem newCte = tempWithItems.get(0);
            newCtes.add(newCte);
          }
        }
      } catch (Exception e) {
        throw new IllegalQueryException(ErrorCode.E7151, e.getMessage(), e);
      }
    }
    // Prepend the new CTEs.
    existingWithItems.addAll(0, newCtes);
  }
}
