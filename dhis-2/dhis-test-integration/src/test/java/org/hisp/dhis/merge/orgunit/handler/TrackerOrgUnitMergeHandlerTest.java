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
package org.hisp.dhis.merge.orgunit.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class TrackerOrgUnitMergeHandlerTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerOrgUnitMergeHandler mergeHandler;

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

  private TrackerEvent eventA;

  private TrackerEvent eventB;

  private TrackerEvent eventC;

  @BeforeAll
  void setUp() {
    prA = createProgram('A', Sets.newHashSet(), ouA);
    manager.save(prA);
    psA = createProgramStage('A', prA);
    manager.save(psA);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    manager.save(ouA);
    manager.save(ouB);
    manager.save(ouC);

    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    trackedEntityA = createTrackedEntity('A', ouA, trackedEntityType);
    trackedEntityB = createTrackedEntity('B', ouB, trackedEntityType);
    trackedEntityC = createTrackedEntity('C', ouC, trackedEntityType);
    manager.save(trackedEntityA);
    manager.save(trackedEntityB);
    manager.save(trackedEntityC);
    enrollmentA = createEnrollment(prA, trackedEntityA, ouA);
    enrollmentB = createEnrollment(prA, trackedEntityB, ouB);
    enrollmentC = createEnrollment(prA, trackedEntityC, ouA);
    manager.save(enrollmentA);
    manager.save(enrollmentB);
    manager.save(enrollmentC);
    eventA = createEvent(psA, enrollmentA, ouA);
    eventB = createEvent(psA, enrollmentB, ouB);
    eventC = createEvent(psA, enrollmentC, ouA);
    manager.save(eventA);
    manager.save(eventB);
    manager.save(eventC);
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
            .createQuery("select count(*) from Enrollment en where en.organisationUnit = :target")
            .setParameter("target", target)
            .getSingleResult();
  }
}
