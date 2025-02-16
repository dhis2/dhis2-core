package org.hisp.dhis.analytics.util.optimizer.cte.transformer;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import org.hisp.dhis.analytics.util.optimizer.cte.ExpressionTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * A dedicated transformer for function-related logic, reducing code duplication
 * by centralizing common operations in a helper method.
 */
public class FunctionTransformer {
    private final ExpressionTransformer parentVisitor;

    private static final String FUNCTION_COALESCE = "coalesce";
    private static final String FUNCTION_EXTRACT = "extract";


    // A small record to store the processed expressions and a "changed" flag.
    private record ProcessedExpressions(List<Expression> expressions, boolean hasChanges) {
    }

    public FunctionTransformer(ExpressionTransformer parentVisitor) {
        this.parentVisitor = parentVisitor;
    }

    /**
     * Entry point to transform any Function expression. Decides which
     * specialized path to take based on the function name, but uses a single
     * helper method to avoid code duplication.
     */
    public Expression transform(Function function) {
        // If the function has no name, there's nothing special we can do
        if (function.getName() == null) {
            return function;
        }

        String nameLower = function.getName().toLowerCase();

        // Decide bracket usage or function name overrides
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

    /**
     * Centralized helper method that:
     * 1. Processes existing parameters (recursively via the parent visitor).
     * 2. If no changes are detected, returns the original function.
     * 3. If changes are detected, creates a new Function object with
     * the updated parameters, copying relevant flags.
     *
     * @param newName     the function name to use in the new function
     * @param useBrackets whether to use brackets around parameters
     * @param original    the original function object
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
     * Visits each expression parameter using the main visitor, returning
     * a new list plus a 'hasChanges' flag to indicate if any expression changed.
     */
    private ProcessedExpressions processExpressions(List<Expression> expressions) {
        List<Expression> transformed = new ArrayList<>(expressions.size());
        boolean changed = false;

        for (Expression expr : expressions) {
            if (expr == null) {
                // Just add null if that’s valid in your context
                transformed.add(null);
                continue;
            }

            // Recursively visit the param expression using the parent visitor
            expr.accept(parentVisitor);
            Expression afterVisit = parentVisitor.getTransformedExpression();

            transformed.add(afterVisit);
            if (afterVisit != expr) {
                changed = true;
            }
        }
        return new ProcessedExpressions(transformed, changed);
    }
}
