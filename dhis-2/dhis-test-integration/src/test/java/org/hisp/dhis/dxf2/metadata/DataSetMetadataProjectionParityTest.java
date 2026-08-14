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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboGenerateService;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataset.DataInputPeriod;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that the streaming projection path ({@link
 * DataSetMetadataExportService#writeDataSetMetadata}) produces a document semantically identical to
 * the legacy field-filter path ({@link DataSetMetadataExportService#getDataSetMetadata()}) over a
 * fixture that exercises the whole exported category graph: multi-category combos with generated
 * option combos, both category-combo variants (data element combos that expose their option combos
 * and data-set attribute combos that do not), category options with organisation units,
 * translations, and the data-set category data-write access path.
 *
 * <p>Set-valued id arrays are compared order-insensitively (the projection emits them in a
 * deterministic order the entity graph does not guarantee); everything else must match exactly.
 *
 * @author david mackessy
 */
@Transactional
class DataSetMetadataProjectionParityTest extends PostgresIntegrationTestBase {

  @Autowired private DataSetMetadataExportService exportService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private CategoryService categoryService;
  @Autowired private CategoryOptionComboGenerateService categoryOptionComboGenerateService;
  @Autowired private PeriodService periodService;

  private static ObjectStyle style(String color, String icon) {
    ObjectStyle style = new ObjectStyle();
    style.setColor(color);
    style.setIcon(icon);
    return style;
  }

  @BeforeEach
  void setUp() {
    CategoryCombo defaultCombo = categoryService.getDefaultCategoryCombo();

    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnit ouB = createOrganisationUnit('B');
    manager.save(ouA);
    manager.save(ouB);

    // disaggregation graph: 2 categories x 2 options -> 4 option combos
    CategoryOption optA1 = createCategoryOption('A');
    CategoryOption optA2 = createCategoryOption('B');
    CategoryOption optB1 = createCategoryOption('C');
    CategoryOption optB2 = createCategoryOption('D');
    optA1.getOrganisationUnits().add(ouA);
    optA1.getOrganisationUnits().add(ouB);
    optA2.getOrganisationUnits().add(ouB);
    // populate every nullable simple field so the parity comparison actually exercises each one
    optA1.setDescription("Option A1 description");
    optA1.setFormName("Option A1 form name");
    optA1.setStartDate(new Date(1_600_000_000_000L));
    optA1.setEndDate(new Date(1_600_100_000_000L));
    optA1.setStyle(style("#111111", "opt-icon"));
    // translations exercise the display* fallback logic on both paths
    optA1.setTranslations(
        Set.of(
            new Translation(Locale.of("fr"), "NAME", "Option A1 FR"),
            new Translation(Locale.of("fr"), "SHORT_NAME", "Opt A1 FR")));
    manager.save(optA1);
    manager.save(optA2);
    manager.save(optB1);
    manager.save(optB2);

    Category catA = createCategory('A', optA1, optA2);
    Category catB = createCategory('B', optB1, optB2);
    catA.setDescription("Category A description");
    catA.setTranslations(
        Set.of(
            new Translation(Locale.of("fr"), "NAME", "Categorie A"),
            new Translation(Locale.of("fr"), "SHORT_NAME", "Cat A")));
    manager.save(catA);
    manager.save(catB);

    CategoryCombo disaggCombo = createCategoryCombo('X', catA, catB);
    disaggCombo.setTranslations(Set.of(new Translation(Locale.of("fr"), "NAME", "Combo X FR")));
    manager.save(disaggCombo);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(disaggCombo);

    // attribute combo used as the data-set category combo (data-set category ACL path)
    CategoryOption attrOpt = createCategoryOption('E');
    manager.save(attrOpt);
    Category attrCat = createCategory('Z', attrOpt);
    attrCat.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    manager.save(attrCat);
    CategoryCombo attrCombo = createCategoryCombo('Y', attrCat);
    attrCombo.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    manager.save(attrCombo);
    categoryOptionComboGenerateService.addAndPruneOptionCombos(attrCombo);

    // option set on a data element
    OptionSet optionSet = createOptionSet('O', createOption('1'), createOption('2'));
    optionSet.setValueType(org.hisp.dhis.common.ValueType.TEXT);
    optionSet.setDescription("Option set O description");
    optionSet.setVersion(3);
    optionSet.getOptions().forEach(manager::save);
    manager.save(optionSet);

    DataElement de1 = createDataElement('A', disaggCombo);
    de1.setOptionSet(optionSet);
    de1.setCommentOptionSet(optionSet);
    de1.setDescription("DataElement A description");
    de1.setStyle(style("#444444", "de-icon"));
    DataElement de2 = createDataElement('B', defaultCombo);
    manager.save(de1);
    manager.save(de2);

    IndicatorType indicatorType = new IndicatorType("IndType", 1, false);
    manager.save(indicatorType);
    Indicator indicator = createIndicator('A', indicatorType);
    manager.save(indicator);

    CategoryOptionCombo defaultCoc = categoryService.getDefaultCategoryOptionCombo();

    DataEntryForm dataEntryForm = createDataEntryForm('A');
    manager.save(dataEntryForm);

    Period period = createPeriod("202001");
    periodService.addPeriod(period);

    DataSet dataSet = createDataSet('A', PeriodType.getPeriodTypeByName("Monthly"), attrCombo);
    dataSet.setDescription("DataSet A description");
    dataSet.setFormName("DataSet A form name");
    dataSet.setStyle(style("#333333", "ds-icon"));
    dataSet.setDataEntryForm(dataEntryForm);
    dataSet.addOrganisationUnit(ouA);
    dataSet.addOrganisationUnit(ouB);
    dataSet.addDataSetElement(de1);
    dataSet.addDataSetElement(de2);
    dataSet.addIndicator(indicator);
    dataSet.addCompulsoryDataElementOperand(new DataElementOperand(de1, defaultCoc));

    DataInputPeriod dataInputPeriod = new DataInputPeriod();
    dataInputPeriod.setPeriod(period);
    dataInputPeriod.setOpeningDate(new Date(1_600_000_000_000L));
    dataInputPeriod.setClosingDate(new Date(1_600_100_000_000L));
    dataSet.getDataInputPeriods().add(dataInputPeriod);
    manager.save(dataSet);

    // build the Section directly (the createSection helper forces an immutable greyedFields set)
    Set<DataElementOperand> greyedFields = new HashSet<>();
    greyedFields.add(new DataElementOperand(de2, defaultCoc));
    Section section = new Section("SectionA", dataSet, List.of(de1, de2), greyedFields);
    section.setAutoFields();
    section.getIndicators().add(indicator);
    manager.save(section);
    dataSet.getSections().add(section);
    manager.update(dataSet);

    manager.flush();
  }

  @Test
  @DisplayName("Streaming projection output is semantically identical to the legacy field filter")
  void streamingMatchesLegacy() throws Exception {
    // clear the session so the legacy path also reads fresh from the DB (as a real request does),
    // rather than from session-cached entities — otherwise DATE columns (startDate/endDate) would
    // still carry the fixture's in-memory time component that the DB round-trip truncates.
    clearSession();
    ObjectNode legacy = exportService.getDataSetMetadata();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    exportService.writeDataSetMetadata(out);
    JsonNode streamed = JacksonObjectMapperConfig.jsonMapper.readTree(out.toByteArray());

    assertEquals(normalize(legacy), normalize(streamed));
  }

  /**
   * Recursively normalizes a JSON document so that arrays are compared as (multi)sets: every
   * array's elements are normalized and then sorted by their canonical serialization. Object field
   * order is already irrelevant to {@link JsonNode#equals}.
   */
  private static JsonNode normalize(JsonNode node) {
    if (node.isArray()) {
      List<JsonNode> children = new ArrayList<>();
      node.forEach(child -> children.add(normalize(child)));
      children.sort(Comparator.comparing(JsonNode::toString));
      ArrayNode array = JacksonObjectMapperConfig.jsonMapper.createArrayNode();
      children.forEach(array::add);
      return array;
    }
    if (node.isObject()) {
      // insert fields in sorted key order so the canonical toString (used to sort arrays below) is
      // independent of the field insertion order, which differs between the two serialisers
      ObjectNode object = JacksonObjectMapperConfig.jsonMapper.createObjectNode();
      List<String> names = new ArrayList<>();
      node.fieldNames().forEachRemaining(names::add);
      names.sort(Comparator.naturalOrder());
      names.forEach(name -> object.set(name, normalize(node.get(name))));
      return object;
    }
    return node;
  }
}
