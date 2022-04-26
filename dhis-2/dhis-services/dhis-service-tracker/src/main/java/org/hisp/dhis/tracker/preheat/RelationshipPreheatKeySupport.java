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
package org.hisp.dhis.tracker.preheat;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.Objects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.tracker.domain.Relationship;

@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class RelationshipPreheatKeySupport
{

    public static boolean hasRelationshipKey( Relationship relationship )
    {
        return ObjectUtils.allNotNull(
            relationship.getRelationshipType(),
            relationship.getFrom(),
            relationship.getTo() );
    }

    public static RelationshipKey getRelationshipKey( Relationship relationship )
    {
        return getRelationshipKey(
            relationship.getRelationshipType(),
            getRelationshipItemKey( relationship.getFrom() ),
            getRelationshipItemKey( relationship.getTo() ) );
    }

    public static RelationshipKey getRelationshipKey( org.hisp.dhis.relationship.Relationship relationship )
    {
        return getRelationshipKey(
            relationship.getRelationshipType().getUid(),
            getRelationshipItemKey( relationship.getFrom() ),
            getRelationshipItemKey( relationship.getTo() ) );
    }

    private static RelationshipKey getRelationshipKey( String relationshipType,
        RelationshipKey.RelationshipItemKey from, RelationshipKey.RelationshipItemKey to )
    {
        return RelationshipKey.of( relationshipType, from, to );
    }

    public static RelationshipKey.RelationshipItemKey getRelationshipItemKey(
        org.hisp.dhis.tracker.domain.RelationshipItem relationshipItem )
    {
        if ( Objects.nonNull( relationshipItem ) )
        {
            return RelationshipKey.RelationshipItemKey.builder()
                .trackedEntity( trimToEmpty( relationshipItem.getTrackedEntity() == null ? null
                    : relationshipItem.getTrackedEntity().getTrackedEntity() ) )
                .enrollment( trimToEmpty( relationshipItem.getEnrollment() == null ? null
                    : relationshipItem.getEnrollment().getEnrollment() ) )
                .event(
                    trimToEmpty( relationshipItem.getEvent() == null ? null : relationshipItem.getEvent().getEvent() ) )
                .build();
        }
        throw new IllegalStateException( "Unable to determine uid for relationship item" );
    }

    public static RelationshipKey.RelationshipItemKey getRelationshipItemKey( RelationshipItem relationshipItem )
    {
        if ( Objects.nonNull( relationshipItem ) )
        {
            return RelationshipKey.RelationshipItemKey.builder()
                .trackedEntity( getUidOrEmptyString( relationshipItem.getTrackedEntityInstance() ) )
                .enrollment( getUidOrEmptyString( relationshipItem.getProgramInstance() ) )
                .event( getUidOrEmptyString( relationshipItem.getProgramStageInstance() ) )
                .build();
        }
        throw new IllegalStateException( "Unable to determine uid for relationship item" );
    }

    private static String getUidOrEmptyString( BaseIdentifiableObject baseIdentifiableObject )
    {
        return Objects.isNull( baseIdentifiableObject ) ? ""
            : StringUtils.trimToEmpty( baseIdentifiableObject.getUid() );
    }

    public static boolean isRelationshipPreheatKey( String s )
    {
        try
        {
            RelationshipKey.fromString( s );
            return true;
        }
        catch ( IllegalArgumentException e )
        {
            return false;
        }
    }
}
