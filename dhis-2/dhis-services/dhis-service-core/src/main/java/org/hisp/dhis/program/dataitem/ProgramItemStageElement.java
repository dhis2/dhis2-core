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
package org.hisp.dhis.program.dataitem;

import static org.hisp.dhis.parser.expression.ParserUtils.assumeStageElementSyntax;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.program.ProgramExpressionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.system.util.SqlUtils;

/**
 * Program indicator expression data item ProgramItemStageElement
 *
 * @author Jim Grace
 */
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
                    SqlUtils.quote(dataElementId),
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
