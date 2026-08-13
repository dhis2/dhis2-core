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
package org.hisp.dhis.dxf2.metadata;

import static org.hisp.dhis.common.IdentifiableObjectUtils.sortById;
import static org.hisp.dhis.common.collection.CollectionUtils.addIfNotNull;
import static org.hisp.dhis.common.collection.CollectionUtils.flatMapToSet;
import static org.hisp.dhis.common.collection.CollectionUtils.mapToSet;
import static org.hisp.dhis.commons.collection.ListUtils.distinctUnion;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.metadata.DataSetMetadataStore.Assoc;
import org.hisp.dhis.dxf2.metadata.DataSetMetadataStore.CategoryRow;
import org.hisp.dhis.dxf2.metadata.DataSetMetadataStore.CocRow;
import org.hisp.dhis.dxf2.metadata.DataSetMetadataStore.ComboRow;
import org.hisp.dhis.dxf2.metadata.DataSetMetadataStore.OptionRow;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.descriptors.CategoryComboSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryOptionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategorySchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IndicatorSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OptionSetSchemaDescriptor;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.dxf2.metadata.DataSetMetadataExportService")
public class DefaultDataSetMetadataExportService implements DataSetMetadataExportService {
  private static final List<Class<? extends IdentifiableObject>> METADATA_TYPES =
      List.of(
          DataSet.class,
          Section.class,
          DataElement.class,
          Indicator.class,
          CategoryCombo.class,
          Category.class,
          CategoryOption.class,
          OptionSet.class);

  private static final String PROPERTY_ORGANISATION_UNITS = "organisationUnits";

  private static final String PROPERTY_DATA_SET_ELEMENTS = "dataSetElements";

  private static final String FIELDS_DATA_SETS =
      ":simple,categoryCombo[id],formType,dataEntryForm[id],"
          + "dataInputPeriods[period,openingDate,closingDate],"
          + "indicators~pluck[id],"
          + "compulsoryDataElementOperands[dataElement[id],categoryOptionCombo[id]],"
          + "sections[:simple,displayOptions,dataElements~pluck[id],indicators~pluck[id],"
          + "greyedFields[dataElement[id],categoryOptionCombo[id]]]";

  private static final String FIELDS_DATA_SET_ELEMENTS = "dataElement[id],categoryCombo[id]";

  private static final String FIELDS_DATA_ELEMENTS =
      ":identifiable,displayName,displayShortName,displayFormName,"
          + "zeroIsSignificant,valueType,aggregationType,categoryCombo[id],optionSet[id],commentOptionSet,description";

  private static final String FIELDS_INDICATORS =
      ":simple,explodedNumerator,explodedDenominator,indicatorType[factor]";

  private static final String FIELDS_DATA_ELEMENT_CAT_COMBOS =
      ":simple,isDefault,categories~pluck[id],"
          + "categoryOptionCombos[id,code,name,displayName,categoryOptions~pluck[id]]";

  private static final String FIELDS_DATA_SET_CAT_COMBOS = ":simple,isDefault,categories~pluck[id]";

  private static final String FIELDS_CATEGORIES = ":simple,categoryOptions~pluck[id]";

  private static final String FIELDS_CATEGORY_OPTIONS = ":simple,organisationUnits~pluck[id]";

  private static final String FIELDS_OPTION_SETS = ":simple,options[id,code,displayName]";

  private final FieldFilterService fieldFilterService;

  private final IdentifiableObjectManager idObjectManager;

  private final CategoryService categoryService;

  private final DataSetService dataSetService;

  private final ExpressionService expressionService;

  private final DataSetMetadataStore dataSetMetadataStore;

  /** The DHIS2 JSON mapper, so streamed dates/numbers match the controller's serialization. */
  private static final ObjectMapper JSON_MAPPER = JacksonObjectMapperConfig.jsonMapper;

  private static final String CATEGORY_OPTION_DIMENSION_ITEM_TYPE = "CATEGORY_OPTION";

  private static final String CATEGORY_DIMENSION_TYPE = "CATEGORY";

  private static final String DEFAULT_NAME = "default";

  @Override
  public ObjectNode getDataSetMetadata() {
    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    SetValuedMap<String, String> dataSetOrgUnits =
        dataSetService.getDataSetOrganisationUnitsAssociations();

    List<DataSet> dataSets = idObjectManager.getDataWriteAll(DataSet.class);
    List<DataElement> dataElements =
        sortById(new HashSet<>(dataSetService.getDataElementsByDataSet(dataSets)));
    List<Indicator> indicators = sortById(flatMapToSet(dataSets, DataSet::getIndicators));
    List<CategoryCombo> dataElementCategoryCombos =
        sortById(flatMapToSet(dataElements, DataElement::getCategoryCombos));
    List<CategoryCombo> dataSetCategoryCombos =
        sortById(mapToSet(dataSets, DataSet::getCategoryCombo));

    // Preload the data-element category combos' associations, so that neither reading their
    // categories below nor field-filter serialisation of categories~pluck[id] and
    // categoryOptionCombos[...,categoryOptions~pluck[id]] initialises them one parent at a time.
    categoryService.preloadCategoryComboAssociations(dataElementCategoryCombos);
    List<Category> dataElementCategories =
        sortById(flatMapToSet(dataElementCategoryCombos, CategoryCombo::getCategories));
    List<Category> dataSetCategories =
        sortById(flatMapToSet(dataSetCategoryCombos, CategoryCombo::getCategories));
    List<Category> categories = distinctUnion(dataElementCategories, dataSetCategories);
    List<CategoryOption> categoryOptions =
        sortById(getCategoryOptions(dataElementCategories, dataSetCategories, currentUserDetails));
    List<OptionSet> optionSets = sortById(getOptionSets(dataElements));

    dataSetCategoryCombos.removeAll(dataElementCategoryCombos);

    expressionService.substituteIndicatorExpressions(indicators);

    ObjectNode rootNode = fieldFilterService.createObjectNode();

    rootNode
        .putArray(DataSetSchemaDescriptor.PLURAL)
        .addAll(toDataSetObjectNodes(dataSets, dataSetOrgUnits));
    rootNode
        .putArray(DataElementSchemaDescriptor.PLURAL)
        .addAll(toObjectNodes(dataElements, FIELDS_DATA_ELEMENTS, DataElement.class));
    rootNode
        .putArray(IndicatorSchemaDescriptor.PLURAL)
        .addAll(toObjectNodes(indicators, FIELDS_INDICATORS, Indicator.class));
    rootNode
        .putArray(CategoryComboSchemaDescriptor.PLURAL)
        .addAll(
            toObjectNodes(
                dataElementCategoryCombos, FIELDS_DATA_ELEMENT_CAT_COMBOS, CategoryCombo.class))
        .addAll(
            toObjectNodes(dataSetCategoryCombos, FIELDS_DATA_SET_CAT_COMBOS, CategoryCombo.class));
    rootNode
        .putArray(CategorySchemaDescriptor.PLURAL)
        .addAll(toObjectNodes(categories, FIELDS_CATEGORIES, Category.class));
    rootNode
        .putArray(CategoryOptionSchemaDescriptor.PLURAL)
        .addAll(toObjectNodes(categoryOptions, FIELDS_CATEGORY_OPTIONS, CategoryOption.class));
    rootNode
        .putArray(OptionSetSchemaDescriptor.PLURAL)
        .addAll(toObjectNodes(optionSets, FIELDS_OPTION_SETS, OptionSet.class));

    return rootNode;
  }

  @Override
  public void writeDataSetMetadata(OutputStream out) throws IOException {
    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    Locale locale = UserSettings.getCurrentSettings().getUserDbLocale();

    SetValuedMap<String, String> dataSetOrgUnits =
        dataSetService.getDataSetOrganisationUnitsAssociations();

    List<DataSet> dataSets = idObjectManager.getDataWriteAll(DataSet.class);
    List<DataElement> dataElements =
        sortById(new HashSet<>(dataSetService.getDataElementsByDataSet(dataSets)));
    List<Indicator> indicators = sortById(flatMapToSet(dataSets, DataSet::getIndicators));
    List<OptionSet> optionSets = sortById(getOptionSets(dataElements));

    // Category-combo scope, derived exactly as the legacy path does (data element combos expose
    // their option combos, data set combos do not). Only their ids are read here; the category
    // graph itself is loaded flat by the projection store, never traversed as a lazy entity graph.
    List<CategoryCombo> dataElementCategoryCombos =
        sortById(flatMapToSet(dataElements, DataElement::getCategoryCombos));
    List<CategoryCombo> dataSetCategoryCombos =
        sortById(mapToSet(dataSets, DataSet::getCategoryCombo));

    List<Long> deComboIds = ids(dataElementCategoryCombos);
    List<Long> dsComboIds = ids(dataSetCategoryCombos);
    List<Long> dsOnlyComboIds = new ArrayList<>(dsComboIds);
    dsOnlyComboIds.removeAll(deComboIds);

    Set<Long> deComboIdSet = new HashSet<>(deComboIds);
    Set<Long> dsComboIdSet = new HashSet<>(dsComboIds);

    // combo -> ordered categories (id + uid), split into data element and data set categories to
    // mirror the legacy distinctUnion(dataElementCategories, dataSetCategories).
    Map<Long, List<String>> comboCategoryUids = new LinkedHashMap<>();
    LinkedHashSet<Long> deCategoryIds = new LinkedHashSet<>();
    LinkedHashSet<Long> dsCategoryIds = new LinkedHashSet<>();
    Map<Long, String> categoryUidById = new LinkedHashMap<>();
    for (Assoc a :
        dataSetMetadataStore.getComboCategories(toArray(union(deComboIds, dsComboIds)))) {
      comboCategoryUids.computeIfAbsent(a.parentId(), k -> new ArrayList<>()).add(a.childUid());
      categoryUidById.put(a.childId(), a.childUid());
      if (deComboIdSet.contains(a.parentId())) {
        deCategoryIds.add(a.childId());
      }
      if (dsComboIdSet.contains(a.parentId())) {
        dsCategoryIds.add(a.childId());
      }
    }
    List<Long> deCategoryIdList = sortedLongs(deCategoryIds);
    List<Long> dsCategoryIdList = sortedLongs(dsCategoryIds);

    // categories array order: data element categories (by id), then data set categories not already
    // present (by id) -- distinctUnion semantics.
    List<Long> categoryArrayIds = new ArrayList<>(deCategoryIdList);
    Set<Long> seenCategoryIds = new HashSet<>(deCategoryIdList);
    for (Long id : dsCategoryIdList) {
      if (seenCategoryIds.add(id)) {
        categoryArrayIds.add(id);
      }
    }

    // category -> ordered options (id + uid), for the categoryOptions~pluck and to gather the
    // option
    // ids of data element categories (which are included unconditionally).
    Map<Long, List<String>> categoryOptionUids = new LinkedHashMap<>();
    Map<Long, List<Long>> categoryOptionIds = new LinkedHashMap<>();
    for (Assoc a : dataSetMetadataStore.getCategoryOptions(toArray(categoryArrayIds))) {
      categoryOptionUids.computeIfAbsent(a.parentId(), k -> new ArrayList<>()).add(a.childUid());
      categoryOptionIds.computeIfAbsent(a.parentId(), k -> new ArrayList<>()).add(a.childId());
    }

    // category options array = all options of data element categories (unconditional) UNION the
    // data-write-accessible options of data set categories.
    LinkedHashSet<Long> optionArrayIds = new LinkedHashSet<>();
    for (Long catId : deCategoryIdList) {
      optionArrayIds.addAll(categoryOptionIds.getOrDefault(catId, List.of()));
    }
    if (!dsCategoryIdList.isEmpty()) {
      List<String> dsCategoryUids =
          dsCategoryIdList.stream().map(categoryUidById::get).collect(Collectors.toList());
      for (Category category : idObjectManager.getByUid(Category.class, dsCategoryUids)) {
        for (CategoryOption option :
            categoryService.getDataWriteCategoryOptions(category, currentUserDetails)) {
          optionArrayIds.add(option.getId());
        }
      }
    }
    List<Long> optionArrayIdList = sortedLongs(optionArrayIds);

    // option combos of the data element combos: the COC list per combo, and each COC's option uids.
    Map<Long, List<CocRow>> cocsByCombo = new LinkedHashMap<>();
    for (CocRow coc : dataSetMetadataStore.getOptionCombos(toArray(deComboIds))) {
      cocsByCombo.computeIfAbsent(coc.comboId(), k -> new ArrayList<>()).add(coc);
    }
    Map<Long, List<String>> cocOptionUids = new LinkedHashMap<>();
    for (Assoc a : dataSetMetadataStore.getOptionComboOptions(toArray(deComboIds))) {
      cocOptionUids.computeIfAbsent(a.parentId(), k -> new ArrayList<>()).add(a.childUid());
    }

    // scalar rows keyed by id
    Map<Long, ComboRow> comboRows = new LinkedHashMap<>();
    for (ComboRow r : dataSetMetadataStore.getCombos(toArray(union(deComboIds, dsOnlyComboIds)))) {
      comboRows.put(r.id(), r);
    }
    Map<Long, CategoryRow> categoryRows = new LinkedHashMap<>();
    for (CategoryRow r : dataSetMetadataStore.getCategories(toArray(categoryArrayIds))) {
      categoryRows.put(r.id(), r);
    }
    Map<Long, OptionRow> optionRows = new LinkedHashMap<>();
    for (OptionRow r : dataSetMetadataStore.getOptions(toArray(optionArrayIdList))) {
      optionRows.put(r.id(), r);
    }
    Map<Long, List<String>> optionOrgUnitUids = new LinkedHashMap<>();
    for (Assoc a : dataSetMetadataStore.getOptionOrgUnits(toArray(optionArrayIdList))) {
      optionOrgUnitUids.computeIfAbsent(a.parentId(), k -> new ArrayList<>()).add(a.childUid());
    }

    expressionService.substituteIndicatorExpressions(indicators);

    try (JsonGenerator gen = JSON_MAPPER.getFactory().createGenerator(out)) {
      gen.writeStartObject();

      gen.writeArrayFieldStart(DataSetSchemaDescriptor.PLURAL);
      for (ObjectNode node : toDataSetObjectNodes(dataSets, dataSetOrgUnits)) {
        gen.writeTree(node);
      }
      gen.writeEndArray();

      writeNodeArray(
          gen,
          DataElementSchemaDescriptor.PLURAL,
          toObjectNodes(dataElements, FIELDS_DATA_ELEMENTS, DataElement.class));

      writeNodeArray(
          gen,
          IndicatorSchemaDescriptor.PLURAL,
          toObjectNodes(indicators, FIELDS_INDICATORS, Indicator.class));

      gen.writeArrayFieldStart(CategoryComboSchemaDescriptor.PLURAL);
      for (Long comboId : deComboIds) {
        writeCombo(
            gen,
            comboRows.get(comboId),
            comboCategoryUids.get(comboId),
            cocsByCombo.get(comboId),
            cocOptionUids,
            locale,
            true);
      }
      for (Long comboId : dsOnlyComboIds) {
        writeCombo(
            gen,
            comboRows.get(comboId),
            comboCategoryUids.get(comboId),
            null,
            cocOptionUids,
            locale,
            false);
      }
      gen.writeEndArray();

      gen.writeArrayFieldStart(CategorySchemaDescriptor.PLURAL);
      for (Long categoryId : categoryArrayIds) {
        writeCategory(
            gen, categoryRows.get(categoryId), categoryOptionUids.get(categoryId), locale);
      }
      gen.writeEndArray();

      gen.writeArrayFieldStart(CategoryOptionSchemaDescriptor.PLURAL);
      for (Long optionId : optionArrayIdList) {
        writeOption(gen, optionRows.get(optionId), optionOrgUnitUids.get(optionId), locale);
      }
      gen.writeEndArray();

      writeNodeArray(
          gen,
          OptionSetSchemaDescriptor.PLURAL,
          toObjectNodes(optionSets, FIELDS_OPTION_SETS, OptionSet.class));

      gen.writeEndObject();
      gen.flush();
    }
  }

  private void writeNodeArray(JsonGenerator gen, String field, List<ObjectNode> nodes)
      throws IOException {
    gen.writeArrayFieldStart(field);
    for (ObjectNode node : nodes) {
      gen.writeTree(node);
    }
    gen.writeEndArray();
  }

  private void writeCombo(
      JsonGenerator gen,
      ComboRow combo,
      List<String> categoryUids,
      List<CocRow> optionCombos,
      Map<Long, List<String>> cocOptionUids,
      Locale locale,
      boolean withOptionCombos)
      throws IOException {
    Set<Translation> translations = parseTranslations(combo.translations());
    gen.writeStartObject();
    gen.writeStringField("id", combo.uid());
    writeIfPresent(gen, "code", combo.code());
    gen.writeStringField("name", combo.name());
    gen.writeObjectField("created", combo.created());
    gen.writeObjectField("lastUpdated", combo.lastUpdated());
    gen.writeStringField("dataDimensionType", combo.dataDimensionType());
    gen.writeBooleanField("skipTotal", combo.skipTotal());
    gen.writeBooleanField("favorite", false);
    gen.writeStringField("displayName", translate(translations, "NAME", combo.name(), locale));
    gen.writeBooleanField("isDefault", DEFAULT_NAME.equals(combo.name()));
    writeUidArray(gen, "categories", categoryUids);
    if (withOptionCombos) {
      gen.writeArrayFieldStart("categoryOptionCombos");
      if (optionCombos != null) {
        for (CocRow coc : optionCombos) {
          writeCoc(gen, coc, cocOptionUids.get(coc.id()), locale);
        }
      }
      gen.writeEndArray();
    }
    gen.writeEndObject();
  }

  private void writeCoc(JsonGenerator gen, CocRow coc, List<String> optionUids, Locale locale)
      throws IOException {
    Set<Translation> translations = parseTranslations(coc.translations());
    gen.writeStartObject();
    gen.writeStringField("id", coc.uid());
    writeIfPresent(gen, "code", coc.code());
    writeIfPresent(gen, "name", coc.name());
    writeIfPresent(gen, "displayName", translate(translations, "NAME", coc.name(), locale));
    writeUidArray(gen, "categoryOptions", optionUids);
    gen.writeEndObject();
  }

  private void writeCategory(
      JsonGenerator gen, CategoryRow category, List<String> optionUids, Locale locale)
      throws IOException {
    Set<Translation> translations = parseTranslations(category.translations());
    String displayName = translate(translations, "NAME", category.name(), locale);
    gen.writeStartObject();
    gen.writeStringField("id", category.uid());
    writeIfPresent(gen, "code", category.code());
    gen.writeStringField("name", category.name());
    gen.writeStringField("shortName", category.shortName());
    gen.writeObjectField("created", category.created());
    gen.writeObjectField("lastUpdated", category.lastUpdated());
    gen.writeBooleanField("dataDimension", category.dataDimension());
    gen.writeStringField("dataDimensionType", category.dataDimensionType());
    gen.writeBooleanField("allItems", false);
    gen.writeStringField("dimension", category.uid());
    gen.writeStringField("dimensionType", CATEGORY_DIMENSION_TYPE);
    gen.writeStringField("displayName", displayName);
    gen.writeStringField(
        "displayShortName", translate(translations, "SHORT_NAME", category.shortName(), locale));
    gen.writeStringField(
        "displayFormName", translate(translations, "FORM_NAME", displayName, locale));
    gen.writeBooleanField("favorite", false);
    writeUidArray(gen, "categoryOptions", optionUids);
    gen.writeEndObject();
  }

  private void writeOption(
      JsonGenerator gen, OptionRow option, List<String> orgUnitUids, Locale locale)
      throws IOException {
    Set<Translation> translations = parseTranslations(option.translations());
    String displayName = translate(translations, "NAME", option.name(), locale);
    String formNameFallback =
        (option.formName() != null && !option.formName().isEmpty())
            ? option.formName()
            : displayName;
    gen.writeStartObject();
    gen.writeStringField("id", option.uid());
    writeIfPresent(gen, "code", option.code());
    gen.writeStringField("name", option.name());
    gen.writeStringField("shortName", option.shortName());
    gen.writeObjectField("created", option.created());
    gen.writeObjectField("lastUpdated", option.lastUpdated());
    gen.writeStringField("dimensionItemType", CATEGORY_OPTION_DIMENSION_ITEM_TYPE);
    gen.writeStringField("dimensionItem", option.uid());
    gen.writeBooleanField("isDefault", DEFAULT_NAME.equals(option.name()));
    gen.writeStringField("displayName", displayName);
    gen.writeStringField(
        "displayShortName", translate(translations, "SHORT_NAME", option.shortName(), locale));
    gen.writeStringField(
        "displayFormName", translate(translations, "FORM_NAME", formNameFallback, locale));
    gen.writeBooleanField("favorite", false);
    writeUidArray(gen, "organisationUnits", orgUnitUids);
    gen.writeEndObject();
  }

  private static void writeIfPresent(JsonGenerator gen, String field, String value)
      throws IOException {
    if (value != null) {
      gen.writeStringField(field, value);
    }
  }

  private static void writeUidArray(JsonGenerator gen, String field, List<String> uids)
      throws IOException {
    gen.writeArrayFieldStart(field);
    if (uids != null) {
      for (String uid : uids) {
        gen.writeString(uid);
      }
    }
    gen.writeEndArray();
  }

  private static Set<Translation> parseTranslations(String json) {
    if (json == null || json.isEmpty() || "[]".equals(json) || "{}".equals(json)) {
      return Set.of();
    }
    try {
      return JSON_MAPPER.readValue(json, new TypeReference<Set<Translation>>() {});
    } catch (IOException e) {
      return Set.of();
    }
  }

  /**
   * Resolves a translated value for the given property from a raw translations set, replicating
   * {@link org.hisp.dhis.common.BaseIdentifiableObject#getTranslation}.
   */
  private static String translate(
      Set<Translation> translations, String key, String defaultValue, Locale locale) {
    if (locale == null || translations.isEmpty()) {
      return defaultValue;
    }
    for (Translation t : translations) {
      if (locale.equals(t.getLocale())
          && key.equalsIgnoreCase(t.getProperty())
          && t.getValue() != null
          && !t.getValue().isEmpty()) {
        return t.getValue();
      }
    }
    return defaultValue;
  }

  private static List<Long> ids(Collection<? extends IdentifiableObject> objects) {
    List<Long> ids = new ArrayList<>(objects.size());
    for (IdentifiableObject object : objects) {
      ids.add(object.getId());
    }
    return ids;
  }

  private static List<Long> sortedLongs(Collection<Long> ids) {
    List<Long> sorted = new ArrayList<>(ids);
    Collections.sort(sorted);
    return sorted;
  }

  private static List<Long> union(Collection<Long> a, Collection<Long> b) {
    LinkedHashSet<Long> set = new LinkedHashSet<>(a);
    set.addAll(b);
    return new ArrayList<>(set);
  }

  private static long[] toArray(Collection<Long> ids) {
    return DataSetMetadataStore.toArray(ids);
  }

  @Override
  public Date getDataSetMetadataLastModified() {
    return ObjectUtils.firstNonNull(
        DateUtils.max(
            METADATA_TYPES.stream()
                .map(idObjectManager::getLastUpdated)
                .collect(Collectors.toList())),
        new Date());
  }

  /**
   * Returns category options for the given data element and data set categories. For the data set
   * categories, only category options which the current user has data write access to are returned.
   *
   * @param dataElementCategories the data element categories.
   * @param dataSetCategories the data set categories.
   * @param user the current user.
   * @return a set of {@link CategoryOption}.
   */
  private Set<CategoryOption> getCategoryOptions(
      Collection<Category> dataElementCategories,
      Collection<Category> dataSetCategories,
      UserDetails userDetails) {
    Set<CategoryOption> options = flatMapToSet(dataElementCategories, Category::getCategoryOptions);
    dataSetCategories.forEach(
        c -> options.addAll(categoryService.getDataWriteCategoryOptions(c, userDetails)));
    return options;
  }

  /**
   * Returns option sets for the given data elements.
   *
   * @param dataElements the collection of data elements.
   * @return a set of {@link OptionSet}.
   */
  private Set<OptionSet> getOptionSets(Collection<DataElement> dataElements) {
    Set<OptionSet> optionSets = new HashSet<>();
    dataElements.forEach(
        de -> {
          addIfNotNull(optionSets, de.getOptionSet());
          addIfNotNull(optionSets, de.getCommentOptionSet());
        });
    return optionSets;
  }

  /**
   * Returns data sets as a list of {@link ObjectNode}. Includes associations to organisation units.
   *
   * @param dataSets the collection of {@link DataSet}.
   * @param dataSetOrgUnits the associations between data sets and organisation units.
   * @return a list of {@link ObjectNode}
   */
  private List<ObjectNode> toDataSetObjectNodes(
      Collection<DataSet> dataSets, SetValuedMap<String, String> dataSetOrgUnits) {
    List<ObjectNode> objectNodes = new ArrayList<>();

    for (DataSet dataSet : dataSets) {
      ObjectNode objectNode = fieldFilterService.toObjectNode(dataSet, FIELDS_DATA_SETS);
      objectNode.set(
          PROPERTY_DATA_SET_ELEMENTS, toDataSetElementsArrayNode(dataSet.getDataSetElements()));
      objectNode.set(PROPERTY_ORGANISATION_UNITS, toOrgUnitsArrayNode(dataSet, dataSetOrgUnits));
      objectNodes.add(objectNode);
    }

    return objectNodes;
  }

  /**
   * Returns data set elements as an {@link ArrayNode}. The data set elements are sorted by
   * identifier to provide a stable order.
   *
   * @param dataSetElements the set of {@link DataSetElement}.
   * @return an {@link ArrayNode}.
   */
  private ArrayNode toDataSetElementsArrayNode(Set<DataSetElement> dataSetElements) {
    List<DataSetElement> elements = toDataSetElementList(dataSetElements);
    ArrayNode arrayNode = fieldFilterService.createArrayNode();
    List<ObjectNode> objectNodes =
        toObjectNodes(elements, FIELDS_DATA_SET_ELEMENTS, DataSetElement.class);
    objectNodes.forEach(arrayNode::add);
    return arrayNode;
  }

  /**
   * Returns a list of {@link DataSetElement} sorted by identifier to provide a stable order.
   *
   * @param dataSetElements the set of {@link DataSetElement}.
   * @return a list of {@link DataSetElement}.
   */
  private List<DataSetElement> toDataSetElementList(Set<DataSetElement> dataSetElements) {
    List<DataSetElement> elements = new ArrayList<>(dataSetElements);
    Collections.sort(elements, Comparator.comparingLong(DataSetElement::getId));
    return elements;
  }

  /**
   * Returns organisation unit associations for the given data set as an {@link ArrayNode}.
   *
   * @param dataSet the {@link DataSet}.
   * @param dataSetOrgUnits the associations between data sets and organisation units.
   * @return an {@link ArrayNode}.
   */
  private ArrayNode toOrgUnitsArrayNode(
      DataSet dataSet, SetValuedMap<String, String> dataSetOrgUnits) {
    ArrayNode arrayNode = fieldFilterService.createArrayNode();
    Set<String> orgUnits = dataSetOrgUnits.get(dataSet.getUid());
    orgUnits.forEach(arrayNode::add);
    return arrayNode;
  }

  /**
   * Returns the given collection of objects as an {@link ObjectNode}.
   *
   * @param <T>
   * @param objects the collection of objects.
   * @param filters the filters to apply.
   * @param type the class type.
   * @return a list of {@link ObjectNode}.
   */
  private <T> List<ObjectNode> toObjectNodes(Collection<T> objects, String filters, Class<T> type) {
    return fieldFilterService.toObjectNodes(
        FieldFilterParams.of(new ArrayList<>(objects), filters, true));
  }
}
