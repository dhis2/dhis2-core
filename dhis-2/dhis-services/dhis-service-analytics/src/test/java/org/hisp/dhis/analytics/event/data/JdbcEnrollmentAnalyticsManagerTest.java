/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcEnrollmentAnalyticsManagerTest {

  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private ProgramIndicatorService programIndicatorService;
  @Mock private ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder;
  @Mock private PiDisagInfoInitializer piDisagInfoInitializer;
  @Mock private PiDisagQueryGenerator piDisagQueryGenerator;
  @Mock private EnrollmentTimeFieldSqlRenderer timeFieldSqlRenderer;
  @Mock private ExecutionPlanStore executionPlanStore;
  @Mock private SystemSettingsService settingsService;
  @Mock private DhisConfigurationProvider config;
  @Mock private AnalyticsSqlBuilder sqlBuilder;
  @Mock private OrganisationUnitResolver organisationUnitResolver;
  @Mock private ColumnMapper columnMapper;

  @InjectMocks private JdbcEnrollmentAnalyticsManager jdbcEnrollmentAnalyticsManager;

  @Test
  void testAddEventPrefix() {
    String result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("ST_Y(ax.geometry)");
    assertEquals("ST_Y(ax.geometry)", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("count() as value");
    assertEquals("count() as value", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("ax.value as \"A03MvHHogjR\"");
    assertEquals("ax.value as \"A03MvHHogjR\"", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("ev.value as \"A03MvHHogjR\"");
    assertEquals("ev.value as \"A03MvHHogjR\"", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("value as \"A03MvHHogjR\"");
    assertEquals("ax.value as \"A03MvHHogjR\"", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("\"A03MvHHogjR\"");
    assertEquals("ax.\"A03MvHHogjR\"", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("ev.\"A03MvHHogjR\"");
    assertEquals("ev.\"A03MvHHogjR\"", result);
  }
}
