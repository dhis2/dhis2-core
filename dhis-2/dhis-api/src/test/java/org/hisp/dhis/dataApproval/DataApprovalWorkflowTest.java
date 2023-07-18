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
package org.hisp.dhis.dataApproval;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.period.BiMonthlyPeriodType;
import org.hisp.dhis.period.BiWeeklyPeriodType;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialJulyPeriodType;
import org.hisp.dhis.period.FinancialNovemberPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.SixMonthlyAprilPeriodType;
import org.hisp.dhis.period.SixMonthlyNovemberPeriodType;
import org.hisp.dhis.period.SixMonthlyPeriodType;
import org.hisp.dhis.period.TwoYearlyPeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.WeeklySaturdayPeriodType;
import org.hisp.dhis.period.WeeklySundayPeriodType;
import org.hisp.dhis.period.WeeklyThursdayPeriodType;
import org.hisp.dhis.period.WeeklyWednesdayPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.junit.jupiter.api.Test;

/**
 * @author Jim Grace
 */
class DataApprovalWorkflowTest {

  @Test
  void testGetSortedLevels() {
    DataApprovalLevel level1 = new DataApprovalLevel("level1", 1, null);
    level1.setLevel(1);
    DataApprovalLevel level2 = new DataApprovalLevel("level2", 2, null);
    level2.setLevel(2);
    DataApprovalLevel level3 = new DataApprovalLevel("level3", 3, null);
    level3.setLevel(3);
    DataApprovalWorkflow workflow =
        new DataApprovalWorkflow(
            "test workflow", new DailyPeriodType(), newHashSet(level3, level2, level1));
    List<DataApprovalLevel> levels = workflow.getSortedLevels();
    assertEquals(1, levels.get(0).getLevel());
    assertEquals(2, levels.get(1).getLevel());
    assertEquals(3, levels.get(2).getLevel());
  }

  /**
   * Note that as part of the following test, we are checking to be sure that the longest extension
   * to the category option end date is returned from data sets that are successively added to the
   * workflow. The total extension length (period length times number of periods) is always
   * increasing as we add data sets to the workflow. The most recently-added data set always has the
   * longest total extension so far.
   */
  @Test
  void testGetWorkflowSqlCoEnddateExtension() {
    DataSet noPeriodsAfterCoEndDate =
        createDataSet("noPeriodsAfterCoEndDate", new MonthlyPeriodType(), 0);
    DataSet dailyDs = createDataSet("dailyDs", new DailyPeriodType(), 3);
    DataSet weeklyDs = createDataSet("weeklyDs", new WeeklyPeriodType(), 3);
    DataSet weeklyWedDs = createDataSet("weeklyWedDs", new WeeklyWednesdayPeriodType(), 4);
    DataSet weeklyThuDs = createDataSet("weeklyThuDs", new WeeklyThursdayPeriodType(), 5);
    DataSet weeklySatDs = createDataSet("weeklySatDs", new WeeklySaturdayPeriodType(), 6);
    DataSet weeklySunDs = createDataSet("weeklySunDs", new WeeklySundayPeriodType(), 7);
    DataSet biWeeklyDs = createDataSet("biWeeklyDs", new BiWeeklyPeriodType(), 4);
    DataSet monthlyDs = createDataSet("monthlyDs", new MonthlyPeriodType(), 3);
    DataSet biMonthlyDs = createDataSet("biMonthlyDs", new BiMonthlyPeriodType(), 3);
    DataSet querterlyDs = createDataSet("querterlyDs", new QuarterlyPeriodType(), 3);
    DataSet sixMonthlyDs = createDataSet("sixMonthlyDs", new SixMonthlyPeriodType(), 3);
    DataSet sixMonthlyAprDs = createDataSet("sixMonthlyAprDs", new SixMonthlyAprilPeriodType(), 4);
    DataSet sixMonthlyNovDs =
        createDataSet("sixMonthlyNovDs", new SixMonthlyNovemberPeriodType(), 5);
    DataSet yearlyDs = createDataSet("yearlyDs", new YearlyPeriodType(), 3);
    DataSet financialAprDs = createDataSet("financialAprDs", new FinancialAprilPeriodType(), 4);
    DataSet financialJulDs = createDataSet("financialJulDs", new FinancialJulyPeriodType(), 5);
    DataSet financialOctDs = createDataSet("financialOctDs", new FinancialOctoberPeriodType(), 6);
    DataSet financialNovDs = createDataSet("financialNovDs", new FinancialNovemberPeriodType(), 7);
    DataSet twoYearlyDs = createDataSet("twoYearlyDs", new TwoYearlyPeriodType(), 4);
    DataApprovalWorkflow workflow =
        new DataApprovalWorkflow("test workflow", new DailyPeriodType(), newHashSet());
    assertEquals("", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(noPeriodsAfterCoEndDate);
    assertEquals("", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(dailyDs);
    assertEquals(" + 3 * INTERVAL '+ 1 day'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(weeklyDs);
    assertEquals(" + 3 * INTERVAL '+ 1 week'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(weeklyWedDs);
    assertEquals(" + 4 * INTERVAL '+ 1 week'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(weeklyThuDs);
    assertEquals(" + 5 * INTERVAL '+ 1 week'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(weeklySatDs);
    assertEquals(" + 6 * INTERVAL '+ 1 week'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(weeklySunDs);
    assertEquals(" + 7 * INTERVAL '+ 1 week'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(biWeeklyDs);
    assertEquals(" + 4 * INTERVAL '+ 2 weeks'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(monthlyDs);
    assertEquals(" + 3 * INTERVAL '+ 1 month'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(biMonthlyDs);
    assertEquals(" + 3 * INTERVAL '+ 2 months'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(querterlyDs);
    assertEquals(" + 3 * INTERVAL '+ 3 months'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(sixMonthlyDs);
    assertEquals(" + 3 * INTERVAL '+ 6 months'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(sixMonthlyAprDs);
    assertEquals(" + 4 * INTERVAL '+ 6 months'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(sixMonthlyNovDs);
    assertEquals(" + 5 * INTERVAL '+ 6 months'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(yearlyDs);
    assertEquals(" + 3 * INTERVAL '+ 1 year'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(financialAprDs);
    assertEquals(" + 4 * INTERVAL '+ 1 year'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(financialJulDs);
    assertEquals(" + 5 * INTERVAL '+ 1 year'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(financialOctDs);
    assertEquals(" + 6 * INTERVAL '+ 1 year'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(financialNovDs);
    assertEquals(" + 7 * INTERVAL '+ 1 year'", workflow.getSqlCoEndDateExtension());
    workflow.getDataSets().add(twoYearlyDs);
    assertEquals(" + 4 * INTERVAL '+ 2 years'", workflow.getSqlCoEndDateExtension());
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------
  private DataSet createDataSet(String name, PeriodType periodType, int openPeriodsAfterCoEndDate) {
    DataSet dataSet = new DataSet(name, periodType);
    dataSet.setOpenPeriodsAfterCoEndDate(openPeriodsAfterCoEndDate);
    return dataSet;
  }
}
