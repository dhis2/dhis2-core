/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.common;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.commons.text.RandomStringGenerator;

/**
 * This class represents a CTE (Common Table Expression) definition that is generated during the
 * analytics enrollment and event SQL generation process. CTE definitions are generated based on the
 * query items or program indicators that are used in the analytics request and are processed to
 * build the final analytics SQL query.
 */
public class CteDefinition {

  /** Query item id * */
  @Getter private String itemId;

  /** The program stage uid * */
  @Getter private final String programStageUid;

  /** The program indicator uid * */
  @Getter private String programIndicatorUid;

  /** The CTE definition (the SQL query) * */
  @Getter private final String cteDefinition;

  /** The calculated offset * */
  @Getter private final List<Integer> offsets = new ArrayList<>();

  /** The alias of the CTE * */
  private final String alias;

  /** Whether the CTE is a row context * */
  @Getter private boolean rowContext;

  /** Whether the CTE is a program indicator * */
  @Getter private boolean programIndicator = false;

  /** Whether the CTE is a filter * */
  @Getter private boolean filter = false;

  /** Whether the CTE is a exists, used for checking if the enrollment exists * */
  private boolean isExists = false;

  @Getter private boolean aggregationBase = false;

  @Getter private boolean requiresCoalesce = false;

  @Getter private String aggregateWhereClause;

  public CteDefinition setExists(boolean exists) {
    this.isExists = exists;
    return this;
  }

  public String getAlias() {
    if (offsets.isEmpty()) {
      return alias;
    }
    return computeAlias(offsets.get(0));
  }

  public String getAlias(int offset) {
    return computeAlias(offset);
  }

  private String computeAlias(int offset) {
    return alias + "_" + offset;
  }

  public CteDefinition(
      String programStageUid, String queryItemId, String cteDefinition, int offset) {
    this.programStageUid = programStageUid;
    this.itemId = queryItemId;
    this.cteDefinition = cteDefinition;
    this.offsets.add(offset);
    // one alias per offset
    this.alias = new RandomStringGenerator.Builder().withinRange('a', 'z').get().generate(5);
    this.rowContext = false;
  }

  public CteDefinition(
      String programStageUid,
      String queryItemId,
      String cteDefinition,
      int offset,
      boolean isRowContext) {
    this(programStageUid, queryItemId, cteDefinition, offset);
    this.rowContext = isRowContext;
  }

  public CteDefinition(String cteDefinition, String aggregateWhereClause) {
    this(null, null, cteDefinition, 0, false);
    this.rowContext = false;
    this.aggregationBase = true;
    this.aggregateWhereClause = aggregateWhereClause;
  }

  public CteDefinition(String programIndicatorUid, String cteDefinition, boolean requiresCoalesce) {
    this.cteDefinition = cteDefinition;
    this.programIndicatorUid = programIndicatorUid;
    this.programStageUid = null;
    // ignore offset
    this.alias = new RandomStringGenerator.Builder().withinRange('a', 'z').get().generate(5);
    this.rowContext = false;
    this.programIndicator = true;
    this.requiresCoalesce = requiresCoalesce;
  }

  public CteDefinition(
      String queryItemId, String programStageUid, String cteDefinition, boolean isFilter) {
    this.itemId = queryItemId;
    this.cteDefinition = cteDefinition;
    this.programIndicatorUid = null;
    this.programStageUid = programStageUid;
    // ignore offset
    this.alias = new RandomStringGenerator.Builder().withinRange('a', 'z').get().generate(5);
    this.rowContext = false;
    this.programIndicator = false;
    this.filter = isFilter;
  }

  public boolean isProgramStage() {
    return !filter && !programIndicator && !isExists;
  }

  public boolean isExists() {
    return isExists;
  }
}
