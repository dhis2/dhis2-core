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
package org.hisp.dhis.db.migration.v36;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.common.CodeGenerator;

/**
 * Populates the missing programinstance row that has to be present exactly once
 * for every program without registration.
 *
 * @author Ameen Mohamed
 */
@Slf4j
public class V2_36_56__Add_missing_programinstance_rows_for_programs_without_registration extends BaseJavaMigration
{
    private static final String FETCH_PROGRAMS_MISSING_DEFAULT_PROGRAMINSTANCE = "select p.programid from program p where p.type='WITHOUT_REGISTRATION' and not exists (select pi.programinstanceid from programinstance pi where pi.programid = p.programid)";

    public static final String INSERT_DEFAULT_PI_SQL = "insert into programinstance(programinstanceid,enrollmentdate,programid,status,followup,uid,created,lastupdated,incidentdate,createdatclient,lastupdatedatclient,deleted,storedby) values (nextval('programinstance_sequence'),now(),?,'ACTIVE','false',?,now(),now(),now(),now(),now(),'false','flyway');";

    @Override
    public void migrate( Context context )
        throws Exception
    {
        List<Long> programsWithMissingDefaultProgramInstance = new ArrayList<>();
        try ( Statement stmt = context.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery( FETCH_PROGRAMS_MISSING_DEFAULT_PROGRAMINSTANCE ); )
        {

            while ( rs.next() )
            {
                programsWithMissingDefaultProgramInstance.add( rs.getLong( "programid" ) );
            }
        }
        catch ( SQLException e )
        {
            throw new FlywayException( e );
        }

        try ( PreparedStatement ps = context.getConnection()
            .prepareStatement(
                INSERT_DEFAULT_PI_SQL ) )
        {
            for ( Long programId : programsWithMissingDefaultProgramInstance )
            {
                ps.setLong( 1, programId );
                ps.setString( 2, CodeGenerator.generateUid() );
                ps.addBatch();
            }
            ps.executeBatch();
        }
        catch ( SQLException e )
        {
            throw new FlywayException( e );
        }

    }
}
