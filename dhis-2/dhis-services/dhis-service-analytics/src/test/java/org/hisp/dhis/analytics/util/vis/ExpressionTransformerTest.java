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
package org.hisp.dhis.analytics.util.vis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
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
      params.setExpressions(
          Arrays.asList(new Column(new Table(), "col1"), new StringValue("default")));
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
      String sql =
          """
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
      String sql =
          """
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
      String sql =
          """
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

    @Test
    void testSubSelect_LastEventValuePattern() throws JSQLParserException {
      String sql =
          """
          SELECT "de_xyz"
          FROM events
          WHERE events.enrollment = subax.enrollment
          AND "de_xyz" IS NOT NULL
          AND ps = 'stage1'
          ORDER BY occurreddate DESC
          LIMIT 1""";

      SubSelect subSelect = new SubSelect();
      subSelect.setSelectBody(((Select) CCJSqlParserUtil.parse(sql)).getSelectBody());

      subSelect.accept((ExpressionVisitor) transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the transformation to a column reference
      assertInstanceOf(
          Column.class, result, "Expression should be transformed to a Column reference");
      Column transformed = (Column) result;
      assertEquals(
          "lv_dexyz", transformed.getTable().getName(), "Table alias should be 'lv_dexyz'");
      assertEquals("\"de_xyz\"", transformed.getColumnName(), "Column name should be 'de_xyz'");

      // Verify the captured subselect
      Map<SubSelect, FoundSubSelect> extracted = transformer.getExtractedSubSelects();
      assertEquals(1, extracted.size(), "Should have captured exactly one subselect");
      assertTrue(extracted.containsKey(subSelect), "Should contain the original subselect");

      FoundSubSelect found = extracted.get(subSelect);
      assertEquals(
          "last_value_dexyz", found.name(), "Pattern should be identified as last_value_dexyz");
      assertEquals("\"de_xyz\"", found.columnReference(), "Column reference should be 'de_xyz'");
    }

    @Test
    void testSubSelect_RelationshipCountPattern() throws JSQLParserException {
      String sql =
          """
          SELECT relationship_count
          FROM analytics_rs_relationship
          WHERE trackedentityid = subax.trackedentity
          AND relationshiptypeuid = 'abc'""";

      SubSelect subSelect = new SubSelect();
      subSelect.setSelectBody(((Select) CCJSqlParserUtil.parse(sql)).getSelectBody());

      subSelect.accept((ExpressionVisitor) transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the transformation to a column reference
      assertInstanceOf(
          Column.class, result, "Expression should be transformed to a Column reference");
      Column transformed = (Column) result;
      assertEquals("rlc", transformed.getTable().getName(), "Table alias should be 'rlc'");
      assertEquals(
          "relationship_count",
          transformed.getColumnName(),
          "Column name should be 'relationship_count'");

      // Verify the captured subselect
      Map<SubSelect, FoundSubSelect> extracted = transformer.getExtractedSubSelects();
      assertEquals(1, extracted.size(), "Should have captured exactly one subselect");
      assertTrue(extracted.containsKey(subSelect), "Should contain the original subselect");

      FoundSubSelect found = extracted.get(subSelect);
      assertEquals(
          "relationship_count", found.name(), "Pattern should be identified as relationship_count");
      assertEquals(
          "relationship_count",
          found.columnReference(),
          "Column reference should be 'relationship_count'");

      // Verify metadata
      Map<String, String> metadata = found.metadata();
      assertNotNull(metadata, "Metadata should not be null");
      assertEquals(
          "abc",
          metadata.get("relationshipTypeUid"),
          "Relationship type UID should be captured in metadata");
      assertEquals("false", metadata.get("isAggregated"), "Should not be marked as aggregated");
    }

    @Test
    void testSubSelect_DataElementCountPattern() throws JSQLParserException {
      String sql =
          """
          SELECT count(de_123456)
          FROM analytics_event ae
          WHERE ae.ps = 'xyz'
          AND ae.enrollment = subax.enrollment
          AND de_123456 IS NOT NULL
          AND de_123456 = '1'""";

      SubSelect subSelect = new SubSelect();
      subSelect.setSelectBody(((Select) CCJSqlParserUtil.parse(sql)).getSelectBody());

      subSelect.accept((ExpressionVisitor) transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the transformation to a column reference
      assertInstanceOf(
          Column.class, result, "Expression should be transformed to a Column reference");
      Column transformed = (Column) result;
      assertEquals(
          "dec_decount", transformed.getTable().getName(), "Table alias should be 'dec_decount'");
      assertEquals("de_count", transformed.getColumnName(), "Column name should be 'de_count'");

      // Verify the captured subselect
      Map<SubSelect, FoundSubSelect> extracted = transformer.getExtractedSubSelects();
      assertEquals(1, extracted.size(), "Should have captured exactly one subselect");
      assertTrue(extracted.containsKey(subSelect), "Should contain the original subselect");

      FoundSubSelect found = extracted.get(subSelect);
      assertEquals(
          "de_count_de123456", found.name(), "Pattern should be identified as de_count_de123456");
      assertEquals("de_count", found.columnReference(), "Column reference should be 'de_count'");
    }

    @Test
    void testSubSelect_NonMatchingPatterns() throws JSQLParserException {
      // Test cases that should not match any pattern
      String[] nonMatchingQueries = {
        // Modified last event value pattern
        """
        SELECT value
        FROM programstageinstance
        WHERE programstageinstance.programstage = 'xyz'
        -- Missing IS NOT NULL check
        ORDER BY executiondate DESC
        LIMIT 1""",

        // Modified relationship count pattern
        """
        SELECT count(*)
        FROM relationship r
        -- Missing required joins
        WHERE r.relationshiptypeid = 'abc'""",

        // Modified data element count pattern
        """
        SELECT count(*)
        FROM programstageinstance psi
        -- Missing required conditions
        WHERE psi.programstageid = 'xyz'"""
      };

      for (String sql : nonMatchingQueries) {
        SubSelect subSelect = new SubSelect();
        subSelect.setSelectBody(((Select) CCJSqlParserUtil.parse(sql)).getSelectBody());

        subSelect.accept((ExpressionVisitor) transformer);
        Expression result = transformer.getTransformedExpression();

        // Assert that non-matching patterns remain as SubSelect
        assertInstanceOf(
            SubSelect.class, result, "Non-matching pattern should remain as SubSelect: " + sql);
        assertTrue(
            transformer.getExtractedSubSelects().isEmpty(),
            "No subselects should be extracted for non-matching pattern: " + sql);
      }
    }
  }

  @Nested
  class ArithmeticOperationTests {

    @Test
    void testAddition() throws JSQLParserException {
      String sql = "SELECT 5 + 3";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();
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
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the multiplication operation
      assertInstanceOf(
          Multiplication.class, result, "Top level expression should be multiplication");
      Multiplication mult = (Multiplication) result;

      // Assert the left side (parenthesized addition)
      assertInstanceOf(
          Parenthesis.class,
          mult.getLeftExpression(),
          "Left side should be a parenthesized expression");
      Parenthesis paren = (Parenthesis) mult.getLeftExpression();

      // Assert the addition inside parentheses
      assertInstanceOf(
          Addition.class,
          paren.getExpression(),
          "Expression inside parentheses should be addition");
      Addition add = (Addition) paren.getExpression();
      assertEquals("5", add.getLeftExpression().toString(), "Left side of addition should be 5");
      assertEquals("3", add.getRightExpression().toString(), "Right side of addition should be 3");

      // Assert the right side (2)
      assertEquals(
          "2", mult.getRightExpression().toString(), "Right side of multiplication should be 2");
    }
  }

  @Nested
  class ComparisonOperationTests {

    @Test
    void testEquals() throws JSQLParserException {
      String sql = "SELECT (col1 = 'value') FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the parenthesis wrapper
      assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in parentheses");
      Parenthesis paren = (Parenthesis) result;

      // Assert equals expression inside the parentheses
      assertInstanceOf(
          EqualsTo.class, paren.getExpression(), "Expression inside parentheses should be EQUALS");
      EqualsTo equals = (EqualsTo) paren.getExpression();

      // Assert left side
      assertEquals("col1", equals.getLeftExpression().toString(), "Left side should be col1");

      // Assert right side
      assertEquals(
          "'value'", equals.getRightExpression().toString(), "Right side should be 'value'");
    }

    @Test
    void testBetween() throws JSQLParserException {
      String sql = "SELECT col1 BETWEEN 1 AND 10";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

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
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the parenthesis wrapper
      assertInstanceOf(
          Parenthesis.class, result, "Result should be wrapped in parentheses like original SQL");
      Parenthesis paren = (Parenthesis) result;

      // Assert the AND expression inside the parentheses
      assertInstanceOf(
          AndExpression.class,
          paren.getExpression(),
          "Expression inside parentheses should be AND");
      AndExpression and = (AndExpression) paren.getExpression();

      // Assert the left side (>)
      assertInstanceOf(
          GreaterThan.class,
          and.getLeftExpression(),
          "Left side should be GREATER THAN expression");
      GreaterThan greaterThan = (GreaterThan) and.getLeftExpression();
      assertEquals(
          "col1",
          greaterThan.getLeftExpression().toString(),
          "GREATER THAN should be comparing col1");
      assertEquals(
          "0",
          greaterThan.getRightExpression().toString(),
          "GREATER THAN should be comparing with 0");

      // Assert the right side (=)
      assertInstanceOf(
          EqualsTo.class, and.getRightExpression(), "Right side should be EQUALS expression");
      EqualsTo equals = (EqualsTo) and.getRightExpression();
      assertEquals(
          "col2", equals.getLeftExpression().toString(), "EQUALS should be comparing col2");
      assertEquals(
          "'value'",
          equals.getRightExpression().toString(),
          "EQUALS should be comparing with 'value'");
    }

    @Test
    void testOrOperation() throws JSQLParserException {
      String sql = "SELECT (col1 IS NULL OR col2 != 'value') FROM dual";

      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the parenthesis wrapper
      assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in parentheses");
      Parenthesis paren = (Parenthesis) result;

      // Assert the OR expression inside the parentheses
      assertInstanceOf(
          OrExpression.class, paren.getExpression(), "Expression inside parentheses should be OR");
      OrExpression or = (OrExpression) paren.getExpression();

      // Assert the left side (IS NULL)
      assertInstanceOf(
          IsNullExpression.class, or.getLeftExpression(), "Left side should be IS NULL expression");
      IsNullExpression isNull = (IsNullExpression) or.getLeftExpression();
      assertEquals(
          "col1", isNull.getLeftExpression().toString(), "IS NULL should be checking col1");

      // Assert the right side (!=)
      assertInstanceOf(
          NotEqualsTo.class, or.getRightExpression(), "Right side should be NOT EQUALS expression");
      NotEqualsTo notEquals = (NotEqualsTo) or.getRightExpression();
      assertEquals(
          "col2", notEquals.getLeftExpression().toString(), "NOT EQUALS should be comparing col2");
      assertEquals(
          "'value'",
          notEquals.getRightExpression().toString(),
          "NOT EQUALS should be comparing with 'value'");
    }

    @Test
    void testComplexLogicalExpression() throws JSQLParserException {
      String sql = "SELECT (col1 > 0 AND col2 = 'value') OR (col3 IS NULL AND col4 < 100)";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      // Add debug output
      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the structure
      assertInstanceOf(OrExpression.class, result, "Top level expression should be OR");
      OrExpression or = (OrExpression) result;

      // Check left side of OR
      Expression leftExpr = or.getLeftExpression();
      assertInstanceOf(
          Parenthesis.class, leftExpr, "Left side of OR should be wrapped in parentheses");
      assertInstanceOf(
          AndExpression.class,
          ((Parenthesis) leftExpr).getExpression(),
          "Inside left parentheses should be AND expression");

      // Check right side of OR
      Expression rightExpr = or.getRightExpression();
      assertInstanceOf(
          Parenthesis.class, rightExpr, "Right side of OR should be wrapped in parentheses");
      assertInstanceOf(
          AndExpression.class,
          ((Parenthesis) rightExpr).getExpression(),
          "Inside right parentheses should be AND expression");

      AndExpression leftAnd = (AndExpression) ((Parenthesis) leftExpr).getExpression();
      assertInstanceOf(
          GreaterThan.class, leftAnd.getLeftExpression(), "First condition should be greater than");
      assertInstanceOf(
          EqualsTo.class, leftAnd.getRightExpression(), "Second condition should be equals");

      AndExpression rightAnd = (AndExpression) ((Parenthesis) rightExpr).getExpression();
      assertInstanceOf(
          IsNullExpression.class,
          rightAnd.getLeftExpression(),
          "Third condition should be IS NULL");
      assertInstanceOf(
          MinorThan.class, rightAnd.getRightExpression(), "Fourth condition should be less than");
    }
  }

  @Nested
  class SpecialOperationsTests {

    @Test
    void testIsNull() throws JSQLParserException {
      String sql = "SELECT col1 IS NULL";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();
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
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

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
      String sql =
          "SELECT ((col1 > 0 AND col2 < 10) OR (col3 = 'value' AND col4 IS NOT NULL)) FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert outer parenthesis
      assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in outer parentheses");
      Parenthesis outerParen = (Parenthesis) result;

      // Assert the OR expression inside outer parentheses
      assertInstanceOf(
          OrExpression.class,
          outerParen.getExpression(),
          "Expression inside outer parentheses should be OR");
      OrExpression or = (OrExpression) outerParen.getExpression();

      // Assert left side of OR (first AND expression)
      assertInstanceOf(
          Parenthesis.class, or.getLeftExpression(), "Left side of OR should be parenthesized");
      Parenthesis leftParen = (Parenthesis) or.getLeftExpression();
      assertInstanceOf(
          AndExpression.class,
          leftParen.getExpression(),
          "Expression inside left parentheses should be AND");

      // Assert right side of OR (second AND expression)
      assertInstanceOf(
          Parenthesis.class, or.getRightExpression(), "Right side of OR should be parenthesized");
      Parenthesis rightParen = (Parenthesis) or.getRightExpression();
      assertInstanceOf(
          AndExpression.class,
          rightParen.getExpression(),
          "Expression inside right parentheses should be AND");

      // Optionally verify the conditions inside each AND
      AndExpression leftAnd = (AndExpression) leftParen.getExpression();
      assertInstanceOf(
          GreaterThan.class, leftAnd.getLeftExpression(), "First condition should be greater than");
      assertInstanceOf(
          MinorThan.class, leftAnd.getRightExpression(), "Second condition should be less than");

      AndExpression rightAnd = (AndExpression) rightParen.getExpression();
      assertInstanceOf(
          EqualsTo.class, rightAnd.getLeftExpression(), "Third condition should be equals");
      assertInstanceOf(
          IsNullExpression.class,
          rightAnd.getRightExpression(),
          "Fourth condition should be IS NOT NULL");
    }

    @Test
    void testMultipleArithmeticOperations() throws JSQLParserException {
      String sql = "SELECT (col1 + 5) * (col2 - 3) / 2 FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the division operation
      Division div = findFirstDivision(result);
      assertNotNull(div, "Should find a division operation in the expression");

      // Assert the multiplication on the left side of division
      assertInstanceOf(
          Multiplication.class,
          div.getLeftExpression(),
          "Left side of division should be multiplication");
      Multiplication mult = (Multiplication) div.getLeftExpression();

      // Assert the first parenthesized addition (col1 + 5)
      Expression leftMult = mult.getLeftExpression();
      assertInstanceOf(
          Parenthesis.class, leftMult, "Left multiplication operand should be parenthesized");
      Parenthesis leftParen = (Parenthesis) leftMult;
      assertInstanceOf(
          Addition.class, leftParen.getExpression(), "Inside left parentheses should be addition");

      // Assert the second parenthesized subtraction (col2 - 3)
      Expression rightMult = mult.getRightExpression();
      assertInstanceOf(
          Parenthesis.class, rightMult, "Right multiplication operand should be parenthesized");
      Parenthesis rightParen = (Parenthesis) rightMult;
      assertInstanceOf(
          Subtraction.class,
          rightParen.getExpression(),
          "Inside right parentheses should be subtraction");

      // Assert the right side of division (2)
      assertEquals("2", div.getRightExpression().toString(), "Right side of division should be 2");
    }

    @Test
    void testNotExpression() throws JSQLParserException {
      String sql = "SELECT (NOT col1 = 'value') FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the parenthesis wrapper
      assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in parentheses");
      Parenthesis paren = (Parenthesis) result;

      // Assert the NOT expression inside the parentheses
      assertInstanceOf(
          NotExpression.class,
          paren.getExpression(),
          "Expression inside parentheses should be NOT");
      NotExpression not = (NotExpression) paren.getExpression();

      // Assert the equals expression inside the NOT
      assertInstanceOf(
          EqualsTo.class, not.getExpression(), "Expression inside NOT should be EQUALS");
      EqualsTo equals = (EqualsTo) not.getExpression();

      // Assert the components of the equals expression
      assertEquals(
          "col1", equals.getLeftExpression().toString(), "Left side of EQUALS should be col1");
      assertEquals(
          "'value'",
          equals.getRightExpression().toString(),
          "Right side of EQUALS should be 'value'");
    }
  }

  @Nested
  class CornerCasesTests {

    @Test
    void testNullFunction() throws JSQLParserException {
      String sql = "SELECT COALESCE(NULL, col1, 'default') FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      assertInstanceOf(Function.class, result);
      Function func = (Function) result;
      assertEquals("coalesce", func.getName());
      assertEquals(3, func.getParameters().getExpressions().size());
    }

    @Test
    void testEmptyFunction() throws JSQLParserException {
      String sql = "SELECT COUNT() FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

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
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the parenthesis wrapper
      assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in parentheses");
      Parenthesis paren = (Parenthesis) result;

      // Assert the IS NULL expression inside the parentheses
      assertInstanceOf(
          IsNullExpression.class,
          paren.getExpression(),
          "Expression inside parentheses should be IS NULL");
      IsNullExpression isNull = (IsNullExpression) paren.getExpression();

      // Assert that it's a NOT NULL check
      assertTrue(isNull.isNot(), "Should be IS NOT NULL");

      // Assert the column being checked
      assertEquals("col1", isNull.getLeftExpression().toString(), "Should be checking col1");
    }

    @Test
    void testInExpressionWithSubselect() throws JSQLParserException {
      String sql =
          """
                    SELECT (col1 IN (
                        SELECT scheduleddate
                        FROM events
                        WHERE events.enrollment = subax.enrollment
                        AND scheduleddate IS NOT NULL
                        ORDER BY occurreddate DESC
                        LIMIT 1
                    )) FROM dual""";

      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the parenthesis wrapper
      assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in parentheses");
      Parenthesis paren = (Parenthesis) result;

      // Assert the IN expression inside the parentheses
      assertInstanceOf(
          InExpression.class, paren.getExpression(), "Expression inside parentheses should be IN");
      InExpression in = (InExpression) paren.getExpression();

      // Verify left expression (col1)
      assertNotNull(in.getLeftExpression(), "Left expression should not be null");
      assertEquals("col1", in.getLeftExpression().toString(), "Left expression should be col1");

      // Verify right expression (transformed to column reference)
      assertNotNull(in.getRightExpression(), "Right expression should not be null");
      assertInstanceOf(
          Column.class,
          in.getRightExpression(),
          "Right expression should be transformed to a Column reference");

      Column transformedColumn = (Column) in.getRightExpression();
      assertEquals(
          "ls.scheduleddate",
          transformedColumn.toString(),
          "Should be transformed to ls.scheduleddate");

      // Verify that the subselect was captured in the transformer
      assertFalse(
          transformer.getExtractedSubSelects().isEmpty(), "Should have captured the subselect");

      // Verify the captured subselect details
      Optional<Map.Entry<SubSelect, FoundSubSelect>> foundSubSelect =
          transformer.getExtractedSubSelects().entrySet().stream()
              .filter(entry -> entry.getValue().columnReference().equals("scheduleddate"))
              .findFirst();

      assertTrue(
          foundSubSelect.isPresent(), "Should find the captured subselect for scheduleddate");
      assertEquals(
          "last_sched",
          foundSubSelect.get().getValue().name(),
          "Should be captured as last_sched pattern");
    }

    @Test
    void testBetweenWithArithmetic() throws JSQLParserException {
      String sql = "SELECT (col1 BETWEEN col2 + 1 AND col3 * 2) FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Assert the parenthesis wrapper
      assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in parentheses");
      Parenthesis paren = (Parenthesis) result;

      // Assert the BETWEEN expression inside the parentheses
      assertInstanceOf(
          Between.class, paren.getExpression(), "Expression inside parentheses should be BETWEEN");
      Between between = (Between) paren.getExpression();

      // Assert the column being checked
      assertEquals(
          "col1", between.getLeftExpression().toString(), "Left expression should be col1");

      // Assert the start of the BETWEEN range (col2 + 1)
      assertInstanceOf(
          Addition.class,
          between.getBetweenExpressionStart(),
          "Start of range should be an addition");
      Addition startExpr = (Addition) between.getBetweenExpressionStart();
      assertEquals(
          "col2", startExpr.getLeftExpression().toString(), "Left side of addition should be col2");
      assertEquals(
          "1", startExpr.getRightExpression().toString(), "Right side of addition should be 1");

      // Assert the end of the BETWEEN range (col3 * 2)
      assertInstanceOf(
          Multiplication.class,
          between.getBetweenExpressionEnd(),
          "End of range should be a multiplication");
      Multiplication endExpr = (Multiplication) between.getBetweenExpressionEnd();
      assertEquals(
          "col3",
          endExpr.getLeftExpression().toString(),
          "Left side of multiplication should be col3");
      assertEquals(
          "2", endExpr.getRightExpression().toString(), "Right side of multiplication should be 2");
    }

    @Test
    void testCastExpression() throws JSQLParserException {
      String sql = "SELECT CAST(col1 AS VARCHAR) FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

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
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      // Add debug output
      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // First assert we have a Parenthesis
      assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in parentheses");
      Parenthesis paren = (Parenthesis) result;

      // Then assert the InExpression inside the parentheses
      assertInstanceOf(
          InExpression.class,
          paren.getExpression(),
          "Expression inside parentheses should be an InExpression");
      InExpression in = (InExpression) paren.getExpression();

      // Assert the structure of the InExpression
      assertEquals("col1", in.getLeftExpression().toString(), "Left expression should be 'col1'");
      assertNotNull(in.getRightItemsList(), "Right items list should not be null");

      // Optionally, we could also verify the actual values in the list
      ExpressionList itemsList = (ExpressionList) in.getRightItemsList();
      List<Expression> expressions = itemsList.getExpressions();
      assertEquals(3, expressions.size(), "Should have 3 values in the IN list");
      assertEquals("1", expressions.get(0).toString());
      assertEquals("2", expressions.get(1).toString());
      assertEquals("3", expressions.get(2).toString());
    }

    @Test
    void testInExpressionWithSubselect() throws JSQLParserException {
      String sql = "SELECT (col1 IN (SELECT id FROM table1)) FROM dual";

      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // First assert we have a Parenthesis
      assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in parentheses");
      Parenthesis paren = (Parenthesis) result;

      // Then assert the InExpression inside the parentheses
      assertInstanceOf(
          InExpression.class,
          paren.getExpression(),
          "Expression inside parentheses should be an InExpression");
      InExpression in = (InExpression) paren.getExpression();

      // Assert the structure of the InExpression
      assertEquals("col1", in.getLeftExpression().toString(), "Left expression should be 'col1'");
      assertNotNull(in.getRightExpression(), "Right expression should not be null");
      assertInstanceOf(
          SubSelect.class, in.getRightExpression(), "Right expression should be a SubSelect");
    }
  }

  @Nested
  class ValueTests {

    @Test
    void testStringValue() throws JSQLParserException {
      String sql = "SELECT 'test string' FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      assertInstanceOf(StringValue.class, result);
      assertEquals("'test string'", result.toString());
    }

    @Test
    void testLongValue() throws JSQLParserException {
      String sql = "SELECT 12345 FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      assertInstanceOf(LongValue.class, result);
      assertEquals("12345", result.toString());
    }

    @Test
    void testDoubleValue() throws JSQLParserException {
      String sql = "SELECT 123.45 FROM dual";
      Expression expr =
          ((SelectExpressionItem)
                  ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                      .getSelectItems()
                      .get(0))
              .getExpression();

      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      assertInstanceOf(DoubleValue.class, result);
      assertEquals("123.45", result.toString());
    }
  }

  @Nested
  class CoalesceFunctionTests {
    @Test
    void testCoalesceWithLastEventValue() throws JSQLParserException {
      String sql =
          """
                    SELECT COALESCE(
                        (SELECT "de_xyz"
                        FROM events
                        WHERE events.enrollment = subax.enrollment
                        AND "de_xyz" IS NOT NULL
                        AND ps = 'stage1'
                        ORDER BY occurreddate DESC
                        LIMIT 1),
                        'default_value'
                    )""";

      Expression expr = parseExpression(sql);
      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Verify COALESCE structure
      assertInstanceOf(Function.class, result);
      Function coalesce = (Function) result;
      assertEquals("COALESCE", coalesce.getName().toUpperCase());

      // Verify parameters
      List<Expression> params = coalesce.getParameters().getExpressions();
      assertEquals(2, params.size());

      // First parameter should be transformed column
      assertInstanceOf(Column.class, params.get(0));
      Column transformedCol = (Column) params.get(0);
      assertEquals("lv_dexyz", transformedCol.getTable().getName());
      assertEquals("\"de_xyz\"", transformedCol.getColumnName());

      // Second parameter should remain unchanged
      assertEquals("'default_value'", params.get(1).toString());
    }

    @Test
    void testCoalesceWithMultipleLastValues() throws JSQLParserException {
      String sql =
          """
                    SELECT COALESCE(
                        (SELECT "de_xyz"
                        FROM events
                        WHERE events.enrollment = subax.enrollment
                        AND "de_xyz" IS NOT NULL
                        AND ps = 'stage1'
                        ORDER BY occurreddate DESC
                        LIMIT 1),
                        (SELECT "de_abc"
                        FROM events
                        WHERE events.enrollment = subax.enrollment
                        AND "de_abc" IS NOT NULL
                        AND ps = 'stage2'
                        ORDER BY occurreddate DESC
                        LIMIT 1),
                        'default'
                    )""";

      Expression expr = parseExpression(sql);
      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      assertInstanceOf(Function.class, result);
      Function coalesce = (Function) result;
      List<Expression> params = coalesce.getParameters().getExpressions();
      assertEquals(3, params.size());

      // Verify both transformed columns and default value
      assertInstanceOf(Column.class, params.get(0));
      assertInstanceOf(Column.class, params.get(1));
      assertEquals("'default'", params.get(2).toString());
    }
  }

  @Nested
  class ExtractFunctionTests {
    @Test
    void testExtractWithLastEventValue() throws JSQLParserException {
      String sql =
          """
                    SELECT EXTRACT(epoch FROM
                        (SELECT "de_date"
                        FROM events
                        WHERE events.enrollment = subax.enrollment
                        AND "de_date" IS NOT NULL
                        AND ps = 'stage1'
                        ORDER BY occurreddate DESC
                        LIMIT 1)
                    )""";

      Expression expr = parseExpression(sql);
      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Verify parenthesis wrapper
      assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in parentheses");
      Parenthesis paren = (Parenthesis) result;

      // Verify EXTRACT structure
      Expression extractExpr = paren.getExpression();
      assertInstanceOf(
          ExtractExpression.class, extractExpr, "Expression should be an ExtractExpression");
      ExtractExpression extract = (ExtractExpression) extractExpr;

      // Verify extract field
      assertEquals("epoch", extract.getName().toLowerCase(), "Should extract epoch");

      // Verify the transformed column reference
      Expression extractFrom = extract.getExpression();
      assertInstanceOf(Column.class, extractFrom, "Should extract from a column reference");
      Column col = (Column) extractFrom;
      assertEquals(
          "lv_dedate", col.getTable().getName(), "Should reference the transformed table alias");
      assertEquals("\"de_date\"", col.getColumnName(), "Should reference the original column name");
    }

    // Additional test for different extract fields
    @Test
    void testExtractWithDifferentFields() throws JSQLParserException {
      String[] extractFields = {"year", "month", "day", "hour", "minute"};

      for (String field : extractFields) {
        String sql =
            String.format(
                """
                        SELECT EXTRACT(%s FROM
                            (SELECT "de_date"
                            FROM events
                            WHERE events.enrollment = subax.enrollment
                            AND "de_date" IS NOT NULL
                            AND ps = 'stage1'
                            ORDER BY occurreddate DESC
                            LIMIT 1)
                        )""",
                field);

        Expression expr = parseExpression(sql);
        expr.accept(transformer);
        Expression result = transformer.getTransformedExpression();

        // Verify parenthesis wrapper
        assertInstanceOf(
            Parenthesis.class, result, "Result should be wrapped in parentheses for " + field);
        Parenthesis paren = (Parenthesis) result;

        // Verify EXTRACT structure
        Expression extractExpr = paren.getExpression();
        assertInstanceOf(
            ExtractExpression.class,
            extractExpr,
            "Expression should be an ExtractExpression for " + field);
        ExtractExpression extract = (ExtractExpression) extractExpr;

        // Verify extract field
        assertEquals(field, extract.getName().toLowerCase(), "Should extract " + field);

        // Verify the transformed column reference
        Expression extractFrom = extract.getExpression();
        assertInstanceOf(
            Column.class, extractFrom, "Should extract from a column reference for " + field);
        Column col = (Column) extractFrom;
        assertEquals(
            "lv_dedate",
            col.getTable().getName(),
            "Should reference the transformed table alias for " + field);
        assertEquals(
            "\"de_date\"",
            col.getColumnName(),
            "Should reference the original column name for " + field);
      }
    }
  }

  @Nested
  class NestedFunctionTests {
    @Test
    void testNestedFunctionsWithLastEventValue() throws JSQLParserException {
      String sql =
          """
                    SELECT COALESCE(
                        EXTRACT(epoch FROM
                            (SELECT "de_date"
                            FROM events
                            WHERE events.enrollment = subax.enrollment
                            AND "de_date" IS NOT NULL
                            AND ps = 'stage1'
                            ORDER BY occurreddate DESC
                            LIMIT 1)
                        ),
                        0
                    )""";

      Expression expr = parseExpression(sql);
      expr.accept(transformer);
      Expression result = transformer.getTransformedExpression();

      // Verify the COALESCE function
      assertInstanceOf(Function.class, result, "Result should be a COALESCE function");
      Function coalesce = (Function) result;
      assertEquals("coalesce", coalesce.getName().toLowerCase(), "Function should be COALESCE");

      // Verify COALESCE parameters
      assertNotNull(coalesce.getParameters(), "COALESCE should have parameters");
      List<Expression> params = coalesce.getParameters().getExpressions();
      assertEquals(2, params.size(), "COALESCE should have 2 parameters");

      // Verify first parameter (EXTRACT expression wrapped in parentheses)
      Expression firstParam = params.get(0);
      assertInstanceOf(
          Parenthesis.class, firstParam, "First parameter should be a parenthesized EXTRACT");
      Parenthesis extractParen = (Parenthesis) firstParam;

      Expression extractExpr = extractParen.getExpression();
      assertInstanceOf(
          ExtractExpression.class, extractExpr, "Inside parentheses should be ExtractExpression");
      ExtractExpression extract = (ExtractExpression) extractExpr;

      // Verify the extract details
      assertEquals("epoch", extract.getName().toLowerCase(), "Extract should be for epoch");

      // Verify the transformed column reference
      Expression extractFrom = extract.getExpression();
      assertInstanceOf(Column.class, extractFrom, "Extract should be from a column reference");
      Column col = (Column) extractFrom;
      assertEquals(
          "lv_dedate", col.getTable().getName(), "Should reference the transformed table alias");
      assertEquals("\"de_date\"", col.getColumnName(), "Should reference the original column name");

      // Verify second parameter (0)
      assertEquals("0", params.get(1).toString(), "Second parameter should be 0");
    }
  }

  @Nested
  class ProgramIndicatorFunctionTests {

    @Nested
    class TimestampDiffTests {
      @Test
      void testDaysBetweenFunction() throws JSQLParserException {
        String sql =
            """
                        SELECT TIMESTAMPDIFF(DAY,
                            (SELECT "enrollment_date"
                            FROM events
                            WHERE events.enrollment = subax.enrollment
                            AND "enrollment_date" IS NOT NULL
                            AND ps = 'stage1'
                            ORDER BY occurreddate DESC
                            LIMIT 1),
                            (SELECT "incident_date"
                            FROM events
                            WHERE events.enrollment = subax.enrollment
                            AND "incident_date" IS NOT NULL
                            AND ps = 'stage1'
                            ORDER BY occurreddate DESC
                            LIMIT 1)
                        )""";

        Expression expr = parseExpression(sql);
        expr.accept(transformer);
        Expression result = transformer.getTransformedExpression();

        // Verify function structure
        assertInstanceOf(Function.class, result, "Result should be a TIMESTAMPDIFF function");
        Function timestampDiff = (Function) result;
        assertEquals(
            "TIMESTAMPDIFF",
            timestampDiff.getName().toUpperCase(),
            "Function should be TIMESTAMPDIFF");

        // Verify parameters
        List<Expression> params = timestampDiff.getParameters().getExpressions();
        assertEquals(3, params.size(), "TIMESTAMPDIFF should have 3 parameters");

        // Verify unit parameter
        assertEquals(
            "DAY", params.get(0).toString().toUpperCase(), "First parameter should be DAY");

        // Verify start date parameter (transformed column)
        assertInstanceOf(
            Column.class, params.get(1), "Second parameter should be a transformed column");
        Column startDate = (Column) params.get(1);
        assertEquals(
            "lv_enrollmentdate",
            startDate.getTable().getName(),
            "Should reference the transformed enrollment_date table alias");

        // Verify end date parameter (transformed column)
        assertInstanceOf(
            Column.class, params.get(2), "Third parameter should be a transformed column");
        Column endDate = (Column) params.get(2);
        assertEquals(
            "lv_incidentdate",
            endDate.getTable().getName(),
            "Should reference the transformed incident_date table alias");
      }
    }

    @Nested
    class ExtractTests {
      @Test
      void testYearsBetweenFunction() throws JSQLParserException {
        String sql =
            """
                        SELECT EXTRACT(YEAR FROM
                            (SELECT "birth_date"
                            FROM events
                            WHERE events.enrollment = subax.enrollment
                            AND "birth_date" IS NOT NULL
                            AND ps = 'stage1'
                            ORDER BY occurreddate DESC
                            LIMIT 1)
                        )""";

        Expression expr = parseExpression(sql);
        expr.accept(transformer);
        Expression result = transformer.getTransformedExpression();

        // Verify parenthesis wrapper
        assertInstanceOf(Parenthesis.class, result, "Result should be wrapped in parentheses");
        Parenthesis paren = (Parenthesis) result;

        // Verify EXTRACT structure
        Expression extractExpr = paren.getExpression();
        assertInstanceOf(
            ExtractExpression.class, extractExpr, "Expression should be an ExtractExpression");
        ExtractExpression extract = (ExtractExpression) extractExpr;

        // Verify extract field
        assertEquals("year", extract.getName().toLowerCase(), "Should extract year");

        // Verify the transformed column reference
        Expression extractFrom = extract.getExpression();
        assertInstanceOf(Column.class, extractFrom, "Should extract from a column reference");
        Column col = (Column) extractFrom;
        assertEquals(
            "lv_birthdate",
            col.getTable().getName(),
            "Should reference the transformed table alias");
      }
    }

    @Nested
    class MathematicalFunctionTests {
      @Test
      void testFloorFunction() throws JSQLParserException {
        String sql =
            """
                        SELECT FLOOR(
                            (SELECT "numeric_value"
                            FROM events
                            WHERE events.enrollment = subax.enrollment
                            AND "numeric_value" IS NOT NULL
                            AND ps = 'stage1'
                            ORDER BY occurreddate DESC
                            LIMIT 1)
                        )""";

        Expression expr = parseExpression(sql);
        expr.accept(transformer);
        Expression result = transformer.getTransformedExpression();

        // Verify function structure
        assertInstanceOf(Function.class, result, "Result should be a FLOOR function");
        Function floor = (Function) result;
        assertEquals("FLOOR", floor.getName().toUpperCase(), "Function should be FLOOR");

        // Verify parameter (transformed column)
        List<Expression> params = floor.getParameters().getExpressions();
        assertEquals(1, params.size(), "FLOOR should have 1 parameter");
        assertInstanceOf(Column.class, params.get(0), "Parameter should be a transformed column");
      }

      @Test
      void testCeilFunction() throws JSQLParserException {
        String sql =
            """
                        SELECT CEIL(
                            (SELECT "numeric_value"
                            FROM events
                            WHERE events.enrollment = subax.enrollment
                            AND "numeric_value" IS NOT NULL
                            AND ps = 'stage1'
                            ORDER BY occurreddate DESC
                            LIMIT 1)
                        )""";

        Expression expr = parseExpression(sql);
        expr.accept(transformer);
        Expression result = transformer.getTransformedExpression();

        // Verify function structure
        assertInstanceOf(Function.class, result, "Result should be a CEIL function");
        Function ceil = (Function) result;
        assertEquals("CEIL", ceil.getName().toUpperCase(), "Function should be CEIL");

        // Verify parameter (transformed column)
        List<Expression> params = ceil.getParameters().getExpressions();
        assertEquals(1, params.size(), "CEIL should have 1 parameter");
        assertInstanceOf(Column.class, params.get(0), "Parameter should be a transformed column");
        Column col = (Column) params.get(0);
        assertEquals(
            "lv_numericvalue",
            col.getTable().getName(),
            "Should reference the transformed table alias");
        assertEquals(
            "\"numeric_value\"", col.getColumnName(), "Should reference the original column name");
      }

      @Test
      void testRoundFunction() throws JSQLParserException {
        String sql =
            """
            SELECT ROUND(
                (SELECT "numeric_value"
                FROM events
                WHERE events.enrollment = subax.enrollment
                AND "numeric_value" IS NOT NULL
                AND ps = 'stage1'
                ORDER BY occurreddate DESC
                LIMIT 1),
                2
            )""";

        Expression expr = parseExpression(sql);
        expr.accept(transformer);
        Expression result = transformer.getTransformedExpression();

        // Verify function structure
        assertInstanceOf(Function.class, result, "Result should be a ROUND function");
        Function round = (Function) result;
        assertEquals("ROUND", round.getName().toUpperCase(), "Function should be ROUND");

        // Verify parameters
        List<Expression> params = round.getParameters().getExpressions();
        assertEquals(2, params.size(), "ROUND should have 2 parameters");

        // Verify first parameter (transformed column)
        assertInstanceOf(
            Column.class, params.get(0), "First parameter should be a transformed column");
        Column col = (Column) params.get(0);
        assertEquals(
            "lv_numericvalue",
            col.getTable().getName(),
            "Should reference the transformed table alias");
        assertEquals(
            "\"numeric_value\"", col.getColumnName(), "Should reference the original column name");

        // Verify second parameter (decimal places)
        assertEquals(
            "2",
            params.get(1).toString(),
            "Second parameter should be number of decimal places (2)");
      }
    }

    @Nested
    class CaseExpressionTests {
      @Test
      void testIfConditionFunction() throws JSQLParserException {
        String sql =
            """
              SELECT CASE
                  WHEN (SELECT "condition_value"
                       FROM events
                       WHERE events.enrollment = subax.enrollment
                       AND "condition_value" IS NOT NULL
                       AND ps = 'stage1'
                       ORDER BY occurreddate DESC
                       LIMIT 1) = 'true'
                  THEN 1
                  ELSE 0
              END""";

        Expression expr = parseExpression(sql);
        expr.accept(transformer);
        Expression result = transformer.getTransformedExpression();

        assertInstanceOf(LongValue.class, result, "Result should be a LongValue (0 or 1)");
        assertInstanceOf(LongValue.class, result, "Result should be a LongValue (0 or 1)");
        // We can't know the exact value, so let's avoid asserting the precise value. We assert it
        // IS a LongValue

        // Verify that the subselect "condition_value" was captured in the transformer
        Optional<Map.Entry<SubSelect, FoundSubSelect>> foundSubSelect =
            transformer.getExtractedSubSelects().entrySet().stream()
                .filter(entry -> entry.getValue().columnReference().equals("\"condition_value\""))
                .findFirst();

        assertTrue(
            foundSubSelect.isPresent(), "Should find the captured subselect for condition_value");
        assertEquals(
            "last_value_conditionvalue",
            foundSubSelect.get().getValue().name(),
            "Should be captured as last_value_conditionvalue pattern");
      }
    }
  }

  // Helper method to find the first Division operation in the expression tree
  private Division findFirstDivision(Expression expr) {
    if (expr instanceof Division) {
      return (Division) expr;
    }
    if (expr instanceof Parenthesis) {
      return findFirstDivision(((Parenthesis) expr).getExpression());
    }
    if (expr instanceof BinaryExpression binary) {
      Division leftDiv = findFirstDivision(binary.getLeftExpression());
      if (leftDiv != null) {
        return leftDiv;
      }
      return findFirstDivision(binary.getRightExpression());
    }
    return null;
  }

  private Expression parseExpression(String sql) throws JSQLParserException {
    return ((SelectExpressionItem)
            ((PlainSelect) ((Select) CCJSqlParserUtil.parse(sql)).getSelectBody())
                .getSelectItems()
                .get(0))
        .getExpression();
  }
}
