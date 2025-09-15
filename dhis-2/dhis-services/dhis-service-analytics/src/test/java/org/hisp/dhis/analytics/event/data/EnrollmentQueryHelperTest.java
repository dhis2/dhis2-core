/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.period.RelativePeriodEnum.LAST_3_DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link EnrollmentQueryHelper}. */
class EnrollmentQueryHelperTest {

  @Test
  void testGetHeaderColumnsSamePrefixedDimension() {
    List<GridHeader> headers =
        List.of(
            new GridHeader("name0"),
            new GridHeader("name1"),
            new GridHeader("dim.name2"),
            new GridHeader("pe"),
            new GridHeader("value"),
            new GridHeader("ou"));

    String sql = "select name0, name1, dim.name2, pe, value, ou from table";

    Set<String> headerColumns = EnrollmentQueryHelper.getHeaderColumns(headers, sql);

    String[] columns = headerColumns.toArray(String[]::new);
    assertEquals(3, columns.length);
    assertEquals("t1.\"name0\"", columns[0]);
    assertEquals("t1.\"name1\"", columns[1]);
    assertEquals("t1.\"dim.name2\"", columns[2]);
  }

  @Test
  void testGetHeaderColumnsDifferentPrefixedDimension() {
    List<GridHeader> headers =
        List.of(
            new GridHeader("name0"),
            new GridHeader("name1"),
            new GridHeader("dim.name2"),
            new GridHeader("pe"),
            new GridHeader("value"),
            new GridHeader("ou"));

    String sql = "select name0, name1, ax.name2, pe, value, ou from table";

    Set<String> headerColumns = EnrollmentQueryHelper.getHeaderColumns(headers, sql);

    String[] columns = headerColumns.toArray(String[]::new);
    assertEquals(3, columns.length);
    assertEquals("t1.\"name0\"", columns[0]);
    assertEquals("t1.\"name1\"", columns[1]);
    assertEquals("t1.\"name2\"", columns[2]);
  }

  @Test
  void testGetOrgUnitLevelColumnsOuMode() {
    OrganisationUnit organisationUnit = new OrganisationUnit("OrgTest");
    organisationUnit.setPath("/Level1/OrgTest");

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withOrganisationUnitMode(CAPTURE)
            .addDimension(
                new BaseDimensionalObject(
                    ORGUNIT_DIM_ID, ORGANISATION_UNIT, List.of(organisationUnit)))
            .build();

    Set<String> orgUnitColumns = EnrollmentQueryHelper.getOrgUnitLevelColumns(params);

    String[] columns = orgUnitColumns.toArray(String[]::new);
    assertEquals(1, columns.length);
    assertEquals("uidlevel2", columns[0]);
  }

  @Test
  void testGetOrgUnitLevelColumnsOuModeSelected() {
    OrganisationUnit organisationUnit = new OrganisationUnit("/Level1/Level2");

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withOrganisationUnitMode(SELECTED)
            .addDimension(
                new BaseDimensionalObject(
                    ORGUNIT_DIM_ID, ORGANISATION_UNIT, List.of(organisationUnit)))
            .build();

    Set<String> orgUnitColumns = EnrollmentQueryHelper.getOrgUnitLevelColumns(params);

    assertEquals(0, orgUnitColumns.size());
  }

  @Test
  void testGetOrgUnitLevelColumnsOuModeChildren() {
    OrganisationUnit organisationUnit = new OrganisationUnit("/Level1/Level2");

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withOrganisationUnitMode(CHILDREN)
            .addDimension(
                new BaseDimensionalObject(
                    ORGUNIT_DIM_ID, ORGANISATION_UNIT, List.of(organisationUnit)))
            .build();

    Set<String> orgUnitColumns = EnrollmentQueryHelper.getOrgUnitLevelColumns(params);

    assertEquals(0, orgUnitColumns.size());
  }

  @Test
  void testGetPeriodColumns() {
    Period period = new Period(LAST_3_DAYS);
    period.setPeriodType(PeriodType.getPeriodTypeFromIsoString("201101"));

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(new BaseDimensionalObject(PERIOD_DIM_ID, PERIOD, List.of(period)))
            .build();

    Set<String> periodColumns = EnrollmentQueryHelper.getPeriodColumns(params);

    String[] columns = periodColumns.toArray(String[]::new);
    assertEquals(1, columns.length);
    assertEquals("t1.Monthly", columns[0]);
  }

  @Test
  void testGetPeriodColumnsNoPeriods() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(new BaseDimensionalObject(ORGUNIT_DIM_ID, ORGANISATION_UNIT, List.of()))
            .build();

    Set<String> periodColumns = EnrollmentQueryHelper.getPeriodColumns(params);

    assertEquals(0, periodColumns.size());
  }
}
