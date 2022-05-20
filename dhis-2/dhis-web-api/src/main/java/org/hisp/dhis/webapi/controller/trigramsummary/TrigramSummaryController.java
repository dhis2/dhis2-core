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
package org.hisp.dhis.webapi.controller.trigramsummary;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.*;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeTableManager;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Trigram Summary endpoint to get a summary of all the trigram indexes and
 * indexable attributes
 *
 * @author Ameen Mohamed
 */
@Controller
@RequestMapping( value = TrigramSummaryController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@AllArgsConstructor
public class TrigramSummaryController
{
    public static final String RESOURCE_PATH = "/trigramSummary";

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final TrackedEntityAttributeTableManager trackedEntityAttributeTableManager;

    protected final ContextService contextService;

    protected final AclService aclService;

    private final FieldFilterService fieldFilterService;

    @GetMapping( produces = APPLICATION_JSON_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public @ResponseBody TrigramSummary getTrigramSummary( @RequestParam Map<String, String> rpParameters )
    {
        TrigramSummary trigramSummary = new TrigramSummary();

        List<String> fields = new ArrayList<>( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.defaultPreset().getFields() );
        }

        Set<TrackedEntityAttribute> allIndexableAttributes = trackedEntityAttributeService
            .getAllTrigramIndexableTrackedEntityAttributes();

        Set<String> allIndexableAttributeUids = allIndexableAttributes.stream().map( TrackedEntityAttribute::getUid )
            .collect( Collectors.toSet() );

        List<Long> indexedAttributeIds = trackedEntityAttributeTableManager.getAttributeIdsWithTrigramIndexCreated();

        List<TrackedEntityAttribute> allIndexedAttributes;

        List<TrackedEntityAttribute> indexedAttributes = new ArrayList<>();

        Set<TrackedEntityAttribute> indexableAttributes = new HashSet<>( allIndexableAttributes );
        List<TrackedEntityAttribute> obsoleteIndexedAttributes = new ArrayList<>();

        if ( !indexedAttributeIds.isEmpty() )
        {
            allIndexedAttributes = trackedEntityAttributeService.getTrackedEntityAttributesById( indexedAttributeIds );

            for ( TrackedEntityAttribute indexedAttribute : allIndexedAttributes )
            {
                if ( !allIndexableAttributeUids.contains( indexedAttribute.getUid() ) )
                {
                    obsoleteIndexedAttributes.add( indexedAttribute );
                }
                else
                {
                    indexedAttributes.add( indexedAttribute );
                }
            }

            indexableAttributes.removeAll( allIndexedAttributes );
        }

        trigramSummary.setIndexedAttributes( fieldFilterService.toObjectNodes( indexedAttributes, fields ) );
        trigramSummary
            .setObsoleteIndexedAttributes( fieldFilterService.toObjectNodes( obsoleteIndexedAttributes, fields ) );
        trigramSummary
            .setIndexableAttributes(
                fieldFilterService.toObjectNodes( new ArrayList<>( indexableAttributes ), fields ) );

        return trigramSummary;
    }
}
