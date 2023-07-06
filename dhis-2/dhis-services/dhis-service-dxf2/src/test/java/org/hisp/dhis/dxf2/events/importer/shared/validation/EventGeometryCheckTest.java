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
package org.hisp.dhis.dxf2.events.importer.shared.validation;

import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.mockito.Mockito.when;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.events.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class EventGeometryCheckTest extends BaseValidationTest {

  private EventGeometryCheck rule;

  @BeforeEach
  void setUp() {
    rule = new EventGeometryCheck();
  }

  @Test
  void allowEventWithNoGeometry() {
    ProgramStage programStage = createProgramStage();
    when(workContext.getProgramStage(programStageIdScheme, event.getProgramStage()))
        .thenReturn(programStage);
    ImportSummary importSummary = rule.check(new ImmutableEvent(event), workContext);
    assertNoError(importSummary);
  }

  @Test
  void failOnEventWithGeometryAndProgramStageWithNoGeometry() {
    event.setGeometry(createRandomPoint());
    ProgramStage programStage = createProgramStage();
    programStage.setFeatureType(FeatureType.NONE);
    when(workContext.getProgramStage(programStageIdScheme, event.getProgramStage()))
        .thenReturn(programStage);
    ImportSummary importSummary = rule.check(new ImmutableEvent(event), workContext);
    assertHasError(
        importSummary,
        event,
        "Geometry (Point) does not conform to the feature type (None) specified for the program stage: "
            + programStage.getUid());
  }

  private ProgramStage createProgramStage() {
    Program program = createProgram('P');
    return DhisConvenienceTest.createProgramStage('A', program);
  }

  public static Point createRandomPoint() {
    double latitude = (Math.random() * 180.0) - 90.0;
    double longitude = (Math.random() * 360.0) - 180.0;
    GeometryFactory geometryFactory = new GeometryFactory();
    /* Longitude (= x coord) first ! */
    return geometryFactory.createPoint(new Coordinate(longitude, latitude));
  }
}
