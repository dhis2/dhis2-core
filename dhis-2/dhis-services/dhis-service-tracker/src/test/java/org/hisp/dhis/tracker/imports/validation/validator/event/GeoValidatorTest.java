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

import static org.hisp.dhis.organisationunit.FeatureType.MULTI_POLYGON;
import static org.hisp.dhis.organisationunit.FeatureType.NONE;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1012;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1074;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.FeatureType;
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
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class GeoValidatorTest {

  private static final String PROGRAM_STAGE = "ProgramStage";

  private GeoValidator validator;

  @Mock private TrackerPreheat preheat;

  private TrackerBundle bundle;

  private Reporter reporter;

  @BeforeEach
  public void setUp() {
    validator = new GeoValidator();

    bundle = TrackerBundle.builder().preheat(preheat).build();

    ProgramStage programStage = new ProgramStage();
    programStage.setFeatureType(FeatureType.POINT);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE))).thenReturn(programStage);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void testGeometryIsValid() {
    // given
    Event event = new Event();
    event.setProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE));
    event.setGeometry(new GeometryFactory().createPoint());

    // when
    validator.validate(reporter, bundle, event);

    // then
    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void testEventWithNoProgramStageThrowsAnError() {
    // given
    Event event = new Event();
    event.setProgramStage(null);
    event.setGeometry(new GeometryFactory().createPoint());

    when(preheat.getProgramStage((MetadataIdentifier) null)).thenReturn(null);

    // when
    assertThrows(NullPointerException.class, () -> validator.validate(reporter, bundle, event));
  }

  @Test
  void testProgramStageWithNullFeatureTypeFailsGeometryValidation() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE));
    event.setGeometry(new GeometryFactory().createPoint());

    // when
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(new ProgramStage());

    validator.validate(reporter, bundle, event);

    // then
    assertHasError(reporter, event, E1074);
  }

  @Test
  void testProgramStageWithFeatureTypeNoneFailsGeometryValidation() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE));
    event.setGeometry(new GeometryFactory().createPoint());

    // when
    ProgramStage programStage = new ProgramStage();
    programStage.setFeatureType(NONE);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);

    validator.validate(reporter, bundle, event);

    // then
    assertHasError(reporter, event, E1012);
  }

  @Test
  void testProgramStageWithFeatureTypeDifferentFromGeometryFails() {
    // given
    Event event = new Event();
    event.setEvent(CodeGenerator.generateUid());
    event.setProgramStage(MetadataIdentifier.ofUid(PROGRAM_STAGE));
    event.setGeometry(new GeometryFactory().createPoint());

    // when
    ProgramStage programStage = new ProgramStage();
    programStage.setFeatureType(MULTI_POLYGON);
    when(preheat.getProgramStage(event.getProgramStage())).thenReturn(programStage);

    validator.validate(reporter, bundle, event);

    // then
    assertHasError(reporter, event, E1012);
  }
}
