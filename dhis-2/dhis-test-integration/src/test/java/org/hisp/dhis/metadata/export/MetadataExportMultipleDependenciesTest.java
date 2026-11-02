/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.metadata.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the multi-object dependency export (DHIS2-21899).
 *
 * <p>The acceptance criterion is that N roots of mixed types come back as one payload containing
 * all of them plus their transitive dependencies, with no root-level duplication. The three shapes
 * that can break that are a dependency shared by two roots of the same type, a dependency shared by
 * roots of different types, and a root that is itself another root's dependency; all three are
 * covered here against a real object graph.
 *
 * @author David Mackessy
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class MetadataExportMultipleDependenciesTest extends PostgresIntegrationTestBase {

  @Autowired private MetadataExportService metadataExportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private ObjectMapper jsonMapper;

  private CategoryCombo sharedCategoryCombo;
  private DataElement sharedDataElement;
  private DataSet dataSetA;
  private DataSet dataSetB;
  private Program program;
  private OptionSet optionSet;

  @BeforeEach
  void setUpMetadataGraph() {
    CategoryOption categoryOption = createCategoryOption('A');
    manager.save(categoryOption);
    Category category = createCategory('A', categoryOption);
    manager.save(category);
    sharedCategoryCombo = createCategoryCombo('A', category);
    manager.save(sharedCategoryCombo);

    sharedDataElement = createDataElement('A', sharedCategoryCombo);
    manager.save(sharedDataElement);

    dataSetA = createDataSet('A', PeriodType.getPeriodTypeByName("Monthly"));
    dataSetA.setCategoryCombo(sharedCategoryCombo);
    dataSetA.addDataSetElement(sharedDataElement);
    manager.save(dataSetA);

    dataSetB = createDataSet('B', PeriodType.getPeriodTypeByName("Monthly"));
    dataSetB.setCategoryCombo(sharedCategoryCombo);
    dataSetB.addDataSetElement(sharedDataElement);
    manager.save(dataSetB);

    // a root of a different type that depends on the same CategoryCombo as the data sets
    program = createProgram('A');
    program.setCategoryCombo(sharedCategoryCombo);
    manager.save(program);

    Option option = createOption('A');
    optionSet = createOptionSet('A', option);
    optionSet.setValueType(ValueType.TEXT);
    manager.save(option);
    manager.save(optionSet);
  }

  @Test
  @DisplayName("A collection of one root gives the same result as the single-root call")
  void singleRootCollectionEqualsSingleRootScalar() {
    assertEquals(
        metadataExportService.getMetadataWithDependencies(dataSetA),
        metadataExportService.getMetadataWithDependencies(List.of(dataSetA)));
  }

  @Test
  @DisplayName("Two roots sharing a dependency yield one copy of that dependency")
  void diamondDependencyAppearsOnce() {
    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> export =
        metadataExportService.getMetadataWithDependencies(List.of(dataSetA, dataSetB));

    assertEquals(2, export.get(DataSet.class).size(), "both roots must be present");
    assertEquals(1, export.get(DataElement.class).size(), "shared DataElement was duplicated");
    assertEquals(1, export.get(CategoryCombo.class).size(), "shared CategoryCombo was duplicated");
    assertEquals(1, export.get(Category.class).size());
    assertEquals(1, export.get(CategoryOption.class).size());
  }

  @Test
  @DisplayName("Roots of different types sharing a dependency yield one copy of it")
  void diamondAcrossRootTypesAppearsOnce() {
    // guard against a vacuous assertion: each root must reach the CategoryCombo on its own,
    // otherwise "appears once" would hold simply because only one root contributed it
    assertTrue(
        metadataExportService
            .getMetadataWithDependencies(dataSetA)
            .get(CategoryCombo.class)
            .contains(sharedCategoryCombo),
        "the DataSet root does not reach the shared CategoryCombo");
    assertTrue(
        metadataExportService
            .getMetadataWithDependencies(program)
            .get(CategoryCombo.class)
            .contains(sharedCategoryCombo),
        "the Program root does not reach the shared CategoryCombo");

    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> export =
        metadataExportService.getMetadataWithDependencies(List.of(dataSetA, program));

    assertEquals(1, export.get(DataSet.class).size(), "the DataSet root must be present");
    assertEquals(1, export.get(Program.class).size(), "the Program root must be present");
    assertEquals(
        1,
        export.get(CategoryCombo.class).size(),
        "the CategoryCombo is reached from roots of two different types");
    assertEquals(1, export.get(Category.class).size(), "shared Category was duplicated");
    assertEquals(
        1, export.get(CategoryOption.class).size(), "shared CategoryOption was duplicated");
  }

  @Test
  @DisplayName("A root that is also another root's dependency appears once")
  void rootThatIsAlsoADependencyAppearsOnce() {
    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> export =
        metadataExportService.getMetadataWithDependencies(List.of(dataSetA, sharedCategoryCombo));

    assertEquals(1, export.get(DataSet.class).size());
    assertEquals(
        1,
        export.get(CategoryCombo.class).size(),
        "the CategoryCombo is both a root and a dependency");
    assertTrue(export.get(CategoryCombo.class).contains(sharedCategoryCombo));
  }

  @Test
  @DisplayName("Roots of mixed types all appear in one payload")
  void mixedRootTypesAllAppear() {
    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> export =
        metadataExportService.getMetadataWithDependencies(
            List.of(dataSetA, optionSet, sharedCategoryCombo));

    assertTrue(export.get(DataSet.class).contains(dataSetA));
    assertTrue(export.get(OptionSet.class).contains(optionSet));
    assertTrue(export.get(CategoryCombo.class).contains(sharedCategoryCombo));
    assertEquals(1, export.get(Option.class).size());
  }

  @Test
  @DisplayName("The same root requested twice contributes once")
  void repeatedRootAppearsOnce() {
    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> export =
        metadataExportService.getMetadataWithDependencies(List.of(dataSetA, dataSetA));

    assertEquals(1, export.get(DataSet.class).size());
  }

  @Test
  @DisplayName("Each declared root type actually produces a dependency closure")
  void declaredSupportedRootTypesProduceOutput() {
    Set<Class<? extends IdentifiableObject>> supported =
        metadataExportService.getDependencyRootTypes();

    assertTrue(supported.contains(DataSet.class));
    assertTrue(supported.contains(OptionSet.class));
    assertTrue(supported.contains(CategoryCombo.class));

    for (IdentifiableObject root : List.of(dataSetA, optionSet, sharedCategoryCombo)) {
      assertFalse(
          metadataExportService.getMetadataWithDependencies(root).isEmpty(),
          root.getClass().getSimpleName() + " is declared supported but produced nothing");
    }
  }

  @Test
  @DisplayName("The rendered payload has no duplicate id in any array")
  void renderedPayloadHasNoDuplicateIds() throws IOException {
    JsonNode payload = render(List.of(dataSetA, dataSetB, sharedCategoryCombo, optionSet));

    assertNotNull(payload.get("system"), "the system node is missing");
    assertTrue(payload.has("dataSets"));
    assertTrue(payload.has("categoryCombos"));
    assertTrue(payload.has("optionSets"));

    assertEquals(2, payload.get("dataSets").size());
    assertEquals(1, payload.get("categoryCombos").size());

    payload
        .properties()
        .forEach(
            entry -> {
              if (entry.getValue().isArray()) {
                List<String> ids = new ArrayList<>();
                entry.getValue().forEach(node -> ids.add(node.path("id").asText()));
                assertEquals(
                    new HashSet<>(ids).size(),
                    ids.size(),
                    "duplicate id in array '" + entry.getKey() + "': " + ids);
              }
            });
  }

  @Test
  @DisplayName("Lean payload flags apply to objects contributed by every root")
  void skipFlagsApplyAcrossAllRoots() throws IOException {
    MetadataExportParams params = new MetadataExportParams();
    params.setSkipSharing(true);
    params.setSkipCreatedAndLastUpdated(true);

    String payload = render(List.of(dataSetA, dataSetB, optionSet), params).toString();

    assertFalse(payload.contains("\"created\""), "created survived skipCreatedAndLastUpdated");
    assertFalse(
        payload.contains("\"lastUpdated\""), "lastUpdated survived skipCreatedAndLastUpdated");
    assertFalse(payload.contains("\"sharing\""), "sharing survived skipSharing");
  }

  private JsonNode render(List<? extends IdentifiableObject> roots) throws IOException {
    return render(roots, new MetadataExportParams());
  }

  private JsonNode render(List<? extends IdentifiableObject> roots, MetadataExportParams params)
      throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    metadataExportService.getMetadataWithDependenciesAsNodeStream(roots, params, out);
    return jsonMapper.readTree(out.toByteArray());
  }
}
