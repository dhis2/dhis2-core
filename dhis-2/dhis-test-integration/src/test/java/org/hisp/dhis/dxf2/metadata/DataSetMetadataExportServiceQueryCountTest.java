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
package org.hisp.dhis.dxf2.metadata;

import static io.hypersistence.utils.jdbc.validator.SQLStatementCountValidator.reset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionComboGenerateService;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regression tests targeting {@link DataSetMetadataExportService#getDataSetMetadata()} (the {@code
 * /api/dataEntry/metadata} handler), guarding against the N+1 selects that previously fired once
 * per {@link org.hisp.dhis.dataset.DataSetElement} ({@code DataSet.getDataElements()} / {@code
 * DataElement.getCategoryCombos()}) and once per {@link org.hisp.dhis.category.CategoryOptionCombo}
 * ({@code CategoryOptionCombo.getCategoryOptions()}), and once per {@link
 * org.hisp.dhis.category.CategoryCombo} ({@code CategoryCombo.getCategories()} / {@code
 * CategoryCombo.getOptionCombos()}) or {@link org.hisp.dhis.category.Category} ({@code
 * Category.getCategoryOptions()}).
 *
 * @author david mackessy
 */
@Transactional
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class DataSetMetadataExportServiceQueryCountTest extends PostgresIntegrationTestBase {

  @Autowired private DataSetMetadataExportService exportService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private CategoryService categoryService;
  @Autowired private CategoryOptionComboGenerateService categoryOptionComboGenerateService;

  private CategoryCombo defaultCategoryCombo;
  private DataSet dataSet;
  private int uniqueCounter = 0;

  @BeforeEach
  void setUp() {
    defaultCategoryCombo = categoryService.getDefaultCategoryCombo();

    OrganisationUnit orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit);

    dataSet = createDataSet('A', PeriodType.getPeriodTypeByName("Monthly"), defaultCategoryCombo);
    dataSet.addOrganisationUnit(orgUnit);
    addDataElements(dataSet, 3);
    manager.save(dataSet);
  }

  @Test
  @DisplayName("Adding data elements to a data set does not add SQL selects to the metadata export")
  void dataElementCountDoesNotScaleQueryCount() {
    // baseline: export with 3 data elements
    long baseline = countSelectsForMetadataExport();
    assertTrue(baseline > 0, "expected the metadata export to trigger some select queries");

    // reload the now-managed data set (the previous measurement cleared then repopulated the
    // session) before mutating it, then add 5 more data elements to the same data set
    DataSet managedDataSet = manager.get(DataSet.class, dataSet.getUid());
    addDataElements(managedDataSet, 5);
    manager.update(managedDataSet);

    // export with 8 data elements
    long withMoreDataElements = countSelectsForMetadataExport();

    // DataElement / DataSetElement loads are batched into a single query, so the select count must
    // not grow with the number of data elements. If it does, the N+1 has been reintroduced (e.g. by
    // iterating DataSet.getDataElements() or DataElement.getCategoryCombos() lazily).
    assertEquals(
        baseline,
        withMoreDataElements,
        "adding data elements must not increase the number of SQL selects");
  }

  @Test
  @DisplayName("Category option combo category options are batch-loaded, not fetched per combo")
  void categoryOptionComboOptionsAreNotLoadedPerCombo() {
    // add a data element whose category combo has several option combos, so that (without the fix)
    // the categoryoptioncombos_categoryoptions join table would be queried once per option combo
    // during serialization of the exported category option combos
    CategoryCombo multiCombo = createCategoryComboWithOptions(4);
    DataElement dataElement = createDataElement((char) ('A' + uniqueCounter++));
    dataElement.setCategoryCombo(multiCombo);
    manager.save(dataElement);

    DataSet extraDataSet =
        createDataSet('Z', PeriodType.getPeriodTypeByName("Monthly"), defaultCategoryCombo);
    extraDataSet.addDataSetElement(dataElement);
    manager.save(extraDataSet);

    assertTrue(
        multiCombo.getOptionCombos().size() > 1,
        "the combo must have multiple option combos to exercise the per-combo N+1");

    clearSession();
    QueryCountDataSourceProxy.clearCapturedSql();
    exportService.getDataSetMetadata();

    // The preload fetch-joins every exported combo's category options in one statement, so the
    // categoryoptioncombos_categoryoptions join table is touched at most once. Without the preload
    // it is queried once per option combo (WHERE categoryoptioncomboid = ?).
    long categoryOptionComboOptionQueries =
        QueryCountDataSourceProxy.countCapturedSqlMatching("categoryoptioncombos_categoryoptions");
    assertTrue(
        categoryOptionComboOptionQueries <= 1,
        "category option combo options must be batch-loaded, but the join table was queried "
            + categoryOptionComboOptionQueries
            + " times");
  }

  @Test
  @DisplayName("Adding category combos does not add SQL selects to the metadata export")
  void categoryComboCountDoesNotScaleQueryCount() {
    // baseline: export with two data elements that each have their own category combo
    addDataElementsWithOwnCategoryCombo(dataSet, 2);
    manager.update(dataSet);
    long baseline = countCategoryJoinTableSelects();
    assertTrue(baseline > 0, "expected the metadata export to query the category join tables");

    // reload the now-managed data set (the previous measurement cleared then repopulated the
    // session) before mutating it, then add four more category combos
    DataSet managedDataSet = manager.get(DataSet.class, dataSet.getUid());
    addDataElementsWithOwnCategoryCombo(managedDataSet, 4);
    manager.update(managedDataSet);

    long withMoreCategoryCombos = countCategoryJoinTableSelects();

    // Each of these associations is preloaded in one fetch-joining query, so the select count must
    // not grow with the number of category combos. If it does, the association is being initialised
    // one parent at a time again — during data gathering for CategoryCombo.getCategories() and
    // Category.getCategoryOptions(), or during serialization for the other two.
    assertEquals(
        baseline,
        withMoreCategoryCombos,
        "adding category combos must not increase the number of SQL selects against the category"
            + " join tables");
  }

  @Test
  @DisplayName("Preloading the category combos keeps the exported categories in sort order")
  void preloadedCategoriesKeepTheirOrder() {
    // the categories of a combo, and the options of a category, are ordered lists (sort_order), and
    // the data entry app relies on that order. Preloading them with a fetch join must not disturb
    // it
    List<CategoryOption> options = saveCategoryOptions(2);
    Category first = createCategory("first" + uniqueCounter++, options.get(0), options.get(1));
    Category second = createCategory("second" + uniqueCounter++, options.get(1), options.get(0));
    manager.save(first);
    manager.save(second);
    CategoryCombo combo = createCategoryCombo("cc" + uniqueCounter++, first, second);
    manager.save(combo);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(combo);

    DataElement dataElement = createDataElement((char) ('A' + uniqueCounter++));
    dataElement.setCategoryCombo(combo);
    manager.save(dataElement);
    dataSet.addDataSetElement(dataElement);
    manager.update(dataSet);

    clearSession();
    ObjectNode metadata = exportService.getDataSetMetadata();

    assertEquals(
        List.of(first.getUid(), second.getUid()),
        pluckedIds(metadata, "categoryCombos", combo.getUid(), "categories"),
        "the combo's categories must be exported in sort order");
    assertEquals(
        List.of(options.get(0).getUid(), options.get(1).getUid()),
        pluckedIds(metadata, "categories", first.getUid(), "categoryOptions"),
        "the first category's options must be exported in sort order");
    assertEquals(
        List.of(options.get(1).getUid(), options.get(0).getUid()),
        pluckedIds(metadata, "categories", second.getUid(), "categoryOptions"),
        "the second category's options must be exported in its own sort order");
  }

  /** The {@code ~pluck[id]} array of the given property, on the object with the given uid. */
  private List<String> pluckedIds(
      ObjectNode metadata, String collection, String uid, String property) {
    for (JsonNode object : metadata.get(collection)) {
      if (uid.equals(object.get("id").asText())) {
        List<String> ids = new ArrayList<>();
        object.get(property).forEach(id -> ids.add(id.asText()));
        return ids;
      }
    }
    throw new AssertionError(uid + " not found in " + collection);
  }

  /**
   * Runs the metadata export from a freshly cleared session and returns the number of selects
   * issued against the join tables behind the category-combo associations it reads and serialises.
   */
  private long countCategoryJoinTableSelects() {
    clearSession();
    QueryCountDataSourceProxy.clearCapturedSql();
    exportService.getDataSetMetadata();
    return Stream.of(
            "categorycombos_categories",
            "categories_categoryoptions",
            "categorycombos_optioncombos",
            "categoryoptioncombos_categoryoptions")
        .mapToLong(QueryCountDataSourceProxy::countCapturedSqlMatching)
        .sum();
  }

  private void addDataElementsWithOwnCategoryCombo(DataSet ds, int count) {
    for (int i = 0; i < count; i++) {
      DataElement dataElement = createDataElement((char) ('A' + uniqueCounter++));
      dataElement.setCategoryCombo(createCategoryComboWithOptions(4));
      manager.save(dataElement);
      ds.addDataSetElement(dataElement);
    }
  }

  @Test
  @DisplayName(
      "Streaming projection export issues a fixed number of selects regardless of graph size")
  void streamingExportQueryCountDoesNotScale() throws Exception {
    // baseline: stream with the set-up graph
    long baseline = countSelectsForStreamingExport();
    assertTrue(baseline > 0, "expected the streaming export to trigger some select queries");

    // add a data element to the SAME data set whose category combo fans out into several option
    // combos: with an entity-graph traversal this would add a per-combo / per-option N+1 (once per
    // option combo and once per category). The projection store issues the same flat queries.
    DataSet managedDataSet = manager.get(DataSet.class, dataSet.getUid());
    CategoryCombo multiCombo = createCategoryComboWithOptions(5);
    DataElement dataElement = createDataElement((char) ('A' + uniqueCounter++));
    dataElement.setCategoryCombo(multiCombo);
    manager.save(dataElement);
    managedDataSet.addDataSetElement(dataElement);
    manager.update(managedDataSet);

    long withLargerGraph = countSelectsForStreamingExport();

    // the projection store issues a fixed set of flat queries; growing the category graph adds rows
    // to those queries, not more queries. If this grows, an N+1 has crept back in.
    assertEquals(
        baseline,
        withLargerGraph,
        "streaming projection select count must not grow with the size of the category graph");
  }

  /**
   * Runs the metadata export from a freshly cleared Hibernate session (to mimic a new request and
   * force queries to hit the database) and returns the number of select statements issued.
   */
  private long countSelectsForMetadataExport() {
    clearSession();
    reset();
    exportService.getDataSetMetadata();
    return QueryCountHolder.getGrandTotal().getSelect();
  }

  /** As {@link #countSelectsForMetadataExport()} but for the streaming projection path. */
  private long countSelectsForStreamingExport() throws Exception {
    clearSession();
    reset();
    exportService.writeDataSetMetadata(new ByteArrayOutputStream());
    return QueryCountHolder.getGrandTotal().getSelect();
  }

  private void addDataElements(DataSet ds, int count) {
    for (int i = 0; i < count; i++) {
      DataElement dataElement = createDataElement((char) ('A' + uniqueCounter++));
      dataElement.setCategoryCombo(defaultCategoryCombo);
      manager.save(dataElement);
      ds.addDataSetElement(dataElement);
    }
  }

  private List<CategoryOption> saveCategoryOptions(int optionCount) {
    List<CategoryOption> options = new ArrayList<>();
    for (int i = 0; i < optionCount; i++) {
      CategoryOption option = createCategoryOption((char) ('A' + uniqueCounter++));
      manager.save(option);
      options.add(option);
    }
    return options;
  }

  private CategoryCombo createCategoryComboWithOptions(int optionCount) {
    List<CategoryOption> options = saveCategoryOptions(optionCount);
    Category category =
        createCategory("cat" + uniqueCounter++, options.toArray(new CategoryOption[0]));
    manager.save(category);
    CategoryCombo categoryCombo = createCategoryCombo("cc" + uniqueCounter++, category);
    manager.save(categoryCombo);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(categoryCombo);
    return categoryCombo;
  }
}
