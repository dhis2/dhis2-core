/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.metadata.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramSection;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MetadataExportWithDependenciesTest extends SingleSetupIntegrationTestBase {

  @PersistenceContext private EntityManager entityManager;
  @Autowired private MetadataExportService metadataExportService;

  @Test
  void testExportProgramWithProgramSection() {
    Program program = createProgram('A');
    entityManager.persist(program);
    ProgramSection programSection = createProgramSection('A', program);
    program.getProgramSections().add(programSection);
    entityManager.persist(programSection);
    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> export =
        metadataExportService.getMetadataWithDependencies(program);

    assertEquals(1, export.get(Program.class).size());
    assertEquals(1, export.get(ProgramSection.class).size());
  }

  @Test
  @DisplayName(
      "ProgramTrackedEntityAttributes and ProgramStageDataElements should not appear at root level in dependency export")
  void testExportProgramWithProgramStageDataElements() {
    Program program = setUpProgramAndDependencies();
    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> export =
        metadataExportService.getMetadataWithDependencies(program);

    Set<IdentifiableObject> programs = export.get(Program.class);
    Set<IdentifiableObject> programStages = export.get(ProgramStage.class);
    assertEquals(1, programs.size());
    assertEquals(1, programStages.size());

    Program p = (Program) programs.iterator().next();
    ProgramStage ps = (ProgramStage) programStages.iterator().next();

    assertEquals(1, p.getProgramAttributes().size());
    assertEquals(1, ps.getProgramStageDataElements().size());
    assertNull(export.get(ProgramTrackedEntityAttribute.class));
    assertNull(export.get(ProgramStageDataElement.class));
  }

  private Program setUpProgramAndDependencies() {
    Program program = createProgram('A');
    entityManager.persist(program);
    ProgramSection programSection = createProgramSection('A', program);
    program.getProgramSections().add(programSection);
    entityManager.persist(programSection);
    TrackedEntityAttribute trackedEntityAttribute =
        createTrackedEntityAttribute('A', ValueType.TEXT);
    entityManager.persist(trackedEntityAttribute);
    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(program, trackedEntityAttribute);
    program.getProgramAttributes().add(programTrackedEntityAttribute);
    entityManager.persist(programTrackedEntityAttribute);
    ProgramStage programStage = createProgramStage('A', program);
    entityManager.persist(programStage);
    program.getProgramStages().add(programStage);
    entityManager.merge(program);

    DataElement dataElement = createDataElement('A');
    entityManager.persist(dataElement);
    ProgramStageDataElement programStageDataElement =
        createProgramStageDataElement(programStage, dataElement, 0);
    entityManager.persist(programStageDataElement);
    programStage.getProgramStageDataElements().add(programStageDataElement);
    entityManager.merge(programStage);
    return program;
  }
}
