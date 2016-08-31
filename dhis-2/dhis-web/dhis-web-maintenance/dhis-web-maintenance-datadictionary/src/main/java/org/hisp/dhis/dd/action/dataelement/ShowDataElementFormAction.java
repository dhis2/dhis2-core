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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.comparator.AttributeSortOrderComparator;
import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.legend.LegendService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.system.util.AttributeUtils;

import com.opensymphony.xwork2.Action;

/**
 * @author Hans S. Toemmerholt
 * @version $Id: GetDataElementAction.java 2869 2007-02-20 14:26:09Z andegje $
 */
public class ShowDataElementFormAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    public Map<Integer, String> attributeValues = new HashMap<>();
    private DataElementService dataElementService;
    private DataElementCategoryService dataElementCategoryService;
    private OrganisationUnitService organisationUnitService;
    private AttributeService attributeService;
    private OptionService optionService;
    private LegendService legendService;
    private Integer id;
    private DataElement dataElement;
    private Collection<DataElementGroup> dataElementGroups;
    private List<DataElementCategoryCombo> dataElementCategoryCombos;
    private List<OrganisationUnitLevel> organisationUnitLevels;

    // -------------------------------------------------------------------------
    // Input/output
    // -------------------------------------------------------------------------
    
    private List<OrganisationUnitLevel> aggregationLevels = new ArrayList<>();
    private DataElementCategoryCombo defaultCategoryCombo;
    private List<DataElementGroupSet> groupSets;
    private List<Attribute> attributes;
    private List<OptionSet> optionSets;
    private List<LegendSet> legendSets;
    private boolean update;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    public void setDataElementCategoryService( DataElementCategoryService dataElementCategoryService )
    {
        this.dataElementCategoryService = dataElementCategoryService;
    }

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    public void setAttributeService( AttributeService attributeService )
    {
        this.attributeService = attributeService;
    }

    public void setOptionService( OptionService optionService )
    {
        this.optionService = optionService;
    }

    public void setLegendService( LegendService legendService )
    {
        this.legendService = legendService;
    }

    public void setId( Integer id )
    {
        this.id = id;
    }

    public DataElement getDataElement()
    {
        return dataElement;
    }

    public Collection<DataElementGroup> getDataElementGroups()
    {
        return dataElementGroups;
    }

    public List<DataElementCategoryCombo> getDataElementCategoryCombos()
    {
        return dataElementCategoryCombos;
    }

    public List<OrganisationUnitLevel> getOrganisationUnitLevels()
    {
        return organisationUnitLevels;
    }

    public List<OrganisationUnitLevel> getAggregationLevels()
    {
        return aggregationLevels;
    }

    public DataElementCategoryCombo getDefaultCategoryCombo()
    {
        return defaultCategoryCombo;
    }

    public List<DataElementGroupSet> getGroupSets()
    {
        return groupSets;
    }

    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    public Map<Integer, String> getAttributeValues()
    {
        return attributeValues;
    }

    public List<OptionSet> getOptionSets()
    {
        return optionSets;
    }

    public List<LegendSet> getLegendSets()
    {
        return legendSets;
    }

    public boolean isUpdate()
    {
        return update;
    }

    public void setUpdate( boolean update )
    {
        this.update = update;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        defaultCategoryCombo = dataElementCategoryService
            .getDataElementCategoryComboByName( DataElementCategoryCombo.DEFAULT_CATEGORY_COMBO_NAME );

        dataElementCategoryCombos = new ArrayList<>( dataElementCategoryService
            .getDisaggregationCategoryCombos() );

        dataElementGroups = dataElementService.getAllDataElementGroups();

        Map<Integer, OrganisationUnitLevel> levelMap = organisationUnitService.getOrganisationUnitLevelMap();

        if ( id != null )
        {
            dataElement = dataElementService.getDataElement( id );

            for ( Integer level : dataElement.getAggregationLevels() )
            {
                aggregationLevels.add( levelMap.get( level ) );
                levelMap.remove( level );
            }

            attributeValues = AttributeUtils.getAttributeValueMap( dataElement.getAttributeValues() );
        }
        else
        {
            dataElement = new DataElement();
            dataElement.setCategoryCombo( defaultCategoryCombo );
        }

        organisationUnitLevels = new ArrayList<>( levelMap.values() );

        groupSets = new ArrayList<>( dataElementService
            .getCompulsoryDataElementGroupSetsWithMembers() );

        attributes = new ArrayList<>( attributeService.getDataElementAttributes() );

        optionSets = new ArrayList<>( optionService.getAllOptionSets() );

        legendSets = new ArrayList<>( legendService.getAllLegendSets() );

        Collections.sort( dataElementCategoryCombos, IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( groupSets, IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( attributes, AttributeSortOrderComparator.INSTANCE );
        Collections.sort( optionSets, IdentifiableObjectNameComparator.INSTANCE );
        Collections.sort( legendSets, IdentifiableObjectNameComparator.INSTANCE );

        return SUCCESS;
    }
}
