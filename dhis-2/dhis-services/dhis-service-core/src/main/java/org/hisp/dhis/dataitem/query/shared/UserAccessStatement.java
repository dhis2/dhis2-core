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
package org.hisp.dhis.dataitem.query.shared;

import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasNonBlankStringPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.USER_GROUP_UIDS;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.USER_ID;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_AND;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_OR;
import static org.springframework.util.Assert.hasText;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * This class held common user access SQL statements for data items.
 *
 * @author maikel arabori
 */
public class UserAccessStatement
{
    private static final String READ_ACCESS = "'r%'";

    private static final String USER_ACCESSES = "useraccesses";

    private static final String USER_GROUP_ACCESSES = "usergroupaccesses";

    private UserAccessStatement()
    {
    }

    /**
     * Creates a sharing statement for the given table and paramsMap. It will
     * also take consideration user groups if this is set in the paramsMap. This
     * statement will check sharing conditions for Metadata ONLY.
     *
     * @param table the table to check for access
     * @param paramsMap the parameters map
     * @return the sharing SQL statement for the current user
     */
    public static String sharingConditions( final String table, final MapSqlParameterSource paramsMap )
    {
        final StringBuilder conditions = new StringBuilder();

        conditions
            .append( " (" ) // Isolator

            .append( " ( " ) // Grouping clauses
            .append( publicAccessCondition( table ) )
            .append( SPACED_OR )
            .append( userAccessCondition( table, table + "id", table + USER_ACCESSES ) )
            .append( " ) " ); // Grouping clauses closing

        if ( hasNonBlankStringPresence( paramsMap, USER_GROUP_UIDS ) )
        {
            conditions.append( " or (" + userGroupAccessCondition( table, table + "id",
                table + USER_GROUP_ACCESSES ) + ")" );
        }

        conditions.append( ")" ); // Isolator closing

        return conditions.toString();
    }

    /**
     * Creates a sharing statement for the given tables and paramsMap. It will
     * also take consideration user groups if this is set in the paramsMap. This
     * statement will check sharing conditions for Metadata ONLY.
     *
     * @param tableOne the first table to check for access
     * @param tableTwo the second table to check for access
     * @param paramsMap the parameters map
     * @return the sharing SQL statement for the current user
     */
    public static String sharingConditions( final String tableOne, final String tableTwo,
        final MapSqlParameterSource paramsMap )
    {
        final StringBuilder conditions = new StringBuilder();

        conditions
            .append( " (" ) // Isolator

            .append( " ( " ) // Grouping clauses
            .append( "(" ) // Table 1 conditions
            .append( publicAccessCondition( tableOne ) )
            .append( SPACED_OR )
            .append( userAccessCondition( tableOne, tableOne + "id", tableOne + USER_ACCESSES ) )
            .append( ")" ) // Table 1 conditions end
            .append( " and (" ) // Table 2 conditions
            .append( publicAccessCondition( tableTwo ) )
            .append( SPACED_OR )
            .append( userAccessCondition( tableTwo, tableTwo + "id", tableTwo + USER_ACCESSES ) )
            .append( ")" ) // Table 2 conditions end
            .append( " )" ); // Grouping clauses closing

        if ( hasNonBlankStringPresence( paramsMap, USER_GROUP_UIDS ) )
        {
            conditions.append( " or (" );

            // Table one group access checks
            conditions.append( userGroupAccessCondition( tableOne, tableOne + "id",
                tableOne + USER_GROUP_ACCESSES ) );

            // Table two access checks
            conditions.append( SPACED_AND + userGroupAccessCondition( tableTwo, tableTwo + "id",
                tableTwo + USER_GROUP_ACCESSES ) );

            // Closing OR condition
            conditions.append( ")" );
        }

        conditions.append( ")" ); // Isolator closing

        return conditions.toString();
    }

    static String publicAccessCondition( final String table )
    {
        assertTableName( table );

        final StringBuilder condition = new StringBuilder();
        condition.append(
            "(t.item_publicaccess like " + READ_ACCESS + " or t.item_publicaccess is null) " );

        return condition.toString();
    }

    static String userAccessCondition( final String table, final String column, final String userAccessTable )
    {
        assertTableName( table );
        assertColumnName( column );

        final StringBuilder condition = new StringBuilder();
        condition.append( " t.item_id IN (" )
            .append( " SELECT " + userAccessTable + "." + column + " FROM " + userAccessTable )
            .append( " WHERE " + userAccessTable + ".useraccessid IN (SELECT useraccessid FROM useraccess" )
            .append( " WHERE access LIKE " + READ_ACCESS + " AND useraccess.userid = :" + USER_ID + "))" );

        return condition.toString();
    }

    static String userGroupAccessCondition( final String table, final String column,
        final String userGroupAccessTable )
    {
        assertTableName( table );
        assertColumnName( column );

        final StringBuilder condition = new StringBuilder();
        condition
            .append( " t.item_id IN (SELECT " + userGroupAccessTable + "." + column + " FROM " + userGroupAccessTable )
            .append( " WHERE " + userGroupAccessTable
                + ".usergroupaccessid IN (SELECT usergroupaccessid FROM usergroupaccess WHERE" )
            .append( " access LIKE " + READ_ACCESS
                + " AND usergroupid IN (SELECT usergroupid FROM usergroupmembers WHERE userid = :"
                + USER_ID + ")))" );

        return condition.toString();
    }

    private static void assertTableName( String tableName )
    {
        hasText( tableName, "The argument table cannot be null/blank." );
    }

    private static void assertColumnName( String columnName )
    {
        hasText( columnName, "The argument column cannot be null/blank." );
    }
}
