package org.hisp.dhis.de.action;

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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.comparator.DataElementCategoryComboSizeComparator;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.FormType;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.comparator.SectionOrderComparator;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

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

    private Map<DataElementCategoryCombo, List<DataElement>> orderedDataElements = new HashMap<>();

    public Map<DataElementCategoryCombo, List<DataElement>> getOrderedDataElements()
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

    private Map<Integer, Map<Integer, List<DataElementCategoryOption>>> orderedOptionsMap = new HashMap<>();

    public Map<Integer, Map<Integer, List<DataElementCategoryOption>>> getOrderedOptionsMap()
    {
        return orderedOptionsMap;
    }

    private Map<Integer, Collection<DataElementCategory>> orderedCategories = new HashMap<>();

    public Map<Integer, Collection<DataElementCategory>> getOrderedCategories()
    {
        return orderedCategories;
    }

    private Map<Integer, Integer> numberOfTotalColumns = new HashMap<>();

    public Map<Integer, Integer> getNumberOfTotalColumns()
    {
        return numberOfTotalColumns;
    }

    private Map<Integer, Map<Integer, Collection<Integer>>> catColRepeat = new HashMap<>();

    public Map<Integer, Map<Integer, Collection<Integer>>> getCatColRepeat()
    {
        return catColRepeat;
    }

    private Map<Integer, Collection<DataElementCategoryOptionCombo>> orderedCategoryOptionCombos = new HashMap<>();

    public Map<Integer, Collection<DataElementCategoryOptionCombo>> getOrderedCategoryOptionCombos()
    {
        return orderedCategoryOptionCombos;
    }

    private List<DataElementCategoryCombo> orderedCategoryCombos = new ArrayList<>();

    public List<DataElementCategoryCombo> getOrderedCategoryCombos()
    {
        return orderedCategoryCombos;
    }

    private Map<Integer, Integer> sectionCombos = new HashMap<>();

    public Map<Integer, Integer> getSectionCombos()
    {
        return sectionCombos;
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

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
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

        orderedDataElements = ListMap.getListMap( dataElements, de -> de.getCategoryCombo( dataSet ) );

        orderedCategoryCombos = getDataElementCategoryCombos( dataElements, dataSet );

        for ( DataElementCategoryCombo categoryCombo : orderedCategoryCombos )
        {
            List<DataElementCategoryOptionCombo> optionCombos = categoryCombo.getSortedOptionCombos();

            orderedCategoryOptionCombos.put( categoryCombo.getId(), optionCombos );

            // -----------------------------------------------------------------
            // Perform ordering of categories and their options so that they
            // could be displayed as in the paper form. Note that the total
            // number of entry cells to be generated are the multiple of options
            // from each category.
            // -----------------------------------------------------------------

            numberOfTotalColumns.put( categoryCombo.getId(), optionCombos.size() );

            orderedCategories.put( categoryCombo.getId(), categoryCombo.getCategories() );
  
            Map<Integer, List<DataElementCategoryOption>> optionsMap = new HashMap<>();

            for ( DataElementCategory category : categoryCombo.getCategories() )
            {
                optionsMap.put( category.getId(), category.getCategoryOptions() );
            }

            orderedOptionsMap.put( categoryCombo.getId(), optionsMap );

            // -----------------------------------------------------------------
            // Calculating the number of times each category should be repeated
            // -----------------------------------------------------------------

            Map<Integer, Integer> catRepeat = new HashMap<>();

            Map<Integer, Collection<Integer>> colRepeat = new HashMap<>();

            int catColSpan = optionCombos.size();

            for ( DataElementCategory cat : categoryCombo.getCategories() )
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
            dataSetCopy.setName( dataSet.getName() );
            dataSetCopy.setShortName( dataSet.getShortName() );
            dataSetCopy.setRenderAsTabs( dataSet.isRenderAsTabs() );
            dataSetCopy.setRenderHorizontally( dataSet.isRenderHorizontally() );
            dataSetCopy.setDataElementDecoration( dataSet.isDataElementDecoration() );
            dataSet = dataSetCopy;

            for ( int i = 0; i < orderedCategoryCombos.size(); i++ )
            {
                DataElementCategoryCombo categoryCombo = orderedCategoryCombos.get( i );
                String name = !categoryCombo.isDefault() ? categoryCombo.getName() : dataSetCopy.getName();

                Section section = new Section();
                section.setUid( CodeGenerator.generateCode() );
                section.setId( i );
                section.setName( name );
                section.setSortOrder( i );
                section.setDataSet( dataSetCopy );
                dataSetCopy.getSections().add( section );

                section.getDataElements().addAll( orderedDataElements.get( categoryCombo ) );
                section.setIndicators( new ArrayList<>( dataSet.getIndicators() ) );
            }

            formType = FormType.SECTION;
        }

        // ---------------------------------------------------------------------
        // For multi-org unit only section forms supported
        // ---------------------------------------------------------------------

        if ( CodeGenerator.isValidCode( multiOrganisationUnit ) )
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

            if ( organisationUnit.getDataSets().contains( dsOriginal ) )
            {
                organisationUnits.add( organisationUnit );
            }

            organisationUnits.addAll( organisationUnitChildren );

            getSectionForm( dataElements, dataSet );

            formType = FormType.SECTION_MULTIORG;
        }

        getSectionForm( dataElements, dataSet );

        return formType.toString();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void getSectionForm( Collection<DataElement> dataElements, DataSet dataSet )
    {
        sections = new ArrayList<>( dataSet.getSections() );

        Collections.sort( sections, new SectionOrderComparator() );

        for ( Section section : sections )
        {
            DataElementCategoryCombo sectionCategoryCombo = section.getCategoryCombo();

            if ( sectionCategoryCombo != null )
            {
                orderedCategoryCombos.add( sectionCategoryCombo );

                sectionCombos.put( section.getId(), sectionCategoryCombo.getId() );
            }

            for ( DataElementOperand operand : section.getGreyedFields() )
            {
                if ( operand != null && operand.getDataElement() != null && operand.getCategoryOptionCombo() != null )
                {
                    greyedFields.put( operand.getDataElement().getUid() + ":" + operand.getCategoryOptionCombo().getUid(), true );
                }
            }
        }
    }
    
    private List<DataElementCategoryCombo> getDataElementCategoryCombos( List<DataElement> dataElements, DataSet dataSet )
    {
        Set<DataElementCategoryCombo> categoryCombos = new HashSet<>();

        for ( DataElement dataElement : dataElements )
        {
            categoryCombos.add( dataElement.getCategoryCombo( dataSet ) );
        }

        List<DataElementCategoryCombo> listCategoryCombos = new ArrayList<>( categoryCombos );

        Collections.sort( listCategoryCombos, new DataElementCategoryComboSizeComparator() );

        return listCategoryCombos;
    }
}
