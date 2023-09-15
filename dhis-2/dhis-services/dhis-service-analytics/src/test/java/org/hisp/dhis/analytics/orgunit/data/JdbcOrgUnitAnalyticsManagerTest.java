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
package org.hisp.dhis.analytics.orgunit.data;

import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnitGroupSet;
import static org.hisp.dhis.feedback.ErrorCode.E7302;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.common.TableInfoReader;
import org.hisp.dhis.analytics.orgunit.OrgUnitQueryParams;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * Unit tests for {@link JdbcOrgUnitAnalyticsManager}.
 *
 * @author maikel arabori
 */
@ExtendWith(MockitoExtension.class)
class JdbcOrgUnitAnalyticsManagerTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private SqlRowSet sqlRowSet;

  @Mock private TableInfoReader tableInfoReader;

  private JdbcOrgUnitAnalyticsManager jdbcOrgUnitAnalyticsManager;

  @BeforeEach
  public void beforeAll() {
    jdbcOrgUnitAnalyticsManager = new JdbcOrgUnitAnalyticsManager(tableInfoReader, jdbcTemplate);
  }

  @Test
  void testGetOrgUnitDataWithSuccess() {
    // Given
    OrganisationUnitGroupSet organisationUnitGroupSet1 = createOrganisationUnitGroupSet('A');
    organisationUnitGroupSet1.setUid("abc123");

    OrganisationUnitGroupSet organisationUnitGroupSet2 = createOrganisationUnitGroupSet('B');
    organisationUnitGroupSet2.setUid("abc456");

    List<OrganisationUnitGroupSet> organisationUnitGroupSets =
        List.of(organisationUnitGroupSet1, organisationUnitGroupSet2);

    OrgUnitQueryParams params =
        new OrgUnitQueryParams.Builder().withOrgUnitGroupSets(organisationUnitGroupSets).build();

    mockSqlRowSet();

    // When
    when(tableInfoReader.checkColumnsPresence(
            "_organisationunitgroupsetstructure", Set.of("abc123", "abc456")))
        .thenReturn(Set.of());
    when(jdbcTemplate.queryForRowSet(anyString())).thenReturn(sqlRowSet);
    Map<String, Integer> data = jdbcOrgUnitAnalyticsManager.getOrgUnitData(params);

    // Then
    // Based on the mocked sqlRowSet.
    assertEquals(1, data.get("OrgUnit-Abc123-Abc456"));
  }

  @Test
  void testGetOrgUnitDataWithInvalidOrgUnitSetDimension() {
    // Given
    String invalidOrgUnitSetDim = "xyz123";

    OrganisationUnitGroupSet organisationUnitGroupSet1 = createOrganisationUnitGroupSet('A');
    organisationUnitGroupSet1.setUid("abc123");

    OrganisationUnitGroupSet organisationUnitGroupSet2 = createOrganisationUnitGroupSet('B');
    organisationUnitGroupSet2.setUid("abc456");

    List<OrganisationUnitGroupSet> organisationUnitGroupSets =
        List.of(organisationUnitGroupSet1, organisationUnitGroupSet2);

    OrgUnitQueryParams params =
        new OrgUnitQueryParams.Builder().withOrgUnitGroupSets(organisationUnitGroupSets).build();

    // When
    when(tableInfoReader.checkColumnsPresence(
            "_organisationunitgroupsetstructure", Set.of("abc123", "abc456")))
        .thenReturn(Set.of(invalidOrgUnitSetDim));

    // Then
    assertThrows(
        QueryRuntimeException.class,
        () -> jdbcOrgUnitAnalyticsManager.getOrgUnitData(params),
        E7302.getMessage());
  }

  private void mockSqlRowSet() {
    // Simulate 2 results.
    when(sqlRowSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);

    when(sqlRowSet.getString("orgunit")).thenReturn("OrgUnit");
    when(sqlRowSet.getString("abc123")).thenReturn("Abc123");
    when(sqlRowSet.getString("abc456")).thenReturn("Abc456");
    when(sqlRowSet.getInt("count")).thenReturn(1);
  }
}
