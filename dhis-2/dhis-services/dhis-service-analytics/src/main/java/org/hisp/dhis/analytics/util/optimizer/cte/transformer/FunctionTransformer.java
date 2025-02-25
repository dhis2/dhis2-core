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
package org.hisp.dhis.analytics.util.optimizer.cte.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import org.hisp.dhis.analytics.util.optimizer.cte.ExpressionTransformer;

/**
 * A dedicated transformer for function-related logic, reducing code duplication by centralizing
 * common operations in a helper method.
 */
public class FunctionTransformer {
  private final ExpressionTransformer parentVisitor;

  private static final String FUNCTION_COALESCE = "coalesce";
  private static final String FUNCTION_EXTRACT = "extract";

  // A small record to store the processed expressions and a "changed" flag.
  record ProcessedExpressions(List<Expression> expressions, boolean hasChanges) {}

  public FunctionTransformer(ExpressionTransformer parentVisitor) {
    this.parentVisitor = parentVisitor;
  }

  /**
   * Entry point to transform any Function expression. Decides which specialized path to take based
   * on the function name, but uses a single helper method to avoid code duplication.
   */
  public Expression transform(Function function) {
    // If the function has no name, there's nothing special we can do
    if (function.getName() == null) {
      return function;
    }

    String nameLower = function.getName().toLowerCase();

    // Special handling for aggregate functions that might contain subqueries
    if (isAggregateFunction(nameLower)) {
      return transformAggregateFunction(function);
    }

    // Existing switch for other function types
    return switch (nameLower) {
      case FUNCTION_COALESCE ->
          // coalesce typically does not use brackets around parameters
          transformFunction(FUNCTION_COALESCE, false, function);
      case FUNCTION_EXTRACT ->
          // extract often uses brackets (e.g. EXTRACT(...) syntax)
          transformFunction(FUNCTION_EXTRACT, true, function);
      default ->
          // For any other function, keep the original name and
          // choose whether or not to use brackets (example: false).
          transformFunction(function.getName(), false, function);
    };
  }

  private boolean isAggregateFunction(String functionName) {
    return "avg".equals(functionName)
        || "sum".equals(functionName)
        || "count".equals(functionName)
        || "min".equals(functionName)
        || "max".equals(functionName);
  }

  /**
   * Centralized helper method that: 1. Processes existing parameters (recursively via the parent
   * visitor). 2. If no changes are detected, returns the original function. 3. If changes are
   * detected, creates a new Function object with the updated parameters, copying relevant flags.
   *
   * @param newName the function name to use in the new function
   * @param useBrackets whether to use brackets around parameters
   * @param original the original function object
   * @return either the original function (unchanged) or a new instance
   */
  private Expression transformFunction(String newName, boolean useBrackets, Function original) {
    if (original.getParameters() == null) {
      return original;
    }

    // Process all parameters via the main visitor
    List<Expression> currentParams = original.getParameters().getExpressions();
    ProcessedExpressions processed = processExpressions(currentParams);

    // If no parameters changed, just return the original function
    if (!processed.hasChanges()) {
      return original;
    }

    // Otherwise, build a new function with updated parameters
    Function newFunction = new Function();
    newFunction.setName(newName);
    // Copy flags from the original
    newFunction.setDistinct(original.isDistinct());
    newFunction.setAllColumns(original.isAllColumns());
    newFunction.setEscaped(original.isEscaped());

    // Set up the new parameter list
    ExpressionList paramList = new ExpressionList(processed.expressions());
    paramList.setUsingBrackets(useBrackets);
    newFunction.setParameters(paramList);

    return newFunction;
  }

  /**
   * Visits each expression parameter using the main visitor, returning a new list plus a
   * 'hasChanges' flag to indicate if any expression changed.
   */
  private ProcessedExpressions processExpressions(List<Expression> expressions) {
    List<Expression> nonNullExpressions = expressions.stream().filter(Objects::nonNull).toList();
    List<Expression> transformed = new ArrayList<>(nonNullExpressions.size());
    boolean changed = expressions.size() != nonNullExpressions.size();

    for (Expression expr : nonNullExpressions) {
      expr.accept(parentVisitor);
      Expression afterVisit = parentVisitor.getTransformedExpression();

      transformed.add(afterVisit);
      if (afterVisit != expr) {
        changed = true;
      }
    }
    return new ProcessedExpressions(transformed, changed);
  }

  private Expression transformAggregateFunction(Function function) {
    // Process parameters the same way as other functions
    ProcessedExpressions processed =
        processExpressions(
            function.getParameters() != null
                ? function.getParameters().getExpressions()
                : List.of());

    // If nothing changed, return the original
    if (!processed.hasChanges()) {
      return function;
    }

    // Create a new function with the processed parameters
    Function newFunction = new Function();
    newFunction.setName(function.getName());
    newFunction.setDistinct(function.isDistinct());
    newFunction.setAllColumns(function.isAllColumns());
    newFunction.setEscaped(function.isEscaped());

    ExpressionList paramList = new ExpressionList(processed.expressions());
    paramList.setUsingBrackets(true); // Aggregate functions typically use brackets
    newFunction.setParameters(paramList);

    return newFunction;
  }
}
