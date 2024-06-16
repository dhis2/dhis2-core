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
import org.apache.commons.collections4.CollectionUtils;
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
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.analytics.TeiAnalyticsTableManager")
public class JdbcTeiAnalyticsTableManager extends AbstractJdbcTableManager {
  private static final String PROGRAMS_BY_TET_KEY = "programsByTetUid";

  private static final String ALL_TET_ATTRIBUTES = "allTetAttributes";

  private final TrackedEntityTypeService trackedEntityTypeService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private static final List<AnalyticsTableColumn> FIXED_GROUP_BY_COLS =
      List.of(
          AnalyticsTableColumn.builder()
              .name("trackedentityinstanceuid")
              .dataType(CHARACTER_11)
              .nullable(NOT_NULL)
              .selectExpression("tei.uid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("trackedentityid")
              .dataType(INTEGER)
              .nullable(NOT_NULL)
              .selectExpression("tei.trackedentityid")
              .build(),
          AnalyticsTableColumn.builder()
              .name("created")
              .dataType(TIMESTAMP)
              .selectExpression("tei.created")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdated")
              .dataType(TIMESTAMP)
              .selectExpression("tei.lastupdated")
              .build(),
          AnalyticsTableColumn.builder()
              .name("inactive")
              .dataType(BOOLEAN)
              .selectExpression("tei.inactive")
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdatclient")
              .dataType(TIMESTAMP)
              .selectExpression("tei.createdatclient")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedatclient")
              .dataType(TIMESTAMP)
              .selectExpression("tei.lastupdatedatclient")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastsynchronized")
              .dataType(TIMESTAMP)
              .selectExpression("tei.lastsynchronized")
              .build(),
          AnalyticsTableColumn.builder()
              .name("geometry")
              .dataType(GEOMETRY)
              .selectExpression("tei.geometry")
              .indexType(IndexType.GIST)
              .build(),
          AnalyticsTableColumn.builder()
              .name("longitude")
              .dataType(DOUBLE)
              .selectExpression(
                  "case when 'POINT' = GeometryType(tei.geometry) then ST_X(tei.geometry) else null end")
              .build(),
          AnalyticsTableColumn.builder()
              .name("latitude")
              .dataType(DOUBLE)
              .selectExpression(
                  "case when 'POINT' = GeometryType(tei.geometry) then ST_Y(tei.geometry) else null end")
              .build(),
          AnalyticsTableColumn.builder()
              .name("featuretype")
              .dataType(VARCHAR_255)
              .selectExpression("tei.featuretype")
              .build(),
          AnalyticsTableColumn.builder()
              .name("coordinates")
              .dataType(TEXT)
              .selectExpression("tei.coordinates")
              .build(),
          AnalyticsTableColumn.builder()
              .name("storedby")
              .dataType(VARCHAR_255)
              .selectExpression("tei.storedby")
              .build(),
          AnalyticsTableColumn.builder()
              .name("potentialduplicate")
              .dataType(BOOLEAN)
              .selectExpression("tei.potentialduplicate")
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
              .selectExpression("tei.createdbyuserinfo ->> 'username' as createdbyusername")
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdbyname")
              .dataType(VARCHAR_255)
              .selectExpression("tei.createdbyuserinfo ->> 'firstName' as createdbyname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdbylastname")
              .dataType(VARCHAR_255)
              .selectExpression("tei.createdbyuserinfo ->> 'surname' as createdbylastname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("createdbydisplayname")
              .dataType(VARCHAR_255)
              .selectExpression(getDisplayName("createdbyuserinfo", "tei", "createdbydisplayname"))
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbyusername")
              .dataType(VARCHAR_255)
              .selectExpression("tei.lastupdatedbyuserinfo ->> 'username' as lastupdatedbyusername")
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbyname")
              .dataType(VARCHAR_255)
              .selectExpression("tei.lastupdatedbyuserinfo ->> 'firstName' as lastupdatedbyname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbylastname")
              .dataType(VARCHAR_255)
              .selectExpression("tei.lastupdatedbyuserinfo ->> 'surname' as lastupdatedbylastname")
              .skipIndex(Skip.SKIP)
              .build(),
          AnalyticsTableColumn.builder()
              .name("lastupdatedbydisplayname")
              .dataType(VARCHAR_255)
              .selectExpression(
                  getDisplayName("lastupdatedbyuserinfo", "tei", "lastupdatedbydisplayname"))
              .skipIndex(Skip.SKIP)
              .build());

  public JdbcTeiAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingManager systemSettingManager,
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
        systemSettingManager,
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
      AnalyticsTableUpdateParams params, TrackedEntityType tet) {
    Map<String, List<Program>> programsByTetUid =
        (Map<String, List<Program>>) params.getExtraParam("", PROGRAMS_BY_TET_KEY);

    List<AnalyticsTableColumn> columns = new ArrayList<>(getFixedColumns());

    String enrolledInProgramExpression =
        """
        \s exists(select 1 from enrollment pi_0 \
        where pi_0.trackedentityid = tei.trackedentityid \
        and pi_0.programid = ${programId})""";

    CollectionUtils.emptyIfNull(programsByTetUid.get(tet.getUid()))
        .forEach(
            program ->
                columns.add(
                    AnalyticsTableColumn.builder()
                        .name(program.getUid())
                        .dataType(BOOLEAN)
                        .selectExpression(
                            replace(
                                enrolledInProgramExpression,
                                Map.of("programId", String.valueOf(program.getId()))))
                        .build()));

    List<TrackedEntityAttribute> trackedEntityAttributes =
        programsByTetUid.containsKey(tet.getUid())
            ?
            // programs defined for TET -> get attr from program and TET
            getAllTrackedEntityAttributes(tet, programsByTetUid.get(tet.getUid()))
            :
            // no programs defined for TET -> get only attributes from TET
            getAllTrackedEntityAttributes(tet).toList();

    params.addExtraParam(tet.getUid(), ALL_TET_ATTRIBUTES, trackedEntityAttributes);

    columns.addAll(
        trackedEntityAttributes.stream()
            .map(
                tea ->
                    AnalyticsTableColumn.builder()
                        .name(tea.getUid())
                        .dataType(getColumnType(tea.getValueType(), isSpatialSupport()))
                        .selectExpression(
                            castBasedOnType(tea.getValueType(), "\"" + tea.getUid() + "\".value"))
                        .build())
            .toList());

    columns.addAll(getOrganisationUnitGroupSetColumns());

    return columns;
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

  private List<TrackedEntityAttribute> getAllTrackedEntityAttributes(
      TrackedEntityType trackedEntityType, List<Program> programs) {
    return Stream.concat(
            /* all attributes of programs */
            trackedEntityAttributeService.getProgramTrackedEntityAttributes(programs).stream(),
            /* all attributes of the trackedEntityType */
            getAllTrackedEntityAttributes(trackedEntityType))
        .distinct()
        .toList();
  }

  private Stream<TrackedEntityAttribute> getAllTrackedEntityAttributes(
      TrackedEntityType trackedEntityType) {
    return CollectionUtils.emptyIfNull(trackedEntityType.getTrackedEntityAttributes()).stream();
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
            replace(
                """
                \s from trackedentity tei \
                left join organisationunit ou on tei.organisationunitid = ou.organisationunitid \
                left join analytics_rs_orgunitstructure ous on ous.organisationunitid = ou.organisationunitid \
                left join analytics_rs_organisationunitgroupsetstructure ougs on tei.organisationunitid = ougs.organisationunitid \
                and (cast(${teiCreatedMonth} as date) = ougs.startdate \
                or ougs.startdate is null)""",
                Map.of("teiCreatedMonth", sqlBuilder.dateTrunc("month", "tei.created"))));

    ((List<TrackedEntityAttribute>)
            params.getExtraParam(trackedEntityType.getUid(), ALL_TET_ATTRIBUTES))
        .forEach(
            tea ->
                sql.append(
                    replace(
                        """
                    \s left join trackedentityattributevalue "${teaUid}" on "${teaUid}".trackedentityid = tei.trackedentityid \
                    and "${teaUid}".trackedentityattributeid = ${teaId}""",
                        Map.of(
                            "teaUid", tea.getUid(),
                            "teaId", String.valueOf(tea.getId())))));
    sql.append(
        replace(
            """
            \s where tei.trackedentitytypeid = ${tetId} \
            and tei.lastupdated < '${startTime}' \
            and exists (select 1 from enrollment pi \
            where pi.trackedentityid = tei.trackedentityid \
            and exists (select 1 from event psi \
            where psi.enrollmentid = pi.enrollmentid \
            and psi.status in (${statuses}) \
            and psi.deleted = false)) \
            and tei.created is not null \
            and tei.deleted = false""",
            Map.of(
                "tetId", String.valueOf(trackedEntityType.getId()),
                "startTime", toLongDate(params.getStartTime()),
                "statuses", join(",", EXPORTABLE_EVENT_STATUSES))));

    invokeTimeAndLog(sql.toString(), "Populating table: '{}'", tableName);
  }
}
