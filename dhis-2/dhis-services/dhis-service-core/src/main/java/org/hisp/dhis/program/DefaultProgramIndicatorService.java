/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.program;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.antlr.AntlrParserUtils.castClass;
import static org.hisp.dhis.antlr.AntlrParserUtils.castString;
import static org.hisp.dhis.expression.ExpressionParams.DEFAULT_EXPRESSION_PARAMS;
import static org.hisp.dhis.parser.expression.ExpressionItem.ITEM_GET_DESCRIPTIONS;
import static org.hisp.dhis.parser.expression.ExpressionItem.ITEM_GET_SQL;
import static org.hisp.dhis.parser.expression.ProgramExpressionParams.DEFAULT_PROGRAM_EXPRESSION_PARAMS;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.Parser;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.parser.expression.ExpressionItemMethod;
import org.hisp.dhis.parser.expression.ExpressionState;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.parser.expression.literal.SqlLiteral;
import org.hisp.dhis.program.function.RelationshipCountPlaceholder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Service("org.hisp.dhis.program.ProgramIndicatorService")
public class DefaultProgramIndicatorService implements ProgramIndicatorService {
  private final ProgramIndicatorStore programIndicatorStore;

  private final IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore;

  private final ProgramStageService programStageService;

  private final IdentifiableObjectManager idObjectManager;

  private final ExpressionService expressionService;

  private final DimensionService dimensionService;

  private final I18nManager i18nManager;

  private final Cache<String> analyticsSqlCache;

  private final SqlBuilder sqlBuilder;

  @Getter private final ImmutableMap<Integer, ExpressionItem> programIndicatorItems;

  public DefaultProgramIndicatorService(
      ProgramIndicatorStore programIndicatorStore,
      @Qualifier("org.hisp.dhis.program.ProgramIndicatorGroupStore")
          IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore,
      ProgramStageService programStageService,
      IdentifiableObjectManager idObjectManager,
      ExpressionService expressionService,
      DimensionService dimensionService,
      I18nManager i18nManager,
      CacheProvider cacheProvider,
      SqlBuilder sqlBuilder) {
    checkNotNull(programIndicatorStore);
    checkNotNull(programIndicatorGroupStore);
    checkNotNull(programStageService);
    checkNotNull(idObjectManager);
    checkNotNull(expressionService);
    checkNotNull(dimensionService);
    checkNotNull(i18nManager);
    checkNotNull(cacheProvider);
    checkNotNull(sqlBuilder);

    this.programIndicatorStore = programIndicatorStore;
    this.programIndicatorGroupStore = programIndicatorGroupStore;
    this.programStageService = programStageService;
    this.idObjectManager = idObjectManager;
    this.expressionService = expressionService;
    this.dimensionService = dimensionService;
    this.i18nManager = i18nManager;
    this.analyticsSqlCache = cacheProvider.createAnalyticsSqlCache();
    this.sqlBuilder = sqlBuilder;
    this.programIndicatorItems = new ExpressionMapBuilder().getExpressionItemMap();
  }

  // -------------------------------------------------------------------------
  // ProgramIndicator CRUD
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addProgramIndicator(ProgramIndicator programIndicator) {
    programIndicatorStore.save(programIndicator);
    return programIndicator.getId();
  }

  @Override
  @Transactional
  public void updateProgramIndicator(ProgramIndicator programIndicator) {
    programIndicatorStore.update(programIndicator);
  }

  @Override
  @Transactional
  public void deleteProgramIndicator(ProgramIndicator programIndicator) {
    programIndicatorStore.delete(programIndicator);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramIndicator getProgramIndicator(long id) {
    return programIndicatorStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramIndicator getProgramIndicator(String name) {
    return programIndicatorStore.getByName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramIndicator getProgramIndicatorByUid(String uid) {
    return programIndicatorStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramIndicator> getAllProgramIndicators() {
    return programIndicatorStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramIndicator> getProgramIndicatorsWithNoExpression() {
    return programIndicatorStore.getProgramIndicatorsWithNoExpression();
  }

  // -------------------------------------------------------------------------
  // ProgramIndicator logic
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  @Deprecated
  public String getUntypedDescription(String expression) {
    return getDescription(expression, null);
  }

  @Override
  @Transactional(readOnly = true)
  public String getExpressionDescription(String expression) {
    return getDescription(expression, Double.class);
  }

  @Override
  @Transactional(readOnly = true)
  public String getFilterDescription(String expression) {
    return getDescription(expression, Boolean.class);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean expressionIsValid(String expression) {
    return isValid(expression, Double.class);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean filterIsValid(String filter) {
    return isValid(filter, Boolean.class);
  }

  @Override
  @Transactional(readOnly = true)
  public void validate(String expression, Class<?> clazz, Map<String, String> itemDescriptions) {
    CommonExpressionVisitor visitor =
        newVisitor(
            ITEM_GET_DESCRIPTIONS,
            DEFAULT_EXPRESSION_PARAMS,
            DEFAULT_PROGRAM_EXPRESSION_PARAMS,
            false);

    castClass(clazz, Parser.visit(expression, visitor));

    itemDescriptions.putAll(visitor.getItemDescriptions());
  }

  @Override
  @Transactional(readOnly = true)
  public String getAnalyticsSql(
      String expression,
      DataType dataType,
      ProgramIndicator programIndicator,
      Date startDate,
      Date endDate) {
    return getAnalyticsSqlCached(
        ProgramIndicatorSqlContext.of(expression, dataType, programIndicator, startDate, endDate));
  }

  @Override
  // Not transactional, needed inside data exchange import
  public String getAnalyticsSqlAllowingNulls(
      String expression,
      DataType dataType,
      ProgramIndicator programIndicator,
      Date startDate,
      Date endDate) {
    return getAnalyticsSqlCached(
        ProgramIndicatorSqlContext.of(expression, dataType, programIndicator, startDate, endDate)
            .allowingNulls());
  }

  @Override
  @Transactional(readOnly = true)
  public String getAnalyticsSql(
      String expression,
      DataType dataType,
      ProgramIndicator programIndicator,
      Date startDate,
      Date endDate,
      String tableAlias) {
    return getAnalyticsSqlCached(
        ProgramIndicatorSqlContext.of(expression, dataType, programIndicator, startDate, endDate)
            .withTableAlias(tableAlias));
  }

  @Override
  @Transactional(readOnly = true)
  public String getAnalyticsSqlDeferRelationshipCount(
      String expression,
      DataType dataType,
      ProgramIndicator programIndicator,
      Date startDate,
      Date endDate,
      String tableAlias) {
    return getAnalyticsSqlCached(
        ProgramIndicatorSqlContext.of(expression, dataType, programIndicator, startDate, endDate)
            .withTableAlias(tableAlias)
            .deferRelationshipCount());
  }

  @Override
  @Transactional(readOnly = true)
  public String getAnalyticsSqlAllowingNulls(
      String expression,
      DataType dataType,
      ProgramIndicator programIndicator,
      Date startDate,
      Date endDate,
      String tableAlias) {
    return getAnalyticsSqlCached(
        ProgramIndicatorSqlContext.of(expression, dataType, programIndicator, startDate, endDate)
            .withTableAlias(tableAlias)
            .allowingNulls());
  }

  /**
   * Returns the analytics SQL for a program indicator expression, using a cache entry scoped to all
   * inputs that affect the generated SQL.
   *
   * <p>The request captures both SQL rendering context (expression, type, program indicator,
   * reporting dates and table alias) and rendering modes. {@link NullHandling#REPLACE_NULLS}
   * substitutes data element and attribute nulls with type-specific defaults, while {@link
   * NullHandling#PRESERVE_NULLS} leaves those nulls visible to null-sensitive expressions. {@link
   * RelationshipCountRendering#EXPAND_CORRELATED} expands {@code d2:relationshipCount} placeholders
   * immediately to correlated subqueries, while {@link
   * RelationshipCountRendering#DEFER_PLACEHOLDER} leaves them for dialect-specific expansion by the
   * analytics layer.
   *
   * @param request the SQL rendering request.
   * @return the generated SQL, or {@code null} when the request expression is {@code null}.
   */
  private String getAnalyticsSqlCached(ProgramIndicatorSqlContext request) {
    if (request.expression() == null) {
      return null;
    }

    String cacheKey = request.cacheKey();
    return analyticsSqlCache.get(cacheKey, k -> getAnalyticsSqlInternal(request));
  }

  /**
   * Request object for rendering a program indicator expression to analytics SQL.
   *
   * <p>The record groups the expression rendering context with the SQL generation modes so cached
   * and uncached helper methods do not need to pass the same long parameter list around.
   *
   * @param expression the program indicator expression or filter to render as SQL.
   * @param dataType the expected analytics result type for the rendered expression.
   * @param programIndicator the program indicator that supplies parser metadata and analytics
   *     context.
   * @param startDate the reporting-period start date used by period-aware program indicator items.
   * @param endDate the reporting-period end date used by period-aware program indicator items.
   * @param tableAlias optional replacement for the default analytics table alias.
   * @param nullHandling whether data element and attribute nulls are replaced with defaults or
   *     preserved.
   * @param relationshipCountRendering whether {@code d2:relationshipCount} placeholders are
   *     expanded immediately or left for later analytics-layer expansion.
   */
  private record ProgramIndicatorSqlContext(
      String expression,
      DataType dataType,
      ProgramIndicator programIndicator,
      Date startDate,
      Date endDate,
      String tableAlias,
      NullHandling nullHandling,
      RelationshipCountRendering relationshipCountRendering) {

    private static ProgramIndicatorSqlContext of(
        String expression,
        DataType dataType,
        ProgramIndicator programIndicator,
        Date startDate,
        Date endDate) {
      return new ProgramIndicatorSqlContext(
          expression,
          dataType,
          programIndicator,
          startDate,
          endDate,
          null,
          NullHandling.REPLACE_NULLS,
          RelationshipCountRendering.EXPAND_CORRELATED);
    }

    private ProgramIndicatorSqlContext allowingNulls() {
      return new ProgramIndicatorSqlContext(
          expression,
          dataType,
          programIndicator,
          startDate,
          endDate,
          tableAlias,
          NullHandling.PRESERVE_NULLS,
          relationshipCountRendering);
    }

    private ProgramIndicatorSqlContext withTableAlias(String tableAlias) {
      return new ProgramIndicatorSqlContext(
          expression,
          dataType,
          programIndicator,
          startDate,
          endDate,
          tableAlias,
          nullHandling,
          relationshipCountRendering);
    }

    private ProgramIndicatorSqlContext deferRelationshipCount() {
      return new ProgramIndicatorSqlContext(
          expression,
          dataType,
          programIndicator,
          startDate,
          endDate,
          tableAlias,
          nullHandling,
          RelationshipCountRendering.DEFER_PLACEHOLDER);
    }

    private boolean shouldReplaceNulls() {
      return nullHandling == NullHandling.REPLACE_NULLS;
    }

    private boolean shouldExpandRelationshipCount() {
      return relationshipCountRendering == RelationshipCountRendering.EXPAND_CORRELATED;
    }

    private String outerAlias() {
      return tableAlias != null ? tableAlias : ANALYTICS_TBL_ALIAS;
    }

    private String cacheKey() {
      return "expression="
          + expression
          + "|dataType="
          + dataType.name()
          + "|programIndicator="
          + programIndicator.getUid()
          + "|startDate="
          + dateValue(startDate)
          + "|endDate="
          + dateValue(endDate)
          + "|tableAlias="
          + (tableAlias == null ? "" : tableAlias)
          + "|nullHandling="
          + nullHandling
          + "|relationshipCountRendering="
          + relationshipCountRendering;
    }

    private static String dateValue(Date date) {
      return date == null ? StringUtils.EMPTY : Long.toString(date.getTime());
    }
  }

  private enum NullHandling {
    REPLACE_NULLS,
    PRESERVE_NULLS
  }

  private enum RelationshipCountRendering {
    EXPAND_CORRELATED,
    DEFER_PLACEHOLDER
  }

  private String getAnalyticsSqlInternal(ProgramIndicatorSqlContext request) {
    // Get the uids from the expression even if this is the filter
    Set<String> uids =
        getDataElementAndAttributeIdentifiers(
            request.programIndicator().getExpression(),
            request.programIndicator().getAnalyticsType());

    ExpressionParams params = ExpressionParams.builder().dataType(request.dataType()).build();

    ProgramExpressionParams progParams =
        ProgramExpressionParams.builder()
            .programIndicator(request.programIndicator())
            .reportingStartDate(request.startDate())
            .reportingEndDate(request.endDate())
            .dataElementAndAttributeIdentifiers(uids)
            .build();

    CommonExpressionVisitor visitor =
        newVisitor(ITEM_GET_SQL, params, progParams, request.shouldReplaceNulls());

    visitor.setExpressionLiteral(new SqlLiteral(visitor.getSqlBuilder()));

    String sql = castString(Parser.visit(request.expression(), visitor));

    String aliasedSql =
        request.tableAlias() != null
            ? sql.replaceAll(ANALYTICS_TBL_ALIAS + "\\.", request.tableAlias() + "\\.")
            : sql;

    return request.shouldExpandRelationshipCount()
        ? RelationshipCountPlaceholder.expandCorrelated(aliasedSql, request.outerAlias())
        : aliasedSql;
  }

  @Override
  @Transactional(readOnly = true)
  public String getAnyValueExistsClauseAnalyticsSql(
      String expression, AnalyticsType analyticsType) {
    if (expression == null) {
      return null;
    }

    try {
      Set<String> uids = getDataElementAndAttributeIdentifiers(expression, analyticsType);

      if (uids.isEmpty()) {
        return null;
      }

      String sql = StringUtils.EMPTY;

      for (String uid : uids) {

        sql += sqlBuilder.quote(uid) + " is not null or ";
      }

      return TextUtils.removeLastOr(sql).trim();
    } catch (ParserException e) {
      return null;
    }
  }

  // -------------------------------------------------------------------------
  // ProgramIndicatorGroup
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addProgramIndicatorGroup(ProgramIndicatorGroup programIndicatorGroup) {
    programIndicatorGroupStore.save(programIndicatorGroup);
    return programIndicatorGroup.getId();
  }

  @Override
  @Transactional
  public void updateProgramIndicatorGroup(ProgramIndicatorGroup programIndicatorGroup) {
    programIndicatorGroupStore.update(programIndicatorGroup);
  }

  @Override
  @Transactional
  public void deleteProgramIndicatorGroup(ProgramIndicatorGroup programIndicatorGroup) {
    programIndicatorGroupStore.delete(programIndicatorGroup);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramIndicatorGroup getProgramIndicatorGroup(long id) {
    return programIndicatorGroupStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramIndicatorGroup getProgramIndicatorGroup(String uid) {
    return programIndicatorGroupStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProgramIndicatorGroup> getAllProgramIndicatorGroups() {
    return programIndicatorGroupStore.getAll();
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private CommonExpressionVisitor newVisitor(
      ExpressionItemMethod itemMethod,
      ExpressionParams params,
      ProgramExpressionParams progParams,
      boolean replaceNulls) {
    return CommonExpressionVisitor.builder()
        .idObjectManager(idObjectManager)
        .dimensionService(dimensionService)
        .programIndicatorService(this)
        .programStageService(programStageService)
        .i18nSupplier(Suppliers.memoize(i18nManager::getI18n))
        .constantMap(expressionService.getConstantMap())
        .itemMap(programIndicatorItems)
        .itemMethod(itemMethod)
        .params(params)
        .progParams(progParams)
        .sqlBuilder(sqlBuilder)
        .state(ExpressionState.builder().replaceNulls(replaceNulls).build())
        .build();
  }

  private String getDescription(String expression, Class<?> clazz) {
    Map<String, String> itemDescriptions = new HashMap<>();

    validate(expression, clazz, itemDescriptions);

    String description = expression;

    for (Map.Entry<String, String> entry : itemDescriptions.entrySet()) {
      description = description.replace(entry.getKey(), entry.getValue());
    }

    return description;
  }

  private boolean isValid(String expression, Class<?> clazz) {
    if (expression != null) {
      try {
        validate(expression, clazz, new HashMap<>());
      } catch (ParserException e) {
        return false;
      }
    }

    return true;
  }

  private Set<String> getDataElementAndAttributeIdentifiers(
      String expression, AnalyticsType analyticsType) {
    Set<String> items = new HashSet<>();

    ProgramElementsAndAttributesCollecter listener =
        new ProgramElementsAndAttributesCollecter(items, analyticsType);

    Parser.listen(expression, listener);

    return items;
  }
}
