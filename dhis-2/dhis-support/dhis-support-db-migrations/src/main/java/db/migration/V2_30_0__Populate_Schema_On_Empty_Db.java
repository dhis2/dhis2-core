package db.migration;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.db.flyway.ScriptRunner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class V2_30_0__Populate_Schema_On_Empty_Db extends BaseJavaMigration
{
    public void migrate( Context context )
        throws Exception
    {
        try (Statement select = context.getConnection().createStatement())
        {
            try (ResultSet rows = select.executeQuery(
                "SELECT EXISTS( SELECT * FROM information_schema.tables  WHERE table_name = 'organisationunit');" ))
            {
                if ( rows.next() )
                {
                    boolean nonEmptyDatabase = rows.getBoolean( 1 );
                    if ( !nonEmptyDatabase )
                    {
                        try (Connection mConnection = context.getConnection())
                        {
                            ScriptRunner runner = new ScriptRunner( mConnection, false, false );
                            String file = "classpath:/base-schema/dhis2_base_schema final.sql";
                            runner.runScript( new BufferedReader( new FileReader( file ) ) );

                            String generateUidFunctionSql = "create or replace function generate_uid()\n" + "  returns text as\n" + "$$\n" + "declare\n"
                                + "  chars  text [] := '{0,1,2,3,4,5,6,7,8,9,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z}';\n"
                                + "  result text := chars [11 + random() * (array_length(chars, 1) - 11)];\n" + "begin\n" + "  for i in 1..10 loop\n"
                                + "    result := result || chars [1 + random() * (array_length(chars, 1) - 1)];\n" + "  end loop;\n" + "  return result;\n"
                                + "end;\n" + "$$\n" + "language plpgsql;";

                            try (PreparedStatement preparedStatement = mConnection.prepareStatement( generateUidFunctionSql ))
                            {
                                preparedStatement.executeUpdate();
                            }

                            String uidFunction = "CREATE OR REPLACE FUNCTION uid() RETURNS text AS $$ SELECT substring('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ' "
                                + "FROM (random()*51)::int +1 for 1) || array_to_string(ARRAY(SELECT substring('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789' "
                                + " FROM (random()*61)::int + 1 FOR 1) FROM generate_series(1,10)), '') $$ LANGUAGE sql;";

                            try (PreparedStatement preparedStatement = mConnection.prepareStatement( uidFunction ))
                            {
                                preparedStatement.executeUpdate();
                            }
                        }

                    }

                }
            }
        }
    }
}