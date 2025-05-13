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
package org.hisp.dhis.parser.expression;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.AntlrExpressionVisitor;
import org.hisp.dhis.antlr.AntlrParserUtils;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.expression.ExpressionInfo;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import org.hisp.dhis.parser.expression.statement.DefaultStatementBuilder;
import org.hisp.dhis.parser.expression.statement.StatementBuilder;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;

/**
 * Common traversal of the ANTLR4 expression parse tree using the visitor pattern.
 *
 * @author Jim Grace
 */
@Getter
@Setter
public class CommonExpressionVisitor extends AntlrExpressionVisitor {

  private StatementBuilder statementBuilder;

  private IdentifiableObjectManager idObjectManager;

  private DimensionService dimensionService;

  private ProgramIndicatorService programIndicatorService;

  private ProgramStageService programStageService;

  private TrackedEntityAttributeService attributeService;

  private SqlBuilder sqlBuilder;

  private boolean useExperimentalSqlEngine;

  /**
   * A {@link Supplier} object that can return a {@link I18n} instance when needed. This is done
   * because retrieving a {@link I18n} instance can be expensive and is not needed for most parsing
   * operations. When it is needed, however, this can provide it.
   */
  private Supplier<I18n> i18nSupplier;

  /** Map of constant values to use in evaluating the expression. */
  private Map<String, Constant> constantMap = new HashMap<>();

  /** Map of ExprItem object instances to call for each expression item. */
  private Map<Integer, ExpressionItem> itemMap;

  /** Method to call within the ExprItem object instance. */
  private ExpressionItemMethod itemMethod;

  /** Parameters to evaluate the expression to a value. */
  private ExpressionParams params;

  /** Parameters to generate SQL from a program expression. */
  private ProgramExpressionParams progParams;

  /** State variables during an expression evaluation. */
  private ExpressionState state;

  /**
   * Information found from parsing the raw expression (contains nothing that is the result of data
   * or metadata found in the database).
   */
  private ExpressionInfo info;

  /**
   * Used to collect the string replacements to build a description. This may contain names of
   * metadata from the database.
   */
  private Map<String, String> itemDescriptions;

  // -------------------------------------------------------------------------
  // Custom constructor
  // -------------------------------------------------------------------------

  @Builder(toBuilder = true)
  public CommonExpressionVisitor(
      IdentifiableObjectManager idObjectManager,
      DimensionService dimensionService,
      ProgramIndicatorService programIndicatorService,
      ProgramStageService programStageService,
      TrackedEntityAttributeService attributeService,
      SqlBuilder sqlBuilder,
      boolean useExperimentalSqlEngine,
      Supplier<I18n> i18nSupplier,
      Map<String, Constant> constantMap,
      Map<Integer, ExpressionItem> itemMap,
      ExpressionItemMethod itemMethod,
      ExpressionParams params,
      ProgramExpressionParams progParams,
      ExpressionState state,
      ExpressionInfo info,
      Map<String, String> itemDescriptions) {

    checkNotNull(sqlBuilder);

    this.statementBuilder = new DefaultStatementBuilder(sqlBuilder);
    this.idObjectManager = idObjectManager;
    this.dimensionService = dimensionService;
    this.programIndicatorService = programIndicatorService;
    this.programStageService = programStageService;
    this.attributeService = attributeService;
    this.sqlBuilder = sqlBuilder;
    this.useExperimentalSqlEngine = useExperimentalSqlEngine;
    this.i18nSupplier = i18nSupplier;
    this.constantMap = constantMap != null ? constantMap : new HashMap<>();
    this.itemMap = itemMap;
    this.itemMethod = itemMethod;
    this.params = params != null ? params : ExpressionParams.builder().build();
    this.progParams = progParams != null ? progParams : ProgramExpressionParams.builder().build();
    this.state = state != null ? state : new ExpressionState();
    this.info = info != null ? info : new ExpressionInfo();
    this.itemDescriptions = itemDescriptions != null ? itemDescriptions : new HashMap<>();
  }

  // -------------------------------------------------------------------------
  // Visitor logic
  // -------------------------------------------------------------------------

  @Override
  public Object visitExpr(ExprContext ctx) {
    if (ctx.it != null) {
      ExpressionItem item = itemMap.get(ctx.it.getType());

      if (item == null) {
        throw new org.hisp.dhis.antlr.ParserExceptionWithoutContext(
            "Item " + ctx.it.getText() + " not supported for this type of expression");
      }

      return itemMethod.apply(item, ctx, this);
    }

    if (!ctx.expr().isEmpty()) // If there's an expr, visit the expr
    {
      return visit(ctx.expr(0));
    }

    return visit(ctx.getChild(0)); // All others: visit first child.
  }

  /**
   * Visits a part of an expression that will be evaluated in predictors on past-period sampled
   * data. During this visit we want any data items that are encountered to be remembered as those
   * for which we will need to collect past-period data.
   *
   * <p>To accomplish this, we first save any {@link DimensionalItemId}s collected so far and set
   * the collection of {@link DimensionalItemId} to gather as the set of sample item ids we have
   * collected so far if any. As we evaluate this part of the expression, any item ids encountered
   * will be added here. After we are done, we put these item ids back into the sample item ids, and
   * restore the saved item ids for any further collecting.
   *
   * @param expr the part of the expression to evaluate for samples
   * @return the value of that part of the expression
   */
  public Object visitSamples(ExprContext expr) {
    Set<DimensionalItemId> savedItemIds = info.getItemIds();
    info.setItemIds(info.getSampleItemIds());

    Object result = visitExpr(expr);

    info.setSampleItemIds(info.getItemIds());
    info.setItemIds(savedItemIds);

    return result;
  }

  /**
   * Visits a context while allowing null values (not replacing them with 0 or ''), even if we would
   * otherwise be replacing them.
   *
   * @param ctx any context
   * @return the value while allowing nulls
   */
  public Object visitAllowingNulls(ParserRuleContext ctx) {
    boolean savedReplaceNulls = state.isReplaceNulls();

    state.setReplaceNulls(false);

    Object result = visit(ctx);

    state.setReplaceNulls(savedReplaceNulls);

    return result;
  }

  /**
   * Visits a context with a configuration of query modifiers.
   *
   * @param ctx any context
   * @param mods the query modifiers
   * @return the value with the applied offset
   */
  public Object visitWithQueryMods(ParserRuleContext ctx, QueryModifiers mods) {
    QueryModifiers savedQueryMods = state.getQueryMods();

    state.setQueryMods(mods);

    Object result = visit(ctx);

    state.setQueryMods(savedQueryMods);

    return result;
  }

  /**
   * Visit a parse subtree to generate SQL with a request that boolean items should generate a
   * boolean value.
   */
  public String sqlBooleanVisit(ExprContext ctx) {
    return AntlrParserUtils.castString(visitWithDataType(ctx, DataType.BOOLEAN));
  }

  /**
   * Visit a parse subtree to generate SQL with a request that boolean items should generate a
   * numeric value.
   */
  public String sqlNumericVisit(ExprContext ctx) {
    return AntlrParserUtils.castString(visitWithDataType(ctx, DataType.NUMERIC));
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private Object visitWithDataType(ExprContext ctx, DataType dataType) {
    ExpressionParams visitParams = params.toBuilder().dataType(dataType).build();
    CommonExpressionVisitor visitor = this.toBuilder().params(visitParams).build();
    visitor.setExpressionLiteral(this.expressionLiteral);

    return visitor.visitExpr(ctx);
  }
}
