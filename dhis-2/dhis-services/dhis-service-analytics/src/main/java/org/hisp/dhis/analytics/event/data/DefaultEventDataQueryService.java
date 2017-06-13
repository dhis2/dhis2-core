package org.hisp.dhis.analytics.event.data;

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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.*;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.*;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.analytics.event.EventAnalyticsService.*;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.ITEM_SEP;
import static org.hisp.dhis.common.DimensionalObjectUtils.*;

/**
 * @author Lars Helge Overland
 */
public class DefaultEventDataQueryService
    implements EventDataQueryService
{
    private static final String COL_NAME_EVENTDATE = "executiondate";

    private static final ImmutableSet<String> SORTABLE_ITEMS = ImmutableSet.of( 
        ITEM_EVENT_DATE, ITEM_ORG_UNIT_NAME, ITEM_ORG_UNIT_CODE );

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private ProgramIndicatorService programIndicatorService;

    @Autowired
    private LegendSetService legendSetService;

    @Autowired
    private DataQueryService dataQueryService;
    
    @Autowired
    private I18nManager i18nManager;

    @Override
    public EventQueryParams getFromUrl( String program, String stage, Date startDate, Date endDate,
        Set<String> dimension, Set<String> filter, String value, AggregationType aggregationType, boolean skipMeta,
        boolean skipData, boolean skipRounding, boolean completedOnly, boolean hierarchyMeta, boolean showHierarchy,
        SortOrder sortOrder, Integer limit, EventOutputType outputType, EventStatus eventStatus, ProgramStatus programStatus, boolean collapseDataDimensions,
        boolean aggregateData, DisplayProperty displayProperty, Date relativePeriodDate, String userOrgUnit, DhisApiVersion apiVersion )
    {
        EventQueryParams query = getFromUrl( program, stage, startDate, endDate, dimension, filter, null, null, null,
            skipMeta, skipData, completedOnly, hierarchyMeta, false, eventStatus, programStatus, displayProperty, 
            relativePeriodDate, userOrgUnit, null, null, null, apiVersion );

        EventQueryParams params = new EventQueryParams.Builder( query )
            .withValue( getValueDimension( value ) )
            .withAggregationType( aggregationType )
            .withSkipRounding( skipRounding )
            .withShowHierarchy( showHierarchy )
            .withSortOrder( sortOrder )
            .withLimit( limit )
            .withOutputType( MoreObjects.firstNonNull( outputType, EventOutputType.EVENT ) )
            .withCollapseDataDimensions( collapseDataDimensions )
            .withAggregateData( aggregateData )
            .withProgramStatus( programStatus )
            .build();

        return params;
    }

    @Override
    public EventQueryParams getFromUrl( String program, String stage, Date startDate, Date endDate,
        Set<String> dimension, Set<String> filter, OrganisationUnitSelectionMode ouMode, Set<String> asc,
        Set<String> desc, boolean skipMeta, boolean skipData, boolean completedOnly, boolean hierarchyMeta,
        boolean coordinatesOnly, EventStatus eventStatus, ProgramStatus programStatus, DisplayProperty displayProperty,
        Date relativePeriodDate, String userOrgUnit, String coordinateField, Integer page, Integer pageSize, DhisApiVersion apiVersion )
    {
        I18nFormat format = i18nManager.getI18nFormat();
        
        EventQueryParams.Builder params = new EventQueryParams.Builder();
        
        IdScheme idScheme = IdScheme.UID;

        List<OrganisationUnit> userOrgUnits = dataQueryService.getUserOrgUnits( null, userOrgUnit );

        Program pr = programService.getProgram( program );

        if ( pr == null )
        {
            throw new IllegalQueryException( "Program does not exist: " + program );
        }

        ProgramStage ps = programStageService.getProgramStage( stage );

        if ( StringUtils.isNotEmpty( stage ) && ps == null )
        {
            throw new IllegalQueryException( "Program stage is specified but does not exist: " + stage );
        }

        if ( dimension != null )
        {
            for ( String dim : dimension )
            {
                String dimensionId = getDimensionFromParam( dim );
                List<String> items = getDimensionItemsFromParam( dim );
                DimensionalObject dimObj = dataQueryService.getDimension( dimensionId, 
                    items, relativePeriodDate, userOrgUnits, format, true, false, idScheme );

                if ( dimObj != null )
                {                    
                    params.addDimension( dimObj );
                }
                else
                {
                    params.addItem( getQueryItem( dim, pr ) );
                }
            }
        }

        if ( filter != null )
        {
            for ( String dim : filter )
            {
                String dimensionId = getDimensionFromParam( dim );
                List<String> items = getDimensionItemsFromParam( dim );
                DimensionalObject dimObj = dataQueryService.getDimension( dimensionId, 
                    items, relativePeriodDate, userOrgUnits, format, true, false, idScheme );

                if ( dimObj != null )
                {
                    params.addFilter( dimObj );
                }
                else
                {
                    params.addItemFilter( getQueryItem( dim, pr ) );
                }
            }
        }

        if ( asc != null )
        {
            for ( String sort : asc )
            {
                params.addAscSortItem( getSortItem( sort, pr ) );
            }
        }

        if ( desc != null )
        {
            for ( String sort : desc )
            {
                params.addDescSortItem( getSortItem( sort, pr ) );
            }
        }

        return params
            .withProgram( pr )
            .withProgramStage( ps )
            .withStartDate( startDate )
            .withEndDate( endDate )
            .withOrganisationUnitMode( ouMode )
            .withSkipMeta( skipMeta )
            .withSkipData( skipData )
            .withCompletedOnly( completedOnly )
            .withHierarchyMeta( hierarchyMeta )
            .withCoordinatesOnly( coordinatesOnly )
            .withEventStatus( eventStatus )
            .withDisplayProperty( displayProperty )
            .withCoordinateField( getCoordinateField( coordinateField ) )
            .withPage( page )
            .withPageSize( pageSize )
            .withProgramStatus( programStatus )
            .withApiVersion( apiVersion )
            .build();
    }

    @Override
    public EventQueryParams getFromAnalyticalObject( EventAnalyticalObject object )
    {
        Assert.notNull( object, "Event analytical object cannot be null" );
        Assert.notNull( object.getProgram(), "Event analytical object must specify a program" );
        
        EventQueryParams.Builder params = new EventQueryParams.Builder();
        
        I18nFormat format = i18nManager.getI18nFormat();        
        IdScheme idScheme = IdScheme.UID;        
        Date date = object.getRelativePeriodDate();

        object.populateAnalyticalProperties();

        for ( DimensionalObject dimension : ListUtils.union( object.getColumns(), object.getRows() ) )
        {
            DimensionalObject dimObj = dataQueryService.getDimension( dimension.getDimension(),
                getDimensionalItemIds( dimension.getItems() ), date, null, format, true, false, idScheme );

            if ( dimObj != null )
            {
                params.addDimension( dimObj );
            }
            else
            {
                params.addItem( getQueryItem( dimension.getDimension(), dimension.getFilter(), object.getProgram() ) );
            }
        }

        for ( DimensionalObject filter : object.getFilters() )
        {
            DimensionalObject dimObj = dataQueryService.getDimension( filter.getDimension(),
                getDimensionalItemIds( filter.getItems() ), date, null, format, true, false, idScheme );

            if ( dimObj != null )
            {
                params.addFilter( dimObj );
            }
            else
            {
                params.addItemFilter( getQueryItem( filter.getDimension(), filter.getFilter(), object.getProgram() ) );
            }
        }

        return params
            .withProgram( object.getProgram() )
            .withProgramStage( object.getProgramStage() )
            .withStartDate( object.getStartDate() )
            .withEndDate( object.getEndDate() )
            .withValue( object.getValue() )
            .withOutputType( object.getOutputType() )
            .build();
    }
    
    @Override
    public String getCoordinateField( String coordinateField )
    {
        if ( coordinateField == null || EventQueryParams.EVENT_COORDINATE_FIELD.equals( coordinateField ) )
        {
            return "geom";
        }
        
        DataElement dataElement = dataElementService.getDataElement( coordinateField );
        
        if ( dataElement != null )
        {
            if ( ValueType.COORDINATE != dataElement.getValueType() )
            {
                throw new IllegalQueryException( "Data element must be of value type coordinate to be used as coordinate field: " + coordinateField );
            }
            
            return dataElement.getUid();
        }
                
        TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( coordinateField );
        
        if ( attribute != null )
        {
            if ( ValueType.COORDINATE != attribute.getValueType() )
            {
                throw new IllegalQueryException( "Attribute must be of value type coordinate to be used as coordinate field: " + coordinateField );
            }
            
            return attribute.getUid();
        }
        
        throw new IllegalQueryException( "Cluster field not valid: " + coordinateField );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private QueryItem getQueryItem( String dimension, String filter, Program program )
    {
        if ( filter != null )
        {
            dimension += DIMENSION_NAME_SEP + filter;
        }

        return getQueryItem( dimension, program );
    }

    private QueryItem getQueryItem( String dimensionString, Program program )
    {
        String[] split = dimensionString.split( DIMENSION_NAME_SEP );

        if ( split == null || (split.length % 2 != 1) )
        {
            throw new IllegalQueryException( "Query item or filter is invalid: " + dimensionString );
        }

        QueryItem queryItem = getQueryItemFromDimension( split[0], program );

        if ( split.length > 1 ) // Filters specified
        {
            for ( int i = 1; i < split.length; i += 2 )
            {
                QueryOperator operator = QueryOperator.fromString( split[i] );
                QueryFilter filter = new QueryFilter( operator, split[i + 1] );
                queryItem.addFilter( filter );
            }
        }

        return queryItem;
    }

    private DimensionalItemObject getSortItem( String item, Program program )
    {
        QueryItem queryItem = getQueryItem( item, program );
        
        if ( !SORTABLE_ITEMS.contains( item.toLowerCase() ) && queryItem == null )
        {
            throw new IllegalQueryException( "Sort item is invalid: " + item );
        }

        item = ITEM_EVENT_DATE.equalsIgnoreCase( item ) ? COL_NAME_EVENTDATE : item;

        return queryItem.getItem();
    }

    private QueryItem getQueryItemFromDimension( String dimension, Program program )
    {
        String[] split = dimension.split( ITEM_SEP );

        String item = split[0];

        LegendSet legendSet = split.length > 1 && split[1] != null ? legendSetService.getLegendSet( split[1] ) : null;

        DataElement de = dataElementService.getDataElement( item );

        if ( de != null && program.containsDataElement( de ) )
        {
            ValueType valueType = legendSet != null ? ValueType.TEXT : de.getValueType();
            
            return new QueryItem( de, legendSet, valueType, de.getAggregationType(), de.getOptionSet() );
        }

        TrackedEntityAttribute at = attributeService.getTrackedEntityAttribute( item );

        if ( at != null && program.containsAttribute( at ) )
        {
            ValueType valueType = legendSet != null ? ValueType.TEXT : at.getValueType();
            
            return new QueryItem( at, legendSet, valueType, at.getAggregationType(), at.getOptionSet() );
        }

        ProgramIndicator pi = programIndicatorService.getProgramIndicatorByUid( item );

        if ( pi != null && program.getProgramIndicators().contains( pi ) )
        {
            return new QueryItem( pi, legendSet, ValueType.NUMBER, pi.getAggregationType(), null );
        }

        throw new IllegalQueryException(
            "Item identifier does not reference any data element, attribute or indicator part of the program: " + item );
    }

    private DimensionalItemObject getValueDimension( String value )
    {
        if ( value == null )
        {
            return null;
        }

        DataElement de = dataElementService.getDataElement( value );

        if ( de != null && de.isNumericType() )
        {
            return de;
        }

        TrackedEntityAttribute at = attributeService.getTrackedEntityAttribute( value );

        if ( at != null && at.isNumericType() )
        {
            return at;
        }

        throw new IllegalQueryException( "Value identifier does not reference any " +
            "data element or attribute which are numeric type and part of the program: " + value );
    }
}
