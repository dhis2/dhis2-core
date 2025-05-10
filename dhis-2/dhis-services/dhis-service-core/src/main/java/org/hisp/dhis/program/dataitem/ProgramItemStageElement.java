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
package org.hisp.dhis.program.dataitem;

import static org.hisp.dhis.parser.expression.ParserUtils.assumeStageElementSyntax;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramExpressionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;

/**
 * Program indicator expression data item ProgramItemStageElement
 *
 * @author Jim Grace
 */
@Slf4j
public class ProgramItemStageElement extends ProgramExpressionItem {
  @Override
  public Object getDescription(ExprContext ctx, CommonExpressionVisitor visitor) {
    assumeStageElementSyntax(ctx);

    String programStageId = ctx.uid0.getText();
    String dataElementId = ctx.uid1.getText();

    ProgramStageService stageService = visitor.getProgramStageService();

    ProgramStage programStage = stageService.getProgramStage(programStageId);
    DataElement dataElement = visitor.getIdObjectManager().get(DataElement.class, dataElementId);

    if (programStage == null) {
      throw new ParserExceptionWithoutContext("Program stage " + programStageId + " not found");
    }

    if (dataElement == null) {
      throw new ParserExceptionWithoutContext("Data element " + dataElementId + " not found");
    }

    if (isNonDefaultStageOffset(visitor.getState().getStageOffset())
        && !isRepeatableStage(stageService, programStageId)) {
      throw new ParserException(getErrorMessage(programStageId));
    }

    String description =
        programStage.getDisplayName()
            + ProgramIndicator.SEPARATOR_ID
            + dataElement.getDisplayName();

    visitor.getItemDescriptions().put(ctx.getText(), description);

    return getNullReplacementValue(dataElement.getValueType());
  }

  @Override
  public Object getSql(ExprContext ctx, CommonExpressionVisitor visitor) {
    if (!visitor.isUseExperimentalSqlEngine()) {
      return getSqlLegacy(ctx, visitor);
    }

    assumeStageElementSyntax(ctx);

    ProgramExpressionParams progParams = visitor.getProgParams();
    ProgramIndicator programIndicator = progParams.getProgramIndicator();
    AnalyticsType analyticsType = programIndicator.getAnalyticsType();

    if (AnalyticsType.ENROLLMENT == analyticsType) {
      String programStageId = ctx.uid0.getText();
      String dataElementId = ctx.uid1.getText();
      Date reportingStartDate = progParams.getReportingStartDate();
      Date reportingEndDate = progParams.getReportingEndDate();
      int stageOffsetRaw = visitor.getState().getStageOffset();

      // Treat MIN_VALUE (no explicit offset) as 0 (latest event)
      int stageOffset = (stageOffsetRaw == Integer.MIN_VALUE) ? 0 : stageOffsetRaw;

      // Generate boundary hash using the internal helper method
      String boundaryHash =
          generateBoundaryHash(programIndicator, reportingStartDate, reportingEndDate);

      // Construct the placeholder string
      return String.format(
          "__PSDE_CTE_PLACEHOLDER__(psUid='%s', deUid='%s', offset='%d', boundaryHash='%s', piUid='%s')",
          programStageId, dataElementId, stageOffset, boundaryHash, programIndicator.getUid());
    } else { // no need to emit a placeholder for event analytics
      return getSqlLegacy(ctx, visitor);
    }
  }

  /**
   * Generates a deterministic hash representation (SHA-1) of the program indicator's boundaries and
   * their calculated dates for the given period. This ensures unique CTE keys when boundary
   * configurations change.
   *
   * @param programIndicator The program indicator.
   * @param reportingStartDate The reporting period start date.
   * @param reportingEndDate The reporting period end date.
   * @return A SHA-1 hash string representing the boundary configuration or "noboundaries" /
   *     "hash_error".
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
        // Include boundary type and operator for more uniqueness if needed
        // For now, UID:Date should suffice
        boundaryInfoList.add(boundary.getUid() + ":" + dateString);
      }
    }

    // Sort to ensure consistent order
    Collections.sort(boundaryInfoList);

    // Concatenate sorted strings
    String boundaryConfigString = String.join(";", boundaryInfoList);

    // Generate SHA-1 Hash
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] messageDigest = md.digest(boundaryConfigString.getBytes(StandardCharsets.UTF_8));

      // Convert byte array into signum representation
      BigInteger no = new BigInteger(1, messageDigest);

      // Convert message digest into hex value, padded to 40 chars for SHA-1

      return String.format("%040x", no);

    } catch (NoSuchAlgorithmException e) {
      // Log the error appropriately in a real application
      log.error("SHA-1 Algorithm not found for boundary hashing: {}", e.getMessage());
      // Fallback to a simple hash code of the string - less unique but avoids invalid chars
      return "hash_error_" + boundaryConfigString.hashCode();
    }
  }

  public Object getSqlLegacy(ExprContext ctx, CommonExpressionVisitor visitor) {
    assumeStageElementSyntax(ctx);

    String programStageId = ctx.uid0.getText();

    String dataElementId = ctx.uid1.getText();

    ProgramExpressionParams params = visitor.getProgParams();

    int stageOffset = visitor.getState().getStageOffset();

    String column;

    if (isNonDefaultStageOffset(stageOffset)) {
      if (isRepeatableStage(visitor.getProgramStageService(), programStageId)) {
        column =
            visitor
                .getStatementBuilder()
                .getProgramIndicatorEventColumnSql(
                    programStageId,
                    Integer.valueOf(stageOffset).toString(),
                    visitor.getSqlBuilder().quote(dataElementId),
                    params.getReportingStartDate(),
                    params.getReportingEndDate(),
                    params.getProgramIndicator());
      } else {
        throw new ParserException(getErrorMessage(programStageId));
      }
    } else {
      column =
          visitor
              .getStatementBuilder()
              .getProgramIndicatorDataValueSelectSql(
                  programStageId,
                  dataElementId,
                  params.getReportingStartDate(),
                  params.getReportingEndDate(),
                  params.getProgramIndicator());
    }

    if (visitor.getState().isReplaceNulls()) {
      DataElement dataElement = visitor.getIdObjectManager().get(DataElement.class, dataElementId);

      if (dataElement == null) {
        throw new ParserExceptionWithoutContext(
            "Data element " + dataElementId + " not found during SQL generation.");
      }

      column = replaceNullSqlValues(column, visitor, dataElement.getValueType());
    }

    return column;
  }

  private static boolean isNonDefaultStageOffset(int stageOffset) {
    return stageOffset != Integer.MIN_VALUE;
  }

  private static boolean isRepeatableStage(
      ProgramStageService stageService, String programStageId) {
    ProgramStage programStage = stageService.getProgramStage(programStageId);

    return programStage != null && programStage.getRepeatable();
  }

  private static String getErrorMessage(String programStageId) {
    ErrorMessage errorMessage = new ErrorMessage(ErrorCode.E2039, programStageId);

    return errorMessage.getMessage();
  }
}
