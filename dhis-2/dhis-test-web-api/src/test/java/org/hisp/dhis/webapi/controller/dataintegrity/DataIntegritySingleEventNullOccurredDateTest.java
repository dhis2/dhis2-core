/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.dataintegrity;

import java.util.Date;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataIntegritySingleProgramStageInstanceNullOccurredDateTest
    extends AbstractDataIntegrityIntegrationTest {

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  private ProgramStage eventProgramStage;

  private Program eventProgram;

  private ProgramStage trackerProgramStage;

  private Program trackerProgram;

  private OrganisationUnit organisationUnit;

  private static final String CHECK = "single_events_null_occurred_date";

  private static final String DETAILS_ID_TYPE = "events";

  @BeforeEach
  void setup() {
    trackerProgram = createProgram('B');
    trackerProgram.setName("programB");
    trackerProgram.setProgramType(ProgramType.WITH_REGISTRATION);
    programService.addProgram(trackerProgram);

    trackerProgramStage = createProgramStage('B', trackerProgram);
    trackerProgramStage.setName("programStageB");
    programStageService.saveProgramStage(trackerProgramStage);

    eventProgram = createProgram('A');
    eventProgram.setName("programA");
    eventProgram.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programService.addProgram(eventProgram);

    eventProgramStage = createProgramStage('A', eventProgram);
    eventProgramStage.setName("programStageA");
    programStageService.saveProgramStage(eventProgramStage);

    organisationUnit = createOrganisationUnit('A');
    manager.save(organisationUnit);
  }

  @Test
  void shouldFindNoIntegrityIssuesWhenThereIsTrackerProgramStageInstanceWithoutOccurredDate() {
    ProgramInstance enrollment = createProgramInstance(trackerProgram, null, organisationUnit);
    manager.save(enrollment);
    ProgramStageInstance trackerProgramStageInstance =
        createProgramStageInstance(trackerProgramStage, enrollment, organisationUnit);
    manager.save(trackerProgramStageInstance);
    dbmsManager.clearSession();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldFindNoIntegrityIssuesWhenThereIsNoSingleProgramStageInstanceWithoutOccurredDate() {
    ProgramInstance enrollment = createProgramInstance(eventProgram, null, organisationUnit);
    manager.save(enrollment);
    ProgramStageInstance singleProgramStageInstance =
        createProgramStageInstance(eventProgramStage, enrollment, organisationUnit);
    singleProgramStageInstance.setExecutionDate(new Date());
    manager.save(singleProgramStageInstance);
    dbmsManager.clearSession();

    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK, true);
  }

  @Test
  void shouldFindIntegrityIssuesWhenThereIsSingleProgramStageInstanceWithoutOccurredDate() {
    ProgramInstance enrollment = createProgramInstance(eventProgram, null, organisationUnit);
    manager.save(enrollment);
    ProgramStageInstance singleProgramStageInstance =
        createProgramStageInstance(eventProgramStage, enrollment, organisationUnit);
    singleProgramStageInstance.setExecutionDate(new Date());
    manager.save(singleProgramStageInstance);
    ProgramStageInstance singleProgramStageInstanceWithoutOccurredDate =
        createProgramStageInstance(eventProgramStage, enrollment, organisationUnit);
    manager.save(singleProgramStageInstanceWithoutOccurredDate);
    dbmsManager.clearSession();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK,
        50,
        singleProgramStageInstanceWithoutOccurredDate.getUid(),
        singleProgramStageInstanceWithoutOccurredDate.getUid(),
        "single event without occurred date",
        true);
  }
}
