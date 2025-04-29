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
package org.hisp.dhis.program.function;

import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.antlr.AntlrParserUtils.trimQuotes;
import static org.hisp.dhis.parser.expression.ParserUtils.DEFAULT_DOUBLE_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import java.util.regex.Pattern;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;
import org.hisp.dhis.program.ProgramIndicatorService;

/**
 * Program indicator function: d2 count if condition
 *
 * @author Jim Grace
 */
public class D2CountIfCondition extends ProgramCountFunction {
  static final String FUNCTION_NAME = "countIfCondition";

  @Override
  public Object getDescription(ExprContext ctx, CommonExpressionVisitor visitor) {
    castDouble(getProgramStageElementDescription(ctx, visitor));

    String conditionExpression = getConditionalExpression(ctx);

    visitor
        .getProgramIndicatorService()
        .validate(conditionExpression, Boolean.class, visitor.getItemDescriptions());

    return DEFAULT_DOUBLE_VALUE;
  }

  /**
   * Generates the complete SQL condition predicate for the {@code d2:countIfCondition} function.
   * The resulting predicate includes the correctly quoted and "casted" column name on the left-hand
   * side and the correctly "casted" literal value from the condition string on the right-hand side,
   * suitable for the target database.
   *
   * <p>This method uses a workaround to leverage the {@link
   * ProgramIndicatorService#getAnalyticsSql} method for generating the database-specific SQL for
   * the operator and the "casted" literal value (the right-hand side of the comparison). The {@code
   * getAnalyticsSql} service requires a complete, valid expression to process.
   *
   * <p>The workaround proceeds as follows:
   *
   * <ol>
   *   <li>A complete dummy expression is constructed by prepending "0" to the condition literal
   *       (e.g., if the condition is {@code "< 11"}, the dummy expression is {@code "0<11"}).
   *   <li>{@code getAnalyticsSql} is called with this dummy expression. This generates the {@code
   *       fullDummyConditionSql}, containing the SQL for the dummy '0', the operator, and the
   *       correctly casted literal value (e.g., {@code "CAST(0 AS DECIMAL) < CAST(11 AS
   *       DECIMAL)"}). This step effectively forces the service to produce the desired operator and
   *       casted right-hand side.
   *   <li>To isolate the unwanted SQL representation of the dummy '0', {@code getAnalyticsSql} is
   *       called again with just "0". This generates the {@code dummyZeroSql} (e.g., {@code "CAST(0
   *       AS DECIMAL)"}).
   *   <li>The {@code dummyZeroSql} string is removed from the beginning of the {@code
   *       fullDummyConditionSql} string. This leaves only the desired operator and the correctly
   *       casted right-hand side (e.g., {@code " < CAST(11 AS DECIMAL)"}).
   *   <li>Separately, the {@link SqlBuilder} is used to generate the correctly quoted and casted
   *       SQL for the actual data element column (the left-hand side), using the provided {@code
   *       baseColumnName}.
   *   <li>Finally, the casted left-hand side is combined with the extracted operator and right-hand
   *       side to form the complete predicate string.
   * </ol>
   *
   * @param ctx the expression context
   * @param visitor the expression visitor
   * @param baseColumnName the database-specifically quoted column name for the data element.
   * @return the complete conditional SQL predicate string (e.g., "cast(\"column\" as numeric) <
   *     11::numeric" or "cast(`column` as decimal) < CAST(11 AS DECIMAL)").
   */
  @Override
  public String getConditionSql(
      ExprContext ctx, CommonExpressionVisitor visitor, String baseColumnName) {
    // Get condition literal (e.g., "< 11")
    ExpressionParser.StringLiteralContext conditionNode = ctx.stringLiteral();
    if (conditionNode == null) {
      throw new ParserExceptionWithoutContext(
          "Condition string literal is missing in d2:countIfCondition");
    }
    // This contains the condition string (e.g., "< 11")
    String conditionLiteral = trimQuotes(conditionNode.getText());

    // Dummy expression (e.g., "0 < 11")
    String dummyConditionExpression = "0" + conditionLiteral;

    ProgramExpressionParams params = visitor.getProgParams();
    ProgramIndicatorService service = visitor.getProgramIndicatorService();
    SqlBuilder sqlBuilder = visitor.getSqlBuilder();

    //  Get SQL for the full dummy condition
    String fullDummyConditionSql =
        service.getAnalyticsSql(
            dummyConditionExpression,
            BOOLEAN,
            params.getProgramIndicator(),
            params.getReportingStartDate(),
            params.getReportingEndDate());

    // Get SQL for the dummy '0' value
    // Get the full expression only for the dummy '0' value
    // e.g. `CAST(0 as decimal)` or `0::numeric`
    String dummyZeroSql =
        service.getAnalyticsSql(
            "0",
            NUMERIC, // Or appropriate domain/type hint
            params.getProgramIndicator(),
            params.getReportingStartDate(),
            params.getReportingEndDate());

    // Extract the operator and right-hand side from the dummy SQL
    //    Example: If fullDummyConditionSql is "CAST(0 AS T) < CAST(11 AS T)"
    //             and dummyZeroSql is "CAST(0 AS T)", we want " < CAST(11 AS T)".
    String operatorAndRightHandSide =
        fullDummyConditionSql
            .replaceFirst(
                Pattern.quote(dummyZeroSql), "" // Replace with empty string
                )
            .trim(); // Trim leading space if any

    // Generate the correctly quoted *and casted* SQL for the actual
    // data element column using the provided baseColumnName.
    String castedLeftHandSide = sqlBuilder.cast(baseColumnName, NUMERIC);

    // Return the complete condition predicate
    // e.g., "cast(`col` as DECIMAL) < CAST(11 AS DECIMAL)"
    return castedLeftHandSide + " " + operatorAndRightHandSide;
  }

  @Override
  protected String getFunctionName() {
    return FUNCTION_NAME;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Gets a complete expression used to test a condition (e.g. "<5") by putting a "0" in front to
   * get a complete expression (e.g., "0<5").
   *
   * @param ctx the expression context
   * @return the complete expression
   */
  private String getConditionalExpression(ExprContext ctx) {
    return "0" + trimQuotes(ctx.stringLiteral().getText());
  }
}
