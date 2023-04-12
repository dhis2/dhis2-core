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
package org.hisp.dhis.tracker.trackedentity.aggregates.mapper;

import static org.hisp.dhis.tracker.trackedentity.aggregates.mapper.JsonbToObjectHelper.setUserInfoSnapshot;
import static org.hisp.dhis.tracker.trackedentity.aggregates.mapper.MapperGeoUtils.resolveGeometry;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.CREATED;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.CREATEDCLIENT;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.CREATED_BY;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.DELETED;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.GEOMETRY;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.INACTIVE;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.LAST_UPDATED_BY;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.ORGUNIT_UID;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.POTENTIALDUPLICATE;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.TYPE_UID;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.UID;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.UPDATED;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.COLUMNS.UPDATEDCLIENT;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.TrackedEntityInstanceQuery.getColumnName;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.springframework.jdbc.core.RowCallbackHandler;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityInstanceRowCallbackHandler
    implements
    RowCallbackHandler
{
    private final Map<String, TrackedEntityInstance> items;

    public TrackedEntityInstanceRowCallbackHandler()
    {
        this.items = new LinkedHashMap<>();
    }

    private TrackedEntityInstance getTei( ResultSet rs )
        throws SQLException
    {

        TrackedEntityInstance tei = new TrackedEntityInstance();
        tei.setUid( rs.getString( getColumnName( UID ) ) );
        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid( rs.getString( getColumnName( TYPE_UID ) ) );
        tei.setTrackedEntityType( trackedEntityType );

        OrganisationUnit orgUnit = new OrganisationUnit();
        orgUnit.setUid( rs.getString( getColumnName( ORGUNIT_UID ) ) );
        tei.setOrganisationUnit( orgUnit );

        tei.setCreated( rs.getTimestamp( getColumnName( CREATED ) ) );
        tei.setCreatedAtClient( rs.getTimestamp( getColumnName( CREATEDCLIENT ) ) );
        setUserInfoSnapshot( rs, getColumnName( CREATED_BY ), tei::setCreatedByUserInfo );
        tei.setLastUpdated( rs.getTimestamp( getColumnName( UPDATED ) ) );
        tei.setLastUpdatedAtClient( rs.getTimestamp( getColumnName( UPDATEDCLIENT ) ) );
        setUserInfoSnapshot( rs, getColumnName( LAST_UPDATED_BY ), tei::setLastUpdatedByUserInfo );
        tei.setInactive( rs.getBoolean( getColumnName( INACTIVE ) ) );
        tei.setDeleted( rs.getBoolean( getColumnName( DELETED ) ) );
        tei.setPotentialDuplicate( rs.getBoolean( getColumnName( POTENTIALDUPLICATE ) ) );
        resolveGeometry( rs.getBytes( getColumnName( GEOMETRY ) ) ).ifPresent( tei::setGeometry );

        return tei;
    }

    @Override
    public void processRow( ResultSet rs )
        throws SQLException
    {
        this.items.put( rs.getString( "tei_uid" ), getTei( rs ) );
    }

    public Map<String, TrackedEntityInstance> getItems()
    {
        return this.items;
    }
}
