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

import static java.time.LocalDate.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import static org.hisp.dhis.period.PeriodDataProvider.PeriodSource.DATABASE;
import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.hisp.dhis.program.ProgramType.WITH_REGISTRATION;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.test.TestBase.createCategory;
import static org.hisp.dhis.test.TestBase.createCategoryCombo;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.hisp.dhis.test.TestBase.createProgramTrackedEntityAttribute;
import static org.hisp.dhis.test.TestBase.createTrackedEntityAttribute;
import static org.hisp.dhis.test.TestBase.getDate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.analytics.util.AnalyticsTableAsserter;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.PeriodDataProvider.PeriodSource;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.test.random.BeanRandomizer;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
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

  @Mock private SystemSettingsProvider settingsProvider;

  @Mock private SystemSettings settings;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private ResourceTableService resourceTableService;

  @Mock private PeriodDataProvider periodDataProvider;

  @Mock private AnalyticsTableSettings analyticsTableSettings;

  @Spy private SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private JdbcEventAnalyticsTableManager subject;

  private Date today;

  private static final Date START_TIME = new DateTime(2019, 8, 1, 0, 0).toDate();

  private static final String TABLE_PREFIX = "analytics_event_";

  private static final String DATE_CLAUSE =
      "CASE WHEN 'SCHEDULE' = ev.status THEN ev.scheduleddate ELSE ev.occurreddate END";

  private static final int OU_NAME_HIERARCHY_COUNT = 1;

  private final List<AnalyticsTableColumn> periodColumns =
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
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    subject =
        new JdbcEventAnalyticsTableManager(
            idObjectManager,
            organisationUnitService,
            categoryService,
            settingsProvider,
            null,
            resourceTableService,
            null,
            null,
            jdbcTemplate,
            analyticsTableSettings,
            periodDataProvider,
            new ColumnMapper(sqlBuilder, settingsProvider),
            sqlBuilder);
    today = Date.from(LocalDate.of(2019, 7, 6).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(settings.getLastSuccessfulResourceTablesUpdate()).thenReturn(new Date(0L));
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
            .startTime(startTime)
            .skipPrograms(skipPrograms)
            .build()
            .withLatestPartition();

    List<Map<String, Object>> queryResp = new ArrayList<>();
    queryResp.add(Map.of("dataelementid", 1));

    when(settings.getLastSuccessfulAnalyticsTablesUpdate()).thenReturn(lastFullTableUpdate);
    when(settings.getLastSuccessfulLatestAnalyticsPartitionUpdate())
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
    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYearProgramWithRegistration(program, true, availableDataYears),
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(2)
            .startTime(START_TIME)
            .today(today)
            .build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withTableType(AnalyticsTableType.EVENT)
        .withName(TABLE_PREFIX + program.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + program.getUid().toLowerCase())
        .withColumnSize(57 + OU_NAME_HIERARCHY_COUNT)
        .withDefaultColumns(EventAnalyticsColumn.getColumns(sqlBuilder, false, true))
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
    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYearProgramWithRegistration(program, true, availableDataYears),
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(2)
            .startTime(START_TIME)
            .today(today)
            .build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    AnalyticsTableColumn lastUpdated = getColumn("lastupdated", tables.get(0));
    AnalyticsTableColumn created = getColumn("created", tables.get(0));

    assertThat(
        lastUpdated.getSelectExpression(),
        is(
            """
            case when ev.lastupdatedatclient is not null \
            then ev.lastupdatedatclient else ev.lastupdated end"""));
    assertThat(
        created.getSelectExpression(),
        is(
            """
            case when ev.createdatclient is not null \
            then ev.createdatclient else ev.created end"""));
  }

  @Test
  void verifyAnalyticsEventTableHasDefaultPartition() {
    Program program = createProgram('A');
    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));
    mockPeriodYears(List.of(2021, 2022, 2023, 2024, 2025));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYearProgramWithRegistration(program, true, availableDataYears),
            Integer.class))
        .thenReturn(List.of());

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(2)
            .startTime(START_TIME)
            .today(today)
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

    String aliasD1 = "eventdatavalues #>> '{%s, value}' as \"%s\"";
    String aliasD2 =
        """
        case when eventdatavalues #>> '{deabcdefghP, value}' ~* '^(-?[0-9]+)(\\.[0-9]+)?$' \
        then cast(eventdatavalues #>> '{deabcdefghP, value}' as double precision) end as "deabcdefghP\"""";
    String aliasD3 =
        """
        case when eventdatavalues #>> '{deabcdefghY, value}' = 'true' then 1 \
        when eventdatavalues #>> '{deabcdefghY, value}' = 'false' then 0 else null end as "deabcdefghY\"""";
    String aliasD4 =
        """
        case when eventdatavalues #>> '{deabcdefghW, value}' ~* '^[0-9]{4}-[0-9]{2}-[0-9]{2}(\\s|T)?(([0-9]{2}:)([0-9]{2}:)?([0-9]{2}))?(|.([0-9]{3})|.([0-9]{3})Z)?$' \
        then cast(eventdatavalues #>> '{deabcdefghW, value}' as timestamp) end as "deabcdefghW\"""";
    String aliasD5 =
        "eventdatavalues #>> '{" + d5.getUid() + ", value}' as \"" + d5.getUid() + "\"";
    String aliasD6 =
        """
        case when eventdatavalues #>> '{deabcdefghH, value}' ~* '^(-?[0-9]+)(\\.[0-9]+)?$' \
        then cast(eventdatavalues #>> '{deabcdefghH, value}' as bigint) end as "deabcdefghH\"""";
    String aliasD7 =
        """
        ST_GeomFromGeoJSON('{\"type\":\"Point\", \"coordinates\":' || (eventdatavalues #>> '{deabcdefghU, value}') || ', "crs":{"type":"name", "properties":{"name":"EPSG:4326"}}}') as "deabcdefghU\"""";
    String aliasD5_geo =
        "(select ou.geometry from \"organisationunit\" ou where ou.uid = eventdatavalues #>> '{"
            + d5.getUid()
            + ", value}') as \""
            + d5.getUid()
            + "\"";
    String aliasD5_name =
        "(select ou.name from \"organisationunit\" ou where ou.uid = eventdatavalues #>> '{"
            + d5.getUid()
            + ", value}') as \""
            + d5.getUid()
            + "\"";
    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(2)
            .startTime(START_TIME)
            .today(today)
            .build();

    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYearProgramWithRegistration(program, true, availableDataYears),
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + program.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + program.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(64 + OU_NAME_HIERARCHY_COUNT)
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
        .addColumn(d5.getUid() + "_geom", GEOMETRY, aliasD5_geo, IndexType.GIST)
        // element d5 also creates a Name column
        .addColumn(d5.getUid() + "_name", TEXT, aliasD5_name, Skip.SKIP)
        .withDefaultColumns(EventAnalyticsColumn.getColumns(sqlBuilder, false, true))
        .build()
        .verify();
  }

  @Test
  void verifyGetTableWithTrackedEntityAttribute() {
    Program program = createProgram('A');

    TrackedEntityAttribute tea1 = rnd.nextObject(TrackedEntityAttribute.class);
    tea1.setValueType(ValueType.ORGANISATION_UNIT);

    ProgramTrackedEntityAttribute tea = new ProgramTrackedEntityAttribute(program, tea1);

    program.setProgramAttributes(List.of(tea));

    DataElement d1 = createDataElement('Z', ValueType.TEXT, AggregationType.SUM);

    ProgramStage ps1 = createProgramStage('A', Set.of(d1));

    program.setProgramStages(Set.of(ps1));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(program));

    String aliasD1 =
        """
        eventdatavalues #>> '{deabcdefghZ, value}' as "deabcdefghZ\"""";
    String aliasTeaUid = "%s.value";

    String ouGeometryQuery =
        String.format(
            """
            (select ou.geometry from "organisationunit" ou where ou.uid = %1$s.value) as %1$s""",
            quote(tea1.getUid()));

    String ouNameQuery =
        String.format(
            """
            (select ou.name from "organisationunit" ou where ou.uid = %1$s.value) as %1$s""",
            quote(tea1.getUid()));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(2)
            .startTime(START_TIME)
            .today(today)
            .build();

    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYearProgramWithRegistration(program, true, availableDataYears),
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + program.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + program.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(59 + OU_NAME_HIERARCHY_COUNT)
        .addColumns(periodColumns)
        .addColumn(d1.getUid(), TEXT, toSelectExpression(aliasD1, d1.getUid()), Skip.SKIP)
        .addColumn(tea1.getUid(), TEXT, String.format(aliasTeaUid, quote(tea1.getUid())))
        // Org unit geometry column
        .addColumn(tea1.getUid() + "_geom", GEOMETRY, ouGeometryQuery, IndexType.GIST)
        // Org unit name column
        .addColumn(tea1.getUid() + "_name", TEXT, ouNameQuery, Skip.SKIP)
        .withDefaultColumns(EventAnalyticsColumn.getColumns(sqlBuilder, false, true))
        .build()
        .verify();
  }

  @Test
  void verifyDataElementTypeOrgUnitFetchesOuNameWhenPopulatingEventAnalyticsTable() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    Program programA = createProgram('A');

    DataElement d5 = createDataElement('G', ValueType.ORGANISATION_UNIT, AggregationType.NONE);

    ProgramStage ps1 = createProgramStage('A', Set.of(d5));

    programA.setProgramStages(Set.of(ps1));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(2)
            .startTime(START_TIME)
            .today(today)
            .build();

    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYearProgramWithRegistration(programA, true, availableDataYears),
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);
    assertFalse(analyticsTables.isEmpty());
    AnalyticsTablePartition partition = new AnalyticsTablePartition(analyticsTables.get(0));

    subject.populateTable(params, partition);
    verify(jdbcTemplate).execute(sql.capture());

    String ouUidQuery =
        String.format(
            """
            eventdatavalues #>> '{%s, value}' as %s""",
            d5.getUid(), quote(d5.getUid()));

    String ouNameQuery =
        String.format(
            """
            (select ou.name from "organisationunit" ou where ou.uid = \
            eventdatavalues #>> '{%s, value}') as %s""",
            d5.getUid(), quote(d5.getUid()));

    assertThat(sql.getValue(), containsString(ouUidQuery));
    assertThat(sql.getValue(), containsString(ouNameQuery));
  }

  @Test
  void verifyTeiTypeOrgUnitFetchesOuNameWhenPopulatingEventAnalyticsTable() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    Program programA = createProgram('A');

    TrackedEntityAttribute tea = createTrackedEntityAttribute('a', ValueType.ORGANISATION_UNIT);
    tea.setId(9999);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, tea);

    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));
    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(2)
            .startTime(START_TIME)
            .today(today)
            .build();

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYearProgramWithRegistration(programA, true, availableDataYears),
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);
    assertFalse(analyticsTables.isEmpty());
    AnalyticsTablePartition partition = new AnalyticsTablePartition(analyticsTables.get(0));

    subject.populateTable(params, partition);
    verify(jdbcTemplate).execute(sql.capture());

    String ouUidQuery = String.format("%s.value", quote(tea.getUid()));

    String ouGeometryQuery =
        String.format(
            """
            (select ou.geometry from "organisationunit" ou where ou.uid = %1$s.value) as %1$s""",
            quote(tea.getUid()));

    String ouNameQuery =
        String.format(
            """
            (select ou.name from "organisationunit" ou where ou.uid = %1$s.value) as %1$s""",
            quote(tea.getUid()));

    assertThat(sql.getValue(), containsString(ouUidQuery));
    assertThat(sql.getValue(), containsString(ouGeometryQuery));
    assertThat(sql.getValue(), containsString(ouNameQuery));
  }

  @Test
  void verifyOrgUnitOwnershipJoinsWhenPopulatingEventAnalyticsTable() {
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    Program programA = createProgram('A');
    programA.setProgramType(WITH_REGISTRATION);

    TrackedEntityAttribute tea = createTrackedEntityAttribute('a', ValueType.ORGANISATION_UNIT);
    tea.setId(9999);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, tea);

    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));
    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder()
            .lastYears(2)
            .startTime(START_TIME)
            .today(today)
            .build();

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYearProgramWithRegistration(programA, true, availableDataYears),
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);
    assertFalse(analyticsTables.isEmpty());
    AnalyticsTablePartition partition = new AnalyticsTablePartition(analyticsTables.get(0));

    subject.populateTable(params, partition);

    verify(jdbcTemplate).execute(sql.capture());

    String ouEnrollmentLeftJoin =
        "left join \"organisationunit\" enrollmentou on en.organisationunitid=enrollmentou.organisationunitid";
    String ouRegistrationLeftJoin =
        "left join \"organisationunit\" registrationou on te.organisationunitid=registrationou.organisationunitid";

    assertThat(sql.getValue(), containsString(ouEnrollmentLeftJoin));
    assertThat(sql.getValue(), containsString(ouRegistrationLeftJoin));
  }

  @Test
  void verifyGetAnalyticsTableWithOuLevels() {
    List<OrganisationUnitLevel> ouLevels = rnd.objects(OrganisationUnitLevel.class, 2).toList();
    ProgramStage psA = new ProgramStage();
    psA.setId(123456);

    Program programA = rnd.nextObject(Program.class);
    programA.setId(0);
    programA.setProgramType(WITH_REGISTRATION);
    programA.setProgramStages(Set.of(psA));

    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);
    int startYear = availableDataYears.get(0);
    int latestYear = availableDataYears.get(availableDataYears.size() - 1);

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));

    when(organisationUnitService.getFilledOrganisationUnitLevels()).thenReturn(ouLevels);
    when(jdbcTemplate.queryForList(
            "select temp.supportedyear from (select distinct extract(year from "
                + DATE_CLAUSE
                + ") as supportedyear "
                + "from \"trackerevent\" ev inner join \"enrollment\" en on ev.enrollmentid = en.enrollmentid "
                + "where ev.lastupdated <= '2019-08-01T00:00:00' and en.programid = 0 and ("
                + DATE_CLAUSE
                + ") is not null "
                + "and ("
                + DATE_CLAUSE
                + ") > '1000-01-01' and ev.\"deleted\" = false ) "
                + "as temp where temp.supportedyear >= "
                + startYear
                + " and temp.supportedyear <= "
                + latestYear,
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().startTime(START_TIME).build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    int extraColumnsOnlyForRegistration = 2;

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + programA.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + programA.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(
            EventAnalyticsColumn.getColumns(sqlBuilder, false, true).size()
                + PeriodType.getAvailablePeriodTypes().size()
                + (programA.isRegistration() ? extraColumnsOnlyForRegistration : 0)
                + ouLevels.size()
                + OU_NAME_HIERARCHY_COUNT)
        .addColumns(periodColumns)
        .withDefaultColumns(EventAnalyticsColumn.getColumns(sqlBuilder, false, true))
        .addColumn(("uidlevel" + ouLevels.get(0).getLevel()), col -> match(ouLevels.get(0), col))
        .addColumn(("uidlevel" + ouLevels.get(1).getLevel()), col -> match(ouLevels.get(1), col))
        .build()
        .verify();
  }

  @Test
  void verifyGetAnalyticsTableWithOuGroupSet() {
    List<OrganisationUnitGroupSet> ouGroupSet =
        rnd.objects(OrganisationUnitGroupSet.class, 2).toList();
    ProgramStage psA = new ProgramStage();
    psA.setId(123456);

    Program programA = rnd.nextObject(Program.class);
    programA.setId(0);
    programA.setProgramType(WITHOUT_REGISTRATION);
    programA.setProgramStages(Set.of(psA));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));
    when(idObjectManager.getDataDimensionsNoAcl(OrganisationUnitGroupSet.class))
        .thenReturn(ouGroupSet);
    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().startTime(START_TIME).build();
    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYearProgramWithoutRegistration(availableDataYears),
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + programA.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + programA.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(
            EventAnalyticsColumn.getColumns(sqlBuilder, false, false).size()
                + PeriodType.getAvailablePeriodTypes().size()
                + ouGroupSet.size()
                + (programA.isRegistration() ? 1 : 0)
                + OU_NAME_HIERARCHY_COUNT)
        .addColumns(periodColumns)
        .withDefaultColumns(EventAnalyticsColumn.getColumns(sqlBuilder, false, false))
        .addColumn(ouGroupSet.get(0).getUid(), col -> match(ouGroupSet.get(0), col))
        .addColumn(ouGroupSet.get(1).getUid(), col -> match(ouGroupSet.get(1), col))
        .build()
        .verify();
  }

  @Test
  void verifyGetAnalyticsTableWithOptionGroupSets() {
    List<CategoryOptionGroupSet> cogs = rnd.objects(CategoryOptionGroupSet.class, 2).toList();
    ProgramStage psA = new ProgramStage();
    psA.setId(123456);

    Program programA = rnd.nextObject(Program.class);
    programA.setId(0);
    programA.setProgramType(WITH_REGISTRATION);
    programA.setProgramStages(Set.of(psA));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));
    when(categoryService.getAttributeCategoryOptionGroupSetsNoAcl()).thenReturn(cogs);
    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);

    when(jdbcTemplate.queryForList(
            getYearQueryForCurrentYearProgramWithRegistration(programA, false, availableDataYears),
            Integer.class))
        .thenReturn(List.of(2018, 2019));

    AnalyticsTableUpdateParams params =
        AnalyticsTableUpdateParams.newBuilder().startTime(START_TIME).build();

    List<AnalyticsTable> tables = subject.getAnalyticsTables(params);

    assertThat(tables, hasSize(1));

    int extraColumnsOnlyForRegistration = 2;

    new AnalyticsTableAsserter.Builder(tables.get(0))
        .withName(TABLE_PREFIX + programA.getUid().toLowerCase() + STAGING_TABLE_SUFFIX)
        .withMainName(TABLE_PREFIX + programA.getUid().toLowerCase())
        .withTableType(AnalyticsTableType.EVENT)
        .withColumnSize(
            EventAnalyticsColumn.getColumns(sqlBuilder, false, true).size()
                + PeriodType.getAvailablePeriodTypes().size()
                + cogs.size()
                + (programA.isRegistration() ? extraColumnsOnlyForRegistration : 0)
                + OU_NAME_HIERARCHY_COUNT)
        .addColumns(periodColumns)
        .withDefaultColumns(EventAnalyticsColumn.getColumns(sqlBuilder, false, true))
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
    Program programA = createProgram('A');
    programA.setProgramType(WITH_REGISTRATION);

    TrackedEntityAttribute tea = createTrackedEntityAttribute('a', ValueType.ORGANISATION_UNIT);
    tea.setId(9999);

    ProgramTrackedEntityAttribute programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, tea);

    programA.setProgramAttributes(List.of(programTrackedEntityAttribute));

    when(idObjectManager.getAllNoAcl(Program.class)).thenReturn(List.of(programA));
    mockPeriodYears(List.of(2018, 2019, now().getYear()));

    List<Integer> availableDataYears = periodDataProvider.getAvailableYears(DATABASE);
    int startYear = availableDataYears.get(0);
    int latestYear = availableDataYears.get(availableDataYears.size() - 1);

    when(jdbcTemplate.queryForList(
            "select temp.supportedyear from (select distinct extract(year from "
                + DATE_CLAUSE
                + ") as supportedyear "
                + "from \"trackerevent\" ev "
                + "inner join \"enrollment\" en on ev.enrollmentid = en.enrollmentid "
                + "where ev.lastupdated <= '2019-08-01T00:00:00' and en.programid = 0 "
                + "and ("
                + DATE_CLAUSE
                + ") is not null "
                + "and ("
                + DATE_CLAUSE
                + ") > '1000-01-01' and ev.\"deleted\" = false and ("
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
            .lastYears(2)
            .startTime(START_TIME)
            .today(today)
            .build();

    List<AnalyticsTable> analyticsTables = subject.getAnalyticsTables(params);
    assertFalse(analyticsTables.isEmpty());
    AnalyticsTablePartition partition = new AnalyticsTablePartition(analyticsTables.get(0));

    subject.populateTable(params, partition);

    verify(jdbcTemplate).execute(sql.capture());

    String ouUidQuery = String.format("%s.value", quote(tea.getUid()));

    String ouGeometryQuery =
        String.format(
            """
            (select ou.geometry from "organisationunit" ou where ou.uid = %1$s.value) as %1$s""",
            quote(tea.getUid()));

    String ouNameQuery =
        String.format(
            """
            (select ou.name from "organisationunit" ou where ou.uid = %1$s.value) as %1$s""",
            quote(tea.getUid()));

    assertThat(sql.getValue(), containsString(ouUidQuery));
    assertThat(sql.getValue(), containsString(ouGeometryQuery));
    assertThat(sql.getValue(), containsString(ouNameQuery));
  }

  private String toSelectExpression(String template, String uid) {
    return String.format(template, uid, uid, uid);
  }

  private void addCategoryCombo(Program program, CategoryCombo categoryCombo) {
    program.setCategoryCombo(categoryCombo);
  }

  private String getYearQueryForCurrentYearProgramWithoutRegistration(
      List<Integer> availableDataYears) {
    int startYear = availableDataYears.get(0);
    int latestYear = availableDataYears.get(availableDataYears.size() - 1);
    String dataClause = "ev.occurreddate";

    String sql =
        "select temp.supportedyear from (select distinct "
            + "extract(year from "
            + dataClause
            + ") as supportedyear "
            + "from \"singleevent\" ev "
            + "inner join \"programstage\" ps on ev.programstageid=ps.programstageid "
            + "where ev.lastupdated <= '2019-08-01T00:00:00' "
            + "and ("
            + dataClause
            + ") is not null and ("
            + dataClause
            + ") > '1000-01-01' "
            + "and ev.programstageid = 123456 "
            + "and ev.\"deleted\" = false "
            + ") as temp where temp.supportedyear >= "
            + startYear
            + " and temp.supportedyear <= "
            + latestYear;

    return sql;
  }

  private String getYearQueryForCurrentYearProgramWithRegistration(
      Program program, boolean withExecutionDate, List<Integer> availableDataYears) {
    int startYear = availableDataYears.get(0);
    int latestYear = availableDataYears.get(availableDataYears.size() - 1);

    String sql =
        "select temp.supportedyear from (select distinct "
            + "extract(year from "
            + DATE_CLAUSE
            + ") as supportedyear "
            + "from \"trackerevent\" ev "
            + "inner join \"enrollment\" en on ev.enrollmentid = en.enrollmentid "
            + "where ev.lastupdated <= '2019-08-01T00:00:00'"
            + " and en.programid = "
            + program.getId()
            + " and ("
            + DATE_CLAUSE
            + ") is not null and ("
            + DATE_CLAUSE
            + ") > '1000-01-01'"
            + " and ev.\"deleted\" = false ";

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

  private void mockPeriodYears(List<Integer> years) {
    when(analyticsTableSettings.getPeriodSource()).thenReturn(PeriodSource.DATABASE);
    when(periodDataProvider.getAvailableYears(DATABASE)).thenReturn(years);
  }
}
