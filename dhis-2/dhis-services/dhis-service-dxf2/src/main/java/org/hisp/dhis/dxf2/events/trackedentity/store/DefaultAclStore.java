package org.hisp.dhis.dxf2.events.trackedentity.store;

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

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Luciano Fiandesio
 */
@Repository
public class DefaultAclStore
    implements
    AclStore
{
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final static String USER_SQL_PARAM_NAME = "userId";

    private final static String PUBLIC_ACCESS_CONDITION = "(publicaccess LIKE '__r%' OR publicaccess IS NULL)";

    private final static String USERACCESS_CONDITION = "(SELECT useraccessid FROM useraccess WHERE access LIKE '__r%' AND useraccess.userid = :"
        + USER_SQL_PARAM_NAME + ")";

    private final static String USERGROUPACCESS_CONDITION = "(SELECT usergroupaccessid FROM usergroupaccess "
        + "WHERE usergroupid IN (SELECT usergroupid FROM usergroupmembers WHERE userid = :" + USER_SQL_PARAM_NAME
        + "))";

    private final static String GET_TEI_TYPE_ACL = "SELECT trackedentitytypeid FROM trackedentitytype "
        + "        WHERE " + PUBLIC_ACCESS_CONDITION
        + "           OR trackedentitytypeid IN  (SELECT trackedentitytypeid "
        + "               FROM trackedentitytypeuseraccesses WHERE useraccessid IN " + USERACCESS_CONDITION + ")"
        + "           OR trackedentitytypeid IN (SELECT trackedentitytypeid "
        + "               FROM trackedentitytypeusergroupaccesses WHERE usergroupaccessid IN "
        + USERGROUPACCESS_CONDITION + ")";

    final static String GET_PROGRAM_ACL = "SELECT p.programid FROM program p WHERE "
        + PUBLIC_ACCESS_CONDITION + " OR ( p.programid IN (SELECT programid "
        + "               FROM programuseraccesses pua WHERE pua.useraccessid IN " + USERACCESS_CONDITION
        + ") OR p.programid IN (SELECT programid FROM programusergroupaccesses puga "
        + "               WHERE puga.usergroupaccessid IN " + USERGROUPACCESS_CONDITION + "))";

    final static String GET_PROGRAMSTAGE_ACL = "SELECT ps.programstageid FROM programstage ps "
        + "        WHERE " + PUBLIC_ACCESS_CONDITION + " OR ( "
        + "            ps.programstageid IN (SELECT psua.programstageid "
        + "               FROM programstageuseraccesses psua WHERE psua.useraccessid IN " + USERACCESS_CONDITION + ")"
        + "               OR ps.programid IN "
        + "              (SELECT psuga.programid FROM programstageusergroupaccesses psuga "
        + "               WHERE psuga.usergroupaccessid IN " + USERGROUPACCESS_CONDITION + "))";

    private final static String GET_RELATIONSHIPTYPE_ACL = "SELECT rs.relationshiptypeid "
        + "        FROM relationshiptype rs WHERE " +
            "" + PUBLIC_ACCESS_CONDITION + " OR ( "
        + "            rs.relationshiptypeid IN (SELECT rtua.relationshiptypeid "
        + "               FROM relationshiptypeuseraccesses rtua WHERE rtua.useraccessid IN " + USERACCESS_CONDITION
        + ") " + "               OR rs.relationshiptypeid IN (SELECT rtuga.relationshiptypeid "
        + "               FROM relationshiptypeusergroupaccesses rtuga "
        + "               WHERE rtuga.usergroupaccessid IN " + USERGROUPACCESS_CONDITION + "))";

    public DefaultAclStore( @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    @Override
    public List<Long> getAccessibleTrackedEntityInstanceTypes( Long userId )
    {
        return executeAclQuery( userId, GET_TEI_TYPE_ACL, "trackedentitytypeid" );
    }

    @Override
    public List<Long> getAccessiblePrograms( Long userId )
    {
        return executeAclQuery( userId, GET_PROGRAM_ACL, "programid" );
    }

    @Override
    public List<Long> getAccessibleProgramStages( Long userId )
    {
        return executeAclQuery( userId, GET_PROGRAMSTAGE_ACL, "programstageid" );
    }

    @Override
    public List<Long> getAccessibleRelationshipTypes( Long userId )
    {
        return executeAclQuery( userId, GET_RELATIONSHIPTYPE_ACL, "relationshiptypeid" );
    }

    private List<Long> executeAclQuery( Long userId, String sql, String primaryKey )
    {
        return jdbcTemplate.query( sql, new MapSqlParameterSource( USER_SQL_PARAM_NAME, userId ),
            ( rs, i ) -> rs.getLong( primaryKey ) );
    }

}
