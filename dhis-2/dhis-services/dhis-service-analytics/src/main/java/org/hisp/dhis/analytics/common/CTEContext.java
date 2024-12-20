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
import org.apache.commons.text.RandomStringGenerator;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;

public class CTEContext {
  private final Map<String, CteDefinitionWithOffset> cteDefinitions = new LinkedHashMap<>();
  @Getter private final Map<String, String> rowContextReferences = new HashMap<>();

  public CteDefinitionWithOffset getDefinitionByItemUid(String itemUid) {
    return cteDefinitions.get(itemUid);
  }

  public void addCTE(ProgramStage programStage, QueryItem item, String cteDefinition, int offset) {
    cteDefinitions.put(
        item.getItem().getUid(),
        new CteDefinitionWithOffset(programStage.getUid(), cteDefinition, offset));
  }

  public void addCTE(
      ProgramStage programStage,
      QueryItem item,
      String cteDefinition,
      int offset,
      boolean isRowContext) {
    cteDefinitions.put(
        item.getItem().getUid(),
        new CteDefinitionWithOffset(programStage.getUid(), cteDefinition, offset, isRowContext));
  }

  /**
   * Adds a CTE definition to the context.
   *
   * @param programIndicator The program indicator
   * @param cteDefinition The CTE definition (the SQL query)
   */
  public void addCTE(ProgramIndicator programIndicator, String cteDefinition) {
    cteDefinitions.put(
        programIndicator.getUid(),
        new CteDefinitionWithOffset(programIndicator.getUid(), cteDefinition));
  }

  public void addCTEFilter(String name, String ctedefinition) {
    cteDefinitions.put(name, new CteDefinitionWithOffset(name, ctedefinition, true));
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
    for (Map.Entry<String, CteDefinitionWithOffset> entry : cteDefinitions.entrySet()) {
      if (!first) {
        sb.append(", ");
      }
      CteDefinitionWithOffset cteDef = entry.getValue();
      sb.append(cteDef.asCteName(entry.getKey()))
          .append(" AS (")
          .append(entry.getValue().cteDefinition)
          .append(")");
      first = false;
    }
    return sb.toString();
  }

  // Rename to item uid
  public Set<String> getCTENames() {
    return cteDefinitions.keySet();
  }

  public boolean containsCteFilter(String cteFilterName) {
    return cteDefinitions.containsKey(cteFilterName);
  }

  @Getter
  public static class CteDefinitionWithOffset {
    // The program stage uid
    private final String programStageUid;
    // The program indicator uid
    private String programIndicatorUid;
    // The CTE definition (the SQL query)
    private final String cteDefinition;
    // The calculated offset
    private final int offset;
    // The alias of the CTE
    private final String alias;
    // Whether the CTE is a row context (TODO this need a better explanation)
    private boolean isRowContext;
    // Whether the CTE is a program indicator
    private boolean isProgramIndicator = false;
    // Whether the CTE is a filter
    private boolean isFilter = false;
    private static final String PS_PREFIX = "ps";
    private static final String PI_PREFIX = "pi";

    public CteDefinitionWithOffset(String programStageUid, String cteDefinition, int offset) {
      this.programStageUid = programStageUid;
      this.cteDefinition = cteDefinition;
      this.offset = offset;
      this.alias = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(5);
      this.isRowContext = false;
    }

    public CteDefinitionWithOffset(
        String programStageUid, String cteDefinition, int offset, boolean isRowContext) {
      this(programStageUid, cteDefinition, offset);
      this.isRowContext = isRowContext;
    }

    public CteDefinitionWithOffset(String programIndicatorUid, String cteDefinition) {
      this.cteDefinition = cteDefinition;
      this.programIndicatorUid = programIndicatorUid;
      this.programStageUid = null;
      this.offset = -999;
      this.alias = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(5);
      this.isRowContext = false;
      this.isProgramIndicator = true;
    }

    public CteDefinitionWithOffset(String cteFilterName, String cteDefinition, boolean isFilter) {
      this.cteDefinition = cteDefinition;
      this.programIndicatorUid = null;
      this.programStageUid = null;
      this.offset = -999;
      this.alias = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(5);
      this.isRowContext = false;
      this.isProgramIndicator = false;
      this.isFilter = isFilter;
    }

    /**
     * @param uid the uid of an dimension item or ProgramIndicator
     * @return the name of the CTE
     */
    public String asCteName(String uid) {
      if (isProgramIndicator) {
        return "%s_%s".formatted(PI_PREFIX, programIndicatorUid.toLowerCase());
      }
      if (isFilter) {
        return "%s".formatted(uid.toLowerCase());
      }
      return "%s_%s_%s".formatted(PS_PREFIX, programStageUid.toLowerCase(), uid.toLowerCase());
    }

    public boolean isProgramStage() {
      return !isFilter && !isProgramIndicator;
    }
  }
}
