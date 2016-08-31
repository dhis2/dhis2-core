package org.hisp.dhis.dd.action.dataelement;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.legend.LegendService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.system.util.AttributeUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Torgeir Lorange Ostby
 */
public class UpdateDataElementAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private OptionService optionService;

    @Autowired
    private LegendService legendService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String shortName;

    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    private String code;

    public void setCode( String code )
    {
        this.code = code;
    }

    private String description;

    public void setDescription( String description )
    {
        this.description = description;
    }

    private String formName;

    public void setFormName( String formName )
    {
        this.formName = formName;
    }

    private String domainType;

    public void setDomainType( String domainType )
    {
        this.domainType = domainType;
    }

    private ValueType valueType;

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    private String aggregationType;

    public void setAggregationType( String aggregationType )
    {
        this.aggregationType = aggregationType;
    }

    private String url;

    public void setUrl( String url )
    {
        this.url = url;
    }

    private Collection<String> aggregationLevels;

    public void setAggregationLevels( Collection<String> aggregationLevels )
    {
        this.aggregationLevels = aggregationLevels;
    }

    private Integer selectedCategoryComboId;

    public void setSelectedCategoryComboId( Integer selectedCategoryComboId )
    {
        this.selectedCategoryComboId = selectedCategoryComboId;
    }

    private boolean zeroIsSignificant;

    public void setZeroIsSignificant( boolean zeroIsSignificant )
    {
        this.zeroIsSignificant = zeroIsSignificant;
    }

    private List<String> dataElementGroupSets = new ArrayList<>();

    public void setDataElementGroupSets( List<String> dataElementGroupSets )
    {
        this.dataElementGroupSets = dataElementGroupSets;
    }

    private List<String> dataElementGroups = new ArrayList<>();

    public void setDataElementGroups( List<String> dataElementGroups )
    {
        this.dataElementGroups = dataElementGroups;
    }

    private List<String> jsonAttributeValues;

    public void setJsonAttributeValues( List<String> jsonAttributeValues )
    {
        this.jsonAttributeValues = jsonAttributeValues;
    }

    private Integer selectedOptionSetId;

    public void setSelectedOptionSetId( Integer selectedOptionSetId )
    {
        this.selectedOptionSetId = selectedOptionSetId;
    }

    private Integer selectedCommentOptionSetId;

    public void setSelectedCommentOptionSetId( Integer selectedCommentOptionSetId )
    {
        this.selectedCommentOptionSetId = selectedCommentOptionSetId;
    }

    private Integer selectedLegendSetId;

    public void setSelectedLegendSetId( Integer selectedLegendSetId )
    {
        this.selectedLegendSetId = selectedLegendSetId;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        DataElement dataElement = dataElementService.getDataElement( id );

        DataElementCategoryCombo categoryCombo = dataElementCategoryService
            .getDataElementCategoryCombo( selectedCategoryComboId );

        OptionSet optionSet = optionService.getOptionSet( selectedOptionSetId );
        OptionSet commentOptionSet = optionService.getOptionSet( selectedCommentOptionSetId );
        LegendSet legendSet = legendService.getLegendSet( selectedLegendSetId );

        valueType = optionSet != null && optionSet.getValueType() != null ? optionSet.getValueType() : valueType;
        
        dataElement.setName( StringUtils.trimToNull( name ) );
        dataElement.setShortName( StringUtils.trimToNull( shortName ) );
        dataElement.setCode( StringUtils.trimToNull( code ) );
        dataElement.setDescription( StringUtils.trimToNull( description ) );
        dataElement.setFormName( StringUtils.trimToNull( formName ) );
        dataElement.setDomainType( DataElementDomain.valueOf( domainType ) );
        dataElement.setValueType( valueType );
        dataElement.setAggregationType( AggregationType.valueOf( aggregationType ) );
        dataElement.setUrl( url );
        dataElement.setZeroIsSignificant( zeroIsSignificant );
        dataElement.setCategoryCombo( categoryCombo );
        dataElement.setAggregationLevels( new ArrayList<>( ListUtils.getIntegerCollection( aggregationLevels ) ) );
        dataElement.setOptionSet( optionSet );
        dataElement.setCommentOptionSet( commentOptionSet );
        dataElement.setLegendSet( legendSet );

        Set<DataSet> dataSets = dataElement.getDataSets();

        for ( DataSet dataSet : dataSets )
        {
            dataSet.increaseVersion();
            dataSetService.updateDataSet( dataSet );
        }

        for ( int i = 0; i < dataElementGroupSets.size(); i++ )
        {
            DataElementGroupSet groupSet = dataElementService.getDataElementGroupSet( Integer
                .parseInt( dataElementGroupSets.get( i ) ) );

            DataElementGroup oldGroup = groupSet.getGroup( dataElement );
            DataElementGroup newGroup = dataElementService.getDataElementGroup( Integer.parseInt( dataElementGroups.get( i ) ) );

            if ( oldGroup != null && oldGroup.getMembers().remove( dataElement ) )
            {
                oldGroup.removeDataElement( dataElement );
                dataElementService.updateDataElementGroup( oldGroup );
            }

            if ( newGroup != null && newGroup.getMembers().add( dataElement ) )
            {
                newGroup.addDataElement( dataElement );
                dataElementService.updateDataElementGroup( newGroup );
            }
        }

        if ( jsonAttributeValues != null )
        {
            AttributeUtils.updateAttributeValuesFromJson( dataElement.getAttributeValues(), jsonAttributeValues, attributeService );
        }

        dataElementService.updateDataElement( dataElement );

        return SUCCESS;
    }
}
