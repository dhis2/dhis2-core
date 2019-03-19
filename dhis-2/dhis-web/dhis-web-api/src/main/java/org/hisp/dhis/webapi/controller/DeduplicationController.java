package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2019, University of Oslo
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
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            query.setTotal( deduplicationService.countPotentialDuplciates( query ) );
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

        PotentialDuplicate newPotentialDuplicate = new PotentialDuplicate( potentialDuplicate.getTeiA(),
            potentialDuplicate.getTeiB() );

        deduplicationService.addPotentialDuplicate( newPotentialDuplicate );
        return newPotentialDuplicate;
    }

    @PutMapping( value = "/{id}/invalidate" )
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

}
