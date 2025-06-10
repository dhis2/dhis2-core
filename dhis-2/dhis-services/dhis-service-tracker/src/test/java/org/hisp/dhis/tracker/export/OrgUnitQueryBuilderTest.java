/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOrgUnitModeClause;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOwnershipClause;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.utils.Assertions;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

@ExtendWith(MockitoExtension.class)
class OrgUnitQueryBuilderTest {

  private final Set<OrganisationUnit> orgUnits = new HashSet<>();
  private final Map<String, Object> expectedParams = new HashMap<>();
  private OrganisationUnit orgUnitA;
  private OrganisationUnit orgUnitB;
  private MockedStatic<CurrentUserUtil> mockedStatic;

  @BeforeEach
  void setUp() {
    orgUnitA = createOrgUnit(1, "orgUnitA", "/orgUnitA");
    orgUnitB = createOrgUnit(2, "orgUnitB", "/orgUnitB");
    orgUnits.add(orgUnitA);
    orgUnits.add(orgUnitB);

    User user = new User();
    UserDetails userDetails = UserDetails.fromUser(user);
    userDetails.getUserOrgUnitIds().add(orgUnitB.getUid());
    userDetails.getUserEffectiveSearchOrgUnitIds().add(orgUnitA.getUid());

    mockedStatic = mockStatic(CurrentUserUtil.class);
    mockedStatic.when(CurrentUserUtil::getCurrentUserDetails).thenReturn(userDetails);
  }

  @AfterEach
  void tearDown() {
    mockedStatic.close();
  }

  @Test
  void shouldBuildOrgUnitModeQueryWhenModeDescendants() {
    StringBuilder sql = new StringBuilder();
    MapSqlParameterSource params = new MapSqlParameterSource();

    buildOrgUnitModeClause(sql, params, orgUnits, DESCENDANTS, "ou", "and ");

    assertEquals(
        "and (  ou.path like :orgUnitPath0 or ou.path like :orgUnitPath1)", sql.toString());

    expectedParams.put("orgUnitPath0", orgUnitA.getPath() + "%");
    expectedParams.put("orgUnitPath1", orgUnitB.getPath() + "%");
    assertParameters(params);
  }

  @Test
  void shouldBuildOrgUnitModeQueryWhenModeChildren() {
    StringBuilder sql = new StringBuilder();
    MapSqlParameterSource params = new MapSqlParameterSource();

    buildOrgUnitModeClause(sql, params, orgUnits, CHILDREN, "ou", "and ");

    assertEquals(
        "and (   ou.path like :orgUnitPath0 and (ou.hierarchylevel = :parentHierarchyLevel0 or ou.hierarchylevel = :childHierarchyLevel0) or  ou.path like :orgUnitPath1 and (ou.hierarchylevel = :parentHierarchyLevel1 or ou.hierarchylevel = :childHierarchyLevel1))",
        sql.toString());

    expectedParams.put("orgUnitPath0", orgUnitA.getPath() + "%");
    expectedParams.put("parentHierarchyLevel0", 1);
    expectedParams.put("childHierarchyLevel0", 2);
    expectedParams.put("orgUnitPath1", orgUnitB.getPath() + "%");
    expectedParams.put("parentHierarchyLevel1", 1);
    expectedParams.put("childHierarchyLevel1", 2);
    assertParameters(params);
  }

  @Test
  void shouldBuildOrgUnitModeQueryWhenModeSelected() {
    StringBuilder sql = new StringBuilder();
    MapSqlParameterSource params = new MapSqlParameterSource();

    buildOrgUnitModeClause(sql, params, Set.of(orgUnitA), SELECTED, "ou", "where ");

    assertEquals("where ou.organisationunitid in (:orgUnits) ", sql.toString());

    expectedParams.put("orgUnits", List.of(orgUnitA.getId()));
    assertParameters(params);
  }

  private static Stream<Arguments> orgUnitModesWithoutOrgUnits() {
    return Stream.of(Arguments.of(ALL), Arguments.of(ACCESSIBLE), Arguments.of(CAPTURE));
  }

  @ParameterizedTest
  @MethodSource("orgUnitModesWithoutOrgUnits")
  void shouldNotBuildOrgUnitModeQueryWhenModeRequiresNoOrgUnits(
      OrganisationUnitSelectionMode orgUnitMode) {
    StringBuilder sql = new StringBuilder();
    MapSqlParameterSource params = new MapSqlParameterSource();

    buildOrgUnitModeClause(sql, params, orgUnits, orgUnitMode, "ou", "and ");

    assertTrue(
        sql.toString().isEmpty(),
        String.format("Expected sql query predicate to be empty, but was %s", sql));
    Assertions.assertIsEmpty(params.getValues().values());
  }

  @Test
  void shouldNotBuildOwnershipQueryWhenModeAll() {
    StringBuilder sql = new StringBuilder();
    MapSqlParameterSource params = new MapSqlParameterSource();

    buildOwnershipClause(sql, params, ALL, "p", "ou", "t");

    assertTrue(
        sql.toString().isEmpty(),
        String.format("Expected sql query predicate to be empty, but was %s", sql));
    Assertions.assertIsEmpty(params.getValues().values());
  }

  @Test
  void shouldBuildOwnershipQueryWhenModeCapture() {
    StringBuilder sql = new StringBuilder();
    MapSqlParameterSource params = new MapSqlParameterSource();

    buildOwnershipClause(sql, params, CAPTURE, "p", "ou", "t");

    assertEquals(
        " and ((p.accesslevel in ('OPEN', 'AUDITED') and ou.path like any (select concat(o.path, '%') from organisationunit o where o.uid in (:captureScopeOrgUnits))) or (p.accesslevel in ('PROTECTED', 'CLOSED') and ou.path like any (select concat(o.path, '%') from organisationunit o where o.uid in (:captureScopeOrgUnits))) or (p.accesslevel = 'PROTECTED' and exists (select 1 from programtempowner where programid = p.programid and trackedentityid = t.trackedentityid and userid = 0 and extract(epoch from validtill)-extract (epoch from now()::timestamp) > 0)))",
        sql.toString());

    expectedParams.put("captureScopeOrgUnits", Set.of(orgUnitB.getUid()));
    assertParameters(params);
  }

  private static Stream<Arguments> orgUnitModesWithEffectiveSearchScope() {
    return Stream.of(
        Arguments.of(SELECTED),
        Arguments.of(CHILDREN),
        Arguments.of(ACCESSIBLE),
        Arguments.of(DESCENDANTS));
  }

  @ParameterizedTest
  @MethodSource("orgUnitModesWithEffectiveSearchScope")
  void shouldBuildOwnershipQueryWhenModeChecksEffectiveSearchScope(
      OrganisationUnitSelectionMode orgUnitMode) {
    StringBuilder sql = new StringBuilder();
    MapSqlParameterSource params = new MapSqlParameterSource();

    buildOwnershipClause(sql, params, orgUnitMode, "p", "ou", "t");

    assertEquals(
        " and ((p.accesslevel in ('OPEN', 'AUDITED') and ou.path like any (select concat(o.path, '%') from organisationunit o where o.uid in (:effectiveSearchScopeOrgUnits))) or (p.accesslevel in ('PROTECTED', 'CLOSED') and ou.path like any (select concat(o.path, '%') from organisationunit o where o.uid in (:captureScopeOrgUnits))) or (p.accesslevel = 'PROTECTED' and exists (select 1 from programtempowner where programid = p.programid and trackedentityid = t.trackedentityid and userid = 0 and extract(epoch from validtill)-extract (epoch from now()::timestamp) > 0)))",
        sql.toString());

    expectedParams.put("effectiveSearchScopeOrgUnits", Set.of(orgUnitA.getUid()));
    expectedParams.put("captureScopeOrgUnits", Set.of(orgUnitB.getUid()));
    assertParameters(params);
  }

  private void assertParameters(MapSqlParameterSource params) {
    assertContainsOnly(expectedParams.keySet(), Arrays.asList(params.getParameterNames()));
    assertContainsOnly(expectedParams.values(), params.getValues().values());
  }

  private OrganisationUnit createOrgUnit(int id, String uid, String path) {
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setId(id);
    orgUnit.setUid(uid);
    orgUnit.setPath(path);

    return orgUnit;
  }
}
