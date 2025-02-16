package org.hisp.dhis.analytics.util.optimizer.cte;

import lombok.Getter;
import net.sf.jsqlparser.expression.BinaryExpression;
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
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hisp.dhis.analytics.util.optimizer.cte.matcher.SubselectMatchers.matchesDataElementCountPattern;
import static org.hisp.dhis.analytics.util.optimizer.cte.matcher.SubselectMatchers.matchesLastCreatedExistsPattern;
import static org.hisp.dhis.analytics.util.optimizer.cte.matcher.SubselectMatchers.matchesLastEventValuePattern;
import static org.hisp.dhis.analytics.util.optimizer.cte.matcher.SubselectMatchers.matchesLastSchedPattern;
import static org.hisp.dhis.analytics.util.optimizer.cte.matcher.SubselectMatchers.matchesRelationshipCountPattern;

@Getter
public class ExpressionTransformer extends ExpressionVisitorAdapter {
    private final Map<SubSelect, FoundSubSelect> extractedSubSelects = new LinkedHashMap<>();
    private Expression currentTransformedExpression;

    @Override
    public void visit(Function function) {
        if ("coalesce".equalsIgnoreCase(function.getName())) {
            visitCoalesce(function);
        } else if ("extract".equalsIgnoreCase(function.getName())) {
            visitExtract(function);
        } else {
            visitRegularFunction(function);
        }
    }

    private void visitCoalesce(Function function) {
        if (function.getParameters() == null) {
            currentTransformedExpression = function;
            return;
        }

        List<Expression> currentParams = function.getParameters().getExpressions();
        List<Expression> newParams = new ArrayList<>();
        boolean hasChanges = false;

        for (Expression param : currentParams) {
            if (param != null) {
                param.accept(this);
                if (currentTransformedExpression != null) {
                    newParams.add(currentTransformedExpression);
                    if (currentTransformedExpression != param) {
                        hasChanges = true;
                    }
                }
            }
        }

        Function newCoalesce = new Function();
        newCoalesce.setName("coalesce");
        ExpressionList paramList = new ExpressionList();
        paramList.setExpressions(newParams);
        paramList.setUsingBrackets(false);  // Try to avoid extra brackets
        newCoalesce.setParameters(paramList);
        currentTransformedExpression = hasChanges ? newCoalesce : function;
    }

    private void visitExtract(Function function) {
        List<Expression> currentParams = function.getParameters().getExpressions();
        List<Expression> newParams = new ArrayList<>();
        boolean hasChanges = false;

        // Process each parameter of the EXTRACT function
        for (Expression param : currentParams) {
            if (param != null) {
                param.accept(this);
                if (currentTransformedExpression != null) {
                    newParams.add(currentTransformedExpression);
                    if (currentTransformedExpression != param) {
                        hasChanges = true;
                    }
                } else {
                    newParams.add(param);
                }
            }
        }

        // Create new EXTRACT function with transformed parameters
        Function newExtract = new Function();
        newExtract.setName("extract");
        ExpressionList paramList = new ExpressionList();
        paramList.setExpressions(newParams);
        paramList.setUsingBrackets(true);
        newExtract.setParameters(paramList);

        // If this EXTRACT is part of a division expression, wrap it in parentheses
        currentTransformedExpression = hasChanges ? newExtract : function;
    }

    private void visitRegularFunction(Function function) {
        if (function.getParameters() == null) {
            currentTransformedExpression = function;
            return;
        }

        List<Expression> currentParams = function.getParameters().getExpressions();
        List<Expression> newParams = new ArrayList<>();
        boolean hasChanges = false;

        for (Expression param : currentParams) {
            if (param != null) {
                param.accept(this);
                if (currentTransformedExpression != null) {
                    newParams.add(currentTransformedExpression);
                    if (currentTransformedExpression != param) {
                        hasChanges = true;
                    }
                }
            }
        }

        Function newFunction = new Function();
        newFunction.setName(function.getName());
        newFunction.setAllColumns(function.isAllColumns());
        newFunction.setDistinct(function.isDistinct());
        newFunction.setEscaped(function.isEscaped());

        ExpressionList paramList = new ExpressionList();
        paramList.setExpressions(newParams);
        paramList.setUsingBrackets(false);  // Try to avoid extra brackets
        newFunction.setParameters(paramList);

        currentTransformedExpression = hasChanges ? newFunction : function;
    }

    @Override
    public void visit(ExtractExpression extract) {
        // Visit the expression being extracted from
        extract.getExpression().accept(this);
        Expression transformedExpr = currentTransformedExpression;

        // Create new extract expression
        ExtractExpression newExtract = new ExtractExpression();
        newExtract.setName(extract.getName());  // 'epoch'
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

    @Override
    public void visit(Parenthesis parenthesis) {
        parenthesis.getExpression().accept(this);
        Expression transformedExpr = currentTransformedExpression;

        currentTransformedExpression = new Parenthesis(transformedExpr);
    }

    // Logical Operators
    @Override
    public void visit(AndExpression andExpression) {
        handleBinaryExpression(andExpression);
    }

    @Override
    public void visit(OrExpression orExpression) {
        handleBinaryExpression(orExpression);
    }

    @Override
    public void visit(CastExpression cast) {
        cast.getLeftExpression().accept(this);
        Expression transformedExpr = currentTransformedExpression;

        CastExpression newCast = new CastExpression();
        newCast.setLeftExpression(transformedExpr);
        newCast.setType(cast.getType());

        currentTransformedExpression = newCast;
    }

    @Override
    public void visit(SubSelect subSelect) {
        if (subSelect == null || subSelect.getSelectBody() == null) {
            currentTransformedExpression = subSelect;
            return;
        }

        Optional<FoundSubSelect> matchedPattern = matchesLastSchedPattern(subSelect)
                .or(() -> matchesLastCreatedExistsPattern(subSelect))
                .or(() -> matchesLastEventValuePattern(subSelect))
                .or(() -> matchesRelationshipCountPattern(subSelect))
                .or(() -> matchesDataElementCountPattern(subSelect));

        if (matchedPattern.isPresent()) {
            FoundSubSelect found = matchedPattern.get();
            extractedSubSelects.put(subSelect, found);

            // Determine the appropriate alias based on the CTE name
            String alias = switch (found.name()) {
                case "last_sched" -> "ls";
                case "last_created" -> "lc";
                case "relationship_count", "relationship_count_agg" -> "rlc";
                default -> {
                    // For last_value_* patterns, use lv_columnname
                    if (found.name().startsWith("last_value_")) {
                        yield "lv_" + preserveLetterNumbers(found.columnReference());
                    }
                    if (found.name().startsWith("de_count_")) {
                        yield "dec_" + preserveLetterNumbers(found.columnReference());
                    }
                    yield found.name(); // fallback
                }
            };

            currentTransformedExpression = new Column(new Table(alias), found.columnReference());
        } else {
            currentTransformedExpression = subSelect;
        }
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

    // Helper method for binary expressions
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

    // Helper method for arithmetic operators
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
            boolean needsParentheses = expr instanceof Division ||
                    (leftExpr instanceof BinaryExpression ||
                            rightExpr instanceof BinaryExpression);

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

    private String preserveLetterNumbers(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }
}