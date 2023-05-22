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
package org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Relationship;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.RelationshipItem;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
public class RelationshipRowCallbackHandler extends AbstractMapper<Relationship>
{
    @Override
    public void processRow( ResultSet rs )
        throws SQLException
    {
        final Relationship relationship = getRelationship( rs );

        this.items.put( extractUid( relationship.getFrom() ), relationship );
        this.items.put( extractUid( relationship.getTo() ), relationship );
    }

    @Override
    Relationship getItem( ResultSet rs )
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
        if ( relationshipItem.getTrackedEntityInstance() != null )
        {
            return relationshipItem.getTrackedEntityInstance().getTrackedEntityInstance();
        }
        else if ( relationshipItem.getEnrollment() != null )
        {
            return relationshipItem.getEnrollment().getEnrollment();
        }
        else if ( relationshipItem.getEvent() != null )
        {
            return relationshipItem.getEvent().getEvent();
        }
        return null; // FIXME: throw exception?
    }

    private Relationship getRelationship( ResultSet rs )
        throws SQLException
    {
        Relationship relationship = new Relationship();
        relationship.setRelationship( rs.getString( "rel_uid" ) );
        relationship.setRelationshipType( rs.getString( "reltype_uid" ) );
        relationship.setRelationshipName( rs.getString( "reltype_name" ) );
        relationship.setFrom( createItem( rs.getString( "from_uid" ) ) );
        relationship.setTo( createItem( rs.getString( "to_uid" ) ) );
        relationship.setBidirectional( rs.getBoolean( "reltype_bi" ) );
        relationship.setCreated( DateUtils.getIso8601NoTz( rs.getTimestamp( "created" ) ) );
        relationship.setLastUpdated( DateUtils.getIso8601NoTz( rs.getTimestamp( "lastupdated" ) ) );

        return relationship;
    }

    /**
     * The SQL query that generates the ResultSet used by this
     * {@see RowCallbackHandler} fetches both sides of a relationship: since
     * each side can be a Tracked Entity Instance, a Enrollment or a Program
     * Stage Instance, the query adds an "hint" to the final result to help this
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

                TrackedEntityInstance tei = new TrackedEntityInstance();
                tei.clear();
                tei.setTrackedEntityInstance( uid );
                ri.setTrackedEntityInstance( tei );
                break;
            case "pi":

                Enrollment enrollment = new Enrollment();
                enrollment.setEnrollment( uid );
                ri.setEnrollment( enrollment );
                break;
            case "psi":

                Event psi = new Event();
                psi.setEvent( uid );
                ri.setEvent( psi );
                break;
            default:
                log.warn( "Expecting tei|psi|pi as type when fetching a relationship, got: " + type );
        }

        return ri;
    }
}
