package org.hisp.dhis.analytics.util.vis;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.hisp.dhis.analytics.util.optimizer.cte.ExpressionTransformer;
import org.hisp.dhis.analytics.util.optimizer.cte.data.FoundSubSelect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionTransformerTest {

    private ExpressionTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new ExpressionTransformer();
    }

    @Nested
    class FunctionTransformationTests {

        @Test
        void testCoalesce_HappyPath() {
            // Create a COALESCE function with multiple parameters
            Function coalesce = new Function();
            coalesce.setName("COALESCE");
            ExpressionList params = new ExpressionList();
            params.setExpressions(Arrays.asList(
                    new Column(new Table(), "col1"),
                    new StringValue("default")
            ));
            coalesce.setParameters(params);

            // Transform the expression
            coalesce.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            // Verify
            assertInstanceOf(Function.class, result);
            Function transformed = (Function) result;
            assertEquals("COALESCE", transformed.getName());
            assertEquals(2, transformed.getParameters().getExpressions().size());
        }

        @Test
        void testCoalesce_WithNullParameters() {
            Function coalesce = new Function();
            coalesce.setName("COALESCE");
            coalesce.setParameters(null);

            coalesce.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertNotNull(result);
            assertInstanceOf(Function.class, result);
            Function transformed = (Function) result;
            assertEquals("COALESCE", transformed.getName());
            assertNull(transformed.getParameters());
        }

        @Test
        void testRegularFunction_HappyPath() {
            Function function = new Function();
            function.setName("COUNT");
            ExpressionList params = new ExpressionList();
            params.setExpressions(List.of(new Column(new Table(), "id")));
            function.setParameters(params);

            function.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(Function.class, result);
            Function transformed = (Function) result;
            assertEquals("COUNT", transformed.getName());
            assertEquals(1, transformed.getParameters().getExpressions().size());
        }
    }

    @Nested
    class SubSelectTransformationTests {

        @Test
        void testSubSelect_LastSchedPattern() throws JSQLParserException {
            String sql = """
            SELECT scheduleddate
            FROM events
            WHERE events.enrollment = subax.enrollment
                AND scheduleddate IS NOT NULL
            ORDER BY occurreddate DESC
            LIMIT 1""";

            SubSelect subSelect = new SubSelect();
            subSelect.setSelectBody(((Select) CCJSqlParserUtil.parse(sql)).getSelectBody());

            subSelect.accept((ExpressionVisitor) transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(Column.class, result);
            Column transformed = (Column) result;
            assertEquals("ls", transformed.getTable().getName());
            assertEquals("scheduleddate", transformed.getColumnName());

            Map<SubSelect, FoundSubSelect> extracted = transformer.getExtractedSubSelects();
            assertEquals(1, extracted.size());
            assertTrue(extracted.containsKey(subSelect));
            assertEquals("last_sched", extracted.get(subSelect).name());
        }

        @Test
        void testSubSelect_LastCreatedPattern() throws JSQLParserException {
            String sql = """
            SELECT created
            FROM events
            WHERE events.enrollment = subax.enrollment
                AND created IS NOT NULL
            ORDER BY occurreddate DESC
            LIMIT 1""";

            SubSelect subSelect = new SubSelect();
            subSelect.setSelectBody(((Select) CCJSqlParserUtil.parse(sql)).getSelectBody());

            subSelect.accept((ExpressionVisitor) transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(Column.class, result);
            Column transformed = (Column) result;
            assertEquals("lc", transformed.getTable().getName());
            assertEquals("created", transformed.getColumnName());

            Map<SubSelect, FoundSubSelect> extracted = transformer.getExtractedSubSelects();
            assertEquals(1, extracted.size());
            assertTrue(extracted.containsKey(subSelect));
            assertEquals("last_created", extracted.get(subSelect).name());
        }

        @Test
        void testSubSelect_NonMatchingPattern() throws JSQLParserException {
            String sql = """
            SELECT different_column
            FROM some_table
            WHERE some_condition = true""";

            SubSelect subSelect = new SubSelect();
            subSelect.setSelectBody(((Select) CCJSqlParserUtil.parse(sql)).getSelectBody());

            subSelect.accept((ExpressionVisitor) transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(SubSelect.class, result);
            assertTrue(transformer.getExtractedSubSelects().isEmpty());
        }
    }

    @Nested
    class ArithmeticOperationTests {

        @Test
        void testAddition() throws JSQLParserException {
            String sql = "SELECT 5 + 3";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();
            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(Addition.class, result);
            Addition addition = (Addition) result;
            assertEquals("5", addition.getLeftExpression().toString());
            assertEquals("3", addition.getRightExpression().toString());
        }

        @Test
        void testNestedArithmetic() throws JSQLParserException {
            String sql = "SELECT (5 + 3) * 2";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(Multiplication.class, result);
            Multiplication mult = (Multiplication) result;
            assertInstanceOf(Addition.class, mult.getLeftExpression());
            assertEquals("2", mult.getRightExpression().toString());
        }
    }

    @Nested
    class ComparisonOperationTests {

        @Test
        void testEquals() throws JSQLParserException {
            String sql = "SELECT (col1 = 'value') FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(EqualsTo.class, result);
            EqualsTo equals = (EqualsTo) result;
            assertEquals("col1", equals.getLeftExpression().toString());
            assertEquals("'value'", equals.getRightExpression().toString());
        }
        @Test
        void testBetween() throws JSQLParserException {
            String sql = "SELECT col1 BETWEEN 1 AND 10";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(Between.class, result);
            Between between = (Between) result;
            assertEquals("col1", between.getLeftExpression().toString());
            assertEquals("1", between.getBetweenExpressionStart().toString());
            assertEquals("10", between.getBetweenExpressionEnd().toString());
        }
    }

    @Nested
    class LogicalOperationTests {

        @Test
        void testAndOperation() throws JSQLParserException {
            String sql = "SELECT (col1 > 0 AND col2 = 'value') from dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(AndExpression.class, result);
            AndExpression and = (AndExpression) result;
            assertInstanceOf(GreaterThan.class, and.getLeftExpression());
            assertInstanceOf(EqualsTo.class, and.getRightExpression());
        }

        @Test
        void testOrOperation() throws JSQLParserException {
            String sql = "SELECT (col1 IS NULL OR col2 != 'value') FROM dual";

            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(OrExpression.class, result);
            OrExpression or = (OrExpression) result;
            assertInstanceOf(IsNullExpression.class, or.getLeftExpression());
            assertInstanceOf(NotEqualsTo.class, or.getRightExpression());
        }

        @Test
        void testComplexLogicalExpression() throws JSQLParserException {
            String sql = "SELECT (col1 > 0 AND col2 = 'value') OR (col3 IS NULL AND col4 < 100)";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(OrExpression.class, result);
            OrExpression or = (OrExpression) result;
            assertInstanceOf(AndExpression.class, or.getLeftExpression());
            assertInstanceOf(AndExpression.class, or.getRightExpression());
        }
    }

    @Nested
    class SpecialOperationsTests {

        @Test
        void testIsNull() throws JSQLParserException {
            String sql = "SELECT col1 IS NULL";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();
            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(IsNullExpression.class, result);
            IsNullExpression isNull = (IsNullExpression) result;
            assertEquals("col1", isNull.getLeftExpression().toString());
            assertFalse(isNull.isNot());
        }

        @Test
        void testInExpression() throws JSQLParserException {
            String sql = "SELECT col1 IN (1, 2, 3)";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(InExpression.class, result);
            InExpression in = (InExpression) result;
            assertEquals("col1", in.getLeftExpression().toString());
            assertFalse(in.isNot());
        }
    }

    @Nested
    class AdvancedTransformationTests {

        @Test
        void testNestedLogicalOperations() throws JSQLParserException {
            String sql = "SELECT ((col1 > 0 AND col2 < 10) OR (col3 = 'value' AND col4 IS NOT NULL)) FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(OrExpression.class, result);
            OrExpression or = (OrExpression) result;
            assertInstanceOf(AndExpression.class, or.getLeftExpression());
            assertInstanceOf(AndExpression.class, or.getRightExpression());
        }

        @Test
        void testMultipleArithmeticOperations() throws JSQLParserException {
            String sql = "SELECT (col1 + 5) * (col2 - 3) / 2 FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(Division.class, result);
            Division div = (Division) result;
            assertInstanceOf(Multiplication.class, div.getLeftExpression());
            assertEquals("2", div.getRightExpression().toString());
        }

        @Test
        void testNotExpression() throws JSQLParserException {
            String sql = "SELECT (NOT col1 = 'value') FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(NotExpression.class, result);
            NotExpression not = (NotExpression) result;
            assertInstanceOf(EqualsTo.class, not.getExpression());
        }
    }

    @Nested
    class CornerCasesTests {

        @Test
        void testNullFunction() throws JSQLParserException {
            String sql = "SELECT COALESCE(NULL, col1, 'default') FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(Function.class, result);
            Function func = (Function) result;
            assertEquals("COALESCE", func.getName());
            assertEquals(3, func.getParameters().getExpressions().size());
        }

        @Test
        void testEmptyFunction() throws JSQLParserException {
            String sql = "SELECT COUNT() FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(Function.class, result);
            Function func = (Function) result;
            assertEquals("COUNT", func.getName());
            assertNull(func.getParameters());
        }

        @Test
        void testIsNullWithNot() throws JSQLParserException {
            String sql = "SELECT (col1 IS NOT NULL) FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(IsNullExpression.class, result);
            IsNullExpression isNull = (IsNullExpression) result;
            assertTrue(isNull.isNot());
            assertEquals("col1", isNull.getLeftExpression().toString());
        }

        @Test
        void testInExpressionWithSubselect() throws JSQLParserException {
            String sql = """
                SELECT (col1 IN (
                    SELECT scheduleddate 
                    FROM events 
                    WHERE events.enrollment = subax.enrollment 
                    AND scheduleddate IS NOT NULL 
                    ORDER BY occurreddate DESC 
                    LIMIT 1
                )) FROM dual""";

            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            // First verify we got a result
            assertNotNull(result, "Transformed expression should not be null");
            System.out.println("Actual result type: " + result.getClass().getName());
            System.out.println("Actual result: " + result);

            // Then verify it's an IN expression
            assertInstanceOf(InExpression.class, result);
            InExpression in = (InExpression) result;

            // Verify left expression
            assertNotNull(in.getLeftExpression(), "Left expression should not be null");
            assertEquals("col1", in.getLeftExpression().toString());

            // Verify right expression
            assertNotNull(in.getRightExpression(), "Right expression should not be null");
            // Print actual right expression for debugging
            System.out.println("Right expression type: " +
                    (in.getRightExpression() != null ? in.getRightExpression().getClass().getName() : "null"));
        }
        @Test
        void testBetweenWithArithmetic() throws JSQLParserException {
            String sql = "SELECT (col1 BETWEEN col2 + 1 AND col3 * 2) FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(Between.class, result);
            Between between = (Between) result;
            assertEquals("col1", between.getLeftExpression().toString());
            assertInstanceOf(Addition.class, between.getBetweenExpressionStart());
            assertInstanceOf(Multiplication.class, between.getBetweenExpressionEnd());
        }

        @Test
        void testCastExpression() throws JSQLParserException {
            String sql = "SELECT CAST(col1 AS VARCHAR) FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(CastExpression.class, result);
            CastExpression cast = (CastExpression) result;
            assertEquals("col1", cast.getLeftExpression().toString());
            assertEquals("VARCHAR", cast.getType().getDataType());
        }
    }

    @Nested
    class InExpressionTests {

        @Test
        void testSimpleInExpression() throws JSQLParserException {
            String sql = "SELECT (col1 IN (1, 2, 3)) FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(InExpression.class, result);
            InExpression in = (InExpression) result;
            assertEquals("col1", in.getLeftExpression().toString());
            assertNotNull(in.getRightItemsList(), "Right items list should not be null");
        }

        @Test
        void testInExpressionWithSubselect() throws JSQLParserException {
            String sql = """
            SELECT (col1 IN (SELECT id FROM table1)) FROM dual""";

            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(InExpression.class, result);
            InExpression in = (InExpression) result;
            assertEquals("col1", in.getLeftExpression().toString());
            assertNotNull(in.getRightExpression(), "Right expression should not be null");
            assertInstanceOf(SubSelect.class, in.getRightExpression());
        }
    }

    @Nested
    class ValueTests {

        @Test
        void testStringValue() throws JSQLParserException {
            String sql = "SELECT 'test string' FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(StringValue.class, result);
            assertEquals("'test string'", result.toString());
        }

        @Test
        void testLongValue() throws JSQLParserException {
            String sql = "SELECT 12345 FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(LongValue.class, result);
            assertEquals("12345", result.toString());
        }

        @Test
        void testDoubleValue() throws JSQLParserException {
            String sql = "SELECT 123.45 FROM dual";
            Expression expr = ((SelectExpressionItem) ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql))
                    .getSelectBody()).getSelectItems().get(0)).getExpression();

            expr.accept(transformer);
            Expression result = transformer.getTransformedExpression();

            assertInstanceOf(DoubleValue.class, result);
            assertEquals("123.45", result.toString());
        }
    }
}