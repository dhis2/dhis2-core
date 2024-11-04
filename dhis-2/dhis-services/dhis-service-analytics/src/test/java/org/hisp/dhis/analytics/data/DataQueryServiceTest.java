/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataQueryServiceTest {
  private static OrganisationUnit ouA;

  private static OrganisationUnit ouB;

  private static OrganisationUnit ouC;

  private static OrganisationUnit ouD;

  private static DataQueryParams dataQueryParams;

  @BeforeAll
  static void setUp() {
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    ouD = createOrganisationUnit('D');
    dataQueryParams =
        DataQueryParams.newBuilder().withUserOrgUnitType(UserOrgUnitType.DATA_OUTPUT).build();
  }

  @Test
  void testGetUserOrgUnitsWithExplicitGrantedForAnalyticsOrganisationUnits() {
    // given
    User currentUser = mock(User.class);
    AnalyticsSecurityManager analyticsSecurityManager = mock(AnalyticsSecurityManager.class);
    when(currentUser.getDataViewOrganisationUnits()).thenReturn(Set.of(ouB, ouC, ouD));
    when(analyticsSecurityManager.getCurrentUser(dataQueryParams)).thenReturn(currentUser);
    DataQueryService dataQueryService =
        new DefaultDataQueryService(
            mock(DimensionalObjectProducer.class),
            mock(IdentifiableObjectManager.class),
            analyticsSecurityManager);

    // when
    List<OrganisationUnit> userOrgUnits = dataQueryService.getUserOrgUnits(dataQueryParams, null);

    // then
    assertEquals(3, userOrgUnits.size());
    assertThat(
        userOrgUnits.stream().map(BaseIdentifiableObject::getName).toList(),
        containsInAnyOrder("OrganisationUnitB", "OrganisationUnitC", "OrganisationUnitD"));
  }

  @Test
  void testGetUserOrgUnitsWithDeniedForAnalyticsOrganisationUnits() {
    // given
    User currentUser = mock(User.class);
    AnalyticsSecurityManager analyticsSecurityManager = mock(AnalyticsSecurityManager.class);
    when(currentUser.getDataViewOrganisationUnits()).thenReturn(Set.of());
    when(currentUser.getDataViewOrganisationUnits()).thenReturn(Set.of(ouA));
    when(analyticsSecurityManager.getCurrentUser(dataQueryParams)).thenReturn(currentUser);
    DataQueryService dataQueryService =
        new DefaultDataQueryService(
            mock(DimensionalObjectProducer.class),
            mock(IdentifiableObjectManager.class),
            analyticsSecurityManager);

    // when
    List<OrganisationUnit> userOrgUnits = dataQueryService.getUserOrgUnits(dataQueryParams, null);

    // then
    assertEquals(1, userOrgUnits.size());
    assertThat(
        userOrgUnits.stream().map(BaseIdentifiableObject::getName).toList(),
        containsInAnyOrder("OrganisationUnitA"));
  }

  @Test
  void testGetUserOrgUnitsWithDeniedForAllOrganisationUnits() {
    // given
    User currentUser = mock(User.class);
    AnalyticsSecurityManager analyticsSecurityManager = mock(AnalyticsSecurityManager.class);
    when(currentUser.getDataViewOrganisationUnits()).thenReturn(Set.of());
    when(currentUser.getDataViewOrganisationUnits()).thenReturn(Set.of());
    when(analyticsSecurityManager.getCurrentUser(dataQueryParams)).thenReturn(currentUser);
    DataQueryService dataQueryService =
        new DefaultDataQueryService(
            mock(DimensionalObjectProducer.class),
            mock(IdentifiableObjectManager.class),
            analyticsSecurityManager);

    // when
    List<OrganisationUnit> userOrgUnits = dataQueryService.getUserOrgUnits(dataQueryParams, null);

    // then
    assertEquals(0, userOrgUnits.size());
  }
}
