/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.util;

import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNITS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT_CHILDREN;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.USER_ORGUNIT_GRANDCHILDREN;
import static org.hisp.dhis.analytics.util.AnalyticsOrganisationUnitUtils.getUserOrganisationUnitsItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsOrganisationUnitUtilsTest {

  @Mock private User user;
  private final OrganisationUnit orgA = new OrganisationUnit("orgA");
  private final OrganisationUnit orgB = new OrganisationUnit("orgB");
  private final OrganisationUnit orgC = new OrganisationUnit("orgC");

  private final String uidA = "abcdefA";
  private final String uidB = "abcdefB";
  private final String uidC = "abcdefC";

  @BeforeEach
  public void setUp() {
    orgA.setUid(uidA);
    orgB.setUid(uidB);
    orgC.setUid(uidC);
    orgA.setChildren(Set.of(orgB));
    orgB.setChildren(Set.of(orgC));
  }

  @Test
  void testAnalyticsOrganisationUnitUtils_All() {
    // given
    List<AnalyticsMetaDataKey> userOrganisationUnitsCriteria =
        List.of(USER_ORGUNIT, USER_ORGUNIT_CHILDREN, USER_ORGUNIT_GRANDCHILDREN);

    // when
    when(user.getOrganisationUnits()).thenReturn(Set.of(orgA));
    List<Map<String, Object>> uidMapList =
        getUserOrganisationUnitsItems(user, userOrganisationUnitsCriteria).stream().toList();

    // then
    assertEquals(3, uidMapList.size());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[" + uidA + "]}",
        uidMapList.get(0).get(USER_ORGUNIT.getKey()).toString());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[" + uidB + "]}",
        uidMapList.get(1).get(USER_ORGUNIT_CHILDREN.getKey()).toString());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[" + uidC + "]}",
        uidMapList.get(2).get(USER_ORGUNIT_GRANDCHILDREN.getKey()).toString());
  }

  @Test
  void testAnalyticsOrganisationUnitUtils_Parent() {
    // given
    List<AnalyticsMetaDataKey> userOrganisationUnitsCriteria = List.of(USER_ORGUNIT);

    // when
    when(user.getOrganisationUnits()).thenReturn(Set.of(orgA));
    List<Map<String, Object>> uidMapList =
        getUserOrganisationUnitsItems(user, userOrganisationUnitsCriteria).stream().toList();

    // then
    assertEquals(1, uidMapList.size());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[" + uidA + "]}",
        uidMapList.get(0).get(USER_ORGUNIT.getKey()).toString());
  }

  @Test
  void testAnalyticsOrganisationUnitUtils_Children() {
    // given
    List<AnalyticsMetaDataKey> userOrganisationUnitsCriteria = List.of(USER_ORGUNIT_CHILDREN);

    // when
    when(user.getOrganisationUnits()).thenReturn(Set.of(orgA));
    List<Map<String, Object>> uidMapList =
        getUserOrganisationUnitsItems(user, userOrganisationUnitsCriteria).stream().toList();

    // then
    assertEquals(1, uidMapList.size());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[" + uidB + "]}",
        uidMapList.get(0).get(USER_ORGUNIT_CHILDREN.getKey()).toString());
  }

  @Test
  void testAnalyticsOrganisationUnitUtils_Grandchildren() {
    // given
    List<AnalyticsMetaDataKey> userOrganisationUnitsCriteria = List.of(USER_ORGUNIT_GRANDCHILDREN);

    // when
    when(user.getOrganisationUnits()).thenReturn(Set.of(orgA));
    List<Map<String, Object>> uidMapList =
        getUserOrganisationUnitsItems(user, userOrganisationUnitsCriteria).stream().toList();

    // then
    assertEquals(1, uidMapList.size());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[" + uidC + "]}",
        uidMapList.get(0).get(USER_ORGUNIT_GRANDCHILDREN.getKey()).toString());
  }

  @Test
  void testAnalyticsOrganisationUnitUtils_All_Empty() {
    // given
    List<AnalyticsMetaDataKey> userOrganisationUnitsCriteria =
        List.of(USER_ORGUNIT, USER_ORGUNIT_CHILDREN, USER_ORGUNIT_GRANDCHILDREN);

    // when
    when(user.getOrganisationUnits()).thenReturn(Set.of());
    List<Map<String, Object>> uidMapList =
        getUserOrganisationUnitsItems(user, userOrganisationUnitsCriteria).stream().toList();

    // then
    assertEquals(3, uidMapList.size());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[]}", uidMapList.get(0).get(USER_ORGUNIT.getKey()).toString());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[]}",
        uidMapList.get(1).get(USER_ORGUNIT_CHILDREN.getKey()).toString());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[]}",
        uidMapList.get(2).get(USER_ORGUNIT_GRANDCHILDREN.getKey()).toString());
  }

  @Test
  void testAnalyticsOrganisationUnitUtils_Parent_Empty() {
    // given
    List<AnalyticsMetaDataKey> userOrganisationUnitsCriteria = List.of(USER_ORGUNIT);

    // when
    when(user.getOrganisationUnits()).thenReturn(Set.of());
    List<Map<String, Object>> uidMapList =
        getUserOrganisationUnitsItems(user, userOrganisationUnitsCriteria).stream().toList();

    // then
    assertEquals(1, uidMapList.size());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[]}", uidMapList.get(0).get(USER_ORGUNIT.getKey()).toString());
  }

  @Test
  void testAnalyticsOrganisationUnitUtils_Children_Empty() {
    // given
    List<AnalyticsMetaDataKey> userOrganisationUnitsCriteria = List.of(USER_ORGUNIT_CHILDREN);

    // when
    when(user.getOrganisationUnits()).thenReturn(Set.of());
    List<Map<String, Object>> uidMapList =
        getUserOrganisationUnitsItems(user, userOrganisationUnitsCriteria).stream().toList();

    // then
    assertEquals(1, uidMapList.size());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[]}",
        uidMapList.get(0).get(USER_ORGUNIT_CHILDREN.getKey()).toString());
  }

  @Test
  void testAnalyticsOrganisationUnitUtils_Grandchildren_Empty() {
    // given
    List<AnalyticsMetaDataKey> userOrganisationUnitsCriteria = List.of(USER_ORGUNIT_GRANDCHILDREN);

    // when
    when(user.getOrganisationUnits()).thenReturn(Set.of());
    List<Map<String, Object>> uidMapList =
        getUserOrganisationUnitsItems(user, userOrganisationUnitsCriteria).stream().toList();

    // then
    assertEquals(1, uidMapList.size());
    assertEquals(
        "{" + ORG_UNITS.getKey() + "=[]}",
        uidMapList.get(0).get(USER_ORGUNIT_GRANDCHILDREN.getKey()).toString());
  }
}
