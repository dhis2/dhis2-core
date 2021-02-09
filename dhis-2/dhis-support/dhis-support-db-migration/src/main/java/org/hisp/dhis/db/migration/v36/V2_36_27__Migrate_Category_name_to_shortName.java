/*
 * Copyright (c) 2004-2021, University of Oslo
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.db.migration.helper.UniqueUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jan Bernitt
 */
public class V2_36_27__Migrate_Category_name_to_shortName extends BaseJavaMigration
{

    private static final Logger log = LoggerFactory.getLogger( V2_36_27__Migrate_Category_name_to_shortName.class );

    @Override
    public void migrate( Context context )
        throws Exception
    {
        Map<Long, String> namesById = new HashMap<>();
        Map<Long, String> shortNamesById = new HashMap<>();
        Set<String> uniqueShortNames = new HashSet<>();
        try ( Statement statement = context.getConnection().createStatement() )
        {
            ResultSet results = statement
                .executeQuery( "select categoryid, name, shortname from dataelementcategory;" );
            while ( results.next() )
            {
                long id = results.getLong( 1 );
                namesById.put( id, results.getString( 2 ) );
                String shortName = results.getString( 3 );
                if ( shortName != null && !uniqueShortNames.contains( shortName ) )
                {
                    uniqueShortNames.add( shortName );
                    shortNamesById.put( id, shortName );
                }
            }
        }
        if ( namesById.isEmpty() || shortNamesById.size() == namesById.size() )
        {
            // either no entries or all entries already have a unique name
            return;
        }
        for ( Entry<Long, String> idAndName : namesById.entrySet() )
        {
            long id = idAndName.getKey();
            String name = idAndName.getValue();
            if ( !shortNamesById.containsKey( id ) )
            {
                String shortName = UniqueUtils.addUnique( name, 50, uniqueShortNames );
                try ( PreparedStatement statement = context.getConnection()
                    .prepareStatement( "update dataelementcategory set shortname = ? where categoryid = ?" ) )
                {
                    statement.setLong( 2, id );
                    statement.setString( 1, shortName );

                    log.info( "Executing shortName migration update: [" + statement + "]" );
                    statement.executeUpdate();
                }
                catch ( SQLException ex )
                {
                    log.error( ex.getMessage() );
                    throw ex;
                }
            }
        }
    }

}
