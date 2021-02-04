package org.hisp.dhis.webapi.controller;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.deduplication.DeduplicationService;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.deduplication.PotentialDuplicateQuery;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.*;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

@RestController
@RequestMapping( value = "/potentialDuplicates" )
@ApiVersion( include = { DhisApiVersion.ALL, DhisApiVersion.DEFAULT } )
public class DeduplicationController
{

    private final DeduplicationService deduplicationService;

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    private final TrackerAccessManager trackerAccessManager;

    private final CurrentUserService currentUserService;

    private final FieldFilterService fieldFilterService;

    private final ContextService contextService;

    public DeduplicationController( DeduplicationService deduplicationService,
        TrackedEntityInstanceService trackedEntityInstanceService,
        TrackerAccessManager trackerAccessManager, CurrentUserService currentUserService,
        FieldFilterService fieldFilterService,
        ContextService contextService )
    {
        this.deduplicationService = deduplicationService;
        this.trackedEntityInstanceService = trackedEntityInstanceService;
        this.trackerAccessManager = trackerAccessManager;
        this.currentUserService = currentUserService;
        this.fieldFilterService = fieldFilterService;
        this.contextService = contextService;
    }

    @GetMapping
    public Node getAll(
        PotentialDuplicateQuery query,
        HttpServletResponse response
    )
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        List<PotentialDuplicate> potentialDuplicates = deduplicationService.getAllPotentialDuplicates( query );

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

    @GetMapping( value = "/{id}" )
    public PotentialDuplicate getPotentialDuplicate(
        @PathVariable String id
    )
        throws WebMessageException
    {
        PotentialDuplicate potentialDuplicate = deduplicationService.getPotentialDuplicateByUid( id );

        if ( potentialDuplicate == null )
        {
            throw new WebMessageException( notFound( "No potentialDuplicate records found with id '" + id + "'." ) );
        }

        return potentialDuplicate;
    }

    @PostMapping
    public PotentialDuplicate postPotentialDuplicate(
        @RequestBody PotentialDuplicate potentialDuplicate
    )
        throws WebMessageException
    {

        validatePotentialDuplicate( potentialDuplicate );

        deduplicationService.addPotentialDuplicate( potentialDuplicate);
        return potentialDuplicate;
    }

    @RequestMapping( method = { RequestMethod.PUT, RequestMethod.POST }, value = "/{id}/invalidation" )
    public void markPotentialDuplicateInvalid(
        @PathVariable String id
    )
        throws WebMessageException
    {
        PotentialDuplicate potentialDuplicate = deduplicationService.getPotentialDuplicateByUid( id );

        if ( potentialDuplicate == null )
        {
            throw new WebMessageException(
                notFound( "No potentialDuplicate records found with id '" + id + "'." ) );
        }

        deduplicationService.markPotentialDuplicateInvalid( potentialDuplicate );
    }

    @DeleteMapping( value = "/{id}" )
    public void deletePotentialDuplicate(
        @PathVariable String id
    )
        throws WebMessageException
    {
        PotentialDuplicate potentialDuplicate = deduplicationService.getPotentialDuplicateByUid( id );

        if ( potentialDuplicate == null )
        {
            throw new WebMessageException(
                notFound( "No potentialDuplicate records found with id '" + id + "'." ) );
        }

        deduplicationService.deletePotentialDuplicate( potentialDuplicate );

    }


    private void validatePotentialDuplicate( PotentialDuplicate potentialDuplicate )
        throws WebMessageException
    {

        // Validate that teiA is present and a valid uid of an existing TEI
        if ( potentialDuplicate.getTeiA() == null )
        {
            throw new WebMessageException( conflict( "Missing required property 'teiA'" ) );
        }

        if ( !CodeGenerator.isValidUid( potentialDuplicate.getTeiA() ) )
        {
            throw new WebMessageException(
                conflict( "'" + potentialDuplicate.getTeiA() + "' is not valid value for property 'teiA'" ) );
        }

        TrackedEntityInstance teiA = trackedEntityInstanceService
            .getTrackedEntityInstance( potentialDuplicate.getTeiA() );

        if ( teiA == null )
        {
            throw new WebMessageException(
                conflict( "No tracked entity instance found with id '" + potentialDuplicate.getTeiA() + "'." ) );
        }

        if ( !trackerAccessManager.canRead( currentUserService.getCurrentUser(), teiA ).isEmpty() )
        {
            throw new WebMessageException(
                forbidden( "You don't have read access to '" + potentialDuplicate.getTeiA() + "'." ) );
        }

        // Validate that teiB is a valid uid of an existing TEI if present
        if ( potentialDuplicate.getTeiB() != null )
        {
            if ( !CodeGenerator.isValidUid( potentialDuplicate.getTeiB() ) )
            {
                throw new WebMessageException(
                    conflict( "'" + potentialDuplicate.getTeiB() + "' is not valid value for property 'teiB'" ) );
            }

            TrackedEntityInstance teiB = trackedEntityInstanceService
                .getTrackedEntityInstance( potentialDuplicate.getTeiB() );

            if ( teiB == null )
            {
                throw new WebMessageException(
                    notFound( "No tracked entity instance found with id '" + potentialDuplicate.getTeiB() + "'." ) );
            }

            if ( !trackerAccessManager.canRead( currentUserService.getCurrentUser(), teiB ).isEmpty() )
            {
                throw new WebMessageException(
                    forbidden( "You don't have read access to '" + potentialDuplicate.getTeiB() + "'." ) );
            }
        }

        if ( deduplicationService.exists( potentialDuplicate ) )
        {
            {
                throw new WebMessageException(
                    conflict( "'" + potentialDuplicate.getTeiA() + "' " +
                        (potentialDuplicate.getTeiB() != null ? "and '" + potentialDuplicate.getTeiB() + "' " : "") +
                        "is already marked as a potential duplicate" ) );
            }
        }
    }
}
