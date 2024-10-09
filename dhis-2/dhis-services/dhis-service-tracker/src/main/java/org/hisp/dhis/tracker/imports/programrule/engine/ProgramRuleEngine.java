/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule.engine;

import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.hisp.dhis.user.UserDetails;

public interface ProgramRuleEngine {
  /**
   * Evaluate program rules as the given {@link UserDetails} for {@link Program} for enrollment and
   * tracker events.
   */
  RuleEngineEffects evaluateEnrollmentAndEvents(
      RuleEnrollment enrollment,
      List<RuleEvent> events,
      Program program,
      UserDetails evaluatingUser);

  /**
   * Evaluate program rules as the given {@link UserDetails} for {@link Program} for program events.
   */
  RuleEngineEffects evaluateProgramEvents(
      List<RuleEvent> events, Program program, UserDetails evaluatingUser);

  /**
   * To getDescription rule condition in order to fetch its description
   *
   * @param condition of program rule
   * @param programUid {@link Program} which the programRule is associated with.
   * @return RuleValidationResult contains description of program rule condition or errorMessage
   */
  RuleValidationResult getDescription(String condition, UID programUid) throws BadRequestException;

  /**
   * To get description for program rule action data field.
   *
   * @param dataExpression of program rule action data field expression.
   * @param programUid {@link Program} which the programRule is associated with.
   * @return RuleValidationResult contains description of program rule condition or errorMessage
   */
  RuleValidationResult getDataExpressionDescription(String dataExpression, UID programUid)
      throws BadRequestException;
}
