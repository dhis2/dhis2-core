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

package org.hisp.dhis.dxf2.events.trackedentity.store.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Luciano Fiandesio
 */
public class RelationshipRowCallbackHandler
    extends
    AbstractMapper<Relationship>
{
    @Override
    public void processRow( ResultSet rs )
        throws SQLException
    {
        final Relationship relationship = getRelationship( rs );

        this.items.put( extractUid( relationship.getFrom() ), relationship );
        this.items.put( extractUid( relationship.getTo() ), relationship );
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
        relationship.setCreated( DateUtils.getIso8601NoTz( rs.getDate( "created" ) ) );
        relationship.setLastUpdated( DateUtils.getIso8601NoTz( rs.getDate( "lastupdated" ) ) );

        return relationship;
    }

    private RelationshipItem createItem( String typeWithUid )
    {
        if ( StringUtils.isEmpty( typeWithUid ) )
        {
            return new RelationshipItem();
        }
        RelationshipItem ri = new RelationshipItem();

        final String type = typeWithUid.split("\\|")[0];
        final String uid = typeWithUid.split("\\|")[1];

        switch ( type )
        {
        case "tei":

            TrackedEntityInstance tei = new TrackedEntityInstance();
            tei.clear();
            tei.setTrackedEntityInstance( uid );
            ri.setTrackedEntityInstance( tei );
            break;
        case "pi":
            Enrollment pi = new Enrollment();
            pi.setEnrollment( uid );
            ri.setEnrollment( pi );
            break;
        case "psi":

            Event psi = new Event();
            psi.setEvent( uid );
            ri.setEvent( psi );
            break;
        }
        return ri;
    }
}
