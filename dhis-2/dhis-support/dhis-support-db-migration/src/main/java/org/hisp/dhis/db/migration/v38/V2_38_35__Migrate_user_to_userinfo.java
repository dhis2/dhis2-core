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

import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V2_38_35__Migrate_user_to_userinfo extends BaseJavaMigration
{
    private static final Logger log = LoggerFactory.getLogger( V2_38_35__Migrate_user_to_userinfo.class );

    public void migrate( Context context )
        throws SQLException
    {
        String query = "UPDATE userinfo set " +
            "username = uc.username, " +
            "password = uc.password, " +
            "secret = uc.secret, " +
            "twofa = uc.twofa, " +
            "externalauth = uc.externalauth, " +
            "openid = uc.openid, " +
            "ldapid = uc.ldapid, " +
            "passwordlastupdated = uc.passwordlastupdated, " +
            "lastlogin = uc.lastlogin, " +
            "restoretoken = uc.restoretoken, " +
            "restoreexpiry = uc.restoreexpiry, " +
            "selfregistered = uc.selfregistered, " +
            "invitation = uc.invitation, " +
            "disabled = uc.disabled, " +
            "uuid = uc.uuid, " +
            "idtoken = uc.idtoken, " +
            "accountexpiry = uc.accountexpiry " +
            "FROM (SELECT * FROM users) AS uc " +
            "WHERE userinfo.userinfoid = uc.userid";

        try ( Statement statement = context.getConnection().createStatement() )
        {
            log.info( "Executing user/usercredentials migration query: [" + query + "]" );
            statement.execute( query );
        }
        catch ( SQLException e )
        {
            log.error( e.getMessage() );
            throw e;
        }
    }
}
