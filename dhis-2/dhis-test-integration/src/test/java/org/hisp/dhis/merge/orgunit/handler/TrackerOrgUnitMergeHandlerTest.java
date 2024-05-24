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
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class TrackerOrgUnitMergeHandlerTest extends SingleSetupIntegrationTestBase {

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private EnrollmentService piService;

  @Autowired private EventService eventService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private TrackerOrgUnitMergeHandler mergeHandler;

  @PersistenceContext private EntityManager entityManager;

  private ProgramStage psA;

  private Program prA;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private TrackedEntity trackedEntityA;

  private TrackedEntity trackedEntityB;

  private TrackedEntity trackedEntityC;

  private Enrollment enrollmentA;

  private Enrollment enrollmentB;

  private Enrollment enrollmentC;

  private Event eventA;

  private Event eventB;

  private Event eventC;

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
    trackedEntityA = createTrackedEntity('A', ouA);
    trackedEntityB = createTrackedEntity('B', ouB);
    trackedEntityC = createTrackedEntity('C', ouC);
    trackedEntityService.addTrackedEntity(trackedEntityA);
    trackedEntityService.addTrackedEntity(trackedEntityB);
    trackedEntityService.addTrackedEntity(trackedEntityC);
    enrollmentA = createEnrollment(prA, trackedEntityA, ouA);
    enrollmentB = createEnrollment(prA, trackedEntityB, ouB);
    enrollmentC = createEnrollment(prA, trackedEntityC, ouA);
    piService.addEnrollment(enrollmentA);
    piService.addEnrollment(enrollmentB);
    piService.addEnrollment(enrollmentC);
    eventA = new Event(enrollmentA, psA, ouA);
    eventB = new Event(enrollmentB, psA, ouB);
    eventC = new Event(enrollmentC, psA, ouA);
    eventService.addEvent(eventA);
    eventService.addEvent(eventB);
    eventService.addEvent(eventC);
  }

  @Test
  void testMigrateEnrollments() {
    assertEquals(2, getEnrollmentCount(ouA));
    assertEquals(1, getEnrollmentCount(ouB));
    assertEquals(0, getEnrollmentCount(ouC));
    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).withTarget(ouC).build();
    mergeHandler.mergeEnrollments(request);
    assertEquals(0, getEnrollmentCount(ouA));
    assertEquals(0, getEnrollmentCount(ouB));
    assertEquals(3, getEnrollmentCount(ouC));
  }

  /**
   * Test migrate HQL update statement with an HQL select statement to ensure the updated rows are
   * visible by the current transaction.
   *
   * @param target the {@link OrganisationUnit}
   * @return the count of interpretations.
   */
  private long getEnrollmentCount(OrganisationUnit target) {
    return (Long)
        entityManager
            .createQuery("select count(*) from Enrollment pi where pi.organisationUnit = :target")
            .setParameter("target", target)
            .getSingleResult();
  }
}
