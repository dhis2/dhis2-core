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
package org.hisp.dhis.de.action;

import static org.hisp.dhis.commons.util.TextUtils.SEP;

import com.google.common.collect.Sets;
import com.opensymphony.xwork2.Action;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UserContext;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class GetMetaDataAction implements Action {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private DataElementService dataElementService;

  public void setDataElementService(DataElementService dataElementService) {
    this.dataElementService = dataElementService;
  }

  private IndicatorService indicatorService;

  public void setIndicatorService(IndicatorService indicatorService) {
    this.indicatorService = indicatorService;
  }

  private ExpressionService expressionService;

  public void setExpressionService(ExpressionService expressionService) {
    this.expressionService = expressionService;
  }

  private CategoryService categoryService;

  public void setCategoryService(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  private CurrentUserService currentUserService;

  public void setCurrentUserService(CurrentUserService currentUserService) {
    this.currentUserService = currentUserService;
  }

  @Autowired private DataSetService dataSetService;

  @Autowired private IdentifiableObjectManager identifiableObjectManager;

  @Autowired protected UserSettingService userSettingService;

  // -------------------------------------------------------------------------
  // Output
  // -------------------------------------------------------------------------

  private Collection<DataElement> significantZeros;

  public Collection<DataElement> getSignificantZeros() {
    return significantZeros;
  }

  private Collection<DataElement> dataElements;

  public Collection<DataElement> getDataElements() {
    return dataElements;
  }

  private List<DataElement> dataElementsWithOptionSet = new ArrayList<>();

  public List<DataElement> getDataElementsWithOptionSet() {
    return dataElementsWithOptionSet;
  }

  private Collection<Indicator> indicators;

  public Collection<Indicator> getIndicators() {
    return indicators;
  }

  private List<DataSet> dataSets;

  public List<DataSet> getDataSets() {
    return dataSets;
  }

  private boolean emptyOrganisationUnits;

  public boolean isEmptyOrganisationUnits() {
    return emptyOrganisationUnits;
  }

  private List<CategoryCombo> categoryCombos;

  public List<CategoryCombo> getCategoryCombos() {
    return categoryCombos;
  }

  private List<Category> categories;

  public List<Category> getCategories() {
    return categories;
  }

  private CategoryCombo defaultCategoryCombo;

  public CategoryCombo getDefaultCategoryCombo() {
    return defaultCategoryCombo;
  }

  private Map<String, List<CategoryOption>> categoryOptionMap = new HashMap<>();

  public Map<String, List<CategoryOption>> getCategoryOptionMap() {
    return categoryOptionMap;
  }

  private List<LockException> lockExceptions;

  public List<LockException> getLockExceptions() {
    return lockExceptions;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() {
    User user = currentUserService.getCurrentUser();

    Locale dbLocale = getLocaleWithDefault(new TranslateParams(true));
    UserContext.setUser(user);
    UserContext.setUserSetting(UserSettingKey.DB_LOCALE, dbLocale);

    Date lastUpdated =
        DateUtils.max(
            Sets.newHashSet(
                identifiableObjectManager.getLastUpdated(DataElement.class),
                identifiableObjectManager.getLastUpdated(OptionSet.class),
                identifiableObjectManager.getLastUpdated(Indicator.class),
                identifiableObjectManager.getLastUpdated(DataSet.class),
                identifiableObjectManager.getLastUpdated(CategoryCombo.class),
                identifiableObjectManager.getLastUpdated(Category.class),
                identifiableObjectManager.getLastUpdated(CategoryOption.class)));
    String tag =
        lastUpdated != null && user != null
            ? (DateUtils.getLongDateString(lastUpdated) + SEP + user.getUid())
            : null;

    if (ContextUtils.isNotModified(
        ServletActionContext.getRequest(), ServletActionContext.getResponse(), tag)) {
      return SUCCESS;
    }

    if (user != null && user.getOrganisationUnits().isEmpty()) {
      emptyOrganisationUnits = true;

      return SUCCESS;
    }

    significantZeros = dataElementService.getDataElementsByZeroIsSignificant(true);

    dataElements = dataElementService.getDataElementsWithDataSets();

    for (DataElement dataElement : dataElements) {
      if (dataElement != null && dataElement.getOptionSet() != null) {
        dataElementsWithOptionSet.add(dataElement);
      }
    }

    indicators = indicatorService.getIndicatorsWithDataSets();

    expressionService.substituteIndicatorExpressions(indicators);

    dataSets = dataSetService.getUserDataWrite(user);

    Set<CategoryCombo> categoryComboSet = new HashSet<>();
    Set<Category> categorySet = new HashSet<>();

    for (DataSet dataSet : dataSets) {
      if (dataSet.getCategoryCombo() != null) {
        categoryComboSet.add(dataSet.getCategoryCombo());
      }
    }

    for (CategoryCombo categoryCombo : categoryComboSet) {
      if (categoryCombo.getCategories() != null) {
        categorySet.addAll(categoryCombo.getCategories());
      }
    }

    categoryCombos = new ArrayList<>(categoryComboSet);
    categories = new ArrayList<>(categorySet);

    for (Category category : categories) {
      List<CategoryOption> categoryOptions =
          new ArrayList<>(categoryService.getDataWriteCategoryOptions(category, user));
      Collections.sort(categoryOptions);
      categoryOptionMap.put(category.getUid(), categoryOptions);
    }

    Set<String> nonAccessibleDataSetIds = new HashSet<>();

    for (DataSet dataSet : dataSets) {
      CategoryCombo categoryCombo = dataSet.getCategoryCombo();

      if (categoryCombo != null && categoryCombo.getCategories() != null) {
        for (Category category : categoryCombo.getCategories()) {
          if (!categoryOptionMap.containsKey(category.getUid())
              || categoryOptionMap.get(category.getUid()).isEmpty()) {
            nonAccessibleDataSetIds.add(dataSet.getUid());
            break;
          }
        }
      }
    }

    dataSets =
        dataSets.stream()
            .filter(dataSet -> !nonAccessibleDataSetIds.contains(dataSet.getUid()))
            .collect(Collectors.toList());

    lockExceptions = dataSetService.getAllLockExceptions();

    Collections.sort(dataSets);
    Collections.sort(categoryCombos);
    Collections.sort(categories);

    defaultCategoryCombo = categoryService.getDefaultCategoryCombo();

    return SUCCESS;
  }

  private Locale getLocaleWithDefault(TranslateParams translateParams) {
    return translateParams.isTranslate()
        ? translateParams.getLocaleWithDefault(
            (Locale) userSettingService.getUserSetting(UserSettingKey.DB_LOCALE))
        : null;
  }
}
