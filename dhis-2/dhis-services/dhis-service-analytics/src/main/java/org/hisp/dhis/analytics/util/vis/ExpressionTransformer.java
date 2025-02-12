package org.hisp.dhis.analytics.util.vis;

import lombok.Getter;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hisp.dhis.analytics.util.vis.SubselectMatchers.matchesLastCreatedExistsPattern;
import static org.hisp.dhis.analytics.util.vis.SubselectMatchers.matchesLastEventValuePattern;
import static org.hisp.dhis.analytics.util.vis.SubselectMatchers.matchesLastSchedPattern;
import static org.hisp.dhis.analytics.util.vis.SubselectMatchers.matchesRelationshipCountPattern;

@Getter
public class ExpressionTransformer extends ExpressionVisitorAdapter {
    private final Map<SubSelect, FoundSubSelect> extractedSubSelects = new LinkedHashMap<>();
    private Expression currentTransformedExpression;

    @Override
    public void visit(Function function) {
        if ("coalesce".equalsIgnoreCase(function.getName())) {
            visitCoalesce(function);
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

    // Arithmetic Operators
    @Override
    public void visit(Addition addition) {
        handleBinaryExpression(addition);
    }

    @Override
    public void visit(Multiplication multiplication) {
        handleBinaryExpression(multiplication);
    }

    @Override
    public void visit(Subtraction subtraction) {
        handleBinaryExpression(subtraction);
    }

    @Override
    public void visit(Division division) {
        handleBinaryExpression(division);
    }

    @Override
    public void visit(Modulo modulo) {
        handleBinaryExpression(modulo);
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

        Optional<FoundSubSelect> lastSched = matchesLastSchedPattern(subSelect);
        Optional<FoundSubSelect> lastCreated = matchesLastCreatedExistsPattern(subSelect);
        Optional<FoundSubSelect> lastEvent = matchesLastEventValuePattern(subSelect);
        Optional<FoundSubSelect> relCount = matchesRelationshipCountPattern(subSelect);

        Optional<FoundSubSelect> matchedPattern = lastSched
                .or(() -> lastCreated)
                .or(() -> lastEvent)
                .or(() -> relCount);

        if (matchedPattern.isPresent()) {
            FoundSubSelect found = matchedPattern.get();
            extractedSubSelects.put(subSelect, found);

            // Determine the appropriate alias based on the CTE name
            String alias = switch (found.name()) {
                case "last_sched" -> "ls";
                case "last_created" -> "lc";
                case "relationship_count" -> "rc";
                default -> {
                    // For last_value_* patterns, use lv_columnname
                    if (found.name().startsWith("last_value_")) {
                        yield "lv_" + preserveLetterNumbers(found.columnReference());
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

    private String preserveLetterNumbers(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }
}