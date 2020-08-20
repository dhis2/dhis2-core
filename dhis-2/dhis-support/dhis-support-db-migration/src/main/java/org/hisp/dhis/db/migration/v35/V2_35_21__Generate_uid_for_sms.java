package org.hisp.dhis.db.migration.v35;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
import org.hisp.dhis.common.CodeGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class V2_35_21__Generate_uid_for_sms extends BaseJavaMigration
{
    private static final Logger log = LoggerFactory.getLogger( V2_35_21__Generate_uid_for_sms.class );

    @Override
    public void migrate( Context context ) throws Exception
    {
        List<Long> incomingSmsIds = new ArrayList<>();
        List<Long> outboundSmsIds = new ArrayList<>();

        try ( Statement statement = context.getConnection().createStatement(); ResultSet resultSetIncomingSms = statement.executeQuery( "select id from incomingsms" ) )
        {
            while ( resultSetIncomingSms.next() )
            {
                incomingSmsIds.add( resultSetIncomingSms.getLong("id") );
            }
        }

        try ( Statement statement = context.getConnection().createStatement(); ResultSet resultSetOutboundSms = statement.executeQuery( "select id from outbound_sms" ) )
        {
            while ( resultSetOutboundSms.next() )
            {
                outboundSmsIds.add( resultSetOutboundSms.getLong( "id" ) );
            }
        }

        if ( !incomingSmsIds.isEmpty() )
        {
            for ( long id : incomingSmsIds )
            {
                String uid = CodeGenerator.generateCode( 11 );

                try ( PreparedStatement ps = context.getConnection().prepareStatement( "UPDATE incomingsms SET uid = ?  where  id = ?;" ) )
                {
                    ps.setString( 1, uid );
                    ps.setLong( 2, id );

                    ps.execute();
                }
            }

            log.info( "Incoming SMS uids have been updated." );
        }

        if ( !outboundSmsIds.isEmpty() )
        {
            for ( long id : outboundSmsIds )
            {
                String uid = CodeGenerator.generateCode( 11 );

                try ( PreparedStatement ps = context.getConnection().prepareStatement( "UPDATE outbound_sms SET uid = ? where  id = ?;" ) )
                {
                    ps.setString( 1, uid );
                    ps.setLong( 2, id );

                    ps.execute();
                }

            }

            log.info( "Outbound SMS uids have been updated." );
        }
    }
}
