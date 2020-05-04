package org.hisp.dhis.dxf2.events.event.context;

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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextProgramsSupplier" )
public class ProgramSupplier extends AbstractSupplier<Map<String, Program>>
{
    private final static String PROGRAM_CACHE_KEY = "000P";

    // @formatter:off
    private final Cache<String, Map<String, Program>> programsCache = new Cache2kBuilder<String, Map<String, Program>>() {}
        .expireAfterWrite( 30, TimeUnit.MINUTES ) // expire/refresh after 30 minutes
        .build();
    // @formatter:on

    public ProgramSupplier( NamedParameterJdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    @Override
    public Map<String, Program> get( List<Event> eventList )
    {
        Map<String, Program> programMap = programsCache.get( PROGRAM_CACHE_KEY );
        if ( programMap == null )
        {
            programMap = loadPrograms();

            Map<Long, Set<OrganisationUnit>> ouMap = loadOrgUnits();
            Map<Long, Set<UserAccess>> programUserAccessMap = loadUserAccessesForPrograms();
            Map<Long, Set<UserAccess>> programStageUserAccessMap = loadUserAccessesForProgramStages();
            // FIXME: this will not work in the ACL layer, because it expects all user in
            // the group
            Map<Long, Set<UserGroupAccess>> programUserGroupAccessMap = loadGroupUserAccessesForPrograms();
            Map<Long, Set<UserGroupAccess>> programStageUserGroupAccessMap = loadGroupUserAccessesForProgramStages();

            for ( Program program : programMap.values() )
            {
                program.setOrganisationUnits( ouMap.get( program.getId() ) );
                program.setUserAccesses( programUserAccessMap.get( program.getId() ) );
                program.setUserGroupAccesses( programUserGroupAccessMap.get( program.getId() ) );
                for ( ProgramStage programStage : program.getProgramStages() )
                {
                    programStage.setUserAccesses( programStageUserAccessMap.get( programStage.getId() ) );
                    programStage.setUserGroupAccesses( programStageUserGroupAccessMap.get( programStage.getId() ) );
                }
            }

            programsCache.put( PROGRAM_CACHE_KEY, programMap );
        }
        return programMap;
    }

    private Map<Long, Set<OrganisationUnit>> loadOrgUnits()
    {
        final String sql = "select p.programid, o.uid, o.organisationunitid from program_organisationunits p join organisationunit o on p.organisationunitid = o.organisationunitid order by programid";
        return jdbcTemplate.query( sql, ( ResultSet rs ) -> {
            Map<Long, Set<OrganisationUnit>> results = new HashMap<>();
            long programId = 0;
            while ( rs.next() )
            {
                if ( programId != rs.getLong( "programid" ) )
                {
                    Set<OrganisationUnit> ouSet = new HashSet<>();
                    ouSet.add( toOrganisationUnit( rs ) );
                    results.put( rs.getLong( "programid" ), ouSet );
                    programId = rs.getLong( "programid" );
                }
                else
                {
                    results.get( rs.getLong( "programid" ) ).add( toOrganisationUnit( rs ) );
                }
            }
            return results;
        } );
    }

    private Map<Long, Set<UserAccess>> loadUserAccessesForPrograms()
    {
        final String sql = "select pua.programid, pua.useraccessid, ua.useraccessid, ua.access, ua.userid, u.uid "
            + "from programuseraccesses pua join useraccess ua on pua.useraccessid = ua.useraccessid "
            + "join users u on ua.userid = ua.userid order by programid";

        return fetchUserAccesses( sql, "programid");
    }

    private Map<Long, Set<UserAccess>> loadUserAccessesForProgramStages()
    {
        final String sql = "select psua.programstageid, psua.useraccessid, ua.useraccessid, ua.access, ua.userid, u.uid "
            + "from programstageuseraccesses psua join useraccess ua on psua.useraccessid = ua.useraccessid "
            + "join users u on ua.userid = ua.userid order by programstageid";

        return fetchUserAccesses( sql, "programstageid");
    }

    private Map<Long, Set<UserAccess>> fetchUserAccesses( String sql, String column )
    {
        return jdbcTemplate.query( sql, ( ResultSet rs ) -> {
            Map<Long, Set<UserAccess>> results = new HashMap<>();
            long programStageId = 0;
            while ( rs.next() )
            {
                if ( programStageId != rs.getLong( column ) )
                {
                    Set<UserAccess> aclSet = new HashSet<>();
                    aclSet.add( toUserAccess( rs ) );
                    results.put( rs.getLong( column ), aclSet );

                    programStageId = rs.getLong( column );
                }
                else
                {
                    results.get( rs.getLong( column ) ).add( toUserAccess( rs ) );
                }
            }
            return results;
        } );
    }
    
    private Map<Long, Set<UserGroupAccess>> loadGroupUserAccessesForPrograms()
    {
        final String sql = "select puga.programid, puga.usergroupaccessid, u.access, u.usergroupid, ug.uid "
            + "from programusergroupaccesses puga "
            + "join usergroupaccess u on puga.usergroupaccessid = u.usergroupaccessid "
            + "join usergroup ug on u.usergroupid = ug.usergroupid order by programid";

        return fetchUserGroupAccess( sql, "programid" );
    }

    private Map<Long, Set<UserGroupAccess>> loadGroupUserAccessesForProgramStages()
    {
        final String sql = "select psuga.programid as programstageid, psuga.usergroupaccessid, u.access, u.usergroupid, ug.uid " +
                "from programstageusergroupaccesses psuga " +
                "join usergroupaccess u on psuga.usergroupaccessid = u.usergroupaccessid " +
                "join usergroup ug on u.usergroupid = ug.usergroupid " +
                "order by programstageid";

        return fetchUserGroupAccess(sql, "programstageid");
    }
    
    private Map<Long, Set<UserGroupAccess>> fetchUserGroupAccess( String sql, String column )
    {
        return jdbcTemplate.query( sql, ( ResultSet rs ) -> {
            Map<Long, Set<UserGroupAccess>> results = new HashMap<>();
            long programId = 0;
            while ( rs.next() )
            {
                if ( programId != rs.getLong( column ) )
                {
                    Set<UserGroupAccess> aclSet = new HashSet<>();
                    aclSet.add( toUserGroupAccess( rs ) );
                    results.put( rs.getLong( column ), aclSet );

                    programId = rs.getLong( column );
                }
                else
                {
                    results.get( rs.getLong( column ) ).add( toUserGroupAccess( rs ) );
                }
            }
            return results;
        } );
    }

    private Map<String, Program> loadPrograms()
    {
        final String sql = "select p.programid, p.uid, p.name, p.type, p.publicaccess, c.categorycomboid as catcombo_id, c.uid as catcombo_uid, c.name as catcombo_name, "
            + "            ps.programstageid as ps_id, ps.uid as ps_uid, ps.featuretype as ps_feature_type, ps.sort_order, ps.publicaccess as ps_public_access"
            + "            from program p LEFT JOIN categorycombo c on p.categorycomboid = c.categorycomboid "
            + "                    LEFT JOIN programstage ps on p.programid = ps.programid "
            + "                    LEFT JOIN program_organisationunits pou on p.programid = pou.programid "
            + "                    LEFT JOIN organisationunit ou on pou.organisationunitid = ou.organisationunitid "
            + "            group by p.programid, p.uid, p.name, p.type, c.categorycomboid, c.uid, c.name, ps.programstageid, ps.uid , ps.featuretype, ps.sort_order "
            + "            order by p.programid, ps.sort_order";

        return jdbcTemplate.query( sql, ( ResultSet rs ) -> {
            Map<String, Program> results = new HashMap<>();
            long programId = 0;
            while ( rs.next() )
            {
                if ( programId != rs.getLong( "programid" ) )
                {
                    Set<ProgramStage> programStages = new HashSet<>();
                    Program program = new Program();
                    program.setId( rs.getLong( "programid" ) );
                    program.setUid( rs.getString( "uid" ) );
                    program.setName( rs.getString( "name" ) );
                    program.setProgramType( ProgramType.fromValue( rs.getString( "type" ) ) );
                    program.setPublicAccess( rs.getString( "publicaccess" ) );

                    programStages.add( toProgramStage( rs ) );

                    CategoryCombo categoryCombo = new CategoryCombo();
                    categoryCombo.setId( rs.getLong( "catcombo_id" ) );
                    categoryCombo.setUid( rs.getString( "catcombo_uid" ) );
                    categoryCombo.setName( rs.getString( "catcombo_name" ) );
                    program.setCategoryCombo( categoryCombo );

                    program.setProgramStages( programStages );
                    results.put( rs.getString( "uid" ), program );

                    programId = program.getId();
                }
                else
                {
                    results.get( rs.getString( "uid" ) ).getProgramStages().add( toProgramStage( rs ) );
                }
            }
            return results;
        } );
    }

    private ProgramStage toProgramStage( ResultSet rs )
        throws SQLException
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setId( rs.getLong( "ps_id" ) );
        programStage.setUid( rs.getString( "ps_uid" ) );
        programStage.setSortOrder( rs.getInt( "sort_order" ) );
        programStage.setPublicAccess( rs.getString( "ps_public_access" ) );
        programStage.setFeatureType(
            rs.getString( "ps_feature_type" ) != null ? FeatureType.getTypeFromName( rs.getString( "ps_feature_type" ) )
                : FeatureType.NONE );

        return programStage;
    }

    private OrganisationUnit toOrganisationUnit( ResultSet rs )
        throws SQLException
    {
        OrganisationUnit ou = new OrganisationUnit();
        ou.setUid( rs.getString( "uid" ) );
        return ou;
    }

    private UserAccess toUserAccess( ResultSet rs )
        throws SQLException
    {
        UserAccess userAccess = new UserAccess();
        userAccess.setId( rs.getInt( "useraccessid" ) );
        userAccess.setAccess( rs.getString( "access" ) );
        User user = new User();
        user.setId( rs.getLong( "userid" ) );
        user.setUid( rs.getString( "uid" ) );
        userAccess.setUser( user );
        return userAccess;
    }

    private UserGroupAccess toUserGroupAccess( ResultSet rs )
        throws SQLException
    {
        UserGroupAccess userGroupAccess = new UserGroupAccess();
        userGroupAccess.setId( rs.getInt( "usergroupaccessid" ) );
        userGroupAccess.setAccess( rs.getString( "access" ) );
        UserGroup userGroup = new UserGroup();
        userGroup.setId( rs.getLong( "usergroupid" ) );
        userGroupAccess.setUserGroup( userGroup );
        userGroup.setUid( rs.getString( "uid" ) );
        return userGroupAccess;
    }
}
