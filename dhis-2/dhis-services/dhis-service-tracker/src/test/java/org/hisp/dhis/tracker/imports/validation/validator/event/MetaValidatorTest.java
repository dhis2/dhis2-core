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

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1010;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1011;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1013;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class MetaValidatorTest {

  private static final String ORG_UNIT_UID = "OrgUnitUid";

  private static final String PROGRAM_UID = "ProgramUid";

  private static final String PROGRAM_STAGE_UID = "ProgramStageUid";

  private MetaValidator validator;

  @Mock private TrackerPreheat preheat;

  @Mock private TrackerBundle bundle;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {
    validator = new MetaValidator();

    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void verifyEventValidationSuccess() {
    Event event = validEvent();

    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_UID)))
        .thenReturn(new OrganisationUnit());
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(new Program());
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(new ProgramStage());

    validator.validate(reporter, bundle, event);

    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void verifyEventValidationFailsWhenProgramIsNotPresentInDb() {
    Event event = validEvent();

    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_UID)))
        .thenReturn(new OrganisationUnit());
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(new ProgramStage());

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1010);
  }

  @Test
  void verifyEventValidationFailsWhenProgramStageIsNotPresentInDb() {
    Event event = validEvent();

    when(preheat.getOrganisationUnit(MetadataIdentifier.ofUid(ORG_UNIT_UID)))
        .thenReturn(new OrganisationUnit());
    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(new Program());

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1013);
  }

  @Test
  void verifyEventValidationFailsWhenOrgUnitIsNotPresentInDb() {
    Event event = validEvent();

    when(preheat.getProgram(MetadataIdentifier.ofUid(PROGRAM_UID))).thenReturn(new Program());
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID)))
        .thenReturn(new ProgramStage());

    validator.validate(reporter, bundle, event);

    assertHasError(reporter, event, E1011);
  }

  private Event validEvent() {
    return Event.builder()
        .event(CodeGenerator.generateUid())
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_UID))
        .orgUnit(MetadataIdentifier.ofUid(ORG_UNIT_UID))
        .program(MetadataIdentifier.ofUid(PROGRAM_UID))
        .build();
  }
}
