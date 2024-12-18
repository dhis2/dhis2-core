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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

public class CTEContext {
  private final Map<String, String> cteDefinitions = new LinkedHashMap<>();
  private final Map<String, String> columnMappings = new HashMap<>();
  @Getter private final Map<String, String> rowContextReferences = new HashMap<>();

  public void addCTE(String cteName, String cteDefinition) {
    cteDefinitions.put(cteName, cteDefinition);
  }

  public void addColumnMapping(String originalColumn, String cteReference) {
    columnMappings.put(originalColumn, cteReference);
  }

  /**
   * Adds a mapping between a row context column and the CTE name that it references.
   *
   * @param alias The alias of the row context column, for instance "EPEcjy3FWmI.lJTx9EZ1dk1"
   * @param cteName The name of the CTE that the row context column references, for instance
   *     "ps_epecjy3fwmi_ljtx9ez1dk1"
   */
  public void addRowContextColumnMapping(String alias, String cteName) {
    rowContextReferences.put(alias, cteName);
  }

  public String getCTEDefinition() {
    if (cteDefinitions.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder("WITH ");
    boolean first = true;
    for (Map.Entry<String, String> entry : cteDefinitions.entrySet()) {
      if (!first) {
        sb.append(", ");
      }
      sb.append(entry.getKey()).append(" AS (").append(entry.getValue()).append(")");
      first = false;
    }
    return sb.toString();
  }

  public Set<String> getCTENames() {
    return cteDefinitions.keySet();
  }

  public String getColumnMapping(String columnId) {
    return columnMappings.getOrDefault(columnId, columnId);
  }

  public boolean containsCteFilter(String cteFilterName) {
    return cteDefinitions.containsKey(cteFilterName);
  }
}
