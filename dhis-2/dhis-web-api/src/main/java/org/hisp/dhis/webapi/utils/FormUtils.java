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
package org.hisp.dhis.webapi.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.NameableObjectUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.comparator.SectionOrderComparator;
import org.hisp.dhis.datavalue.DataValueEntry;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.webapi.webdomain.form.Category;
import org.hisp.dhis.webapi.webdomain.form.CategoryCombo;
import org.hisp.dhis.webapi.webdomain.form.Field;
import org.hisp.dhis.webapi.webdomain.form.Form;
import org.hisp.dhis.webapi.webdomain.form.Group;
import org.hisp.dhis.webapi.webdomain.form.Option;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class FormUtils {
  private static final String KEY_PERIOD_TYPE = "periodType";

  private static final String KEY_OPEN_FUTURE_PERIODS = "openFuturePeriods";

  private static final String KEY_OPEN_PERIODS_AFTER_CO_END_DATE = "openPeriodsAfterCoEndDate";

  private static final String KEY_DATA_ELEMENTS = "dataElements";

  private static final String KEY_INDICATORS = "indicators";

  private static final String KEY_EXPIRY_DAYS = "expiryDays";

  private static final String SEP = "-";

  public static Form fromDataSet(
      DataSet dataSet, boolean metaData, Set<OrganisationUnit> userOrganisationUnits) {
    Form form = new Form();
    form.setLabel(dataSet.getDisplayName());
    form.setSubtitle(dataSet.getDisplayShortName());

    form.getOptions().put(KEY_PERIOD_TYPE, dataSet.getPeriodType().getName());
    form.getOptions().put(KEY_OPEN_FUTURE_PERIODS, dataSet.getOpenFuturePeriods());
    form.getOptions()
        .put(KEY_OPEN_PERIODS_AFTER_CO_END_DATE, dataSet.getOpenPeriodsAfterCoEndDate());
    form.getOptions().put(KEY_EXPIRY_DAYS, dataSet.getExpiryDays());
    form.setCategoryCombo(getCategoryCombo(dataSet, userOrganisationUnits));

    if (dataSet.hasSections()) {
      List<Section> sections = new ArrayList<>(dataSet.getSections());
      Collections.sort(sections, SectionOrderComparator.INSTANCE);

      for (Section section : sections) {
        List<Field> fields =
            inputFromDataElements(
                new ArrayList<>(section.getDataElements()),
                new ArrayList<>(section.getGreyedFields()));

        Group group = new Group();
        group.setLabel(section.getDisplayName());
        group.setDescription(section.getDescription());
        group.setDataElementCount(section.getDataElements().size());
        group.setFields(fields);

        if (metaData) {
          group
              .getMetaData()
              .put(
                  KEY_DATA_ELEMENTS,
                  NameableObjectUtils.getAsNameableObjects(section.getDataElements()));
          group
              .getMetaData()
              .put(
                  KEY_INDICATORS,
                  NameableObjectUtils.getAsNameableObjects(section.getIndicators()));
        }

        form.getGroups().add(group);
      }
    } else {
      List<Field> fields = inputFromDataElements(new ArrayList<>(dataSet.getDataElements()));

      Group group = new Group();
      group.setLabel(org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME);
      group.setDescription(org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME);
      group.setDataElementCount(dataSet.getDataElements().size());
      group.setFields(fields);

      if (metaData) {
        group
            .getMetaData()
            .put(
                KEY_DATA_ELEMENTS,
                NameableObjectUtils.getAsNameableObjects(
                    new ArrayList<>(dataSet.getDataElements())));
      }

      form.getGroups().add(group);
    }

    return form;
  }

  private static CategoryCombo getCategoryCombo(
      DataSet dataset, Set<OrganisationUnit> userOrganisationUnits) {
    if (dataset.hasCategoryCombo()) {
      org.hisp.dhis.category.CategoryCombo categoryCombo = dataset.getCategoryCombo();
      CategoryCombo catCombo = new CategoryCombo();
      catCombo.setId(categoryCombo.getUid());

      List<org.hisp.dhis.category.Category> cats = categoryCombo.getCategories();

      if (cats != null && cats.size() > 0) {
        for (org.hisp.dhis.category.Category cat : cats) {
          if (cat.getAccess() != null && !cat.getAccess().isRead()) {
            continue;
          }

          Category c = new Category();
          c.setId(cat.getUid());
          c.setLabel(cat.getName());

          List<CategoryOption> options = cat.getCategoryOptions();

          if (options != null && options.size() > 0) {
            for (CategoryOption option : options) {
              if (option.getAccess() != null && !option.getAccess().isRead()) {
                continue;
              }

              Option o = new Option();
              o.setId(option.getUid());
              o.setLabel(option.getDisplayName());
              o.setStartDate(option.getStartDate());
              o.setEndDate(option.getEndDate());

              Set<OrganisationUnit> catOptionOUs = option.getOrganisationUnits();

              if (userOrganisationUnits == null
                  || userOrganisationUnits.isEmpty()
                  || catOptionOUs == null
                  || catOptionOUs.isEmpty()) {
                c.getOptions().add(o);
              } else if (userOrganisationUnits != null
                  && catOptionOUs != null
                  && !Collections.disjoint(userOrganisationUnits, catOptionOUs)) {
                HashSet<OrganisationUnit> organisationUnits = new HashSet<>();

                catOptionOUs.stream()
                    .filter(ou -> userOrganisationUnits.contains(ou))
                    .forEach(
                        ou -> {
                          organisationUnits.add(ou);
                          organisationUnits.addAll(getChildren(ou, new HashSet<>()));
                        });

                o.setOrganisationUnits(organisationUnits);

                c.getOptions().add(o);
              }
            }
          }

          catCombo.getCategories().add(c);
        }
      }
      return catCombo;
    }
    return null;
  }

  private static List<Field> inputFromDataElements(List<DataElement> dataElements) {
    return inputFromDataElements(dataElements, new ArrayList<>());
  }

  private static List<Field> inputFromDataElements(
      List<DataElement> dataElements, final List<DataElementOperand> greyedFields) {
    List<Field> fields = new ArrayList<>();

    for (DataElement dataElement : dataElements) {
      for (CategoryOptionCombo categoryOptionCombo : dataElement.getSortedCategoryOptionCombos()) {
        if (!isDisabled(dataElement, categoryOptionCombo, greyedFields)) {
          Field field = new Field();

          if (categoryOptionCombo.isDefault()) {
            field.setLabel(dataElement.getDisplayFormName());
          } else {
            field.setLabel(
                dataElement.getDisplayFormName() + " " + categoryOptionCombo.getDisplayName());
          }

          field.setDataElement(dataElement.getUid());
          field.setCategoryOptionCombo(categoryOptionCombo.getUid());
          field.setType(inputTypeFromDataElement(dataElement));

          if (dataElement.getOptionSet() != null) {
            field.setOptionSet(dataElement.getOptionSet().getUid());
          }

          fields.add(field);
        }
      }
    }

    return fields;
  }

  private static boolean isDisabled(
      DataElement dataElement,
      CategoryOptionCombo dataElementCategoryOptionCombo,
      List<DataElementOperand> greyedFields) {
    for (DataElementOperand operand : greyedFields) {
      if (dataElement.getUid().equals(operand.getDataElement().getUid())
          && dataElementCategoryOptionCombo
              .getUid()
              .equals(operand.getCategoryOptionCombo().getUid())) {
        return true;
      }
    }

    return false;
  }

  private static ValueType inputTypeFromDataElement(DataElement dataElement) {
    return dataElement.getValueType();
  }

  public static void fillWithDataValues(Form form, Collection<DataValueEntry> dataValues) {
    Map<String, Field> operandFieldMap = buildCacheMap(form);

    for (DataValueEntry dataValue : dataValues) {
      UID dataElement = dataValue.dataElement();
      UID categoryOptionCombo = dataValue.categoryOptionCombo();

      Field field = operandFieldMap.get(dataElement + SEP + categoryOptionCombo);

      if (field != null) {
        field.setValue(dataValue.value());
        field.setComment(dataValue.comment());
      }
    }
  }

  private static Map<String, Field> buildCacheMap(Form form) {
    Map<String, Field> cacheMap = new HashMap<>();

    for (Group group : form.getGroups()) {
      for (Field field : group.getFields()) {
        cacheMap.put(field.getDataElement() + SEP + field.getCategoryOptionCombo(), field);
      }
    }

    return cacheMap;
  }

  private static Set<OrganisationUnit> getChildren(
      OrganisationUnit ou, Set<OrganisationUnit> children) {
    if (ou != null && ou.getChildren() != null) {
      for (OrganisationUnit organisationUnit : ou.getChildren()) {
        children.add(organisationUnit);
        children.addAll(getChildren(organisationUnit, children));
      }
    }

    return children;
  }
}
