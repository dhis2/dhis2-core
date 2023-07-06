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
package org.hisp.dhis.merge.orgunit.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class TrackerOrgUnitMergeHandlerTest extends SingleSetupIntegrationTestBase {

  @Autowired private TrackedEntityInstanceService teiService;

  @Autowired private ProgramInstanceService piService;

  @Autowired private ProgramStageInstanceService psiService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private TrackerOrgUnitMergeHandler mergeHandler;

  @Autowired private SessionFactory sessionFactory;

  private ProgramStage psA;

  private Program prA;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private TrackedEntityInstance teiA;

  private TrackedEntityInstance teiB;

  private TrackedEntityInstance teiC;

  private ProgramInstance piA;

  private ProgramInstance piB;

  private ProgramInstance piC;

  private ProgramStageInstance psiA;

  private ProgramStageInstance psiB;

  private ProgramStageInstance psiC;

  @Override
  public void setUpTest() {
    prA = createProgram('A', Sets.newHashSet(), ouA);
    idObjectManager.save(prA);
    psA = createProgramStage('A', prA);
    idObjectManager.save(psA);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    idObjectManager.save(ouA);
    idObjectManager.save(ouB);
    idObjectManager.save(ouC);
    teiA = createTrackedEntityInstance('A', ouA);
    teiB = createTrackedEntityInstance('B', ouB);
    teiC = createTrackedEntityInstance('C', ouC);
    teiService.addTrackedEntityInstance(teiA);
    teiService.addTrackedEntityInstance(teiB);
    teiService.addTrackedEntityInstance(teiC);
    piA = createProgramInstance(prA, teiA, ouA);
    piB = createProgramInstance(prA, teiB, ouB);
    piC = createProgramInstance(prA, teiC, ouA);
    piService.addProgramInstance(piA);
    piService.addProgramInstance(piB);
    piService.addProgramInstance(piC);
    psiA = new ProgramStageInstance(piA, psA, ouA);
    psiB = new ProgramStageInstance(piB, psA, ouB);
    psiC = new ProgramStageInstance(piC, psA, ouA);
    psiService.addProgramStageInstance(psiA);
    psiService.addProgramStageInstance(psiB);
    psiService.addProgramStageInstance(psiC);
  }

  @Test
  void testMigrateProgramInstances() {
    assertEquals(2, getProgramInstanceCount(ouA));
    assertEquals(1, getProgramInstanceCount(ouB));
    assertEquals(0, getProgramInstanceCount(ouC));
    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).withTarget(ouC).build();
    mergeHandler.mergeProgramInstances(request);
    assertEquals(0, getProgramInstanceCount(ouA));
    assertEquals(0, getProgramInstanceCount(ouB));
    assertEquals(3, getProgramInstanceCount(ouC));
  }

  /**
   * Test migrate HQL update statement with an HQL select statement to ensure the updated rows are
   * visible by the current transaction.
   *
   * @param target the {@link OrganisationUnit}
   * @return the count of interpretations.
   */
  private long getProgramInstanceCount(OrganisationUnit target) {
    return (Long)
        sessionFactory
            .getCurrentSession()
            .createQuery(
                "select count(*) from ProgramInstance pi where pi.organisationUnit = :target")
            .setParameter("target", target)
            .uniqueResult();
  }
}
