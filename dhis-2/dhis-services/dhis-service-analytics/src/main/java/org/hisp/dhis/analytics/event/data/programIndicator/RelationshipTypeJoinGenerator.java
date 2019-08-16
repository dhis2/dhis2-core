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

package org.hisp.dhis.analytics.event.data.programIndicator;

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;

/**
 * Generates a SQL JOIN to join an enrollment or event with a related entity, based on the specified
 * relationship type
 *
 *
 * @author Luciano Fiandesio
 */
public class RelationshipTypeJoinGenerator
{
    protected final static String RELATIONSHIP_JOIN = " "
        + "LEFT JOIN relationship r on r.relationshipid = ri.relationshipid"
        + " LEFT JOIN relationshiptype rty on rty.relationshiptypeid = r.relationshiptypeid"
        + " WHERE rty.relationshiptypeid = ${relationshipid})";

    public static String generate( RelationshipType relationshipType )
    {
        String sql = "";

        if ( relationshipLinksSameEntities( relationshipType ) )
        {
            if ( isTeiToTei( relationshipType ) )
            {
                sql += getTei( relationshipType );
            }
            else if ( isEventToEvent( relationshipType ) )
            {
                sql += getEvent( relationshipType );
            }
            else if ( isEnrollmentToEnrollment( relationshipType ) )
            {
                sql += getEnrollment( relationshipType );

            }
            else
            {
                // TODO throw exception
            }
        }
        else
        {
            sql += " ( ("
                + getFromRelationshipEntity( relationshipType,
                    relationshipType.getFromConstraint().getRelationshipEntity() )
                + ") OR (" + getFromRelationshipEntity( relationshipType,
                    relationshipType.getToConstraint().getRelationshipEntity() )
                + ") )";
        }

        return sql;
    }

    private static String getFromRelationshipEntity( RelationshipType relationshipType,
        RelationshipEntity relationshipEntity )
    {
        switch ( relationshipEntity )
        {

        case TRACKED_ENTITY_INSTANCE:
            return getTei( relationshipType );
        case PROGRAM_STAGE_INSTANCE:
            return getEvent( relationshipType );
        case PROGRAM_INSTANCE:
            return getEnrollment( relationshipType );
        }
        throw new IllegalQueryException( "Non valid Relationship Entity type: "
            + relationshipType.getFromConstraint().getRelationshipEntity().getName() );
    }

    private static boolean relationshipLinksSameEntities( RelationshipType relationshipType )
    {
        return getFromConstraintName( relationshipType ).equalsIgnoreCase( getToConstraintName( relationshipType ) );
    }

    private static boolean isTeiToTei( RelationshipType relationshipType )
    {
        return getFromConstraintName( relationshipType )
            .equalsIgnoreCase( RelationshipEntity.TRACKED_ENTITY_INSTANCE.getName() );
    }

    private static boolean isEventToEvent( RelationshipType relationshipType )
    {
        return getFromConstraintName( relationshipType )
            .equalsIgnoreCase( RelationshipEntity.PROGRAM_STAGE_INSTANCE.getName() );
    }

    private static boolean isEnrollmentToEnrollment( RelationshipType relationshipType )
    {
        return getFromConstraintName( relationshipType )
            .equalsIgnoreCase( RelationshipEntity.PROGRAM_INSTANCE.getName() );
    }

    private static String getFromConstraintName( RelationshipType relationshipType )
    {
        return relationshipType.getFromConstraint().getRelationshipEntity().getName();
    }

    private static String getToConstraintName( RelationshipType relationshipType )
    {
        return relationshipType.getToConstraint().getRelationshipEntity().getName();
    }

    private static String getTei( RelationshipType relationshipType )
    {
        return " ax.tei in (select tei.uid from trackedentityinstance tei"
            + " LEFT JOIN relationshipitem ri on tei.trackedentityinstanceid = ri.trackedentityinstanceid "
            + addRelationshipJoinClause( relationshipType.getId() );
    }

    private static String getEnrollment( RelationshipType relationshipType )
    {
        return " ax.pi in (select pi.uid from programinstance pi"
            + " LEFT JOIN relationshipitem ri on pi.programinstanceid = ri.programinstanceid "
            + addRelationshipJoinClause( relationshipType.getId() );
    }

    private static String getEvent( RelationshipType relationshipType )
    {
        return " ax.pi in (select pi.uid from programstageinstance psi"
            + " LEFT JOIN relationshipitem ri on psi.programstageinstance = ri.programinstanceid "
            + addRelationshipJoinClause( relationshipType.getId() );
    }

    private static String addRelationshipJoinClause(Long relationshipTypeId )
    {
        return new org.apache.commons.text.StrSubstitutor(
            ImmutableMap.<String, Long> builder().put( "relationshipid", relationshipTypeId ).build() )
                .replace( RELATIONSHIP_JOIN );
    }
}
