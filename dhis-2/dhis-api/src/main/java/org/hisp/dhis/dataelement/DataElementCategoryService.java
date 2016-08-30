package org.hisp.dhis.dataelement;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.hierarchy.HierarchyViolationException;
import org.hisp.dhis.user.UserCredentials;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Abyot Asalefew
 */
public interface DataElementCategoryService
{
    String ID = DataElementCategoryService.class.getName();

    // -------------------------------------------------------------------------
    // Category
    // -------------------------------------------------------------------------

    /**
     * Adds a DataElementCategory.
     *
     * @param dataElementCategory the DataElementCategory to add.
     * @return a generated unique id of the added Category.
     */
    int addDataElementCategory( DataElementCategory dataElementCategory );

    /**
     * Updates a DataElementCategory.
     *
     * @param dataElementCategory the DataElementCategory to update.
     */
    void updateDataElementCategory( DataElementCategory dataElementCategory );

    /**
     * Deletes a DataElementCategory. The DataElementCategory is also removed
     * from any DataElementCategoryCombos if it is a member of. It is not
     * possible to delete a DataElementCategory with options.
     *
     * @param dataElementCategory the DataElementCategory to delete.
     * @throws HierarchyViolationException if the DataElementCategory has
     *                                     children.
     */
    void deleteDataElementCategory( DataElementCategory dataElementCategory );

    /**
     * Returns a DataElementCategory.
     *
     * @param id the id of the DataElementCategory to return.
     * @return the DataElementCategory with the given id, or null if no match.
     */
    DataElementCategory getDataElementCategory( int id );

    /**
     * Returns a DataElementCategory.
     *
     * @param uid the uid of the DataElementCategory to return.
     * @return the DataElementCategory with the given uid, or null if no match.
     */
    DataElementCategory getDataElementCategory( String uid );

    /**
     * Retrieves the DataElementCategories with the given uids.
     *
     * @param uids the uids of the DataElementCategories to retrieve.
     * @return a list of DataElementCategories.
     */
    List<DataElementCategory> getDataElementCategoriesByUid( Collection<String> uids );

    /**
     * Retrieves the DataElementCategory with the given name.
     *
     * @param name the name of the DataElementCategory to retrieve.
     * @return the DataElementCategory.
     */
    DataElementCategory getDataElementCategoryByName( String name );

    /**
     * Retrieves the DataElementCategory with the given code.
     *
     * @param code the name of the DataElementCategory to retrieve.
     * @return the DataElementCategory.
     */
    DataElementCategory getDataElementCategoryByCode( String code );

    /**
     * Returns all DataElementCategories.
     *
     * @return a list of all DataElementCategories.
     */
    List<DataElementCategory> getAllDataElementCategories();

    /**
     * Retrieves all DataElementCategories of dimension type disaggregation.
     *
     * @return a list of DataElementCategoryCombos.
     */
    List<DataElementCategory> getDisaggregationCategories();

    /**
     * Retrieves all DataElementCategories of dimension type disaggregation and
     * data dimensional. Ignores ACL / sharing.
     *
     * @return a list of DataElementCategoryCombos.
     */
    List<DataElementCategory> getDisaggregationDataDimensionCategoriesNoAcl();

    /**
     * Retrieves all DataElementCategories of dimension type attribute.
     *
     * @return a list of DataElementCategoryCombos.
     */
    List<DataElementCategory> getAttributeCategories();

    /**
     * Retrieves all DataElementCategories of dimension type attribute and data
     * dimensional. Ignores ACL / sharing.
     *
     * @return a list of DataElementCategoryCombos.
     */
    List<DataElementCategory> getAttributeDataDimensionCategoriesNoAcl();

    // -------------------------------------------------------------------------
    // CategoryOption
    // -------------------------------------------------------------------------

    /**
     * Adds a DataElementCategoryOption.
     *
     * @param dataElementCategoryOption the DataElementCategoryOption to add.
     * @return a generated unique id of the added DataElementCategoryOption.
     */
    int addDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption );

    /**
     * Updates a DataElementCategoryOption.
     *
     * @param dataElementCategoryOption the DataElementCategoryOption to update.
     */
    void updateDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption );

    /**
     * Deletes a DataElementCategoryOption.
     *
     * @param dataElementCategoryOption
     */
    void deleteDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption );

    /**
     * Returns a DataElementCategoryOption.
     *
     * @param id the id of the DataElementCategoryOption to return.
     * @return the DataElementCategoryOption with the given id, or null if no
     * match.
     */
    DataElementCategoryOption getDataElementCategoryOption( int id );

    /**
     * Returns a DataElementCategoryOption.
     *
     * @param uid the id of the DataElementCategoryOption to return.
     * @return the DataElementCategoryOption with the given uid, or null if no
     * match.
     */
    DataElementCategoryOption getDataElementCategoryOption( String uid );

    /**
     * Retrieves the DataElementCategoryOptions with the given uids.
     *
     * @param uids the uids of the DataElementCategoryOption to retrieve.
     * @return a list of DataElementCategoryOptions.
     */
    List<DataElementCategoryOption> getDataElementCategoryOptionsByUid( Collection<String> uids );

    /**
     * Retrieves the DataElementCategoryOption with the given name.
     *
     * @param name the name.
     * @return the DataElementCategoryOption with the given name.
     */
    DataElementCategoryOption getDataElementCategoryOptionByName( String name );

    /**
     * Retrieves the DataElementCategoryOption with the given short name.
     *
     * @param shortName the short name.
     * @return the DataElementCategoryOption with the given short name.
     */
    DataElementCategoryOption getDataElementCategoryOptionByShortName( String shortName );

    /**
     * Retrieves the DataElementCategoryOption with the given code.
     *
     * @param code the code.
     * @return the DataElementCategoryOption with the given code.
     */
    DataElementCategoryOption getDataElementCategoryOptionByCode( String code );

    /**
     * Returns all DataElementCategoryOptions.
     *
     * @return a list of all DataElementCategoryOptions, or an empty
     * collection if there are no DataElementCategoryOptions.
     */
    List<DataElementCategoryOption> getAllDataElementCategoryOptions();

    /**
     * Returns all DataElementCategoryOptions for the given DataElementCategory.
     *
     * @param category the DataElementCategory.
     * @return a list of all DataElementCategoryOptions, or an empty
     * collection if there are no DataElementCategoryOptions.
     */
    List<DataElementCategoryOption> getDataElementCategoryOptions( DataElementCategory category );

    /**
     * Returns a set of CategoryOptions that may be seen by the current
     * user, if the current user has any Category constraint(s).
     *
     * @param userCredentials User credentials to check restrictions for.
     * @return Set of CategoryOptions if constrained, else null.
     */
    Set<DataElementCategoryOption> getCoDimensionConstraints( UserCredentials userCredentials );

    // -------------------------------------------------------------------------
    // CategoryCombo
    // -------------------------------------------------------------------------

    /**
     * Adds a DataElementCategoryCombo.
     *
     * @param dataElementCategoryCombo the DataElementCategoryCombo to add.
     * @return the generated identifier.
     */
    int addDataElementCategoryCombo( DataElementCategoryCombo dataElementCategoryCombo );

    /**
     * Updates a DataElementCategoryCombo.
     *
     * @param dataElementCategoryCombo the DataElementCategoryCombo to update.
     */
    void updateDataElementCategoryCombo( DataElementCategoryCombo dataElementCategoryCombo );

    /**
     * Deletes a DataElementCategoryCombo.
     *
     * @param dataElementCategoryCombo the DataElementCategoryCombo to delete.
     */
    void deleteDataElementCategoryCombo( DataElementCategoryCombo dataElementCategoryCombo );

    /**
     * Retrieves a DataElementCategoryCombo with the given identifier.
     *
     * @param id the identifier of the DataElementCategoryCombo to retrieve.
     * @return the DataElementCategoryCombo.
     */
    DataElementCategoryCombo getDataElementCategoryCombo( int id );

    /**
     * Retrieves a DataElementCategoryCombo with the given uid.
     *
     * @param uid the identifier of the DataElementCategoryCombo to retrieve.
     * @return the DataElementCategoryCombo.
     */
    DataElementCategoryCombo getDataElementCategoryCombo( String uid );

    /**
     * Retrieves the DataElementCategoryCombo with the given name.
     *
     * @param name the name of the DataElementCategoryCombo to retrieve.
     * @return the DataElementCategoryCombo.
     */
    DataElementCategoryCombo getDataElementCategoryComboByName( String name );

    /**
     * Returns the default category combo.
     */
    DataElementCategoryCombo getDefaultDataElementCategoryCombo();

    /**
     * Retrieves all DataElementCategoryCombos.
     *
     * @return a list of DataElementCategoryCombos.
     */
    List<DataElementCategoryCombo> getAllDataElementCategoryCombos();

    /**
     * Retrieves all DataElementCategoryCombos of dimension type disaggregation.
     *
     * @return a list of DataElementCategoryCombos.
     */
    List<DataElementCategoryCombo> getDisaggregationCategoryCombos();

    /**
     * Retrieves all DataElementCategoryCombos of dimension type attribute.
     *
     * @return a list of DataElementCategoryCombos.
     */
    List<DataElementCategoryCombo> getAttributeCategoryCombos();

    /**
     * Validates the category combo. Possible return values are:
     * <p>
     * <ul>
     * <li>category_combo_is_null</li>
     * <li>category_combo_must_have_at_least_one_category</li>
     * <li>category_combo_cannot_have_duplicate_categories</li>
     * <li>categories_must_have_at_least_one_category_option</li>
     * <li>categories_cannot_share_category_options</li>
     * </ul>
     *
     * @param categoryCombo the category combo to validate.
     * @return null if valid, non-empty string if invalid.
     */
    String validateCategoryCombo( DataElementCategoryCombo categoryCombo );

    // -------------------------------------------------------------------------
    // CategoryOptionCombo
    // -------------------------------------------------------------------------

    /**
     * Adds a DataElementCategoryOptionCombo.
     *
     * @param dataElementCategoryOptionCombo the DataElementCategoryOptionCombo
     *                                       to add.
     * @return the generated identifier.
     */
    int addDataElementCategoryOptionCombo( DataElementCategoryOptionCombo dataElementCategoryOptionCombo );

    /**
     * Updates a DataElementCategoryOptionCombo.
     *
     * @param dataElementCategoryOptionCombo the DataElementCategoryOptionCombo
     *                                       to update.
     */
    void updateDataElementCategoryOptionCombo( DataElementCategoryOptionCombo dataElementCategoryOptionCombo );

    /**
     * Deletes a DataElementCategoryOptionCombo.
     *
     * @param dataElementCategoryOptionCombo the DataElementCategoryOptionCombo
     *                                       to delete.
     */
    void deleteDataElementCategoryOptionCombo( DataElementCategoryOptionCombo dataElementCategoryOptionCombo );

    /**
     * Retrieves the DataElementCategoryOptionCombo with the given identifier.
     *
     * @param id the identifier of the DataElementCategoryOptionCombo.
     * @return the DataElementCategoryOptionCombo.
     */
    DataElementCategoryOptionCombo getDataElementCategoryOptionCombo( int id );

    /**
     * Retrieves the DataElementCategoryOptionCombo with the given uid.
     *
     * @param uid the uid of the DataElementCategoryOptionCombo.
     * @return the DataElementCategoryOptionCombo.
     */
    DataElementCategoryOptionCombo getDataElementCategoryOptionCombo( String uid );

    /**
     * Retrieves the DataElementCategoryOptionCombo with the given uid.
     *
     * @param code the code of the DataElementCategoryOptionCombo.
     * @return the DataElementCategoryOptionCombo.
     */
    DataElementCategoryOptionCombo getDataElementCategoryOptionComboByCode( String code );

    /**
     * Retrieves the DataElementCategoryOptionCombos with the given uids.
     *
     * @param uids the uids of the
     *             DataElementCategoryOptionCombos.
     * @return a List of DataElementCategoryOptionCombos.
     */
    List<DataElementCategoryOptionCombo> getDataElementCategoryOptionCombosByUid( Collection<String> uids );

    /**
     * Retrieves the DataElementCategoryOptionCombo with the given Collection of
     * DataElementCategoryOptions.
     *
     * @param categoryOptions
     */
    DataElementCategoryOptionCombo getDataElementCategoryOptionCombo(
        Collection<DataElementCategoryOption> categoryOptions );

    /**
     * Retrieves a DataElementCategoryOptionCombo.
     *
     * @param categoryCombo   the DataElementCategoryOptionCombo.
     * @param categoryOptions the set of DataElementCategoryOptions.
     */
    DataElementCategoryOptionCombo getDataElementCategoryOptionCombo( DataElementCategoryCombo categoryCombo,
        Set<DataElementCategoryOption> categoryOptions );

    /**
     * Retrieves all DataElementCategoryOptionCombos.
     *
     * @return a list of DataElementCategoryOptionCombos.
     */
    List<DataElementCategoryOptionCombo> getAllDataElementCategoryOptionCombos();

    /**
     * Returns {@link DataElementCategoryOptionCombo} list with paging
     *
     * @param min First result
     * @param max Maximum results
     * @return a list of all category-option-combo
     */
    List<DataElementCategoryOptionCombo> getOptionCombosBetween( int min, int max );

    /**
     * Returns The number of all DataElementCategoryOptionCombo available
     */
    Integer getOptionComboCount();

    /**
     * Generates and persists a default DataElementCategory,
     * DataElementCategoryOption, DataElementCategoryCombo and
     * DataElementCategoryOptionCombo.
     */
    void generateDefaultDimension();

    /**
     * Retrieves the default DataElementCategoryOptionCombo.
     *
     * @return the DataElementCategoryOptionCombo.
     */
    DataElementCategoryOptionCombo getDefaultDataElementCategoryOptionCombo();

    /**
     * Generates and persists DataElementCategoryOptionCombos for the given
     * DataElementCategoryCombo.
     *
     * @param categoryCombo the DataElementCategoryCombo.
     */
    void generateOptionCombos( DataElementCategoryCombo categoryCombo );

    /**
     * Invokes updateOptionCombos( DataElementCategoryCombo ) for all category
     * combos which the given category is a part of.
     *
     * @param category the DataElementCategory.
     */
    void updateOptionCombos( DataElementCategory category );

    /**
     * Generates the complete set of category option combos for the given
     * category combo and compares it to the set of persisted category option
     * combos. Those which are not matched are persisted.
     *
     * @param categoryCombo the DataElementCategoryCombo.
     */
    void updateOptionCombos( DataElementCategoryCombo categoryCombo );

    void addAndPruneOptionCombos( DataElementCategoryCombo categoryCombo );

    /**
     * Generates the complete set of category option combos for all category
     * combos.
     */
    void addAndPruneAllOptionCombos();

    /**
     * Returns the category option combo with the given uid. Respects access control
     * by only returning objects which all category options are accessible.
     *
     * @param property the property.
     * @param id       the id.
     * @return a category option combo.
     */
    DataElementCategoryOptionCombo getDataElementCategoryOptionComboAcl( IdentifiableProperty property, String id );

    List<DataElementCategory> getDataElementCategoriesBetween( int first, int max );

    List<DataElementCategory> getDataElementCategoriesBetweenByName( String name, int first, int max );

    Map<String, Integer> getDataElementCategoryOptionComboUidIdMap();

    int getDataElementCategoryCount();

    int getDataElementCategoryCountByName( String name );

    List<DataElementCategory> getDataElementCategoryBetween( int first, int max );

    List<DataElementCategory> getDataElementCategoryBetweenByName( String name, int first, int max );

    int getDataElementCategoryOptionCount();

    int getDataElementCategoryOptionCountByName( String name );

    List<DataElementCategoryOption> getDataElementCategoryOptionsBetween( int first, int max );

    List<DataElementCategoryOption> getDataElementCategoryOptionsBetweenByName( String name, int first, int max );

    int getDataElementCategoryOptionComboCount();

    int getDataElementCategoryOptionComboCountByName( String name );

    int getDataElementCategoryComboCount();

    int getDataElementCategoryComboCountByName( String name );

    List<DataElementCategoryCombo> getDataElementCategoryCombosBetween( int first, int max );

    List<DataElementCategoryCombo> getDataElementCategoryCombosBetweenByName( String name, int first, int max );

    void updateCategoryOptionComboNames();

    // -------------------------------------------------------------------------
    // DataElementOperand
    // -------------------------------------------------------------------------

    /**
     * Gets the Operands for the given Collection of DataElements.
     *
     * @param dataElements the Collection of DataElements.
     * @return the Operands for the given Collection of DataElements.
     */
    List<DataElementOperand> getOperands( Collection<DataElement> dataElements );

    /**
     * Gets the Operands for the given Collection of DataElements.
     *
     * @param dataElements  the Collection of DataElements.
     * @param includeTotals whether to include DataElement totals in the
     *                      Collection of Operands.
     * @return the Operands for the given Collection of DataElements.
     */
    List<DataElementOperand> getOperands( Collection<DataElement> dataElements, boolean includeTotals );

    // -------------------------------------------------------------------------
    // CategoryOptionGroup
    // -------------------------------------------------------------------------

    int saveCategoryOptionGroup( CategoryOptionGroup group );

    void updateCategoryOptionGroup( CategoryOptionGroup group );

    CategoryOptionGroup getCategoryOptionGroup( int id );

    CategoryOptionGroup getCategoryOptionGroup( String uid );

    List<CategoryOptionGroup> getCategoryOptionGroupsByUid( Collection<String> uids );

    void deleteCategoryOptionGroup( CategoryOptionGroup group );

    List<CategoryOptionGroup> getCategoryOptionGroupsBetween( int first, int max );

    List<CategoryOptionGroup> getCategoryOptionGroupsBetweenByName( int first, int max, String name );

    List<CategoryOptionGroup> getAllCategoryOptionGroups();

    List<CategoryOptionGroup> getCategoryOptionGroups( CategoryOptionGroupSet groupSet );

    CategoryOptionGroup getCategoryOptionGroupByName( String name );

    CategoryOptionGroup getCategoryOptionGroupByCode( String code );

    CategoryOptionGroup getCategoryOptionGroupByShortName( String shortName );

    int getCategoryOptionGroupCount();

    int getCategoryOptionGroupCountByName( String name );

    /**
     * Returns a set of CategoryOptionGroups that may be seen by the current
     * user, if the current user has any CategoryOptionGroupSet constraint(s).
     *
     * @param userCredentials User credentials to check restrictions for.
     * @return Set of CategoryOptionGroups if constrained, else null.
     */
    Set<CategoryOptionGroup> getCogDimensionConstraints( UserCredentials userCredentials );

    // -------------------------------------------------------------------------
    // CategoryOptionGroupSet
    // -------------------------------------------------------------------------

    int saveCategoryOptionGroupSet( CategoryOptionGroupSet group );

    void updateCategoryOptionGroupSet( CategoryOptionGroupSet group );

    CategoryOptionGroupSet getCategoryOptionGroupSet( int id );

    CategoryOptionGroupSet getCategoryOptionGroupSet( String uid );

    List<CategoryOptionGroupSet> getCategoryOptionGroupSetsByUid( Collection<String> uids );

    void deleteCategoryOptionGroupSet( CategoryOptionGroupSet group );

    List<CategoryOptionGroupSet> getCategoryOptionGroupSetsBetween( int first, int max );

    List<CategoryOptionGroupSet> getCategoryOptionGroupSetsBetweenByName( int first, int max, String name );

    List<CategoryOptionGroupSet> getAllCategoryOptionGroupSets();

    List<CategoryOptionGroupSet> getDisaggregationCategoryOptionGroupSetsNoAcl();

    List<CategoryOptionGroupSet> getAttributeCategoryOptionGroupSetsNoAcl();

    CategoryOptionGroupSet getCategoryOptionGroupSetByName( String name );

    int getCategoryOptionGroupSetCount();

    int getCategoryOptionGroupSetCountByName( String name );

    DataElementCategoryOption getDefaultDataElementCategoryOption();

    DataElementCategory getDefaultDataElementCategory();
}
