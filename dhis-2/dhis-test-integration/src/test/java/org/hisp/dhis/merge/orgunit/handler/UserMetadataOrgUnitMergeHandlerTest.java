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

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class UserMetadataOrgUnitMergeHandlerTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private MetadataOrgUnitMergeHandler handler;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  @BeforeEach
  void beforeTest() {
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    idObjectManager.save(ouA);
    idObjectManager.save(ouB);
    idObjectManager.save(ouC);
  }

  @Test
  void testMergeUsers() {
    User userA = makeUser("A");
    userA.addOrganisationUnit(ouA);
    userA.getDataViewOrganisationUnits().add(ouA);
    userA.getTeiSearchOrganisationUnits().add(ouA);
    User userB = makeUser("B");
    userB.addOrganisationUnit(ouB);
    userB.getDataViewOrganisationUnits().add(ouB);
    userB.getTeiSearchOrganisationUnits().add(ouB);
    userService.addUser(userA);
    userService.addUser(userB);
    assertTrue(ouA.getUsers().contains(userA));
    assertTrue(userA.getOrganisationUnits().contains(ouA));
    assertTrue(ouB.getUsers().contains(userB));
    assertTrue(userB.getOrganisationUnits().contains(ouB));
    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder().addSource(ouA).addSource(ouB).withTarget(ouC).build();
    handler.mergeUsers(request);
    assertTrue(ouA.getUsers().isEmpty());
    assertFalse(userA.getOrganisationUnits().contains(ouA));
    assertFalse(userA.getDataViewOrganisationUnits().contains(ouA));
    assertFalse(userA.getTeiSearchOrganisationUnits().contains(ouA));
    assertTrue(ouB.getUsers().isEmpty());
    assertFalse(userB.getOrganisationUnits().contains(ouB));
    assertFalse(userB.getDataViewOrganisationUnits().contains(ouB));
    assertFalse(userB.getTeiSearchOrganisationUnits().contains(ouB));
    assertTrue(ouC.getUsers().contains(userA));
    assertTrue(ouC.getUsers().contains(userB));
    assertContainsOnly(Set.of(ouC), userA.getOrganisationUnits());
    assertContainsOnly(Set.of(ouC), userA.getDataViewOrganisationUnits());
    assertContainsOnly(Set.of(ouC), userA.getTeiSearchOrganisationUnits());
    assertContainsOnly(Set.of(ouC), userB.getOrganisationUnits());
    assertContainsOnly(Set.of(ouC), userB.getDataViewOrganisationUnits());
    assertContainsOnly(Set.of(ouC), userB.getTeiSearchOrganisationUnits());
  }
}
