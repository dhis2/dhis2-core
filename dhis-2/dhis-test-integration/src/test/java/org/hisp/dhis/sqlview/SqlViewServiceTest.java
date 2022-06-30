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
package org.hisp.dhis.sqlview;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SqlViewServiceTest extends TransactionalIntegrationTest
{
    @Autowired
    private SqlViewService sqlViewService;

    @Autowired
    private UserService internalUserService;

    private String sqlA = "SELECT   *  FROM     _categorystructure;;  ; ;;;  ;; ; ";

    private String sqlB = "SELECT COUNT(*) from organisationunit;";

    private String sqlC = "SELECT COUNT(_cocn.*) AS so_dem, _icgss.indicatorid AS in_id"
        + "FROM _indicatorgroupsetstructure AS _icgss, categoryoptioncombo AS _cocn "
        + "GROUP BY _icgss.indicatorid;";

    private String sqlD = "SELECT de.name, dv.sourceid, dv.value, p.startdate "
        + "FROM dataelement AS de, datavalue AS dv, period AS p " + "WHERE de.dataelementid=dv.dataelementid "
        + "AND dv.periodid=p.periodid LIMIT 10";

    private String sqlE = "WITH foo as (SELECT * FROM organisationunit) SELECT * FROM foo LIMIT 2; ";

    @Override
    public void setUpTest()
    {
        super.userService = internalUserService;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void assertEq( char uniqueCharacter, SqlView sqlView, String sql )
    {
        assertEquals( "SqlView" + uniqueCharacter, sqlView.getName() );
        assertEquals( "Description" + uniqueCharacter, sqlView.getDescription() );
        assertEquals( sql, sqlView.getSqlQuery() );
    }

    // -------------------------------------------------------------------------
    // SqlView
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------

    @Test
    void testAddSqlView()
    {
        SqlView sqlViewA = createSqlView( 'A', sqlA );
        SqlView sqlViewB = createSqlView( 'B', sqlB );
        SqlView sqlViewE = createSqlView( 'E', sqlE );

        long idA = sqlViewService.saveSqlView( sqlViewA );
        long idB = sqlViewService.saveSqlView( sqlViewB );
        long idE = sqlViewService.saveSqlView( sqlViewE );

        sqlViewA = sqlViewService.getSqlView( idA );
        sqlViewB = sqlViewService.getSqlView( idB );
        sqlViewE = sqlViewService.getSqlView( idE );

        assertEquals( idA, sqlViewA.getId() );
        assertEq( 'A', sqlViewA, sqlA );

        assertEquals( idB, sqlViewB.getId() );
        assertEq( 'B', sqlViewB, sqlB );

        assertEquals( idE, sqlViewE.getId() );
        assertEq( 'E', sqlViewE, sqlE );
    }

    @Test
    void testUpdateSqlView()
    {
        SqlView sqlView = createSqlView( 'A', sqlA );

        long id = sqlViewService.saveSqlView( sqlView );

        sqlView = sqlViewService.getSqlView( id );

        assertEq( 'A', sqlView, sqlA );

        sqlView.setName( "SqlViewC" );

        sqlViewService.updateSqlView( sqlView );
    }

    @Test
    void testGetAndDeleteSqlView()
    {
        SqlView sqlViewA = createSqlView( 'A', sqlC );
        SqlView sqlViewB = createSqlView( 'B', sqlD );

        long idA = sqlViewService.saveSqlView( sqlViewA );
        long idB = sqlViewService.saveSqlView( sqlViewB );

        assertNotNull( sqlViewService.getSqlView( idA ) );
        assertNotNull( sqlViewService.getSqlView( idB ) );

        sqlViewService.deleteSqlView( sqlViewService.getSqlView( idA ) );

        assertNull( sqlViewService.getSqlView( idA ) );
        assertNotNull( sqlViewService.getSqlView( idB ) );

        sqlViewService.deleteSqlView( sqlViewService.getSqlView( idB ) );

        assertNull( sqlViewService.getSqlView( idA ) );
        assertNull( sqlViewService.getSqlView( idB ) );
    }

    @Test
    void testGetSqlViewByName()
    {
        SqlView sqlViewA = createSqlView( 'A', sqlA );
        SqlView sqlViewB = createSqlView( 'B', sqlB );

        long idA = sqlViewService.saveSqlView( sqlViewA );
        long idB = sqlViewService.saveSqlView( sqlViewB );

        assertEquals( sqlViewService.getSqlView( "SqlViewA" ).getId(), idA );
        assertEquals( sqlViewService.getSqlView( "SqlViewB" ).getId(), idB );
        assertNull( sqlViewService.getSqlView( "SqlViewC" ) );
    }

    @Test
    void testSetUpViewTableName()
    {
        SqlView sqlViewC = createSqlView( 'C', sqlC );
        SqlView sqlViewD = createSqlView( 'D', sqlD );

        assertEquals( "_view_sqlviewc", sqlViewC.getViewName() );
        assertNotSame( "_view_sqlviewc", sqlViewD.getViewName() );
    }

    @Test
    void testValidateIllegalKeywords()
    {
        assertThrows( IllegalQueryException.class,
            () -> sqlViewService.validateSqlView( getSqlView( "delete * from dataelement" ), null, null ) );
    }

    @Test
    void testValidateIllegalKeywordsCTE()
    {
        SqlView sqlView = getSqlView( "WITH foo as (delete FROM dataelement returning *) SELECT * FROM foo;" );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> sqlViewService.validateSqlView( sqlView, null, null ) ),
            ErrorCode.E4311 );
    }

    @Test
    void testValidateIllegalKeywordsAtEnd()
    {
        SqlView sqlView = getSqlView( "WITH foo as (SELECT * FROM organisationunit) commit" );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> sqlViewService.validateSqlView( sqlView, null, null ) ),
            ErrorCode.E4311 );
    }

    @Test
    void testValidateIllegalKeywordsAfterSemicolon()
    {
        SqlView sqlView = getSqlView( "select * from dataelement; delete from dataelement" );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> sqlViewService.validateSqlView( sqlView, null, null ) ),
            ErrorCode.E4311 );
    }

    @Test
    void testValidateProtectedTables()
    {
        SqlView sqlView = getSqlView( "select * from userinfo where userinfoid=1" );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> sqlViewService.validateSqlView( sqlView, null, null ) ),
            ErrorCode.E4310 );
    }

    @Test
    void testValidateProtectedTables2()
    {
        SqlView sqlView = getSqlView( "select * from \"userinfo\" where userinfoid=1" );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> sqlViewService.validateSqlView( sqlView, null, null ) ),
            ErrorCode.E4310 );
    }

    @Test
    void testValidateProtectedTables3()
    {
        SqlView sqlView = getSqlView( "select userinfo.username \n FROM \"public\".users;" );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> sqlViewService.validateSqlView( sqlView, null, null ) ),
            ErrorCode.E4310 );
    }

    @Test
    void testValidateMissingVariables()
    {
        SqlView sqlView = getSqlView(
            "select * from dataelement where valueType = '${valueType}' and aggregationtype = '${aggregationType}'" );

        Map<String, String> variables = new HashMap<>();
        variables.put( "valueType", "int" );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class,
                () -> sqlViewService.validateSqlView( sqlView, null, variables ) ),
            ErrorCode.E4307 );
    }

    @Test
    void testValidateNotSelectQuery()
    {
        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class,
                () -> sqlViewService.validateSqlView( getSqlView( "* from dataelement" ), null, null ) ),
            ErrorCode.E4301 );
    }

    @Test
    void testValidateTableList()
    {
        SqlView sqlView = getSqlView( "select username,password from users,dataapprovallevel" );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> sqlViewService.validateSqlView( sqlView, null, null ) ),
            ErrorCode.E4310 );
    }

    @Test
    void testGetGridValidationFailure()
    {
        // this is the easiest way to be allowed to read SQL view data
        createAndInjectAdminUser();

        SqlView sqlView = getSqlView( "select * from dataelement; delete from dataelement" );
        sqlViewService.saveSqlView( sqlView );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class,
                () -> sqlViewService.getSqlViewGrid( sqlView, null, null, null, null ) ),
            ErrorCode.E4311 );
    }

    @Test
    void testGetGridRequiresDataReadSharing()
    {
        createAndInjectAdminUser( "F_SQLVIEW_PUBLIC_ADD" );

        // we have the right to create the view
        SqlView sqlView = getSqlView( "select * from dataelement; delete from dataelement" );
        sqlViewService.saveSqlView( sqlView );

        // but we lack sharing to view the result grid
        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class,
                () -> sqlViewService.getSqlViewGrid( sqlView, null, null, null, null ) ),
            ErrorCode.E4312 );
    }

    @Test
    void testValidateSuccess_NonAsciiLetterVariableValues()
    {
        sqlViewService.validateSqlView( getSqlView( "select * from dataelement where valueType = '${valueType}'" ),
            null, singletonMap( "valueType", "å" ) );
    }

    @Test
    void testValidateSuccessA()
    {
        SqlView sqlView = getSqlView( "select * from dataelement where valueType = '${valueType}'" );

        Map<String, String> variables = new HashMap<>();
        variables.put( "valueType", "int" );

        sqlViewService.validateSqlView( sqlView, null, variables );
    }

    @Test
    void testValidateSuccessB()
    {
        SqlView sqlView = getSqlView(
            "select ug.name from usergroup ug where ug.name ~* '^OU\\s(\\w.*)\\sAgency\\s(\\w.*)\\susers$'" );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test
    void testValidateSuccessC()
    {
        SqlView sqlView = getSqlView(
            "SELECT a.dataelementid as dsd_id,a.name as dsd_name,b.dataelementid as ta_id,b.ta_name FROM dataelement a" );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test
    void testValidateSuccessD()
    {
        SqlView sqlView = getSqlView( "SELECT name, created, lastupdated FROM dataelement" );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test
    void testValidateSuccessE()
    {
        SqlView sqlView = getSqlView( "select * from datavalue where storedby = '${_current_username}'" );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test
    void testValidateSuccessF()
    {
        SqlView sqlView = getSqlView(
            "select * from dataset where timelydays = ${timelyDays} and userid = ${_current_user_id}" );

        Map<String, String> variables = new HashMap<>();
        variables.put( "timelyDays", "15" );

        sqlViewService.validateSqlView( sqlView, null, variables );
    }

    @Test
    void testValidate_InvalidVarName()
    {
        SqlView sqlView = getSqlView(
            "select * from dataelement where valueType = '${typö}' and aggregationtype = '${aggregationType}'" );

        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> sqlViewService.validateSqlView( sqlView, null, null ) );
        assertEquals( ErrorCode.E4313, ex.getErrorCode() );
        assertEquals( "SQL query contains variable names that are invalid: `[typö]`", ex.getMessage() );
    }

    @Test
    void testGetSqlViewGrid()
    {
        User admin = createAndInjectAdminUser(); // makes admin current user

        Map<String, String> variables = new HashMap<>();
        variables.put( "ten", "10" );

        SqlView sqlView = new SqlView( "Name", "select '${_current_username}' as username, " +
            "${_current_user_id} as id, ${ten} as value",
            SqlViewType.QUERY );

        Grid grid = sqlViewService.getSqlViewGrid( sqlView, null, variables, null, null );

        String username = admin.getUsername();
        long id = admin.getId();
        assertEquals( "[\n" +
            "[username, id, value]\n" +
            "[" + username + ", " + id + ", 10]\n" +
            "]", grid.toString() );
    }

    private SqlView getSqlView( String sqlViewString )
    {
        return new SqlView( "Name", sqlViewString, SqlViewType.QUERY );
    }
}
