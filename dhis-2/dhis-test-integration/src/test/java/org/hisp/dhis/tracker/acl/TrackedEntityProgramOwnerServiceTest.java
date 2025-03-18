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
package org.hisp.dhis.tracker.acl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class TrackedEntityProgramOwnerServiceTest extends PostgresIntegrationTestBase {

  private static final String PA = "PA";

  private static final String TE_B1 = "TE-B1";

  private static final String TE_A1 = "TE-A1";

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityProgramOwnerService programOwnerService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private Program programA;

  private TrackedEntity trackedEntityA1;

  private TrackedEntity trackedEntityB1;

  @BeforeAll
  void setUp() {
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);
    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    trackedEntityA1 = createTrackedEntity(organisationUnitA, trackedEntityType);
    trackedEntityA1.setUid(TE_A1);
    trackedEntityB1 = createTrackedEntity(organisationUnitA, trackedEntityType);
    trackedEntityB1.setUid(TE_B1);
    manager.save(trackedEntityA1);
    manager.save(trackedEntityB1);
    programA = createProgram('A');
    programA.setUid(PA);
    programService.addProgram(programA);
  }

  @Test
  void testCreateTrackedEntityProgramOwner() {
    programOwnerService.createTrackedEntityProgramOwner(
        trackedEntityA1, programA, organisationUnitA);
    assertNotNull(programOwnerService.getTrackedEntityProgramOwner(trackedEntityA1, programA));
    assertNull(programOwnerService.getTrackedEntityProgramOwner(trackedEntityB1, programA));
  }

  @Test
  void testCreateOrUpdateTrackedEntityProgramOwner() {
    programOwnerService.createOrUpdateTrackedEntityProgramOwner(
        trackedEntityA1, programA, organisationUnitA);
    TrackedEntityProgramOwner programOwner =
        programOwnerService.getTrackedEntityProgramOwner(trackedEntityA1, programA);
    assertNotNull(programOwner);
    assertEquals(organisationUnitA.getUid(), programOwner.getOrganisationUnit().getUid());
    programOwnerService.createOrUpdateTrackedEntityProgramOwner(
        trackedEntityA1, programA, organisationUnitB);
    programOwner = programOwnerService.getTrackedEntityProgramOwner(trackedEntityA1, programA);
    assertNotNull(programOwner);
    assertEquals(organisationUnitB.getUid(), programOwner.getOrganisationUnit().getUid());
  }
}
