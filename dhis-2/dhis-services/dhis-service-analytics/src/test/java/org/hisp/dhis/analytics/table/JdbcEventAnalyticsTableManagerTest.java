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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.DhisConvenienceTest.createCategory;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryCombo;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.hisp.dhis.DhisConvenienceTest.createProgramTrackedEntityAttribute;
import static org.hisp.dhis.DhisConvenienceTest.createTrackedEntityAttribute;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.hisp.dhis.db.model.DataType.BIGINT;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.GEOMETRY_POINT;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;
import static org.hisp.dhis.db.model.Table.STAGING_TABLE_SUFFIX;
import static org.hisp.dhis.db.model.constraint.Nullable.NULL;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.DATABASE;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.util.AnalyticsTableAsserter;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class JdbcEventAnalyticsTableManagerTest {
  @Mock private IdentifiableObjectManager idObjectManager;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CategoryService categoryService;

  @Mock private SystemSettingManager systemSettingManager;

  @Mock private DatabaseInfoProvider databaseInfoProvider;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private ResourceTableService resourceTableService;

  @Mock private PeriodDataProvider periodDataProvider;

  @Mock private AnalyticsTableSettings analyticsTableSettings;

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private JdbcEventAnalyticsTableManager subject;

  private Date today;

  private static final Date START_TIME = new DateTime(2019, 8, 1, 0, 0).toDate();

  private static final String TABLE_PREFIX = "analytics_event_";

  private static final String FROM_CLAUSE = "from event where eventid=psi.eventid";

  private static final String DATE_CLAUSE =
      "CASE WHEN 'SCHEDULE' = psi.status THEN psi.scheduleddate ELSE psi.occurreddate END";

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

  private final BeanRandomizer rnd = BeanRandomizer.create();

  @BeforeEach
  public void setUp() {
    today = Date.from(LocalDate.of(2019, 7, 6).atStartOfDay(ZoneId.systemDefault()).toInstant());

    when(databaseInfoProvider.getDatabaseInfo()).thenReturn(DatabaseInfo.builder().build());
    subject =
        new JdbcEventAnalyticsTableManager(
            idObjectManager,
            organisationUnitService,
            categoryService,
            systemSettingManager,
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
  void verifyGetLatestAnalyticsTables() {
    Program prA = createProgram('A');
    Program prB = createProgram('B');
    Program prC = createProgram('C');
    Program prD = createProgram('D');
    List<Program> programs = List.of(prA, prB, prC, prD);

    Date lastFullTableUpdate = new DateTime(2019, 3, 1, 2, 0).toDate();
    Date lastLatestPartitionUpdate = new DateTime(2019, 3, 1, 9, 0).toDate();
    Date startTime = new DateTime(2019, 3, 1, 10, 0).toDate();

    Set<String> skipPrograms = new HashSet<>();
    skipPrograms.add(prC.getUid());
    skipPrograms.add(prD.getUid());

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withStartTime(startTime)
            .withLatestPartition()
            .withSkipPrograms(skipPrograms)
            .build();

    List<Map<String, Object>> queryResp = new ArrayList<>();
    queryResp.add(Map.of("dataelementid", 1));

    when(systemSettingManager.getDateSetting(SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE))
        .thenReturn(lastFullTableUpdate);
    when(systemSettingManager.getDateSetting(
            SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE))
        .thenReturn(lastLatestPartitionUpdate);
    when(jdbcTemplate.queryForList(Mockito.anyString())).thenReturn(queryResp);
    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(programs);

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);
    assertThat(tables, hasSize(2));

    AnalyticsTable tableA = tables.get(0);
    AnalyticsTable tableB = tables.get(1);

    assertThat(tableA, notNullValue());
    assertThat(tableB, notNullValue());

    AnalyticsTablePartition partitionA = tableA.getLatestTablePartition();
    AnalyticsTablePartition partitionB = tableA.getLatestTablePartition();

    assertThat(partitionA, notNullValue());
    assertThat(partitionA.isLatestPartition(), equalTo(true));
    assertThat(partitionA.getStartDate(), equalTo(lastFullTableUpdate));
    assertThat(partitionA.getEndDate(), equalTo(startTime));

    assertThat(partitionB, notNullValue());
    assertThat(partitionB.isLatestPartition(), equalTo(true));
    assertThat(partitionB.getStartDate(), equalTo(lastFullTableUpdate));
    assertThat(partitionB.getEndDate(), equalTo(startTime));
  }

  @Test
  void verifyGetTableWithCategoryCombo() {
    Program program = createProgram('A');

    Category categoryA = createCategory('A');
    categoryA.setCreated(getDate(2019, 12, 3));
    Category categoryB = createCategory('B');
    categoryA.setCreated(getDate(2018, 8, 5));
    CategoryCombo categoryCombo = createCategoryCombo('B', categoryA, categoryB);

    addCategoryCombo(program, categoryCombo);

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));
    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(program, true, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withTableType(AnalyticsTableType.EVENT)
        .withName(TABLE_PREFIX + program.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + program.getUid().toLowerCase())
        .withColumnSize(56 + OU_NAME_HIERARCHY_COUNT)
        .withDefaultColumns(subject.getFixedColumns())
        .addColumns(periodColumns)
        .addColumn(
            categoryA.getUid(),
            CHARACTER_11,
            ("acs." + quote(categoryA.getUid())),
            categoryA.getCreated())
        .addColumn(
            categoryB.getUid(),
            CHARACTER_11,
            ("acs." + quote(categoryB.getUid())),
            categoryB.getCreated())
        .build()
        .verify();
  }

  @Test
  void verifyClientSideTimestampsColumns() {
    Program program = createProgram('A');
    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));
    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(program, true, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    AnalyticsTableColumn lastUpdated = getColumn("lastupdated", tables.get(0));
    AnalyticsTableColumn created = getColumn("created", tables.get(0));

    assertThat(
        lastUpdated.getSelectExpression(),
        is(
            "case when psi.lastupdatedatclient is not null then psi.lastupdatedatclient else psi.lastupdated end"));
    assertThat(
        created.getSelectExpression(),
        is(
            "case when psi.createdatclient is not null then psi.createdatclient else psi.created end"));
  }

  @Test
  void verifyAnalyticsEventTableHasDefaultPartition() {
    Program program = createProgram('A');
    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));
    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2021, 2022, 2023, 2024, 2025));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(program, true, availableDataYears), Integer.class))
        .thenReturn(List.of());

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    assertThat(tables.get(0).getTablePartitions().get(0).getYear(), equalTo(Year.now().getValue()));
  }

  private AnalyticsTableColumn getColumn(String column, AnalyticsTable analyticsTable) {
    return analyticsTable.getDimensionColumns().stream()
        .filter(col -> col.getName().equals(column))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Column '" + column + "' not found"));
  }

  @Test
  void verifyGetTableWithDataElements() {
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

    String aliasD1 = "(select eventdatavalues #>> '{%s, value}' " + FROM_CLAUSE + " ) as \"%s\"";
    String aliasD2 =
        "(select cast(eventdatavalues #>> '{%s, value}' as double precision) "
            + FROM_CLAUSE
            + "  and eventdatavalues #>> '{%s,value}' ~* '^(-?[0-9]+)(\\.[0-9]+)?$') as \"%s\"";
    String aliasD3 =
        "(select case when eventdatavalues #>> '{%s, value}' = 'true' then 1 when eventdatavalues #>> '{%s, value}' = 'false' then 0 else null end "
            + FROM_CLAUSE
            + " ) as \"%s\"";
    String aliasD4 =
        "(select cast(eventdatavalues #>> '{%s, value}' as timestamp) "
            + FROM_CLAUSE
            + "  and eventdatavalues #>> '{%s,value}' ~* '^\\d{4}-\\d{2}-\\d{2}(\\s|T)?((\\d{2}:)(\\d{2}:)?(\\d{2}))?(|.(\\d{3})|.(\\d{3})Z)?$') as \"%s\"";
    String aliasD5 =
        "(select ou.uid from organisationunit ou where ou.uid = "
            + "(select eventdatavalues #>> '{"
            + d5.getUid()
            + ", value}' "
            + FROM_CLAUSE
            + " )) as \""
            + d5.getUid()
            + "\"";
    String aliasD6 =
        "(select cast(eventdatavalues #>> '{%s, value}' as bigint) "
            + FROM_CLAUSE
            + "  and eventdatavalues #>> '{%s,value}' ~* '^(-?[0-9]+)(\\.[0-9]+)?$') as \"%s\"";
    String aliasD7 =
        "(select ST_GeomFromGeoJSON('{\"type\":\"Point\", \"coordinates\":' || (eventdatavalues #>> '{%s, value}') || ', \"crs\":{\"type\":\"name\", \"properties\":{\"name\":\"EPSG:4326\"}}}') from event where eventid=psi.eventid ) as \"%s\"";
    String aliasD5_geo =
        "(select ou.geometry from organisationunit ou where ou.uid = (select eventdatavalues #>> '{"
            + d5.getUid()
            + ", value}' "
            + FROM_CLAUSE
            + " )) as \""
            + d5.getUid()
            + "\"";
    String aliasD5_name =
        "(select ou.name from organisationunit ou where ou.uid = (select eventdatavalues #>> '{"
            + d5.getUid()
            + ", value}' "
            + FROM_CLAUSE
            + " )) as \""
            + d5.getUid()
            + "\"";

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(program, true, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + program.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + program.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(63 + OU_NAME_HIERARCHY_COUNT)
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
        .addColumn(
            d7.getUid(),
            GEOMETRY_POINT,
            toSelectExpression(aliasD7, d7.getUid())) // ValueType.COORDINATES
        // element d5 also creates a Geo column
        .addColumn(
            d5.getUid() + "_geom",
            GEOMETRY,
            toSelectExpression(aliasD5_geo, d5.getUid()),
            IndexType.GIST)
        // element d5 also creates a Name column
        .addColumn(
            d5.getUid() + "_name",
            TEXT,
            toSelectExpression(aliasD5_name, d5.getUid() + "_name"),
            Skip.SKIP)
        .withDefaultColumns(subject.getFixedColumns())
        .build()
        .verify();
  }

  @Test
  void verifyGetTableWithTrackedEntityAttribute() {
    when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().spatialSupport(true).build());
    Program program = createProgram('A');

    TrackedEntityAttribute tea1 = rnd.nextObject(TrackedEntityAttribute.class);
    tea1.setValueType(ValueType.ORGANISATION_UNIT);

    ProgramTrackedEntityAttribute tea = new ProgramTrackedEntityAttribute(program, tea1);

    program.setProgramAttributes(List.of(tea));

    DataElement d1 = createDataElement('Z', ValueType.TEXT, AggregationType.SUM);

    ProgramStage ps1 = createProgramStage('A', Set.of(d1));

    program.setProgramStages(Set.of(ps1));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));

    String aliasD1 = "(select eventdatavalues #>> '{%s, value}' " + FROM_CLAUSE + " ) as \"%s\"";
    String aliasTea1 =
        "(select %s from organisationunit ou where ou.uid = (select value from "
            + "trackedentityattributevalue where trackedentityid=pi.trackedentityid and "
            + "trackedentityattributeid=%d)) as \"%s\"";

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(program, true, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

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
            tea1.getUid(), TEXT, String.format(aliasTea1, "ou.uid", tea1.getId(), tea1.getUid()))
        // Second Geometry column created from the OU column above
        .addColumn(
            tea1.getUid() + "_geom",
            GEOMETRY,
            String.format(aliasTea1, "ou.geometry", tea1.getId(), tea1.getUid()),
            IndexType.GIST)
        .addColumn(
            tea1.getUid() + "_name",
            TEXT,
            String.format(aliasTea1, "ou.name", tea1.getId(), tea1.getUid()),
            Skip.SKIP)
        .withDefaultColumns(subject.getFixedColumns())
        .build()
        .verify();
  }

  @Test
  void verifyDataElementTypeOrgUnitFetchesOuNameWhenPopulatingEventAnalyticsTable() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().spatialSupport(true).build());
    Program programA = createProgram('A');

    DataElement d5 = createDataElement('G', ValueType.ORGANISATION_UNIT, AggregationType.NONE);

    ProgramStage ps1 = createProgramStage('A', Set.of(d5));

    programA.setProgramStages(Set.of(ps1));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(programA, true, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);
    assertFalse(analyticsTables.isEmpty());
    AnalyticsTablePartition partition = new AnalyticsTablePartition(analyticsTables.get(0));

    subject.populateTable(params, partition);
    verify(jdbcTemplate).execute(sql.capture());

    String ouQuery =
        "(select ou.%s from organisationunit ou where ou.uid = "
            + "(select eventdatavalues #>> '{"
            + d5.getUid()
            + ", value}' from event where "
            + "eventid=psi.eventid )) as \""
            + d5.getUid()
            + "\"";

    assertThat(sql.getValue(), containsString(String.format(ouQuery, "uid")));
    assertThat(sql.getValue(), containsString(String.format(ouQuery, "name")));
  }

  @Test
  void verifyTeiTypeOrgUnitFetchesOuNameWhenPopulatingEventAnalyticsTable() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().spatialSupport(true).build());
    Program programA = createProgram('A');

    TrackedEntityAttribute tea = createTrackedEntityAttribute('a', ValueType.ORGANISATION_UNIT);
    tea.setId(9999);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, tea);

    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));
    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(programA, true, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);
    assertFalse(analyticsTables.isEmpty());
    AnalyticsTablePartition partition = new AnalyticsTablePartition(analyticsTables.get(0));

    subject.populateTable(params, partition);
    verify(jdbcTemplate).execute(sql.capture());

    String ouQuery =
        "(select ou.%s from organisationunit ou where ou.uid = "
            + "(select value from trackedentityattributevalue where trackedentityid=pi.trackedentityid and "
            + "trackedentityattributeid=9999)) as \""
            + tea.getUid()
            + "\"";

    assertThat(sql.getValue(), containsString(String.format(ouQuery, "uid")));
    assertThat(sql.getValue(), containsString(String.format(ouQuery, "name")));
  }

  @Test
  @DisplayName("Verify that the TEA Attribute OU uses Centroid when the setting is enabled")
  void verifyGetTableWithOuTeisUseCentroid() {
    when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().spatialSupport(true).build());
    when(systemSettingManager.getBooleanSetting(SettingKey.ANALYTICS_EVENTS_OU_CENTROID))
        .thenReturn(true);
    Program program = createProgram('A');

    TrackedEntityAttribute tea1 = rnd.nextObject(TrackedEntityAttribute.class);
    tea1.setValueType(ValueType.ORGANISATION_UNIT);

    ProgramTrackedEntityAttribute tea = new ProgramTrackedEntityAttribute(program, tea1);

    program.setProgramAttributes(List.of(tea));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));

    // Approach 1: Extract the common part as a base template
    final String BASE_TEA_QUERY =
        "(select %s from organisationunit ou where ou.uid = (select value from "
            + "trackedentityattributevalue where trackedentityid=pi.trackedentityid and "
            + "trackedentityattributeid=%d))";

    String aliasTea1 = BASE_TEA_QUERY + " as \"%s\"";
    String centroidTea1 = "ST_Centroid(" + BASE_TEA_QUERY + ") as \"%s\"";

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(program, true, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + program.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + program.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(57 + OU_NAME_HIERARCHY_COUNT)
        .addColumns(periodColumns)
        .addColumn(
            tea1.getUid(), TEXT, String.format(aliasTea1, "ou.uid", tea1.getId(), tea1.getUid()))
        // Second Geometry column created from the OU column above
        .addColumn(
            tea1.getUid() + "_geom",
            GEOMETRY,
            String.format(centroidTea1, "ou.geometry", tea1.getId(), tea1.getUid()),
            IndexType.GIST)
        .addColumn(
            tea1.getUid() + "_name",
            TEXT,
            String.format(aliasTea1, "ou.name", tea1.getId(), tea1.getUid()),
            Skip.SKIP)
        .withDefaultColumns(subject.getFixedColumns())
        .build()
        .verify();
  }

  @Test
  @DisplayName("Verify that the DE Attribute OU uses Centroid when the setting is enabled")
  void verifyGetTableWithOuDeUseCentroid() {
    when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().spatialSupport(true).build());
    when(systemSettingManager.getBooleanSetting(SettingKey.ANALYTICS_EVENTS_OU_CENTROID))
        .thenReturn(true);
    Program program = createProgram('A');
    DataElement de1 = createDataElement('G', ValueType.ORGANISATION_UNIT, AggregationType.NONE);
    ProgramStage ps1 = createProgramStage('A', Set.of(de1));

    program.setProgramStages(Set.of(ps1));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));

    // Approach 1: Extract the common part as a base template
    final String BASE_DE_QUERY =
        "(select %s from organisationunit ou where ou.uid = "
            + "(select eventdatavalues #>> '{deabcdefghG, value}' "
            + "from event where eventid=psi.eventid ))";

    String aliasTea1 = BASE_DE_QUERY + " as \"%s\"";
    String centroidTea1 = "ST_Centroid(" + BASE_DE_QUERY + ") as \"%s\"";

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(program, true, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + program.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + program.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(57 + OU_NAME_HIERARCHY_COUNT)
        .addColumns(periodColumns)
        .addColumn(
            de1.getUid() + "_geom",
            GEOMETRY,
            String.format(centroidTea1, "ou.geometry", de1.getUid(), de1.getUid()),
            IndexType.GIST)
        .addColumn(
            de1.getUid() + "_name",
            TEXT,
            String.format(aliasTea1, "ou.name", de1.getUid(), de1.getUid()),
            Skip.SKIP)
        .withDefaultColumns(subject.getFixedColumns())
        .build()
        .verify();
  }

  @Test
  @DisplayName("Verify that the TEA Attribute: OU Centroid is used when the setting is enabled")
  void verifyGetTableWithOuTeisUseCentroid2() {
    when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().spatialSupport(true).build());
    when(systemSettingManager.getBooleanSetting(SettingKey.ANALYTICS_EVENTS_OU_CENTROID))
        .thenReturn(true);
    Program program = createProgram('A');

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(program, true, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + program.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + program.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(55)
        .withDefaultColumns(subject.getFixedColumns())
        .addColumns(periodColumns)
        .addColumn("ougeometry", GEOMETRY, "ST_Centroid(ou.geometry)", IndexType.GIST)
        .build()
        .verify();
  }

  @Test
  void verifyOrgUnitOwnershipJoinsWhenPopulatingEventAnalyticsTable() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().spatialSupport(true).build());
    Program programA = createProgram('A');

    TrackedEntityAttribute tea = createTrackedEntityAttribute('a', ValueType.ORGANISATION_UNIT);
    tea.setId(9999);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, tea);

    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));
    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(programA, true, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);
    assertFalse(analyticsTables.isEmpty());
    AnalyticsTablePartition partition = new AnalyticsTablePartition(analyticsTables.get(0));

    subject.populateTable(params, partition);

    verify(jdbcTemplate).execute(sql.capture());

    String ouEnrollmentLeftJoin =
        "left join organisationunit enrollmentou on pi.organisationunitid=enrollmentou.organisationunitid";
    String ouRegistrationLeftJoin =
        "left join organisationunit registrationou on tei.organisationunitid=registrationou.organisationunitid";

    assertThat(sql.getValue(), containsString(ouEnrollmentLeftJoin));
    assertThat(sql.getValue(), containsString(ouRegistrationLeftJoin));
  }

  @Test
  void verifyGetAnalyticsTableWithOuLevels() {
    List<OrganisationUnitLevel> ouLevels = rnd.objects(OrganisationUnitLevel.class, 2).toList();
    Program programA = rnd.nextObject(Program.class);
    programA.setId(0);

    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);
    int startYear = availableDataYears.get(0);
    int latestYear = availableDataYears.get(availableDataYears.size() - 1);

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));

    when(organisationUnitService.getFilledOrganisationUnitLevels()).thenReturn(ouLevels);
    when(jdbcTemplate.queryForList(
            "select temp.supportedyear from (select distinct extract(year from "
                + DATE_CLAUSE
                + ") as supportedyear "
                + "from event psi inner join enrollment pi on psi.enrollmentid = pi.enrollmentid "
                + "where psi.lastupdated <= '2019-08-01T00:00:00' and pi.programid = 0 and ("
                + DATE_CLAUSE
                + ") is not null "
                + "and ("
                + DATE_CLAUSE
                + ") > '1000-01-01' and psi.deleted = false ) "
                + "as temp where temp.supportedyear >= "
                + startYear
                + " and temp.supportedyear <= "
                + latestYear,
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().withStartTime(START_TIME).build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + programA.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + programA.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(
            subject.getFixedColumns().size()
                + PeriodType.getAvailablePeriodTypes().size()
                + ouLevels.size()
                + (programA.isRegistration() ? 1 : 0)
                + OU_NAME_HIERARCHY_COUNT)
        .addColumns(periodColumns)
        .withDefaultColumns(subject.getFixedColumns())
        .addColumn(("uidlevel" + ouLevels.get(0).getLevel()), col -> match(ouLevels.get(0), col))
        .addColumn(("uidlevel" + ouLevels.get(1).getLevel()), col -> match(ouLevels.get(1), col))
        .build()
        .verify();
  }

  @Test
  void verifyGetAnalyticsTableWithOuGroupSet() {
    List<OrganisationUnitGroupSet> ouGroupSet =
        rnd.objects(OrganisationUnitGroupSet.class, 2).toList();
    Program programA = rnd.nextObject(Program.class);
    programA.setId(0);

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));
    when(idObjectManager.getDataDimensionsNoAcl(OrganisationUnitGroupSet.class))
        .thenReturn(ouGroupSet);
    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().withStartTime(START_TIME).build();
    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(programA, false, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));
    List<AnalyticsTableColumn> fixedColumns = subject.getFixedColumns();
    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + programA.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + programA.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(
            fixedColumns.size()
                + PeriodType.getAvailablePeriodTypes().size()
                + ouGroupSet.size()
                + (programA.isRegistration() ? 1 : 0)
                + OU_NAME_HIERARCHY_COUNT)
        .addColumns(periodColumns)
        .withDefaultColumns(fixedColumns)
        .addColumn(ouGroupSet.get(0).getUid(), col -> match(ouGroupSet.get(0), col))
        .addColumn(ouGroupSet.get(1).getUid(), col -> match(ouGroupSet.get(1), col))
        .build()
        .verify();
  }

  @Test
  void verifyGetAnalyticsTableWithOptionGroupSets() {
    List<CategoryOptionGroupSet> cogs = rnd.objects(CategoryOptionGroupSet.class, 2).toList();
    Program programA = rnd.nextObject(Program.class);
    programA.setId(0);

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));
    when(categoryService.getAttributeCategoryOptionGroupSetsNoAcl()).thenReturn(cogs);
    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYear(programA, false, availableDataYears), Integer.class))
        .thenReturn(List.of(2018, 2019));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().withStartTime(START_TIME).build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));
    List<AnalyticsTableColumn> fixedColumns = subject.getFixedColumns();
    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + programA.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + programA.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(
            fixedColumns.size()
                + PeriodType.getAvailablePeriodTypes().size()
                + cogs.size()
                + (programA.isRegistration() ? 1 : 0)
                + OU_NAME_HIERARCHY_COUNT)
        .addColumns(periodColumns)
        .withDefaultColumns(fixedColumns)
        .addColumn(cogs.get(0).getUid(), col -> match(cogs.get(0), col))
        .addColumn(cogs.get(1).getUid(), col -> match(cogs.get(1), col))
        .build()
        .verify();
  }

  private void match(OrganisationUnitGroupSet ouGroupSet, AnalyticsTableColumn col) {
    String expression = "ougs." + quote(ouGroupSet.getUid());
    assertNotNull(col);
    assertThat(col.getSelectExpression(), is(expression));
    match(col);
  }

  private void match(OrganisationUnitLevel ouLevel, AnalyticsTableColumn col) {
    String expression = "ous." + quote("uidlevel" + ouLevel.getLevel());
    assertNotNull(col);
    assertThat(col.getSelectExpression(), is(expression));
    match(col);
  }

  private void match(CategoryOptionGroupSet cog, AnalyticsTableColumn col) {
    String expression = "acs." + quote(cog.getUid());
    assertNotNull(col);
    assertThat(col.getSelectExpression(), is(expression));
    match(col);
  }

  private void match(AnalyticsTableColumn col) {
    assertNotNull(col.getCreated());
    assertThat(col.getDataType(), is(CHARACTER_11));
    assertThat(col.isSkipIndex(), is(false));
    assertThat(col.getNullable(), is(NULL));
    assertThat(col.getIndexColumns(), hasSize(0));
  }

  @Test
  void verifyTeaTypeOrgUnitFetchesOuNameWhenPopulatingEventAnalyticsTable() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    when(databaseInfoProvider.getDatabaseInfo())
        .thenReturn(DatabaseInfo.builder().spatialSupport(true).build());
    Program programA = createProgram('A');

    TrackedEntityAttribute tea = createTrackedEntityAttribute('a', ValueType.ORGANISATION_UNIT);
    tea.setId(9999);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, tea);

    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));

    when(periodDataProvider.getAvailableYears(DATABASE))
        .thenReturn(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);
    int startYear = availableDataYears.get(0);
    int latestYear = availableDataYears.get(availableDataYears.size() - 1);

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));

    when(jdbcTemplate.queryForList(
            "select temp.supportedyear from (select distinct extract(year from "
                + DATE_CLAUSE
                + ") as supportedyear "
                + "from event psi "
                + "inner join enrollment pi on psi.enrollmentid = pi.enrollmentid "
                + "where psi.lastupdated <= '2019-08-01T00:00:00' and pi.programid = 0 "
                + "and ("
                + DATE_CLAUSE
                + ") is not null "
                + "and ("
                + DATE_CLAUSE
                + ") > '1000-01-01' and psi.deleted = false and ("
                + DATE_CLAUSE
                + ") >= '2018-01-01') "
                + "as temp where temp.supportedyear >= "
                + startYear
                + " and temp.supportedyear <= "
                + latestYear,
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .withLastYears(2)
            .withStartTime(START_TIME)
            .withToday(today)
            .build();

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);
    assertFalse(analyticsTables.isEmpty());
    AnalyticsTablePartition partition = new AnalyticsTablePartition(analyticsTables.get(0));

    subject.populateTable(params, partition);

    verify(jdbcTemplate).execute(sql.capture());

    String ouQuery =
        """
        (select ou.%s from organisationunit ou where ou.uid = \
        (select value from trackedentityattributevalue where trackedentityid=pi.trackedentityid and \
        trackedentityattributeid=9999)) as %s""";

    assertThat(sql.getValue(), containsString(String.format(ouQuery, "uid", quote(tea.getUid()))));
    assertThat(sql.getValue(), containsString(String.format(ouQuery, "name", quote(tea.getUid()))));
  }

  private String toSelectExpression(String template, String uid) {
    return String.format(template, uid, uid, uid);
  }

  private void addCategoryCombo(Program program, CategoryCombo categoryCombo) {
    program.setCategoryCombo(categoryCombo);
  }

  private String getYearQueryForCurrentYear(
      Program program, boolean withExecutionDate, List<Integer> availableDataYears) {
    int startYear = availableDataYears.get(0);
    int latestYear = availableDataYears.get(availableDataYears.size() - 1);

    String sql =
        "select temp.supportedyear from (select distinct "
            + "extract(year from "
            + DATE_CLAUSE
            + ") as supportedyear "
            + "from event psi "
            + "inner join enrollment pi on psi.enrollmentid = pi.enrollmentid "
            + "where psi.lastupdated <= '2019-08-01T00:00:00' "
            + "and pi.programid = "
            + program.getId()
            + " and ("
            + DATE_CLAUSE
            + ") is not null and ("
            + DATE_CLAUSE
            + ") > '1000-01-01' and psi.deleted = false ";

    if (withExecutionDate) {
      sql += "and (" + DATE_CLAUSE + ") >= '2018-01-01'";
    }

    sql +=
        ") as temp where temp.supportedyear >= "
            + startYear
            + " and temp.supportedyear <= "
            + latestYear;

    return sql;
  }
}
