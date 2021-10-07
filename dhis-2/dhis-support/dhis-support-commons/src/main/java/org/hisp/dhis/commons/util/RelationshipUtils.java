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
package org.hisp.dhis.commons.util;

import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;

public class RelationshipUtils
{

    /**
     * Generates a key of a relationship. The key consists of three parts: The
     * relationship type's uid, from's uid and to's uid, split by an underscore.
     *
     * @param relationship the relationship to generate a key for
     * @return a key
     */
    public static String generateRelationshipKey( Relationship relationship )
    {
        return relationship.getRelationshipType().getUid() + "_" +
            extractRelationshipItemUid( relationship.getFrom() ) + "_" +
            extractRelationshipItemUid( relationship.getTo() );
    }

    /**
     * Generates an inverted key of a relationship. The inverted key consists of
     * three parts: The relationship type's uid, to's uid and from's uid, split
     * by an underscore.
     *
     * @param relationship the relationship to generate an inverted key for
     * @return an inverted key
     */
    public static String generateRelationshipInvertedKey( Relationship relationship )
    {
        return relationship.getRelationshipType().getUid() + "_" +
            extractRelationshipItemUid( relationship.getTo() ) + "_" +
            extractRelationshipItemUid( relationship.getFrom() );
    }

    /**
     * Extracts the uid of the entity represented in a RelationshipItem. A
     * RelationshipItem should only have a single entity represented, and this
     * method will return the first non null entity it fields.
     *
     * @param relationshipItem to extract uid of
     * @return a uid
     */
    public static String extractRelationshipItemUid( RelationshipItem relationshipItem )
    {
        IdentifiableObject identifiableObject = ObjectUtils.firstNonNull( relationshipItem.getTrackedEntityInstance(),
            relationshipItem.getProgramInstance(),
            relationshipItem.getProgramStageInstance() );

        return identifiableObject.getUid();
    }

}
