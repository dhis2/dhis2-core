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
package org.hisp.dhis.category;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetValuedMap;
import org.hisp.dhis.association.jdbc.JdbcOrgUnitAssociationsStore;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@Slf4j
@Service("org.hisp.dhis.category.CategoryService")
@RequiredArgsConstructor
public class DefaultCategoryService implements CategoryService {

  private final CategoryStore categoryStore;
  private final CategoryOptionStore categoryOptionStore;
  private final CategoryComboStore categoryComboStore;
  private final CategoryOptionComboStore categoryOptionComboStore;
  private final CategoryOptionGroupStore categoryOptionGroupStore;
  private final CategoryOptionGroupSetStore categoryOptionGroupSetStore;
  private final IdentifiableObjectManager idObjectManager;
  private final AclService aclService;
  private final DhisConfigurationProvider configuration;

  @Qualifier("jdbcCategoryOptionOrgUnitAssociationsStore")
  private final JdbcOrgUnitAssociationsStore jdbcOrgUnitAssociationsStore;

  // -------------------------------------------------------------------------
  // Category
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public void validate(Category category) throws ConflictException {
    int maxOptions = configuration.getIntProperty(ConfigurationKey.METADATA_CATEGORIES_MAX_OPTIONS);
    int actualOptions = category.getCategoryOptions().size();
    if (actualOptions == 0)
      // assume a transient object that does not have options set
      actualOptions = categoryOptionStore.getCategoryOptionsCount(UID.of(category.getUid()));
    if (actualOptions > maxOptions)
      throw new ConflictException(ErrorCode.E1127, category.getUid(), maxOptions, actualOptions);
  }

  @Override
  @Transactional
  public long addCategory(Category dataElementCategory) {
    categoryStore.save(dataElementCategory);

    return dataElementCategory.getId();
  }

  @Override
  @Transactional
  public long addCategory(Category dataElementCategory, UserDetails actingUser) {
    categoryStore.save(dataElementCategory, actingUser, false);

    return dataElementCategory.getId();
  }

  @Override
  @Transactional
  public void updateCategory(Category dataElementCategory) {
    categoryStore.update(dataElementCategory);
  }

  @Override
  @Transactional
  public void updateCategory(Category category, UserDetails actingUser) {
    categoryStore.update(category, actingUser);
  }

  @Override
  @Transactional
  public void deleteCategory(Category dataElementCategory) {
    categoryStore.delete(dataElementCategory);
  }

  @Override
  @Transactional(readOnly = true)
  public Category getCategory(long id) {
    return categoryStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Category getCategory(String uid) {
    return categoryStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public Category getCategoryByName(String name) {
    List<Category> dataElementCategories = new ArrayList<>(categoryStore.getAllEqName(name));

    if (dataElementCategories.isEmpty()) {
      return null;
    }

    return dataElementCategories.get(0);
  }

  @Override
  @Transactional(readOnly = true)
  public Category getCategoryByName(String name, UserDetails userDetails) {
    List<Category> dataElementCategories =
        new ArrayList<>(categoryStore.getAllEqName(name, userDetails));

    if (dataElementCategories.isEmpty()) {
      return null;
    }

    return dataElementCategories.get(0);
  }

  @Override
  @Transactional(readOnly = true)
  public Category getDefaultCategory() {
    return getCategoryByName(Category.DEFAULT_NAME);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> getDisaggregationCategories() {
    return categoryStore.getCategoriesByDimensionType(DataDimensionType.DISAGGREGATION);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> getDisaggregationDataDimensionCategoriesNoAcl() {
    return categoryStore.getCategoriesNoAcl(DataDimensionType.DISAGGREGATION, true);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> getAttributeCategories() {
    return categoryStore.getCategoriesByDimensionType(DataDimensionType.ATTRIBUTE);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Category> getAttributeDataDimensionCategoriesNoAcl() {
    return categoryStore.getCategoriesNoAcl(DataDimensionType.ATTRIBUTE, true);
  }

  @Override
  public List<Category> getCategoriesByCategoryOption(Collection<UID> categoryOptions) {
    return categoryStore.getCategoriesByCategoryOption(UID.toValueList(categoryOptions));
  }

  // -------------------------------------------------------------------------
  // CategoryOption
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addCategoryOption(CategoryOption dataElementCategoryOption) {
    categoryOptionStore.save(dataElementCategoryOption);

    return dataElementCategoryOption.getId();
  }

  @Override
  @Transactional
  public long addCategoryOption(CategoryOption dataElementCategoryOption, UserDetails actingUser) {
    categoryOptionStore.save(dataElementCategoryOption, actingUser, false);

    return dataElementCategoryOption.getId();
  }

  @Override
  @Transactional
  public void updateCategoryOption(CategoryOption dataElementCategoryOption) {
    categoryOptionStore.update(dataElementCategoryOption);
  }

  @Override
  @Transactional
  public void updateCategoryOption(
      CategoryOption dataElementCategoryOption, UserDetails actingUser) {
    categoryOptionStore.update(dataElementCategoryOption, actingUser);
  }

  @Override
  @Transactional
  public void deleteCategoryOption(CategoryOption dataElementCategoryOption) {
    categoryOptionStore.delete(dataElementCategoryOption);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOption getCategoryOption(long id) {
    return categoryOptionStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOption getCategoryOption(String uid) {
    return categoryOptionStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOption getCategoryOptionByName(String name) {
    return categoryOptionStore.getByName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOption getDefaultCategoryOption() {
    return getCategoryOptionByName(CategoryOption.DEFAULT_NAME);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryOption> getAllCategoryOptions() {
    return categoryOptionStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryOption> getCategoryOptions(Category category) {
    return categoryOptionStore.getCategoryOptions(category);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryOption> getDataWriteCategoryOptions(
      Category category, @Nonnull UserDetails userDetails) {
    return userDetails.isSuper()
        ? getCategoryOptions(category)
        : categoryOptionStore.getDataWriteCategoryOptions(category, userDetails);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<CategoryOption> getCoDimensionConstraints(User user) {
    Set<CategoryOption> options = null;

    Set<Category> catConstraints = user.getCatDimensionConstraints();

    if (catConstraints != null && !catConstraints.isEmpty()) {
      options = new HashSet<>();

      for (Category category : catConstraints) {
        options.addAll(getCategoryOptions(category));
      }
    }

    return options;
  }

  @Override
  public List<CategoryOption> getCategoryOptionsByUid(List<String> catOptionUids) {
    return categoryOptionStore.getByUid(catOptionUids);
  }

  // -------------------------------------------------------------------------
  // CategoryCombo
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public void validate(CategoryCombo combo) throws ConflictException {
    int maxCategories =
        configuration.getIntProperty(ConfigurationKey.METADATA_CATEGORIES_MAX_PER_COMBO);
    int actualCategories = combo.getCategories().size();
    if (actualCategories > maxCategories)
      throw new ConflictException(ErrorCode.E1126, combo.getUid(), maxCategories, actualCategories);
    for (Category c : combo.getCategories()) validate(c);
    int maxCombinations =
        configuration.getIntProperty(ConfigurationKey.METADATA_CATEGORIES_MAX_COMBINATIONS);
    int actualCombinations = 1;
    for (Category c : combo.getCategories()) {
      int options = c.getCategoryOptions().size();
      if (options == 0)
        // assume c is a transient object that has no options set
        options = categoryOptionStore.getCategoryOptionsCount(UID.of(c.getUid()));
      actualCombinations *= options;
    }
    if (actualCombinations > maxCombinations)
      throw new ConflictException(
          ErrorCode.E1128, combo.getUid(), maxCombinations, actualCombinations);
  }

  @Override
  @Transactional
  public long addCategoryCombo(CategoryCombo dataElementCategoryCombo) {
    categoryComboStore.save(dataElementCategoryCombo);

    return dataElementCategoryCombo.getId();
  }

  @Override
  @Transactional
  public long addCategoryCombo(CategoryCombo dataElementCategoryCombo, UserDetails actingUser) {
    categoryComboStore.save(dataElementCategoryCombo, actingUser, false);

    return dataElementCategoryCombo.getId();
  }

  @Override
  @Transactional
  public void updateCategoryCombo(CategoryCombo dataElementCategoryCombo) {
    categoryComboStore.update(dataElementCategoryCombo);
  }

  @Override
  @Transactional
  public void updateCategoryCombo(CategoryCombo dataElementCategoryCombo, UserDetails actingUser) {
    categoryComboStore.update(dataElementCategoryCombo, actingUser);
  }

  @Override
  @Transactional
  public void deleteCategoryCombo(CategoryCombo dataElementCategoryCombo) {
    categoryComboStore.delete(dataElementCategoryCombo);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryCombo> getAllCategoryCombos() {
    return categoryComboStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryCombo getCategoryCombo(long id) {
    return categoryComboStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryCombo getCategoryCombo(String uid) {
    return categoryComboStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryCombo getCategoryComboByName(String name) {
    return categoryComboStore.getByName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryCombo getDefaultCategoryCombo() {
    return getCategoryComboByName(CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryCombo> getDisaggregationCategoryCombos() {
    return categoryComboStore.getCategoryCombosByDimensionType(DataDimensionType.DISAGGREGATION);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryCombo> getAttributeCategoryCombos() {
    return categoryComboStore.getCategoryCombosByDimensionType(DataDimensionType.ATTRIBUTE);
  }

  // -------------------------------------------------------------------------
  // CategoryOptionCombo
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public void validate(CategoryOptionCombo combo) throws ConflictException {
    validate(combo.getCategoryCombo());
  }

  @Override
  @Transactional
  public ImportSummaries addAndPruneOptionCombosWithSummary(@Nonnull CategoryCombo categoryCombo) {
    ImportSummaries importSummaries = new ImportSummaries();
    if (!categoryCombo.isValid()) {
      String msg =
          "Category combo %s is invalid, could not update option combos"
              .formatted(categoryCombo.getUid());
      log.warn(msg);
      importSummaries.addImportSummary(new ImportSummary(ImportStatus.ERROR, msg));
      return importSummaries;
    }
    return addAndPruneOptionCombo(categoryCombo, importSummaries);
  }

  @Override
  @Transactional
  public void addAndPruneOptionCombos(CategoryCombo categoryCombo) {
    addAndPruneOptionCombo(categoryCombo, null);
  }

  @Override
  @Transactional
  public void addAndPruneAllOptionCombos() {
    List<CategoryCombo> categoryCombos = getAllCategoryCombos();

    for (CategoryCombo categoryCombo : categoryCombos) {
      addAndPruneOptionCombo(categoryCombo, null);
    }
  }

  @Override
  @Transactional
  public long addCategoryOptionCombo(CategoryOptionCombo dataElementCategoryOptionCombo) {
    categoryOptionComboStore.save(dataElementCategoryOptionCombo);

    return dataElementCategoryOptionCombo.getId();
  }

  @Override
  @Transactional
  public long addCategoryOptionCombo(
      CategoryOptionCombo dataElementCategoryOptionCombo, UserDetails actingUser) {
    categoryOptionComboStore.save(dataElementCategoryOptionCombo, actingUser, false);

    return dataElementCategoryOptionCombo.getId();
  }

  @Override
  @Transactional
  public void updateCategoryOptionCombo(CategoryOptionCombo dataElementCategoryOptionCombo) {
    categoryOptionComboStore.update(dataElementCategoryOptionCombo);
  }

  @Override
  @Transactional
  public void updateCategoryOptionCombo(
      CategoryOptionCombo dataElementCategoryOptionCombo, UserDetails actingUser) {
    categoryOptionComboStore.update(dataElementCategoryOptionCombo, actingUser);
  }

  @Override
  @Transactional
  public void deleteCategoryOptionCombo(CategoryOptionCombo dataElementCategoryOptionCombo) {
    categoryOptionComboStore.delete(dataElementCategoryOptionCombo);
  }

  @Override
  @Transactional(noRollbackFor = DeleteNotAllowedException.class)
  public void deleteCategoryOptionComboNoRollback(CategoryOptionCombo categoryOptionCombo) {
    categoryOptionComboStore.deleteNoRollBack(categoryOptionCombo);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOptionCombo getCategoryOptionCombo(long id) {
    return categoryOptionComboStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOptionCombo getCategoryOptionCombo(String uid) {
    return categoryOptionComboStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOptionCombo getCategoryOptionComboByCode(String code) {
    return categoryOptionComboStore.getByCode(code);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOptionCombo getCategoryOptionCombo(
      CategoryCombo categoryCombo, Set<CategoryOption> categoryOptions) {
    return categoryOptionComboStore.getCategoryOptionCombo(categoryCombo, categoryOptions);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryOptionCombo> getAllCategoryOptionCombos() {
    return categoryOptionComboStore.getAll();
  }

  @Override
  @Transactional
  public void generateDefaultDimension(UserDetails actingUser) {
    // ---------------------------------------------------------------------
    // CategoryOption
    // ---------------------------------------------------------------------

    CategoryOption categoryOption = new CategoryOption(CategoryOption.DEFAULT_NAME);
    categoryOption.setUid("xYerKDKCefk");
    categoryOption.setCode("default");

    addCategoryOption(categoryOption, actingUser);

    categoryOption.getSharing().setPublicAccess(AccessStringHelper.CATEGORY_OPTION_DEFAULT);
    updateCategoryOption(categoryOption, actingUser);

    // ---------------------------------------------------------------------
    // Category
    // ---------------------------------------------------------------------

    Category category = new Category(Category.DEFAULT_NAME, DataDimensionType.DISAGGREGATION);
    category.setUid("GLevLNI9wkl");
    category.setCode("default");
    category.setShortName("default");
    category.setDataDimension(false);
    category.addCategoryOption(categoryOption);

    addCategory(category, actingUser);

    category.getSharing().setPublicAccess(AccessStringHelper.CATEGORY_NO_DATA_SHARING_DEFAULT);
    updateCategory(category, actingUser);

    // ---------------------------------------------------------------------
    // CategoryCombo
    // ---------------------------------------------------------------------

    CategoryCombo categoryCombo =
        new CategoryCombo(
            CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME, DataDimensionType.DISAGGREGATION);
    categoryCombo.setUid("bjDvmb4bfuf");
    categoryCombo.setCode("default");
    categoryCombo.setDataDimensionType(DataDimensionType.DISAGGREGATION);
    categoryCombo.addCategory(category);

    addCategoryCombo(categoryCombo, actingUser);

    categoryCombo.getSharing().setPublicAccess(AccessStringHelper.CATEGORY_NO_DATA_SHARING_DEFAULT);
    updateCategoryCombo(categoryCombo, actingUser);

    // ---------------------------------------------------------------------
    // CategoryOptionCombo
    // ---------------------------------------------------------------------

    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    categoryOptionCombo.setUid("HllvX50cXC0");
    categoryOptionCombo.setCode("default");
    categoryOptionCombo.setCategoryCombo(categoryCombo);
    categoryOptionCombo.addCategoryOption(categoryOption);

    addCategoryOptionCombo(categoryOptionCombo, actingUser);

    categoryOptionCombo
        .getSharing()
        .setPublicAccess(AccessStringHelper.CATEGORY_NO_DATA_SHARING_DEFAULT);

    updateCategoryOptionCombo(categoryOptionCombo, actingUser);

    Set<CategoryOptionCombo> categoryOptionCombos = new HashSet<>();
    categoryOptionCombos.add(categoryOptionCombo);
    categoryCombo.setOptionCombos(categoryOptionCombos);

    updateCategoryCombo(categoryCombo, actingUser);

    categoryOption.setCategoryOptionCombos(categoryOptionCombos);

    updateCategoryOption(categoryOption, actingUser);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOptionCombo getDefaultCategoryOptionCombo() {
    return categoryOptionComboStore.getByName(CategoryOptionCombo.DEFAULT_NAME);
  }

  @Override
  @Transactional
  public void updateOptionCombos(Category category) {
    for (CategoryCombo categoryCombo : getAllCategoryCombos()) {
      if (categoryCombo.getCategories().contains(category)) {
        addAndPruneOptionCombos(categoryCombo);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOptionCombo getCategoryOptionComboAcl(IdScheme idScheme, String id) {
    CategoryOptionCombo coc = idObjectManager.getObject(CategoryOptionCombo.class, idScheme, id);

    if (coc != null) {
      UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
      for (CategoryOption categoryOption : coc.getCategoryOptions()) {
        if (!aclService.canDataWrite(currentUserDetails, categoryOption)) {
          return null;
        }
      }
    }

    return coc;
  }

  @Override
  @Transactional
  public void updateCategoryOptionComboNames() {
    categoryOptionComboStore.updateNames();
  }

  @Override
  public List<CategoryOptionCombo> getCategoryOptionCombosByCategoryOption(
      @Nonnull Collection<UID> categoryOptionsUids) {
    return categoryOptionComboStore.getCategoryOptionCombosByCategoryOption(
        UID.toValueList(categoryOptionsUids));
  }

  @Override
  public List<CategoryOptionCombo> getCategoryOptionCombosByUid(@Nonnull Collection<UID> uids) {
    return categoryOptionComboStore.getByUid(UID.toValueList(uids));
  }

  // -------------------------------------------------------------------------
  // DataElementOperand
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public List<DataElementOperand> getOperands(Collection<DataElement> dataElements) {
    return getOperands(dataElements, false);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataElementOperand> getOperands(
      Collection<DataElement> dataElements, boolean includeTotals) {
    List<DataElementOperand> operands = Lists.newArrayList();

    for (DataElement dataElement : dataElements) {
      Set<CategoryCombo> categoryCombos = dataElement.getCategoryCombos();

      boolean anyIsDefault = categoryCombos.stream().anyMatch(cc -> cc.isDefault());

      if (includeTotals && !anyIsDefault) {
        operands.add(new DataElementOperand(dataElement));
      }

      for (CategoryCombo categoryCombo : categoryCombos) {
        operands.addAll(getOperands(dataElement, categoryCombo));
      }
    }

    return operands;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataElementOperand> getOperands(DataSet dataSet, boolean includeTotals) {
    List<DataElementOperand> operands = Lists.newArrayList();

    for (DataSetElement element : dataSet.getDataSetElements()) {
      CategoryCombo categoryCombo = element.getResolvedCategoryCombo();

      if (includeTotals && !categoryCombo.isDefault()) {
        operands.add(new DataElementOperand(element.getDataElement()));
      }

      operands.addAll(getOperands(element.getDataElement(), element.getResolvedCategoryCombo()));
    }

    return operands;
  }

  private List<DataElementOperand> getOperands(
      DataElement dataElement, CategoryCombo categoryCombo) {
    List<DataElementOperand> operands = Lists.newArrayList();

    for (CategoryOptionCombo categoryOptionCombo : categoryCombo.getSortedOptionCombos()) {
      operands.add(new DataElementOperand(dataElement, categoryOptionCombo));
    }

    return operands;
  }

  // -------------------------------------------------------------------------
  // CategoryOptionGroup
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long saveCategoryOptionGroup(CategoryOptionGroup group) {
    categoryOptionGroupStore.save(group);

    return group.getId();
  }

  @Override
  @Transactional
  public void updateCategoryOptionGroup(CategoryOptionGroup group) {
    categoryOptionGroupStore.update(group);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOptionGroup getCategoryOptionGroup(long id) {
    return categoryOptionGroupStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOptionGroup getCategoryOptionGroup(String uid) {
    return categoryOptionGroupStore.getByUid(uid);
  }

  @Override
  @Transactional
  public void deleteCategoryOptionGroup(CategoryOptionGroup group) {
    categoryOptionGroupStore.delete(group);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryOptionGroup> getAllCategoryOptionGroups() {
    return categoryOptionGroupStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryOptionGroup> getCategoryOptionGroups(CategoryOptionGroupSet groupSet) {
    return categoryOptionGroupStore.getCategoryOptionGroups(groupSet);
  }

  @Override
  public List<CategoryOptionGroup> getCategoryOptionGroupByCategoryOption(
      Collection<UID> categoryOptions) {
    return categoryOptionGroupStore.getByCategoryOption(UID.toValueList(categoryOptions));
  }

  @Override
  @Transactional(readOnly = true)
  public Set<CategoryOptionGroup> getCogDimensionConstraints(User user) {
    Set<CategoryOptionGroup> groups = null;

    Set<CategoryOptionGroupSet> cogsConstraints = user.getCogsDimensionConstraints();

    if (cogsConstraints != null && !cogsConstraints.isEmpty()) {
      groups = new HashSet<>();

      for (CategoryOptionGroupSet cogs : cogsConstraints) {
        groups.addAll(getCategoryOptionGroups(cogs));
      }
    }

    return groups;
  }

  // -------------------------------------------------------------------------
  // CategoryOptionGroupSet
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long saveCategoryOptionGroupSet(CategoryOptionGroupSet group) {
    categoryOptionGroupSetStore.save(group);

    return group.getId();
  }

  @Override
  @Transactional
  public void updateCategoryOptionGroupSet(CategoryOptionGroupSet group) {
    categoryOptionGroupSetStore.update(group);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOptionGroupSet getCategoryOptionGroupSet(long id) {
    return categoryOptionGroupSetStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryOptionGroupSet getCategoryOptionGroupSet(String uid) {
    return categoryOptionGroupSetStore.getByUid(uid);
  }

  @Override
  @Transactional
  public void deleteCategoryOptionGroupSet(CategoryOptionGroupSet group) {
    categoryOptionGroupSetStore.delete(group);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryOptionGroupSet> getAllCategoryOptionGroupSets() {
    return categoryOptionGroupSetStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryOptionGroupSet> getDisaggregationCategoryOptionGroupSetsNoAcl() {
    return categoryOptionGroupSetStore.getCategoryOptionGroupSetsNoAcl(
        DataDimensionType.DISAGGREGATION, true);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryOptionGroupSet> getAttributeCategoryOptionGroupSetsNoAcl() {
    return categoryOptionGroupSetStore.getCategoryOptionGroupSetsNoAcl(
        DataDimensionType.ATTRIBUTE, true);
  }

  @Override
  public SetValuedMap<String, String> getCategoryOptionOrganisationUnitsAssociations(
      Set<String> uids) {
    return jdbcOrgUnitAssociationsStore.getOrganisationUnitsAssociationsForCurrentUser(uids);
  }

  /**
   * Aligns the persisted state (DB) of COCs with the generated state (in-memory) of COCs for a CC.
   * The generated state is treated as the most up-to-date state of the COCs. This method does the
   * following: <br>
   *
   * <ol>
   *   <li>Delete a persisted COC if it is not present in the generated COCs
   *   <li>Updates a persisted COC name if it is present and has a different name to its generated
   *       COC match
   *   <li>Add a generated COC if it is not present in the persisted COCs
   * </ol>
   *
   * @param categoryCombo the CategoryCombo
   * @param importSummaries pass in an instantiated Import Summary if a report of the process is
   *     required valid one
   * @return returns an Import Summary if one was provided
   */
  @CheckForNull
  private ImportSummaries addAndPruneOptionCombo(
      @Nonnull CategoryCombo categoryCombo, ImportSummaries importSummaries) {
    Set<CategoryOptionCombo> generatedCocs = categoryCombo.generateOptionCombosList();
    Set<CategoryOptionCombo> persistedCocs =
        Sets.newHashSet(categoryComboStore.getByUid(categoryCombo.getUid()).getOptionCombos());

    // Persisted COC checks (update name or delete)
    for (CategoryOptionCombo persistedCoc : persistedCocs) {
      generatedCocs.stream()
          .filter(generatedCoc -> equalOrSameUid.test(persistedCoc, generatedCoc))
          .findFirst()
          .ifPresentOrElse(
              generatedCoc -> updateNameIfNotEqual(persistedCoc, generatedCoc, importSummaries),
              () -> deleteObsoleteCoc(persistedCoc, categoryCombo, importSummaries));
    }

    // Generated COC check (add if missing and not empty)
    for (CategoryOptionCombo generatedCoc : generatedCocs) {
      if (generatedCoc.getCategoryOptions().isEmpty()) {
        log.warn(
            "Generated category option combo %S has 0 options, skip adding for category combo `%s` as this is an invalid category option combo. Consider cleaning up the metadata model."
                .formatted(generatedCoc.getName(), categoryCombo.getName()));
      } else if (!persistedCocs.contains(generatedCoc)) {
        if (categoryCombo.getOptionCombos().add(generatedCoc)) {
          addCategoryOptionCombo(generatedCoc);

          String msg =
              "Added missing category option combo: `%s` for category combo: `%s`"
                  .formatted(generatedCoc.getName(), categoryCombo.getName());
          log.info(msg);
          if (importSummaries != null) {
            ImportSummary importSummary = new ImportSummary();
            importSummary.setDescription(msg);
            importSummary.incrementImported();
            importSummaries.addImportSummary(importSummary);
          }
        }
      }
    }
    return importSummaries;
  }

  private void deleteObsoleteCoc(
      CategoryOptionCombo coc, CategoryCombo cc, ImportSummaries summaries) {
    try {
      String cocName = coc.getName();
      cc.getOptionCombos().remove(coc);
      deleteCategoryOptionComboNoRollback(coc);

      String msg =
          ("Deleted obsolete category option combo: `%s` for category combo: `%s`"
              .formatted(cocName, cc.getName()));
      log.info(msg);
      if (summaries != null) {
        ImportSummary importSummary = new ImportSummary();
        importSummary.setDescription(msg);
        importSummary.incrementDeleted();
        summaries.addImportSummary(importSummary);
      }
    } catch (DeleteNotAllowedException ex) {
      String msg =
          "Could not delete category option combo: `%s` due to `%s`"
              .formatted(coc.getName(), ex.getMessage());
      log.warn(msg);

      if (summaries != null) {
        ImportSummary importSummary = new ImportSummary();
        importSummary.setStatus(ImportStatus.WARNING);
        importSummary.setDescription(msg);
        importSummary.incrementIgnored();
        summaries.addImportSummary(importSummary);
      }
    }
  }

  private final BiPredicate<CategoryOptionCombo, CategoryOptionCombo> equalOrSameUid =
      (coc1, coc2) -> coc1.equals(coc2) || coc1.getUid().equals(coc2.getUid());

  private void updateNameIfNotEqual(
      CategoryOptionCombo coc1, CategoryOptionCombo coc2, ImportSummaries summaries) {
    if (!coc1.getName().equals(coc2.getName())) {
      coc1.setName(coc2.getName());
      if (summaries != null) {
        ImportSummary importSummary = new ImportSummary();
        importSummary.setDescription(
            "Update category option combo `%S` name to `%s`"
                .formatted(coc1.getUid(), coc1.getName()));
        importSummary.incrementUpdated();
        summaries.addImportSummary(importSummary);
      }
    }
  }
}
