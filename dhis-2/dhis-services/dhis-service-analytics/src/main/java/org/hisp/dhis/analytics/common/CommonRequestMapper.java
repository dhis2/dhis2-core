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
package org.hisp.dhis.analytics.common;

import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamType;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.common.AnalyticsPagingCriteria;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

@Component
@RequiredArgsConstructor
public class CommonRequestMapper
{

    private final I18nManager i18nManager;

    private final DataQueryService dataQueryService;

    private final EventDataQueryService eventDataQueryService;

    private final ProgramService programService;

    public CommonParams map( CommonQueryRequest request, AnalyticsPagingCriteria pagingCriteria,
        DhisApiVersion apiVersion )
    {

        List<OrganisationUnit> userOrgUnits = dataQueryService.getUserOrgUnits( null, request.getUserOrgUnit() );

        Collection<Program> programs = getPrograms( request );

        return CommonParams.builder()
            .programs( programs )
            .pagingAndSortingParams( AnalyticsPagingAndSortingParams.builder()
                .countRequested( pagingCriteria.isTotalPages() )
                .requestPaged( pagingCriteria.isPaging() )
                .page( pagingCriteria.getPage() )
                .pageSize( pagingCriteria.getPageSize() )
                // not mapping EndpointItem and RequestType -- not needed at the
                // moment
                .build() )
            .dimensionParams( retrieveDimensionParams( request, programs, userOrgUnits ) )
            .build();
    }

    private Collection<Program> getPrograms( CommonQueryRequest queryRequest )
    {
        Collection<Program> programs = programService.getPrograms( queryRequest.getProgram() );

        if ( programs.size() != queryRequest.getProgram().size() )
        {
            Collection<String> foundProgramUids = programs.stream()
                .map( Program::getUid )
                .collect( Collectors.toList() );

            Collection<String> missingProgramUids = Optional.of( queryRequest )
                .map( CommonQueryRequest::getProgram )
                .orElse( Collections.emptyList() ).stream()
                .filter( uidFromRequest -> !foundProgramUids.contains( uidFromRequest ) )
                .collect( Collectors.toList() );

            throw new IllegalArgumentException( "The following programs couldn't be found: " + missingProgramUids );
        }
        return programs;
    }

    private List<DimensionParam> retrieveDimensionParams( CommonQueryRequest request,
        Collection<Program> programs, List<OrganisationUnit> userOrgUnits )
    {
        List<DimensionParam> dimensionParams = new ArrayList<>();
        for ( DimensionParamType dimensionParamType : DimensionParamType.values() )
        {
            Collection<String> dimensionsOrFilter = dimensionParamType.getUidsGetter().apply( request );
            dimensionParams.addAll(
                dimensionsOrFilter.stream()
                    .map( dof -> toDimensionParams( dof, dimensionParamType, request, programs, userOrgUnits ) )
                    .flatMap( Collection::stream )
                    .collect( Collectors.toList() ) );
        }
        return ImmutableList.copyOf( dimensionParams );
    }

    private Collection<DimensionParam> toDimensionParams( String uid, DimensionParamType dimensionParamType,
        CommonQueryRequest request,
        Collection<Program> programs, List<OrganisationUnit> userOrgUnits )
    {
        String dimensionId = getDimensionFromParam( uid );
        List<String> items = getDimensionItemsFromParam( uid );

        DimensionalObject dimensionalObject = dataQueryService.getDimension( dimensionId,
            items,
            request.getRelativePeriodDate(),
            userOrgUnits,
            i18nManager.getI18nFormat(), true,
            IdScheme.UID );

        if ( dimensionalObject != null )
        {
            return Collections.singleton( DimensionParam.ofObject( dimensionalObject, dimensionParamType, items ) );
        }
        else
        {
            // We're currently searching the queryItem inside all passed
            // programs, but we will NEED to
            // change this when we can properly parse inputs (dimension/filter)
            // as
            // {programId}.{stageId}.{DE|PI|PA uid}
            return programs.stream()
                .map( program -> eventDataQueryService.getQueryItem( dimensionId, program,
                    EventOutputType.TRACKED_ENTITY_INSTANCE ) )
                .map( queryItem -> DimensionParam.ofObject( queryItem, dimensionParamType, items ) )
                .collect( Collectors.toList() );
        }
    }
}
