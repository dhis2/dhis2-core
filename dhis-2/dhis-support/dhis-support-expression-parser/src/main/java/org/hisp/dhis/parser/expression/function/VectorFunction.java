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
package org.hisp.dhis.parser.expression.function;

import static org.hisp.dhis.antlr.AntlrParserUtils.castClass;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.parser.expression.ExpressionState;
import org.hisp.dhis.period.Period;

/**
 * Aggregates a vector of samples (base class) of a given type.
 *
 * @author Jim Grace
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class VectorFunction<T> implements ExpressionItem {

  /**
   * This is the class of the type argument from any subclass that defines the type. At the time of
   * this writing Java does not provide an easy way to find this class by introspection. It is
   * possible but involves quite a bit of code. The easiest way is to have each of the subclasses
   * pass the class of the type argument when creating an instance of a given type.
   */
  private final Class<T> typeClass;

  @Override
  public Object getExpressionInfo(ExprContext ctx, CommonExpressionVisitor visitor) {
    // ItemIds in all but last expr (if any) are from current period.

    for (int i = 0; i < ctx.expr().size() - 1; i++) {
      visitor.visitExpr(ctx.expr().get(i));
    }

    return visitor.visitSamples(ctx.expr().get(ctx.expr().size() - 1));
  }

  @Override
  public Object evaluate(ExprContext ctx, CommonExpressionVisitor visitor) {
    List<T> args = new ArrayList<>();

    // All but last expr (if any) are from current period.

    for (int i = 0; i < ctx.expr().size() - 1; i++) {
      args.add(castToType(visitor.visitExpr(ctx.expr().get(i))));
    }

    // Last (or only) expr is from sampled periods.

    ExprContext lastExpr = ctx.expr().get(ctx.expr().size() - 1);

    return compute(lastExpr, visitor, args);
  }

  /**
   * Computes a vector (aggregation) function. This is exposed to enable other functions to call an
   * aggregation function if needed for their calculation.
   *
   * @param expr the expression as argument to the vector function
   * @param visitor the tree visitor
   * @param args any other function args (if any)
   * @return the vector function value
   */
  public Object compute(ExprContext expr, CommonExpressionVisitor visitor, List<T> args) {
    List<T> values = getSampleValues(expr, visitor);

    return vectorHandleNulls(aggregate(values, args), visitor);
  }

  /**
   * By default, if there is a null value, count it as a value found for the purpose of
   * missingValueStrategy. This can be overridden by a vector function (like count) that returns a
   * non-null value (0) if no actual values are found.
   *
   * @param value the value to count (might be null)
   * @param visitor the tree visitor
   * @return the value to return (null might be replaced)
   */
  public Object vectorHandleNulls(Object value, CommonExpressionVisitor visitor) {
    return visitor.getState().handleNulls(value, ValueType.NUMBER);
  }

  /**
   * Aggregates the values, using arguments (if any)
   *
   * @param args the values to aggregate.
   * @param args the arguments (if any) for aggregating the values.
   * @return the aggregated value.
   */
  public abstract Object aggregate(List<T> values, List<T> args);

  /**
   * Gets a list of sample values to aggregate.
   *
   * <p>The missingValueStrategy is handled as follows: for each sample expression inside the
   * aggregation function, if there are any sample values missing and the strategy is
   * SKIP_IF_ANY_VALUE_MISSING, then that sample is skipped. Also if all the values are missing and
   * the strategy is SKIP_IF_ALL_VALUES_MISSING, then that sample is skipped.
   *
   * <p>Finally, if there were any items in the sample expression, the count of items in the main
   * expression is incremented. And if there was at least one sample value, the count of item values
   * in the main expression is incremented. This means that if the vector is empty, it counts as a
   * missing value in the main expression.
   */
  private List<T> getSampleValues(ExprContext ctx, CommonExpressionVisitor visitor) {
    ExpressionState state = visitor.getState();

    int savedItemsFound = state.getItemsFound();
    int savedItemValuesFound = state.getItemValuesFound();

    List<T> values = visitSampledPeriods(ctx, visitor);

    if (state.getItemsFound() > 0) {
      savedItemsFound++;

      if (!values.isEmpty()) {
        savedItemValuesFound++;
      }
    }

    state.setItemsFound(savedItemsFound);
    state.setItemValuesFound(savedItemValuesFound);

    return values;
  }

  /** Visits each of the sample periods and compiles a list of the double values produced. */
  private List<T> visitSampledPeriods(ExprContext ctx, CommonExpressionVisitor visitor) {
    ExpressionParams params = visitor.getParams();
    ExpressionState state = visitor.getState();

    List<T> values = new ArrayList<>();

    for (Period p : params.getSamplePeriods()) {
      state.setItemsFound(0);
      state.setItemValuesFound(0);

      Map<DimensionalItemObject, Object> valueMap =
          firstNonNull(params.getPeriodValueMap().get(p), Collections.emptyMap());

      T value = visitWithValueMap(ctx, visitor, valueMap);

      if ((params.getMissingValueStrategy() == SKIP_IF_ANY_VALUE_MISSING
              && state.getItemValuesFound() < state.getItemsFound())
          || (params.getMissingValueStrategy() == SKIP_IF_ALL_VALUES_MISSING
              && state.getItemsFound() != 0
              && state.getItemValuesFound() == 0)) {
        value = null;
      }

      if (value != null) {
        values.add(value);
      }
    }

    return values;
  }

  /** Visits a subtree with an explicit valueMap. */
  private T visitWithValueMap(
      ExprContext ctx,
      CommonExpressionVisitor visitor,
      Map<DimensionalItemObject, Object> valueMap) {
    ExpressionParams savedParams = visitor.getParams();

    ExpressionParams params = visitor.getParams().toBuilder().valueMap(valueMap).build();

    visitor.setParams(params);

    T value = castToType(visitor.visit(ctx));

    visitor.setParams(savedParams);

    return value;
  }

  /** Casts to the type class. */
  @SuppressWarnings("unchecked")
  private T castToType(Object object) {
    return (T) castClass(typeClass, object);
  }
}
