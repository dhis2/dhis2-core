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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1048;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;

import com.google.common.collect.Lists;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Enrico Colasante
 */
class UidValidatorTest {

  private static final String INVALID_UID = "InvalidUID";

  private UidValidator validator;

  private TrackerBundle bundle;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    TrackerPreheat preheat = new TrackerPreheat();
    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    preheat.setIdSchemes(idSchemes);
    reporter = new Reporter(idSchemes);
    bundle = TrackerBundle.builder().preheat(preheat).build();

    validator = new UidValidator();
  }

  @Test
  void verifyEnrollmentValidationSuccess() {
    Note note = Note.builder().note(CodeGenerator.generateUid()).build();
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .notes(Lists.newArrayList(note))
            .build();

    validator.validate(reporter, bundle, enrollment);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyEnrollmentWithInvalidUidFails() {
    Enrollment enrollment = Enrollment.builder().enrollment(INVALID_UID).build();

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1048);
  }

  @Test
  void verifyEnrollmentWithNoteWithInvalidUidFails() {
    Note note = Note.builder().note(INVALID_UID).build();
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(CodeGenerator.generateUid())
            .notes(Lists.newArrayList(note))
            .build();

    validator.validate(reporter, bundle, enrollment);

    assertHasError(reporter, enrollment, E1048);
  }
}
