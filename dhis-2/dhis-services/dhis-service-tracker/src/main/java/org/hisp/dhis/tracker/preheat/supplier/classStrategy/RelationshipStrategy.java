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
package org.hisp.dhis.tracker.preheat.supplier.classStrategy;

import static org.hisp.dhis.tracker.preheat.supplier.ClassBasedSupplier.SPLIT_LIST_PARTITION_SIZE;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.preheat.DetachUtils;
import org.hisp.dhis.tracker.preheat.RelationshipPreheatKeySupport;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.mappers.RelationshipMapper;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
@Component
@StrategyFor( value = Relationship.class, mapper = RelationshipMapper.class )
public class RelationshipStrategy implements ClassBasedSupplierStrategy
{
    @NonNull
    private final RelationshipStore relationshipStore;

    @Override
    public void add( TrackerImportParams params, List<List<String>> splitList, TrackerPreheat preheat )
    {
        List<org.hisp.dhis.relationship.Relationship> relationships = retrieveRelationships( splitList );

        preheat.putRelationships( TrackerIdScheme.UID,
            DetachUtils.detach( this.getClass().getAnnotation( StrategyFor.class ).mapper(), relationships ) );
    }

    private List<org.hisp.dhis.relationship.Relationship> retrieveRelationships( List<List<String>> splitList )
    {
        return splitList.stream()
            .flatMap( Collection::stream )
            .collect( Collectors.partitioningBy( RelationshipPreheatKeySupport::isRelationshipPreheatKey ) )
            .entrySet().stream()
            .flatMap( this::getRelationships )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    private Stream<org.hisp.dhis.relationship.Relationship> getRelationships( Map.Entry<Boolean, List<String>> entry )
    {
        return entry.getKey() ? entry.getValue().stream()
            .map( relationshipStore::getByRelationshipKey )
            : Lists.partition( entry.getValue(), SPLIT_LIST_PARTITION_SIZE ).stream()
                .map( relationshipStore::getByUid )
                .flatMap( Collection::stream );
    }
}
