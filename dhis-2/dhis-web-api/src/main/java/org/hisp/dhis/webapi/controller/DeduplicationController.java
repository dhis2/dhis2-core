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
import org.hisp.dhis.deduplication.MergeObject;
import org.hisp.dhis.deduplication.MergeStrategy;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
        canReadTei( tei );

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
    @ResponseStatus( value = HttpStatus.OK )
    public PotentialDuplicate postPotentialDuplicate(
        @RequestBody PotentialDuplicate potentialDuplicate )
        throws HttpStatusCodeException,
        OperationNotAllowedException,
        ConflictException,
        NotFoundException,
        BadRequestException
    {
        validatePotentialDuplicate( potentialDuplicate );
        deduplicationService.addPotentialDuplicate( potentialDuplicate );
        return potentialDuplicate;
    }

    @PutMapping( value = "/{id}" )
    @ResponseStatus( value = HttpStatus.OK )
    public void updatePotentialDuplicate( @PathVariable String id, @RequestParam( value = "status" ) String status )
        throws NotFoundException,
        HttpStatusCodeException,
        BadRequestException
    {
        checkDeduplicationStatusRequestParam( status );

        PotentialDuplicate potentialDuplicate = getPotentialDuplicateBy( id );
        DeduplicationStatus deduplicationStatus = DeduplicationStatus.valueOf( status );

        checkDbAndRequestStatus( potentialDuplicate, deduplicationStatus );

        potentialDuplicate.setStatus( deduplicationStatus );
        deduplicationService.updatePotentialDuplicate( potentialDuplicate );
    }

    @PostMapping( value = "/{id}/merge" )
    @ResponseStatus( value = HttpStatus.OK )
    public void mergePotentialDuplicate(
        @PathVariable String id,
        @RequestParam( value = "MANUAL" ) MergeStrategy mergeStrategy,
        @RequestBody MergeObject mergeObject )
        throws NotFoundException,
        ConflictException
    {

        PotentialDuplicate potentialDuplicate = deduplicationService.getPotentialDuplicateByUid( id );

        if ( potentialDuplicate == null )
        {
            throw new NotFoundException( "PotentialDuplicate with uid '" + id + "' was not found." );
        }

        if ( potentialDuplicate.getTeiA() == null || potentialDuplicate.getTeiB() == null )
        {
            throw new ConflictException( "PotentialDuplicate is missing references and cannot be merged." );
        }

        TrackedEntityInstance original = trackedEntityInstanceService
            .getTrackedEntityInstance( potentialDuplicate.getTeiA() );
        TrackedEntityInstance duplicate = trackedEntityInstanceService
            .getTrackedEntityInstance( potentialDuplicate.getTeiB() );

        if ( original == null || duplicate == null )
        {
            throw new ConflictException( "One or more Tracked Entities in the Potential Duplicate no longer exist." );
        }

        if ( original.getCreated().after( duplicate.getCreated() ) )
        {
            TrackedEntityInstance t = original;
            original = duplicate;
            duplicate = t;
        }

        if ( MergeStrategy.MANUAL.equals( mergeStrategy ) )
        {
            // TODO: manualMerge(original, duplicate, mergeObject);
        }
        else
        {
            // TODO: autoMerge(original, duplicate);
        }

    }

    private void checkDbAndRequestStatus( PotentialDuplicate potentialDuplicate,
        DeduplicationStatus deduplicationStatus )
        throws BadRequestException
    {
        if ( deduplicationStatus == DeduplicationStatus.MERGED )
            throw new BadRequestException(
                "Can't update a potential duplicate to " + DeduplicationStatus.MERGED.name() );
        if ( potentialDuplicate.getStatus() == DeduplicationStatus.MERGED )
            throw new BadRequestException( "Can't update a potential duplicate that is already "
                + DeduplicationStatus.MERGED.name() );
    }

    private void checkDeduplicationStatusRequestParam( String status )
        throws BadRequestException
    {
        if ( null == status || Arrays.stream( DeduplicationStatus.values() )
            .noneMatch( ds -> ds.name().equals( status.toUpperCase() ) ) )
        {
            throw new BadRequestException(
                "Valid deduplication status are : " + Arrays.stream( DeduplicationStatus.values() )
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
        NotFoundException,
        BadRequestException
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
        NotFoundException,
        BadRequestException
    {
        if ( tei == null )
        {
            throw new BadRequestException( "Missing required input property '" + teiFieldName + "'" );
        }

        if ( !CodeGenerator.isValidUid( tei ) )
        {
            throw new BadRequestException( "'" + tei + "' is not valid value for property '" + teiFieldName + "'" );
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
