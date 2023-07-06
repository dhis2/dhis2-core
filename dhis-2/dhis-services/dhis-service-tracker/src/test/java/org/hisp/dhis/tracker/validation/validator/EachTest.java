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
package org.hisp.dhis.tracker.validation.validator;

import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.UPDATE;
import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.validation.validator.Each.each;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.validation.Error;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.hisp.dhis.tracker.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EachTest {
  private Reporter reporter;

  private TrackerBundle bundle;

  @BeforeEach
  void setUp() {
    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
    bundle = TrackerBundle.builder().build();
  }

  @Test
  void testCallsValidatorForEachItemInCollection() {
    // @formatter:off
    Validator<Enrollment> validator =
        each(Enrollment::getNotes, (r, b, n) -> addError(r, n.getNote()));

    validator.validate(reporter, bundle, enrollment("Kj6vYde4LHh", "V1", "V2", "V3"));

    // order of input collection is preserved
    assertEquals(List.of("V1", "V2", "V3"), actualErrorMessages());
  }

  @Test
  void testDoesNotCallValidatorIfItShouldNotRunOnGivenStrategy() {
    bundle = TrackerBundle.builder().importStrategy(UPDATE).build();

    Validator<Enrollment> validator =
        each(
            Enrollment::getNotes,
            new Validator<>() {
              @Override
              public void validate(Reporter reporter, TrackerBundle bundle, Note input) {
                addError(reporter, "V1");
              }

              @Override
              public boolean needsToRun(TrackerImportStrategy strategy) {
                return strategy == DELETE;
              }
            });

    validator.validate(reporter, bundle, enrollment("Kj6vYde4LHh", "input1", "input2", "input2"));

    assertIsEmpty(actualErrorMessages());
  }

  @Test
  void testDoesNotCallValidatorIfItShouldNotRunOnGivenStrategyForATrackerDto() {
    bundle =
        TrackerBundle.builder()
            .importStrategy(CREATE_AND_UPDATE)
            .resolvedStrategyMap(
                new EnumMap<>(
                    Map.of(
                        ENROLLMENT,
                        Map.of(
                            "Kj6vYde4LHh", UPDATE,
                            "Nav6inZRw1u", CREATE))))
            .enrollments(List.of(enrollment("Kj6vYde4LHh"), enrollment("Nav6inZRw1u")))
            .build();

    Validator<TrackerBundle> validator =
        each(
            TrackerBundle::getEnrollments,
            new Validator<>() {
              @Override
              public void validate(Reporter reporter, TrackerBundle bundle, Enrollment enrollment) {
                addError(reporter, enrollment.getEnrollment());
              }

              @Override
              public boolean needsToRun(TrackerImportStrategy strategy) {
                return strategy == CREATE;
              }
            });

    validator.validate(reporter, bundle, bundle);

    assertContainsOnly(List.of("Nav6inZRw1u"), actualErrorMessages());
  }

  private static Enrollment enrollment(String uid, String... notes) {
    List<Note> n =
        Arrays.stream(notes).map(s -> Note.builder().note(s).build()).collect(Collectors.toList());

    return Enrollment.builder().enrollment(uid).notes(n).build();
  }

  /**
   * Add error with given message to {@link Reporter}. Every {@link Error} is attributed to a {@link
   * TrackerDto}, which makes adding errors cumbersome when you do not care about any particular
   * tracker type, uid or error code.
   */
  private static void addError(Reporter reporter, String message) {
    reporter.addError(new Error(message, ValidationCode.E9999, TrackerType.TRACKED_ENTITY, "uid"));
  }

  private List<String> actualErrorMessages() {
    return reporter.getErrors().stream().map(Error::getMessage).collect(Collectors.toList());
  }
}
