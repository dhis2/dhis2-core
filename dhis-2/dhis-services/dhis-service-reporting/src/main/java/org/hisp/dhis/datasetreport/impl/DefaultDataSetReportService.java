package org.hisp.dhis.datasetreport.impl;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import static org.hisp.dhis.dataentryform.DataEntryFormService.DATAELEMENT_TOTAL_PATTERN;
import static org.hisp.dhis.dataentryform.DataEntryFormService.IDENTIFIER_PATTERN;
import static org.hisp.dhis.dataentryform.DataEntryFormService.INDICATOR_PATTERN;
import static org.hisp.dhis.dataentryform.DataEntryFormService.INPUT_PATTERN;
import static org.hisp.dhis.datasetreport.DataSetReportStore.SEPARATOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.GridValue;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.FormType;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.comparator.SectionOrderComparator;
import org.hisp.dhis.datasetreport.DataSetReportService;
import org.hisp.dhis.datasetreport.DataSetReportStore;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.filter.AggregatableDataElementFilter;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.system.grid.ListGrid;
import org.springframework.beans.factory.annotation.Autowired;
import org.hisp.dhis.commons.filter.FilterUtils;

/**
 * @author Abyot Asalefew
 * @author Lars Helge Overland
 */
public class DefaultDataSetReportService
    implements DataSetReportService
{
    private static final String DEFAULT_HEADER = "Value";
    private static final String TOTAL_HEADER = "Total";
    private static final String SPACE = " ";

    private static final String ATTR_DE = "de";
    private static final String ATTR_CO = "co";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataValueService dataValueService;

    public void setDataValueService( DataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }

    private DataSetReportStore dataSetReportStore;

    public void setDataSetReportStore( DataSetReportStore dataSetReportStore )
    {
        this.dataSetReportStore = dataSetReportStore;
    }

    @Autowired
    private I18nManager i18nManager;

    // -------------------------------------------------------------------------
    // DataSetReportService implementation
    // -------------------------------------------------------------------------

    @Override
    public String getCustomDataSetReport( DataSet dataSet, Period period, OrganisationUnit orgUnit, Set<String> filters,
        boolean selectedUnitOnly )
    {
        Map<String, Object> valueMap = dataSetReportStore.getAggregatedValues( dataSet, period, orgUnit, filters );

        valueMap.putAll( dataSetReportStore.getAggregatedTotals( dataSet, period, orgUnit, filters ) );

        Map<String, Object> indicatorValueMap = dataSetReportStore.getAggregatedIndicatorValues( dataSet, period, orgUnit, filters );

        return prepareReportContent( dataSet.getDataEntryForm(), valueMap, indicatorValueMap );
    }

    @Override
    public List<Grid> getDataSetReportAsGrid( DataSet dataSet, Period period, OrganisationUnit orgUnit, Set<String> filters, boolean selectedUnitOnly )
    {
        List<Grid> grids = new ArrayList<>();

        FormType formType = dataSet.getFormType();

        if ( formType.isCustom() )
        {
            grids = getCustomDataSetReportAsGrid( dataSet, period, orgUnit, filters, selectedUnitOnly );
        }
        else if ( formType.isSection() )
        {
            grids = getSectionDataSetReport( dataSet, period, orgUnit, filters, selectedUnitOnly );
        }
        else
        {
            grids = getDefaultDataSetReport( dataSet, period, orgUnit, filters, selectedUnitOnly );
        }

        return grids;
    }

    // -------------------------------------------------------------------------
    // Data set report as grid for the various form types
    // -------------------------------------------------------------------------

    private List<Grid> getCustomDataSetReportAsGrid( DataSet dataSet, Period period, OrganisationUnit unit, Set<String> filters, boolean selectedUnitOnly )
    {
        String html = getCustomDataSetReport( dataSet, period, unit, filters, selectedUnitOnly );

        try
        {
            return GridUtils.fromHtml( html, dataSet.getName() );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Failed to render custom data set report as grid", ex );
        }
    }

    private List<Grid> getSectionDataSetReport( DataSet dataSet, Period period, OrganisationUnit unit, Set<String> filters, boolean selectedUnitOnly )
    {
        I18nFormat format = i18nManager.getI18nFormat();
        I18n i18n = i18nManager.getI18n();

        List<Section> sections = new ArrayList<>( dataSet.getSections() );
        Collections.sort( sections, new SectionOrderComparator() );

        Map<String, Object> valueMap = dataSetReportStore.getAggregatedValues( dataSet, period, unit, filters );
        Map<String, Object> subTotalMap = dataSetReportStore.getAggregatedSubTotals( dataSet, period, unit, filters );
        Map<String, Object> totalMap = dataSetReportStore.getAggregatedTotals( dataSet, period, unit, filters );

        List<Grid> grids = new ArrayList<>();

        // ---------------------------------------------------------------------
        // Create a grid for each section
        // ---------------------------------------------------------------------

        for ( Section section : sections )
        {
            for( CategoryCombo categoryCombo : section.getCategoryCombos() )
            {

                Grid grid = new ListGrid().setTitle( section.getName() + SPACE + categoryCombo.getName() ).
                    setSubtitle( unit.getName() + SPACE + format.formatPeriod( period ) );

                // -----------------------------------------------------------------
                // Grid headers
                // -----------------------------------------------------------------

                grid.addHeader( new GridHeader( i18n.getString( "dataelement" ), false, true ) );

                List<CategoryOptionCombo> optionCombos = categoryCombo.getSortedOptionCombos();

                for ( CategoryOptionCombo optionCombo : optionCombos )
                {
                    grid.addHeader( new GridHeader( optionCombo.isDefault() ? DEFAULT_HEADER : optionCombo.getName(),
                        false, false ) );
                }

                if ( categoryCombo.doSubTotals() && !selectedUnitOnly ) // Sub-total
                {
                    for ( CategoryOption categoryOption : categoryCombo.getCategoryOptions() )
                    {
                        grid.addHeader( new GridHeader( categoryOption.getName(), false, false ) );
                    }
                }

                if ( categoryCombo.doTotal() && !selectedUnitOnly ) // Total
                {
                    grid.addHeader( new GridHeader( TOTAL_HEADER, false, false ) );
                }

                // -----------------------------------------------------------------
                // Grid values
                // -----------------------------------------------------------------

                List<DataElement> dataElements = new ArrayList<>( section.getDataElementsByCategoryCombo( categoryCombo ) );

                FilterUtils.filter( dataElements, AggregatableDataElementFilter.INSTANCE );

                for ( DataElement dataElement : dataElements )
                {
                    grid.addRow();
                    grid.addValue( new GridValue( dataElement.getFormNameFallback() ) ); // Data element name

                    for ( CategoryOptionCombo optionCombo : optionCombos ) // Values
                    {
                        Map<Object, Object> attributes = new HashMap<>();
                        attributes.put( ATTR_DE, dataElement.getUid() );
                        attributes.put( ATTR_CO, optionCombo.getUid() );

                        Object value = null;

                        if ( selectedUnitOnly )
                        {
                            DataValue dataValue = dataValueService.getDataValue( dataElement, period, unit, optionCombo );
                            value = dataValue != null && dataValue.getValue() != null ? Double.parseDouble( dataValue
                                .getValue() ) : null;
                        }
                        else
                        {
                            value = valueMap.get( dataElement.getUid() + SEPARATOR + optionCombo.getUid() );
                        }

                        grid.addValue( new GridValue( value, attributes ) );
                    }

                    if ( categoryCombo.doSubTotals() && !selectedUnitOnly ) // Sub-total
                    {
                        for ( CategoryOption categoryOption : categoryCombo.getCategoryOptions() )
                        {
                            Object value = subTotalMap.get( dataElement.getUid() + SEPARATOR + categoryOption.getUid() );

                            grid.addValue( new GridValue( value ) );
                        }
                    }

                    if ( categoryCombo.doTotal() && !selectedUnitOnly ) // Total
                    {
                        Object value = totalMap.get( String.valueOf( dataElement.getUid() ) );

                        grid.addValue( new GridValue( value ) );
                    }
                }

                grids.add( grid );

            }
        }

        return grids;
    }

    private List<Grid> getDefaultDataSetReport( DataSet dataSet, Period period, OrganisationUnit unit, Set<String> filters, boolean selectedUnitOnly )
    {
        ListMap<CategoryCombo, DataElement> map = new ListMap<>();

        for ( DataSetElement element : dataSet.getDataSetElements() )
        {
            map.putValue( element.getResolvedCategoryCombo(), element.getDataElement() );
        }

        DataSet tmpDataSet = new DataSet( dataSet.getName(), dataSet.getShortName(), dataSet.getPeriodType() );
        tmpDataSet.setDataSetElements( dataSet.getDataSetElements() );

        for ( CategoryCombo categoryCombo : map.keySet() )
        {
            List<DataElement> dataElements = map.get( categoryCombo );

            String name = categoryCombo.isDefault() ? dataSet.getName() : categoryCombo.getName();

            Section section = new Section( name, dataSet, dataElements, null );

            tmpDataSet.getSections().add( section );
        }

        return getSectionDataSetReport( tmpDataSet, period, unit, filters, selectedUnitOnly );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Puts in aggregated datavalues in the custom dataentry form and returns
     * whole report text.
     *
     * @param dataEntryForm the data entry form.
     * @param a map with aggregated data values mapped to data element operands.
     * @return data entry form HTML code populated with aggregated data in the
     *         input fields.
     */
    private String prepareReportContent( DataEntryForm dataEntryForm, Map<String, Object> dataValues,
        Map<String, Object> indicatorValues )
    {
        I18nFormat format = i18nManager.getI18nFormat();

        StringBuffer buffer = new StringBuffer();

        Matcher inputMatcher = INPUT_PATTERN.matcher( dataEntryForm.getHtmlCode() );

        // ---------------------------------------------------------------------
        // Iterate through all matching data element fields.
        // ---------------------------------------------------------------------

        while ( inputMatcher.find() )
        {
            // -----------------------------------------------------------------
            // Get input HTML code
            // -----------------------------------------------------------------

            String inputHtml = inputMatcher.group( 1 );

            Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher( inputHtml );
            Matcher dataElementTotalMatcher = DATAELEMENT_TOTAL_PATTERN.matcher( inputHtml );
            Matcher indicatorMatcher = INDICATOR_PATTERN.matcher( inputHtml );

            // -----------------------------------------------------------------
            // Find existing data or indicator value and replace input tag
            // -----------------------------------------------------------------

            if ( identifierMatcher.find() && identifierMatcher.groupCount() > 0 )
            {
                String dataElementId = identifierMatcher.group( 1 );
                String optionComboId = identifierMatcher.group( 2 );

                Object dataValue = dataValues.get( dataElementId + SEPARATOR + optionComboId );

                String value = "<span class=\"val\" data-de=\"" + dataElementId + "\" data-co=\"" + optionComboId + "\">" + format.formatValue( dataValue ) + "</span>";

                inputMatcher.appendReplacement( buffer, Matcher.quoteReplacement( value ) );
            }
            else if ( dataElementTotalMatcher.find() && dataElementTotalMatcher.groupCount() > 0 )
            {
                String dataElementId = dataElementTotalMatcher.group( 1 );

                Object dataValue = dataValues.get( dataElementId );

                inputMatcher.appendReplacement( buffer, Matcher.quoteReplacement( format.formatValue( dataValue ) ) );
            }
            else if ( indicatorMatcher.find() && indicatorMatcher.groupCount() > 0 )
            {
                String indicatorId = indicatorMatcher.group( 1 );

                Object indicatorValue = indicatorValues.get( indicatorId );

                inputMatcher.appendReplacement( buffer, Matcher.quoteReplacement( format.formatValue( indicatorValue ) ) );
            }
        }

        inputMatcher.appendTail( buffer );

        return buffer.toString();
    }
}
