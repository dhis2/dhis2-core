package org.hisp.dhis.dataelement;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.deletion.DeletionManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Abyot Asalefew
 */
@Transactional
public class DefaultDataElementCategoryService
    implements DataElementCategoryService
{
    private static final Log log = LogFactory.getLog( DefaultDataElementCategoryService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CategoryStore categoryStore;

    public void setCategoryStore( CategoryStore categoryStore )
    {
        this.categoryStore = categoryStore;
    }

    private CategoryOptionStore categoryOptionStore;

    public void setCategoryOptionStore( CategoryOptionStore categoryOptionStore )
    {
        this.categoryOptionStore = categoryOptionStore;
    }

    private CategoryComboStore categoryComboStore;

    public void setCategoryComboStore( CategoryComboStore categoryComboStore )
    {
        this.categoryComboStore = categoryComboStore;
    }

    private CategoryOptionComboStore categoryOptionComboStore;

    public void setCategoryOptionComboStore( CategoryOptionComboStore categoryOptionComboStore )
    {
        this.categoryOptionComboStore = categoryOptionComboStore;
    }

    private CategoryOptionGroupStore categoryOptionGroupStore;

    public void setCategoryOptionGroupStore( CategoryOptionGroupStore categoryOptionGroupStore )
    {
        this.categoryOptionGroupStore = categoryOptionGroupStore;
    }

    private CategoryOptionGroupSetStore categoryOptionGroupSetStore;

    public void setCategoryOptionGroupSetStore( CategoryOptionGroupSetStore categoryOptionGroupSetStore )
    {
        this.categoryOptionGroupSetStore = categoryOptionGroupSetStore;
    }

    private IdentifiableObjectManager idObjectManager;

    public void setIdObjectManager( IdentifiableObjectManager idObjectManager )
    {
        this.idObjectManager = idObjectManager;
    }

    private CurrentUserService currentUserService;
    
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private AclService aclService;

    public void setAclService( AclService aclService )
    {
        this.aclService = aclService;
    }
    
    private DeletionManager deletionManager;

    public void setDeletionManager( DeletionManager deletionManager )
    {
        this.deletionManager = deletionManager;
    }
    
    // -------------------------------------------------------------------------
    // Category
    // -------------------------------------------------------------------------

    @Override
    public int addDataElementCategory( DataElementCategory dataElementCategory )
    {
        categoryStore.save( dataElementCategory );

        return dataElementCategory.getId();
    }

    @Override
    public void updateDataElementCategory( DataElementCategory dataElementCategory )
    {
        categoryStore.update( dataElementCategory );
    }

    @Override
    public void deleteDataElementCategory( DataElementCategory dataElementCategory )
    {
        categoryStore.delete( dataElementCategory );
    }

    @Override
    public List<DataElementCategory> getAllDataElementCategories()
    {
        return categoryStore.getAll();
    }

    @Override
    public DataElementCategory getDataElementCategory( int id )
    {
        return categoryStore.get( id );
    }

    @Override
    public DataElementCategory getDataElementCategory( String uid )
    {
        return categoryStore.getByUid( uid );
    }

    @Override
    public DataElementCategory getDataElementCategoryByName( String name )
    {
        List<DataElementCategory> dataElementCategories = new ArrayList<>(
            categoryStore.getAllEqName( name ) );

        if ( dataElementCategories.isEmpty() )
        {
            return null;
        }

        return dataElementCategories.get( 0 );
    }

    @Override
    public DataElementCategory getDefaultDataElementCategory()
    {
        return getDataElementCategoryByName( DataElementCategory.DEFAULT_NAME );
    }

    @Override
    public DataElementCategory getDataElementCategoryByCode( String code )
    {
        return categoryStore.getByCode( code );
    }

    @Override
    public List<DataElementCategory> getDisaggregationCategories()
    {
        return categoryStore.getCategoriesByDimensionType( DataDimensionType.DISAGGREGATION );
    }

    @Override
    public List<DataElementCategory> getDisaggregationDataDimensionCategoriesNoAcl()
    {
        return categoryStore.getCategoriesNoAcl( DataDimensionType.DISAGGREGATION, true );
    }

    @Override
    public List<DataElementCategory> getAttributeCategories()
    {
        return categoryStore.getCategoriesByDimensionType( DataDimensionType.ATTRIBUTE );
    }

    @Override
    public List<DataElementCategory> getAttributeDataDimensionCategoriesNoAcl()
    {
        return categoryStore.getCategoriesNoAcl( DataDimensionType.ATTRIBUTE, true );
    }

    // -------------------------------------------------------------------------
    // CategoryOption
    // -------------------------------------------------------------------------

    @Override
    public int addDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption )
    {
        categoryOptionStore.save( dataElementCategoryOption );

        return dataElementCategoryOption.getId();
    }

    @Override
    public void updateDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption )
    {
        categoryOptionStore.update( dataElementCategoryOption );
    }

    @Override
    public void deleteDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption )
    {
        categoryOptionStore.delete( dataElementCategoryOption );
    }

    @Override
    public DataElementCategoryOption getDataElementCategoryOption( int id )
    {
        return categoryOptionStore.get( id );
    }

    @Override
    public DataElementCategoryOption getDataElementCategoryOption( String uid )
    {
        return categoryOptionStore.getByUid( uid );
    }

    @Override
    public DataElementCategoryOption getDataElementCategoryOptionByName( String name )
    {
        return categoryOptionStore.getByName( name );
    }

    @Override
    public DataElementCategoryOption getDefaultDataElementCategoryOption()
    {
        return getDataElementCategoryOptionByName( DataElementCategoryOption.DEFAULT_NAME );
    }

    @Override
    public DataElementCategoryOption getDataElementCategoryOptionByShortName( String shortName )
    {
        return categoryOptionStore.getByShortName( shortName );
    }

    @Override
    public DataElementCategoryOption getDataElementCategoryOptionByCode( String code )
    {
        return categoryOptionStore.getByCode( code );
    }

    @Override
    public List<DataElementCategoryOption> getAllDataElementCategoryOptions()
    {
        return categoryOptionStore.getAll();
    }

    @Override
    public List<DataElementCategoryOption> getDataElementCategoryOptions( DataElementCategory category )
    {
        return categoryOptionStore.getCategoryOptions( category );
    }

    @Override
    public Set<DataElementCategoryOption> getCoDimensionConstraints( UserCredentials userCredentials )
    {
        Set<DataElementCategoryOption> options = null;

        Set<DataElementCategory> catConstraints = userCredentials.getCatDimensionConstraints();

        if ( catConstraints != null && !catConstraints.isEmpty() )
        {
            options = new HashSet<>();

            for ( DataElementCategory category : catConstraints )
            {
                options.addAll( getDataElementCategoryOptions( category ) );
            }
        }

        return options;
    }

    // -------------------------------------------------------------------------
    // CategoryCombo
    // -------------------------------------------------------------------------

    @Override
    public int addDataElementCategoryCombo( DataElementCategoryCombo dataElementCategoryCombo )
    {
        categoryComboStore.save( dataElementCategoryCombo );

        return dataElementCategoryCombo.getId();
    }

    @Override
    public void updateDataElementCategoryCombo( DataElementCategoryCombo dataElementCategoryCombo )
    {
        categoryComboStore.update( dataElementCategoryCombo );
    }

    @Override
    public void deleteDataElementCategoryCombo( DataElementCategoryCombo dataElementCategoryCombo )
    {
        categoryComboStore.delete( dataElementCategoryCombo );
    }

    @Override
    public List<DataElementCategoryCombo> getAllDataElementCategoryCombos()
    {
        return categoryComboStore.getAll();
    }

    @Override
    public DataElementCategoryCombo getDataElementCategoryCombo( int id )
    {
        return categoryComboStore.get( id );
    }

    @Override
    public DataElementCategoryCombo getDataElementCategoryCombo( String uid )
    {
        return categoryComboStore.getByUid( uid );
    }

    @Override
    public DataElementCategoryCombo getDataElementCategoryComboByName( String name )
    {
        return categoryComboStore.getByName( name );
    }

    @Override
    public DataElementCategoryCombo getDefaultDataElementCategoryCombo()
    {
        return getDataElementCategoryComboByName( DataElementCategoryCombo.DEFAULT_CATEGORY_COMBO_NAME );
    }

    @Override
    public List<DataElementCategoryCombo> getDisaggregationCategoryCombos()
    {
        return categoryComboStore.getCategoryCombosByDimensionType( DataDimensionType.DISAGGREGATION );
    }

    @Override
    public List<DataElementCategoryCombo> getAttributeCategoryCombos()
    {
        return categoryComboStore.getCategoryCombosByDimensionType( DataDimensionType.ATTRIBUTE );
    }

    @Override
    public String validateCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        if ( categoryCombo == null )
        {
            return "category_combo_is_null";
        }

        if ( categoryCombo.getCategories() == null || categoryCombo.getCategories().isEmpty() )
        {
            return "category_combo_must_have_at_least_one_category";
        }

        if ( Sets.newHashSet( categoryCombo.getCategories() ).size() < categoryCombo.getCategories().size() )
        {
            return "category_combo_cannot_have_duplicate_categories";
        }

        Set<DataElementCategoryOption> categoryOptions = new HashSet<DataElementCategoryOption>();

        for ( DataElementCategory category : categoryCombo.getCategories() )
        {
            if ( category == null || category.getCategoryOptions().isEmpty() )
            {
                return "categories_must_have_at_least_one_category_option";
            }

            if ( !Sets.intersection( categoryOptions, Sets.newHashSet( category.getCategoryOptions() ) ).isEmpty() )
            {
                return "categories_cannot_share_category_options";
            }
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // CategoryOptionCombo
    // -------------------------------------------------------------------------

    @Override
    public int addDataElementCategoryOptionCombo( DataElementCategoryOptionCombo dataElementCategoryOptionCombo )
    {
        categoryOptionComboStore.save( dataElementCategoryOptionCombo );

        return dataElementCategoryOptionCombo.getId();
    }

    @Override
    public void updateDataElementCategoryOptionCombo( DataElementCategoryOptionCombo dataElementCategoryOptionCombo )
    {
        categoryOptionComboStore.update( dataElementCategoryOptionCombo );
    }

    @Override
    public void deleteDataElementCategoryOptionCombo( DataElementCategoryOptionCombo dataElementCategoryOptionCombo )
    {
        categoryOptionComboStore.delete( dataElementCategoryOptionCombo );
    }

    @Override
    public DataElementCategoryOptionCombo getDataElementCategoryOptionCombo( int id )
    {
        return categoryOptionComboStore.get( id );
    }

    @Override
    public DataElementCategoryOptionCombo getDataElementCategoryOptionCombo( String uid )
    {
        return categoryOptionComboStore.getByUid( uid );
    }

    @Override
    public DataElementCategoryOptionCombo getDataElementCategoryOptionComboByCode( String code )
    {
        return categoryOptionComboStore.getByCode( code );
    }

    @Override
    public DataElementCategoryOptionCombo getDataElementCategoryOptionCombo(
        Collection<DataElementCategoryOption> categoryOptions )
    {
        for ( DataElementCategoryOptionCombo categoryOptionCombo : getAllDataElementCategoryOptionCombos() )
        {
            if ( CollectionUtils.isEqualCollection( categoryOptions, categoryOptionCombo.getCategoryOptions() ) )
            {
                return categoryOptionCombo;
            }
        }

        return null;
    }

    @Override
    public DataElementCategoryOptionCombo getDataElementCategoryOptionCombo( DataElementCategoryCombo categoryCombo,
        Set<DataElementCategoryOption> categoryOptions )
    {
        return categoryOptionComboStore.getCategoryOptionCombo( categoryCombo, categoryOptions );
    }

    @Override
    public List<DataElementCategoryOptionCombo> getAllDataElementCategoryOptionCombos()
    {
        return categoryOptionComboStore.getAll();
    }

    @Override
    public void generateDefaultDimension()
    {
        // ---------------------------------------------------------------------
        // DataElementCategoryOption
        // ---------------------------------------------------------------------

        DataElementCategoryOption categoryOption = new DataElementCategoryOption( DataElementCategoryOption.DEFAULT_NAME );
        categoryOption.setUid( "xYerKDKCefk" );
        categoryOption.setCode( "default" );

        addDataElementCategoryOption( categoryOption );

        // ---------------------------------------------------------------------
        // DataElementCategory
        // ---------------------------------------------------------------------

        DataElementCategory category = new DataElementCategory( DataElementCategory.DEFAULT_NAME, DataDimensionType.DISAGGREGATION );
        category.setUid( "GLevLNI9wkl" );
        category.setCode( "default" );
        category.setDataDimension( false );

        category.addCategoryOption( categoryOption );
        addDataElementCategory( category );

        // ---------------------------------------------------------------------
        // DataElementCategoryCombo
        // ---------------------------------------------------------------------

        DataElementCategoryCombo categoryCombo = new DataElementCategoryCombo( DataElementCategoryCombo.DEFAULT_CATEGORY_COMBO_NAME, DataDimensionType.DISAGGREGATION );
        categoryCombo.setUid( "bjDvmb4bfuf" );
        categoryCombo.setCode( "default" );
        categoryCombo.setDataDimensionType( DataDimensionType.DISAGGREGATION );

        categoryCombo.addDataElementCategory( category );
        addDataElementCategoryCombo( categoryCombo );

        // ---------------------------------------------------------------------
        // DataElementCategoryOptionCombo
        // ---------------------------------------------------------------------

        DataElementCategoryOptionCombo categoryOptionCombo = new DataElementCategoryOptionCombo();
        categoryOptionCombo.setUid( "HllvX50cXC0" );
        categoryOptionCombo.setCode( "default" );

        categoryOptionCombo.setCategoryCombo( categoryCombo );
        categoryOptionCombo.addDataElementCategoryOption( categoryOption );

        addDataElementCategoryOptionCombo( categoryOptionCombo );

        Set<DataElementCategoryOptionCombo> categoryOptionCombos = new HashSet<>();
        categoryOptionCombos.add( categoryOptionCombo );
        categoryCombo.setOptionCombos( categoryOptionCombos );

        updateDataElementCategoryCombo( categoryCombo );

        categoryOption.setCategoryOptionCombos( categoryOptionCombos );
        updateDataElementCategoryOption( categoryOption );
    }

    @Override
    public DataElementCategoryOptionCombo getDefaultDataElementCategoryOptionCombo()
    {
        DataElementCategoryCombo categoryCombo = getDataElementCategoryComboByName( DataElementCategoryCombo.DEFAULT_CATEGORY_COMBO_NAME );

        return categoryCombo != null && categoryCombo.hasOptionCombos() ? categoryCombo.getOptionCombos().iterator().next() : null;
    }

    @Override
    public void generateOptionCombos( DataElementCategoryCombo categoryCombo )
    {
        categoryCombo.generateOptionCombos();

        for ( DataElementCategoryOptionCombo optionCombo : categoryCombo.getOptionCombos() )
        {
            categoryCombo.getOptionCombos().add( optionCombo );
            addDataElementCategoryOptionCombo( optionCombo );
        }

        updateDataElementCategoryCombo( categoryCombo );
    }

    @Override
    public void updateOptionCombos( DataElementCategory category )
    {
        for ( DataElementCategoryCombo categoryCombo : getAllDataElementCategoryCombos() )
        {
            if ( categoryCombo.getCategories().contains( category ) )
            {
                updateOptionCombos( categoryCombo );
            }
        }
    }

    @Override
    public void updateOptionCombos( DataElementCategoryCombo categoryCombo )
    {
        if ( categoryCombo == null || !categoryCombo.isValid() )
        {
            log.warn( "Category combo is null or invalid, could not update option combos: " + categoryCombo );
            return;
        }

        List<DataElementCategoryOptionCombo> generatedOptionCombos = categoryCombo.generateOptionCombosList();
        Set<DataElementCategoryOptionCombo> persistedOptionCombos = categoryCombo.getOptionCombos();

        boolean modified = false;

        for ( DataElementCategoryOptionCombo optionCombo : generatedOptionCombos )
        {
            if ( !persistedOptionCombos.contains( optionCombo ) )
            {
                categoryCombo.getOptionCombos().add( optionCombo );
                addDataElementCategoryOptionCombo( optionCombo );

                log.info( "Added missing category option combo: " + optionCombo + " for category combo: "
                    + categoryCombo.getName() );
                modified = true;
            }
        }

        if ( modified )
        {
            updateDataElementCategoryCombo( categoryCombo );
        }
    }

    @Override
    public void addAndPruneOptionCombos( DataElementCategoryCombo categoryCombo )
    {
        if ( categoryCombo == null || !categoryCombo.isValid() )
        {
            log.warn( "Category combo is null or invalid, could not update option combos: " + categoryCombo );
            return;
        }

        List<DataElementCategoryOptionCombo> generatedOptionCombos = categoryCombo.generateOptionCombosList();
        Set<DataElementCategoryOptionCombo> persistedOptionCombos = Sets.newHashSet( categoryCombo.getOptionCombos() );

        boolean modified = false;

        for ( DataElementCategoryOptionCombo optionCombo : generatedOptionCombos )
        {
            if ( !persistedOptionCombos.contains( optionCombo ) )
            {
                categoryCombo.getOptionCombos().add( optionCombo );
                addDataElementCategoryOptionCombo( optionCombo );

                log.info( "Added missing category option combo: " + optionCombo + " for category combo: " + categoryCombo.getName() );
                modified = true;
            }
        }

        Iterator<DataElementCategoryOptionCombo> iterator = persistedOptionCombos.iterator();

        while ( iterator.hasNext() )
        {
            DataElementCategoryOptionCombo optionCombo = iterator.next();

            if ( !generatedOptionCombos.contains( optionCombo ) )
            {
                try
                {
                    deletionManager.execute( optionCombo );
                }
                catch ( DeleteNotAllowedException ex )
                {
                    log.warn( "Not allowed to delete category option combo: " + optionCombo );
                    continue;
                }

                iterator.remove();
                categoryCombo.getOptionCombos().remove( optionCombo );
                deleteDataElementCategoryOptionCombo( optionCombo );

                log.info( "Deleted obsolete category option combo: " + optionCombo + " for category combo: " + categoryCombo.getName() );
                modified = true;
            }
        }

        if ( modified )
        {
            updateDataElementCategoryCombo( categoryCombo );
        }
    }

    @Override
    public void addAndPruneAllOptionCombos()
    {
        List<DataElementCategoryCombo> categoryCombos = getAllDataElementCategoryCombos();

        for ( DataElementCategoryCombo categoryCombo : categoryCombos )
        {
            addAndPruneOptionCombos( categoryCombo );
        }
    }

    @Override
    public DataElementCategoryOptionCombo getDataElementCategoryOptionComboAcl( IdentifiableProperty property, String id )
    {
        DataElementCategoryOptionCombo coc = idObjectManager.getObject( DataElementCategoryOptionCombo.class, property, id );
        
        if ( coc != null )
        {            
            User user = currentUserService.getCurrentUser();
            
            for ( DataElementCategoryOption categoryOption : coc.getCategoryOptions() )
            {                
                if ( !aclService.canRead( user, categoryOption ) )
                {
                    return null;
                }
            }
        }
        
        return coc;
    }

    @Override
    public void updateCategoryOptionComboNames()
    {
        categoryOptionComboStore.updateNames();
    }

    // -------------------------------------------------------------------------
    // DataElementOperand
    // -------------------------------------------------------------------------

    @Override
    public List<DataElementOperand> getOperands( Collection<DataElement> dataElements )
    {
        return getOperands( dataElements, false );
    }

    @Override
    public List<DataElementOperand> getOperands( Collection<DataElement> dataElements, boolean includeTotals )
    {
        List<DataElementOperand> operands = Lists.newArrayList();

        for ( DataElement dataElement : dataElements )
        {
            Set<DataElementCategoryCombo> categoryCombos = dataElement.getCategoryCombos();
            
            boolean anyIsDefault = categoryCombos.stream().anyMatch( cc -> cc.isDefault() );
            
            if ( includeTotals && !anyIsDefault )
            {
                operands.add( new DataElementOperand( dataElement ) );
            }
            
            for ( DataElementCategoryCombo categoryCombo : categoryCombos )
            {
                operands.addAll( getOperands( dataElement, categoryCombo ) );
            }
        }

        return operands;
    }

    @Override
    public List<DataElementOperand> getOperands( DataSet dataSet, boolean includeTotals )
    {
        List<DataElementOperand> operands = Lists.newArrayList();
                
        for ( DataSetElement element : dataSet.getDataSetElements() )
        {
            DataElementCategoryCombo categoryCombo = element.getResolvedCategoryCombo();
            
            if ( includeTotals && !categoryCombo.isDefault() )
            {
                operands.add( new DataElementOperand( element.getDataElement() ) );
            }
            
            operands.addAll( getOperands( element.getDataElement(), element.getResolvedCategoryCombo() ) );
        }
        
        return operands;
    }

    private List<DataElementOperand> getOperands( DataElement dataElement, DataElementCategoryCombo categoryCombo )
    {
        List<DataElementOperand> operands = Lists.newArrayList();
        
        for ( DataElementCategoryOptionCombo categoryOptionCombo : categoryCombo.getSortedOptionCombos() )
        {
            operands.add( new DataElementOperand( dataElement, categoryOptionCombo ) );
        }
        
        return operands;
    }
    
    // -------------------------------------------------------------------------
    // CategoryOptionGroup
    // -------------------------------------------------------------------------

    @Override
    public int saveCategoryOptionGroup( CategoryOptionGroup group )
    {
        categoryOptionGroupStore.save( group );

        return group.getId();
    }

    @Override
    public void updateCategoryOptionGroup( CategoryOptionGroup group )
    {
        categoryOptionGroupStore.update( group );
    }

    @Override
    public CategoryOptionGroup getCategoryOptionGroup( int id )
    {
        return categoryOptionGroupStore.get( id );
    }

    @Override
    public CategoryOptionGroup getCategoryOptionGroup( String uid )
    {
        return categoryOptionGroupStore.getByUid( uid );
    }

    @Override
    public void deleteCategoryOptionGroup( CategoryOptionGroup group )
    {
        categoryOptionGroupStore.delete( group );
    }

    @Override
    public List<CategoryOptionGroup> getAllCategoryOptionGroups()
    {
        return categoryOptionGroupStore.getAll();
    }

    @Override
    public List<CategoryOptionGroup> getCategoryOptionGroups( CategoryOptionGroupSet groupSet )
    {
        return categoryOptionGroupStore.getCategoryOptionGroups( groupSet );
    }

    @Override
    public Set<CategoryOptionGroup> getCogDimensionConstraints( UserCredentials userCredentials )
    {
        Set<CategoryOptionGroup> groups = null;

        Set<CategoryOptionGroupSet> cogsConstraints = userCredentials.getCogsDimensionConstraints();

        if ( cogsConstraints != null && !cogsConstraints.isEmpty() )
        {
            groups = new HashSet<>();

            for ( CategoryOptionGroupSet cogs : cogsConstraints )
            {
                groups.addAll( getCategoryOptionGroups( cogs ) );
            }
        }

        return groups;
    }

    // -------------------------------------------------------------------------
    // CategoryOptionGroupSet
    // -------------------------------------------------------------------------

    @Override
    public int saveCategoryOptionGroupSet( CategoryOptionGroupSet group )
    {
        categoryOptionGroupSetStore.save( group );

        return group.getId();
    }

    @Override
    public void updateCategoryOptionGroupSet( CategoryOptionGroupSet group )
    {
        categoryOptionGroupSetStore.update( group );
    }

    @Override
    public CategoryOptionGroupSet getCategoryOptionGroupSet( int id )
    {
        return categoryOptionGroupSetStore.get( id );
    }

    @Override
    public CategoryOptionGroupSet getCategoryOptionGroupSet( String uid )
    {
        return categoryOptionGroupSetStore.getByUid( uid );
    }

    @Override
    public void deleteCategoryOptionGroupSet( CategoryOptionGroupSet group )
    {
        categoryOptionGroupSetStore.delete( group );
    }

    @Override
    public List<CategoryOptionGroupSet> getAllCategoryOptionGroupSets()
    {
        return categoryOptionGroupSetStore.getAll();
    }

    @Override
    public List<CategoryOptionGroupSet> getDisaggregationCategoryOptionGroupSetsNoAcl()
    {
        return categoryOptionGroupSetStore.getCategoryOptionGroupSetsNoAcl( DataDimensionType.DISAGGREGATION, true );
    }

    @Override
    public List<CategoryOptionGroupSet> getAttributeCategoryOptionGroupSetsNoAcl()
    {
        return categoryOptionGroupSetStore.getCategoryOptionGroupSetsNoAcl( DataDimensionType.ATTRIBUTE, true );
    }
}
