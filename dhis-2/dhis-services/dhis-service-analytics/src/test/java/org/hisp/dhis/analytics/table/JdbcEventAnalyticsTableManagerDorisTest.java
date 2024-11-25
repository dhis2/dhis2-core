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

import static java.time.LocalDate.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.db.model.DataType.BIGINT;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;
import static org.hisp.dhis.db.model.Table.STAGING_TABLE_SUFFIX;
import static org.hisp.dhis.period.PeriodDataProvider.PeriodSource.DATABASE;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.util.AnalyticsTableAsserter;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.sql.DorisSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class JdbcEventAnalyticsTableManagerDorisTest {
  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CategoryService categoryService;

  @Mock private SystemSettingsProvider settingsProvider;
  
  @Mock private SystemSettings settings;

  @Mock private DatabaseInfoProvider databaseInfoProvider;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private ResourceTableService resourceTableService;

  @Mock private PeriodDataProvider periodDataProvider;

  @Mock private AnalyticsTableSettings analyticsTableSettings;

  private final SqlBuilder sqlBuilder = new DorisSqlBuilder("dhis2", "driver");

  private JdbcEventAnalyticsTableManager subject;

  private Date today;

  private static final Date START_TIME = new DateTime(2019, 8, 1, 0, 0).toDate();

  private static final String TABLE_PREFIX = "analytics_event_";

  private static final String FROM_CLAUSE = "from dhis2.public.`event` where eventid=ev.eventid";

  private static final int OU_NAME_HIERARCHY_COUNT = 1;

  private List<AnalyticsTableColumn> periodColumns =
      PeriodType.getAvailablePeriodTypes().stream()
          .map(
              pt -> {
                String column = pt.getName().toLowerCase();
                return AnalyticsTableColumn.builder()
                    .name(column)
                    .dataType(TEXT)
                    .selectExpression("dps" + "." + quote(column))
                    .build();
              })
          .toList();

  private String quote(String relation) {
    return sqlBuilder.quote(relation);
  }

  @BeforeEach
  void setUp() {
    today = Date.from(LocalDate.of(2019, 7, 6).atStartOfDay(ZoneId.systemDefault()).toInstant());

    when(databaseInfoProvider.getDatabaseInfo()).thenReturn(DatabaseInfo.builder().build());
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(settings.getLastSuccessfulResourceTablesUpdate()).thenReturn(new Date(0L));

    subject =
        new JdbcEventAnalyticsTableManager(
            idObjectManager,
            organisationUnitService,
            categoryService,
            settingsProvider,
            mock(DataApprovalLevelService.class),
            resourceTableService,
            mock(AnalyticsTableHookService.class),
            mock(PartitionManager.class),
            databaseInfoProvider,
            jdbcTemplate,
            analyticsTableSettings,
            periodDataProvider,
            sqlBuilder);
    assertThat(subject.getAnalyticsTableType(), is(AnalyticsTableType.EVENT));
  }

  @Test
  void verifyGetTableWithDataElements() {

    subject =
        new JdbcEventAnalyticsTableManager(
            idObjectManager,
            organisationUnitService,
            categoryService,
            settingsProvider,
            mock(DataApprovalLevelService.class),
            resourceTableService,
            mock(AnalyticsTableHookService.class),
            mock(PartitionManager.class),
            databaseInfoProvider,
            jdbcTemplate,
            analyticsTableSettings,
            periodDataProvider,
            new DorisSqlBuilder("dhis2", "driver"));

    when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().spatialSupport(true).build());
    Program program = createProgram('A');

    DataElement d1 = createDataElement('Z', ValueType.TEXT, AggregationType.SUM);
    DataElement d2 = createDataElement('P', ValueType.PERCENTAGE, AggregationType.SUM);
    DataElement d3 = createDataElement('Y', ValueType.BOOLEAN, AggregationType.NONE);
    DataElement d4 = createDataElement('W', ValueType.DATE, AggregationType.LAST);
    DataElement d5 = createDataElement('G', ValueType.ORGANISATION_UNIT, AggregationType.NONE);
    DataElement d6 = createDataElement('H', ValueType.INTEGER, AggregationType.SUM);
    DataElement d7 = createDataElement('U', ValueType.COORDINATE, AggregationType.NONE);

    ProgramStage ps1 = createProgramStage('A', Set.of(d1, d2, d3, d4, d5, d6, d7));

    program.setProgramStages(Set.of(ps1));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));

    String aliasD1 =
        "(select json_unquote(json_extract(eventdatavalues, '$.%s.value')) "
            + FROM_CLAUSE
            + " ) as `%s`";
    String aliasD2 =
        "(select cast(json_unquote(json_extract(eventdatavalues, '$.%s.value')) as double) "
            + FROM_CLAUSE
            + "  and json_unquote(json_extract(eventdatavalues, '$.%s.value')) regexp '^(-?[0-9]+)(\\.[0-9]+)?$') as `%s`";
    String aliasD3 =
        "(select case when json_unquote(json_extract(eventdatavalues, '$.%s.value')) = 'true' then 1 when json_unquote(json_extract(eventdatavalues, '$.%s.value')) = 'false' then 0 else null end "
            + FROM_CLAUSE
            + " ) as `%s`";
    String aliasD4 =
        "(select cast(json_unquote(json_extract(eventdatavalues, '$.%s.value')) as datetime) "
            + FROM_CLAUSE
            + "  and json_unquote(json_extract(eventdatavalues, '$.%s.value')) regexp '^\\d{4}-\\d{2}-\\d{2}(\\s|T)?((\\d{2}:)(\\d{2}:)?(\\d{2}))?(|.(\\d{3})|.(\\d{3})Z)?$') as `%s`";
    String aliasD5 =
        "(select ou.uid from dhis2.public.`organisationunit` ou where ou.uid = json_unquote(json_extract(eventdatavalues, '$.deabcdefghG.value')) ) as `deabcdefghG`";
    String aliasD5Name =
        "(select ou.name from dhis2.public.`organisationunit` ou where ou.uid = json_unquote(json_extract(eventdatavalues, "
            + "'$.%s.value')) "
            + ") as `%s`";
    String aliasD6 =
        "(select cast(json_unquote(json_extract(eventdatavalues, '$.%s.value')) as bigint) "
            + FROM_CLAUSE
            + "  and json_unquote(json_extract(eventdatavalues, '$.%s.value')) regexp '^(-?[0-9]+)(\\.[0-9]+)?$') as `%s`";

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(2)
            .startTime(START_TIME)
            .today(today)
            .build();

    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + program.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + program.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(58 + OU_NAME_HIERARCHY_COUNT)
        .addColumns(periodColumns)
        .addColumn(
            d1.getUid(),
            TEXT,
            toSelectExpression(aliasD1, d1.getUid()),
            Skip.SKIP) // ValueType.TEXT
        .addColumn(
            d2.getUid(),
            DOUBLE,
            toSelectExpression(aliasD2, d2.getUid()),
            IndexType.BTREE) // ValueType.PERCENTAGE
        .addColumn(
            d3.getUid(),
            INTEGER,
            toSelectExpression(aliasD3, d3.getUid()),
            IndexType.BTREE) // ValueType.BOOLEAN
        .addColumn(
            d4.getUid(),
            TIMESTAMP,
            toSelectExpression(aliasD4, d4.getUid()),
            IndexType.BTREE) // ValueType.DATE
        .addColumn(
            d5.getUid(),
            TEXT,
            toSelectExpression(aliasD5, d5.getUid()),
            IndexType.BTREE) // ValueType.ORGANISATION_UNIT
        .addColumn(
            d6.getUid(),
            BIGINT,
            toSelectExpression(aliasD6, d6.getUid()),
            IndexType.BTREE) // ValueType.INTEGER

        // element d5 also creates a Name column
        .addColumn(
            d5.getUid() + "_name", TEXT, toSelectExpression(aliasD5Name, d5.getUid()), Skip.SKIP)
        .withDefaultColumns(EventAnalyticsColumn.getColumns(sqlBuilder))
        .build()
        .verify();
  }

  private String toSelectExpression(String template, String uid) {
    return String.format(template, uid, uid, uid);
  }
}
