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

import static org.hisp.dhis.antlr.AntlrParserUtils.castString;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsConstants;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import org.hisp.dhis.parser.expression.statement.StatementBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.ProgramExpressionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.dataitem.ProgramItemStageElement;

/**
 * Program indicator count functions
 *
 * @author Jim Grace
 */
@Slf4j
public abstract class ProgramCountFunction extends ProgramExpressionItem {

  private static final String SQL_TEMPLATE =
      """
            (select count(%1$s)
            from %2$s
            where %2$s.enrollment = %3$s.enrollment
            and %1$s is not null
            and %4$s
            %5$s
            %6$s
            and ps = '%7$s')
            """;

  /**
   * Argument type for d2:countIfCondition Used for: d2:countIfCondition(#{ps.de}, '< 10') Indicates
   * that the arg64 field in the placeholder contains the Base64 encoded string representation of
   * the raw string literal provided as the condition argument (including the quotes, e.g., "< 10").
   * Example condition literal:
   *
   * <p>'< 10', '>= 5', '!= "Completed"'
   */
  private static final String ARGTYPE_COND_LIT_64 = "condLit64";

  /**
   * Argument type for d2:countIfValue Used for: d2:countIfValue(#{ps.de}, valueExpr) Indicates that
   * the arg64 field in the placeholder contains the Base64 encoded string representation of the SQL
   * generated for the valueExpr. Example:
   *
   * <p>valueExpr: 5, 'ABC', V{some_variable} valueSql (before encoding): cast(5 as numeric), 'ABC',
   * coalesce(alias.value, 0)
   */
  private static final String ARGTYPE_VAL_64 = "val64";

  /** Argument type for d2:count */
  private static final String ARGTYPE_NONE = "none";

  @Override
  public final Object getSql(ExprContext ctx, CommonExpressionVisitor visitor) {
    if (!visitor.isUseExperimentalSqlEngine()) {
      return getSqlLegacy(ctx, visitor);
    }
    validateCountFunctionArgs(ctx);
    // Extract Common Parameters
    String programStageId = ctx.uid0.getText();
    String dataElementId = ctx.uid1.getText();
    ProgramExpressionParams progParams = visitor.getProgParams();
    ProgramIndicator programIndicator = progParams.getProgramIndicator();
    Date startDate = progParams.getReportingStartDate();
    Date endDate = progParams.getReportingEndDate();
    String piUid = programIndicator.getUid();

    // Generate Boundary Hash
    String boundaryHash = generateBoundaryHash(programIndicator, startDate, endDate);

    // Get Function Name from subclass
    String functionName = getFunctionName();

    // Determine Argument Type and Encode Argument SQL/Literal
    String argType = ARGTYPE_NONE;
    String encodedArgSql = ""; // Default for d2:count

    if (D2CountIfValue.FUNCTION_NAME.equals(functionName)) {
      // --- Handle d2:countIfValue ---
      // Find the CORRECT accessor for the value expression context
      ExprContext valueExprCtx = ctx.expr(0); // Assuming expr(0) is correct, VERIFY this!

      if (valueExprCtx == null) {
        log.error(
            "Value expression (second argument) is missing for d2:countIfValue in: {}",
            ctx.getText());
        return "__INVALID_D2FUNC_ARGS__"; // Return error placeholder
      }

      // Get valueSql using temporary visitor
      CommonExpressionVisitor sqlVisitor =
          visitor.toBuilder()
              .itemMethod(ITEM_GET_SQL)
              .params(visitor.getParams().toBuilder().dataType(DataType.NUMERIC).build())
              .itemMap(visitor.getItemMap())
              .constantMap(visitor.getConstantMap())
              .build();
      String valueSql = castString(sqlVisitor.visit(valueExprCtx));

      if (valueSql == null) {
        log.error(
            "Visiting the value expression resulted in null SQL for d2:countIfValue in: {}. Value text: {}",
            ctx.getText(),
            valueExprCtx.getText());
        return "__INVALID_D2FUNC_VALUE_SQL__"; // Return error placeholder
      }

      encodedArgSql = Base64.getEncoder().encodeToString(valueSql.getBytes(StandardCharsets.UTF_8));
      argType = ARGTYPE_VAL_64;

    } else if (D2CountIfCondition.FUNCTION_NAME.equals(functionName)) {
      ExpressionParser.StringLiteralContext conditionNode = ctx.stringLiteral();

      if (conditionNode == null) {
        log.error(
            "Condition string literal (second argument) is missing for d2:countIfCondition in: {}",
            ctx.getText());
        return "__INVALID_D2FUNC_ARGS__"; // Return error placeholder
      }

      String rawConditionLiteral = conditionNode.getText(); // Get the literal including quotes
      encodedArgSql =
          Base64.getEncoder().encodeToString(rawConditionLiteral.getBytes(StandardCharsets.UTF_8));
      argType = ARGTYPE_COND_LIT_64;

    } else if (!D2Count.FUNCTION_NAME.equals(functionName)) {
      // Handle unknown function derived from this base class
      log.error("Unknown ProgramCountFunction type '{}' encountered.", functionName);
      return "__UNKNOWN_D2FUNC__";
    }
    // For "count", argType remains "none" and encodedArgSql remains ""

    // Return Rich Placeholder
    return String.format(
        "__D2FUNC__(func='%s', ps='%s', de='%s', argType='%s', arg64='%s', hash='%s', pi='%s')__",
        functionName,
        programStageId,
        dataElementId,
        argType, // Embed the argument type indicator
        encodedArgSql, // Embed encoded arg (valueSql or conditionLiteral) or empty string
        boundaryHash,
        piUid);
  }

  public final Object getSqlLegacy(ExprContext ctx, CommonExpressionVisitor visitor) {
    validateCountFunctionArgs(ctx);

    StatementBuilder sb = visitor.getStatementBuilder();

    ProgramExpressionParams params = visitor.getProgParams();

    ProgramIndicator pi = params.getProgramIndicator();

    Date startDate = params.getReportingStartDate();
    Date endDate = params.getReportingEndDate();

    String programStage = ctx.uid0.getText();
    String dataElement = ctx.uid1.getText();

    String eventTableName = "analytics_event_" + pi.getProgram().getUid();
    String baseColumnName = visitor.getSqlBuilder().quote(dataElement);
    String conditionSql = getConditionSql(ctx, visitor, baseColumnName);

    String endBoundarySql =
        (pi.getEndEventBoundary() != null)
            ? "and " + sb.getBoundaryCondition(pi.getEndEventBoundary(), pi, startDate, endDate)
            : ""; // Empty if no end boundary
    String startBoundarySql =
        (pi.getStartEventBoundary() != null)
            ? "and " + sb.getBoundaryCondition(pi.getStartEventBoundary(), pi, startDate, endDate)
            : ""; // Empty if no start boundary

    return String.format(
        SQL_TEMPLATE,
        baseColumnName, // %1$s: Quoted column name
        eventTableName, // %2$s: Event table name
        AnalyticsConstants.ANALYTICS_TBL_ALIAS, // %3$s: Outer table alias
        conditionSql, // %4$s: Full condition predicate from subclass
        endBoundarySql, // %5$s: End boundary condition (or empty)
        startBoundarySql, // %6$s: Start boundary condition (or empty)
        programStage // %7$s: Program stage UID
        );
  }

  /**
   * Get the description for the first arg #{programStageUid.dataElementUid} and return a value with
   * its data type.
   *
   * @param ctx the expression context
   * @param visitor the tree visitor
   * @return a dummy value for the item (of the right type, for type checking)
   */
  protected Object getProgramStageElementDescription(
      ExprContext ctx, CommonExpressionVisitor visitor) {
    validateCountFunctionArgs(ctx);

    return (new ProgramItemStageElement()).getDescription(ctx, visitor);
  }

  /**
   * Generate the conditional part of the SQL for a d2 count function
   *
   * @param ctx the expression context
   * @param visitor the program indicator expression tree visitor
   * @return the conditional part of the SQL
   */
  public abstract String getConditionSql(
      ExprContext ctx, CommonExpressionVisitor visitor, String baseColumnName);

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Abstract method for subclasses to provide their specific function name (e.g., "countIfValue").
   *
   * @return The d2 function name.
   */
  protected abstract String getFunctionName();

  private void validateCountFunctionArgs(ExprContext ctx) {
    if (!(getProgramArgType(ctx) instanceof ProgramItemStageElement)) {
      throw new ParserExceptionWithoutContext(
          "First argument not supported for d2:count... functions: " + ctx.getText());
    }
  }

  /**
   * Generate a hash from the program indicator boundaries and the reporting period. This hash acts
   * as a fingerprint for the specific boundary setup within the context of the current reporting
   * period. It allows the placeholder processing logic to correctly distinguish and potentially
   * reuse CTEs based not only on the data item/function but also on the applicable date boundaries
   *
   * @return The generated hash string.
   */
  private static String generateBoundaryHash(
      ProgramIndicator programIndicator, Date reportingStartDate, Date reportingEndDate) {
    Set<AnalyticsPeriodBoundary> boundaries = programIndicator.getAnalyticsPeriodBoundaries();
    if (boundaries == null || boundaries.isEmpty()) {
      return "noboundaries";
    }
    List<String> boundaryInfoList = new ArrayList<>();
    SimpleDateFormat dateFormat = new SimpleDateFormat(Period.DEFAULT_DATE_FORMAT);
    for (AnalyticsPeriodBoundary boundary : boundaries) {
      if (boundary != null) {
        Date boundaryDate = boundary.getBoundaryDate(reportingStartDate, reportingEndDate);
        String dateString = (boundaryDate != null) ? dateFormat.format(boundaryDate) : "null";
        boundaryInfoList.add(boundary.getUid() + ":" + dateString);
      }
    }
    Collections.sort(boundaryInfoList);
    String boundaryConfigString = String.join(";", boundaryInfoList);
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] messageDigest = md.digest(boundaryConfigString.getBytes(StandardCharsets.UTF_8));
      BigInteger no = new BigInteger(1, messageDigest);
      return String.format("%040x", no);
    } catch (NoSuchAlgorithmException e) {
      log.error("SHA-1 Algorithm not found for boundary hashing. Falling back.", e);
      return "hash_error_" + boundaryConfigString.hashCode();
    }
  }
}
