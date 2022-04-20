/*
 * Copyright (c) 2004-2021, University of Oslo
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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
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

    private Set<String> dimension;

    private Set<String> filter;

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

    private EventStatus eventStatus;

    private ProgramStatus programStatus;

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
        queryRequest.eventStatus = this.eventStatus;
        queryRequest.programStatus = this.programStatus;
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
        public EventDataQueryRequestBuilder fromCriteria( EventsAnalyticsQueryCriteria criteria )
        {
            return aggregationType( criteria.getAggregationType() )
                .aggregateData( criteria.isAggregateData() )
                .asc( criteria.getAsc() )
                .collapseDataDimensions( criteria.isCollapseDataDimensions() )
                .completedOnly( criteria.isCompletedOnly() )
                .coordinateField( criteria.getCoordinateField() )
                .fallbackCoordinateField( criteria.getFallbackCoordinateField() )
                .desc( criteria.getDesc() )
                .dimension( criteria.getDimension() )
                .displayProperty( criteria.getDisplayProperty() )
                .endDate( criteria.getEndDate() )
                .eventStatus( criteria.getEventStatus() )
                .filter( criteria.getFilter() )
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
                .coordinateOuFallback( criteria.isCoordinateOuFallback() );
        }

        public EventDataQueryRequestBuilder fromCriteria( EnrollmentAnalyticsQueryCriteria criteria )
        {
            return startDate( criteria.getStartDate() )
                .endDate( criteria.getEndDate() )
                .dimension( criteria.getDimension() )
                .filter( criteria.getFilter() )
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
                .sortOrder( criteria.getSortOrder() );
        }
    }

}
