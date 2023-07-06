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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1008;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.ValidationMode;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.validator.AssertValidations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class MandatoryFieldsValidatorTest {

  private MandatoryFieldsValidator validator;

  @Mock private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {
    validator = new MandatoryFieldsValidator();

    when(bundle.getImportStrategy()).thenReturn(TrackerImportStrategy.CREATE_AND_UPDATE);
    when(bundle.getValidationMode()).thenReturn(ValidationMode.FULL);
    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void verifyEventValidationSuccess() {
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .programStage(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .program(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .build();

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyEventValidationFailsOnMissingProgram() {
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .programStage(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .program(MetadataIdentifier.EMPTY_UID)
            .build();

    validator.validate(reporter, bundle, event);

    assertMissingProperty(reporter, event, "program");
  }

  @Test
  void verifyEventValidationFailsOnMissingProgramStageReferenceToProgram() {
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .programStage(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .build();
    ProgramStage programStage = new ProgramStage();
    programStage.setUid(event.getProgramStage().getIdentifier());
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);

    validator.validate(reporter, bundle, event);

    assertTrue(reporter.hasErrors());
    assertThat(reporter.getErrors(), hasSize(1));
    assertHasError(reporter, event, E1008);
  }

  @Test
  void verifyEventValidationFailsOnMissingProgramStage() {
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .programStage(MetadataIdentifier.EMPTY_UID)
            .program(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .build();

    validator.validate(reporter, bundle, event);

    assertMissingProperty(reporter, event, "programStage");
  }

  @Test
  void verifyEventValidationFailsOnMissingOrgUnit() {
    Event event =
        Event.builder()
            .event(CodeGenerator.generateUid())
            .orgUnit(MetadataIdentifier.EMPTY_UID)
            .programStage(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .program(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .build();

    validator.validate(reporter, bundle, event);

    assertMissingProperty(reporter, event, "orgUnit");
  }

  private void assertMissingProperty(Reporter reporter, TrackerDto dto, String property) {
    AssertValidations.assertMissingProperty(reporter, dto, ValidationCode.E1123, property);
  }
}
