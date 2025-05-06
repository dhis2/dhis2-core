/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hisp.dhis.common.UserOrgUnitType.DATA_OUTPUT;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataQueryServiceTest {

  @Mock private DimensionalObjectProvider dimensionalObjectProducer;

  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private AnalyticsSecurityManager analyticsSecurityManager;

  @InjectMocks private DefaultDataQueryService dataQueryService;

  @Test
  void testGetUserOrgUnitsWithExplicitlyDefinedAnalyticsOrganisationUnits() {
    OrganisationUnit ouB = createOrganisationUnit('B');
    OrganisationUnit ouC = createOrganisationUnit('C');
    OrganisationUnit ouD = createOrganisationUnit('D');
    DataQueryParams dataQueryParams =
        DataQueryParams.newBuilder().withUserOrgUnitType(DATA_OUTPUT).build();
    User currentUser = mock(User.class);
    when(currentUser.getDataViewOrganisationUnits()).thenReturn(Set.of(ouB, ouC, ouD));
    when(analyticsSecurityManager.getCurrentUser(dataQueryParams)).thenReturn(currentUser);

    List<OrganisationUnit> userOrgUnits = dataQueryService.getUserOrgUnits(dataQueryParams, null);

    assertEquals(3, userOrgUnits.size());
    assertThat(
        userOrgUnits.stream().map(IdentifiableObject::getName).toList(),
        containsInAnyOrder("OrganisationUnitB", "OrganisationUnitC", "OrganisationUnitD"));
  }

  @Test
  void testGetUserOrgUnitsWithNoAnalyticsOrganisationUnitsDefined() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    DataQueryParams dataQueryParams =
        DataQueryParams.newBuilder().withUserOrgUnitType(DATA_OUTPUT).build();
    User currentUser = mock(User.class);
    when(currentUser.getOrganisationUnits()).thenReturn(Set.of(ouA));
    when(currentUser.getDataViewOrganisationUnits()).thenReturn(Set.of());
    when(analyticsSecurityManager.getCurrentUser(dataQueryParams)).thenReturn(currentUser);

    List<OrganisationUnit> userOrgUnits = dataQueryService.getUserOrgUnits(dataQueryParams, null);

    assertEquals(1, userOrgUnits.size());
    assertThat(
        userOrgUnits.stream().map(IdentifiableObject::getName).toList(),
        containsInAnyOrder("OrganisationUnitA"));
  }

  @Test
  void testGetUserOrgUnitsWithNoneOrganisationUnitDefined() {
    DataQueryParams dataQueryParams =
        DataQueryParams.newBuilder().withUserOrgUnitType(DATA_OUTPUT).build();

    List<OrganisationUnit> userOrgUnits = dataQueryService.getUserOrgUnits(dataQueryParams, null);

    assertEquals(0, userOrgUnits.size());
  }
}
