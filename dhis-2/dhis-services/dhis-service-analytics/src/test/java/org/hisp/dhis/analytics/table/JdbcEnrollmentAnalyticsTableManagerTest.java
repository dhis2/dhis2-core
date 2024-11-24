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
package org.hisp.dhis.analytics.table;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramTrackedEntityAttribute;
import static org.hisp.dhis.test.TestBase.createTrackedEntityAttribute;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.Distribution;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class JdbcEnrollmentAnalyticsTableManagerTest {
  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private DatabaseInfoProvider databaseInfoProvider;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private AnalyticsTableSettings analyticsTableSettings;

  @Mock private PeriodDataProvider periodDataProvider;

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private JdbcEnrollmentAnalyticsTableManager subject;

  private static final Date START_TIME = new DateTime(2019, 8, 1, 0, 0).toDate();

  @BeforeEach
  public void setUp() {
    lenient()
        .when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().build());
    SystemSettingsProvider settingsProvider = mock(SystemSettingsProvider.class);
    lenient().when(settingsProvider.getCurrentSettings()).thenReturn(SystemSettings.of(Map.of()));
    subject =
        new JdbcEnrollmentAnalyticsTableManager(
            idObjectManager,
            mock(OrganisationUnitService.class),
            mock(CategoryService.class),
            settingsProvider,
            mock(DataApprovalLevelService.class),
            mock(ResourceTableService.class),
            mock(AnalyticsTableHookService.class),
            mock(PartitionManager.class),
            databaseInfoProvider,
            jdbcTemplate,
            analyticsTableSettings,
            periodDataProvider,
            sqlBuilder);
  }

  @Test
  void verifyTableNotDistributedWhenCitusEnabledAndTableTypeIsSkipped() {
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .skipCitusTypes(Set.of(AnalyticsTableType.ENROLLMENT))
            .lastYears(2)
            .startTime(START_TIME)
            .build();

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(createProgram('A')));

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);

    assertFalse(analyticsTables.get(0).isDistributed());
  }

  @Test
  void verifyTableIsDistributedWhenCitusEnabledAndTableTypeIsNotSkipped() {
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().lastYears(2).startTime(START_TIME).build();

    when(analyticsTableSettings.getDistribution()).thenReturn(Distribution.DISTRIBUTED);
    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(createProgram('A')));

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);

    assertTrue(analyticsTables.get(0).isDistributed());
  }

  @Test
  void verifyTeiTypeOrgUnitFetchesOuUidWhenPopulatingEventAnalyticsTable() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().spatialSupport(true).build());
    Program p1 = createProgram('A');

    TrackedEntityAttribute tea = createTrackedEntityAttribute('a', ValueType.ORGANISATION_UNIT);
    tea.setId(9999);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(p1, tea);

    p1.setProgramAttributes(List.of(programTrackedEntityAttribute));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(p1));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().lastYears(2).startTime(START_TIME).build();

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);
    assertFalse(analyticsTables.isEmpty());
    AnalyticsTablePartition partition = new AnalyticsTablePartition(analyticsTables.get(0));

    subject.populateTable(params, partition);
    verify(jdbcTemplate).execute(sql.capture());

    String ouQuery =
        format(
            """
        (select ou.%s from \"organisationunit\" ou where ou.uid = \
        (select value from \"trackedentityattributevalue\" where trackedentityid=en.trackedentityid and \
        trackedentityattributeid=9999)) as %s""",
            "uid", quote(tea.getUid()));

    assertThat(sql.getValue(), containsString(ouQuery));
  }
}
