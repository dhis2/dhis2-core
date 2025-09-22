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
package org.hisp.dhis.resourcetable;

import static java.time.temporal.ChronoUnit.YEARS;
import static java.util.Comparator.reverseOrder;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
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
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
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
import org.hisp.dhis.resourcetable.table.DataSetResourceTable;
import org.hisp.dhis.resourcetable.table.DatePeriodResourceTable;
import org.hisp.dhis.resourcetable.table.IndicatorGroupSetResourceTable;
import org.hisp.dhis.resourcetable.table.OrganisationUnitGroupSetResourceTable;
import org.hisp.dhis.resourcetable.table.OrganisationUnitStructureResourceTable;
import org.hisp.dhis.resourcetable.table.PeriodResourceTable;
import org.hisp.dhis.resourcetable.table.RelationshipCountResourceTable;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewService;
import org.hisp.dhis.tablereplication.TableReplicationStore;
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

  private final TableReplicationStore tableReplicationStore;

  private final IdentifiableObjectManager idObjectManager;

  private final OrganisationUnitService organisationUnitService;

  private final PeriodService periodService;

  private final SqlViewService sqlViewService;

  private final DataApprovalLevelService dataApprovalLevelService;

  private final CategoryService categoryService;

  private final AnalyticsTableSettings analyticsTableSettings;

  private final PeriodDataProvider periodDataProvider;

  @Override
  @Transactional
  public void generateResourceTables() {
    for (ResourceTable table : getResourceTables()) {
      resourceTableStore.generateResourceTable(table);
    }
  }

  @Override
  @Transactional
  public void replicateAnalyticsResourceTables() {
    tableReplicationStore.replicateAnalyticsDatabaseTables(
        getResourceTables().stream().map(ResourceTable::getMainTable).toList());
  }

  @Override
  @Transactional
  public void generateDataApprovalResourceTables() {
    for (ResourceTable table : getApprovalResourceTables()) {
      resourceTableStore.generateResourceTable(table);
    }
  }

  @Override
  @Transactional
  public void replicateDataApprovalResourceTables() {
    for (ResourceTable table : getApprovalResourceTables()) {
      tableReplicationStore.replicateAnalyticsDatabaseTable(table.getMainTable());
    }
  }

  /**
   * Returns a list of resource tables.
   *
   * @return a list of {@link ResourceTable}.
   */
  private List<ResourceTable> getResourceTables() {
    Logged logged = analyticsTableSettings.getTableLogged();
    return List.of(
        new OrganisationUnitStructureResourceTable(
            logged,
            organisationUnitService.getNumberOfOrganisationalLevels(),
            organisationUnitService),
        new DataSetOrganisationUnitCategoryResourceTable(
            logged,
            idObjectManager.getAllNoAcl(DataSet.class),
            categoryService.getDefaultCategoryOptionCombo()),
        new CategoryOptionComboNameResourceTable(
            logged, idObjectManager.getAllNoAcl(CategoryCombo.class)),
        new DataElementGroupSetResourceTable(
            logged, idObjectManager.getDataDimensionsNoAcl(DataElementGroupSet.class)),
        new IndicatorGroupSetResourceTable(
            logged, idObjectManager.getAllNoAcl(IndicatorGroupSet.class)),
        new DataSetResourceTable(logged),
        new OrganisationUnitGroupSetResourceTable(
            logged,
            idObjectManager.getDataDimensionsNoAcl(OrganisationUnitGroupSet.class),
            organisationUnitService.getNumberOfOrganisationalLevels()),
        new CategoryResourceTable(
            logged,
            idObjectManager.getDataDimensionsNoAcl(Category.class),
            idObjectManager.getDataDimensionsNoAcl(CategoryOptionGroupSet.class)),
        new DataElementResourceTable(logged, idObjectManager.getAllNoAcl(DataElement.class)),
        new DatePeriodResourceTable(logged, getAndValidateAvailableDataYears()),
        new PeriodResourceTable(logged, periodService.getAllPeriods()),
        new CategoryOptionComboResourceTable(logged),
        new RelationshipCountResourceTable(logged));
  }

  /**
   * Returns a list of data approval resource tables.
   *
   * @return a list of data approval {@link ResourceTable}.
   */
  private final List<ResourceTable> getApprovalResourceTables() {
    Logged logged = analyticsTableSettings.getTableLogged();
    return List.of(
        new DataApprovalRemapLevelResourceTable(logged),
        new DataApprovalMinLevelResourceTable(
            logged,
            Lists.newArrayList(dataApprovalLevelService.getOrganisationUnitApprovalLevels())));
  }

  /**
   * Validates and returns the available data years.
   *
   * @return the list of available data years.
   */
  List<Integer> getAndValidateAvailableDataYears() {
    List<Integer> availableYears =
        periodDataProvider.getAvailableYears(analyticsTableSettings.getPeriodSource());
    validateYearsOffset(availableYears);
    return availableYears;
  }

  /**
   * This method validates if any of the year in the given list is within the offset defined in
   * system settings. The constant where the offset is defined can be seen at {@link
   * SystemSettings#getAnalyticsPeriodYearsOffset()}.
   *
   * <p>Based on the current year YYYY and the defined offset X. This method allows a range of X
   * years in the past and X years in the future. Including also the current year YYYY. So, for
   * YYYY=2023 and offset=2, the valid range would be [2021,2022,2023,2024,2025].
   *
   * @param yearsToCheck the list of years to be checked.
   */
  private void validateYearsOffset(List<Integer> yearsToCheck) {
    Integer maxYearsOffset = analyticsTableSettings.getMaxPeriodYearsOffset();

    if (maxYearsOffset != null) {
      int minRangeAllowed = Year.now().minus(maxYearsOffset, YEARS).getValue();
      int maxRangeAllowed = Year.now().plus(maxYearsOffset, YEARS).getValue();

      List<Integer> yearsOutOfRange =
          yearsToCheck.stream()
              .filter(year -> year < minRangeAllowed || year > maxRangeAllowed)
              .toList();

      List<Integer> yearsInRange =
          yearsToCheck.stream()
              .filter(year -> year >= minRangeAllowed && year <= maxRangeAllowed)
              .toList();

      if (isNotEmpty(yearsOutOfRange)) {
        String errorMessage =
            String.format(
                """
                Database contains years outside of the allowed offset. \
                Years in allowed range: %d \
                Years out of range: %d\
                """,
                yearsInRange, yearsOutOfRange);

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
