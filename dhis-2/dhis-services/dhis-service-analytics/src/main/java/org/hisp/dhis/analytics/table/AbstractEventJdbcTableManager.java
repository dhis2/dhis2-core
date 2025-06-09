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

import static org.hisp.dhis.analytics.AnalyticsStringUtils.replaceQualify;
import static org.hisp.dhis.analytics.AnalyticsStringUtils.toCommaSeparated;

import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Markus Bekken
 */
public abstract class AbstractEventJdbcTableManager extends AbstractJdbcTableManager {
  protected final ColumnMapper columnMapper;

  public AbstractEventJdbcTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingsProvider settingsProvider,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      JdbcTemplate jdbcTemplate,
      AnalyticsTableSettings analyticsTableSettings,
      PeriodDataProvider periodDataProvider,
      ColumnMapper columnMapper,
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
        jdbcTemplate,
        analyticsTableSettings,
        periodDataProvider,
        sqlBuilder);
    this.columnMapper = columnMapper;
  }

  @Override
  public boolean validState() {
    return tableIsNotEmpty("event");
  }

  /**
   * Populates the given analytics table partition using the given columns and join statement.
   *
   * @param partition the {@link AnalyticsTablePartition}.
   * @param fromClause the SQL from clause.
   */
  protected void populateTableInternal(AnalyticsTablePartition partition, String fromClause) {
    String tableName = partition.getName();

    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();

    String sql = "insert into " + tableName + " (";
    sql += toCommaSeparated(columns, col -> quote(col.getName()));
    sql += ") select ";
    sql += toCommaSeparated(columns, AnalyticsTableColumn::getSelectExpression);
    sql += " " + fromClause;

    invokeTimeAndLog(sql, "Populating table: '{}'", tableName);
  }

  /**
   * Returns a list of columns based on the given attribute.
   *
   * @param attribute the {@link TrackedEntityAttribute}.
   * @return a list of {@link AnalyticsTableColumn}.
   */
  protected List<AnalyticsTableColumn> getColumnForAttribute(TrackedEntityAttribute attribute) {
    return columnMapper.getColumnsForAttribute(attribute);
  }

  /**
   * Returns a join clause for attribute value for every attribute of the given program.
   *
   * @param program the {@link Program}.
   * @return a join clause.
   */
  protected String getAttributeValueJoinClause(Program program) {
    String template =
        """
        left join trackedentityattributevalue as ${uid} \
        on en.trackedentityid=${uid}.trackedentityid \
        and ${uid}.trackedentityattributeid = ${id}\s""";

    return program.getNonConfidentialTrackedEntityAttributes().stream()
        .map(attribute -> replaceQualify(sqlBuilder, template, toVariableMap(attribute)))
        .collect(Collectors.joining());
  }
}
