/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.parser.expression;

import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.AntlrExpressionVisitor;
import org.hisp.dhis.antlr.AntlrParserUtils;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.expression.ExpressionInfo;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.jdbc.StatementBuilder;
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
@Builder(toBuilder = true)
public class CommonExpressionVisitor extends AntlrExpressionVisitor {
  private IdentifiableObjectManager idObjectManager;

  private DimensionService dimensionService;

  private ProgramIndicatorService programIndicatorService;

  private ProgramStageService programStageService;

  private TrackedEntityAttributeService attributeService;

  private StatementBuilder statementBuilder;

  /**
   * A {@link Supplier} object that can return a {@link I18n} instance when needed. This is done
   * because retrieving a {@link I18n} instance can be expensive and is not needed for most parsing
   * operations. When it is needed, however, this can provide it.
   */
  private Supplier<I18n> i18nSupplier;

  /** Map of constant values to use in evaluating the expression. */
  @Builder.Default private Map<String, Constant> constantMap = new HashMap<>();

  /** Map of ExprItem object instances to call for each expression item. */
  private Map<Integer, ExpressionItem> itemMap;

  /** Method to call within the ExprItem object instance. */
  private ExpressionItemMethod itemMethod;

  /** Parameters to evaluate the expression to a value. */
  @Builder.Default private ExpressionParams params = ExpressionParams.builder().build();

  /** Parameters to generate SQL from a program expression. */
  @Builder.Default
  private ProgramExpressionParams progParams = ProgramExpressionParams.builder().build();

  /** State variables during an expression evaluation. */
  @Builder.Default private ExpressionState state = new ExpressionState();

  /**
   * Information found from parsing the raw expression (contains nothing that is the result of data
   * or metadata found in the database).
   */
  @Builder.Default private ExpressionInfo info = new ExpressionInfo();

  /**
   * Used to collect the string replacements to build a description. This may contain names of
   * metadata from the database.
   */
  @Builder.Default private Map<String, String> itemDescriptions = new HashMap<>();

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
