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
package org.hisp.dhis.association;

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.CHECK_USER_ACCESS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.CHECK_USER_GROUPS_ACCESS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.EXTRACT_PATH_TEXT;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.HAS_USER_GROUP_IDS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.HAS_USER_ID;
import static org.hisp.dhis.security.acl.AclService.LIKE_READ_METADATA;
import static org.hisp.dhis.system.util.SqlUtils.singleQuote;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.user.CurrentUserGroupInfo;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;

@RequiredArgsConstructor
public abstract class AbstractOrganisationUnitAssociationsQueryBuilder
{

    private final CurrentUserService currentUserService;

    private static final String SHARING_OUTER_QUERY_BEGIN = "select " +
        "    inner_query_alias.uid, " +
        "    inner_query_alias.agg_ou_uid " +
        "from (";

    private static final String SHARING_OUTER_QUERY_END = ") as inner_query_alias";

    private static final String REL_TABLE_ALIAS = "relationship_table_alias";

    private static final String T_ALIAS = "base_table_alias";

    private static final String INNER_QUERY_GROUPING_BY = "group by " + T_ALIAS + ".uid, " + T_ALIAS
        + ".sharing";

    private String getInnerQuerySql()
    {
        return "select " +
            T_ALIAS + ".uid, " +
            T_ALIAS + ".sharing, " +
            "array_agg(ou.uid) agg_ou_uid " +
            "from " + getBaseTableName() + " " + T_ALIAS +
            " left join " + getRelationshipTableName() + " " + REL_TABLE_ALIAS +
            " on " + T_ALIAS + "." + getJoinColumnName() + " = " + REL_TABLE_ALIAS + "." + getJoinColumnName() +
            " left join organisationunit ou " +
            " on " + REL_TABLE_ALIAS + ".organisationunitid = ou.organisationunitid " +
            "where";
    }

    protected abstract String getRelationshipTableName();

    protected abstract String getJoinColumnName();

    protected abstract String getBaseTableName();

    public String buildSqlQuery( Set<String> uids, Set<String> userOrgUnitPaths, User currentUser )
    {
        Stream<String> queryParts = Stream.of(
            SHARING_OUTER_QUERY_BEGIN,
            innerQueryProvider( uids, userOrgUnitPaths, currentUser ),
            SHARING_OUTER_QUERY_END );

        if ( nonSuperUser( currentUser ) )
        {
            queryParts = Stream.concat(
                queryParts,
                Stream.of(
                    "where",
                    getSharingConditions( LIKE_READ_METADATA ) ) );
        }
        return queryParts.collect( joining( " " ) );
    }

    public String buildSqlQueryForRawAssociation( Set<String> uids )
    {
        Stream<String> queryParts = Stream.of(
            SHARING_OUTER_QUERY_BEGIN,
            innerQueryProvider( uids, null, null ),
            SHARING_OUTER_QUERY_END );

        return queryParts.collect( joining( " " ) );
    }

    private String innerQueryProvider( Set<String> uids, Set<String> userOrgUnitPaths, User currentUser )
    {
        Stream<String> queryParts = Stream.of(
            getInnerQuerySql(),
            getUidsFilter( uids ) );

        if ( nonSuperUser( currentUser ) )
        {
            queryParts = Stream.concat( queryParts,
                Stream.of(
                    "and",
                    getUserOrgUnitPathsFilter( userOrgUnitPaths ) ) );
        }

        queryParts = Stream.concat( queryParts, Stream.of( INNER_QUERY_GROUPING_BY ) );

        return queryParts.collect( joining( " " ) );
    }

    private String getSharingConditions( String access )
    {
        CurrentUserGroupInfo currentUserGroupInfo = currentUserService.getCurrentUserGroupsInfo();
        return String.join( " or ",
            getOwnerCondition( currentUserGroupInfo ),
            getPublicSharingCondition( access ),
            getUserGroupAccessCondition( currentUserGroupInfo, access ),
            getUserAccessCondition( currentUserGroupInfo, access ) );
    }

    private String getOwnerCondition( CurrentUserGroupInfo currentUserGroupInfo )
    {
        return String.join( " or ",
            jsonbFunction( EXTRACT_PATH_TEXT, "owner" ) + " = " + singleQuote( currentUserGroupInfo.getUserUID() ),
            jsonbFunction( EXTRACT_PATH_TEXT, "owner" ) + " is null" );
    }

    private String getPublicSharingCondition( String access )
    {
        return String.join( " or ",
            jsonbFunction( EXTRACT_PATH_TEXT, "public" ) + " like " + singleQuote( access ),
            jsonbFunction( EXTRACT_PATH_TEXT, "public" ) + " is null" );
    }

    private String getUserAccessCondition( CurrentUserGroupInfo currentUserGroupInfo, String access )
    {
        String userUid = currentUserGroupInfo.getUserUID();
        return Stream.of(
            jsonbFunction( HAS_USER_ID, userUid ),
            jsonbFunction( CHECK_USER_ACCESS, userUid, access ) )
            .collect( joining( " and ", "(", ")" ) );
    }

    private String getUserGroupAccessCondition( CurrentUserGroupInfo currentUserGroupInfo, String access )
    {
        if ( CollectionUtils.isEmpty( currentUserGroupInfo.getUserGroupUIDs() ) )
        {
            return "1=0";
        }
        String groupUids = "{" + String.join( ",", currentUserGroupInfo.getUserGroupUIDs() ) + "}";
        return Stream.of(
            jsonbFunction( HAS_USER_GROUP_IDS, groupUids ),
            jsonbFunction( CHECK_USER_GROUPS_ACCESS, access, groupUids ) )
            .collect( joining( " and ", "(", ")" ) );
    }

    private String jsonbFunction( String functionName, String... params )
    {
        return String.join( "",
            functionName,
            "(",
            String.join( ",", "inner_query_alias.sharing",
                Arrays.stream( params )
                    .map( SqlUtils::singleQuote )
                    .collect( joining( "," ) ) ),
            ")" );
    }

    private boolean nonSuperUser( User currentUser )
    {
        return Objects.nonNull( currentUser ) && !currentUser.isSuper();
    }

    private String getUidsFilter( Set<String> uids )
    {
        return T_ALIAS + ".uid in (" +
            uids.stream()
                .map( SqlUtils::singleQuote )
                .collect( joining( "," ) )
            + ")";
    }

    private String getUserOrgUnitPathsFilter( Set<String> userOrgUnitPaths )
    {
        return Stream.concat(
            Stream.of( "ou.organisationUnitId is null" ),
            userOrgUnitPaths.stream()
                .map( userOrgUnitPath -> "ou.path like '" + userOrgUnitPath + "%'" ) )
            .collect( joining( " or ", "(", ")" ) );
    }

}
