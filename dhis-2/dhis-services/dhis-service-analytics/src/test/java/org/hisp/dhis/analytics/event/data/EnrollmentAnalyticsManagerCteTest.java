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
package org.hisp.dhis.analytics.event.data;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.data.programindicator.DefaultProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.external.conf.DefaultDhisConfigurationProvider;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EnrollmentAnalyticsManagerCteTest extends EventAnalyticsTest {
  private JdbcEnrollmentAnalyticsManager subject;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private ExecutionPlanStore executionPlanStore;

  @Mock private SqlRowSet rowSet;

  @Mock private SqlRowSetMetaData rowSetMetaData;

  @Mock private ProgramIndicatorService programIndicatorService;

  @Mock private DataElementService dataElementService;

  @Spy private PostgreSqlAnalyticsSqlBuilder sqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  @Mock private SystemSettingsService systemSettingsService;

  @Mock private OrganisationUnitResolver organisationUnitResolver;

  @Mock private PiDisagInfoInitializer piDisagInfoInitializer;

  @Mock private PiDisagQueryGenerator piDisagQueryGenerator;

  @Spy
  private EnrollmentTimeFieldSqlRenderer enrollmentTimeFieldSqlRenderer =
      new EnrollmentTimeFieldSqlRenderer(sqlBuilder);

  @Spy private SystemSettings systemSettings;

  @Mock private DefaultDhisConfigurationProvider config;

  @Captor private ArgumentCaptor<String> sql;

  @BeforeEach
  public void setUp() {
    when(jdbcTemplate.queryForRowSet(anyString())).thenReturn(this.rowSet);
    when(systemSettingsService.getCurrentSettings()).thenReturn(systemSettings);
    when(systemSettings.getUseExperimentalAnalyticsQueryEngine()).thenReturn(true);
    when(config.getPropertyOrDefault(ANALYTICS_DATABASE, "")).thenReturn("postgresql");
    when(rowSet.getMetaData()).thenReturn(rowSetMetaData);
    DefaultProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder =
        new DefaultProgramIndicatorSubqueryBuilder(
            programIndicatorService,
            systemSettingsService,
            new PostgreSqlBuilder(),
            dataElementService);
    ColumnMapper columnMapper = new ColumnMapper(sqlBuilder);

    subject =
        new JdbcEnrollmentAnalyticsManager(
            jdbcTemplate,
            programIndicatorService,
            programIndicatorSubqueryBuilder,
            piDisagInfoInitializer,
            piDisagQueryGenerator,
            enrollmentTimeFieldSqlRenderer,
            executionPlanStore,
            systemSettingsService,
            config,
            sqlBuilder,
            organisationUnitResolver,
            columnMapper);
  }

  @Test
  void verifyGetEnrollmentsWithoutMissingValueAndNumericValuesInFilter() {
    String cte =
        noEof(
                """
                ( select enrollment,
                         "fWIAEtYVEGk" as value,
                         row_number() over (
                            partition by enrollment order by occurreddate desc, created desc ) as rn
                  from analytics_event_%s
                  where eventstatus != 'SCHEDULE' and ps = '%s' )
                """)
            .formatted(programA.getUid(), programStage.getUid());

    String numericValues = String.join(OPTION_SEP, "10", "11", "12");
    String inClause = " in (" + String.join(",", numericValues.split(OPTION_SEP)) + ")";

    Collection<Consumer<String>> assertions =
        Arrays.asList(
            sql -> assertThat(sql, containsString(cte)),
            sql -> {
              // check that the IN clause is in the root query where condition
              var whereToOrderBy =
                  sql.toLowerCase()
                      .substring(
                          sql.toLowerCase().lastIndexOf("where"),
                          sql.toLowerCase().lastIndexOf("limit"));
              assertThat(whereToOrderBy, containsString(inClause));
            },
            sql -> assertThat(sql, containsString(inClause)));

    testIt(IN, numericValues, assertions);
  }

  @Override
  String getTableName() {
    return "analytics_enrollment";
  }

  private void testIt(
      QueryOperator operator, String filter, Collection<Consumer<String>> assertions) {
    subject.getEnrollments(
        createRequestParamsWithFilter(programStage, ValueType.INTEGER, operator, filter),
        new ListGrid(),
        10000);
    verify(jdbcTemplate).queryForRowSet(sql.capture());

    assertions.forEach(consumer -> consumer.accept(sql.getValue()));
  }

  private String noEof(String sql) {
    return sql.replaceAll("\\s+", " ").trim();
  }
}
