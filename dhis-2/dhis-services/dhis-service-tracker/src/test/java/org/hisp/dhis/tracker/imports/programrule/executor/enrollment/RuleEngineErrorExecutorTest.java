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
package org.hisp.dhis.tracker.imports.programrule.executor.enrollment;

import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.warning;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1300;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleEngineErrorExecutorTest extends TestBase {
  private static final UID RULE_UID = UID.of("TvctPPhpD8u");

  private static final String ENROLLMENT_ERROR_MESSAGE = "Enrollment error message";

  private static final UID ENROLLMENT_ID = UID.generate();

  private static final UID TE_ID = UID.generate();

  private final RuleEngineErrorExecutor executor =
      new RuleEngineErrorExecutor(RULE_UID, ENROLLMENT_ERROR_MESSAGE);

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @BeforeEach
  void setUpTest() {
    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);
  }

  @Test
  void shouldReturnAWarningWhenThereIsSyntaxErrorInRule() {
    Optional<ProgramRuleIssue> warning = executor.executeRuleAction(bundle, getEnrollment());

    assertTrue(warning.isPresent());
    assertEquals(warning(RULE_UID, E1300, ENROLLMENT_ERROR_MESSAGE), warning.get());
  }

  private Enrollment getEnrollment() {
    return Enrollment.builder().enrollment(ENROLLMENT_ID).trackedEntity(TE_ID).build();
  }
}
