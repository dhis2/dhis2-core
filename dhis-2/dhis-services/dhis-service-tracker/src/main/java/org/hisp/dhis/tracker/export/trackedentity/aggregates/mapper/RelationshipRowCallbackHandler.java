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
package org.hisp.dhis.tracker.export.trackedentity.aggregates.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntity;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
public class RelationshipRowCallbackHandler extends AbstractMapper<RelationshipItem>
{
    @Override
    public void processRow( ResultSet rs )
        throws SQLException
    {
        final Relationship relationship = getRelationship( rs );
        this.items.put( extractUid( relationship.getFrom() ), relationship.getFrom() );
        this.items.put( extractUid( relationship.getTo() ), relationship.getTo() );
    }

    @Override
    RelationshipItem getItem( ResultSet rs )
    {
        return null;
    }

    @Override
    String getKeyColumn()
    {
        return null;
    }

    private String extractUid( RelationshipItem relationshipItem )
    {
        if ( relationshipItem.getTrackedEntity() != null )
        {
            return relationshipItem.getTrackedEntity().getUid();
        }
        else if ( relationshipItem.getEnrollment() != null )
        {
            return relationshipItem.getEnrollment().getUid();
        }
        else if ( relationshipItem.getEvent() != null )
        {
            return relationshipItem.getEvent().getUid();
        }
        throw new IllegalStateException( "RelationshipItem must have one of trackedEntity, enrollment or event set" );
    }

    private Relationship getRelationship( ResultSet rs )
        throws SQLException
    {
        Relationship relationship = new Relationship();
        relationship.setUid( rs.getString( "rel_uid" ) );
        RelationshipType type = new RelationshipType();
        type.setUid( rs.getString( "reltype_uid" ) );
        type.setName( rs.getString( "reltype_name" ) );
        type.setBidirectional( rs.getBoolean( "reltype_bi" ) );
        relationship.setRelationshipType( type );
        relationship.setCreated( rs.getTimestamp( "created" ) );
        relationship.setLastUpdated( rs.getTimestamp( "lastupdated" ) );

        RelationshipItem from = createItem( rs.getString( "from_uid" ) );
        from.setRelationship( relationship );
        relationship.setFrom( from );

        RelationshipItem to = createItem( rs.getString( "to_uid" ) );
        to.setRelationship( relationship );
        relationship.setTo( to );

        return relationship;
    }

    /**
     * The SQL query that generates the ResultSet used by this
     * {@see RowCallbackHandler} fetches both sides of a relationship: since
     * each side can be a Tracked Entity Instance, a Enrollment or a Program
     * Stage Instance, the query adds a "hint" to the final result to help this
     * Handler to correctly associate the type to the left or right side of the
     * relationship. The "typeWithUid" variable contains the UID of the object
     * and a string representing the type. E.g.
     *
     * tei|dj3382832 psi|332983893
     *
     * This function parses the string and extract the type and the uid, in
     * order to instantiate the appropriate object and assign it to the
     * {@see RelationshipItem}
     *
     * @param typeWithUid a String containing the object type and the UID of the
     *        object, separated by | (pipe)
     * @return a {@see RelationshipItem}
     */
    private RelationshipItem createItem( String typeWithUid )
    {
        if ( StringUtils.isEmpty( typeWithUid ) )
        {
            return new RelationshipItem();
        }
        RelationshipItem ri = new RelationshipItem();

        final String type = typeWithUid.split( "\\|" )[0];
        final String uid = typeWithUid.split( "\\|" )[1];

        switch ( type )
        {
        case "tei":
            TrackedEntity tei = new TrackedEntity();
            tei.setUid( uid );
            ri.setTrackedEntity( tei );
            break;
        case "pi":
            Enrollment enrollment = new Enrollment();
            enrollment.setUid( uid );
            ri.setEnrollment( enrollment );
            break;
        case "psi":
            Event event = new Event();
            event.setUid( uid );
            ri.setEvent( event );
            break;
        default:
            log.warn( "Expecting tei|psi|pi as type when fetching a relationship, got: " + type );
        }

        return ri;
    }
}
