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
package org.hisp.dhis.resourcetable;

import static java.time.temporal.ChronoUnit.YEARS;
import static java.util.Comparator.reverseOrder;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.DATABASE;
import static org.hisp.dhis.period.PeriodDataProvider.DataSource.SYSTEM_DEFINED;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;

import com.google.common.collect.Lists;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.resourcetable.table.CategoryOptionComboNameResourceTable;
import org.hisp.dhis.resourcetable.table.CategoryOptionComboResourceTable;
import org.hisp.dhis.resourcetable.table.CategoryResourceTable;
import org.hisp.dhis.resourcetable.table.DataApprovalMinLevelResourceTable;
import org.hisp.dhis.resourcetable.table.DataApprovalRemapLevelResourceTable;
import org.hisp.dhis.resourcetable.table.DataElementGroupSetResourceTable;
import org.hisp.dhis.resourcetable.table.DataElementResourceTable;
import org.hisp.dhis.resourcetable.table.DataSetOrganisationUnitCategoryResourceTable;
import org.hisp.dhis.resourcetable.table.DatePeriodResourceTable;
import org.hisp.dhis.resourcetable.table.IndicatorGroupSetResourceTable;
import org.hisp.dhis.resourcetable.table.OrganisationUnitGroupSetResourceTable;
import org.hisp.dhis.resourcetable.table.OrganisationUnitStructureResourceTable;
import org.hisp.dhis.resourcetable.table.PeriodResourceTable;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultResourceTableService implements ResourceTableService {
  private final ResourceTableStore resourceTableStore;

  private final IdentifiableObjectManager idObjectManager;

  private final OrganisationUnitService organisationUnitService;

  private final PeriodService periodService;

  private final SqlViewService sqlViewService;

  private final DataApprovalLevelService dataApprovalLevelService;

  private final CategoryService categoryService;

  private final AnalyticsTableSettings analyticsTableSettings;

  private final PeriodDataProvider periodDataProvider;

  private final SqlBuilder sqlBuilder;

  @Override
  @Transactional
  public void generateOrganisationUnitStructureTable() {
    resourceTableStore.generateResourceTable(
        new OrganisationUnitStructureResourceTable(
            sqlBuilder,
            analyticsTableSettings.getTableLogged(),
            organisationUnitService.getNumberOfOrganisationalLevels(),
            organisationUnitService));
  }

  @Override
  @Transactional
  public void generateDataSetOrganisationUnitCategoryTable() {
    resourceTableStore.generateResourceTable(
        new DataSetOrganisationUnitCategoryResourceTable(
            sqlBuilder,
            analyticsTableSettings.getTableLogged(),
            idObjectManager.getAllNoAcl(DataSet.class),
            categoryService.getDefaultCategoryOptionCombo()));
  }

  @Override
  @Transactional
  public void generateCategoryOptionComboNameTable() {
    resourceTableStore.generateResourceTable(
        new CategoryOptionComboNameResourceTable(
            sqlBuilder,
            analyticsTableSettings.getTableLogged(),
            idObjectManager.getAllNoAcl(CategoryCombo.class)));
  }

  @Override
  @Transactional
  public void generateDataElementGroupSetTable() {
    resourceTableStore.generateResourceTable(
        new DataElementGroupSetResourceTable(
            sqlBuilder,
            analyticsTableSettings.getTableLogged(),
            idObjectManager.getDataDimensionsNoAcl(DataElementGroupSet.class)));
  }

  @Override
  @Transactional
  public void generateIndicatorGroupSetTable() {
    resourceTableStore.generateResourceTable(
        new IndicatorGroupSetResourceTable(
            sqlBuilder,
            analyticsTableSettings.getTableLogged(),
            idObjectManager.getAllNoAcl(IndicatorGroupSet.class)));
  }

  @Override
  @Transactional
  public void generateOrganisationUnitGroupSetTable() {
    resourceTableStore.generateResourceTable(
        new OrganisationUnitGroupSetResourceTable(
            sqlBuilder,
            analyticsTableSettings.getTableLogged(),
            idObjectManager.getDataDimensionsNoAcl(OrganisationUnitGroupSet.class),
            organisationUnitService.getNumberOfOrganisationalLevels()));
  }

  @Override
  @Transactional
  public void generateCategoryTable() {
    resourceTableStore.generateResourceTable(
        new CategoryResourceTable(
            sqlBuilder,
            analyticsTableSettings.getTableLogged(),
            idObjectManager.getDataDimensionsNoAcl(Category.class),
            idObjectManager.getDataDimensionsNoAcl(CategoryOptionGroupSet.class)));
  }

  @Override
  @Transactional
  public void generateDataElementTable() {
    resourceTableStore.generateResourceTable(
        new DataElementResourceTable(
            sqlBuilder,
            analyticsTableSettings.getTableLogged(),
            idObjectManager.getAllNoAcl(DataElement.class)));
  }

  @Override
  @Transactional
  public void generateDatePeriodTable() {
    List<Integer> availableYears =
        periodDataProvider.getAvailableYears(
            analyticsTableSettings.getMaxPeriodYearsOffset() == null ? SYSTEM_DEFINED : DATABASE);
    checkYearsOffset(availableYears);
    resourceTableStore.generateResourceTable(
        new DatePeriodResourceTable(
            sqlBuilder, analyticsTableSettings.getTableLogged(), availableYears));
  }

  @Override
  public void generatePeriodTable() {
    resourceTableStore.generateResourceTable(
        new PeriodResourceTable(
            sqlBuilder, analyticsTableSettings.getTableLogged(), periodService.getAllPeriods()));
  }

  @Override
  @Transactional
  public void generateCategoryOptionComboTable() {
    resourceTableStore.generateResourceTable(
        new CategoryOptionComboResourceTable(sqlBuilder, analyticsTableSettings.getTableLogged()));
  }

  @Override
  public void generateDataApprovalRemapLevelTable() {
    resourceTableStore.generateResourceTable(
        new DataApprovalRemapLevelResourceTable(
            sqlBuilder, analyticsTableSettings.getTableLogged()));
  }

  @Override
  public void generateDataApprovalMinLevelTable() {
    List<OrganisationUnitLevel> orgUnitLevels =
        Lists.newArrayList(dataApprovalLevelService.getOrganisationUnitApprovalLevels());

    if (!orgUnitLevels.isEmpty()) {
      resourceTableStore.generateResourceTable(
          new DataApprovalMinLevelResourceTable(
              sqlBuilder, analyticsTableSettings.getTableLogged(), orgUnitLevels));
    }
  }

  /**
   * This method checks if any of the year in the given list is within the offset defined in system
   * settings. The constant where the offset is defined can be seen at {@link
   * SettingKey.ANALYTICS_MAX_PERIOD_YEARS_OFFSET}.
   *
   * <p>Based on the current year YYYY and the defined offset X. This method allows a range of X
   * years in the past and X years in the future. Including also the current year YYYY. So, for
   * YYYY=2023 and offset=2, the valid range would be [2021,2022,2023,2024,2025].
   *
   * @param yearsToCheck the list of years to be checked.
   */
  private void checkYearsOffset(List<Integer> yearsToCheck) {
    Integer maxYearsOffset = analyticsTableSettings.getMaxPeriodYearsOffset();

    if (maxYearsOffset != null) {
      int minRangeAllowed = Year.now().minus(maxYearsOffset, YEARS).getValue();
      int maxRangeAllowed = Year.now().plus(maxYearsOffset, YEARS).getValue();

      boolean yearsOutOfRange =
          yearsToCheck.stream().anyMatch(year -> year < minRangeAllowed || year > maxRangeAllowed);

      if (yearsOutOfRange) {
        String errorMessage = "Your database contains years out of the allowed offset.";
        errorMessage +=
            "\n Range of years allowed (based on your system settings and existing data): "
                + yearsToCheck.stream()
                    .filter(year -> year >= minRangeAllowed && year <= maxRangeAllowed)
                    .toList()
                + ".";
        errorMessage +=
            "\n Years out of range found: "
                + yearsToCheck.stream()
                    .filter(year -> year < minRangeAllowed || year > maxRangeAllowed)
                    .toList()
                + ".";

        log.warn(errorMessage);
        throw new RuntimeException(errorMessage);
      }
    }
  }

  // -------------------------------------------------------------------------
  // SQL Views. Each view is created/dropped in separate transactions so that
  // process continues even if individual operations fail.
  // -------------------------------------------------------------------------

  @Override
  public void createAllSqlViews(JobProgress progress) {
    List<SqlView> nonQueryViews =
        new ArrayList<>(sqlViewService.getAllSqlViewsNoAcl())
            .stream().sorted().filter(view -> !view.isQuery()).collect(Collectors.toList());

    progress.startingStage("Create SQL views", nonQueryViews.size(), SKIP_ITEM);
    progress.runStage(
        nonQueryViews,
        SqlView::getViewName,
        view -> {
          try {
            sqlViewService.createViewTable(view);
          } catch (IllegalQueryException ex) {
            log.warn(
                "Ignoring SQL view which failed validation: '{}', '{}', message: '{}'",
                view.getUid(),
                view.getName(),
                ex.getMessage());
          }
        });
  }

  @Override
  public void dropAllSqlViews(JobProgress progress) {
    List<SqlView> nonQueryViews =
        new ArrayList<>(sqlViewService.getAllSqlViewsNoAcl())
            .stream()
                .filter(view -> !view.isQuery())
                .sorted(reverseOrder())
                .collect(Collectors.toList());
    progress.startingStage("Drop SQL views", nonQueryViews.size(), SKIP_ITEM);
    progress.runStage(nonQueryViews, SqlView::getViewName, sqlViewService::dropViewTable);
  }
}
