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
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Multimap;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.util.DateUtils;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowCallbackHandler;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityInstanceRowCallbackHandler
        implements
        RowCallbackHandler
{
    private Map<String, TrackedEntityInstance> items;

    public TrackedEntityInstanceRowCallbackHandler( )
    {
        this.items = new HashMap<>();
    }

    private TrackedEntityInstance getTei(ResultSet rs )
        throws SQLException
    {

        TrackedEntityInstance tei = new TrackedEntityInstance();

        PGobject geometry = (PGobject) rs.getObject( "geometry" );

        tei.setTrackedEntityInstance( rs.getString( "teiuid" ) );
        tei.setOrgUnit( rs.getString( "ou_uid" ) );
        tei.setTrackedEntityType( rs.getString( "type_uid" ) );
        tei.setCreated( DateUtils.getIso8601NoTz( rs.getDate( "created" ) ) );
        tei.setCreatedAtClient( DateUtils.getIso8601NoTz( rs.getDate( "createdatclient" ) ) );
        tei.setLastUpdated( DateUtils.getIso8601NoTz( rs.getDate( "lastupdated" ) ) );
        tei.setLastUpdatedAtClient( DateUtils.getIso8601NoTz( rs.getDate( "lastupdatedatclient" ) ) );
        tei.setInactive( rs.getBoolean( "inactive" ) );
        tei.setDeleted( rs.getBoolean( "deleted" ) );

        if ( geometry != null )
        {
            // TODO how to convert PGobject to Geometry
            // tei.setGeometry( geometry );
            // tei.setFeatureType( FeatureType.getTypeFromName( geometry.getGeometryType() )
            // );
            // tei.setCoordinates( GeoUtils.getCoordinatesFromGeometry( geometry ) );
        }

        return tei;
    }

    @Override
    public void processRow( ResultSet rs )
        throws SQLException
    {
        this.items.put( rs.getString( "teiuid" ), getTei( rs ) );
    }

    public Map<String, TrackedEntityInstance> getItems()
    {
        return this.items;
    }
}
