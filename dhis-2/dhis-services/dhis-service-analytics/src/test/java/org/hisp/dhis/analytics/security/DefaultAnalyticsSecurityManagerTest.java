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
package org.hisp.dhis.analytics.security;

import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultAnalyticsSecurityManagerTest extends TestBase {

  @Mock private DataApprovalLevelService approvalLevelService;
  @Mock private SystemSettingsProvider settingsProvider;
  @Mock private DimensionService dimensionService;
  @Mock private AclService aclService;
  @Mock private UserService userService;
  @Mock private User currentUser;

  @InjectMocks private DefaultAnalyticsSecurityManager securityManager;

  private OrganisationUnit ouA;
  private OrganisationUnit ouB;

  @BeforeEach
  void setUp() {
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');

    when(userService.getUserByUsername(nullable(String.class))).thenReturn(currentUser);
    when(currentUser.isSuper()).thenReturn(true);
    when(currentUser.getDimensionConstraints()).thenReturn(Set.of());
    lenient().when(currentUser.hasDataViewOrganisationUnit()).thenReturn(true);
    lenient().when(currentUser.getDataViewOrganisationUnits()).thenReturn(Set.of(ouB));
    lenient().when(currentUser.getUsername()).thenReturn("tester");

    UserDetails currentUserDetails = UserDetails.empty().username("tester").build();
    CurrentUserUtil.injectUserInSecurityContext(currentUserDetails);
  }

  @AfterEach
  void tearDown() {
    CurrentUserUtil.clearSecurityContext();
  }

  @Test
  void shouldNotAssignDefaultOuConstraintWhenEnrollmentOuIsUsedWithoutExplicitOu() {
    EventQueryParams params =
        new EventQueryParams.Builder().withEnrollmentOuFilter(List.of(ouA)).build();

    EventQueryParams constrained = securityManager.withUserConstraints(params);

    assertTrue(constrained.getDimensionOrFilterItems(ORGUNIT_DIM_ID).isEmpty());
    assertTrue(constrained.hasEnrollmentOu());
  }

  @Test
  void shouldKeepExplicitOuWhenEnrollmentOuIsAlsoUsed() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withOrganisationUnits(List.of(ouA))
            .withEnrollmentOuFilter(List.of(ouB))
            .build();

    EventQueryParams constrained = securityManager.withUserConstraints(params);
    List<DimensionalItemObject> constrainedOus =
        constrained.getDimensionOrFilterItems(ORGUNIT_DIM_ID);

    assertEquals(1, constrainedOus.size());
    assertEquals(ouA.getUid(), constrainedOus.get(0).getUid());
  }

  @Test
  void shouldAssignDefaultOuConstraintWhenNoOuAndNoEnrollmentOuAreProvided() {
    EventQueryParams params = new EventQueryParams.Builder().build();

    EventQueryParams constrained = securityManager.withUserConstraints(params);
    List<DimensionalItemObject> constrainedOus =
        constrained.getDimensionOrFilterItems(ORGUNIT_DIM_ID);

    assertEquals(1, constrainedOus.size());
    assertEquals(ouB.getUid(), constrainedOus.get(0).getUid());
  }
}
