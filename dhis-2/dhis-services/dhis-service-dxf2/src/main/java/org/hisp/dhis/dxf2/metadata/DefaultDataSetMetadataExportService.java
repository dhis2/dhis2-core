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
import java.util.HashMap;
import java.util.HashSet;
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
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataset.DataInputPeriod;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.Bulk;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.Col;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.Computed;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.Constant;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.Def;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.DerivedColumn;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.Field;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.Nested;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.Pluck;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.Ref;
import org.hisp.dhis.dxf2.metadata.MetadataProjection.Translated;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.schema.descriptors.CategoryComboSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategoryOptionSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.CategorySchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DataSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IndicatorSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.OptionSetSchemaDescriptor;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
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

    List<DataSet> dataSets = idObjectManager.getDataWriteAll(DataSet.class);
    List<DataElement> dataElements =
        sortById(new HashSet<>(dataSetService.getDataElementsByDataSet(dataSets)));
    List<Indicator> indicators = sortById(flatMapToSet(dataSets, DataSet::getIndicators));

    // Option sets referenced by the data elements (own option set + comment option set), read as
    // ids only and loaded flat by the projection store.
    LinkedHashSet<Long> optionSetIds = new LinkedHashSet<>();
    for (DataElement dataElement : dataElements) {
      if (dataElement.getOptionSet() != null) {
        optionSetIds.add(dataElement.getOptionSet().getId());
      }
      if (dataElement.getCommentOptionSet() != null) {
        optionSetIds.add(dataElement.getCommentOptionSet().getId());
      }
    }
    List<Long> optionSetIdList = sortedLongs(optionSetIds);

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

    // combo -> category ids, split into data element and data set categories to mirror the legacy
    // distinctUnion(dataElementCategories, dataSetCategories).
    Set<Long> deComboIdSet = new HashSet<>(deComboIds);
    Set<Long> dsComboIdSet = new HashSet<>(dsComboIds);
    LinkedHashSet<Long> deCategoryIds = new LinkedHashSet<>();
    LinkedHashSet<Long> dsCategoryIds = new LinkedHashSet<>();
    dataSetMetadataStore
        .idLists(COMBO_CATEGORY_IDS_SQL, toArray(union(deComboIds, dsComboIds)))
        .forEach(
            (comboId, categoryIds) -> {
              if (deComboIdSet.contains(comboId)) {
                deCategoryIds.addAll(categoryIds);
              }
              if (dsComboIdSet.contains(comboId)) {
                dsCategoryIds.addAll(categoryIds);
              }
            });
    List<Long> deCategoryIdList = sortedLongs(deCategoryIds);
    List<Long> dsCategoryIdList = sortedLongs(dsCategoryIds);

    // categories array = data element categories (by id), then data set categories not already
    // present (by id) -- distinctUnion semantics.
    List<Long> categoryArrayIds = new ArrayList<>(deCategoryIdList);
    Set<Long> seenCategoryIds = new HashSet<>(deCategoryIdList);
    for (Long id : dsCategoryIdList) {
      if (seenCategoryIds.add(id)) {
        categoryArrayIds.add(id);
      }
    }

    // category options array = all options of data element categories (unconditional) UNION the
    // data-write-accessible options of data set categories.
    LinkedHashSet<Long> optionArrayIds = new LinkedHashSet<>();
    dataSetMetadataStore
        .idLists(CATEGORY_OPTION_IDS_SQL, toArray(deCategoryIdList))
        .values()
        .forEach(optionArrayIds::addAll);
    if (!dsCategoryIdList.isEmpty()) {
      List<String> dsCategoryUids =
          dataSetMetadataStore
              .loadRows(Category.class, List.of("uid"), List.of(), toArray(dsCategoryIdList))
              .values()
              .stream()
              .map(MetadataProjection.Row::uid)
              .collect(Collectors.toList());
      for (Category category : idObjectManager.getByUid(Category.class, dsCategoryUids)) {
        for (CategoryOption option :
            categoryService.getDataWriteCategoryOptions(category, currentUserDetails)) {
          optionArrayIds.add(option.getId());
        }
      }
    }
    List<Long> optionArrayIdList = sortedLongs(optionArrayIds);

    expressionService.substituteIndicatorExpressions(indicators);

    // Exploded numerator/denominator are derived by the expression engine from the (bounded) set of
    // indicators referenced by the data sets; the projection then streams them as bulk-resolved
    // scalars rather than reading a column. Everything else is read flat from SQL.
    Map<Long, Object> explodedNumerators = new HashMap<>();
    Map<Long, Object> explodedDenominators = new HashMap<>();
    for (Indicator indicator : indicators) {
      explodedNumerators.put(indicator.getId(), indicator.getExplodedNumerator());
      explodedDenominators.put(indicator.getId(), indicator.getExplodedDenominator());
    }

    List<Long> dataSetIds = ids(dataSets);
    List<Long> dataElementIds = ids(dataElements);
    List<Long> indicatorIds = ids(indicators);

    // resolves each def's table and columns from the Hibernate mapping (no physical column names)
    MetadataProjection.RowLoader rowLoader = dataSetMetadataStore::loadRows;

    try (JsonGenerator gen = JSON_MAPPER.getFactory().createGenerator(out)) {
      gen.writeStartObject();

      gen.writeArrayFieldStart(DataSetSchemaDescriptor.PLURAL);
      MetadataProjection.writeObjects(gen, dataSetDef(), toArray(dataSetIds), locale, rowLoader);
      gen.writeEndArray();

      gen.writeArrayFieldStart(DataElementSchemaDescriptor.PLURAL);
      MetadataProjection.writeObjects(
          gen, dataElementDef(), toArray(dataElementIds), locale, rowLoader);
      gen.writeEndArray();

      gen.writeArrayFieldStart(IndicatorSchemaDescriptor.PLURAL);
      MetadataProjection.writeObjects(
          gen,
          indicatorDef(explodedNumerators, explodedDenominators),
          toArray(indicatorIds),
          locale,
          rowLoader);
      gen.writeEndArray();

      gen.writeArrayFieldStart(CategoryComboSchemaDescriptor.PLURAL);
      MetadataProjection.writeObjects(
          gen, categoryComboDef(true), toArray(deComboIds), locale, rowLoader);
      MetadataProjection.writeObjects(
          gen, categoryComboDef(false), toArray(dsOnlyComboIds), locale, rowLoader);
      gen.writeEndArray();

      gen.writeArrayFieldStart(CategorySchemaDescriptor.PLURAL);
      MetadataProjection.writeObjects(
          gen, categoryDef(), toArray(categoryArrayIds), locale, rowLoader);
      gen.writeEndArray();

      gen.writeArrayFieldStart(CategoryOptionSchemaDescriptor.PLURAL);
      MetadataProjection.writeObjects(
          gen, categoryOptionDef(), toArray(optionArrayIdList), locale, rowLoader);
      gen.writeEndArray();

      gen.writeArrayFieldStart(OptionSetSchemaDescriptor.PLURAL);
      MetadataProjection.writeObjects(
          gen, optionSetDef(), toArray(optionSetIdList), locale, rowLoader);
      gen.writeEndArray();

      gen.writeEndObject();
      gen.flush();
    }
  }

  // --- projection definitions: one declarative field map per type for the generic engine --------

  private static final String COMBO_CATEGORY_IDS_SQL =
      """
      select cc.categorycomboid, c.categoryid from categorycombos_categories cc
      join category c on c.categoryid = cc.categoryid
      where cc.categorycomboid = any(?) order by cc.categorycomboid, cc.sort_order
      """;
  private static final String CATEGORY_OPTION_IDS_SQL =
      """
      select cco.categoryid, cco.categoryoptionid from categories_categoryoptions cco
      where cco.categoryid = any(?) order by cco.categoryid, cco.sort_order
      """;
  private static final String COMBO_CATEGORY_UIDS_SQL =
      """
      select cc.categorycomboid, c.uid from categorycombos_categories cc
      join category c on c.categoryid = cc.categoryid
      where cc.categorycomboid = any(?) order by cc.categorycomboid, cc.sort_order
      """;
  private static final String CATEGORY_OPTION_UIDS_SQL =
      """
      select cco.categoryid, o.uid from categories_categoryoptions cco
      join categoryoption o on o.categoryoptionid = cco.categoryoptionid
      where cco.categoryid = any(?) order by cco.categoryid, cco.sort_order
      """;
  private static final String COC_OPTION_UIDS_SQL =
      """
      select cocco.categoryoptioncomboid, o.uid from categoryoptioncombos_categoryoptions cocco
      join categoryoption o on o.categoryoptionid = cocco.categoryoptionid
      where cocco.categoryoptioncomboid = any(?)
      order by cocco.categoryoptioncomboid, o.categoryoptionid
      """;
  private static final String COMBO_COC_IDS_SQL =
      """
      select link.categorycomboid, coc.categoryoptioncomboid from categorycombos_optioncombos link
      join categoryoptioncombo coc on coc.categoryoptioncomboid = link.categoryoptioncomboid
      where link.categorycomboid = any(?) order by link.categorycomboid, coc.categoryoptioncomboid
      """;
  private static final String OPTION_ORG_UNIT_UIDS_SQL =
      """
      select coou.categoryoptionid, ou.uid from categoryoption_organisationunits coou
      join organisationunit ou on ou.organisationunitid = coou.organisationunitid
      where coou.categoryoptionid = any(?) order by coou.categoryoptionid, ou.organisationunitid
      """;
  private static final String OPTION_SET_OPTION_IDS_SQL =
      """
      select optionsetid, optionvalueid from optionvalue
      where optionsetid = any(?) order by optionsetid, sort_order
      """;
  private static final String DATA_SET_SECTION_IDS_SQL =
      """
      select datasetid, sectionid from section
      where datasetid = any(?) order by datasetid, sortorder
      """;
  private static final String DATA_SET_INPUT_PERIOD_IDS_SQL =
      """
      select datasetid, datainputperiodid from datainputperiod
      where datasetid = any(?) order by datasetid, datainputperiodid
      """;
  private static final String DATA_SET_COMPULSORY_OPERAND_IDS_SQL =
      """
      select datasetid, dataelementoperandid from datasetoperands
      where datasetid = any(?) order by datasetid, dataelementoperandid
      """;
  private static final String DATA_SET_ELEMENT_IDS_SQL =
      """
      select datasetid, datasetelementid from datasetelement
      where datasetid = any(?) order by datasetid, datasetelementid
      """;
  private static final String DATA_SET_INDICATOR_UIDS_SQL =
      """
      select dsi.datasetid, i.uid from datasetindicators dsi
      join indicator i on i.indicatorid = dsi.indicatorid
      where dsi.datasetid = any(?) order by dsi.datasetid, i.indicatorid
      """;
  private static final String DATA_SET_ORG_UNIT_UIDS_SQL =
      """
      select dss.datasetid, ou.uid from datasetsource dss
      join organisationunit ou on ou.organisationunitid = dss.sourceid
      where dss.datasetid = any(?) order by dss.datasetid, ou.organisationunitid
      """;
  private static final String SECTION_DATA_ELEMENT_UIDS_SQL =
      """
      select sde.sectionid, de.uid from sectiondataelements sde
      join dataelement de on de.dataelementid = sde.dataelementid
      where sde.sectionid = any(?) order by sde.sectionid, sde.sort_order
      """;
  private static final String SECTION_INDICATOR_UIDS_SQL =
      """
      select si.sectionid, i.uid from sectionindicators si
      join indicator i on i.indicatorid = si.indicatorid
      where si.sectionid = any(?) order by si.sectionid, si.sort_order
      """;
  private static final String SECTION_GREYED_FIELD_IDS_SQL =
      """
      select sectionid, dataelementoperandid from sectiongreyedfields
      where sectionid = any(?) order by sectionid, dataelementoperandid
      """;

  private Def categoryComboDef(boolean withOptionCombos) {
    List<Field> fields =
        new ArrayList<>(
            List.of(
                new Col("id", "uid"),
                new Col("code", "code"),
                new Col("name", "name"),
                new Col("created", "created"),
                new Col("lastUpdated", "lastUpdated"),
                new Col("dataDimensionType", "dataDimensionType"),
                new Col("skipTotal", "skipTotal"),
                new Translated("displayName", "name", "NAME"),
                isDefault(),
                new Pluck(
                    "categories",
                    ids -> dataSetMetadataStore.uidLists(COMBO_CATEGORY_UIDS_SQL, ids))));
    if (withOptionCombos) {
      fields.add(
          new Nested(
              "categoryOptionCombos",
              categoryOptionComboDef(),
              ids -> dataSetMetadataStore.idLists(COMBO_COC_IDS_SQL, ids)));
    }
    return new Def(CategoryCombo.class, fields);
  }

  private Def categoryOptionComboDef() {
    return new Def(
        CategoryOptionCombo.class,
        List.of(
            new Col("id", "uid"),
            new Col("code", "code"),
            new Col("name", "name"),
            new Translated("displayName", "name", "NAME"),
            new Pluck(
                "categoryOptions",
                ids -> dataSetMetadataStore.uidLists(COC_OPTION_UIDS_SQL, ids))));
  }

  private Def categoryDef() {
    return new Def(
        Category.class,
        List.of(
            new Col("id", "uid"),
            new Col("code", "code"),
            new Col("name", "name"),
            new Col("shortName", "shortName"),
            new Col("description", "description"),
            new Col("created", "created"),
            new Col("lastUpdated", "lastUpdated"),
            new Col("dataDimension", "dataDimension"),
            new Col("dataDimensionType", "dataDimensionType"),
            new Constant("allItems", false),
            new Computed("dimension", List.of("uid"), (r, l) -> r.uid()),
            new Translated("displayName", "name", "NAME"),
            new Translated("displayShortName", "shortName", "SHORT_NAME"),
            new Translated("displayDescription", "description", "DESCRIPTION"),
            isDefault(),
            new Pluck(
                "categoryOptions",
                ids -> dataSetMetadataStore.uidLists(CATEGORY_OPTION_UIDS_SQL, ids))));
  }

  private Def categoryOptionDef() {
    return new Def(
        CategoryOption.class,
        List.of(
            new Col("id", "uid"),
            new Col("code", "code"),
            new Col("name", "name"),
            new Col("shortName", "shortName"),
            new Col("formName", "formName"),
            new Col("description", "description"),
            new Col("startDate", "startDate"),
            new Col("endDate", "endDate"),
            new Col("created", "created"),
            new Col("lastUpdated", "lastUpdated"),
            new Constant("dimensionItemType", "CATEGORY_OPTION"),
            new Computed("dimensionItem", List.of("uid"), (r, l) -> r.uid()),
            isDefault(),
            new Translated("displayName", "name", "NAME"),
            new Translated("displayShortName", "shortName", "SHORT_NAME"),
            new Translated("displayDescription", "description", "DESCRIPTION"),
            displayFormName("formName", "name"),
            new Pluck(
                "organisationUnits",
                ids -> dataSetMetadataStore.uidLists(OPTION_ORG_UNIT_UIDS_SQL, ids))));
  }

  private Def optionSetDef() {
    return new Def(
        OptionSet.class,
        List.of(
            new Col("id", "uid"),
            new Col("code", "code"),
            new Col("name", "name"),
            new Col("description", "description"),
            new Col("created", "created"),
            new Col("lastUpdated", "lastUpdated"),
            new Col("valueType", "valueType"),
            new Col("version", "version"),
            new Translated("displayName", "name", "NAME"),
            new Nested(
                "options",
                optionValueDef(),
                ids -> dataSetMetadataStore.idLists(OPTION_SET_OPTION_IDS_SQL, ids))));
  }

  private Def optionValueDef() {
    return new Def(
        Option.class,
        List.of(
            new Col("id", "uid"),
            new Col("code", "code"),
            new Translated("displayName", "name", "NAME")));
  }

  private Def dataElementDef() {
    return new Def(
        DataElement.class,
        List.of(
            new Col("id", "uid"),
            new Col("code", "code"),
            new Col("name", "name"),
            new Col("created", "created"),
            new Col("lastUpdated", "lastUpdated"),
            new Col("description", "description"),
            new Col("valueType", "valueType"),
            new Col("aggregationType", "aggregationType"),
            new Col("zeroIsSignificant", "zeroIsSignificant"),
            new Translated("displayName", "name", "NAME"),
            new Translated("displayShortName", "shortName", "SHORT_NAME"),
            displayFormName("formName", "name"),
            new Ref("categoryCombo", "categoryCombo", idOnlyDef(CategoryCombo.class)),
            new Ref("optionSet", "optionSet", idOnlyDef(OptionSet.class)),
            new Ref("commentOptionSet", "commentOptionSet", idOnlyDef(OptionSet.class)),
            new Ref("lastUpdatedBy", "lastUpdatedBy", userDef())));
  }

  private Def indicatorDef(
      Map<Long, Object> explodedNumerators, Map<Long, Object> explodedDenominators) {
    return new Def(
        Indicator.class,
        List.of(
            new Col("id", "uid"),
            new Col("code", "code"),
            new Col("name", "name"),
            new Col("shortName", "shortName"),
            new Col("created", "created"),
            new Col("lastUpdated", "lastUpdated"),
            new Col("description", "description"),
            new Col("annualized", "annualized"),
            new Col("numerator", "numerator"),
            new Col("numeratorDescription", "numeratorDescription"),
            new Col("denominator", "denominator"),
            new Col("denominatorDescription", "denominatorDescription"),
            new Constant("dimensionItemType", "INDICATOR"),
            new Computed("dimensionItem", List.of("uid"), (r, l) -> r.uid()),
            new Translated("displayName", "name", "NAME"),
            new Translated("displayShortName", "shortName", "SHORT_NAME"),
            new Translated("displayDescription", "description", "DESCRIPTION"),
            displayFormName("formName", "name"),
            new Translated(
                "displayNumeratorDescription", "numeratorDescription", "NUMERATOR_DESCRIPTION"),
            new Translated(
                "displayDenominatorDescription",
                "denominatorDescription",
                "DENOMINATOR_DESCRIPTION"),
            new Bulk("explodedNumerator", ids -> explodedNumerators),
            new Bulk("explodedDenominator", ids -> explodedDenominators),
            new Ref("indicatorType", "indicatorType", indicatorTypeDef())));
  }

  private Def dataSetDef() {
    return new Def(
        DataSet.class,
        List.of(
            // periodType serialises as its name, and formType depends on whether sections exist;
            // both live outside the dataset row, so they are the rare genuinely-derived columns.
            new DerivedColumn(
                "periodType",
                "select pt.name from periodtype pt where pt.periodtypeid = t.periodtypeid"),
            new DerivedColumn(
                "hasSections", "exists (select 1 from section s where s.datasetid = t.datasetid)")),
        List.of(
            new Col("id", "uid"),
            new Col("code", "code"),
            new Col("name", "name"),
            new Col("shortName", "shortName"),
            new Col("description", "description"),
            new Col("formName", "formName"),
            // reads the derived periodType-name column (empty deps: not a scalar property, so the
            // periodType FK column is not selected and cannot collide with this alias)
            new Computed("periodType", List.of(), (r, l) -> r.get("periodType")),
            new Col("mobile", "mobile"),
            new Col("version", "version"),
            new Computed(
                "expiryDays", List.of("expiryDays"), (r, l) -> toDouble(r.get("expiryDays"))),
            new Computed(
                "timelyDays", List.of("timelyDays"), (r, l) -> toDouble(r.get("timelyDays"))),
            new Col("notifyCompletingUser", "notifyCompletingUser"),
            new Col("openFuturePeriods", "openFuturePeriods"),
            new Col("openPeriodsAfterCoEndDate", "openPeriodsAfterCoEndDate"),
            new Col("fieldCombinationRequired", "fieldCombinationRequired"),
            new Col("validCompleteOnly", "validCompleteOnly"),
            new Col("noValueRequiresComment", "noValueRequiresComment"),
            new Col("skipOffline", "skipOffline"),
            new Col("dataElementDecoration", "dataElementDecoration"),
            new Col("renderAsTabs", "renderAsTabs"),
            new Col("renderHorizontally", "renderHorizontally"),
            new Col("compulsoryFieldsCompleteOnly", "compulsoryFieldsCompleteOnly"),
            new Col("created", "created"),
            new Col("lastUpdated", "lastUpdated"),
            new Constant("dimensionItemType", "REPORTING_RATE"),
            new Computed("dimensionItem", List.of("uid"), (r, l) -> r.uid()),
            new Translated("displayName", "name", "NAME"),
            new Translated("displayShortName", "shortName", "SHORT_NAME"),
            new Translated("displayDescription", "description", "DESCRIPTION"),
            // DataSet shadows the base formName field, so its displayFormName always falls back to
            // the (translated) name rather than the form name -- mirror that exactly.
            new Computed(
                "displayFormName",
                List.of("name", MetadataProjection.TRANSLATIONS),
                (r, l) ->
                    MetadataProjection.translate(
                        r.translations(),
                        "FORM_NAME",
                        MetadataProjection.translate(r.translations(), "NAME", r.str("name"), l),
                        l)),
            new Computed("formType", List.of("dataEntryForm"), (r, l) -> formType(r)),
            new Ref("categoryCombo", "categoryCombo", idOnlyDef(CategoryCombo.class)),
            new Ref("dataEntryForm", "dataEntryForm", idOnlyDef(DataEntryForm.class)),
            new Pluck(
                "indicators",
                ids -> dataSetMetadataStore.uidLists(DATA_SET_INDICATOR_UIDS_SQL, ids)),
            new Pluck(
                "organisationUnits",
                ids -> dataSetMetadataStore.uidLists(DATA_SET_ORG_UNIT_UIDS_SQL, ids)),
            new Nested(
                "dataInputPeriods",
                dataInputPeriodDef(),
                ids -> dataSetMetadataStore.idLists(DATA_SET_INPUT_PERIOD_IDS_SQL, ids)),
            new Nested(
                "compulsoryDataElementOperands",
                operandDef(),
                ids -> dataSetMetadataStore.idLists(DATA_SET_COMPULSORY_OPERAND_IDS_SQL, ids)),
            new Nested(
                "dataSetElements",
                dataSetElementDef(),
                ids -> dataSetMetadataStore.idLists(DATA_SET_ELEMENT_IDS_SQL, ids)),
            new Nested(
                "sections",
                sectionDef(),
                ids -> dataSetMetadataStore.idLists(DATA_SET_SECTION_IDS_SQL, ids))));
  }

  private Def sectionDef() {
    return new Def(
        Section.class,
        List.of(
            new Col("id", "uid"),
            new Col("name", "name"),
            new Col("created", "created"),
            new Col("lastUpdated", "lastUpdated"),
            new Col("sortOrder", "sortOrder"),
            new Col("showRowTotals", "showRowTotals"),
            new Col("showColumnTotals", "showColumnTotals"),
            new Col("disableDataElementAutoGroup", "disableDataElementAutoGroup"),
            new Translated("displayName", "name", "NAME"),
            new Pluck(
                "dataElements",
                ids -> dataSetMetadataStore.uidLists(SECTION_DATA_ELEMENT_UIDS_SQL, ids)),
            new Pluck(
                "indicators",
                ids -> dataSetMetadataStore.uidLists(SECTION_INDICATOR_UIDS_SQL, ids)),
            new Nested(
                "greyedFields",
                operandDef(),
                ids -> dataSetMetadataStore.idLists(SECTION_GREYED_FIELD_IDS_SQL, ids))));
  }

  private Def dataInputPeriodDef() {
    return new Def(
        DataInputPeriod.class,
        List.of(
            new Ref("period", "period", periodDef()),
            new Col("openingDate", "openingDate"),
            new Col("closingDate", "closingDate")));
  }

  private Def operandDef() {
    return new Def(
        DataElementOperand.class,
        List.of(
            new Ref("dataElement", "dataElement", idOnlyDef(DataElement.class)),
            new Ref(
                "categoryOptionCombo",
                "categoryOptionCombo",
                idOnlyDef(CategoryOptionCombo.class))));
  }

  private Def dataSetElementDef() {
    return new Def(
        DataSetElement.class,
        List.of(
            new Ref("dataElement", "dataElement", idOnlyDef(DataElement.class)),
            new Ref("categoryCombo", "categoryCombo", idOnlyDef(CategoryCombo.class))));
  }

  private Def periodDef() {
    // Period's serialised id is its ISO string, mapped by the isoDate property.
    return new Def(Period.class, List.of(new Col("id", "isoDate")));
  }

  private Def indicatorTypeDef() {
    return new Def(IndicatorType.class, List.of(new Col("factor", "factor")));
  }

  private Def userDef() {
    // The user-property-transformer shape ({id,code,name,displayName,username}); users have no
    // translations, so displayName is just the name.
    return new Def(
        User.class,
        List.of(
            new Col("id", "uid"),
            new Col("code", "code"),
            new Col("name", "name"),
            new Computed("displayName", List.of("name"), (r, l) -> r.str("name")),
            new Col("username", "username")));
  }

  /** A {@code {id}}-only reference object over an entity type (its uid under the JSON key id). */
  private Def idOnlyDef(Class<?> entityType) {
    return new Def(entityType, List.of(new Col("id", "uid")));
  }

  /** {@code isDefault}: true iff the object's name is the reserved default name. */
  private static Field isDefault() {
    return new Computed("isDefault", List.of("name"), (r, l) -> DEFAULT_NAME.equals(r.str("name")));
  }

  /**
   * The nameable {@code displayFormName} rule: the translated form name, falling back to the form
   * name property, then to the (translated) name.
   */
  private static Field displayFormName(String formNameProperty, String nameProperty) {
    return new Computed(
        "displayFormName",
        List.of(formNameProperty, nameProperty, MetadataProjection.TRANSLATIONS),
        (r, l) -> {
          String formName = r.str(formNameProperty);
          String base =
              (formName != null && !formName.isEmpty())
                  ? formName
                  : MetadataProjection.translate(r.translations(), "NAME", r.str(nameProperty), l);
          return MetadataProjection.translate(r.translations(), "FORM_NAME", base, l);
        });
  }

  private static String formType(MetadataProjection.Row row) {
    if (row.get("dataEntryForm") != null) {
      return "CUSTOM";
    }
    return Boolean.TRUE.equals(row.get("hasSections")) ? "SECTION" : "DEFAULT";
  }

  private static Double toDouble(Object value) {
    return value == null ? 0.0 : ((Number) value).doubleValue();
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
