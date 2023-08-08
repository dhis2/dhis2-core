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
package org.hisp.dhis.category;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.SetValuedMap;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.user.User;

/**
 * @author Abyot Asalefew
 */
public interface CategoryService {
  String ID = CategoryService.class.getName();

  // -------------------------------------------------------------------------
  // Category
  // -------------------------------------------------------------------------

  /**
   * Adds a Category.
   *
   * @param category the Category to add.
   * @return a generated unique id of the added Category.
   */
  long addCategory(Category category);

  /**
   * Updates a Category.
   *
   * @param category the Category to update.
   */
  void updateCategory(Category category);

  /**
   * Deletes a Category. The Category is also removed from any CategoryCombos if it is a member of.
   * It is not possible to delete a Category with options.
   *
   * @param category the Category to delete.
   */
  void deleteCategory(Category category);

  /**
   * Returns a Category.
   *
   * @param id the id of the Category to return.
   * @return the Category with the given id, or null if no match.
   */
  Category getCategory(long id);

  /**
   * Returns a Category.
   *
   * @param uid the uid of the Category to return.
   * @return the Category with the given uid, or null if no match.
   */
  Category getCategory(String uid);

  /**
   * Retrieves the Category with the given name.
   *
   * @param name the name of the Category to retrieve.
   * @return the Category.
   */
  Category getCategoryByName(String name);

  /**
   * Returns all DataElementCategories.
   *
   * @return a list of all DataElementCategories.
   */
  List<Category> getAllDataElementCategories();

  /**
   * Retrieves all DataElementCategories of dimension type disaggregation.
   *
   * @return a list of CategoryCombos.
   */
  List<Category> getDisaggregationCategories();

  /**
   * Retrieves all DataElementCategories of dimension type disaggregation and data dimensional.
   * Ignores ACL / sharing.
   *
   * @return a list of CategoryCombos.
   */
  List<Category> getDisaggregationDataDimensionCategoriesNoAcl();

  /**
   * Retrieves all DataElementCategories of dimension type attribute.
   *
   * @return a list of CategoryCombos.
   */
  List<Category> getAttributeCategories();

  /**
   * Retrieves all DataElementCategories of dimension type attribute and data dimensional. Ignores
   * ACL / sharing.
   *
   * @return a list of CategoryCombos.
   */
  List<Category> getAttributeDataDimensionCategoriesNoAcl();

  // -------------------------------------------------------------------------
  // CategoryOption
  // -------------------------------------------------------------------------

  /**
   * Adds a CategoryOption.
   *
   * @param dataElementCategoryOption the CategoryOption to add.
   * @return a generated unique id of the added CategoryOption.
   */
  long addCategoryOption(CategoryOption dataElementCategoryOption);

  /**
   * Updates a CategoryOption.
   *
   * @param dataElementCategoryOption the CategoryOption to update.
   */
  void updateCategoryOption(CategoryOption dataElementCategoryOption);

  /**
   * Deletes a CategoryOption.
   *
   * @param dataElementCategoryOption
   */
  void deleteCategoryOption(CategoryOption dataElementCategoryOption);

  /**
   * Returns a CategoryOption.
   *
   * @param id the id of the CategoryOption to return.
   * @return the CategoryOption with the given id, or null if no match.
   */
  CategoryOption getCategoryOption(long id);

  /**
   * Returns a CategoryOption.
   *
   * @param uid the id of the CategoryOption to return.
   * @return the CategoryOption with the given uid, or null if no match.
   */
  CategoryOption getCategoryOption(String uid);

  /**
   * Retrieves the CategoryOption with the given name.
   *
   * @param name the name.
   * @return the CategoryOption with the given name.
   */
  CategoryOption getCategoryOptionByName(String name);

  /**
   * Returns all CategoryOptions.
   *
   * @return a list of all CategoryOptions, or an empty collection if there are no CategoryOptions.
   */
  List<CategoryOption> getAllCategoryOptions();

  /**
   * Returns all CategoryOptions for the given Category.
   *
   * @param category the Category.
   * @return a list of all CategoryOptions, or an empty collection if there are no CategoryOptions.
   */
  List<CategoryOption> getCategoryOptions(Category category);

  /**
   * Returns all CategoryOptions for the given Category that the user has data write access.
   *
   * @param category the Category.
   * @param user to check data write access for
   * @return a list of all CategoryOptions, or an empty collection if there are no CategoryOptions.
   */
  List<CategoryOption> getDataWriteCategoryOptions(Category category, User user);

  /**
   * Returns a set of CategoryOptions that may be seen by the current user, if the current user has
   * any Category constraint(s).
   *
   * @param user User user to check restrictions for.
   * @return Set of CategoryOptions if constrained, else null.
   */
  Set<CategoryOption> getCoDimensionConstraints(User user);

  /**
   * returns associations between categoryOptions and orgUnits
   *
   * @param categoryOptionsUids a list of categoryOption uids
   * @return an IdentifiableObjectAssociations representing associations between each categoryOption
   *     in input and orgUnits
   */
  SetValuedMap<String, String> getCategoryOptionOrganisationUnitsAssociations(
      Set<String> categoryOptionsUids);

  // -------------------------------------------------------------------------
  // CategoryCombo
  // -------------------------------------------------------------------------

  /**
   * Adds a CategoryCombo.
   *
   * @param dataElementCategoryCombo the CategoryCombo to add.
   * @return the generated identifier.
   */
  long addCategoryCombo(CategoryCombo dataElementCategoryCombo);

  /**
   * Updates a CategoryCombo.
   *
   * @param dataElementCategoryCombo the CategoryCombo to update.
   */
  void updateCategoryCombo(CategoryCombo dataElementCategoryCombo);

  /**
   * Deletes a CategoryCombo.
   *
   * @param dataElementCategoryCombo the CategoryCombo to delete.
   */
  void deleteCategoryCombo(CategoryCombo dataElementCategoryCombo);

  /**
   * Retrieves a CategoryCombo with the given identifier.
   *
   * @param id the identifier of the CategoryCombo to retrieve.
   * @return the CategoryCombo.
   */
  CategoryCombo getCategoryCombo(long id);

  /**
   * Retrieves a CategoryCombo with the given uid.
   *
   * @param uid the identifier of the CategoryCombo to retrieve.
   * @return the CategoryCombo.
   */
  CategoryCombo getCategoryCombo(String uid);

  /**
   * Retrieves the CategoryCombo with the given name.
   *
   * @param name the name of the CategoryCombo to retrieve.
   * @return the CategoryCombo.
   */
  CategoryCombo getCategoryComboByName(String name);

  /** Returns the default category combo. */
  CategoryCombo getDefaultCategoryCombo();

  /**
   * Retrieves all CategoryCombos.
   *
   * @return a list of CategoryCombos.
   */
  List<CategoryCombo> getAllCategoryCombos();

  /**
   * Retrieves all CategoryCombos of dimension type disaggregation.
   *
   * @return a list of CategoryCombos.
   */
  List<CategoryCombo> getDisaggregationCategoryCombos();

  /**
   * Retrieves all CategoryCombos of dimension type attribute.
   *
   * @return a list of CategoryCombos.
   */
  List<CategoryCombo> getAttributeCategoryCombos();

  /**
   * Validates the category combo. Possible return values are:
   *
   * <p>
   *
   * <ul>
   *   <li>category_combo_is_null
   *   <li>category_combo_must_have_at_least_one_category
   *   <li>category_combo_cannot_have_duplicate_categories
   *   <li>categories_must_have_at_least_one_category_option
   *   <li>categories_cannot_share_category_options
   * </ul>
   *
   * @param categoryCombo the category combo to validate.
   * @return null if valid, non-empty string if invalid.
   */
  String validateCategoryCombo(CategoryCombo categoryCombo);

  // -------------------------------------------------------------------------
  // CategoryOptionCombo
  // -------------------------------------------------------------------------

  /**
   * Adds a CategoryOptionCombo.
   *
   * @param dataElementCategoryOptionCombo the CategoryOptionCombo to add.
   * @return the generated identifier.
   */
  long addCategoryOptionCombo(CategoryOptionCombo dataElementCategoryOptionCombo);

  /**
   * Updates a CategoryOptionCombo.
   *
   * @param dataElementCategoryOptionCombo the CategoryOptionCombo to update.
   */
  void updateCategoryOptionCombo(CategoryOptionCombo dataElementCategoryOptionCombo);

  /**
   * Deletes a CategoryOptionCombo.
   *
   * @param dataElementCategoryOptionCombo the CategoryOptionCombo to delete.
   */
  void deleteCategoryOptionCombo(CategoryOptionCombo dataElementCategoryOptionCombo);

  void deleteCategoryOptionComboNoRollback(CategoryOptionCombo categoryOptionCombo);

  /**
   * Retrieves the CategoryOptionCombo with the given identifier.
   *
   * @param id the identifier of the CategoryOptionCombo.
   * @return the CategoryOptionCombo.
   */
  CategoryOptionCombo getCategoryOptionCombo(long id);

  /**
   * Retrieves the CategoryOptionCombo with the given uid.
   *
   * @param uid the uid of the CategoryOptionCombo.
   * @return the CategoryOptionCombo.
   */
  CategoryOptionCombo getCategoryOptionCombo(String uid);

  /**
   * Retrieves the CategoryOptionCombo with the given uid.
   *
   * @param code the code of the CategoryOptionCombo.
   * @return the CategoryOptionCombo.
   */
  CategoryOptionCombo getCategoryOptionComboByCode(String code);

  /**
   * Retrieves a CategoryOptionCombo.
   *
   * @param categoryCombo the CategoryOptionCombo.
   * @param categoryOptions the set of CategoryOptions.
   */
  CategoryOptionCombo getCategoryOptionCombo(
      CategoryCombo categoryCombo, Set<CategoryOption> categoryOptions);

  /**
   * Retrieves all CategoryOptionCombos.
   *
   * @return a list of CategoryOptionCombos.
   */
  List<CategoryOptionCombo> getAllCategoryOptionCombos();

  /**
   * Generates and persists a default Category, CategoryOption, CategoryCombo and
   * CategoryOptionCombo.
   */
  void generateDefaultDimension();

  /**
   * Retrieves the default CategoryOptionCombo.
   *
   * @return the CategoryOptionCombo.
   */
  CategoryOptionCombo getDefaultCategoryOptionCombo();

  /**
   * Generates and persists CategoryOptionCombos for the given CategoryCombo.
   *
   * @param categoryCombo the CategoryCombo.
   */
  void generateOptionCombos(CategoryCombo categoryCombo);

  /**
   * Invokes updateOptionCombos( CategoryCombo ) for all category combos which the given category is
   * a part of.
   *
   * @param category the Category.
   */
  void updateOptionCombos(Category category);

  /**
   * Generates the complete set of category option combos for the given category combo and compares
   * it to the set of persisted category option combos. Those which are not matched are persisted.
   *
   * @param categoryCombo the CategoryCombo.
   */
  void updateOptionCombos(CategoryCombo categoryCombo);

  /**
   * Returns the category option combo with the given uid. Respects access control by only returning
   * objects which the current user has {@code data write} access to.
   *
   * @param idScheme the id scheme.
   * @param id the id.
   * @return a category option combo.
   */
  CategoryOptionCombo getCategoryOptionComboAcl(IdScheme idScheme, String id);

  /** Updates the name property of all category option combinations. */
  void updateCategoryOptionComboNames();

  // -------------------------------------------------------------------------
  // DataElementOperand
  // -------------------------------------------------------------------------

  /**
   * Returns generated Operands for the given Collection of DataElements.
   *
   * @param dataElements the Collection of DataElements.
   * @return the Operands for the given Collection of DataElements.
   */
  List<DataElementOperand> getOperands(Collection<DataElement> dataElements);

  /**
   * Returns generated Operands for the given Collection of DataElements.
   *
   * @param dataElements the Collection of DataElements.
   * @param includeTotals whether to include DataElement totals.
   * @return the Operands for the given Collection of DataElements.
   */
  List<DataElementOperand> getOperands(Collection<DataElement> dataElements, boolean includeTotals);

  /**
   * Returns generated Operands for the given data set. Totals are included.
   *
   * @param dataSet the data set.
   * @param includeTotals whether to include DataElement totals.
   * @return the Operands for the given DataSet.
   */
  List<DataElementOperand> getOperands(DataSet dataSet, boolean includeTotals);

  // -------------------------------------------------------------------------
  // CategoryOptionGroup
  // -------------------------------------------------------------------------

  long saveCategoryOptionGroup(CategoryOptionGroup group);

  void updateCategoryOptionGroup(CategoryOptionGroup group);

  CategoryOptionGroup getCategoryOptionGroup(long id);

  CategoryOptionGroup getCategoryOptionGroup(String uid);

  void deleteCategoryOptionGroup(CategoryOptionGroup group);

  List<CategoryOptionGroup> getAllCategoryOptionGroups();

  List<CategoryOptionGroup> getCategoryOptionGroups(CategoryOptionGroupSet groupSet);

  /**
   * Returns a set of CategoryOptionGroups that may be seen by the current user, if the current user
   * has any CategoryOptionGroupSet constraint(s).
   *
   * @param user User user to check restrictions for.
   * @return Set of CategoryOptionGroups if constrained, else null.
   */
  Set<CategoryOptionGroup> getCogDimensionConstraints(User user);

  // -------------------------------------------------------------------------
  // CategoryOptionGroupSet
  // -------------------------------------------------------------------------

  long saveCategoryOptionGroupSet(CategoryOptionGroupSet group);

  void updateCategoryOptionGroupSet(CategoryOptionGroupSet group);

  CategoryOptionGroupSet getCategoryOptionGroupSet(long id);

  CategoryOptionGroupSet getCategoryOptionGroupSet(String uid);

  void deleteCategoryOptionGroupSet(CategoryOptionGroupSet group);

  List<CategoryOptionGroupSet> getAllCategoryOptionGroupSets();

  List<CategoryOptionGroupSet> getDisaggregationCategoryOptionGroupSetsNoAcl();

  List<CategoryOptionGroupSet> getAttributeCategoryOptionGroupSetsNoAcl();

  CategoryOption getDefaultCategoryOption();

  Category getDefaultCategory();
}
