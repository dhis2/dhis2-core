/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.webmessage.DescriptiveWebMessage;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.rules.models.RuleValidationIssue;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.springframework.http.HttpStatus;

/**
 * {@link DescriptiveWebMessage} returned by the program rule expression description endpoints. On
 * top of the human-readable {@code description} it exposes the individual errors and warnings
 * reported by the rule engine, so that clients can point at the offending part of the expression.
 */
@Getter
public class ProgramRuleExpressionWebMessage extends DescriptiveWebMessage {

  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<ExpressionIssue> errors = List.of();

  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<ExpressionIssue> warnings = List.of();

  /** Only for deserialisation */
  public ProgramRuleExpressionWebMessage() {
    super();
  }

  private ProgramRuleExpressionWebMessage(Status status, HttpStatus httpStatus) {
    super(status, httpStatus);
  }

  /**
   * @param result the rule engine validation result
   * @param i18n used to translate the message
   * @param invalidStatus HTTP status to use when the expression is not valid
   */
  public static ProgramRuleExpressionWebMessage of(
      @Nonnull RuleValidationResult result, @Nonnull I18n i18n, @Nonnull HttpStatus invalidStatus) {
    if (result.getValid()) {
      ProgramRuleExpressionWebMessage message =
          new ProgramRuleExpressionWebMessage(Status.OK, HttpStatus.OK);
      message.setDescription(result.getDescription());
      message.setMessage(i18n.getString(ProgramIndicator.VALID));
      return message;
    }

    ProgramRuleExpressionWebMessage message =
        new ProgramRuleExpressionWebMessage(Status.ERROR, invalidStatus);
    message.setDescription(result.getErrorMessage());
    message.setMessage(i18n.getString(ProgramIndicator.EXPRESSION_NOT_VALID));
    message.errors = toIssues(result.getErrors());
    message.warnings = toIssues(result.getWarnings());
    return message;
  }

  private static List<ExpressionIssue> toIssues(List<RuleValidationIssue> issues) {
    return issues.stream()
        .map(
            i ->
                new ExpressionIssue(
                    i.getMessage(), StringUtils.isBlank(i.getPosition()) ? null : i.getPosition()))
        .toList();
  }

  /**
   * A single problem found in an expression.
   *
   * @param message what is wrong
   * @param position the fragment of the expression the problem refers to, if known
   */
  public record ExpressionIssue(
      @JsonProperty @Nonnull String message,
      @JsonProperty @JsonInclude(JsonInclude.Include.NON_NULL) @CheckForNull String position) {}
}
