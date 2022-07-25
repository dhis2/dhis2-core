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
package org.hisp.dhis.db.migration.v38;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentityfilter.FilterPeriod;
import org.hisp.dhis.trackedentityfilter.TrackedEntityInstanceFilter;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class V2_38_37__Add_entityquerycriteria_column_to_trackedentityinstancefilter extends BaseJavaMigration
{
    private static final Logger log = LoggerFactory.getLogger(
        V2_38_37__Add_entityquerycriteria_column_to_trackedentityinstancefilter.class );

    private static final String ADD_ENTITYQUERYCRITERIA_QUERY = "alter table trackedentityinstancefilter add column if not exists entityquerycriteria jsonb default '{}'::jsonb";

    private static final String FETCH_EXISTING_CRITERIA = "select trackedentityinstancefilterid,enrollmentstatus,followup,enrollmentcreatedperiod from trackedentityinstancefilter;";

    private static final String UPDATE_EQC_PREPARED_SQL = "update trackedentityinstancefilter set entityquerycriteria=? where trackedentityinstancefilterid=?";

    private static final String REMOVE_OBSOLETE_COLUMN_SQL = "alter table trackedentityinstancefilter drop column if exists followup, drop column if exists enrollmentcreatedperiod, drop column if exists enrollmentstatus";

    private ObjectMapper objectMapper = JacksonObjectMapperConfig.jsonMapper;

    public void migrate( Context context )
        throws SQLException
    {

        // Step 1: Add new entityquerycriteria jsonb column.
        try ( Statement statement = context.getConnection().createStatement() )
        {
            log.info( "Adding entityquerycriteria column to trackedentityinstancefilter with query: {}",
                ADD_ENTITYQUERYCRITERIA_QUERY );
            statement.execute( ADD_ENTITYQUERYCRITERIA_QUERY );
        }
        catch ( SQLException e )
        {
            log.error( e.getMessage() );
            throw e;
        }

        // Step 2: Fetch existing values from followup,enrollmentstatus and
        // enrollmentcreatedperiod column
        log.info(
            "Fetching existing  entries for trackedentityinstancefilterid,followup,enrollmentstatus and enrollmentcreatedperiod with query: {}",
            FETCH_EXISTING_CRITERIA );
        List<TrackedEntityInstanceFilter> existingFiltersMap = new ArrayList<>();
        try ( Statement statement = context.getConnection().createStatement();
            ResultSet rs = statement.executeQuery( FETCH_EXISTING_CRITERIA ) )
        {
            while ( rs.next() )
            {
                TrackedEntityInstanceFilter filter = new TrackedEntityInstanceFilter();
                filter.setId( rs.getLong( 1 ) );
                filter.setEnrollmentStatus(
                    rs.getString( 2 ) == null ? null : ProgramStatus.valueOf( rs.getString( 2 ) ) );
                filter.setFollowup( rs.getBoolean( 3 ) );
                filter.setEnrollmentCreatedPeriod( rs.getString( 4 ) == null ? null
                    : objectMapper.readValue( rs.getString( 4 ), FilterPeriod.class ) );
                existingFiltersMap.add( filter );
            }
        }
        catch ( SQLException | JsonProcessingException e )
        {
            log.error( e.getMessage() );
            throw new FlywayException( e );
        }

        // Step 3: Add the existing values to new jsonb structure
        try ( PreparedStatement ps = context.getConnection().prepareStatement( UPDATE_EQC_PREPARED_SQL ) )
        {
            int count = 0;
            for ( TrackedEntityInstanceFilter teif : existingFiltersMap )
            {
                PGobject jsonObject = new PGobject();
                jsonObject.setType( "json" );
                jsonObject.setValue( objectMapper.writeValueAsString( teif.getEntityQueryCriteria() ) );
                ps.setObject( 1, jsonObject );
                ps.setLong( 2, teif.getId() );
                count = count + ps.executeUpdate();
            }

            log.info( "TrackedEntityInstanceFilters updated entityquerycriteria for {} rows", count );
        }
        catch ( SQLException | JsonProcessingException e )
        {
            log.error( e.getMessage() );
            throw new FlywayException( e );
        }

        // Step 4: Remove the columns enrollmentstatus,followup and
        // enrollmentcreatedperiod
        try ( Statement statement = context.getConnection().createStatement() )
        {
            log.info(
                "Removing columns enrollmentstatus,followup and enrollmentcreatedperiod from trackedentityinstancefilter with query: {}",
                REMOVE_OBSOLETE_COLUMN_SQL );
            statement.execute( REMOVE_OBSOLETE_COLUMN_SQL );
        }
        catch ( SQLException e )
        {
            log.error( e.getMessage() );
            throw e;
        }
    }
}
