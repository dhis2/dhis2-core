/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.dxf2.metadata.objectbundle.validation.DashboardCheck.LAYOUT_COLUMN_LIMIT;
import static org.hisp.dhis.feedback.ErrorCode.E4070;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.importexport.ImportStrategy.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardCheckTest extends DhisConvenienceTest {
  @InjectMocks private DashboardCheck dashboardCheck;

  @ParameterizedTest
  @MethodSource("columnsAndStrategyCombos")
  void testLayoutColumnLimitNoErrors(int columnCount, ImportStrategy strategy) {
    Dashboard dashboard = createDashboard('d');
    dashboard.setLayout(createLayoutWithColumns(columnCount));

    TypeReport check =
        dashboardCheck.check(
            null, Dashboard.class, emptyList(), List.of(dashboard), strategy, null);

    assertEquals(0, check.getErrorReportsCount());
    assertEquals(0, check.getStats().getIgnored());
    assertEquals(0, check.getStats().getCreated());
    assertEquals(0, check.getStats().getUpdated());
    assertEquals(0, check.getStats().getTotal());
  }

  @ParameterizedTest
  @EnumSource(
      value = ImportStrategy.class,
      names = {"CREATE", "CREATE_AND_UPDATE", "NEW", "NEW_AND_UPDATES"})
  void testLayoutColumnLimitWithErrors_CreateFlow(ImportStrategy strategy) {
    Dashboard dashboard = createDashboard('d');
    String uid = dashboard.getUid();
    dashboard.setLayout(createLayoutWithColumns(61));

    TypeReport check =
        dashboardCheck.check(null, Dashboard.class, List.of(), List.of(dashboard), strategy, null);

    ErrorReport errorReport = check.getFirstObjectReport().getErrorReports().get(0);

    assertEquals(1, check.getErrorReportsCount());
    assertEquals(1, check.getStats().getIgnored());
    assertEquals(0, check.getStats().getCreated());
    assertEquals(0, check.getStats().getUpdated());
    assertEquals(1, check.getStats().getTotal());
    assertEquals(E4070, errorReport.getErrorCode());
    assertEquals(
        "Dashboard `" + uid + "` has a layout with more than 60 columns. `61` columns found",
        errorReport.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = ImportStrategy.class,
      names = {"UPDATE", "UPDATES"})
  void testLayoutColumnLimitWithErrors_UpdateFlow(ImportStrategy strategy) {
    Dashboard dashboard = createDashboard('d');
    String uid = dashboard.getUid();
    dashboard.setLayout(createLayoutWithColumns(61));

    TypeReport check =
        dashboardCheck.check(null, Dashboard.class, List.of(dashboard), List.of(), strategy, null);

    ErrorReport errorReport = check.getFirstObjectReport().getErrorReports().get(0);

    assertEquals(1, check.getErrorReportsCount());
    assertEquals(1, check.getStats().getIgnored());
    assertEquals(0, check.getStats().getCreated());
    assertEquals(0, check.getStats().getUpdated());
    assertEquals(1, check.getStats().getTotal());
    assertEquals(E4070, errorReport.getErrorCode());
    assertEquals(
        "Dashboard `" + uid + "` has a layout with more than 60 columns. `61` columns found",
        errorReport.getMessage());
  }

  private static Stream<Arguments> columnsAndStrategyCombos() {
    return Stream.of(
        arguments(0, CREATE),
        arguments(1, CREATE),
        arguments(59, CREATE),
        arguments(LAYOUT_COLUMN_LIMIT, CREATE),
        arguments(0, CREATE_AND_UPDATE),
        arguments(1, CREATE_AND_UPDATE),
        arguments(59, CREATE_AND_UPDATE),
        arguments(LAYOUT_COLUMN_LIMIT, CREATE_AND_UPDATE),
        arguments(0, UPDATE),
        arguments(1, UPDATE),
        arguments(59, UPDATE),
        arguments(LAYOUT_COLUMN_LIMIT, UPDATE));
  }
}
