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

import static org.hisp.dhis.analytics.common.CteContext.ENROLLMENT_AGGR_BASE;
import static org.hisp.dhis.analytics.common.CteDefinition.CteType.PROGRAM_INDICATOR_ENROLLMENT;
import static org.hisp.dhis.analytics.common.CteDefinition.CteType.PROGRAM_INDICATOR_EVENT;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.commons.text.RandomStringGenerator;
import org.hisp.dhis.program.AnalyticsType;

/**
 * This class represents a CTE (Common Table Expression) definition generated during the analytics
 * enrollment and event SQL generation process. CTE definitions are generated based on the query
 * items or program indicators that are used in the analytics request and are processed to build the
 * final analytics SQL query.
 */
@Getter
public class CteDefinition {

  @Getter
  public enum CteType {
    /** CTE for standard program stage data/offsets. */
    PROGRAM_STAGE(10),
    /** CTE for the base aggregation query (e.g., enrollment_aggr_base). */
    BASE_AGGREGATION(10),
    /** CTE representing a full Program Indicator calculation. */
    PROGRAM_INDICATOR_EVENT(10),

    PROGRAM_INDICATOR_ENROLLMENT(10),

    /** CTE representing a simple filter condition. */
    FILTER(10),
    /** CTE replacing a V{...} variable subquery. */
    VARIABLE(10),
    /** CTE replacing a #{...} programStage/dataElement subquery. */
    PROGRAM_STAGE_DATE_ELEMENT(10),
    /** CTE replacing a d2:function(...) subquery (like d2:countIfValue). */
    D2_FUNCTION(10),
    /** CTE for checking existence (rowContext=true). */
    EXISTS(10),
    /** Special CTE for pre-aggregated enrollments. */
    TOP_ENROLLMENTS(1),
    SHADOW_ENROLLMENT_TABLE(2),
    SHADOW_EVENT_TABLE(3);

    private final int priority;

    CteType(int priority) {
      this.priority = priority;
    }
  }

  /* Query item id */
  private final String itemId;
  /* The program stage uid */
  private final String programStageUid;
  /* The program indicator uid */
  private final String programIndicatorUid;
  /* The CTE definition (the SQL query) */
  private final String cteDefinition;
  /* The calculated offset */
  private final List<Integer> offsets = new ArrayList<>();
  /* The alias of the CTE */
  private final String alias;
  /* Whether the CTE is a row context */
  private final boolean rowContext;
  /* Whether the CTE is a program indicator */
  private boolean isExists;
  private final boolean requiresCoalesce;
  private final String aggregateWhereClause;

  /** The column name to use for joining this CTE (e.g., "enrollment") */
  private final String joinColumn;

  private final Integer targetRank;
  private final CteType cteType;

  private CteDefinition(
      String itemId,
      String programStageUid,
      String programIndicatorUid,
      String cteDefinition,
      String alias,
      boolean rowContext,
      boolean isExists,
      boolean requiresCoalesce,
      String aggregateWhereClause,
      String joinColumn,
      Integer targetRank,
      CteType cteType) {
    this.itemId = itemId;
    this.programStageUid = programStageUid;
    this.programIndicatorUid = programIndicatorUid;
    this.cteDefinition = cteDefinition;
    this.alias = alias;
    this.rowContext = rowContext;
    this.isExists = isExists;
    this.requiresCoalesce = requiresCoalesce;
    this.aggregateWhereClause = aggregateWhereClause;
    this.joinColumn = joinColumn;
    this.targetRank = targetRank;
    this.cteType = cteType;
  }

  /** Creates a CTE definition for program stage data elements. */
  public CteDefinition(
      String programStageUid, String queryItemId, String cteDefinition, int offset) {
    this(
        queryItemId,
        programStageUid,
        null,
        cteDefinition,
        generateRandomAlias(),
        false, // rowContext
        false, // isExists
        false, // requiresCoalesce
        null, // aggregateWhereClause
        null, // joinColumn
        null, // targetRank
        CteType.PROGRAM_STAGE); // Set type
    this.offsets.add(offset);
  }

  // Constructor for standard Program Stage CTEs with rowContext
  public CteDefinition(
      String programStageUid,
      String queryItemId,
      String cteDefinition,
      int offset,
      boolean isRowContext) {
    this(
        queryItemId,
        programStageUid,
        null,
        cteDefinition,
        generateRandomAlias(),
        isRowContext, // Pass rowContext
        false, // isExists
        false, // requiresCoalesce
        null, // aggregateWhereClause
        null, // joinColumn
        null, // targetRank
        CteType.PROGRAM_STAGE); // Set type
    this.offsets.add(offset);
  }

  public CteDefinition(String cteDefinition, String aggregateWhereClause) {
    this(
        ENROLLMENT_AGGR_BASE, // itemId
        null, // programStageUid
        null, // programIndicatorUid
        cteDefinition,
        generateRandomAlias(), // alias
        false, // rowContext
        false, // isExists
        false, // requiresCoalesce
        aggregateWhereClause, // Pass aggregateWhereClause
        null, // joinColumn
        null, // targetRank
        CteType.BASE_AGGREGATION); // Set type
  }

  /** Creates a CTE definition for program indicators. */
  public static CteDefinition forProgramIndicator(
      String programIndicatorUid,
      AnalyticsType programIndicatorType,
      String cteDefinition,
      boolean requiresCoalesce) {
    // Calls private constructor, passing CteType.PROGRAM_INDICATOR
    return new CteDefinition(
        programIndicatorUid, // itemId
        null, // programStageUid
        programIndicatorUid, // programIndicatorUid
        cteDefinition,
        generateRandomAlias(), // alias
        false, // rowContext
        false, // isExists
        requiresCoalesce, // requiresCoalesce
        null, // aggregateWhereClause
        null, // joinColumn
        null, // targetRank
        programIndicatorType == AnalyticsType.EVENT
            ? PROGRAM_INDICATOR_EVENT
            : PROGRAM_INDICATOR_ENROLLMENT); // Set type
  }

  /** Creates a CTE definition for filter CTEs (replacing filter subqueries). */
  public static CteDefinition forFilter(
      String queryItemId, String programStageUid, String cteDefinition) {
    // Calls private constructor, passing CteType.FILTER
    return new CteDefinition(
        queryItemId,
        programStageUid,
        null, // programIndicatorUid
        cteDefinition,
        generateRandomAlias(), // alias
        false, // rowContext
        false, // isExists
        false, // requiresCoalesce
        null, // aggregateWhereClause
        null, // joinColumn
        null, // targetRank
        CteType.FILTER); // Set type
  }

  /**
   * Creates a Cte definition for Variable CTEs, used to replace nested subqueries from V{...}
   * Variables.
   *
   * @param key A unique key identifying this variable CTE instance (e.g.,
   *     "varcte_column_piUid_offset").
   * @param cteDefinitionSql The SQL body for the CTE.
   * @param joinColumn The column to use when joining this CTE (e.g., "enrollment").
   */
  public static CteDefinition forVariable(String key, String cteDefinitionSql, String joinColumn) {
    // Calls private constructor, passing CteType.VARIABLE
    return new CteDefinition(
        key, // itemId
        null, // programStageUid
        null, // programIndicatorUid
        cteDefinitionSql,
        generateRandomAlias(), // alias
        false, // rowContext
        false, // isExists
        false, // requiresCoalesce
        null, // aggregateWhereClause
        joinColumn, // Pass joinColumn
        null, // targetRank
        CteType.VARIABLE); // Set type
  }

  /**
   * Creates a CTE definition for Program Stage / Data Element CTEs (replacing #{...} subqueries).
   */
  public static CteDefinition forProgramStageDataElement(
      String key, String cteDefinitionSql, String joinColumn, int targetRank) {
    // Calls private constructor, passing CteType.PSDE
    return new CteDefinition(
        key, // itemId
        null, // programStageUid
        null, // programIndicatorUid
        cteDefinitionSql,
        generateRandomAlias(), // alias
        false, // rowContext
        false, // isExists
        false, // requiresCoalesce
        null, // aggregateWhereClause
        joinColumn, // Pass joinColumn
        targetRank, // Pass targetRank
        CteType.PROGRAM_STAGE_DATE_ELEMENT); // Set type
  }

  /**
   * Creates a CTE definition for D2 Function CTEs (e.g., d2:countIfValue).
   *
   * @param key A unique key identifying this D2 Function CTE instance.
   * @param cteDefinitionSql The SQL body for the CTE.
   * @param joinColumn The column to use when joining this CTE (typically "enrollment").
   * @return The created CteDefinition.
   */
  public static CteDefinition forD2Function(
      String key, String cteDefinitionSql, String joinColumn) {
    return new CteDefinition(
        key, // itemId
        null, // programStageUid
        null, // programIndicatorUid
        cteDefinitionSql,
        generateRandomAlias(), // alias
        false, // rowContext
        false, // isExists
        false, // requiresCoalesce
        null,
        joinColumn,
        null, // targetRank
        CteType.D2_FUNCTION);
  }

  public static CteDefinition forShadowTable(String tableName, String sql, CteType cteType) {
    return new CteDefinition(
        tableName, null, // programStageUid
        null, // programIndicatorUid
        sql, // cteDefinition
        tableName, // alias (same as table name!)
        false, // rowContext
        false, // isExists
        false, // requiresCoalesce
        null, // aggregateWhereClause
        null, // joinColumn
        null, // targetRank
        cteType);
  }

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
    if (offset < 0) {
      return alias + "_neg" + Math.abs(offset);
    } else {
      return alias + "_" + offset;
    }
  }

  public boolean isProgramStage() {
    return this.cteType == CteType.PROGRAM_STAGE && !isExists;
  }

  private static String generateRandomAlias() {
    // Use the same alias generation as before
    return new RandomStringGenerator.Builder().withinRange('a', 'z').get().generate(5);
  }

  public boolean isProgramIndicator() {
    return this.cteType == PROGRAM_INDICATOR_EVENT || this.cteType == PROGRAM_INDICATOR_ENROLLMENT;
  }

  public boolean isVariable() {
    return this.cteType == CteType.VARIABLE;
  }

  public boolean isPsDe() {
    return this.cteType == CteType.PROGRAM_STAGE_DATE_ELEMENT;
  }

  public boolean isFilter() {
    return this.cteType == CteType.FILTER;
  }

  public boolean isAggregationBase() {
    return this.cteType == CteType.BASE_AGGREGATION;
  }
}
