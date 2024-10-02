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
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.UserDetails;

public interface ProgramRuleEngine {
  RuleEngineEffects evaluateEnrollmentAndEvents(
      Enrollment enrollment,
      Set<Event> events,
      List<TrackedEntityAttributeValue> trackedEntityAttributeValues,
      UserDetails user);

  RuleEngineEffects evaluateProgramEvents(Set<Event> events, Program program, UserDetails user);

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
