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
        Relationship relationship = getRelationship( rs );

        this.items.put( relationship.getFrom().getTrackedEntityInstance().getTrackedEntityInstance(), getRelationship( rs ) );
        this.items.put( relationship.getTo().getTrackedEntityInstance().getTrackedEntityInstance(), getRelationship( rs ) );
    }

    @Override
    Relationship getItem(ResultSet rs) {
        return null;
    }

    @Override
    String getKeyColumn() {
        return null;
    }


    private Relationship getRelationship( ResultSet rs )
        throws SQLException
    {
        Relationship relationship = new Relationship();
        relationship.setRelationship( rs.getString( "rel_uid" ) );
        relationship.setRelationshipType( rs.getString( "reltype_uid" ) );
        relationship.setRelationshipName( rs.getString( "reltype_name" ) );
        relationship.setFrom( createItem( rs.getString( "to_uid" ) ) );
        relationship.setTo( createItem( rs.getString( "from_uid" ) ) );
        relationship.setBidirectional( rs.getBoolean( "reltype_bi" ) );
        relationship.setCreated( DateUtils.getIso8601NoTz( rs.getDate( "created" ) ) );
        relationship.setLastUpdated( DateUtils.getIso8601NoTz( rs.getDate( "lastupdated" ) ) );

        return relationship;
    }

    private RelationshipItem createItem( String uid )
    {
        RelationshipItem ri = new RelationshipItem();

        TrackedEntityInstance tei = new TrackedEntityInstance();
        tei.clear();
        tei.setTrackedEntityInstance( uid );
        ri.setTrackedEntityInstance( tei );

        return ri;
    }
}
