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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class ProgramServiceTest extends TransactionalIntegrationTest {

  @Autowired private ProgramService programService;

  @Autowired private OrganisationUnitService organisationUnitService;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private Program programA;

  private Program programB;

  private Program programC;

  @Override
  public void setUpTest() {
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);
    programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programA.setUid("UID-A");
    programB = createProgram('B', new HashSet<>(), organisationUnitA);
    programB.setUid("UID-B");
    programC = createProgram('C', new HashSet<>(), organisationUnitB);
    programC.setUid("UID-C");
  }

  @Test
  void testAddProgram() {
    long idA = programService.addProgram(programA);
    long idB = programService.addProgram(programB);
    assertNotNull(programService.getProgram(idA));
    assertNotNull(programService.getProgram(idB));
  }

  @Test
  void testUpdateProgram() {
    long idA = programService.addProgram(programA);
    assertNotNull(programService.getProgram(idA));
    programA.setName("B");
    programService.updateProgram(programA);
    assertEquals("B", programService.getProgram(idA).getName());
  }

  @Test
  void testDeleteProgram() {
    long idA = programService.addProgram(programA);
    long idB = programService.addProgram(programB);
    assertNotNull(programService.getProgram(idA));
    assertNotNull(programService.getProgram(idB));
    programService.deleteProgram(programA);
    assertNull(programService.getProgram(idA));
    assertNotNull(programService.getProgram(idB));
    programService.deleteProgram(programB);
    assertNull(programService.getProgram(idA));
    assertNull(programService.getProgram(idB));
  }

  @Test
  void testGetProgramById() {
    long idA = programService.addProgram(programA);
    long idB = programService.addProgram(programB);
    assertEquals(programA, programService.getProgram(idA));
    assertEquals(programB, programService.getProgram(idB));
  }

  @Test
  void testGetAllPrograms() {
    programService.addProgram(programA);
    programService.addProgram(programB);
    assertTrue(equals(programService.getAllPrograms(), programA, programB));
  }

  @Test
  void testGetProgramsByOu() {
    programService.addProgram(programA);
    programService.addProgram(programB);
    programService.addProgram(programC);
    List<Program> programs = programService.getPrograms(organisationUnitA);
    assertTrue(equals(programs, programA, programB));
    programs = programService.getPrograms(organisationUnitB);
    assertTrue(equals(programs, programC));
  }

  @Test
  void testGetProgramByUid() {
    programService.addProgram(programA);
    programService.addProgram(programB);
    assertEquals(programA, programService.getProgram("UID-A"));
    assertEquals(programB, programService.getProgram("UID-B"));
  }

  @Test
  void testProgramHasOrgUnit() {
    programService.addProgram(programA);
    Program p = programService.getProgram(programA.getUid());
    OrganisationUnit ou = organisationUnitService.getOrganisationUnit(organisationUnitA.getUid());
    assertTrue(programService.hasOrgUnit(p, ou));
  }
}
