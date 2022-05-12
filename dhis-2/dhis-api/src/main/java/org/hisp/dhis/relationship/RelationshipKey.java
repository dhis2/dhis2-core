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
package org.hisp.dhis.relationship;

import static org.apache.commons.lang3.StringUtils.splitByWholeSeparatorPreserveAllTokens;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.apache.commons.lang3.StringUtils;

@Data
@Builder( toBuilder = true )
@AllArgsConstructor( staticName = "of" )
public class RelationshipKey
{

    private static final String RELATIONSHIP_KEY_SEPARATOR = "-";

    private final String type;

    private final RelationshipItemKey from;

    private final RelationshipItemKey to;

    public String asString()
    {
        return String.join( RELATIONSHIP_KEY_SEPARATOR, type, from.asString(), to.asString() );
    }

    public static RelationshipKey fromString( String relationshipKeyAsString )
    {
        String[] splitKey = splitByWholeSeparatorPreserveAllTokens( relationshipKeyAsString,
            RELATIONSHIP_KEY_SEPARATOR );
        if ( splitKey.length != 7 )
        {
            throw new IllegalArgumentException(
                "components in relationshipkey must be 7. Got " + relationshipKeyAsString + " instead" );
        }
        return RelationshipKey.builder()
            .type( splitKey[0] )
            .from( RelationshipItemKey.builder()
                .trackedEntity( splitKey[1] )
                .enrollment( splitKey[2] )
                .event( splitKey[3] )
                .build() )
            .to( RelationshipItemKey.builder()
                .trackedEntity( splitKey[4] )
                .enrollment( splitKey[5] )
                .event( splitKey[6] )
                .build() )
            .build();
    }

    public RelationshipKey inverseKey()
    {
        return toBuilder()
            .from( to )
            .to( from )
            .build();
    }

    @Data
    @Builder
    public static class RelationshipItemKey
    {
        private final String trackedEntity;

        private final String enrollment;

        private final String event;

        public String asString()
        {
            if ( isTrackedEntity() )
            {
                return trackedEntity;
            }
            else if ( isEnrollment() )
            {
                return enrollment;
            }
            else if ( isEvent() )
            {
                return event;
            }

            return "ERROR";
        }

        public boolean isTrackedEntity()
        {
            return StringUtils.isNoneBlank( trackedEntity );
        }

        public boolean isEnrollment()
        {
            return StringUtils.isNoneBlank( enrollment );
        }

        public boolean isEvent()
        {
            return StringUtils.isNoneBlank( event );
        }
    }
}
