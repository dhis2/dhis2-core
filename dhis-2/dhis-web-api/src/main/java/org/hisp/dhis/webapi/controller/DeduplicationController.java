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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.deduplication.DeduplicationService;
import org.hisp.dhis.deduplication.DeduplicationStatus;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.deduplication.PotentialDuplicateQuery;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.controller.exception.BadRequestException;
import org.hisp.dhis.webapi.controller.exception.ConflictException;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.controller.exception.OperationNotAllowedException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;

import com.google.common.collect.Lists;

@RestController
@RequestMapping( value = "/potentialDuplicates" )
@ApiVersion( include = { DhisApiVersion.ALL, DhisApiVersion.DEFAULT } )
@RequiredArgsConstructor
public class DeduplicationController
{
    private final DeduplicationService deduplicationService;

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    private final TrackerAccessManager trackerAccessManager;

    private final CurrentUserService currentUserService;

    private final FieldFilterService fieldFilterService;

    private final ContextService contextService;

    @GetMapping
    public Node getAllByQuery(
        PotentialDuplicateQuery query,
        HttpServletResponse response )
        throws BadRequestException
    {
        checkDeduplicationStatusRequestParam(
            Optional.ofNullable( query.getStatus() ).map( Enum::name ).orElse( null ) );

        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        List<PotentialDuplicate> potentialDuplicates = deduplicationService.getAllPotentialDuplicatesBy( query );

        RootNode rootNode = NodeUtils.createMetadata();

        if ( !query.isSkipPaging() )
        {
            query.setTotal( deduplicationService.countPotentialDuplicates( query ) );
            rootNode.addChild( NodeUtils.createPager( query.getPager() ) );
        }

        rootNode.addChild( fieldFilterService
            .toCollectionNode( PotentialDuplicate.class, new FieldFilterParams( potentialDuplicates, fields ) ) );

        setNoStore( response );
        return rootNode;
    }

    @GetMapping( value = "/tei/{tei}" )
    public List<PotentialDuplicate> getPotentialDuplicateByTei( @PathVariable String tei,
        @RequestParam( value = "status", defaultValue = "ALL", required = false ) String status )
        throws NotFoundException,
        OperationNotAllowedException,
        BadRequestException,
        HttpStatusCodeException
    {
        checkDeduplicationStatusRequestParam( status );

        List<PotentialDuplicate> potentialDuplicateList = deduplicationService.getPotentialDuplicateByTei( tei,
            DeduplicationStatus.valueOf( status ) );

        for ( PotentialDuplicate potentialDuplicate : potentialDuplicateList )
        {
            canReadTei( potentialDuplicate.getTeiA() );
            canReadTei( potentialDuplicate.getTeiB() );
        }

        return potentialDuplicateList;
    }

    @GetMapping( value = "/{id}" )
    public PotentialDuplicate getPotentialDuplicateById(
        @PathVariable String id )
        throws NotFoundException,
        HttpStatusCodeException
    {
        return getPotentialDuplicateBy( id );
    }

    @PostMapping
    public PotentialDuplicate postPotentialDuplicate(
        @RequestBody PotentialDuplicate potentialDuplicate )
        throws HttpStatusCodeException,
        OperationNotAllowedException,
        ConflictException,
        NotFoundException
    {
        validatePotentialDuplicate( potentialDuplicate );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        return potentialDuplicate;
    }

    @RequestMapping( method = { RequestMethod.PUT, RequestMethod.POST }, value = "/{id}/invalidation" )
    public void markPotentialDuplicateInvalid(
        @PathVariable String id )
        throws NotFoundException,
        HttpStatusCodeException
    {
        PotentialDuplicate potentialDuplicate = getPotentialDuplicateBy( id );

        potentialDuplicate.setStatus( DeduplicationStatus.INVALID );
        deduplicationService.updatePotentialDuplicate( potentialDuplicate );
    }

    @DeleteMapping( value = "/{id}" )
    public void deletePotentialDuplicate(
        @PathVariable String id )
        throws NotFoundException,
        HttpStatusCodeException
    {
        PotentialDuplicate potentialDuplicate = getPotentialDuplicateBy( id );
        deduplicationService.deletePotentialDuplicate( potentialDuplicate );
    }

    private void checkDeduplicationStatusRequestParam( String status )
        throws BadRequestException
    {
        if ( null == status || Arrays.stream( DeduplicationStatus.values() )
            .noneMatch( ds -> ds.name().equals( status.toUpperCase() ) ) )
        {
            throw new BadRequestException(
                "Bad Request. Valid deduplication status are : " + Arrays.stream( DeduplicationStatus.values() )
                    .map( Object::toString ).collect( Collectors.joining( "," ) ) );
        }
    }

    private PotentialDuplicate getPotentialDuplicateBy( String id )
        throws NotFoundException
    {
        return Optional.ofNullable( deduplicationService.getPotentialDuplicateByUid( id ) ).orElseThrow(
            () -> new NotFoundException( "No potentialDuplicate records found with id '" + id + "'." ) );
    }

    private void validatePotentialDuplicate( PotentialDuplicate potentialDuplicate )
        throws OperationNotAllowedException,
        ConflictException,
        NotFoundException
    {
        checkValidAndCanReadTei( potentialDuplicate.getTeiA(), "teiA" );

        checkValidAndCanReadTei( potentialDuplicate.getTeiB(), "teiB" );

        checkAlreadyExistingDuplicate( potentialDuplicate );
    }

    private void checkAlreadyExistingDuplicate( PotentialDuplicate potentialDuplicate )
        throws ConflictException
    {
        if ( deduplicationService.exists( potentialDuplicate ) )
        {
            throw new ConflictException( "'" + potentialDuplicate.getTeiA() + "' " + "and '"
                + potentialDuplicate.getTeiB() + " is already marked as a potential duplicate" );
        }
    }

    private void checkValidAndCanReadTei( String tei, String teiFieldName )
        throws OperationNotAllowedException,
        ConflictException,
        NotFoundException
    {
        if ( tei == null )
        {
            throw new ConflictException( "Missing required input property '" + teiFieldName + "'" );
        }

        if ( !CodeGenerator.isValidUid( tei ) )
        {
            throw new ConflictException( "'" + tei + "' is not valid value for property '" + teiFieldName + "'" );
        }

        canReadTei( tei );
    }

    private void canReadTei( String tei )
        throws OperationNotAllowedException,
        NotFoundException
    {
        TrackedEntityInstance trackedEntityInstance = Optional.ofNullable( trackedEntityInstanceService
            .getTrackedEntityInstance( tei ) )
            .orElseThrow( () -> new NotFoundException( "No tracked entity instance found with id '" + tei + "'." ) );

        if ( !trackerAccessManager.canRead( currentUserService.getCurrentUser(), trackedEntityInstance ).isEmpty() )
        {
            throw new OperationNotAllowedException( "You don't have read access to '" + tei + "'." );
        }
    }
}
