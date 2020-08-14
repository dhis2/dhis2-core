package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_ENROLLMENT_DATE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_EVENT_DATE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_INCIDENT_DATE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_ORG_UNIT_CODE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_ORG_UNIT_NAME;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.QueryItemLocator;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.event.EventDataQueryService" )
public class DefaultEventDataQueryService
    implements EventDataQueryService
{
    private static final String COL_NAME_EVENTDATE = "executiondate";
    private static final String COL_NAME_ENROLLMENTDATE = "enrollmentdate";
    private static final String COL_NAME_INCIDENTDATE = "incidentdate";

    private static final ImmutableSet<String> SORTABLE_ITEMS = ImmutableSet.of(
        ITEM_ENROLLMENT_DATE, ITEM_INCIDENT_DATE, ITEM_EVENT_DATE, ITEM_ORG_UNIT_NAME, ITEM_ORG_UNIT_CODE );

    private final ProgramService programService;

    private final ProgramStageService programStageService;

    private final DataElementService dataElementService;

    private final QueryItemLocator queryItemLocator;

    private final TrackedEntityAttributeService attributeService;

    private final DataQueryService dataQueryService;

    private final I18nManager i18nManager;

    public DefaultEventDataQueryService( ProgramService programService, ProgramStageService programStageService,
        DataElementService dataElementService, QueryItemLocator queryItemLocator,
        TrackedEntityAttributeService attributeService, ProgramIndicatorService programIndicatorService,
        LegendSetService legendSetService, DataQueryService dataQueryService, I18nManager i18nManager )
    {
        checkNotNull( programService );
        checkNotNull( programStageService );
        checkNotNull( dataElementService );
        checkNotNull( attributeService );
        checkNotNull( programIndicatorService );
        checkNotNull( queryItemLocator );
        checkNotNull( legendSetService );
        checkNotNull( dataQueryService );
        checkNotNull( i18nManager );

        this.programService = programService;
        this.programStageService = programStageService;
        this.dataElementService = dataElementService;
        this.attributeService = attributeService;
        this.queryItemLocator = queryItemLocator;
        this.dataQueryService = dataQueryService;
        this.i18nManager = i18nManager;
    }

    @Override
    public EventQueryParams getFromRequest( EventDataQueryRequest request )
    {
        I18nFormat format = i18nManager.getI18nFormat();

        EventQueryParams.Builder params = new EventQueryParams.Builder();

        IdScheme idScheme = IdScheme.UID;

        List<OrganisationUnit> userOrgUnits = dataQueryService.getUserOrgUnits( null, request.getUserOrgUnit() );

        Program pr = programService.getProgram( request.getProgram() );

        if ( pr == null )
        {
            throwIllegalQueryEx( ErrorCode.E7129, request.getProgram() );
        }

        ProgramStage ps = programStageService.getProgramStage( request.getStage() );

        if ( StringUtils.isNotEmpty( request.getStage() ) && ps == null )
        {
            throwIllegalQueryEx( ErrorCode.E7130, request.getStage() );
        }

        if ( request.getDimension() != null )
        {
            for ( String dim : request.getDimension() )
            {
                String dimensionId = getDimensionFromParam( dim );
                List<String> items = getDimensionItemsFromParam( dim );
                DimensionalObject dimObj = dataQueryService.getDimension( dimensionId,
                    items, request.getRelativePeriodDate(), userOrgUnits, format, true, false, idScheme );

                if ( dimObj != null )
                {
                    params.addDimension( dimObj );
                }
                else
                {
                    params.addItem( getQueryItem( dim, pr, request.getOutputType() ) );
                }
            }
        }

        if ( request.getFilter() != null )
        {
            for ( String dim : request.getFilter() )
            {
                String dimensionId = getDimensionFromParam( dim );
                List<String> items = getDimensionItemsFromParam( dim );
                DimensionalObject dimObj = dataQueryService.getDimension( dimensionId,
                    items, request.getRelativePeriodDate(), userOrgUnits, format, true, false, idScheme );

                if ( dimObj != null )
                {
                    params.addFilter( dimObj );
                }
                else
                {
                    params.addItemFilter( getQueryItem( dim, pr, request.getOutputType() ) );
                }
            }
        }

        if ( request.getAsc() != null )
        {
            for ( String sort : request.getAsc() )
            {
                params.addAscSortItem( getSortItem( sort, pr, request.getOutputType() ) );
            }
        }

        if ( request.getDesc() != null )
        {
            for ( String sort : request.getDesc() )
            {
                params.addDescSortItem( getSortItem( sort, pr, request.getOutputType() ) );
            }
        }

        if ( request.getAggregationType() != null )
        {
            params.withAggregationType( AnalyticsAggregationType.fromAggregationType( request.getAggregationType() ) );
        }

        return params
            .withValue( getValueDimension( request.getValue() ) )
            .withSkipRounding( request.isSkipRounding() )
            .withShowHierarchy( request.isShowHierarchy() )
            .withSortOrder( request.getSortOrder() )
            .withLimit( request.getLimit() )
            .withOutputType( MoreObjects.firstNonNull( request.getOutputType(), EventOutputType.EVENT ) )
            .withCollapseDataDimensions( request.isCollapseDataDimensions() )
            .withAggregateData( request.isAggregateData() )
            .withProgram( pr )
            .withProgramStage( ps )
            .withStartDate( request.getStartDate() )
            .withEndDate( request.getEndDate() )
            .withOrganisationUnitMode( request.getOuMode() )
            .withSkipMeta( request.isSkipMeta() )
            .withSkipData( request.isSkipData() )
            .withCompletedOnly( request.isCompletedOnly() )
            .withHierarchyMeta( request.isHierarchyMeta() )
            .withCoordinatesOnly( request.isCoordinatesOnly() )
            .withIncludeMetadataDetails( request.isIncludeMetadataDetails() )
            .withDataIdScheme( request.getDataIdScheme() )
            .withEventStatus( request.getEventStatus() )
            .withDisplayProperty( request.getDisplayProperty() )
            .withTimeField( request.getTimeField() )
            .withOrgUnitField( request.getOrgUnitField() )
            .withCoordinateField( getCoordinateField( request.getCoordinateField() ) )
            .withPage( request.getPage() )
            .withPageSize( request.getPageSize() )
            .withProgramStatus( request.getProgramStatus() )
            .withApiVersion( request.getApiVersion() )
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
                params.addItem( getQueryItem( dimension.getDimension(), dimension.getFilter(), object.getProgram(), object.getOutputType() ) );
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
                params.addItemFilter( getQueryItem( filter.getDimension(), filter.getFilter(), object.getProgram(), object.getOutputType() ) );
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
            return "psigeometry";
        }

        if ( EventQueryParams.ENROLLMENT_COORDINATE_FIELD.equals( coordinateField ) )
        {
            return "pigeometry";
        }

        DataElement dataElement = dataElementService.getDataElement( coordinateField );

        if ( dataElement != null )
        {
            if ( ValueType.COORDINATE != dataElement.getValueType() )
            {
                throwIllegalQueryEx( ErrorCode.E7219, coordinateField );
            }

            return dataElement.getUid();
        }

        TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( coordinateField );

        if ( attribute != null )
        {
            if ( ValueType.COORDINATE != attribute.getValueType() )
            {
                throwIllegalQueryEx( ErrorCode.E7220, coordinateField );
            }

            return attribute.getUid();
        }

        throw new IllegalQueryException( new ErrorMessage( ErrorCode.E7221, coordinateField ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private QueryItem getQueryItem( String dimension, String filter, Program program, EventOutputType type )
    {
        if ( filter != null )
        {
            dimension += DIMENSION_NAME_SEP + filter;
        }

        return getQueryItem( dimension, program, type );
    }

    private QueryItem getQueryItem( String dimensionString, Program program, EventOutputType type )
    {
        String[] split = dimensionString.split( DIMENSION_NAME_SEP );

        if ( split.length % 2 != 1 )
        {
            throwIllegalQueryEx( ErrorCode.E7222, dimensionString );
        }

        QueryItem queryItem = queryItemLocator.getQueryItemFromDimension( split[0], program, type );

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

    private DimensionalItemObject getSortItem( String item, Program program, EventOutputType type )
    {
        QueryItem queryItem;

        if ( SORTABLE_ITEMS.contains( item.toLowerCase() ) )
        {
            item = ITEM_EVENT_DATE.equalsIgnoreCase( item ) ? COL_NAME_EVENTDATE : item;
            item = ITEM_ENROLLMENT_DATE.equalsIgnoreCase( item ) ? COL_NAME_ENROLLMENTDATE : item;
            item = ITEM_INCIDENT_DATE.equalsIgnoreCase( item ) ? COL_NAME_INCIDENTDATE : item;
            queryItem = new QueryItem( new BaseDimensionalItemObject( item ) );
        }
        else
        {
            queryItem = getQueryItem( item, program, type );
        }

        return queryItem.getItem();
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

        throw new IllegalQueryException( new ErrorMessage( ErrorCode.E7223, value ) );
    }
}
