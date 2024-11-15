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

import static java.lang.String.join;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.EXPORTABLE_EVENT_STATUSES;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getColumnType;
import static org.hisp.dhis.analytics.util.DisplayNameUtils.getDisplayName;
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.DataType.BOOLEAN;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;
import static org.hisp.dhis.db.model.DataType.VARCHAR_255;
import static org.hisp.dhis.db.model.DataType.VARCHAR_50;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.util.DateUtils.toLongDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.analytics.TrackedEntityAnalyticsTableManager")
public class JdbcTrackedEntityAnalyticsTableManager extends AbstractJdbcTableManager {
  private static final String PROGRAMS_BY_TET_KEY = "programsByTetUid";

  private static final String ALL_NON_CONFIDENTIAL_TET_ATTRIBUTES =
      "allNonConfidentialTetAttributes";

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private static final List<AnalyticsTableColumn> FIXED_GROUP_BY_COLS =
      List.of(
          AnalyticsTableColumn.builder()
              .name("trackedentity")
              .dataType(CHARACTER_11)
              .nullable(NOT_NULL)
              .selectExpression("te.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("trackedentityid")
              .dataType(INTEGER)
              .nullable(NOT_NULL)
              .selectExpression("te.trackedentityid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("created")
              .dataType(TIMESTAMP)
              .selectExpression("te.created")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdated")
              .dataType(TIMESTAMP)
              .selectExpression("te.lastupdated")
              .build(),
          AnalyticsTableColumn.builder()
              .name("inactive")
              .dataType(BOOLEAN)
              .selectExpression("te.inactive")
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdatclient")
              .dataType(TIMESTAMP)
              .selectExpression("te.createdatclient")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedatclient")
              .dataType(TIMESTAMP)
              .selectExpression("te.lastupdatedatclient")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastsynchronized")
              .dataType(TIMESTAMP)
              .selectExpression("te.lastsynchronized")
              .build(),
          AnalyticsTableColumn.builder()
              .name("geometry")
              .dataType(GEOMETRY)
              .selectExpression("te.geometry")
              .indexType(IndexType.GIST)
              .build(),
          AnalyticsTableColumn.builder()
              .name("longitude")
              .dataType(DOUBLE)
              .selectExpression(
                  "case when 'POINT' = GeometryType(te.geometry) then ST_X(te.geometry) else null end")
              .build(),
          AnalyticsTableColumn.builder()
              .name("latitude")
              .dataType(DOUBLE)
              .selectExpression(
                  "case when 'POINT' = GeometryType(te.geometry) then ST_Y(te.geometry) else null end")
              .build(),
          AnalyticsTableColumn.builder()
              .name("featuretype")
              .dataType(VARCHAR_255)
              .selectExpression("te.featuretype")
              .build(),
          AnalyticsTableColumn.builder()
              .name("coordinates")
              .dataType(TEXT)
              .selectExpression("te.coordinates")
              .build(),
          AnalyticsTableColumn.builder()
              .name("storedby")
              .dataType(VARCHAR_255)
              .selectExpression("te.storedby")
              .build(),
          AnalyticsTableColumn.builder()
              .name("potentialduplicate")
              .dataType(BOOLEAN)
              .selectExpression("te.potentialduplicate")
              .build(),
          AnalyticsTableColumn.builder()
              .name("ou")
              .dataType(CHARACTER_11)
              .selectExpression("ou.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("ouname")
              .dataType(VARCHAR_255)
              .selectExpression("ou.name")
              .build(),
          AnalyticsTableColumn.builder()
              .name("oucode")
              .dataType(VARCHAR_50)
              .selectExpression("ou.code")
              .build(),
          AnalyticsTableColumn.builder()
              .name("oulevel")
              .dataType(INTEGER)
              .selectExpression("ous.level")
              .build());

  private static final List<AnalyticsTableColumn> FIXED_NON_GROUP_BY_COLS =
      List.of(
          AnalyticsTableColumn.builder()
              .name("createdbyusername")
              .dataType(VARCHAR_255)
              .selectExpression("te.createdbyuserinfo ->> 'username' as createdbyusername")
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdbyname")
              .dataType(VARCHAR_255)
              .selectExpression("te.createdbyuserinfo ->> 'firstName' as createdbyname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdbylastname")
              .dataType(VARCHAR_255)
              .selectExpression("te.createdbyuserinfo ->> 'surname' as createdbylastname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdbydisplayname")
              .dataType(VARCHAR_255)
              .selectExpression(getDisplayName("createdbyuserinfo", "te", "createdbydisplayname"))
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbyusername")
              .dataType(VARCHAR_255)
              .selectExpression("te.lastupdatedbyuserinfo ->> 'username' as lastupdatedbyusername")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbyname")
              .dataType(VARCHAR_255)
              .selectExpression("te.lastupdatedbyuserinfo ->> 'firstName' as lastupdatedbyname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbylastname")
              .dataType(VARCHAR_255)
              .selectExpression("te.lastupdatedbyuserinfo ->> 'surname' as lastupdatedbylastname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbydisplayname")
              .dataType(VARCHAR_255)
              .selectExpression(
                  getDisplayName("lastupdatedbyuserinfo", "te", "lastupdatedbydisplayname"))
              .skipIndex(Skip.SKIP)
              .build());

  public JdbcTrackedEntityAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingsProvider settingsProvider,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      DatabaseInfoProvider databaseInfoProvider,
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      TrackedEntityTypeService trackedEntityTypeService,
      TrackedEntityAttributeService trackedEntityAttributeService,
      AnalyticsTableSettings analyticsTableSettings,
      PeriodDataProvider periodDataProvider,
      SqlBuilder sqlBuilder) {
    super(
        idObjectManager,
        organisationUnitService,
        categoryService,
        settingsProvider,
        dataApprovalLevelService,
        resourceTableService,
        tableHookService,
        partitionManager,
        databaseInfoProvider,
        jdbcTemplate,
        analyticsTableSettings,
        periodDataProvider,
        sqlBuilder);
    this.trackedEntityAttributeService = trackedEntityAttributeService;
    this.trackedEntityTypeService = trackedEntityTypeService;
  }

  /**
   * Returns the {@link AnalyticsTableType} of analytics table which this manager handles.
   *
   * @return type of analytics table.
   */
  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return TRACKED_ENTITY_INSTANCE;
  }

  /**
   * Returns a {@link AnalyticsTable} with a list of yearly {@link AnalyticsTablePartition}.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @return the analytics table with partitions.
   */
  @Override
  @Transactional
  public List<AnalyticsTable> getAnalyticsTables(AnalyticsTableUpdateParams params) {
    Map<String, List<Program>> programsByTetUid = getProgramsByTetUid(params);

    params.addExtraParam("", PROGRAMS_BY_TET_KEY, programsByTetUid);

    Logged logged = analyticsTableSettings.getTableLogged();

    return trackedEntityTypeService.getAllTrackedEntityType().stream()
        .map(
            tet ->
                new AnalyticsTable(getAnalyticsTableType(), getColumns(params, tet), logged, tet))
        .toList();
  }

  private Map<String, List<Program>> getProgramsByTetUid(AnalyticsTableUpdateParams params) {
    List<Program> programs =
        params.isSkipPrograms()
            ? idObjectManager.getAllNoAcl(Program.class).stream()
                .filter(p -> !params.getSkipPrograms().contains(p.getUid()))
                .toList()
            : idObjectManager.getAllNoAcl(Program.class);

    return programs.stream()
        .filter(program -> Objects.nonNull(program.getTrackedEntityType()))
        .collect(groupingBy(o -> o.getTrackedEntityType().getUid()));
  }

  @SuppressWarnings("unchecked")
  private List<AnalyticsTableColumn> getColumns(
      AnalyticsTableUpdateParams params, TrackedEntityType trackedEntityType) {
    Map<String, List<Program>> programsByTetUid =
        (Map<String, List<Program>>) params.getExtraParam("", PROGRAMS_BY_TET_KEY);

    List<AnalyticsTableColumn> columns = new ArrayList<>(getFixedColumns());

    String enrolledInProgramExpression =
        """
        \s exists(select 1 from ${enrollment} en_0 \
        where en_0.trackedentityid = te.trackedentityid \
        and en_0.programid = ${programId})""";

    emptyIfNull(programsByTetUid.get(trackedEntityType.getUid()))
        .forEach(
            program ->
                columns.add(
                    AnalyticsTableColumn.builder()
                        .name(program.getUid())
                        .dataType(BOOLEAN)
                        .selectExpression(
                            replaceQualify(
                                enrolledInProgramExpression,
                                Map.of("programId", String.valueOf(program.getId()))))
                        .build()));

    List<TrackedEntityAttribute> trackedEntityAttributes =
        getAllTrackedEntityAttributes(trackedEntityType, programsByTetUid)
            .filter(tea -> !tea.isConfidentialBool())
            .toList();

    params.addExtraParam(
        trackedEntityType.getUid(), ALL_NON_CONFIDENTIAL_TET_ATTRIBUTES, trackedEntityAttributes);

    columns.addAll(
        trackedEntityAttributes.stream()
            .map(
                tea ->
                    AnalyticsTableColumn.builder()
                        .name(tea.getUid())
                        .dataType(getColumnType(tea.getValueType(), isSpatialSupport()))
                        .selectExpression(
                            castBasedOnType(tea.getValueType(), quote(tea.getUid()) + ".value"))
                        .build())
            .toList());

    columns.addAll(getOrganisationUnitGroupSetColumns());
    if (sqlBuilder.supportsDeclarativePartitioning()) {
      columns.add(getPartitionColumn());
    }

    return columns;
  }

  /**
   * Returns all {@link TrackedEntityAttribute} for the given {@link TrackedEntityType}.
   *
   * @param trackedEntityType the {@link TrackedEntityType} to get attributes for.
   * @param programsByTetUid the programs by TrackedEntityType UID.
   * @return a Stream of {@link TrackedEntityAttribute}.
   */
  private Stream<TrackedEntityAttribute> getAllTrackedEntityAttributes(
      TrackedEntityType trackedEntityType, Map<String, List<Program>> programsByTetUid) {

    // Given TET has program(s) defined.
    if (programsByTetUid.containsKey(trackedEntityType.getUid())) {

      // Programs defined for TET -> get attr from program and TET.
      return getAllTrackedEntityAttributesByPrograms(
          trackedEntityType, programsByTetUid.get(trackedEntityType.getUid()));
    }

    // No programs defined for TET -> get only attributes from TET.
    return getAllTrackedEntityAttributesByEntityType(trackedEntityType);
  }

  /**
   * Returns the select clause, potentially with a cast statement, based on the given value type.
   * (this method is an adapted version of {@link
   * JdbcEventAnalyticsTableManager#getSelectClause(ValueType, String)})
   *
   * @param valueType the value type to represent as database column type.
   */
  private String castBasedOnType(ValueType valueType, String columnName) {
    if (valueType.isDecimal()) {
      return replace(" cast(${columnName} as double precision)", Map.of("columnName", columnName));
    }
    if (valueType.isInteger()) {
      return replace(" cast(${columnName} as bigint)", Map.of("columnName", columnName));
    }
    if (valueType.isBoolean()) {
      return replace(
          " case when ${columnName} = 'true' then 1 when ${columnName} = 'false' then 0 end ",
          Map.of("columnName", columnName));
    }
    if (valueType.isDate()) {
      return replace(" cast(${columnName} as timestamp)", Map.of("columnName", columnName));
    }
    if (valueType.isGeo() && isSpatialSupport()) {
      return replace(
          """
          \s ST_GeomFromGeoJSON('{"type":"Point", "coordinates":' || (${columnName}) || ',
          "crs":{"type":"name", "properties":{"name":"EPSG:4326"}}}')""",
          Map.of("columnName", columnName));
    }
    return columnName;
  }

  /**
   * Returns all {@link TrackedEntityAttribute} for the given {@link TrackedEntityType} and
   * programs.
   *
   * @param trackedEntityType the {@link TrackedEntityType} to get attributes for.
   * @param programs the programs to get attributes for.
   * @return a Stream of {@link TrackedEntityAttribute}.
   */
  private Stream<TrackedEntityAttribute> getAllTrackedEntityAttributesByPrograms(
      TrackedEntityType trackedEntityType, List<Program> programs) {
    return Stream.concat(
            /* all attributes of programs */
            trackedEntityAttributeService.getProgramTrackedEntityAttributes(programs).stream(),
            /* all attributes of the trackedEntityType */
            getAllTrackedEntityAttributesByEntityType(trackedEntityType))
        .distinct();
  }

  private Stream<TrackedEntityAttribute> getAllTrackedEntityAttributesByEntityType(
      TrackedEntityType trackedEntityType) {
    return emptyIfNull(trackedEntityType.getTrackedEntityAttributes()).stream();
  }

  /**
   * Returns a list of non-dynamic {@link AnalyticsTableColumn}.
   *
   * @return a List of {@link AnalyticsTableColumn}.
   */
  private List<AnalyticsTableColumn> getFixedColumns() {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(FIXED_GROUP_BY_COLS);
    columns.addAll(getOrganisationUnitLevelColumns());
    columns.add(getOrganisationUnitNameHierarchyColumn());
    columns.addAll(FIXED_NON_GROUP_BY_COLS);

    return columns;
  }

  @Override
  protected List<String> getPartitionChecks(Integer year, Date endDate) {
    return List.of();
  }

  /**
   * Populates the given analytics table.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param partition the {@link AnalyticsTablePartition} to populate.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void populateTable(AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    String tableName = partition.getName();

    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();

    StringBuilder sql = new StringBuilder("insert into " + tableName + " (");

    for (AnalyticsTableColumn col : columns) {
      sql.append(quote(col.getName()) + ",");
    }

    removeLastComma(sql).append(") select ");

    for (AnalyticsTableColumn col : columns) {
      sql.append(col.getSelectExpression() + ",");
    }

    TrackedEntityType trackedEntityType = partition.getMasterTable().getTrackedEntityType();

    removeLastComma(sql)
        .append(
            replaceQualify(
                """
                \s from ${trackedentity} te \
                left join ${organisationunit} ou on te.organisationunitid=ou.organisationunitid \
                left join analytics_rs_orgunitstructure ous on ous.organisationunitid=ou.organisationunitid \
                left join analytics_rs_organisationunitgroupsetstructure ougs on te.organisationunitid=ougs.organisationunitid \
                and (cast(${trackedEntityCreatedMonth} as date)=ougs.startdate \
                or ougs.startdate is null)""",
                Map.of("trackedEntityCreatedMonth", sqlBuilder.dateTrunc("month", "te.created"))));

    ((List<TrackedEntityAttribute>)
            params.getExtraParam(trackedEntityType.getUid(), ALL_NON_CONFIDENTIAL_TET_ATTRIBUTES))
        .forEach(
            tea ->
                sql.append(
                    replaceQualify(
                        """
                    \s left join ${trackedentityattributevalue} "${teaUid}" on "${teaUid}".trackedentityid=te.trackedentityid \
                    and "${teaUid}".trackedentityattributeid = ${teaId}""",
                        Map.of(
                            "teaUid", tea.getUid(),
                            "teaId", String.valueOf(tea.getId())))));
    sql.append(
        replace(
            """
            \s where te.trackedentitytypeid = ${tetId} \
            and te.lastupdated < '${startTime}' \
            and exists (select 1 from enrollment en \
            where en.trackedentityid = te.trackedentityid \
            and exists (select 1 from event ev \
            where ev.enrollmentid = en.enrollmentid \
            and ev.status in (${statuses}) \
            and ev.deleted = false)) \
            and te.created is not null \
            and te.deleted = false""",
            Map.of(
                "tetId", String.valueOf(trackedEntityType.getId()),
                "startTime", toLongDate(params.getStartTime()),
                "statuses", join(",", EXPORTABLE_EVENT_STATUSES))));

    invokeTimeAndLog(sql.toString(), "Populating table: '{}'", tableName);
  }
}
