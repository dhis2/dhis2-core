package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
 *
 */

import com.google.common.collect.Lists;
import org.hisp.dhis.deletedobject.DeletedObject;
import org.hisp.dhis.deletedobject.DeletedObjectQuery;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RestController
@RequestMapping( value = "/deletedObjects" )
public class DeletedObjectController
{
    private final FieldFilterService fieldFilterService;
    private final DeletedObjectService deletedObjectService;
    private final ContextService contextService;

    public DeletedObjectController( FieldFilterService fieldFilterService, DeletedObjectService deletedObjectService,
        ContextService contextService )
    {
        this.fieldFilterService = fieldFilterService;
        this.deletedObjectService = deletedObjectService;
        this.contextService = contextService;
    }

    @GetMapping
    public RootNode getDeletedObjects( DeletedObjectQuery query )
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        List<DeletedObject> deletedObjects = deletedObjectService.getDeletedObjects( query );

        RootNode rootNode = NodeUtils.createMetadata();

        if ( !query.isSkipPaging() )
        {
            query.setTotal( deletedObjectService.countDeletedObjects( query ) );
            rootNode.addChild( NodeUtils.createPager( query.getPager() ) );
        }

        rootNode.addChild( fieldFilterService.filter( DeletedObject.class, deletedObjects, fields ) );

        return rootNode;
    }
}
