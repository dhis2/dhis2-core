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
package org.hisp.dhis.tracker.preheat.supplier;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.mappers.RelationshipMapper;
import org.hisp.dhis.tracker.util.RelationshipKeySupport;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DuplicateRelationshipSupplier extends AbstractPreheatSupplier
{
    @NonNull
    private final RelationshipStore relationshipStore;

    @Override
    public void preheatAdd( TrackerImportParams params, TrackerPreheat preheat )
    {
        List<org.hisp.dhis.relationship.Relationship> relationships = retrieveRelationshipKeys(
            params.getRelationships(), preheat );

        relationships.stream()
            .map( RelationshipMapper.INSTANCE::map )
            .filter( Objects::nonNull )
            .forEach( preheat::addExistingRelationship );
    }

    private List<org.hisp.dhis.relationship.Relationship> retrieveRelationshipKeys( List<Relationship> relationships,
        TrackerPreheat preheat )
    {
        List<RelationshipType> relationshipTypes = preheat.getAll( RelationshipType.class );
        List<String> keys = relationships.stream()
            .filter(
                rel -> RelationshipKeySupport.hasRelationshipKey( rel, getRelationshipType( rel, relationshipTypes ) ) )
            .map( rel -> RelationshipKeySupport.getRelationshipKey( rel, getRelationshipType( rel, relationshipTypes ) )
                .asString() )
            .collect( Collectors.toList() );

        return relationshipStore.getByUid( relationshipStore.getUidsByRelationshipKeys( keys ) );
    }

    private RelationshipType getRelationshipType( Relationship rel, List<RelationshipType> relationshipTypes )
    {
        return relationshipTypes.stream()
            .filter( type -> rel.getRelationshipType().isEqualTo( type ) )
            .findAny()
            .orElse( null );
    }
}
