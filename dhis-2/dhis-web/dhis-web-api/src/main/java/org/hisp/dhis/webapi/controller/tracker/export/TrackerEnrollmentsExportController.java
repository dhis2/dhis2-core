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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.webapi.controller.tracker.TrackerControllerSupport.RESOURCE_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.enrollment.Enrollments;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.mapper.EnrollmentMapper;
import org.hisp.dhis.tracker.domain.web.PagingWrapper;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( value = RESOURCE_PATH + "/" + TrackerEnrollmentsExportController.ENROLLMENTS )
@RequiredArgsConstructor
public class TrackerEnrollmentsExportController
{
    protected final static String ENROLLMENTS = "enrollments";

    private final ProgramInstanceService programInstanceService;

    private final EnrollmentService enrollmentService;

    private static final EnrollmentMapper ENROLLMENT_MAPPER = Mappers.getMapper( EnrollmentMapper.class );

    @GetMapping( produces = APPLICATION_JSON_VALUE )
    PagingWrapper<Enrollment> getInstances(
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String lastUpdatedDuration,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) String trackedEntityType,
        @RequestParam( required = false ) String trackedEntityInstance,
        @RequestParam( required = false ) String enrollment,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false, defaultValue = "false" ) boolean includeDeleted )
    {
        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );

        skipPaging = PagerUtils.isSkipPaging( skipPaging, paging );

        PagingWrapper<Enrollment> enrollmentPagingWrapper = new PagingWrapper<>();

        List<org.hisp.dhis.dxf2.events.enrollment.Enrollment> enrollmentList;

        if ( enrollment == null )
        {
            ProgramInstanceQueryParams params = programInstanceService.getFromUrl( orgUnits, ouMode, lastUpdated,
                lastUpdatedDuration, program, programStatus, programStartDate, programEndDate, trackedEntityType,
                trackedEntityInstance, followUp, page, pageSize, totalPages, skipPaging, includeDeleted );

            Enrollments enrollments = enrollmentService.getEnrollments( params );

            if ( enrollments.getPager() != null )
            {
                enrollmentPagingWrapper.withPager( enrollments.getPager() );
            }

            enrollmentList = enrollments.getEnrollments();

        }
        else
        {
            Set<String> enrollmentIds = TextUtils.splitToArray( enrollment, TextUtils.SEMICOLON );
            enrollmentList = enrollmentIds != null
                ? enrollmentIds.stream().map( enrollmentService::getEnrollment ).collect( Collectors.toList() )
                : Collections.emptyList();
        }

        return enrollmentPagingWrapper.withInstances( ENROLLMENT_MAPPER.fromCollection( enrollmentList ) );

    }

    @GetMapping( value = "{id}" )
    public Enrollment getTrackedEntityInstanceById( @PathVariable String id )
        throws NotFoundException
    {

        return Optional.ofNullable( enrollmentService.getEnrollment( id ) )
            .map( ENROLLMENT_MAPPER::from )
            .orElseThrow( () -> new NotFoundException( "Enrollment", id ) );
    }

}
