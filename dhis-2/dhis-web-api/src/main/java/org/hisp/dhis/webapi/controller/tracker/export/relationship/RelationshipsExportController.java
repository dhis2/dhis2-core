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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.hisp.dhis.webapi.controller.tracker.ControllerSupport.RESOURCE_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

@OpenApi.Tags( "tracker" )
@RestController
@RequestMapping( produces = APPLICATION_JSON_VALUE, value = RESOURCE_PATH + "/"
    + RelationshipsExportController.RELATIONSHIPS )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class RelationshipsExportController
{
    protected static final String RELATIONSHIPS = "relationships";

    private static final String DEFAULT_FIELDS_PARAM = "relationship,relationshipType,from[trackedEntity[trackedEntity],enrollment[enrollment],event[event]],to[trackedEntity[trackedEntity],enrollment[enrollment],event[event]]";

    private static final RelationshipMapper RELATIONSHIP_MAPPER = Mappers.getMapper( RelationshipMapper.class );

    @Nonnull
    private final TrackedEntityService trackedEntityService;

    @Nonnull
    private final EnrollmentService enrollmentService;

    @Nonnull
    private final EventService eventService;

    @Nonnull
    private final RelationshipService relationshipService;

    @Nonnull
    private final FieldFilterService fieldFilterService;

    private Map<Class<?>, Function<String, ?>> objectRetrievers;

    private Map<Class<?>, CheckedBiFunction<Object, PagingAndSortingCriteriaAdapter, List<Relationship>>> relationshipRetrievers;

    interface CheckedBiFunction<T, U, R>
    {
        R apply( T t, U u )
            throws ForbiddenException,
            NotFoundException;
    }

    @PostConstruct
    void setupMaps()
    {
        objectRetrievers = ImmutableMap.<Class<?>, Function<String, ?>> builder()
            .put( TrackedEntity.class, trackedEntityService::getTrackedEntityInstance )
            .put( Enrollment.class, enrollmentService::getEnrollment )
            .put( Event.class, eventService::getEvent )
            .build();

        relationshipRetrievers = ImmutableMap
            .<Class<?>, CheckedBiFunction<Object, PagingAndSortingCriteriaAdapter, List<Relationship>>> builder()
            .put( TrackedEntity.class, getRelationshipsByTrackedEntity() )
            .put( Enrollment.class, getRelationshipsByEnrollment() )
            .put( Event.class, getRelationshipsByEvent() )
            .build();
    }

    private CheckedBiFunction<Object, PagingAndSortingCriteriaAdapter, List<Relationship>> getRelationshipsByTrackedEntity()
    {
        return ( o, criteria ) -> relationshipService
            .getRelationshipsByTrackedEntityInstance( (TrackedEntity) o, criteria );
    }

    private CheckedBiFunction<Object, PagingAndSortingCriteriaAdapter, List<Relationship>> getRelationshipsByEnrollment()
    {
        return ( o, criteria ) -> relationshipService.getRelationshipsByEnrollment( (Enrollment) o,
            criteria );
    }

    private CheckedBiFunction<Object, PagingAndSortingCriteriaAdapter, List<Relationship>> getRelationshipsByEvent()
    {
        return ( o, criteria ) -> relationshipService.getRelationshipsByEvent( (Event) o,
            criteria );
    }

    @GetMapping
    PagingWrapper<ObjectNode> getInstances(
        RelationshipCriteria criteria,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<FieldPath> fields )
        throws NotFoundException,
        BadRequestException,
        ForbiddenException
    {
        List<org.hisp.dhis.webapi.controller.tracker.view.Relationship> relationships = tryGetRelationshipFrom(
            criteria.getIdentifierClass(), criteria.getIdentifierParam(), criteria.getIdentifierName(), criteria );

        PagingWrapper<ObjectNode> pagingWrapper = new PagingWrapper<>();
        if ( criteria.isPagingRequest() )
        {
            pagingWrapper = pagingWrapper.withPager(
                PagingWrapper.Pager.builder()
                    .page( criteria.getPage() )
                    .pageSize( criteria.getPageSize() )
                    .build() );
        }

        List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes( relationships, fields );
        return pagingWrapper.withInstances( objectNodes );
    }

    @GetMapping( "{id}" )
    public ResponseEntity<ObjectNode> getRelationship(
        @PathVariable String id,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<FieldPath> fields )
        throws NotFoundException,
        ForbiddenException
    {
        org.hisp.dhis.webapi.controller.tracker.view.Relationship relationship = RELATIONSHIP_MAPPER
            .from( relationshipService.findRelationshipByUid( id ).orElse( null ) );

        if ( relationship == null )
        {
            throw new NotFoundException( Relationship.class, id );
        }

        return ResponseEntity.ok( fieldFilterService.toObjectNode( relationship, fields ) );
    }

    private List<org.hisp.dhis.webapi.controller.tracker.view.Relationship> tryGetRelationshipFrom(
        Class<?> type, String identifier, String identifierName,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteria )
        throws NotFoundException,
        ForbiddenException
    {
        Object object = getObjectRetriever( type ).apply( identifier );
        if ( object == null )
        {
            throw new NotFoundException( identifierName + " with id " + identifier + " could not be found." );
        }

        return RELATIONSHIP_MAPPER
            .fromCollection( getRelationshipRetriever( type ).apply( object, pagingAndSortingCriteria ) );
    }

    private Function<String, ?> getObjectRetriever( Class<?> type )
    {
        return Optional.ofNullable( type )
            .map( objectRetrievers::get )
            .orElseThrow( () -> new IllegalArgumentException( "Unable to detect object retriever from " + type ) );
    }

    private CheckedBiFunction<Object, PagingAndSortingCriteriaAdapter, List<Relationship>> getRelationshipRetriever(
        Class<?> type )
    {
        return Optional.ofNullable( type )
            .map( relationshipRetrievers::get )
            .orElseThrow(
                () -> new IllegalArgumentException( "Unable to detect relationship retriever from " + type ) );
    }
}
