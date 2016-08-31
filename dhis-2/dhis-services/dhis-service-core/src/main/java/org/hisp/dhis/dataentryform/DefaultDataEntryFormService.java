package org.hisp.dhis.dataentryform;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.system.callable.IdentifiableObjectCallable;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml3;

/**
 * @author Bharath Kumar
 */
@Transactional
public class DefaultDataEntryFormService
    implements DataEntryFormService
{
    private static final Log log = LogFactory.getLog( DefaultDataEntryFormService.class );

    private static final String EMPTY_VALUE_TAG = "value=\"\"";
    private static final String EMPTY_TITLE_TAG = "title=\"\"";
    private static final String TAG_CLOSE = "/>";
    private static final String EMPTY = "";

    // ------------------------------------------------------------------------
    // Dependencies
    // ------------------------------------------------------------------------

    private DataEntryFormStore dataEntryFormStore;

    public void setDataEntryFormStore( DataEntryFormStore dataEntryFormStore )
    {
        this.dataEntryFormStore = dataEntryFormStore;
    }

    private IdentifiableObjectManager idObjectManager;

    public void setIdObjectManager( IdentifiableObjectManager idObjectManager )
    {
        this.idObjectManager = idObjectManager;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private IndicatorService indicatorService;

    public void setIndicatorService( IndicatorService indicatorService )
    {
        this.indicatorService = indicatorService;
    }

    // ------------------------------------------------------------------------
    // Implemented Methods
    // ------------------------------------------------------------------------

    @Override
    public int addDataEntryForm( DataEntryForm dataEntryForm )
    {
        if ( dataEntryForm != null )
        {
            dataEntryForm.setFormat( DataEntryForm.CURRENT_FORMAT );
        }

        return dataEntryFormStore.save( dataEntryForm );
    }

    @Override
    public void updateDataEntryForm( DataEntryForm dataEntryForm )
    {
        dataEntryFormStore.update( dataEntryForm );
    }

    @Override
    public void deleteDataEntryForm( DataEntryForm dataEntryForm )
    {
        dataEntryFormStore.delete( dataEntryForm );
    }

    @Override
    public DataEntryForm getDataEntryForm( int id )
    {
        return dataEntryFormStore.get( id );
    }

    @Override
    public DataEntryForm getDataEntryFormByName( String name )
    {
        return dataEntryFormStore.getDataEntryFormByName( name );
    }

    @Override
    public List<DataEntryForm> getAllDataEntryForms()
    {
        return dataEntryFormStore.getAll();
    }

    @Override
    public String prepareDataEntryFormForSave( String htmlCode )
    {
        if ( htmlCode == null )
        {
            return null;
        }

        StringBuffer sb = new StringBuffer();

        Matcher inputMatcher = INPUT_PATTERN.matcher( htmlCode );

        while ( inputMatcher.find() )
        {
            // -----------------------------------------------------------------
            // Remove value and title tags from the HTML code
            // -----------------------------------------------------------------

            String dataElementCode = inputMatcher.group();

            Matcher valueTagMatcher = VALUE_TAG_PATTERN.matcher( dataElementCode );
            Matcher titleTagMatcher = TITLE_TAG_PATTERN.matcher( dataElementCode );

            if ( valueTagMatcher.find() && valueTagMatcher.groupCount() > 0 )
            {
                dataElementCode = dataElementCode.replace( valueTagMatcher.group( 1 ), EMPTY );
            }

            if ( titleTagMatcher.find() && titleTagMatcher.groupCount() > 0 )
            {
                dataElementCode = dataElementCode.replace( titleTagMatcher.group( 1 ), EMPTY );
            }

            inputMatcher.appendReplacement( sb, dataElementCode );
        }

        inputMatcher.appendTail( sb );

        return sb.toString();
    }

    @Override
    public String prepareDataEntryFormForEdit( DataEntryForm dataEntryForm, DataSet dataSet, I18n i18n )
    {
        //TODO HTML encode names

        if ( dataEntryForm == null || !dataEntryForm.hasForm() || dataSet == null )
        {
            return null;
        }

        CachingMap<String, DataElementCategoryOptionCombo> optionComboMap = new CachingMap<>();

        optionComboMap.putAll( IdentifiableObjectUtils.getUidObjectMap( dataSet.getDataElementOptionCombos() ) );

        IdentifiableObjectCallable<DataElementCategoryOptionCombo> optionComboCallabel =
            new IdentifiableObjectCallable<>( idObjectManager, DataElementCategoryOptionCombo.class, null );

        StringBuffer sb = new StringBuffer();

        Matcher inputMatcher = INPUT_PATTERN.matcher( dataEntryForm.getHtmlCode() );

        while ( inputMatcher.find() )
        {
            String inputHtml = inputMatcher.group();

            Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher( inputHtml );
            Matcher dataElementTotalMatcher = DATAELEMENT_TOTAL_PATTERN.matcher( inputHtml );
            Matcher indicatorMatcher = INDICATOR_PATTERN.matcher( inputHtml );

            String displayValue = null;
            String displayTitle = null;

            if ( identifierMatcher.find() && identifierMatcher.groupCount() > 0 )
            {
                String dataElementId = identifierMatcher.group( 1 );
                DataElement dataElement = dataElementService.getDataElement( dataElementId );

                String optionComboId = identifierMatcher.group( 2 );

                DataElementCategoryOptionCombo categoryOptionCombo = optionComboMap.get( optionComboId, optionComboCallabel.setId( optionComboId ) );

                String optionComboName = categoryOptionCombo != null ? escapeHtml3( categoryOptionCombo.getName() ) : "[ " + i18n.getString( "cat_option_combo_not_exist" ) + " ]";

                StringBuilder title = dataElement != null ?
                    new StringBuilder( "title=\"" ).append( dataElementId ).append( " - " ).
                        append( escapeHtml3( dataElement.getDisplayName() ) ).append( " - " ).append( optionComboId ).append( " - " ).
                        append( optionComboName ).append( " - " ).append( dataElement.getValueType() ).append( "\"" ) : new StringBuilder();

                displayValue = dataElement != null ? "value=\"[ " + escapeHtml3( dataElement.getDisplayName() ) + " " + optionComboName + " ]\"" : "[ " + i18n.getString( "data_element_not_exist" ) + " ]";
                displayTitle = dataElement != null ? title.toString() : "[ " + i18n.getString( "dataelement_not_exist" ) + " ]";
            }
            else if ( dataElementTotalMatcher.find() && dataElementTotalMatcher.groupCount() > 0 )
            {
                String dataElementId = dataElementTotalMatcher.group( 1 );
                DataElement dataElement = dataElementService.getDataElement( dataElementId );

                displayValue = dataElement != null ? "value=\"[ " + escapeHtml3( dataElement.getDisplayName() ) + " ]\"" : "[ " + i18n.getString( "data_element_not_exist" ) + " ]";
                displayTitle = dataElement != null ? "title=\"" + escapeHtml3( dataElement.getDisplayName() ) + "\"" : "[ " + i18n.getString( "data_element_not_exist" ) + " ]";
            }
            else if ( indicatorMatcher.find() && indicatorMatcher.groupCount() > 0 )
            {
                String indicatorId = indicatorMatcher.group( 1 );
                Indicator indicator = indicatorService.getIndicator( indicatorId );

                displayValue = indicator != null ? "value=\"[ " + escapeHtml3( indicator.getDisplayName() ) + " ]\"" : "[ " + i18n.getString( "indicator_not_exist" ) + " ]";
                displayTitle = indicator != null ? "title=\"" + escapeHtml3( indicator.getDisplayName() ) + "\"" : "[ " + i18n.getString( "indicator_not_exist" ) + " ]";
            }

            // -----------------------------------------------------------------
            // Insert name of data element operand as value and title
            // -----------------------------------------------------------------

            if ( displayValue == null || displayTitle == null )
            {
                log.warn( "Ignoring invalid form markup: '" + inputHtml + "'" );
                continue;
            }

            inputHtml = inputHtml.contains( EMPTY_VALUE_TAG ) ? inputHtml.replace( EMPTY_VALUE_TAG, displayValue ) : inputHtml.replace( TAG_CLOSE, (displayValue + TAG_CLOSE) );
            inputHtml = inputHtml.contains( EMPTY_TITLE_TAG ) ? inputHtml.replace( EMPTY_TITLE_TAG, displayTitle ) : inputHtml.replace( TAG_CLOSE, (displayTitle + TAG_CLOSE) );

            inputMatcher.appendReplacement( sb, inputHtml );
        }

        inputMatcher.appendTail( sb );

        return sb.toString();
    }

    @Override
    public String prepareDataEntryFormForEntry( DataEntryForm dataEntryForm, DataSet dataSet, I18n i18n )
    {
        //TODO HTML encode names

        if ( dataEntryForm == null || !dataEntryForm.hasForm() || dataSet == null )
        {
            return null;
        }

        // ---------------------------------------------------------------------
        // Inline javascript/html to add to HTML before output
        // ---------------------------------------------------------------------

        Map<String, DataElement> dataElementMap = getDataElementMap( dataSet );

        CachingMap<String, DataElementCategoryOptionCombo> optionComboMap = new CachingMap<>();

        optionComboMap.putAll( IdentifiableObjectUtils.getUidObjectMap( dataSet.getDataElementOptionCombos() ) );

        IdentifiableObjectCallable<DataElementCategoryOptionCombo> optionComboCallabel =
            new IdentifiableObjectCallable<>( idObjectManager, DataElementCategoryOptionCombo.class, null );

        int i = 1;

        StringBuffer sb = new StringBuffer();

        Matcher inputMatcher = INPUT_PATTERN.matcher( dataEntryForm.getHtmlCode() );

        while ( inputMatcher.find() )
        {
            // -----------------------------------------------------------------
            // Get HTML input field code
            // -----------------------------------------------------------------

            String inputHtml = inputMatcher.group();

            Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher( inputHtml );
            Matcher dataElementTotalMatcher = DATAELEMENT_TOTAL_PATTERN.matcher( inputHtml );
            Matcher indicatorMatcher = INDICATOR_PATTERN.matcher( inputHtml );

            if ( identifierMatcher.find() && identifierMatcher.groupCount() > 0 )
            {
                String dataElementId = identifierMatcher.group( 1 );
                String optionComboId = identifierMatcher.group( 2 );

                DataElement dataElement = dataElementMap.get( dataElementId );

                if ( dataElement == null )
                {
                    return i18n.getString( "dataelement_with_id" ) + ": " + dataElementId + " " + i18n.getString( "does_not_exist_in_data_set" );
                }

                DataElementCategoryOptionCombo categoryOptionCombo = optionComboMap.get( optionComboId, optionComboCallabel.setId( optionComboId ) );

                if ( categoryOptionCombo == null )
                {
                    return i18n.getString( "category_option_combo_with_id" ) + ": " + optionComboId + " " + i18n.getString( "does_not_exist_in_data_set" );
                }

                if ( dataSet.isDataElementDecoration() && dataElement.hasDescription() )
                {
                    String titleTag = " title=\"" + escapeHtml3( dataElement.getDisplayDescription() ) + "\" ";
                    inputHtml = inputHtml.replaceAll( "title=\".*?\"", "" ).replace( TAG_CLOSE, titleTag + TAG_CLOSE );
                }

                String appendCode = "";
                ValueType valueType = dataElement.getValueType();

                if ( ValueType.BOOLEAN == valueType )
                {
                    inputHtml = inputHtml.replaceAll(inputHtml, TAG_CLOSE);
                    
                    appendCode += "<label>";
                    appendCode += "<input type=\"radio\" class=\"entryselect\" name=\"" + dataElementId + "-" + optionComboId + "-val\"  id=\"" + dataElementId + "-" + optionComboId + "-val\" tabindex=\"" + i++ + "\" value=\"\">";
                    appendCode += i18n.getString( "no_value" );
                    appendCode += "</label>";
                    
                    appendCode += "<label>";
                    appendCode += "<input type=\"radio\" class=\"entryselect\" name=\"" + dataElementId + "-" + optionComboId + "-val\"  id=\"" + dataElementId + "-" + optionComboId + "-val\" tabindex=\"" + i++ + "\" value=\"true\">";
                    appendCode += i18n.getString( "yes" );
                    appendCode += "</label>";
                    
                    appendCode += "<label>";
                    appendCode += "<input type=\"radio\" class=\"entryselect\" name=\"" + dataElementId + "-" + optionComboId + "-val\"  id=\"" + dataElementId + "-" + optionComboId + "-val\" tabindex=\"" + i++ + "\" value=\"false\">";
                    appendCode += i18n.getString( "no" );
                    appendCode += "</label>";
                }
                else if ( ValueType.TRUE_ONLY == valueType )
                {
                    appendCode += " name=\"entrytrueonly\" class=\"entrytrueonly\" type=\"checkbox\" tabindex=\"" + i++ + "\"" + TAG_CLOSE;
                }
                else if ( dataElement.hasOptionSet() )
                {
                    appendCode += " name=\"entryoptionset\" class=\"entryoptionset\" tabindex=\"" + i++ + "\"" + TAG_CLOSE;
                }
                else if ( ValueType.LONG_TEXT == valueType )
                {
                    inputHtml = inputHtml.replace( "input", "textarea" );

                    appendCode += " name=\"entryfield\" class=\"entryfield entryarea\" tabindex=\"" + i++ + "\"" + "></textarea>";
                }
                else if ( ValueType.FILE_RESOURCE == valueType )
                {
                    inputHtml = inputHtml.replace( "input", "div" );

                    appendCode += " class=\"entryfileresource\" tabindex=\"" + i++ + "\">" +
                                    "<input class=\"entryfileresource-input\" id=\"input-"+ dataElementId + "-" + optionComboId + "-val\">" +
                                    "<div class=\"upload-field\">" +
                                        "<div class=\"upload-fileinfo\">" +
                                            "<div class=\"upload-fileinfo-size\"></div>" +
                                            "<div class=\"upload-fileinfo-name\"></div>" +
                                        "</div>" +
                                        "<div class=\"upload-progress\">" +
                                            "<div class=\"upload-progress-bar\"></div>" +
                                            "<div class=\"upload-progress-info\"></div>" +
                                        "</div>" +
                                    "</div>" +
                                    "<div class=\"upload-button-group\">" +
                                        "<button class=\"upload-button\"></button>" +
                                    "</div>" +
                                    "<input type=\"file\" style=\"display: none;\">" +
                                "</div>";
                }
                else if ( ValueType.TIME == valueType ) {
                    appendCode += " type=\"text\" name=\"entrytime\" class=\"entrytime\" tabindex=\"" + i++ + "\" id=\""+ dataElementId + "-" + optionComboId + "\">";
                }
                else
                {
                    appendCode += " type=\"text\" name=\"entryfield\" class=\"entryfield\" tabindex=\"" + i++ + "\"" + TAG_CLOSE;
                }

                inputHtml = inputHtml.replace( TAG_CLOSE, appendCode );

                inputHtml += "<span id=\"" + dataElement.getUid() + "-dataelement\" style=\"display:none\">" + dataElement.getFormNameFallback() + "</span>";
                inputHtml += "<span id=\"" + categoryOptionCombo.getUid() + "-optioncombo\" style=\"display:none\">" + categoryOptionCombo.getName() + "</span>";
            }
            else if ( dataElementTotalMatcher.find() && dataElementTotalMatcher.groupCount() > 0 )
            {
                inputHtml = inputHtml.replace( TAG_CLOSE, " type=\"text\" class=\"dataelementtotal\"" + TAG_CLOSE );
            }
            else if ( indicatorMatcher.find() && indicatorMatcher.groupCount() > 0 )
            {
                inputHtml = inputHtml.replace( TAG_CLOSE, " type=\"text\" class=\"indicator\"" + TAG_CLOSE );
            }

            inputMatcher.appendReplacement( sb, inputHtml );
        }

        inputMatcher.appendTail( sb );

        return sb.toString();
    }

    @Override
    public Set<DataElement> getDataElementsInDataEntryForm( DataSet dataSet )
    {
        if ( dataSet == null || !dataSet.hasDataEntryForm() )
        {
            return null;
        }

        Map<String, DataElement> dataElementMap = getDataElementMap( dataSet );

        Set<DataElement> dataElements = new HashSet<>();

        Matcher inputMatcher = INPUT_PATTERN.matcher( dataSet.getDataEntryForm().getHtmlCode() );

        while ( inputMatcher.find() )
        {
            String inputHtml = inputMatcher.group();

            Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher( inputHtml );
            Matcher dataElementTotalMatcher = DATAELEMENT_TOTAL_PATTERN.matcher( inputHtml );

            DataElement dataElement = null;

            if ( identifierMatcher.find() && identifierMatcher.groupCount() > 0 )
            {
                String dataElementId = identifierMatcher.group( 1 );
                dataElement = dataElementMap.get( dataElementId );
            }
            else if ( dataElementTotalMatcher.find() && dataElementTotalMatcher.groupCount() > 0 )
            {
                String dataElementId = dataElementTotalMatcher.group( 1 );
                dataElement = dataElementMap.get( dataElementId );
            }

            if ( dataElement != null )
            {
                dataElements.add( dataElement );
            }
        }

        return dataElements;
    }

    @Override
    public Set<DataElementOperand> getOperandsInDataEntryForm( DataSet dataSet )
    {
        if ( dataSet == null || !dataSet.hasDataEntryForm() )
        {
            return null;
        }

        Set<DataElementOperand> operands = new HashSet<>();

        Matcher inputMatcher = INPUT_PATTERN.matcher( dataSet.getDataEntryForm().getHtmlCode() );

        while ( inputMatcher.find() )
        {
            String inputHtml = inputMatcher.group();

            Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher( inputHtml );

            if ( identifierMatcher.find() && identifierMatcher.groupCount() > 0 )
            {
                String dataElementId = identifierMatcher.group( 1 );
                String categoryOptionComboId = identifierMatcher.group( 2 );

                DataElementOperand operand = new DataElementOperand( dataElementId, categoryOptionComboId );

                operands.add( operand );
            }
        }

        return operands;
    }

    @Override
    public List<DataEntryForm> listDistinctDataEntryFormByProgramStageIds( List<Integer> programStageIds )
    {
        if ( programStageIds == null || programStageIds.isEmpty() )
        {
            return null;
        }

        return dataEntryFormStore.listDistinctDataEntryFormByProgramStageIds( programStageIds );
    }

    @Override
    public List<DataEntryForm> listDistinctDataEntryFormByDataSetIds( List<Integer> dataSetIds )
    {
        if ( dataSetIds == null || dataSetIds.size() == 0 )
        {
            return null;
        }

        return dataEntryFormStore.listDistinctDataEntryFormByDataSetIds( dataSetIds );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a Map of all DataElements in the given DataSet where the key is
     * the DataElement identifier and the value is the DataElement.
     */
    private Map<String, DataElement> getDataElementMap( DataSet dataSet )
    {
        Map<String, DataElement> map = new HashMap<>();

        for ( DataElement element : dataSet.getDataElements() )
        {
            map.put( element.getUid(), element );
        }

        return map;
    }
}
