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
package org.hisp.dhis.datavalue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.junit.jupiter.api.Test;

class DataExportParamsTest {

  @Test
  void testHasPeriodFilters_NoFiltersFalse() {
    DataExportParams params = DataExportParams.builder().build();

    assertFalse(params.hasPeriodFilters());
  }

  @Test
  void testHasPeriodFilters_LastUpdatedAloneIsNotAPeriodFilter() {
    // lastUpdated/lastUpdatedDuration do not filter by the period table, so they must not
    // trigger the pe_ids CTE/JOIN the query builder erases based on hasPeriodFilters().
    DataExportParams params =
        DataExportParams.builder()
            .lastUpdated(new Date())
            .lastUpdatedDuration(Duration.ofDays(10000))
            .build();

    assertFalse(params.hasPeriodFilters());
  }

  @Test
  void testHasPeriodFilters_PeriodsTrue() {
    DataExportParams params =
        DataExportParams.builder().periods(List.of(Period.of("202201"))).build();

    assertTrue(params.hasPeriodFilters());
  }

  @Test
  void testHasPeriodFilters_PeriodTypesTrue() {
    DataExportParams params =
        DataExportParams.builder().periodTypes(Set.of(new MonthlyPeriodType())).build();

    assertTrue(params.hasPeriodFilters());
  }

  @Test
  void testHasPeriodFilters_StartAndEndDateTrue() {
    DataExportParams params =
        DataExportParams.builder().startDate(new Date(0)).endDate(new Date(1)).build();

    assertTrue(params.hasPeriodFilters());
  }

  @Test
  void testHasPeriodFilters_StartDateAloneFalse() {
    DataExportParams params = DataExportParams.builder().startDate(new Date(0)).build();

    assertFalse(params.hasPeriodFilters());
  }

  @Test
  void testHasPeriodFilters_IncludedDateTrue() {
    DataExportParams params = DataExportParams.builder().includedDate(new Date()).build();

    assertTrue(params.hasPeriodFilters());
  }

  @Test
  void testHasLastUpdatedFilters_NoFiltersFalse() {
    DataExportParams params = DataExportParams.builder().build();

    assertFalse(params.hasLastUpdatedFilters());
  }

  @Test
  void testHasLastUpdatedFilters_LastUpdatedDurationTrue() {
    DataExportParams params =
        DataExportParams.builder().lastUpdatedDuration(Duration.ofDays(10000)).build();

    assertTrue(params.hasLastUpdatedFilters());
  }

  @Test
  void testHasLastUpdatedFilters_LastUpdatedTrue() {
    DataExportParams params = DataExportParams.builder().lastUpdated(new Date()).build();

    assertTrue(params.hasLastUpdatedFilters());
  }

  @Test
  void testHasDataElementFilters_NoFiltersFalse() {
    DataExportParams params = DataExportParams.builder().build();

    assertFalse(params.hasDataElementFilters());
  }

  @Test
  void testHasDataElementFilters_DataSetsTrue() {
    DataExportParams params = DataExportParams.builder().dataSets(List.of(UID.generate())).build();

    assertTrue(params.hasDataElementFilters());
  }

  @Test
  void testHasDataElementFilters_DataElementsTrue() {
    DataExportParams params =
        DataExportParams.builder().dataElements(List.of(UID.generate())).build();

    assertTrue(params.hasDataElementFilters());
  }

  @Test
  void testHasDataElementFilters_DataElementGroupsTrue() {
    DataExportParams params =
        DataExportParams.builder().dataElementGroups(List.of(UID.generate())).build();

    assertTrue(params.hasDataElementFilters());
  }

  @Test
  void testHasOrgUnitFilters_NoFiltersFalse() {
    DataExportParams params = DataExportParams.builder().build();

    assertFalse(params.hasOrgUnitFilters());
  }

  @Test
  void testHasOrgUnitFilters_OrganisationUnitsTrue() {
    DataExportParams params =
        DataExportParams.builder().organisationUnits(List.of(UID.generate())).build();

    assertTrue(params.hasOrgUnitFilters());
  }

  @Test
  void testHasOrgUnitFilters_OrganisationUnitGroupsTrue() {
    DataExportParams params =
        DataExportParams.builder().organisationUnitGroups(List.of(UID.generate())).build();

    assertTrue(params.hasOrgUnitFilters());
  }

  @Test
  void testIsExactOrgUnitsFilter_OrgUnitsOnlyTrue() {
    DataExportParams params =
        DataExportParams.builder().organisationUnits(List.of(UID.generate())).build();

    assertTrue(params.isExactOrgUnitsFilter());
  }

  @Test
  void testIsExactOrgUnitsFilter_NoOrgUnitsFalse() {
    DataExportParams params = DataExportParams.builder().build();

    assertFalse(params.isExactOrgUnitsFilter());
  }

  @Test
  void testIsExactOrgUnitsFilter_IncludeDescendantsFalse() {
    DataExportParams params =
        DataExportParams.builder()
            .organisationUnits(List.of(UID.generate()))
            .includeDescendants(true)
            .build();

    assertFalse(params.isExactOrgUnitsFilter());
  }

  @Test
  void testIsExactOrgUnitsFilter_OrgUnitGroupsAlsoSetFalse() {
    DataExportParams params =
        DataExportParams.builder()
            .organisationUnits(List.of(UID.generate()))
            .organisationUnitGroups(List.of(UID.generate()))
            .build();

    assertFalse(params.isExactOrgUnitsFilter());
  }

  @Test
  void testIsPeriodOverSpecified_PeriodsAndStartEndDateTrue() {
    DataExportParams params =
        DataExportParams.builder()
            .periods(List.of(Period.of("202201")))
            .startDate(new Date(0))
            .endDate(new Date(1))
            .build();

    assertTrue(params.isPeriodOverSpecified());
  }

  @Test
  void testIsPeriodOverSpecified_PeriodsOnlyFalse() {
    DataExportParams params =
        DataExportParams.builder().periods(List.of(Period.of("202201"))).build();

    assertFalse(params.isPeriodOverSpecified());
  }

  @Test
  void testIsPeriodOverSpecified_StartEndDateOnlyFalse() {
    DataExportParams params =
        DataExportParams.builder().startDate(new Date(0)).endDate(new Date(1)).build();

    assertFalse(params.isPeriodOverSpecified());
  }

  @Test
  void testIsDateRangeOutOfBounds_StartAfterEndTrue() {
    DataExportParams params =
        DataExportParams.builder().startDate(new Date(1)).endDate(new Date(0)).build();

    assertTrue(params.isDateRangeOutOfBounds());
  }

  @Test
  void testIsDateRangeOutOfBounds_StartBeforeEndFalse() {
    DataExportParams params =
        DataExportParams.builder().startDate(new Date(0)).endDate(new Date(1)).build();

    assertFalse(params.isDateRangeOutOfBounds());
  }

  @Test
  void testIsDateRangeOutOfBounds_StartDateOnlyFalse() {
    DataExportParams params = DataExportParams.builder().startDate(new Date(1)).build();

    assertFalse(params.isDateRangeOutOfBounds());
  }

  @Test
  void testIsLimitOutOfBounds_NegativeTrue() {
    DataExportParams params = DataExportParams.builder().limit(-1).build();

    assertTrue(params.isLimitOutOfBounds());
  }

  @Test
  void testIsLimitOutOfBounds_ZeroFalse() {
    DataExportParams params = DataExportParams.builder().limit(0).build();

    assertFalse(params.isLimitOutOfBounds());
  }

  @Test
  void testIsLimitOutOfBounds_NullFalse() {
    DataExportParams params = DataExportParams.builder().build();

    assertFalse(params.isLimitOutOfBounds());
  }

  @Test
  void testIsOrgUnitGroupsOverSpecified_GroupsAndDescendantsTrue() {
    DataExportParams params =
        DataExportParams.builder()
            .organisationUnitGroups(List.of(UID.generate()))
            .includeDescendants(true)
            .build();

    assertTrue(params.isOrgUnitGroupsOverSpecified());
  }

  @Test
  void testIsOrgUnitGroupsOverSpecified_GroupsWithoutDescendantsFalse() {
    DataExportParams params =
        DataExportParams.builder().organisationUnitGroups(List.of(UID.generate())).build();

    assertFalse(params.isOrgUnitGroupsOverSpecified());
  }

  @Test
  void testIsOrgUnitGroupsOverSpecified_DescendantsWithoutGroupsFalse() {
    DataExportParams params = DataExportParams.builder().includeDescendants(true).build();

    assertFalse(params.isOrgUnitGroupsOverSpecified());
  }
}
