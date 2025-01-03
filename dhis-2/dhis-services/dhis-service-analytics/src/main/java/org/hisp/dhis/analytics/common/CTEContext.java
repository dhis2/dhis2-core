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

import static org.hisp.dhis.analytics.common.CTEUtils.computeKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;

public class CTEContext {
  private final Map<String, CteDefinition> cteDefinitions = new LinkedHashMap<>();

  public CteDefinition getDefinitionByItemUid(String itemUid) {
    return cteDefinitions.get(itemUid);
  }

  /**
   * Adds a CTE definition to the context.
   *
   * @param programStage The program stage
   * @param item The query item
   * @param cteDefinition The CTE definition (the SQL query)
   * @param offset The calculated offset
   * @param isRowContext Whether the CTE is a row context
   */
  public void addCTE(
      ProgramStage programStage,
      QueryItem item,
      String cteDefinition,
      int offset,
      boolean isRowContext) {
    String key = computeKey(item);
    if (cteDefinitions.containsKey(key)) {
      cteDefinitions.get(key).getOffsets().add(offset);
    } else {
      var cteDef =
          new CteDefinition(
              programStage.getUid(), item.getItemId(), cteDefinition, offset, isRowContext);
      cteDefinitions.put(key, cteDef);
    }
  }

  public void addExistsCTE(ProgramStage programStage, QueryItem item, String cteDefinition) {
    var cteDef =
        new CteDefinition(programStage.getUid(), item.getItemId(), cteDefinition, -999, false)
            .setExists(true);
    cteDefinitions.put(programStage.getUid(), cteDef);
  }

  /**
   * Adds a CTE definition to the context.
   *
   * @param programIndicator The program indicator
   * @param cteDefinition The CTE definition (the SQL query)
   */
  public void addProgramIndicatorCTE(ProgramIndicator programIndicator, String cteDefinition) {
    cteDefinitions.put(
        programIndicator.getUid(), new CteDefinition(programIndicator.getUid(), cteDefinition));
  }

  public void addCTEFilter(QueryItem item, String ctedefinition) {
    String key = computeKey(item);
    if (!cteDefinitions.containsKey(key)) {
      ProgramStage programStage = item.getProgramStage();
      cteDefinitions.put(
          key,
          new CteDefinition(
              item.getItemId(),
              programStage == null ? null : programStage.getUid(),
              ctedefinition,
              true));
    }
  }

  public String getCTEDefinition() {
    if (cteDefinitions.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder("WITH ");
    boolean first = true;
    for (Map.Entry<String, CteDefinition> entry : cteDefinitions.entrySet()) {
      if (!first) {
        sb.append(", ");
      }
      CteDefinition cteDef = entry.getValue();
      sb.append(cteDef.asCteName(entry.getKey()))
          .append(" AS (")
          .append(entry.getValue().getCteDefinition())
          .append(")");
      first = false;
    }
    return sb.toString();
  }

  // Rename to item uid
  public Set<String> getCTENames() {
    return cteDefinitions.keySet();
  }

  public boolean containsCte(String cteName) {
    return cteDefinitions.containsKey(cteName);
  }
}
