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
package org.hisp.dhis.trackedentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
class TrackedEntityProgramOwnerServiceTest extends SingleSetupIntegrationTestBase {

  private static final String PA = "PA";

  private static final String TEIB1 = "TEI-B1";

  private static final String TEIA1 = "TEI-A1";

  @Autowired private TrackedEntityInstanceService entityInstanceService;

  @Autowired private TrackedEntityProgramOwnerService programOwnerService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  @Override
  public void setUpTest() {
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);
    TrackedEntityInstance entityInstanceA1 = createTrackedEntityInstance(organisationUnitA);
    entityInstanceA1.setUid(TEIA1);
    TrackedEntityInstance entityInstanceB1 = createTrackedEntityInstance(organisationUnitA);
    entityInstanceB1.setUid(TEIB1);
    entityInstanceService.addTrackedEntityInstance(entityInstanceA1);
    entityInstanceService.addTrackedEntityInstance(entityInstanceB1);
    Program programA = createProgram('A');
    programA.setUid(PA);
    programService.addProgram(programA);
  }

  @Test
  void testCreateTrackedEntityProgramOwner() {
    programOwnerService.createTrackedEntityProgramOwner(TEIA1, PA, organisationUnitA.getUid());
    assertNotNull(programOwnerService.getTrackedEntityProgramOwner(TEIA1, PA));
    assertNull(programOwnerService.getTrackedEntityProgramOwner(TEIB1, PA));
  }

  @Test
  void testCreateOrUpdateTrackedEntityProgramOwner() {
    programOwnerService.createOrUpdateTrackedEntityProgramOwner(
        TEIA1, PA, organisationUnitA.getUid());
    TrackedEntityProgramOwner programOwner =
        programOwnerService.getTrackedEntityProgramOwner(TEIA1, PA);
    assertNotNull(programOwner);
    assertEquals(organisationUnitA.getUid(), programOwner.getOrganisationUnit().getUid());
    programOwnerService.createOrUpdateTrackedEntityProgramOwner(
        TEIA1, PA, organisationUnitB.getUid());
    programOwner = programOwnerService.getTrackedEntityProgramOwner(TEIA1, PA);
    assertNotNull(programOwner);
    assertEquals(organisationUnitB.getUid(), programOwner.getOrganisationUnit().getUid());
  }
}
