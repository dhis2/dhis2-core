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
package org.hisp.dhis.analytics.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.table.util.ColumnUtils;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcTrackedEntityAnalyticsTableManagerTest {
  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private AnalyticsTableSettings analyticsTableSettings;

  @Mock private PeriodDataProvider periodDataProvider;

  @Spy private SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  @Mock private PartitionManager partitionManager;

  @Mock private SystemSettingsProvider systemSettingsProvider;

  @Mock private IdentifiableObjectManager identifiableObjectManager;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private CategoryService categoryService;

  @Mock private AnalyticsTableSettings settings;

  @Mock private DataApprovalLevelService dataApprovalLevelService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ResourceTableService resourceTableService;

  @Mock private AnalyticsTableHookService analyticsTableHookService;

  private JdbcTrackedEntityAnalyticsTableManager tableManager;

  @BeforeEach
  void setUp() {
    tableManager =
        new JdbcTrackedEntityAnalyticsTableManager(
            identifiableObjectManager,
            organisationUnitService,
            categoryService,
            systemSettingsProvider,
            dataApprovalLevelService,
            resourceTableService,
            analyticsTableHookService,
            partitionManager,
            jdbcTemplate,
            trackedEntityTypeService,
            trackedEntityAttributeService,
            analyticsTableSettings,
            periodDataProvider,
            new ColumnUtils(sqlBuilder),
            sqlBuilder);
  }

  @Test
  void verifyNonConfidentialTeasAreSkipped() {
    AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().build();

    TrackedEntityType tet = mock(TrackedEntityType.class);
    when(tet.getUid()).thenReturn("tetUid");

    TrackedEntityAttribute nonConfidentialTea = new TrackedEntityAttribute();
    nonConfidentialTea.setUid("nonConfidentialTeaUid");
    nonConfidentialTea.setConfidential(false);
    nonConfidentialTea.setValueType(ValueType.TEXT);

    TrackedEntityAttribute confidentialTea = new TrackedEntityAttribute();
    confidentialTea.setUid("confidentialTeaUid");
    confidentialTea.setConfidential(true);
    confidentialTea.setValueType(ValueType.TEXT);

    Program program = mock(Program.class);

    when(tet.getTrackedEntityAttributes()).thenReturn(List.of(nonConfidentialTea, confidentialTea));

    when(program.getTrackedEntityType()).thenReturn(tet);

    when(trackedEntityTypeService.getAllTrackedEntityType()).thenReturn(List.of(tet));

    when(trackedEntityAttributeService.getProgramTrackedEntityAttributes(List.of(program)))
        .thenReturn(List.of());

    when(identifiableObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));

    List<AnalyticsTable> analyticsTables = tableManager.getAnalyticsTables(params);

    assertEquals(1, analyticsTables.size());

    AnalyticsTable analyticsTable = analyticsTables.get(0);

    assertContainsNonConfidentialTeaColumns(analyticsTable);
    assertDoesntContainConfidentialTeaColumns(analyticsTable);
  }

  private void assertDoesntContainConfidentialTeaColumns(AnalyticsTable analyticsTable) {
    List<Column> columns = analyticsTable.getColumns();

    assertFalse(columns.stream().map(Column::getName).anyMatch("confidentialTeaUid"::equals));
  }

  private void assertContainsNonConfidentialTeaColumns(AnalyticsTable analyticsTable) {
    List<Column> columns = analyticsTable.getColumns();

    assertTrue(columns.stream().map(Column::getName).anyMatch("nonConfidentialTeaUid"::equals));
  }
}
