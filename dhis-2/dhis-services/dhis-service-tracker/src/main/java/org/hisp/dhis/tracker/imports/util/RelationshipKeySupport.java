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
package org.hisp.dhis.tracker.imports.util;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.Objects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;

@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class RelationshipKeySupport
{

    public static boolean hasRelationshipKey( Relationship relationship, RelationshipType relationshipType )
    {
        return ObjectUtils.allNotNull(
            relationshipType,
            relationship.getFrom(),
            relationship.getTo() );
    }

    private static RelationshipKey.RelationshipItemKey getRelationshipItemKey( RelationshipItem relationshipItem )
    {
        if ( Objects.nonNull( relationshipItem ) )
        {
            return RelationshipKey.RelationshipItemKey.builder()
                .trackedEntity( trimToEmpty( relationshipItem.getTrackedEntity() ) )
                .enrollment( trimToEmpty( relationshipItem.getEnrollment() ) )
                .event( trimToEmpty( relationshipItem.getEvent() ) )
                .build();
        }
        throw new IllegalStateException( "Unable to determine uid for relationship item" );
    }

    public static RelationshipKey getRelationshipKey( Relationship relationship, RelationshipType relationshipType )
    {
        return RelationshipKey.of(
            relationshipType.getUid(),
            getRelationshipItemKey( relationship.getFrom() ),
            getRelationshipItemKey( relationship.getTo() ) );
    }
}