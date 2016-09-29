package org.hisp.dhis.analytics.event.data;

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

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.ITEM_SEP;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;

import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_EXECUTION_DATE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_ORG_UNIT_CODE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_ORG_UNIT_NAME;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.legend.LegendService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class DefaultEventDataQueryService
    implements EventDataQueryService
{
    private static final String COL_NAME_EVENTDATE = "executiondate";

    private static final List<String> SORTABLE_ITEMS = Lists.newArrayList( ITEM_EXECUTION_DATE, ITEM_ORG_UNIT_NAME,
        ITEM_ORG_UNIT_CODE );

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
    private LegendService legendService;

    @Autowired
    private DataQueryService dataQueryService;
    
    @Autowired
    private I18nManager i18nManager;

    @Override
    public EventQueryParams getFromUrl( String program, String stage, String startDate, String endDate,
        Set<String> dimension, Set<String> filter, String value, AggregationType aggregationType, boolean skipMeta,
        boolean skipData, boolean skipRounding, boolean completedOnly, boolean hierarchyMeta, boolean showHierarchy,
        SortOrder sortOrder, Integer limit, EventOutputType outputType, EventStatus eventStatus, boolean collapseDataDimensions,
        boolean aggregateData, DisplayProperty displayProperty, String userOrgUnit, I18nFormat format )
    {
        EventQueryParams query = getFromUrl( program, stage, startDate, endDate, dimension, filter, null, null, null,
            skipMeta, skipData, completedOnly, hierarchyMeta, false, eventStatus, displayProperty, userOrgUnit, null, null, format );

        EventQueryParams params = new EventQueryParams.Builder( query )
            .withValue( getValueDimension( value ) )
            .withAggregationType( aggregationType )
            .withSkipRounding( skipRounding )
            .withShowHierarchy( showHierarchy )
            .withSortOrder( sortOrder )
            .withLimit( limit )
            .withOutputType( MoreObjects.firstNonNull( outputType, EventOutputType.EVENT ) )
            .withCollapseDataDimensions( collapseDataDimensions )
            .withAggregateData( aggregateData ).build();

        return params;
    }

    @Override
    public EventQueryParams getFromUrl( String program, String stage, String startDate, String endDate,
        Set<String> dimension, Set<String> filter, OrganisationUnitSelectionMode ouMode, Set<String> asc,
        Set<String> desc, boolean skipMeta, boolean skipData, boolean completedOnly, boolean hierarchyMeta,
        boolean coordinatesOnly, EventStatus eventStatus, DisplayProperty displayProperty, String userOrgUnit, Integer page, Integer pageSize,
        I18nFormat format )
    {
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

        Date start = null;
        Date end = null;

        if ( startDate != null && endDate != null )
        {
            try
            {
                start = DateUtils.getMediumDate( startDate );
                end = DateUtils.getMediumDate( endDate );
            }
            catch ( RuntimeException ex )
            {
                throw new IllegalQueryException( "Start date or end date is invalid: " + startDate + " - " + endDate );
            }
        }

        if ( dimension != null )
        {
            for ( String dim : dimension )
            {
                String dimensionId = getDimensionFromParam( dim );
                List<String> items = getDimensionItemsFromParam( dim );
                DimensionalObject dimObj = dataQueryService.getDimension( dimensionId, items, null, userOrgUnits,
                    format, true, idScheme );

                if ( dimObj != null )
                {                    
                    params.addDimension( dimObj );
                }
                else
                {
                    params.addItem( getQueryItem( dim ) );
                }
            }
        }

        if ( filter != null )
        {
            for ( String dim : filter )
            {
                String dimensionId = getDimensionFromParam( dim );
                List<String> items = getDimensionItemsFromParam( dim );
                DimensionalObject dimObj = dataQueryService.getDimension( dimensionId, items, null, userOrgUnits,
                    format, true, idScheme );

                if ( dimObj != null )
                {
                    params.addFilter( dimObj );
                }
                else
                {
                    params.addItemFilter( getQueryItem( dim ) );
                }
            }
        }

        if ( asc != null )
        {
            for ( String sort : asc )
            {
                params.addAscSortItem( getSortItem( sort ) );
            }
        }

        if ( desc != null )
        {
            for ( String sort : desc )
            {
                params.addDescSortItem( getSortItem( sort ) );
            }
        }

        return params
            .withProgram( pr )
            .withProgramStage( ps )
            .withStartDate( start )
            .withEndDate( end )
            .withOrganisationUnitMode( ouMode )
            .withSkipMeta( skipMeta )
            .withSkipData( skipData )
            .withCompletedOnly( completedOnly )
            .withHierarchyMeta( hierarchyMeta )
            .withCoordinatesOnly( coordinatesOnly )
            .withEventStatus( eventStatus )
            .withDisplayProperty( displayProperty )
            .withPage( page )
            .withPageSize( pageSize ).build();
    }

    @Override
    public EventQueryParams getFromAnalyticalObject( EventAnalyticalObject object )
    {
        EventQueryParams.Builder params = new EventQueryParams.Builder();
        
        I18nFormat format = i18nManager.getI18nFormat();
        
        IdScheme idScheme = IdScheme.UID;

        if ( object != null )
        {
            Date date = object.getRelativePeriodDate();

            object.populateAnalyticalProperties();

            for ( DimensionalObject dimension : ListUtils.union( object.getColumns(), object.getRows() ) )
            {
                DimensionalObject dimObj = dataQueryService.getDimension( dimension.getDimension(),
                    getDimensionalItemIds( dimension.getItems() ), date, null, format, true, idScheme );

                if ( dimObj != null )
                {
                    params.addDimension( dimObj );
                }
                else
                {
                    params.addItem( getQueryItem( dimension.getDimension(), dimension.getFilter() ) );
                }
            }

            for ( DimensionalObject filter : object.getFilters() )
            {
                DimensionalObject dimObj = dataQueryService.getDimension( filter.getDimension(),
                    getDimensionalItemIds( filter.getItems() ), date, null, format, true, idScheme );

                if ( dimObj != null )
                {
                    params.addFilter( dimObj );
                }
                else
                {
                    params.addItemFilter( getQueryItem( filter.getDimension(), filter.getFilter() ) );
                }
            }

            params
                .withProgram( object.getProgram() )
                .withProgramStage( object.getProgramStage() )
                .withStartDate( object.getStartDate() )
                .withEndDate( object.getEndDate() )
                .withValue( object.getValue() )
                .withOutputType( object.getOutputType() );
        }

        return params.build();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private QueryItem getQueryItem( String dimension, String filter )
    {
        if ( filter != null )
        {
            dimension += DIMENSION_NAME_SEP + filter;
        }

        return getQueryItem( dimension );
    }

    private QueryItem getQueryItem( String dimensionString )
    {
        String[] split = dimensionString.split( DIMENSION_NAME_SEP );

        if ( split == null || (split.length % 2 != 1) )
        {
            throw new IllegalQueryException( "Query item or filter is invalid: " + dimensionString );
        }

        QueryItem queryItem = getQueryItemFromDimension( split[0] );

        if ( split.length > 1 ) // Filters specified
        {
            for ( int i = 1; i < split.length; i += 2 )
            {
                QueryOperator operator = QueryOperator.fromString( split[i] );
                QueryFilter filter = new QueryFilter( operator, split[i + 1] );
                queryItem.getFilters().add( filter );
            }
        }

        return queryItem;
    }

    private String getSortItem( String item )
    {
        if ( !SORTABLE_ITEMS.contains( item.toLowerCase() ) && getQueryItem( item ) == null )
        {
            throw new IllegalQueryException( "Descending sort item is invalid: " + item );
        }

        item = ITEM_EXECUTION_DATE.equalsIgnoreCase( item ) ? COL_NAME_EVENTDATE : item;

        return item;
    }

    private QueryItem getQueryItemFromDimension( String dimension )
    {
        String[] split = dimension.split( ITEM_SEP );

        String item = split[0];

        LegendSet legendSet = split.length > 1 && split[1] != null ? legendService.getLegendSet( split[1] ) : null;

        DataElement de = dataElementService.getDataElement( item );

        if ( de != null ) // TODO check if part of program
        {
            return new QueryItem( de, legendSet, de.getValueType(), de.getAggregationType(), de.getOptionSet() );
        }

        TrackedEntityAttribute at = attributeService.getTrackedEntityAttribute( item );

        if ( at != null )
        {
            return new QueryItem( at, legendSet, at.getValueType(), at.getAggregationType(), at.getOptionSet() );
        }

        ProgramIndicator pi = programIndicatorService.getProgramIndicatorByUid( item );

        if ( pi != null )
        {
            return new QueryItem( pi, legendSet, ValueType.NUMBER, pi.getAggregationType(), null );
        }

        throw new IllegalQueryException(
            "Item identifier does not reference any data element or attribute part of the program: " + item );
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
