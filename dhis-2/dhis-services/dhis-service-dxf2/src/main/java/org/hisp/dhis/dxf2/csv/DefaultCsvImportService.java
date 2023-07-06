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
package org.hisp.dhis.dxf2.csv;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hisp.dhis.util.DateUtils.getMediumDate;

import com.csvreader.CsvReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupService;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.MissingValueStrategy;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupService;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.option.OptionGroupSet;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.system.util.CsvUtils;
import org.hisp.dhis.validation.Importance;
import org.hisp.dhis.validation.ValidationRule;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@AllArgsConstructor
@Service("org.hisp.dhis.dxf2.csv.CsvImportService")
public class DefaultCsvImportService implements CsvImportService {
  private final CategoryService categoryService;

  private final OrganisationUnitGroupService organisationUnitGroupService;

  private final DataElementGroupService dataElementGroupService;

  private final IndicatorGroupService indicatorGroupService;

  private final OptionService optionService;

  private static final String JSON_GEOM_TEMPL = "{\"type\":\"%s\", \"coordinates\":%s}";

  // -------------------------------------------------------------------------
  // CsvImportService implementation
  // -------------------------------------------------------------------------

  // TODO Add unit tests

  @Override
  public Metadata fromCsv(InputStream input, CsvImportOptions options) throws IOException {
    CsvReader reader = CsvUtils.getReader(input);
    reader.setSafetySwitch(false); // Disabled due to large geometry
    // values for org units

    if (options.isFirstRowIsHeader()) {
      reader.readRecord(); // Ignore first row
    }

    Metadata metadata = new Metadata();

    switch (options.getImportClass()) {
      case DATA_ELEMENT:
        metadata.setDataElements(dataElementsFromCsv(reader));
        break;
      case DATA_ELEMENT_GROUP:
        metadata.setDataElementGroups(dataElementGroupsFromCsv(reader));
        break;
      case DATA_ELEMENT_GROUP_MEMBERSHIP:
        metadata.setDataElementGroups(dataElementGroupMembersFromCsv(reader));
        break;
      case INDICATOR_GROUP_MEMBERSHIP:
        metadata.setIndicatorGroups(indicatorGroupMembersFromCsv(reader));
        break;
      case CATEGORY_OPTION:
        metadata.setCategoryOptions(categoryOptionsFromCsv(reader));
        break;
      case CATEGORY:
        metadata.setCategories(categoriesFromCsv(reader));
        break;
      case CATEGORY_COMBO:
        metadata.setCategoryCombos(categoryCombosFromCsv(reader));
        break;
      case CATEGORY_OPTION_GROUP:
        metadata.setCategoryOptionGroups(categoryOptionGroupsFromCsv(reader));
        break;
      case ORGANISATION_UNIT:
        metadata.setOrganisationUnits(orgUnitsFromCsv(reader));
        break;
      case ORGANISATION_UNIT_GROUP:
        metadata.setOrganisationUnitGroups(orgUnitGroupsFromCsv(reader));
        break;
      case ORGANISATION_UNIT_GROUP_MEMBERSHIP:
        metadata.setOrganisationUnitGroups(orgUnitGroupMembersFromCsv(reader));
        break;
      case VALIDATION_RULE:
        metadata.setValidationRules(validationRulesFromCsv(reader));
        break;
      case OPTION_SET:
        setOptionSetsFromCsv(reader, metadata);
        break;
      case OPTION_GROUP:
        setOptionGroupsFromCsv(reader, metadata);
        break;
      case OPTION_GROUP_SET:
        metadata.setOptionGroupSets(setOptionGroupSetFromCsv(reader));
        break;
      case OPTION_GROUP_SET_MEMBERSHIP:
        metadata.setOptionGroupSets(optionGroupSetMembersFromCsv(reader));
        break;
      default:
        break;
    }

    return metadata;
  }

  // -------------------------------------------------------------------------
  // Private methods
  // -------------------------------------------------------------------------

  private List<DataElement> dataElementsFromCsv(CsvReader reader) throws IOException {
    CategoryCombo categoryCombo = categoryService.getDefaultCategoryCombo();

    List<DataElement> list = new ArrayList<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        DataElement object = new DataElement();
        setIdentifiableObject(object, values);
        object.setShortName(getSafe(values, 3, object.getName(), 50));
        object.setDescription(getSafe(values, 4));
        object.setFormName(getSafe(values, 5, 230));

        String domainType = getSafe(values, 6, DataElementDomain.AGGREGATE.getValue(), 16);
        object.setDomainType(DataElementDomain.fromValue(domainType));
        object.setValueType(
            ValueType.valueOf(getSafe(values, 7, ValueType.INTEGER.toString(), 50)));

        object.setAggregationType(
            AggregationType.valueOf(getSafe(values, 8, AggregationType.SUM.toString(), 50)));
        String categoryComboUid = getSafe(values, 9, 11);
        object.setUrl(getSafe(values, 10, 255));
        object.setZeroIsSignificant(Boolean.parseBoolean(getSafe(values, 11, "false", null)));
        String optionSetUid = getSafe(values, 12, 11);
        String commentOptionSetUid = getSafe(values, 13, 11);
        object.setAutoFields();

        CategoryCombo cc = new CategoryCombo();
        cc.setUid(categoryComboUid);
        cc.setAutoFields();

        if (categoryComboUid == null) {
          cc.setUid(categoryCombo.getUid());
        }

        object.setCategoryCombo(cc);

        if (optionSetUid != null) {
          OptionSet optionSet = new OptionSet();
          optionSet.setUid(optionSetUid);
          optionSet.setAutoFields();
          object.setOptionSet(optionSet);
        }

        if (commentOptionSetUid != null) {
          OptionSet optionSet = new OptionSet();
          optionSet.setUid(commentOptionSetUid);
          optionSet.setAutoFields();
          object.setCommentOptionSet(optionSet);
        }

        list.add(object);
      }
    }

    return list;
  }

  private List<DataElementGroup> dataElementGroupsFromCsv(CsvReader reader) throws IOException {
    List<DataElementGroup> list = new ArrayList<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        DataElementGroup object = new DataElementGroup();
        setIdentifiableObject(object, values);
        object.setShortName(getSafe(values, 3, object.getName(), 50));
        object.setAutoFields();
        list.add(object);
      }
    }

    return list;
  }

  private List<DataElementGroup> dataElementGroupMembersFromCsv(CsvReader reader)
      throws IOException {
    Map<String, DataElementGroup> uidMap = new HashMap<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        String groupUid = values[0];
        String memberUid = values[1];

        DataElementGroup persistedGroup =
            dataElementGroupService.getDataElementGroupByUid(groupUid);

        if (persistedGroup != null) {
          DataElementGroup group =
              uidMap.computeIfAbsent(
                  groupUid,
                  key -> {
                    DataElementGroup nonPersistedGroup = new DataElementGroup();
                    nonPersistedGroup.setUid(persistedGroup.getUid());
                    nonPersistedGroup.setName(persistedGroup.getName());
                    return nonPersistedGroup;
                  });

          DataElement member = new DataElement();
          member.setUid(memberUid);
          group.addDataElement(member);
        }
      }
    }

    return new ArrayList<>(uidMap.values());
  }

  private List<IndicatorGroup> indicatorGroupMembersFromCsv(CsvReader reader) throws IOException {
    Map<String, IndicatorGroup> uidMap = new HashMap<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        String groupUid = values[0];
        String memberUid = values[1];

        IndicatorGroup persistedGroup = indicatorGroupService.getIndicatorGroupByUid(groupUid);

        if (persistedGroup != null) {

          IndicatorGroup group =
              uidMap.computeIfAbsent(
                  groupUid,
                  key -> {
                    IndicatorGroup nonPersistedGroup = new IndicatorGroup();
                    nonPersistedGroup.setUid(persistedGroup.getUid());
                    nonPersistedGroup.setName(persistedGroup.getName());
                    return nonPersistedGroup;
                  });

          Indicator member = new Indicator();
          member.setUid(memberUid);
          group.addIndicator(member);
        }
      }
    }
    return new ArrayList<>(uidMap.values());
  }

  private List<CategoryOption> categoryOptionsFromCsv(CsvReader reader) throws IOException {
    List<CategoryOption> list = new ArrayList<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        CategoryOption object = new CategoryOption();
        setIdentifiableObject(object, values);
        object.setShortName(getSafe(values, 3, object.getName(), 50));
        list.add(object);
      }
    }

    return list;
  }

  private List<Category> categoriesFromCsv(CsvReader reader) throws IOException {
    List<Category> list = new ArrayList<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        Category object = new Category();
        setIdentifiableObject(object, values);
        object.setDescription(getSafe(values, 3, 255));
        object.setDataDimensionType(
            DataDimensionType.valueOf(
                getSafe(values, 4, DataDimensionType.DISAGGREGATION.toString(), 40)));
        object.setDataDimension(
            Boolean.parseBoolean(getSafe(values, 5, Boolean.FALSE.toString(), 40)));
        list.add(object);
      }
    }

    return list;
  }

  private List<CategoryCombo> categoryCombosFromCsv(CsvReader reader) throws IOException {
    List<CategoryCombo> list = new ArrayList<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        CategoryCombo object = new CategoryCombo();
        setIdentifiableObject(object, values);
        object.setDataDimensionType(
            DataDimensionType.valueOf(
                getSafe(values, 3, DataDimensionType.DISAGGREGATION.toString(), 40)));
        object.setSkipTotal(Boolean.parseBoolean(getSafe(values, 4, Boolean.FALSE.toString(), 40)));
        list.add(object);
      }
    }

    return list;
  }

  private List<CategoryOptionGroup> categoryOptionGroupsFromCsv(CsvReader reader)
      throws IOException {
    List<CategoryOptionGroup> list = new ArrayList<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        CategoryOptionGroup object = new CategoryOptionGroup();
        setIdentifiableObject(object, values);
        object.setShortName(getSafe(values, 3, object.getName(), 50));
        list.add(object);
      }
    }

    return list;
  }

  private List<ValidationRule> validationRulesFromCsv(CsvReader reader) throws IOException {
    List<ValidationRule> list = new ArrayList<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        Expression leftSide = new Expression();
        Expression rightSide = new Expression();

        ValidationRule object = new ValidationRule();
        setIdentifiableObject(object, values);
        object.setDescription(getSafe(values, 3, 255));
        object.setInstruction(getSafe(values, 4, 255));
        object.setImportance(
            Importance.valueOf(getSafe(values, 5, Importance.MEDIUM.toString(), 255)));
        // Index 6 was rule type which has been removed from the data
        // model
        object.setOperator(
            Operator.safeValueOf(getSafe(values, 7, Operator.equal_to.toString(), 255)));
        object.setPeriodType(
            PeriodType.getByNameIgnoreCase(
                getSafe(values, 8, PeriodTypeEnum.MONTHLY.getName(), 255)));

        leftSide.setExpression(getSafe(values, 9, 255));
        leftSide.setDescription(getSafe(values, 10, 255));
        leftSide.setMissingValueStrategy(
            MissingValueStrategy.safeValueOf(
                getSafe(values, 11, MissingValueStrategy.NEVER_SKIP.toString(), 50)));

        rightSide.setExpression(getSafe(values, 12, 255));
        rightSide.setDescription(getSafe(values, 13, 255));
        rightSide.setMissingValueStrategy(
            MissingValueStrategy.safeValueOf(
                getSafe(values, 14, MissingValueStrategy.NEVER_SKIP.toString(), 50)));

        object.setLeftSide(leftSide);
        object.setRightSide(rightSide);
        object.setAutoFields();

        list.add(object);
      }
    }

    return list;
  }

  private void setGeometry(OrganisationUnit ou, FeatureType featureType, String coordinates) {
    if (featureType != FeatureType.NONE && StringUtils.isNotBlank(coordinates)) {
      ou.setGeometryAsJson(String.format(JSON_GEOM_TEMPL, featureType.value(), coordinates));
    }
  }

  private List<OrganisationUnit> orgUnitsFromCsv(CsvReader reader) throws IOException {
    List<OrganisationUnit> list = new ArrayList<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        OrganisationUnit object = new OrganisationUnit();
        setIdentifiableObject(object, values);
        String parentUid = getSafe(values, 3, 230);
        object.setShortName(getSafe(values, 4, object.getName(), 50));
        object.setDescription(getSafe(values, 5));
        object.setOpeningDate(getMediumDate(getSafe(values, 6, "1970-01-01", null)));
        object.setClosedDate(getMediumDate(getSafe(values, 7)));
        object.setComment(getSafe(values, 8));
        setGeometry(
            object, FeatureType.valueOf(getSafe(values, 9, "NONE", 50)), getSafe(values, 10));
        object.setUrl(getSafe(values, 11, 255));
        object.setContactPerson(getSafe(values, 12, 255));
        object.setAddress(getSafe(values, 13, 255));
        object.setEmail(getSafe(values, 14, 150));
        object.setPhoneNumber(getSafe(values, 15, 150));
        object.setAutoFields();

        if (parentUid != null) {
          OrganisationUnit parent = new OrganisationUnit();
          parent.setUid(parentUid);
          object.setParent(parent);
        }

        list.add(object);
      }
    }

    return list;
  }

  private List<OrganisationUnitGroup> orgUnitGroupsFromCsv(CsvReader reader) throws IOException {
    List<OrganisationUnitGroup> list = new ArrayList<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        OrganisationUnitGroup object = new OrganisationUnitGroup();
        setIdentifiableObject(object, values);
        object.setAutoFields();
        object.setShortName(getSafe(values, 3, object.getName(), 50));
        list.add(object);
      }
    }

    return list;
  }

  private List<OrganisationUnitGroup> orgUnitGroupMembersFromCsv(CsvReader reader)
      throws IOException {
    Map<String, OrganisationUnitGroup> uidMap = new HashMap<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        String groupUid = values[0];
        String memberUid = values[1];

        OrganisationUnitGroup persistedGroup =
            organisationUnitGroupService.getOrganisationUnitGroup(groupUid);

        if (persistedGroup != null) {

          OrganisationUnitGroup group =
              uidMap.computeIfAbsent(
                  groupUid,
                  key -> {
                    OrganisationUnitGroup nonPersistedGroup = new OrganisationUnitGroup();

                    nonPersistedGroup.setUid(persistedGroup.getUid());
                    nonPersistedGroup.setName(persistedGroup.getName());

                    return nonPersistedGroup;
                  });

          OrganisationUnit member = new OrganisationUnit();
          member.setUid(memberUid);
          group.addOrganisationUnit(member);
        }
      }
    }

    return new ArrayList<>(uidMap.values());
  }

  /**
   * Option set format:
   *
   * <p>
   *
   * <ul>
   *   <li>option set name
   *   <li>option set uid
   *   <li>option set code
   *   <li>option name
   *   <li>option uid
   *   <li>option code
   * </ul>
   */
  private void setOptionSetsFromCsv(CsvReader reader, Metadata metadata) throws IOException {
    ListMap<String, Option> nameOptionMap = new ListMap<>();
    Map<String, OptionSet> nameOptionSetMap = new HashMap<>();
    List<Option> options = new ArrayList<>();
    // Read option sets and options and put in maps

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        OptionSet optionSet = new OptionSet();
        setIdentifiableObject(optionSet, values);
        optionSet.setAutoFields();
        optionSet.setValueType(ValueType.TEXT);

        Option option = new Option();
        option.setName(getSafe(values, 3, 230));
        option.setUid(getSafe(values, 4, CodeGenerator.generateUid(), 11));
        option.setCode(getSafe(values, 5, 50));
        option.setAutoFields();

        if (optionSet.getName() == null || option.getCode() == null) {
          continue;
        }

        nameOptionSetMap.put(optionSet.getName(), optionSet);

        nameOptionMap.putValue(optionSet.getName(), option);

        options.add(option);
      }
    }

    // Read option sets from map and set in meta data

    for (Entry<String, OptionSet> optionSetEntry : nameOptionSetMap.entrySet()) {
      optionSetEntry
          .getValue()
          .setOptions(new ArrayList<>(nameOptionMap.get(optionSetEntry.getKey())));
    }
    metadata.setOptions(options);
    metadata.setOptionSets(new ArrayList<>(nameOptionSetMap.values()));
  }

  /**
   * Option group format:
   *
   * <p>
   *
   * <ul>
   *   <li>option group name
   *   <li>option group uid
   *   <li>option group code
   *   <li>option group short name
   *   <li>option set uid
   *   <li>option uid
   * </ul>
   */
  private void setOptionGroupsFromCsv(CsvReader reader, Metadata metadata) throws IOException {
    ListMap<String, Option> nameOptionMap = new ListMap<>();
    Map<String, OptionGroup> nameOptionGroupMap = new HashMap<>();
    Map<String, OptionSet> mapOptionSet = new HashMap<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        OptionGroup optionGroup = new OptionGroup();
        setIdentifiableObject(optionGroup, values);
        optionGroup.setShortName(getSafe(values, 3, 50));
        optionGroup.setAutoFields();

        if (optionGroup.getName() == null || optionGroup.getShortName() == null) {
          continue;
        }

        OptionSet optionSet = new OptionSet();
        optionSet.setUid(getSafe(values, 4, 11));

        if (optionSet.getUid() == null) {
          continue;
        }

        OptionSet persistedOptionSet =
            optionSet.getUid() != null
                ? mapOptionSet.computeIfAbsent(
                    optionSet.getUid(), key -> optionService.getOptionSet(optionSet.getUid()))
                : mapOptionSet.computeIfAbsent(
                    optionSet.getCode(),
                    key -> optionService.getOptionSetByCode(optionSet.getCode()));
        if (persistedOptionSet == null) {
          continue;
        }

        optionGroup.setOptionSet(optionSet);

        Option option = new Option();
        option.setUid(getSafe(values, 5, 11));
        option.setCode(getSafe(values, 6, 50));

        if (option.getCode() == null && option.getUid() == null) {
          continue;
        }

        Optional<Option> isOptionExisted =
            persistedOptionSet.getOptions().stream()
                .filter(
                    persistedOption -> {
                      if (option.getUid() != null) {
                        return persistedOption.getUid().equals(option.getUid());
                      } else {
                        return persistedOption.getCode().equals(option.getCode());
                      }
                    })
                .findFirst();

        if (!isOptionExisted.isPresent()) {
          continue;
        }

        nameOptionGroupMap.put(optionGroup.getName(), optionGroup);

        nameOptionMap.putValue(optionGroup.getName(), isOptionExisted.get());
      }
    }

    // Read option groups from map and set in meta data
    for (Entry<String, OptionGroup> optionGroupEntry : nameOptionGroupMap.entrySet()) {
      optionGroupEntry
          .getValue()
          .setMembers(new HashSet<>(nameOptionMap.get(optionGroupEntry.getKey())));
    }
    metadata.setOptionGroups(new ArrayList<>(nameOptionGroupMap.values()));
  }

  /**
   * Option group set format:
   *
   * <p>
   *
   * <ul>
   *   <li>option group set name
   *   <li>option group set uid
   *   <li>option group set code
   *   <li>option group set description
   *   <li>data dimension
   *   <li>option set uid
   *   <li>option set code
   * </ul>
   */
  private List<OptionGroupSet> setOptionGroupSetFromCsv(CsvReader reader) throws IOException {
    List<OptionGroupSet> optionGroupSets = new ArrayList<>();
    Map<String, OptionSet> mapOptionSet = new HashMap<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        OptionGroupSet optionGroupSet = new OptionGroupSet();
        optionGroupSet.setAutoFields();
        setIdentifiableObject(optionGroupSet, values);
        optionGroupSet.setDescription(getSafe(values, 4));
        optionGroupSet.setDataDimension(
            Boolean.parseBoolean(getSafe(values, 3, Boolean.FALSE.toString(), 40))); // boolean

        OptionSet optionSet = new OptionSet();
        optionSet.setUid(getSafe(values, 5, 11));
        optionSet.setCode(getSafe(values, 6, 50));

        if (optionSet.getUid() == null && optionSet.getCode() == null) {
          continue;
        }

        OptionSet persistedOptionSet =
            optionSet.getUid() != null
                ? mapOptionSet.computeIfAbsent(
                    optionSet.getUid(), key -> optionService.getOptionSet(optionSet.getUid()))
                : mapOptionSet.computeIfAbsent(
                    optionSet.getCode(),
                    key -> optionService.getOptionSetByCode(optionSet.getCode()));

        if (persistedOptionSet == null) {
          continue;
        }

        optionGroupSet.setOptionSet(optionSet);

        optionGroupSets.add(optionGroupSet);
      }
    }
    return optionGroupSets;
  }

  /**
   * Option Group Set Members format
   *
   * <ul>
   *   <li>option group set uid
   *   <li>option group uid
   * </ul>
   */
  public List<OptionGroupSet> optionGroupSetMembersFromCsv(CsvReader reader) throws IOException {
    Map<String, OptionGroupSet> uidMap = new HashMap<>();
    Map<String, OptionGroupSet> persistedGroupSetMap = new HashMap<>();

    while (reader.readRecord()) {
      String[] values = reader.getValues();

      if (values != null && values.length > 0) {
        String groupSetUid = getSafe(values, 0, 11);
        String groupUid = getSafe(values, 1, 11);
        if (groupSetUid == null || groupUid == null) {
          continue;
        }

        OptionGroupSet persistedGroupSet =
            persistedGroupSetMap.computeIfAbsent(
                groupSetUid, key -> optionService.getOptionGroupSet(key));

        if (persistedGroupSet != null) {
          OptionGroupSet optionSetGroup =
              uidMap.computeIfAbsent(
                  groupSetUid,
                  key -> {
                    OptionGroupSet nonPersistedGroup = new OptionGroupSet();
                    nonPersistedGroup.setAutoFields();
                    nonPersistedGroup.setUid(persistedGroupSet.getUid());
                    nonPersistedGroup.setName(persistedGroupSet.getName());
                    return nonPersistedGroup;
                  });

          OptionGroup member = new OptionGroup();
          member.setAutoFields();
          member.setUid(groupUid);
          optionSetGroup.addOptionGroup(member);
        }
      }
    }

    return new ArrayList<>(uidMap.values());
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Sets the name, uid and code properties on the given object.
   *
   * @param object the object to set identifiable properties.
   * @param values the array of property values.
   */
  private static void setIdentifiableObject(BaseIdentifiableObject object, String[] values) {
    object.setName(getSafe(values, 0, 230));
    object.setUid(getSafe(values, 1, CodeGenerator::generateUid, 11));
    object.setCode(getSafe(values, 2, 50));
  }

  /**
   * Returns a string from the given array avoiding exceptions.
   *
   * @param values the string array.
   * @param index the array index of the string to get, zero-based.
   * @param defaultValue the default value in case index is out of bounds.
   * @param maxChars the max number of characters to return for the string.
   */
  private static String getSafe(String[] values, int index, String defaultValue, Integer maxChars) {
    if (values == null || index < 0 || index >= values.length || isBlank(values[index])) {
      return defaultValue == null ? null : limitLength(defaultValue, maxChars);
    }
    return limitLength(values[index], maxChars);
  }

  private static String getSafe(String[] values, int index, int maxChars) {
    return getSafe(values, index, (String) null, maxChars);
  }

  private static String getSafe(String[] values, int index) {
    return getSafe(values, index, (String) null, null);
  }

  private static String getSafe(
      String[] values, int index, Supplier<String> defaultValue, Integer maxChars) {
    if (values == null || index < 0 || index >= values.length || isBlank(values[index])) {
      return defaultValue == null ? null : limitLength(defaultValue.get(), maxChars);
    }
    return limitLength(values[index], maxChars);
  }

  private static String limitLength(String str, Integer maxChars) {
    return maxChars != null ? StringUtils.substring(str, 0, maxChars) : str;
  }
}
