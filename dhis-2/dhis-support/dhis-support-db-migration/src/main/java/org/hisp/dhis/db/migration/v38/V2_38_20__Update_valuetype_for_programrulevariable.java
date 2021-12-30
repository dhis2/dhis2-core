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
package org.hisp.dhis.db.migration.v38;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update ValueType parameter for ProgramRuleVariable. ValueType depends upon
 * the value sourceType parameter. ValueType will be fetched from either
 * DataElement or TrackedEntityAttribute or will be provided by user.
 *
 * @author Zubair Asghar
 */
public class V2_38_20__Update_valuetype_for_programrulevariable extends BaseJavaMigration
{
    private static final Logger log = LoggerFactory
        .getLogger( V2_38_20__Update_valuetype_for_programrulevariable.class );

    @Override
    public void migrate( Context context )
        throws Exception
    {
        try ( Statement statement = context.getConnection().createStatement() )
        {
            ResultSet results = statement.executeQuery( "select prv.uid, de.valuetype from programrulevariable prv, " +
                "dataelement de where prv.dataelementid=de.dataelementid" );
            while ( results.next() )
            {
                updateRow( context, results.getString( 1 ), results.getString( 2 ) );
            }
        }

        try ( Statement statement = context.getConnection().createStatement() )
        {
            ResultSet results = statement.executeQuery(
                "select prv.uid, attr.valuetype from programrulevariable prv, trackedentityattribute attr where " +
                    "prv.trackedentityattributeid=attr.trackedentityattributeid" );
            while ( results.next() )
            {
                updateRow( context, results.getString( 1 ), results.getString( 2 ) );
            }
        }
    }

    private void updateRow( Context context, String prvUid, String valueType )
    {
        try ( PreparedStatement statement = context.getConnection()
            .prepareStatement( "update programrulevariable set valuetype = ? where uid = ?" ) )
        {
            statement.setString( 1, valueType );
            statement.setString( 2, prvUid );

            log.debug( "Executing ProgramRuleVariable migration query: [" + statement + "]" );
            statement.executeUpdate();
        }
        catch ( SQLException e )
        {
            log.error( e.getMessage() );
        }
    }
}
