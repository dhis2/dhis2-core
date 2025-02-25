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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.matcher.DataElementCountMatcher;
import org.hisp.dhis.analytics.util.optimizer.cte.matcher.LastCreatedMatcher;
import org.hisp.dhis.analytics.util.optimizer.cte.matcher.LastEventValueMatcher;
import org.hisp.dhis.analytics.util.optimizer.cte.matcher.LastSchedMatcher;
import org.hisp.dhis.analytics.util.optimizer.cte.matcher.RelationshipCountMatcher;
import org.hisp.dhis.analytics.util.optimizer.cte.transformer.FunctionTransformer;
import org.hisp.dhis.analytics.util.optimizer.cte.transformer.SubSelectTransformer;

/**
 * A SQL expression transformer that handles complex expression transformations while preserving
 * operator precedence, CASE expressions, and CAST operations. This transformer is specifically
 * designed to handle correlated subqueries and convert them into CTE-based expressions.
 *
 * <p>The transformer implements the Visitor pattern through JSQLParser's ExpressionVisitorAdapter
 * and provides specialized handling for: - Logical operators (AND, OR) with proper parenthesization
 * - Arithmetic operators with precedence preservation - CASE expressions including nested cases -
 * CAST operations - Subquery transformations
 */
@Getter
public class ExpressionTransformer extends ExpressionVisitorAdapter {
  /**
   * Holds the current transformed expression during the transformation process. This field is
   * updated by each visit method as the transformer traverses the expression tree.
   */
  private Expression currentTransformedExpression;

  /**
   * Handles the transformation of SubSelect expressions into CTEs. Contains the logic for matching
   * and transforming different types of subqueries.
   */
  private final SubSelectTransformer subSelectTransformer;

  /**
   * Handles the transformation of SQL functions. Provides specialized handling for different
   * function types.
   */
  private final FunctionTransformer functionTransformer;

  public ExpressionTransformer() {
    this.subSelectTransformer =
        new SubSelectTransformer(
            Arrays.asList(
                new LastSchedMatcher(),
                new LastCreatedMatcher(),
                new LastEventValueMatcher(),
                new RelationshipCountMatcher(),
                new DataElementCountMatcher()));
    this.functionTransformer = new FunctionTransformer(this);
  }

  @Override
  public void visit(Function function) {
    // First, use the existing FunctionTransformer to process the function
    Expression processedFunc = functionTransformer.transform(function);

    // If the function contains subquery parameters, we need additional processing
    if (function.getParameters() != null && function.getParameters().getExpressions() != null) {
      boolean hasSubqueries = false;
      List<Expression> originalParams = function.getParameters().getExpressions();

      // Check if any parameters are subqueries
      for (Expression param : originalParams) {
        if (param instanceof SubSelect) {
          hasSubqueries = true;
          break;
        }
      }

      // If there are subqueries in the parameters, they would have been transformed
      // by functionTransformer.transform() through recursive visiting
      if (hasSubqueries) {
        // The function with transformed parameters is in processedFunc
        // We can use it directly
        currentTransformedExpression = processedFunc;
        return;
      }
    }

    // If no special handling was needed, use the result from functionTransformer
    currentTransformedExpression = processedFunc;
  }

  public Map<SubSelect, FoundSubSelect> getExtractedSubSelects() {
    return subSelectTransformer.getExtractedSubSelects();
  }

  @Override
  public void visit(ExtractExpression extract) {
    // Visit the expression being extracted from
    extract.getExpression().accept(this);
    Expression transformedExpr = currentTransformedExpression;

    // Create new extract expression
    ExtractExpression newExtract = new ExtractExpression();
    newExtract.setName(extract.getName()); // 'epoch'
    newExtract.setExpression(transformedExpr);

    // Wrap in parentheses if it's part of a division
    currentTransformedExpression = new Parenthesis(newExtract);
  }

  // Arithmetic Operators
  @Override
  public void visit(Addition addition) {
    handleBinaryArithmeticExpression(addition);
  }

  @Override
  public void visit(Multiplication multiplication) {
    handleBinaryArithmeticExpression(multiplication);
  }

  @Override
  public void visit(Subtraction subtraction) {
    handleBinaryArithmeticExpression(subtraction);
  }

  @Override
  public void visit(Division division) {
    handleBinaryArithmeticExpression(division);
  }

  @Override
  public void visit(Modulo modulo) {
    handleBinaryArithmeticExpression(modulo);
  }

  // Comparison Operators
  @Override
  public void visit(EqualsTo equalsTo) {
    handleBinaryExpression(equalsTo);
  }

  @Override
  public void visit(GreaterThan greaterThan) {
    handleBinaryExpression(greaterThan);
  }

  @Override
  public void visit(GreaterThanEquals greaterThanEquals) {
    handleBinaryExpression(greaterThanEquals);
  }

  @Override
  public void visit(MinorThan minorThan) {
    handleBinaryExpression(minorThan);
  }

  @Override
  public void visit(MinorThanEquals minorThanEquals) {
    handleBinaryExpression(minorThanEquals);
  }

  @Override
  public void visit(NotEqualsTo notEqualsTo) {
    handleBinaryExpression(notEqualsTo);
  }

  /**
   * Transforms OR expressions while preserving operator precedence through proper parenthesization.
   * Ensures that both sides of the OR are properly wrapped in parentheses to maintain the correct
   * evaluation order.
   *
   * <p>Example transformation: (A AND B) OR (C AND D) -> ((A AND B)) OR ((C AND D))
   *
   * @param expr The OR expression to transform
   */
  @Override
  public void visit(OrExpression expr) {
    Expression leftExpr = expr.getLeftExpression();
    Expression rightExpr = expr.getRightExpression();

    // Transform left side
    leftExpr.accept(this);
    Expression transformedLeft = currentTransformedExpression;
    // Ensure left side is double-parenthesized
    transformedLeft =
        new Parenthesis(
            transformedLeft instanceof Parenthesis
                ? transformedLeft
                : new Parenthesis(transformedLeft));

    // Transform right side
    rightExpr.accept(this);
    Expression transformedRight = currentTransformedExpression;
    // Ensure right side is double-parenthesized
    transformedRight =
        new Parenthesis(
            transformedRight instanceof Parenthesis
                ? transformedRight
                : new Parenthesis(transformedRight));

    currentTransformedExpression = new OrExpression(transformedLeft, transformedRight);
  }

  /**
   * Preserves CAST operations while transforming their inner expressions. Maintains the type
   * information and ensures proper handling of date arithmetic and other type-sensitive operations.
   *
   * @param cast The CAST expression to transform
   */
  @Override
  public void visit(CastExpression cast) {

    // Get the inner expression
    Expression innerExpr = cast.getLeftExpression();
    innerExpr.accept(this);
    Expression transformedInner = currentTransformedExpression;

    // Create new cast with the same type but transformed inner expression
    CastExpression newCast = new CastExpression();
    newCast.setLeftExpression(transformedInner);
    newCast.setType(cast.getType());

    currentTransformedExpression = newCast;
  }

  /**
   * Handles CASE expression transformation while preserving the structure and logic of
   * WHEN/THEN/ELSE clauses. Supports nested CASE expressions and ensures all sub-expressions are
   * properly transformed.
   *
   * @param caseExpression The CASE expression to transform
   */
  @Override
  public void visit(CaseExpression caseExpression) {

    // Preserve the original CASE expression structure
    Expression switchExp = caseExpression.getSwitchExpression();
    if (switchExp != null) {
      switchExp.accept(this);
      switchExp = currentTransformedExpression;
    }

    List<WhenClause> transformedWhenClauses = new ArrayList<>();
    for (WhenClause whenClause : caseExpression.getWhenClauses()) {
      whenClause.getWhenExpression().accept(this);
      Expression transformedWhen = currentTransformedExpression;
      whenClause.getThenExpression().accept(this);
      Expression transformedThen = currentTransformedExpression;

      WhenClause newWhenClause = new WhenClause();
      newWhenClause.setWhenExpression(transformedWhen);
      newWhenClause.setThenExpression(transformedThen);
      transformedWhenClauses.add(newWhenClause);
    }

    Expression elseExpression = caseExpression.getElseExpression();
    if (elseExpression != null) {
      elseExpression.accept(this);
      elseExpression = currentTransformedExpression;
    }

    CaseExpression transformedCase = new CaseExpression();
    transformedCase.setSwitchExpression(switchExp);
    transformedCase.setWhenClauses(transformedWhenClauses);
    transformedCase.setElseExpression(elseExpression);

    currentTransformedExpression = transformedCase;
  }

  @Override
  public void visit(AndExpression expr) {
    Expression leftExpr = expr.getLeftExpression();
    Expression rightExpr = expr.getRightExpression();

    leftExpr.accept(this);
    Expression transformedLeft = currentTransformedExpression;

    rightExpr.accept(this);
    Expression transformedRight = currentTransformedExpression;

    currentTransformedExpression = new AndExpression(transformedLeft, transformedRight);
  }

  @Override
  public void visit(Parenthesis parenthesis) {
    Expression inner = parenthesis.getExpression();
    inner.accept(this);
    Expression transformedInner = currentTransformedExpression;

    currentTransformedExpression = new Parenthesis(transformedInner);
  }

  @Override
  public void visit(SubSelect subSelect) {
    currentTransformedExpression = subSelectTransformer.transform(subSelect);
  }

  @Override
  public void visit(NotExpression notExpression) {
    notExpression.getExpression().accept(this);
    Expression transformedExpr = currentTransformedExpression;

    NotExpression newNot = new NotExpression();
    newNot.setExpression(transformedExpr);

    currentTransformedExpression = newNot;
  }

  // Special Operators
  @Override
  public void visit(IsNullExpression isNull) {
    isNull.getLeftExpression().accept(this);
    Expression transformedExpr = currentTransformedExpression;

    IsNullExpression newIsNull = new IsNullExpression();
    newIsNull.setLeftExpression(transformedExpr);
    newIsNull.setNot(isNull.isNot());

    currentTransformedExpression = newIsNull;
  }

  @Override
  public void visit(InExpression inExpression) {
    inExpression.getLeftExpression().accept(this);
    Expression leftExpr = currentTransformedExpression;

    Expression rightExpr = inExpression.getRightExpression();
    if (rightExpr != null) {
      rightExpr.accept(this);
      rightExpr = currentTransformedExpression;
    }

    ItemsList rightItemsList = inExpression.getRightItemsList();
    if (rightItemsList != null) {
      rightItemsList.accept(this);
    }

    InExpression newIn = new InExpression();
    newIn.setLeftExpression(leftExpr);
    if (rightExpr != null) {
      newIn.setRightExpression(rightExpr);
    } else {
      newIn.setRightItemsList(rightItemsList);
    }
    newIn.setNot(inExpression.isNot());

    currentTransformedExpression = newIn;
  }

  @Override
  public void visit(Between between) {
    between.getLeftExpression().accept(this);
    Expression leftExpr = currentTransformedExpression;

    between.getBetweenExpressionStart().accept(this);
    Expression startExpr = currentTransformedExpression;

    between.getBetweenExpressionEnd().accept(this);
    Expression endExpr = currentTransformedExpression;

    Between newBetween = new Between();
    newBetween.setLeftExpression(leftExpr);
    newBetween.setBetweenExpressionStart(startExpr);
    newBetween.setBetweenExpressionEnd(endExpr);
    newBetween.setNot(between.isNot());

    currentTransformedExpression = newBetween;
  }

  // Values
  @Override
  public void visit(DateValue value) {
    currentTransformedExpression = value;
  }

  @Override
  public void visit(TimestampValue value) {
    currentTransformedExpression = value;
  }

  @Override
  public void visit(TimeValue value) {
    currentTransformedExpression = value;
  }

  @Override
  public void visit(DoubleValue value) {
    currentTransformedExpression = value;
  }

  @Override
  public void visit(Column column) {
    currentTransformedExpression = column;
  }

  @Override
  public void visit(StringValue value) {
    currentTransformedExpression = value;
  }

  @Override
  public void visit(LongValue value) {
    currentTransformedExpression = value;
  }

  public Expression getTransformedExpression() {
    return currentTransformedExpression;
  }

  /**
   * Helper method for handling binary expressions (comparison operators, arithmetic operators).
   * Creates a new instance of the appropriate expression type with transformed operands.
   *
   * @param expression The binary expression to transform
   */
  private void handleBinaryExpression(BinaryExpression expression) {
    expression.getLeftExpression().accept(this);
    Expression leftExpr = currentTransformedExpression;

    expression.getRightExpression().accept(this);
    Expression rightExpr = currentTransformedExpression;

    try {
      // Create a new instance of the same type of expression
      BinaryExpression newExpr = expression.getClass().getDeclaredConstructor().newInstance();
      newExpr.setLeftExpression(leftExpr);
      newExpr.setRightExpression(rightExpr);

      currentTransformedExpression = newExpr;
    } catch (Exception e) {
      // Fallback to original expression if instantiation fails
      currentTransformedExpression = expression;
    }
  }

  /**
   * Helper method for arithmetic expressions that handles operator precedence through
   * parenthesization. Ensures proper evaluation order especially for division and mixed arithmetic
   * operations.
   *
   * @param expr The arithmetic expression to transform
   */
  private void handleBinaryArithmeticExpression(BinaryExpression expr) {
    expr.getLeftExpression().accept(this);
    Expression leftExpr = currentTransformedExpression;

    expr.getRightExpression().accept(this);
    Expression rightExpr = currentTransformedExpression;

    try {
      BinaryExpression newExpr = expr.getClass().getDeclaredConstructor().newInstance();
      newExpr.setLeftExpression(leftExpr);
      newExpr.setRightExpression(rightExpr);

      // For arithmetic expressions, wrap in parentheses if:
      // 1. It's a division (to preserve precedence)
      // 2. When mixing different arithmetic operations
      boolean needsParentheses =
          expr instanceof Division
              || (leftExpr instanceof BinaryExpression || rightExpr instanceof BinaryExpression);

      if (needsParentheses) {
        currentTransformedExpression = new Parenthesis(newExpr);
      } else {
        currentTransformedExpression = newExpr;
      }
    } catch (Exception e) {
      // Fallback to original expression if instantiation fails
      currentTransformedExpression = expr;
    }
  }
}
