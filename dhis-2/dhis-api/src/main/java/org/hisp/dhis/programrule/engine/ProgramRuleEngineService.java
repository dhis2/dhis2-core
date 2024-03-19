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
package org.hisp.dhis.programrule.engine;

import java.util.List;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleValidationResult;

/** Created by zubair@dhis2.org on 23.10.17. */
public interface ProgramRuleEngineService {
  /**
   * Call rule engine to evaluate the target enrollment and get a list of rule effects, then run the
   * actions present in these effects.
   *
   * @param enrollment identifier of the target enrollment.
   * @return the list of rule effects calculated by rule engine.
   */
  List<RuleEffect> evaluateEnrollmentAndRunEffects(long enrollment);

  /**
   * Call rule engine to evaluate the target event and get a list of rule effects, then run the
   * actions present in these effects.
   *
   * @param event identifier (uid) of the target event.
   * @return the list of rule effects calculated by rule engine
   */
  List<RuleEffect> evaluateEventAndRunEffects(String event);

  /**
   * Gets the description of program rule condition. This also provides run time validation for
   * program rule condition.
   *
   * @param condition to get description for.
   * @param programId program id which program rule is associated to.
   * @return {@link RuleValidationResult}
   */
  RuleValidationResult getDescription(String condition, String programId);

  /**
   * Gets the description of program rule action data field.
   *
   * @param dataExpression to get description for.
   * @param programId program id which program rule is associated to.
   * @return {@link RuleValidationResult}
   */
  RuleValidationResult getDataExpressionDescription(String dataExpression, String programId);
}
