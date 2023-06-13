/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_CREATED_BY_DISPLAY_NAME;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_ENROLLMENT_DATE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_EVENT_DATE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_EVENT_STATUS;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_INCIDENT_DATE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_LAST_UPDATED;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_LAST_UPDATED_BY_DISPLAY_NAME;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_ORG_UNIT_CODE;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_ORG_UNIT_NAME;
import static org.hisp.dhis.analytics.event.EventAnalyticsService.ITEM_PROGRAM_STATUS;
import static org.hisp.dhis.analytics.event.data.DefaultEventDataQueryService.SortableItems.isSortable;
import static org.hisp.dhis.analytics.event.data.DefaultEventDataQueryService.SortableItems.translateItemIfNecessary;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
import org.hisp.dhis.common.RequestTypeAware;
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
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.base.MoreObjects;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.event.EventDataQueryService" )
public class DefaultEventDataQueryService
    implements EventDataQueryService
{
    private static final String COL_NAME_PROGRAM_STATUS_EVENTS = "pistatus";

    private static final String COL_NAME_PROGRAM_STATUS_ENROLLMENTS = "enrollmentstatus";

    private static final String COL_NAME_EVENT_STATUS = "psistatus";

    private static final String COL_NAME_EVENTDATE = "executiondate";

    private static final String COL_NAME_ENROLLMENTDATE = "enrollmentdate";

    private static final String COL_NAME_INCIDENTDATE = "incidentdate";

    private final ProgramService programService;

    private final ProgramStageService programStageService;

    private final DataElementService dataElementService;

    private final QueryItemLocator queryItemLocator;

    private final TrackedEntityAttributeService attributeService;

    private final DataQueryService dataQueryService;

    private final UserSettingService userSettingService;

    private final I18nManager i18nManager;

    public DefaultEventDataQueryService( ProgramService programService, ProgramStageService programStageService,
        DataElementService dataElementService, QueryItemLocator queryItemLocator,
        TrackedEntityAttributeService attributeService, ProgramIndicatorService programIndicatorService,
        LegendSetService legendSetService, DataQueryService dataQueryService,
        UserSettingService userSettingService, I18nManager i18nManager )
    {
        checkNotNull( programService );
        checkNotNull( programStageService );
        checkNotNull( dataElementService );
        checkNotNull( attributeService );
        checkNotNull( programIndicatorService );
        checkNotNull( queryItemLocator );
        checkNotNull( legendSetService );
        checkNotNull( dataQueryService );
        checkNotNull( userSettingService );
        checkNotNull( i18nManager );

        this.programService = programService;
        this.programStageService = programStageService;
        this.dataElementService = dataElementService;
        this.attributeService = attributeService;
        this.queryItemLocator = queryItemLocator;
        this.dataQueryService = dataQueryService;
        this.userSettingService = userSettingService;
        this.i18nManager = i18nManager;
    }

    @Override
    public EventQueryParams getFromRequest( EventDataQueryRequest request )
    {
        return getFromRequest( request, false );
    }

    @Override
    public EventQueryParams getFromRequest( EventDataQueryRequest request, boolean analyzeOnly )
    {
        I18nFormat format = i18nManager.getI18nFormat();

        EventQueryParams.Builder params = new EventQueryParams.Builder();

        IdScheme idScheme = IdScheme.UID;

        Locale locale = (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE );

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

        addDimensionsIntoParams( params, request, userOrgUnits, format, pr, idScheme );

        addFiltersIntoParams( params, request, userOrgUnits, format, pr, idScheme );

        addSortIntoParams( params, request, pr );

        if ( request.getAggregationType() != null )
        {
            params.withAggregationType( AnalyticsAggregationType.fromAggregationType( request.getAggregationType() ) );
        }

        EventQueryParams.Builder builder = params
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
            .withCoordinateOuFallback( request.isCoordinateOuFallback() )
            .withIncludeMetadataDetails( request.isIncludeMetadataDetails() )
            .withDataIdScheme( request.getDataIdScheme() )
            .withOutputIdScheme( request.getOutputIdScheme() )
            .withEventStatuses( request.getEventStatus() )
            .withDisplayProperty( request.getDisplayProperty() )
            .withTimeField( request.getTimeField() )
            .withOrgUnitField( request.getOrgUnitField() )
            .withCoordinateField( getCoordinateField( request.getCoordinateField() ) )
            .withFallbackCoordinateField( getFallbackCoordinateField( request.getFallbackCoordinateField() ) )
            .withHeaders( request.getHeaders() )
            .withPage( request.getPage() )
            .withPageSize( request.getPageSize() )
            .withPaging( request.isPaging() )
            .withTotalPages( request.isTotalPages() )
            .withProgramStatuses( request.getProgramStatus() )
            .withApiVersion( request.getApiVersion() )
            .withEndpointItem( request.getEndpointItem() )
            .withEndpointItem( request.getEndpointItem() )
            .withEndpointAction( request.getEndpointAction() )
            .withLocale( locale );

        if ( analyzeOnly )
        {
            builder = builder
                .withSkipData( true )
                .withAnalyzeOrderId();
        }

        EventQueryParams eventQueryParams = builder.build();

        // partitioning can be used only when default period is specified.
        // empty period dimension means default period.
        if ( hasPeriodDimension( eventQueryParams ) && hasNotDefaultPeriod( eventQueryParams ) )
        {
            builder.withSkipPartitioning( true );
            eventQueryParams = builder.build();
        }

        return eventQueryParams;
    }

    private boolean hasPeriodDimension( EventQueryParams eventQueryParams )
    {
        return Objects.nonNull( getPeriodDimension( eventQueryParams ) );
    }

    private boolean hasNotDefaultPeriod( EventQueryParams eventQueryParams )
    {
        return Optional.ofNullable( getPeriodDimension( eventQueryParams ) )
            .map( DimensionalObject::getItems )
            .orElse( Collections.emptyList() )
            .stream()
            .noneMatch( this::isDefaultPeriod );
    }

    private DimensionalObject getPeriodDimension( EventQueryParams eventQueryParams )
    {
        return eventQueryParams.getDimension( PERIOD_DIM_ID );
    }

    private boolean isDefaultPeriod( DimensionalItemObject dimensionalItemObject )
    {
        return ((Period) dimensionalItemObject).isDefault();
    }

    private void addSortIntoParams( EventQueryParams.Builder params, EventDataQueryRequest request, Program pr )
    {
        if ( request.getAsc() != null )
        {
            for ( String sort : request.getAsc() )
            {
                params.addAscSortItem( getSortItem( sort, pr, request.getOutputType(), request.getEndpointItem() ) );
            }
        }

        if ( request.getDesc() != null )
        {
            for ( String sort : request.getDesc() )
            {
                params.addDescSortItem( getSortItem( sort, pr, request.getOutputType(), request.getEndpointItem() ) );
            }
        }
    }

    private void addFiltersIntoParams( EventQueryParams.Builder params, EventDataQueryRequest request,
        List<OrganisationUnit> userOrgUnits,
        I18nFormat format, Program pr, IdScheme idScheme )
    {
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
    }

    private void addDimensionsIntoParams( EventQueryParams.Builder params, EventDataQueryRequest request,
        List<OrganisationUnit> userOrgUnits,
        I18nFormat format, Program pr, IdScheme idScheme )
    {
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

        Locale locale = (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE );

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
                params.addItem( getQueryItem( dimension.getDimension(), dimension.getFilter(), object.getProgram(),
                    object.getOutputType() ) );
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
                params.addItemFilter( getQueryItem( filter.getDimension(), filter.getFilter(), object.getProgram(),
                    object.getOutputType() ) );
            }
        }

        return params
            .withProgram( object.getProgram() )
            .withProgramStage( object.getProgramStage() )
            .withStartDate( object.getStartDate() )
            .withEndDate( object.getEndDate() )
            .withValue( object.getValue() )
            .withOutputType( object.getOutputType() )
            .withLocale( locale )
            .build();
    }

    @Override
    public String getCoordinateField( String coordinateField )
    {
        return getCoordinateField( coordinateField, "psigeometry" );
    }

    @Override
    public String getFallbackCoordinateField( String fallbackCoordinateField )
    {
        return fallbackCoordinateField == null ? "ougeometry" : fallbackCoordinateField;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String getCoordinateField( String coordinateField, String defaultEventCoordinateField )
    {
        if ( coordinateField == null || EventQueryParams.EVENT_COORDINATE_FIELD.equals( coordinateField ) )
        {
            return defaultEventCoordinateField;
        }

        if ( EventQueryParams.ENROLLMENT_COORDINATE_FIELD.equals( coordinateField ) )
        {
            return "pigeometry";
        }

        DataElement dataElement = dataElementService.getDataElement( coordinateField );

        if ( dataElement != null )
        {
            return getCoordinateFieldOrFail( dataElement.getValueType(), coordinateField, ErrorCode.E7219 );
        }

        TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( coordinateField );

        if ( attribute != null )
        {
            return getCoordinateFieldOrFail( attribute.getValueType(), coordinateField, ErrorCode.E7220 );
        }

        throw new IllegalQueryException( new ErrorMessage( ErrorCode.E7221, coordinateField ) );
    }

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
                // FE uses HH.MM time format instead of HH:MM. This is not
                // compatible with db table/cell values
                modifyFilterWhenTimeQueryItem( queryItem, filter );
                queryItem.addFilter( filter );
            }
        }

        return queryItem;
    }

    private static void modifyFilterWhenTimeQueryItem( QueryItem queryItem, QueryFilter filter )
    {
        if ( queryItem.getItem() instanceof DataElement
            && ((DataElement) queryItem.getItem()).getValueType() == ValueType.TIME )
        {
            filter.setFilter( filter.getFilter().replace( ".", ":" ) );
        }

    }

    private QueryItem getSortItem( String item, Program program, EventOutputType type,
        RequestTypeAware.EndpointItem endpointItem )
    {
        if ( isSortable( item ) )
        {
            return new QueryItem( new BaseDimensionalItemObject( translateItemIfNecessary( item, endpointItem ) ) );
        }
        return getQueryItem( item, program, type );
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

    private String getCoordinateFieldOrFail( ValueType valueType, String field, ErrorCode errorCode )
    {
        if ( ValueType.COORDINATE != valueType && ValueType.ORGANISATION_UNIT != valueType )
        {
            throwIllegalQueryEx( errorCode, field );
        }
        return field;
    }

    @Getter
    @RequiredArgsConstructor
    enum SortableItems
    {
        ENROLLMENT_DATE( ITEM_ENROLLMENT_DATE, COL_NAME_ENROLLMENTDATE ),
        INCIDENT_DATE( ITEM_INCIDENT_DATE, COL_NAME_INCIDENTDATE ),
        EVENT_DATE( ITEM_EVENT_DATE, COL_NAME_EVENTDATE ),
        ORG_UNIT_NAME( ITEM_ORG_UNIT_NAME ),
        ORG_UNIT_CODE( ITEM_ORG_UNIT_CODE ),
        PROGRAM_STATUS( ITEM_PROGRAM_STATUS, COL_NAME_PROGRAM_STATUS_EVENTS, COL_NAME_PROGRAM_STATUS_ENROLLMENTS ),
        EVENT_STATUS( ITEM_EVENT_STATUS, COL_NAME_EVENT_STATUS ),
        CREATED_BY_DISPLAY_NAME( ITEM_CREATED_BY_DISPLAY_NAME ),
        LAST_UPDATED_BY_DISPLAY_NAME( ITEM_LAST_UPDATED_BY_DISPLAY_NAME ),
        LAST_UPDATED( ITEM_LAST_UPDATED );

        private final String itemName;

        private final String eventColumnName;

        private final String enrollmentColumnName;

        SortableItems( String itemName )
        {
            this.itemName = itemName;
            this.eventColumnName = null;
            this.enrollmentColumnName = null;
        }

        SortableItems( String itemName, String columnName )
        {
            this.itemName = itemName;
            this.eventColumnName = columnName;
            this.enrollmentColumnName = columnName;
        }

        static boolean isSortable( String itemName )
        {
            return Arrays.stream( values() )
                .map( SortableItems::getItemName )
                .anyMatch( itemName::equals );
        }

        static String translateItemIfNecessary( String item, RequestTypeAware.EndpointItem type )
        {
            return Arrays.stream( values() )
                .filter( sortableItems -> sortableItems.getItemName().equals( item ) )
                .findFirst()
                .map( sortableItems -> sortableItems.getColumnName( type ) )
                .orElse( item );
        }

        private String getColumnName( RequestTypeAware.EndpointItem type )
        {
            return type == RequestTypeAware.EndpointItem.EVENT ? eventColumnName : enrollmentColumnName;
        }

    }
}
