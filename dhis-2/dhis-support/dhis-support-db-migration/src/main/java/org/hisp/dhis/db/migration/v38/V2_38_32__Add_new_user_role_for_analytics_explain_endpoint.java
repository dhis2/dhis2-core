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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V2_38_32__Add_new_user_role_for_analytics_explain_endpoint extends BaseJavaMigration
{
    private static final Logger log = LoggerFactory
        .getLogger( V2_38_32__Add_new_user_role_for_analytics_explain_endpoint.class );

    @Override
    public void migrate( Context context )
        throws Exception
    {
        List<Integer> superUserRoleIds = new ArrayList<>();

        try ( Statement statement = context.getConnection().createStatement();
            ResultSet superUserRole = statement.executeQuery(
                "select userroleid from userrole where lower(name)='superuser'" ) )
        {
            while ( superUserRole.next() )
            {
                superUserRoleIds.add( superUserRole.getInt( 1 ) );
            }
        }
        catch ( SQLException e )
        {
            log.error( "Flyway java migration error (select)", e );
            throw new FlywayException( e );
        }

        if ( !superUserRoleIds.isEmpty() )
        {
            try ( PreparedStatement ps = context.getConnection().prepareStatement(
                "INSERT INTO userroleauthorities (userroleid, authority) VALUES (?, 'F_PERFORM_ANALYTICS_EXPLAIN')" ) )
            {
                for ( Integer id : superUserRoleIds )
                {
                    ps.setInt( 1, id );
                    ps.execute();
                }
            }
            catch ( SQLException e )
            {
                log.error( "Flyway java migration error (insert):", e );
                throw new FlywayException( e );
            }
        }
    }
}
