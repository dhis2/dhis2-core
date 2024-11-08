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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.imports.bundle.persister.TrackerObjectDeletionService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class TrackedEntityServiceTest extends PostgresIntegrationTestBase {
  private static final String TE_A_UID = uidWithPrefix('A');
  private static final String TE_B_UID = uidWithPrefix('B');
  private static final String TE_C_UID = uidWithPrefix('C');
  private static final String TE_D_UID = uidWithPrefix('D');

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private TrackedEntityAttributeService trackedEntityAttributeService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerObjectDeletionService trackerObjectDeletionService;

  private TrackedEntity trackedEntityA1;

  @BeforeEach
  void setUp() {
    TrackedEntityAttribute attrD = createTrackedEntityAttribute('D');
    TrackedEntityAttribute attrE = createTrackedEntityAttribute('E');
    TrackedEntityAttribute filtF = createTrackedEntityAttribute('F');
    TrackedEntityAttribute filtG = createTrackedEntityAttribute('G');
    TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute('H');

    trackedEntityAttributeService.addTrackedEntityAttribute(attrD);
    trackedEntityAttributeService.addTrackedEntityAttribute(attrE);
    trackedEntityAttributeService.addTrackedEntityAttribute(filtF);
    trackedEntityAttributeService.addTrackedEntityAttribute(filtG);
    trackedEntityAttributeService.addTrackedEntityAttribute(trackedEntityAttribute);

    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    OrganisationUnit organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);
    attributeService.addTrackedEntityAttribute(createTrackedEntityAttribute('A'));
    trackedEntityA1 = createTrackedEntity(organisationUnit);
    TrackedEntity trackedEntityB1 = createTrackedEntity(organisationUnit);
    TrackedEntity trackedEntityC1 = createTrackedEntity(organisationUnit);
    TrackedEntity trackedEntityD1 = createTrackedEntity(organisationUnit);
    trackedEntityA1.setUid(TE_A_UID);
    trackedEntityB1.setUid(TE_B_UID);
    trackedEntityC1.setUid(TE_C_UID);
    trackedEntityD1.setUid(TE_D_UID);

    attributeService.addTrackedEntityAttribute(attrD);
    attributeService.addTrackedEntityAttribute(attrE);
    attributeService.addTrackedEntityAttribute(filtF);
    attributeService.addTrackedEntityAttribute(filtG);

    User user = createUserWithAuth("testUser");
    user.setTeiSearchOrganisationUnits(Set.of(organisationUnit));
    userService.addUser(user);
    injectSecurityContextUser(user);
  }

  @Test
  void testDeleteTrackedEntityAndLinkedEnrollmentsAndEvents() throws NotFoundException {
    manager.save(trackedEntityA1);
    manager.update(trackedEntityA1);
    TrackedEntity trackedEntityA = manager.get(TrackedEntity.class, trackedEntityA1.getUid());
    assertNotNull(trackedEntityA);
    trackerObjectDeletionService.deleteTrackedEntities(List.of(UID.of(trackedEntityA)));
    assertNull(manager.get(TrackedEntity.class, trackedEntityA.getUid()));
  }

  private static String uidWithPrefix(char prefix) {
    String value = prefix + CodeGenerator.generateUid().substring(0, 10);
    return UID.of(value).getValue();
  }
}
