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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.webapi.controller.tracker.TrackerControllerSupport.RESOURCE_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.enrollment.Enrollments;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.webapi.controller.event.mapper.EnrollmentCriteriaMapper;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.controller.event.webrequest.tracker.TrackerEnrollmentCriteria;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.controller.tracker.export.support.EnrollmentsSupportService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping( value = RESOURCE_PATH + "/" + TrackerEnrollmentsExportController.ENROLLMENTS )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class TrackerEnrollmentsExportController
{
    protected static final String ENROLLMENTS = "enrollments";

    private static final String DEFAULT_FIELDS_PARAM = "*,!relationships,!events,!attributes";

    private static final EnrollmentMapper ENROLLMENT_MAPPER = Mappers.getMapper( EnrollmentMapper.class );

    @NonNull
    private final EnrollmentCriteriaMapper enrollmentCriteriaMapper;

    @NonNull
    private final EnrollmentService enrollmentService;

    @NonNull
    private final FieldFilterService fieldFilterService;

    @NonNull
    private final EnrollmentsSupportService enrollmentsSupportService;

    @GetMapping( produces = APPLICATION_JSON_VALUE )
    PagingWrapper<ObjectNode> getInstances(
        TrackerEnrollmentCriteria trackerEnrollmentCriteria,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<String> fields )
    {
        PagingWrapper<ObjectNode> pagingWrapper = new PagingWrapper<>();

        List<org.hisp.dhis.dxf2.events.enrollment.Enrollment> enrollmentList;

        if ( trackerEnrollmentCriteria.getEnrollment() == null )
        {
            ProgramInstanceQueryParams params = enrollmentCriteriaMapper.getFromUrl( trackerEnrollmentCriteria );

            Enrollments enrollments = enrollmentService.getEnrollments( params );

            if ( trackerEnrollmentCriteria.isPagingRequest() )
            {
                pagingWrapper = pagingWrapper.withPager(
                    PagingWrapper.Pager.fromLegacy( trackerEnrollmentCriteria, enrollments.getPager() ) );
            }

            enrollmentList = enrollments.getEnrollments();

        }
        else
        {
            Set<String> enrollmentIds = TextUtils.splitToSet( trackerEnrollmentCriteria.getEnrollment(),
                TextUtils.SEMICOLON );
            enrollmentList = enrollmentIds != null
                ? enrollmentIds.stream()
                    .map( uid -> enrollmentService.getEnrollment( uid,
                        enrollmentsSupportService.getTrackedEntityInstanceParams( fields ) ) )
                    .collect( Collectors.toList() )
                : Collections.emptyList();
        }

        List<ObjectNode> objectNodes = fieldFilterService
            .toObjectNodes( ENROLLMENT_MAPPER.fromCollection( enrollmentList ), fields );
        return pagingWrapper.withInstances( objectNodes );
    }

    @GetMapping( value = "{id}" )
    public ResponseEntity<ObjectNode> getEnrollmentById(
        @PathVariable String id,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<String> fields )
        throws NotFoundException
    {
        return Optional.ofNullable( enrollmentService.getEnrollment( id,
            enrollmentsSupportService.getTrackedEntityInstanceParams( fields ) ) )
            .map( e -> ResponseEntity.ok( fieldFilterService.toObjectNode( ENROLLMENT_MAPPER.from( e ), fields ) ) )
            .orElseThrow( () -> new NotFoundException( "Enrollment", id ) );
    }
}
