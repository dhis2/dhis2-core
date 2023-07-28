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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertReportEntities;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.tracker.JsonImportReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link ImportReport} behavior through {@link TrackerImportController} using (mocked) REST
 * requests
 */
class TrackerImportReportTest extends DhisControllerConvenienceTest {

  private static final String ORG_UNIT_UID = "PSeMWi7rBgb";

  private static final String TE_TYPE_UID = "PrZMWi7rBga";

  private static final String PROGRAM_UID = "XrZRFi7rU9e";

  private static final String PS_UID = "I8RRFi7rUps";

  private static final String REL_TYPE_UID = "A4ORFi7rReL";

  @BeforeEach
  void setUp() {
    TrackedEntityType type = createTrackedEntityType('A');
    type.setUid(TE_TYPE_UID);
    manager.save(type);

    OrganisationUnit orgUnit = createOrganisationUnit('A');
    orgUnit.setUid(ORG_UNIT_UID);
    manager.save(orgUnit);

    Program program = createProgram('A');
    program.setOrganisationUnits(Set.of(orgUnit));
    program.setTrackedEntityType(type);
    program.setUid(PROGRAM_UID);
    manager.save(program);

    ProgramStage programStage = createProgramStage('A', program);
    programStage.setUid(PS_UID);
    manager.save(programStage);

    RelationshipType relType = createPersonToPersonRelationshipType('A', program, type, false);
    relType.setUid(REL_TYPE_UID);
    manager.save(relType);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "import_flatten_payload.json",
        "import_nested_payload.json",
        "import_nested_payload_repeated_relationships.json"
      })
  void shouldReturnOrderedEntitiesInReportWhenImportIsSuccessful(String fileName) {
    JsonImportReport importReport =
        POST(
                "/tracker?async=false&reportMode=FULL"
                    + "&validationMode=SKIP"
                    + "&atomicMode=OBJECT"
                    + "&skipRuleEngine=true",
                WebClient.Body("tracker/" + fileName))
            .content(HttpStatus.OK)
            .as(JsonImportReport.class);

    assertReportEntities(
        List.of("IybbQIQt6te", "daMwzsKN3te", "FRM97UKN8te"), TRACKED_ENTITY, importReport);
    assertReportEntities(List.of("IybbQIQt6en", "daMwzsKN3en"), ENROLLMENT, importReport);
    assertReportEntities(List.of("IybbQIQt6ev", "daMwzsKN3ev"), EVENT, importReport);
    assertReportEntities(List.of("IybbQIQtrel", "daMwzsKNrel"), RELATIONSHIP, importReport);
  }
}
