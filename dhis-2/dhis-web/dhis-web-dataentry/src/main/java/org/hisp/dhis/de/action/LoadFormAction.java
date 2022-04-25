/*
 * Copyright (c) 2004-2021, University of Oslo
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.comparator.CategoryComboSizeNameComparator;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.UserContext;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.FormType;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.comparator.SectionOrderComparator;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.util.SectionUtils;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Torgeir Lorange Ostby
 */
public class LoadFormAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataEntryFormService dataEntryFormService;

    public void setDataEntryFormService( DataEntryFormService dataEntryFormService )
    {
        this.dataEntryFormService = dataEntryFormService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    @Autowired
    private AggregateAccessManager accessManager;

    public void setAccessManager( AggregateAccessManager accessManager )
    {
        this.accessManager = accessManager;
    }

    @Autowired
    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    @Autowired
    private SectionUtils sectionUtils;

    @Autowired
    protected UserSettingService userSettingService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String dataSetId;

    public void setDataSetId( String dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    private String multiOrganisationUnit;

    public void setMultiOrganisationUnit( String multiOrganisationUnit )
    {
        this.multiOrganisationUnit = multiOrganisationUnit;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private List<OrganisationUnit> organisationUnits = new ArrayList<>();

    public List<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    private Map<CategoryCombo, List<DataElement>> orderedDataElements = new HashMap<>();

    public Map<CategoryCombo, List<DataElement>> getOrderedDataElements()
    {
        return orderedDataElements;
    }

    private String customDataEntryFormCode;

    public String getCustomDataEntryFormCode()
    {
        return this.customDataEntryFormCode;
    }

    private DataEntryForm dataEntryForm;

    public DataEntryForm getDataEntryForm()
    {
        return dataEntryForm;
    }

    private List<Section> sections = new ArrayList<>();

    public List<Section> getSections()
    {
        return sections;
    }

    private Map<Long, Map<Long, List<CategoryOption>>> orderedOptionsMap = new HashMap<>();

    public Map<Long, Map<Long, List<CategoryOption>>> getOrderedOptionsMap()
    {
        return orderedOptionsMap;
    }

    private Map<Long, Collection<Category>> orderedCategories = new HashMap<>();

    public Map<Long, Collection<Category>> getOrderedCategories()
    {
        return orderedCategories;
    }

    private Map<Long, Integer> numberOfTotalColumns = new HashMap<>();

    public Map<Long, Integer> getNumberOfTotalColumns()
    {
        return numberOfTotalColumns;
    }

    private Map<Long, Map<Long, Collection<Integer>>> catColRepeat = new HashMap<>();

    public Map<Long, Map<Long, Collection<Integer>>> getCatColRepeat()
    {
        return catColRepeat;
    }

    private Map<Long, Collection<CategoryOptionCombo>> orderedCategoryOptionCombos = new HashMap<>();

    public Map<Long, Collection<CategoryOptionCombo>> getOrderedCategoryOptionCombos()
    {
        return orderedCategoryOptionCombos;
    }

    private List<CategoryCombo> orderedCategoryCombos = new ArrayList<>();

    public List<CategoryCombo> getOrderedCategoryCombos()
    {
        return orderedCategoryCombos;
    }

    private Map<Long, Collection<String>> sectionCombos = new HashMap<>();

    private Map<String, Boolean> optionComboAccessMap = new HashMap<>();

    public Map<String, Boolean> getOptionComboAccessMap()
    {
        return optionComboAccessMap;
    }

    public Map<Long, Collection<String>> getSectionCombos()
    {
        return sectionCombos;
    }

    private Map<String, Long> orderedSectionCategoryCombos = new HashMap<>();

    public Map<String, Long> getOrderedSectionCategoryCombos()
    {
        return orderedSectionCategoryCombos;
    }

    private Map<String, Boolean> greyedFields = new HashMap<>();

    public Map<String, Boolean> getGreyedFields()
    {
        return greyedFields;
    }

    private DataSet dataSet;

    public DataSet getDataSet()
    {
        return dataSet;
    }

    private Map<String, Collection<DataElement>> sectionCategoryComboDataElements = new LinkedHashMap<>();

    public Map<String, Collection<DataElement>> getSectionCategoryComboDataElements()
    {
        return sectionCategoryComboDataElements;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        User currentUser = currentUserService.getCurrentUser();

        Locale dbLocale = getLocaleWithDefault( new TranslateParams( true ) );
        UserContext.setUser( currentUser );
        UserContext.setUserSetting( UserSettingKey.DB_LOCALE, dbLocale );

        dataSet = dataSetService.getDataSet( dataSetId );

        if ( dataSet == null )
        {
            return INPUT;
        }

        FormType formType = dataSet.getFormType();

        // ---------------------------------------------------------------------
        // Custom form
        // ---------------------------------------------------------------------

        if ( formType.isCustom() && dataSet.hasDataEntryForm() )
        {
            dataEntryForm = dataSet.getDataEntryForm();
            customDataEntryFormCode = dataEntryFormService.prepareDataEntryFormForEntry( dataEntryForm, dataSet, i18n );
            return formType.toString();
        }

        // ---------------------------------------------------------------------
        // Section / default form
        // ---------------------------------------------------------------------

        List<DataElement> dataElements = new ArrayList<>( dataSet.getDataElements() );

        if ( dataElements.isEmpty() )
        {
            return INPUT;
        }

        Collections.sort( dataElements );

        orderedDataElements = ListMap.getListMap( dataElements, de -> de.getDataElementCategoryCombo( dataSet ) );

        orderedCategoryCombos = getCategoryCombos( dataElements, dataSet );

        for ( CategoryCombo categoryCombo : orderedCategoryCombos )
        {
            List<CategoryOptionCombo> optionCombos = categoryCombo.getSortedOptionCombos();

            orderedCategoryOptionCombos.put( categoryCombo.getId(), optionCombos );

            addOptionAccess( currentUser, optionComboAccessMap, optionCombos );

            // -----------------------------------------------------------------
            // Perform ordering of categories and their options so that they
            // could be displayed as in the paper form. Note that the total
            // number of entry cells to be generated are the multiple of options
            // from each category.
            // -----------------------------------------------------------------

            numberOfTotalColumns.put( categoryCombo.getId(), optionCombos.size() );

            orderedCategories.put( categoryCombo.getId(), categoryCombo.getCategories() );

            Map<Long, List<CategoryOption>> optionsMap = new HashMap<>();

            for ( Category category : categoryCombo.getCategories() )
            {
                optionsMap.put( category.getId(), category.getCategoryOptions() );
            }

            orderedOptionsMap.put( categoryCombo.getId(), optionsMap );

            // -----------------------------------------------------------------
            // Calculating the number of times each category should be repeated
            // -----------------------------------------------------------------

            Map<Long, Integer> catRepeat = new HashMap<>();

            Map<Long, Collection<Integer>> colRepeat = new HashMap<>();

            int catColSpan = optionCombos.size();

            for ( Category cat : categoryCombo.getCategories() )
            {
                int categoryOptionSize = cat.getCategoryOptions().size();

                if ( categoryOptionSize > 0 && catColSpan >= categoryOptionSize )
                {
                    catColSpan = catColSpan / categoryOptionSize;
                    int total = optionCombos.size() / (catColSpan * categoryOptionSize);
                    Collection<Integer> cols = new ArrayList<>( total );

                    for ( int i = 0; i < total; i++ )
                    {
                        cols.add( i );
                    }

                    colRepeat.put( cat.getId(), cols );

                    catRepeat.put( cat.getId(), catColSpan );
                }
            }

            catColRepeat.put( categoryCombo.getId(), colRepeat );
        }

        // ---------------------------------------------------------------------
        // Get data entry form
        // ---------------------------------------------------------------------

        DataSet dsOriginal = dataSet;

        if ( dataSet.getFormType().isDefault() )
        {
            DataSet dataSetCopy = new DataSet();
            dataSetCopy.setUid( dataSet.getUid() );
            dataSetCopy.setCode( dataSet.getCode() );
            dataSetCopy.setName( dataSet.getName() );
            dataSetCopy.setShortName( dataSet.getShortName() );
            dataSetCopy.setRenderAsTabs( dataSet.isRenderAsTabs() );
            dataSetCopy.setRenderHorizontally( dataSet.isRenderHorizontally() );
            dataSetCopy.setDataElementDecoration( dataSet.isDataElementDecoration() );
            dataSetCopy.setCompulsoryDataElementOperands( dataSet.getCompulsoryDataElementOperands() );

            for ( int i = 0; i < orderedCategoryCombos.size(); i++ )
            {
                CategoryCombo categoryCombo = orderedCategoryCombos.get( i );
                String name = !categoryCombo.isDefault() ? categoryCombo.getName() : dataSetCopy.getName();

                Section section = new Section();
                section.setUid( CodeGenerator.generateUid() );
                section.setId( i );
                section.setName( name );
                section.setSortOrder( i );
                section.setDataSet( dataSetCopy );
                dataSetCopy.getSections().add( section );

                section.getDataElements().addAll( orderedDataElements.get( categoryCombo ) );

                if ( i == 0 )
                {
                    section.setIndicators( new ArrayList<>( dataSet.getIndicators() ) );
                }
            }

            dataSet = dataSetCopy;

            formType = FormType.SECTION;
        }

        // ---------------------------------------------------------------------
        // For multi-org unit only section forms supported
        // ---------------------------------------------------------------------

        if ( CodeGenerator.isValidUid( multiOrganisationUnit ) )
        {
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( multiOrganisationUnit );
            List<OrganisationUnit> organisationUnitChildren = new ArrayList<>();

            for ( OrganisationUnit child : organisationUnit.getChildren() )
            {
                if ( child.getDataSets().contains( dsOriginal ) )
                {
                    organisationUnitChildren.add( child );
                }
            }

            Collections.sort( organisationUnitChildren );

            organisationUnits.addAll( organisationUnitChildren );

            getSectionForm( dataSet );

            formType = FormType.SECTION_MULTIORG;
        }

        getSectionForm( dataSet );

        return formType.toString();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void getSectionForm( DataSet dataSet )
    {
        sections = new ArrayList<>( dataSet.getSections() );

        Collections.sort( sections, new SectionOrderComparator() );

        for ( Section section : sections )
        {
            if ( !section.getCategoryCombos().isEmpty() )
            {
                if ( section.isDisableDataElementAutoGroup() )
                {
                    processSectionForUserOrdering( section );
                }
                else
                {
                    processSectionForAutoOrdering( section );
                }
            }

            for ( DataElementOperand operand : section.getGreyedFields() )
            {
                if ( operand != null && operand.getDataElement() != null && operand.getCategoryOptionCombo() != null )
                {
                    greyedFields.put(
                        operand.getDataElement().getUid() + ":" + operand.getCategoryOptionCombo().getUid(), true );
                }
            }
        }
    }

    private void processSectionForUserOrdering( Section section )
    {
        Map<String, Collection<DataElement>> orderedDataElementsMap = sectionUtils.getOrderedDataElementsMap( section );

        List<String> sectionCategoryCombos = new ArrayList<>();

        for ( Map.Entry<String, Collection<DataElement>> entry : orderedDataElementsMap.entrySet() )
        {
            String key = entry.getKey();
            String[] split = key.split( "-" );

            if ( split.length > 0 )
            {
                sectionCategoryComboDataElements.put( section.getId() + "-" + key, entry.getValue() );

                sectionCategoryCombos.add( key );

                orderedSectionCategoryCombos.put( key, Long.parseLong( split[1] ) );
            }
        }

        sectionCombos.put( section.getId(), sectionCategoryCombos );
    }

    private void processSectionForAutoOrdering( Section section )
    {
        List<CategoryCombo> sortedCategoryCombos = getSortedCategoryCombos( section.getCategoryCombos() );

        for ( CategoryCombo categoryCombo : sortedCategoryCombos )
        {
            sectionCategoryComboDataElements.put( section.getId() + "-" + categoryCombo.getId(),
                section.getDataElementsByCategoryCombo( categoryCombo ) );

            orderedSectionCategoryCombos.put( String.valueOf( categoryCombo.getId() ), categoryCombo.getId() );
        }

        sectionCombos.put( section.getId(),
            sortedCategoryCombos.stream().map( ca -> String.valueOf( ca.getId() ) )
                .collect( Collectors.toList() ) );
    }

    private List<CategoryCombo> getCategoryCombos( List<DataElement> dataElements, DataSet dataSet )
    {
        Set<CategoryCombo> categoryCombos = new HashSet<>();

        for ( DataElement dataElement : dataElements )
        {
            categoryCombos.add( dataElement.getDataElementCategoryCombo( dataSet ) );
        }

        return getSortedCategoryCombos( categoryCombos );
    }

    private void addOptionAccess( User user, Map<String, Boolean> optionAccessMap,
        List<CategoryOptionCombo> optionCombos )
    {
        optionCombos.forEach( o -> {

            List<String> err = accessManager.canWrite( user, o );

            if ( !err.isEmpty() )
            {
                optionAccessMap.put( o.getUid(), false );
            }
            else
            {
                optionAccessMap.put( o.getUid(), true );
            }
        } );
    }

    private List<CategoryCombo> getSortedCategoryCombos( Set<CategoryCombo> categoryCombos )
    {
        List<CategoryCombo> listCategoryCombos = new ArrayList<>( categoryCombos );

        Collections.sort( listCategoryCombos, new CategoryComboSizeNameComparator() );

        return listCategoryCombos;
    }

    private Locale getLocaleWithDefault( TranslateParams translateParams )
    {
        return translateParams.isTranslate()
            ? translateParams.getLocaleWithDefault(
                (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE ) )
            : null;
    }
}
