package org.hisp.dhis.dxf2.events.importer.context;

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
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.cache2k.integration.CacheLoader;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventUtils;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

/**
 * This supplier builds and caches a Map of all the Programs in the system.
 * For each Program, the following additional data is retrieved:
 *
 *
 *
 * @formatter:off
 *
         Program
         +
         |
         +---+ Program Stage (List)
         |           |
         |           +---+ User Access (ACL)
         |           |
         |           +---+ User Group Access (ACL)
         |
         |
         +---+ Category Combo
         |
         |
         +---+ Tracked Entity Instance
         |           |
         |           +---+ User Access (ACL)
         |           |
         |           +---+ User Group Access (ACL)
         |
         |
         |
         +---+ Organizational Unit (List)
         |
         +---+ User Access (ACL)
         |
         +---+ User Group Access (ACL)
 * @formatter:on
 *
 * @author Luciano Fiandesio
 */
@Slf4j
@Component( "workContextProgramsSupplier" )
public class ProgramSupplier extends AbstractSupplier<Map<String, Program>>
{
    private final static String PROGRAM_CACHE_KEY = "000P";

    private final ObjectMapper jsonMapper;

    private final static String ATTRIBUTESCHEME_COL = "attributevalues";

    // @formatter:off
    private final static String USER_ACCESS_SQL = "select eua.${column_name}, eua.useraccessid, ua.useraccessid, ua.access, ua.userid, ui.uid " +
        "from ${table_name} eua " +
        "join useraccess ua on eua.useraccessid = ua.useraccessid " +
        "join userinfo ui on ui.userinfoid = ua.useraccessid " +
        "order by eua.${column_name}";

    private final static String USER_GROUP_ACCESS_SQL = "select ega.${column_name}, ega.usergroupaccessid, u.access, u.usergroupid, ug.uid " +
        "from ${table_name} ega " +
        "join usergroupaccess u on ega.usergroupaccessid = u.usergroupaccessid " +
        "join usergroup ug on u.usergroupid = ug.usergroupid " +
        "order by ega.${column_name}";

    // Caches the entire Program hierarchy, including Program Stages and ACL data
    private final Cache<String, Map<String, Program>> programsCache = new Cache2kBuilder<String, Map<String, Program>>() {}
        .name( "eventImportProgramCache" + RandomStringUtils.randomAlphabetic(5) )
        .expireAfterWrite( 1, TimeUnit.MINUTES )
        .build();

    // Caches the User Groups and the Users belonging to each group
    private final Cache<Long, Set<User>> userGroupCache = new Cache2kBuilder<Long, Set<User>>() {}
        .name( "eventImportUserGroupCache" + RandomStringUtils.randomAlphabetic(5) )
        .expireAfterWrite( 5, TimeUnit.MINUTES )
        .permitNullValues( true )
        .loader( new CacheLoader<Long, Set<User>>()
        {
            @Override
            public Set<User> load( Long userGroupId ) {
                return loadUserGroups( userGroupId );
            }
        } ).build() ;

    // @formatter:on

    public ProgramSupplier( NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper jsonMapper )
    {
        super( jdbcTemplate );
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Map<String, Program> get( ImportOptions importOptions, List<Event> eventList )
    {
        boolean requiresReload = false;
        //
        // do not use cache is `skipCache` is true
        //
        if ( importOptions.isSkipCache() )
        {
            programsCache.removeAll();
            userGroupCache.removeAll();
        }
        Map<String, Program> programMap = programsCache.get( PROGRAM_CACHE_KEY );

        //
        // Check if the list of incoming Events contains one or more Program uid which
        // is
        // not in cache. Reload the entire program cache if a Program UID is not found
        //
        if ( programMap != null )
        {
            Set<String> programs = eventList.stream().map( Event::getProgram ).collect( Collectors.toSet() );

            final Set<String> programsInCache = programMap.keySet();

            for ( String program : programs )
            {
                if ( !programsInCache.contains( program ) )
                {
                    // invalidate cache and rebuild
                    programsCache.removeAll();
                    requiresReload = true;
                    break;
                }
            }
        }

        if ( requiresReload || programMap == null )
        {
            //
            // Load all the Programs
            //
            programMap = loadPrograms( importOptions.getIdSchemes() );

            //
            // Load all mandatory DataElements for each Program Stage
            //
            Map<Long, Set<DataElement>> dataElementMandatoryMap = loadProgramStageDataElementMandatoryMap();

            //
            // Load a Map of OrgUnits belonging to a Program (key: program id, value: Set of OrgUnits)
            //
            Map<Long, Set<OrganisationUnit>> ouMap = loadOrgUnits();

            //
            // Load User Access data for all the Programs (required for ACL checks)
            //
            Map<Long, Set<UserAccess>> programUserAccessMap = loadUserAccessesForPrograms();
            Map<Long, Set<UserAccess>> programStageUserAccessMap = loadUserAccessesForProgramStages();
            Map<Long, Set<UserAccess>> tetUserAccessMap = loadUserAccessesForTrackedEntityTypes();

            //
            // Load User Group Access data for all the Programs (required for ACL checks)
            //
            Map<Long, Set<UserGroupAccess>> programUserGroupAccessMap = loadGroupUserAccessesForPrograms();
            Map<Long, Set<UserGroupAccess>> programStageUserGroupAccessMap = loadGroupUserAccessesForProgramStages();
            Map<Long, Set<UserGroupAccess>> tetUserGroupAccessMap = loadGroupUserAccessesForTrackedEntityTypes();

            for ( Program program : programMap.values() )
            {
                program.setOrganisationUnits( ouMap.getOrDefault( program.getId(), new HashSet<>() ) );
                program.setUserAccesses( programUserAccessMap.getOrDefault( program.getId(), new HashSet<>() ) );
                program
                    .setUserGroupAccesses( programUserGroupAccessMap.getOrDefault( program.getId(), new HashSet<>() ) );
                TrackedEntityType trackedEntityType = program.getTrackedEntityType();
                if ( trackedEntityType != null )
                {
                    trackedEntityType
                        .setUserAccesses( tetUserAccessMap.getOrDefault( trackedEntityType.getId(), new HashSet<>() ) );
                    trackedEntityType.setUserGroupAccesses(
                        tetUserGroupAccessMap.getOrDefault( trackedEntityType.getId(), new HashSet<>() ) );
                }

                for ( ProgramStage programStage : program.getProgramStages() )
                {
                    programStage.setUserAccesses(
                        programStageUserAccessMap.getOrDefault( programStage.getId(), new HashSet<>() ) );
                    programStage.setUserGroupAccesses(
                        programStageUserGroupAccessMap.getOrDefault( programStage.getId(), new HashSet<>() ) );

                    Set<DataElement> dataElements = dataElementMandatoryMap.get( programStage.getId() );
                    if ( dataElements != null )
                    {
                        programStage.setProgramStageDataElements( dataElements.stream()
                            .map( de -> new ProgramStageDataElement( programStage, de ) )
                            .collect( Collectors.toSet() ) );
                    }
                }
            }

            programsCache.put( PROGRAM_CACHE_KEY, programMap );
        }

        return programMap;
    }

    private Map<Long, Set<OrganisationUnit>> loadOrgUnits()
    {
        final String sql = "select p.programid, ou.organisationunitid, ou.uid, ou.code, ou.name, ou.attributevalues "
            + "from program_organisationunits p "
            + "join organisationunit ou on p.organisationunitid = ou.organisationunitid order by programid";

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
        return fetchUserAccesses( replaceAclQuery( USER_ACCESS_SQL, "programuseraccesses", "programid" ), "programid" );
    }

    private Map<Long, Set<UserAccess>> loadUserAccessesForProgramStages()
    {
        return fetchUserAccesses( replaceAclQuery( USER_ACCESS_SQL, "programstageuseraccesses", "programstageid" ),
            "programstageid" );
    }

    private Map<Long, Set<UserAccess>> loadUserAccessesForTrackedEntityTypes()
    {
        return fetchUserAccesses(
            replaceAclQuery( USER_ACCESS_SQL, "trackedentitytypeuseraccesses", "trackedentitytypeid" ),
            "trackedentitytypeid" );
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
        return fetchUserGroupAccess( replaceAclQuery( USER_GROUP_ACCESS_SQL, "programusergroupaccesses", "programid" ),
            "programid" );
    }

    private Map<Long, Set<UserGroupAccess>> loadGroupUserAccessesForProgramStages()
    {
        // TODO: can't use replace because the table programstageusergroupaccesses
        // should use 'programstageid' as column name
        final String sql = "select psuga.programid as programstageid, psuga.usergroupaccessid, u.access, u.usergroupid, ug.uid "
            + "from programstageusergroupaccesses psuga "
            + "join usergroupaccess u on psuga.usergroupaccessid = u.usergroupaccessid "
            + "join usergroup ug on u.usergroupid = ug.usergroupid order by programstageid";

        return fetchUserGroupAccess( sql, "programstageid" );
    }

    private Map<Long, Set<UserGroupAccess>> loadGroupUserAccessesForTrackedEntityTypes()
    {
        return fetchUserGroupAccess(
            replaceAclQuery( USER_GROUP_ACCESS_SQL, "trackedentitytypeusergroupaccesses", "trackedentitytypeid" ),
            "trackedentitytypeid" );
    }

    private Map<Long, Set<UserGroupAccess>> fetchUserGroupAccess( String sql, String column )
    {
        return jdbcTemplate.query( sql, ( ResultSet rs ) -> {
            Map<Long, Set<UserGroupAccess>> results = new HashMap<>();
            long entityId = 0;
            while ( rs.next() )
            {
                if ( entityId != rs.getLong( column ) )
                {
                    Set<UserGroupAccess> aclSet = new HashSet<>();
                    aclSet.add( toUserGroupAccess( rs ) );
                    results.put( rs.getLong( column ), aclSet );

                    entityId = rs.getLong( column );
                }
                else
                {
                    results.get( rs.getLong( column ) ).add( toUserGroupAccess( rs ) );
                }
            }
            return results;
        } );
    }

    private Map<Long, Set<DataElement>> loadProgramStageDataElementMandatoryMap()
    {
        final String sql = "select psde.programstageid, de.dataelementid, de.uid as de_uid, de.code as de_code "
            + "from programstagedataelement psde "
            + "         join dataelement de on psde.dataelementid = de.dataelementid " + "where psde.compulsory = true "
            + "order by psde.programstageid";

        return jdbcTemplate.query( sql, ( ResultSet rs ) -> {

            Map<Long, Set<DataElement>> results = new HashMap<>();
            long programStageId = 0;
            while ( rs.next() )
            {
                if ( programStageId != rs.getLong( "programstageid" ) )
                {
                    Set<DataElement> dataElements = new HashSet<>();
                    DataElement dataElement = new DataElement();
                    dataElement.setId( rs.getLong( "dataelementid" ) );
                    dataElement.setUid( rs.getString( "de_uid" ) );
                    dataElement.setCode( rs.getString( "de_code" ) );
                    dataElements.add( dataElement );
                    results.put( rs.getLong( "programstageid" ), dataElements );
                }
                else
                {
                    DataElement dataElement = new DataElement();
                    dataElement.setId( rs.getLong( "dataelementid" ) );
                    dataElement.setUid( rs.getString( "de_uid" ) );
                    dataElement.setCode( rs.getString( "de_code" ) );

                    results.get( rs.getLong( "programstageid" ) ).add( dataElement );
                }
            }
            return results;
        } );
    }

    private Map<String, Program> loadPrograms( IdSchemes idSchemes )
    {
        //
        // Get the IdScheme for Programs. Programs should support also the Attribute
        // Scheme, based on JSONB
        //
        IdScheme idScheme = idSchemes.getProgramIdScheme();

        String sqlSelect = "select p.programid as id, p.uid, p.code, p.name, p.publicaccess, "
            + "p.type, tet.trackedentitytypeid, tet.publicaccess  as tet_public_access, "
            + "tet.uid           as tet_uid, c.categorycomboid as catcombo_id, "
            + "c.uid             as catcombo_uid, c.name            as catcombo_name, "
            + "c.code            as catcombo_code, ps.programstageid as ps_id, ps.uid as ps_uid, "
            + "ps.code           as ps_code, ps.name           as ps_name, "
            + "ps.featuretype    as ps_feature_type, ps.sort_order, ps.publicaccess   as ps_public_access, "
            + "ps.repeatable     as ps_repeatable, ps.enableuserassignment, ps.validationstrategy";

        if ( idScheme.isAttribute() )
        {
            sqlSelect += ",p.attributevalues->'" + idScheme.getAttribute()
                + "'->>'value' as " + ATTRIBUTESCHEME_COL;
        }

        final String sql = sqlSelect + " from program p "
            + "LEFT JOIN categorycombo c on p.categorycomboid = c.categorycomboid "
            + "LEFT JOIN trackedentitytype tet on p.trackedentitytypeid = tet.trackedentitytypeid "
            + "LEFT JOIN programstage ps on p.programid = ps.programid "
            + "LEFT JOIN program_organisationunits pou on p.programid = pou.programid "
            + "LEFT JOIN organisationunit ou on pou.organisationunitid = ou.organisationunitid "
            + "group by p.programid, tet.trackedentitytypeid, c.categorycomboid, ps.programstageid, ps.sort_order "
            + "order by p.programid, ps.sort_order";

        return jdbcTemplate.query( sql, ( ResultSet rs ) -> {
            Map<String, Program> results = new HashMap<>();
            long programId = 0;
            while ( rs.next() )
            {
                if ( programId != rs.getLong( "id" ) )
                {
                    Set<ProgramStage> programStages = new HashSet<>();
                    Program program = new Program();
                    // identifiers
                    program.setId( rs.getLong( "id" ) );
                    program.setUid( rs.getString( "uid" ) );
                    program.setName( rs.getString( "name" ) );
                    program.setCode( rs.getString( "code" ) );

                    program.setProgramType( ProgramType.fromValue( rs.getString( "type" ) ) );
                    program.setPublicAccess( rs.getString( "publicaccess" ) );
                    // Do not add program stages without primary key (this should not really happen,
                    // but the database does allow Program Stage without a Program Id
                    if ( rs.getLong( "ps_id" ) != 0 )
                    {
                        programStages.add( toProgramStage( rs ) );
                    }

                    CategoryCombo categoryCombo = new CategoryCombo();
                    categoryCombo.setId( rs.getLong( "catcombo_id" ) );
                    categoryCombo.setUid( rs.getString( "catcombo_uid" ) );
                    categoryCombo.setName( rs.getString( "catcombo_name" ) );
                    categoryCombo.setCode( rs.getString( "catcombo_code" ) );
                    program.setCategoryCombo( categoryCombo );

                    long tetId = rs.getLong( "trackedentitytypeid" );
                    if ( tetId != 0 )
                    {
                        TrackedEntityType trackedEntityType = new TrackedEntityType();
                        trackedEntityType.setId( tetId );
                        trackedEntityType.setUid( rs.getString( "tet_uid" ) );
                        trackedEntityType.setPublicAccess( rs.getString( "tet_public_access" ) );
                        program.setTrackedEntityType( trackedEntityType );
                    }

                    program.setProgramStages( programStages );
                    results.put( getProgramKey( idScheme, rs ), program );

                    programId = program.getId();
                }
                else
                {
                    results.get( getProgramKey( idScheme, rs ) ).getProgramStages()
                        .add( toProgramStage( rs ) );
                }
            }
            return results;
        } );
    }

    /**
     * Resolve the key to place in the Program Map, based on the Scheme specified in
     * the request If the scheme is of type Attribute, use the attribute value from
     * the JSONB column
     */
    private String getProgramKey( IdScheme programIdScheme, ResultSet rs )
        throws SQLException
    {
        return programIdScheme.isAttribute() ? rs.getString( ATTRIBUTESCHEME_COL )
            : IdSchemeUtils.getKey( programIdScheme, rs );
    }

    private ProgramStage toProgramStage( ResultSet rs )
        throws SQLException
    {
        ProgramStage programStage = new ProgramStage();
        programStage.setId( rs.getLong( "ps_id" ) );
        programStage.setUid( rs.getString( "ps_uid" ) );
        programStage.setCode( rs.getString( "ps_code" ) );
        programStage.setName( rs.getString( "ps_name" ) );
        programStage.setSortOrder( rs.getInt( "sort_order" ) );
        programStage.setPublicAccess( rs.getString( "ps_public_access" ) );
        programStage.setFeatureType(
            rs.getString( "ps_feature_type" ) != null ? FeatureType.valueOf( rs.getString( "ps_feature_type" ) )
                : FeatureType.NONE );
        programStage.setRepeatable( rs.getBoolean( "ps_repeatable" ) );
        programStage.setEnableUserAssignment( rs.getBoolean( "enableuserassignment" ) );
        String validationStrategy = rs.getString( "validationstrategy" );
        if ( StringUtils.isNotEmpty( validationStrategy ) )
        {
            programStage.setValidationStrategy( ValidationStrategy.valueOf( validationStrategy ) );
        }
        return programStage;
    }

    private OrganisationUnit toOrganisationUnit( ResultSet rs )
        throws SQLException
    {
        OrganisationUnit ou = new OrganisationUnit();
        ou.setUid( rs.getString( "uid" ) );
        ou.setId( rs.getLong( "organisationunitid" ) );
        ou.setName( rs.getString( "name" ) );
        ou.setCode( rs.getString( "code" ) );

        final String attributeValueJson = rs.getString( "attributevalues" );

        if ( StringUtils.isNotEmpty( attributeValueJson ) && !attributeValueJson.equals( "{}" ) )
        {
            try
            {
                ou.setAttributeValues( EventUtils.getAttributeValues( jsonMapper, rs.getObject( "attributevalues" ) ) );
            }
            catch ( JsonProcessingException e )
            {
                log.error( "An error occurred when processing an Organisation Unit's [id=" + ou.getId()
                    + "] attribute values", e );
            }
        }

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
        // TODO This is not very efficient for large DHIS2 installations:
        // it would be better to run a direct query in the Access Layer
        // to determine if the user belongs to the group
        userGroup.setMembers( userGroupCache.get( userGroup.getId() ) );
        return userGroupAccess;
    }

    private Set<User> loadUserGroups( Long userGroupId )
    {
        final String sql = "select ug.uid, ug.usergroupid, ui.uid user_uid, ui.userinfoid user_id from usergroupmembers ugm "
            + "join usergroup ug on ugm.usergroupid = ug.usergroupid join userinfo ui on ugm.userid = ui.userinfoid where ug.usergroupid = "
            + userGroupId;

        return jdbcTemplate.query( sql, ( ResultSet rs ) -> {

            Set<User> users = new HashSet<>();
            while ( rs.next() )
            {
                User user = new User();
                user.setUid( rs.getString( "user_uid" ) );
                user.setId( rs.getLong( "user_id" ) );

                users.add( user );
            }

            return users;
        } );
    }

    private String replaceAclQuery( String sql, String tableName, String column )
    {
        // @formatter:off
        StrSubstitutor sub = new StrSubstitutor( ImmutableMap.<String, String>builder()
                .put( "table_name", tableName)
                .put( "column_name", column )
                .build() );
        // @formatter:on
        return sub.replace( sql );
    }
}
