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
package org.hisp.dhis.common;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.MULTI_CHOICES_OPTION_SEP;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.common.RequestTypeAware.EndpointAction;
import org.hisp.dhis.common.RequestTypeAware.EndpointItem;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStatus;

@Builder
@Getter
@AllArgsConstructor
public class EventDataQueryRequest
{
    private String program;

    private String stage;

    private Date startDate;

    private Date endDate;

    private Set<Set<String>> dimension;

    private Set<Set<String>> filter;

    private Set<String> headers;

    private String value;

    private AggregationType aggregationType;

    private boolean skipMeta;

    private boolean skipData;

    private boolean skipRounding;

    private boolean completedOnly;

    private boolean hierarchyMeta;

    private boolean showHierarchy;

    private SortOrder sortOrder;

    private Integer limit;

    private EventOutputType outputType;

    private Set<EventStatus> eventStatus;

    private Set<ProgramStatus> programStatus;

    private boolean collapseDataDimensions;

    private boolean aggregateData;

    private boolean includeMetadataDetails;

    private IdScheme dataIdScheme;

    private IdScheme outputIdScheme;

    private DisplayProperty displayProperty;

    private Date relativePeriodDate;

    private String userOrgUnit;

    private DhisApiVersion apiVersion;

    private OrganisationUnitSelectionMode ouMode;

    private Set<String> asc;

    private Set<String> desc;

    private String timeField;

    private String orgUnitField;

    private boolean coordinatesOnly;

    private boolean coordinateOuFallback;

    private String coordinateField;

    private String fallbackCoordinateField;

    private Integer page;

    private Integer pageSize;

    private boolean paging;

    private boolean totalPages;

    private EndpointItem endpointItem;

    private EndpointAction endpointAction;

    private boolean enhancedConditions;

    /**
     * Copies all properties of this request onto the given request.
     *
     * @param request the request to copy properties onto.
     * @return the given request with all properties of this request set.
     */
    public <T extends EventDataQueryRequest> T copyTo( T request )
    {
        EventDataQueryRequest queryRequest = request;
        queryRequest.program = this.program;
        queryRequest.stage = this.stage;
        queryRequest.startDate = this.startDate;
        queryRequest.endDate = this.endDate;
        queryRequest.dimension = new HashSet<>( this.dimension );
        queryRequest.filter = new HashSet<>( this.filter );
        queryRequest.headers = new LinkedHashSet<>( this.headers );
        queryRequest.value = this.value;
        queryRequest.aggregationType = this.aggregationType;
        queryRequest.skipMeta = this.skipMeta;
        queryRequest.skipData = this.skipData;
        queryRequest.skipRounding = this.skipRounding;
        queryRequest.completedOnly = this.completedOnly;
        queryRequest.hierarchyMeta = this.hierarchyMeta;
        queryRequest.showHierarchy = this.showHierarchy;
        queryRequest.sortOrder = this.sortOrder;
        queryRequest.limit = this.limit;
        queryRequest.outputType = this.outputType;
        queryRequest.eventStatus = new LinkedHashSet<>( this.eventStatus );
        queryRequest.programStatus = new LinkedHashSet<>( this.programStatus );
        queryRequest.collapseDataDimensions = this.collapseDataDimensions;
        queryRequest.aggregateData = this.aggregateData;
        queryRequest.includeMetadataDetails = this.includeMetadataDetails;
        queryRequest.displayProperty = this.displayProperty;
        queryRequest.relativePeriodDate = this.relativePeriodDate;
        queryRequest.userOrgUnit = this.userOrgUnit;
        queryRequest.apiVersion = this.apiVersion;
        queryRequest.ouMode = this.ouMode;
        queryRequest.asc = new HashSet<>( this.asc );
        queryRequest.desc = new HashSet<>( this.desc );
        queryRequest.timeField = this.timeField;
        queryRequest.coordinatesOnly = this.coordinatesOnly;
        queryRequest.coordinateField = this.coordinateField;
        queryRequest.fallbackCoordinateField = this.fallbackCoordinateField;
        queryRequest.page = this.page;
        queryRequest.pageSize = this.pageSize;
        queryRequest.paging = this.paging;
        queryRequest.totalPages = this.totalPages;
        queryRequest.endpointItem = this.endpointItem;
        queryRequest.endpointAction = this.endpointAction;
        queryRequest.enhancedConditions = this.enhancedConditions;
        queryRequest.outputIdScheme = outputIdScheme;
        return request;
    }

    public static ExtendedEventDataQueryRequestBuilder builder()
    {
        return new ExtendedEventDataQueryRequestBuilder();
    }

    public boolean hasStartEndDate()
    {
        return startDate != null && endDate != null;
    }

    public static class ExtendedEventDataQueryRequestBuilder extends EventDataQueryRequestBuilder
    {
        private static final String DIMENSION_OR_SEPARATOR = "_OR_";

        public EventDataQueryRequestBuilder fromCriteria( EventsAnalyticsQueryCriteria criteria )
        {
            EventDataQueryRequestBuilder builder = aggregationType( criteria.getAggregationType() )
                .aggregateData( criteria.isAggregateData() )
                .asc( criteria.getAsc() )
                .collapseDataDimensions( criteria.isCollapseDataDimensions() )
                .completedOnly( criteria.isCompletedOnly() )
                .coordinateField( criteria.getCoordinateField() )
                .fallbackCoordinateField( criteria.getFallbackCoordinateField() )
                .desc( criteria.getDesc() )
                .displayProperty( criteria.getDisplayProperty() )
                .endDate( criteria.getEndDate() )
                .eventStatus( criteria.getEventStatus() )
                .filter( getGrouped( criteria.getFilter() ) )
                .headers( criteria.getHeaders() )
                .hierarchyMeta( criteria.isHierarchyMeta() )
                .includeMetadataDetails( criteria.isIncludeMetadataDetails() )
                .limit( criteria.getLimit() )
                .ouMode( criteria.getOuMode() )
                .outputType( criteria.getOutputType() )
                .page( criteria.getPage() )
                .pageSize( criteria.getPageSize() )
                .paging( criteria.isPaging() )
                .programStatus( criteria.getProgramStatus() )
                .relativePeriodDate( criteria.getRelativePeriodDate() )
                .showHierarchy( criteria.isShowHierarchy() )
                .skipRounding( criteria.isSkipRounding() )
                .skipData( criteria.isSkipData() )
                .skipMeta( criteria.isSkipMeta() )
                .sortOrder( criteria.getSortOrder() )
                .stage( criteria.getStage() )
                .startDate( criteria.getStartDate() )
                .timeField( criteria.getTimeField() )
                .userOrgUnit( criteria.getUserOrgUnit() )
                .value( criteria.getValue() )
                .dataIdScheme( criteria.getDataIdScheme() )
                .outputIdScheme( criteria.getOutputIdScheme() )
                .orgUnitField( criteria.getOrgUnitField() )
                .coordinatesOnly( criteria.isCoordinatesOnly() )
                .coordinateOuFallback( criteria.isCoordinateOuFallback() )
                .totalPages( criteria.isTotalPages() )
                .endpointItem( criteria.getEndpointItem() )
                .endpointAction( criteria.getEndpointAction() )
                .coordinateOuFallback( criteria.isCoordinateOuFallback() )
                .enhancedConditions( criteria.isEnhancedConditions() );

            if ( criteria.getDimension() == null )
            {
                criteria.setDimension( new HashSet<>() );
            }

            Set<String> dimensions;

            if ( criteria.isQueryEndpoint() )
            {
                /*
                 * for each AnalyticsDateFilter whose event extractor is set,
                 * concatenates the timeField with the extracted value:
                 *
                 * example: eventCriteria.lastUpdated=TODAY
                 * eventCriteria.incidentDate=LAST_WEEK
                 *
                 * returns: TODAY:LAST_UPDATED;LAST_WEEK:INCIDENT_DATE
                 */
                String customDateFilters = getCustomDateFilters(
                    AnalyticsDateFilter::appliesToEvents,
                    analyticsDateFilter -> o -> analyticsDateFilter.getEventExtractor()
                        .apply( (EventsAnalyticsQueryCriteria) o ),
                    criteria );
                /*
                 * sets the new time dimensions into the requestBuilder
                 */
                dimensions = new HashSet<>( getDimensionsWithRefactoredPeDimension(
                    criteria.getDimension(),
                    customDateFilters ) );
            }
            else
            {
                dimensions = new HashSet<>( criteria.getDimension() );
            }

            return builder.dimension( getGrouped( dimensions ) );
        }

        private Set<String> splitDimension( String dimension )
        {
            return Arrays.stream( dimension.split( DIMENSION_OR_SEPARATOR ) )
                .collect( Collectors.toSet() );
        }

        private String getCustomDateFilters( Predicate<AnalyticsDateFilter> appliesTo,
            Function<AnalyticsDateFilter, Function<Object, String>> function, Object criteria )
        {
            return Arrays.stream( AnalyticsDateFilter.values() )
                .filter( appliesTo )
                .filter( analyticsDateFilter -> function.apply( analyticsDateFilter ).apply( criteria ) != null )
                .map( analyticsDateFilter -> String.join( DIMENSION_NAME_SEP,
                    handleMultiOptions(
                        function.apply( analyticsDateFilter ).apply( criteria ),
                        analyticsDateFilter.getTimeField().name() ) ) )
                .collect( Collectors.joining( OPTION_SEP ) );
        }

        private String withOptionSeparator( String options )
        {
            return joinOnOptionSeparator( splitOnMultiOptionSeparator( options ) );
        }

        private String joinOnOptionSeparator( String[] strings )
        {
            return StringUtils.join( strings, OPTION_SEP );
        }

        private String[] splitOnMultiOptionSeparator( String s )
        {
            return StringUtils.split( s, MULTI_CHOICES_OPTION_SEP );
        }

        public EventDataQueryRequestBuilder fromCriteria( EnrollmentAnalyticsQueryCriteria criteria )
        {
            EventDataQueryRequestBuilder builder = startDate( criteria.getStartDate() )
                .timeField( criteria.getTimeField() )
                .endDate( criteria.getEndDate() )
                .filter( getGrouped( criteria.getFilter() ) )
                .headers( criteria.getHeaders() )
                .ouMode( criteria.getOuMode() )
                .asc( criteria.getAsc() )
                .desc( criteria.getDesc() )
                .skipMeta( criteria.isSkipMeta() )
                .skipData( criteria.isSkipData() )
                .completedOnly( criteria.isCompletedOnly() )
                .hierarchyMeta( criteria.isHierarchyMeta() )
                .coordinatesOnly( criteria.isCoordinatesOnly() )
                .includeMetadataDetails( criteria.isIncludeMetadataDetails() )
                .dataIdScheme( criteria.getDataIdScheme() )
                .outputIdScheme( criteria.getOutputIdScheme() )
                .programStatus( criteria.getProgramStatus() )
                .page( criteria.getPage() )
                .pageSize( criteria.getPageSize() )
                .paging( criteria.isPaging() )
                .displayProperty( criteria.getDisplayProperty() )
                .relativePeriodDate( criteria.getRelativePeriodDate() )
                .userOrgUnit( criteria.getUserOrgUnit() )
                .coordinateField( criteria.getCoordinateField() )
                .sortOrder( criteria.getSortOrder() )
                .totalPages( criteria.isTotalPages() )
                .endpointItem( criteria.getEndpointItem() )
                .endpointAction( criteria.getEndpointAction() )
                .enhancedConditions( criteria.isEnhancedConditions() );

            if ( criteria.getDimension() == null )
            {
                criteria.setDimension( new HashSet<>() );
            }

            Set<String> dimensions;
            if ( criteria.isQueryEndpoint() )
            {
                /*
                 * for each AnalyticsDateFilter whose enrollment extractor is
                 * set, concatenates the timeField with the extracted value:
                 *
                 * example: enrollmentCriteria.lastUpdated=TODAY
                 * enrollmentCriteria.incidentDate=LAST_WEEK
                 *
                 * returns: TODAY:LAST_UPDATED;LAST_WEEK:INCIDENT_DATE
                 */
                String customDateFilters = getCustomDateFilters(
                    AnalyticsDateFilter::appliesToEnrollments,
                    analyticsDateFilter -> o -> analyticsDateFilter.getEnrollmentExtractor()
                        .apply( (EnrollmentAnalyticsQueryCriteria) o ),
                    criteria );

                dimensions = new HashSet<>( getDimensionsWithRefactoredPeDimension(
                    criteria.getDimension(),
                    customDateFilters ) );
            }
            else
            {
                dimensions = new HashSet<>( criteria.getDimension() );
            }

            return builder.dimension( getGrouped( dimensions ) );
        }

        private Set<Set<String>> getGrouped( Set<String> dimensions )
        {
            if ( dimensions == null )
            {
                return null;
            }
            Set<Set<String>> groupedDimensions = new HashSet<>();
            for ( String dimension : dimensions )
            {
                if ( isPeDimension( dimension ) )
                {
                    groupedDimensions.add( new HashSet<>( Set.of( dimension ) ) );
                }
                else
                {
                    groupedDimensions.add( splitDimension( dimension ) );
                }
            }

            return groupedDimensions;
        }

        private String handleMultiOptions( String values, String timeField )
        {
            return Arrays.stream( withOptionSeparator( values ).split( OPTION_SEP ) )
                .map( aValue -> String.join( DIMENSION_NAME_SEP,
                    aValue, timeField ) )
                .collect( Collectors.joining( OPTION_SEP ) );
        }

        /**
         * Given existing dimensions from controller and the time filters
         * passed, returns a new collection of dimensions with proper period
         * dimension set
         */
        private Set<String> getDimensionsWithRefactoredPeDimension( Set<String> dimensions, String customDateFilters )
        {
            if ( customDateFilters.isEmpty() )
            {
                return dimensions;
            }

            return dimensions.stream()
                .filter( ExtendedEventDataQueryRequestBuilder::isPeDimension )
                .findFirst()
                // if PE dimensions already exists, return dimensions, with
                // existing PE+customDateFilters
                .map( peDimension -> dimensionsWithRefactoredPe( dimensions, customDateFilters, peDimension ) )
                // if PE dimensions didn't exist, returns dimensions with a new
                // PE dimension,
                // represented by customDateFilters
                .orElseGet( () -> dimensionsWithNewPe( dimensions,
                    String.join( DIMENSION_NAME_SEP, PERIOD_DIM_ID, customDateFilters ) ) );

        }

        private Set<String> dimensionsWithNewPe( Set<String> dimension, String peDimension )
        {
            dimension.add( peDimension );
            return dimension;
        }

        /**
         * concatenate existing dimensions (but PE one) with a new pe dimension,
         * represented by existing PE+customDateFilters
         */
        private Set<String> dimensionsWithRefactoredPe( Set<String> dimension, String customDateFilters,
            String peDimension )
        {
            return Stream.concat(
                dimension.stream().filter( d -> !isPeDimension( d ) ),
                Stream.of( String.join( OPTION_SEP, withoutTimeField( peDimension ), customDateFilters ) ) )
                .collect( Collectors.toSet() );
        }

        /**
         * "sanitize" legacy pe dimension, in case it is passed along with time
         * field specification. Example
         * pe:TODAY:LAST_UPDATED;LAST_WEEK:INCIDENT_DATE -> pe:TODAY;LAST_WEEK
         */
        private String withoutTimeField( String dimension )
        {
            dimension = dimension.replaceFirst( PERIOD_DIM_ID + DIMENSION_NAME_SEP, "" );
            return String.join( DIMENSION_NAME_SEP, PERIOD_DIM_ID,
                Arrays.stream( dimension.split( OPTION_SEP ) )
                    .map( this::removeTimeField )
                    .collect( Collectors.joining( OPTION_SEP ) ) );
        }

        private String removeTimeField( String dimensionItem )
        {
            String[] splitDimension = dimensionItem.split( DIMENSION_NAME_SEP );
            if ( splitDimension.length > 1 )
            {
                return splitDimension[0];
            }
            else
                return dimensionItem;
        }

        private static boolean isPeDimension( String dimension )
        {
            return dimension.startsWith( PERIOD_DIM_ID + DIMENSION_NAME_SEP );
        }
    }
}
