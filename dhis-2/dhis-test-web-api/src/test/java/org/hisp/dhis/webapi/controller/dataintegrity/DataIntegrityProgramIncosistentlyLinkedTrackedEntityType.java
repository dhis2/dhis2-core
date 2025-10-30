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
package org.hisp.dhis.webapi.controller.dataintegrity;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for programs which are inconsistently linked to tracked entity types. {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/
 * data-integrity-checks/programs/programs_inconsistent_tracked_entity_type.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityProgramIncosistentlyLinkedTrackedEntityType
    extends AbstractDataIntegrityIntegrationTest {

  @Autowired private ProgramService programService;
  private static final String DETAILS_ID_TYPE = "programs";
  private static final String CHECK_NAME = "programs_inconsistent_tracked_entity_type";

  @Test
  void testSingleEventProgramWithTrackedEntityType() {
    // Single event with no tracked entity type should not be flagged
    // Use the service layer, since hopefully the API layer will block this in future
    Program programA = new Program();
    programA.setAutoFields();
    programA.setName("Program A");
    programA.setShortName("Program A");
    programA.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programA.setCategoryCombo(categoryService.getCategoryCombo(getDefaultCatCombo()));
    programService.addProgram(programA);

    TrackedEntityType tet = createTrackedEntityType('A');
    manager.save(tet);

    // Single event with tracked entity type should be flagged
    Program programB = new Program();
    programB.setAutoFields();
    programB.setName("Program B");
    programB.setShortName("Program B");
    programB.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    programB.setCategoryCombo(categoryService.getCategoryCombo(getDefaultCatCombo()));
    programB.setTrackedEntityType(tet);
    programService.addProgram(programB);

    dbmsManager.clearSession();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK_NAME,
        50,
        programB.getUid(),
        programB.getName(),
        "Single event with tracked entity type",
        true);
  }

  @Test
  void testTrackerProgramWithoutTrackedEntityType() {
    // Tracker program without tracked entity type should be flagged
    Program programA = new Program();
    programA.setAutoFields();
    programA.setName("Program A");
    programA.setShortName("Program A");
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    programA.setCategoryCombo(categoryService.getCategoryCombo(getDefaultCatCombo()));
    programService.addProgram(programA);

    TrackedEntityType tet = createTrackedEntityType('A');
    manager.save(tet);

    // Tracker program with tracked entity type should not be flagged
    Program programB = new Program();
    programB.setAutoFields();
    programB.setName("Program B");
    programB.setShortName("Program B");
    programB.setProgramType(ProgramType.WITH_REGISTRATION);
    programB.setCategoryCombo(categoryService.getCategoryCombo(getDefaultCatCombo()));
    programB.setTrackedEntityType(tet);
    programService.addProgram(programB);

    dbmsManager.clearSession();

    assertHasDataIntegrityIssues(
        DETAILS_ID_TYPE,
        CHECK_NAME,
        50,
        programA.getUid(),
        programA.getName(),
        "Tracker program without tracked entity type",
        true);
  }

  @Test
  void testCheckRuns() {
    assertHasNoDataIntegrityIssues(DETAILS_ID_TYPE, CHECK_NAME, false);
  }
}
