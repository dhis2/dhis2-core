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
package org.hisp.dhis.dataitem.query.shared;

import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasNonBlankStringPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.USER_GROUP_UIDS;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.USER_UID;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_AND;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_OR;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.CHECK_USER_ACCESS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.CHECK_USER_GROUPS_ACCESS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.EXTRACT_PATH_TEXT;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.HAS_USER_GROUP_IDS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.HAS_USER_ID;
import static org.springframework.util.Assert.hasText;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * This class held common user access SQL statements for data items.
 *
 * @author maikel arabori
 */
public class UserAccessStatement
{
    public static final String READ_ACCESS = "r%";

    private UserAccessStatement()
    {
    }

    /**
     * Creates a sharing statement for the given column and on the paramsMap. It
     * will also take consideration user groups if this is set in the paramsMap.
     * This statement will check sharing conditions for Metadata ONLY.
     *
     * @param column the sharing column
     * @param paramsMap the parameters map
     * @param access the access condition we are checking for. ie.: r%, for read
     *        only access
     * @return the sharing SQL statement for the current user
     */
    public static String sharingConditions( final String column, final String access,
        final MapSqlParameterSource paramsMap )
    {
        final StringBuilder conditions = new StringBuilder();

        conditions
            .append( " (" ) // Isolator

            .append( " ( " ) // Grouping clauses
            .append( publicAccessCondition( column, access ) )
            .append( SPACED_OR )
            .append( ownerAccessCondition( column ) )
            .append( SPACED_OR )
            .append( userAccessCondition( column, access ) )
            .append( " ) " ); // Grouping clauses closing

        if ( hasNonBlankStringPresence( paramsMap, USER_GROUP_UIDS ) )
        {
            conditions.append( " or (" + userGroupAccessCondition( column, access ) + ")" );
        }

        conditions.append( ")" ); // Isolator closing

        return conditions.toString();
    }

    /**
     * Creates a sharing statement for the given column that checks if given
     * sharing settings present in the column belongs to the current logged
     * user.
     *
     * @param column the sharing column
     * @return the sharing SQL statement for the current user
     */
    public static String checkOwnerConditions( String column )
    {
        return "(" + EXTRACT_PATH_TEXT + "(" + column + ", 'owner') = :userUid)";
    }

    /**
     * Creates a sharing statement for the given columns, based on the
     * paramsMap. It will also take consideration user groups if this is set in
     * the paramsMap. This statement will check sharing conditions for Metadata
     * ONLY.
     *
     * @param columnOne a sharing column
     * @param columnTwo the other sharing column
     * @param access the access condition we are checking for. ie.: r%, for read
     *        only access
     * @param paramsMap the parameters map
     * @return the sharing SQL statement for the current user
     */
    public static String sharingConditions( final String columnOne, final String columnTwo,
        final String access, final MapSqlParameterSource paramsMap )
    {
        final StringBuilder conditions = new StringBuilder();

        conditions
            .append( " (" ) // Isolator

            .append( " ( " ) // Grouping clauses
            .append( "(" ) // Table 1 conditions
            .append( publicAccessCondition( columnOne, access ) )
            .append( SPACED_OR )
            .append( ownerAccessCondition( columnOne ) )
            .append( SPACED_OR )
            .append( userAccessCondition( columnOne, access ) )
            .append( ")" ) // Table 1 conditions end
            .append( " and (" ) // Table 2 conditions
            .append( publicAccessCondition( columnTwo, access ) )
            .append( SPACED_OR )
            .append( ownerAccessCondition( columnTwo ) )
            .append( SPACED_OR )
            .append( userAccessCondition( columnTwo, access ) )
            .append( ")" ) // Table 2 conditions end
            .append( " )" ); // Grouping clauses closing

        if ( hasNonBlankStringPresence( paramsMap, USER_GROUP_UIDS ) )
        {
            conditions.append( " or (" );

            // Program group access checks
            conditions.append( userGroupAccessCondition( columnOne, access ) );

            // DataElement access checks
            conditions.append( SPACED_AND + userGroupAccessCondition( columnTwo, access ) );

            // Closing OR condition
            conditions.append( ")" );
        }

        conditions.append( ")" ); // Isolator closing

        return conditions.toString();
    }

    static String ownerAccessCondition( final String column )
    {
        assertTableAlias( column );

        return "(" + EXTRACT_PATH_TEXT + "(" + column + ", 'owner') is null or "
            + EXTRACT_PATH_TEXT + "(" + column + ", 'owner') = 'null' or "
            + EXTRACT_PATH_TEXT + "(" + column + ", 'owner') = :userUid)";
    }

    static String publicAccessCondition( final String column, final String access )
    {
        assertTableAlias( column );

        return "(" + EXTRACT_PATH_TEXT + "(" + column + ", 'public') is null or "
            + EXTRACT_PATH_TEXT + "(" + column + ", 'public') = 'null' or "
            + EXTRACT_PATH_TEXT + "(" + column + ", 'public') like '" + access + "')";
    }

    static String userAccessCondition( final String tableName, final String access )
    {
        assertTableAlias( tableName );

        return "(" + HAS_USER_ID + "(" + tableName + ", :" + USER_UID + ") = true "
            + SPACED_AND + CHECK_USER_ACCESS + "(" + tableName + ", :" + USER_UID + ", '" + access + "') = true)";
    }

    static String userGroupAccessCondition( final String column, final String access )
    {
        assertTableAlias( column );

        return "(" + HAS_USER_GROUP_IDS + "(" + column + ", :" + USER_GROUP_UIDS + ") = true " +
            SPACED_AND + CHECK_USER_GROUPS_ACCESS + "(" + column + ", '" + access + "', :" + USER_GROUP_UIDS
            + ") = true)";
    }

    private static void assertTableAlias( String columnName )
    {
        hasText( columnName, "The argument columnName cannot be null/blank." );
    }
}
