package org.hisp.dhis.sqlview;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IllegalQueryException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Dang Duy Hieu
 */
public class SqlViewServiceTest
    extends DhisSpringTest
{
    @Autowired
    private SqlViewService sqlViewService;

    private String sqlA = "SELECT   *  FROM     _categorystructure;;  ; ;;;  ;; ; ";

    private String sqlB = "SELECT COUNT(*) from organisationunit;";

    private String sqlC = "SELECT COUNT(_cocn.*) AS so_dem, _icgss.indicatorid AS in_id"
        + "FROM _indicatorgroupsetstructure AS _icgss, categoryoptioncombo AS _cocn "
        + "GROUP BY _icgss.indicatorid;";

    private String sqlD = "SELECT de.name, dv.sourceid, dv.value, p.startdate "
        + "FROM dataelement AS de, datavalue AS dv, period AS p " + "WHERE de.dataelementid=dv.dataelementid "
        + "AND dv.periodid=p.periodid LIMIT 10";

    private String sqlE = "WITH foo as (SELECT * FROM organisationunit) SELECT * FROM foo LIMIT 2; " ;
    
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

    @Test
    public void testAddSqlView()
    {
        SqlView sqlViewA = createSqlView( 'A', sqlA );
        SqlView sqlViewB = createSqlView( 'B', sqlB );
        SqlView sqlViewE = createSqlView( 'E', sqlE );

        int idA = sqlViewService.saveSqlView( sqlViewA );
        int idB = sqlViewService.saveSqlView( sqlViewB );
        int idE = sqlViewService.saveSqlView( sqlViewE );

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
    public void testUpdateSqlView()
    {
        SqlView sqlView = createSqlView( 'A', sqlA );

        int id = sqlViewService.saveSqlView( sqlView );

        sqlView = sqlViewService.getSqlView( id );

        assertEq( 'A', sqlView, sqlA );

        sqlView.setName( "SqlViewC" );

        sqlViewService.updateSqlView( sqlView );
    }

    @Test
    public void testGetAndDeleteSqlView()
    {
        SqlView sqlViewA = createSqlView( 'A', sqlC );
        SqlView sqlViewB = createSqlView( 'B', sqlD );

        int idA = sqlViewService.saveSqlView( sqlViewA );
        int idB = sqlViewService.saveSqlView( sqlViewB );

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
    public void testGetSqlViewByName()
    {
        SqlView sqlViewA = createSqlView( 'A', sqlA );
        SqlView sqlViewB = createSqlView( 'B', sqlB );

        int idA = sqlViewService.saveSqlView( sqlViewA );
        int idB = sqlViewService.saveSqlView( sqlViewB );

        assertEquals( sqlViewService.getSqlView( "SqlViewA" ).getId(), idA );
        assertEquals( sqlViewService.getSqlView( "SqlViewB" ).getId(), idB );
        assertNull( sqlViewService.getSqlView( "SqlViewC" ) );
    }

    @Test
    public void testSetUpViewTableName()
    {
        SqlView sqlViewC = createSqlView( 'C', sqlC );
        SqlView sqlViewD = createSqlView( 'D', sqlD );

        assertEquals( "_view_sqlviewc", sqlViewC.getViewName() );
        assertNotSame( "_view_sqlviewc", sqlViewD.getViewName() );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateIllegalKeywords()
    {
        SqlView sqlView = new SqlView( "Name", "delete * from dataelement", SqlViewType.QUERY );
        
        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test (expected = IllegalQueryException.class)
    public void testValidateIllegalKeywordsCTE()
    {
        SqlView sqlView = new SqlView( "Name", "WITH foo as (delete FROM dataelement returning *) SELECT * FROM foo;", SqlViewType.QUERY );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test (expected = IllegalQueryException.class)
    public void testValidateIllegalKeywordsAtEnd()
    {
        SqlView sqlView = new SqlView( "Name", "WITH foo as (SELECT * FROM organisationunit) commit", SqlViewType.QUERY );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateProtectedTables()
    {
        SqlView sqlView = new SqlView( "Name", "select * from userinfo where userinfoid=1", SqlViewType.QUERY );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateProtectedTables2()
    {
        SqlView sqlView = new SqlView( "Name", "select * from \"userinfo\" where userinfoid=1", SqlViewType.QUERY );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateProtectedTables3()
    {
        SqlView sqlView = new SqlView( "Name", "select users.username \n FROM \"public\".users;", SqlViewType.QUERY );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateMissingVariables()
    {
        SqlView sqlView = new SqlView( "Name", "select * from dataelement where valueType = '${valueType}' and aggregationtype = '${aggregationType}'", SqlViewType.QUERY );
        
        Map<String, String> variables = new HashMap<>();
        variables.put( "valueType", "int" );
        
        sqlViewService.validateSqlView( sqlView, null, variables );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateIllegalSemiColon()
    {
        SqlView sqlView = new SqlView( "Name", "select * from dataelement; delete from dataelement", SqlViewType.QUERY );
        
        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateNotSelectQuery()
    {
        SqlView sqlView = new SqlView( "Name", "* from dataelement", SqlViewType.QUERY );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test( expected = IllegalQueryException.class )
    public void testValidateTableList()
    {
        SqlView sqlView = new SqlView( "Name", "select username,password from users,dataapprovallevel", SqlViewType.QUERY );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test( expected = IllegalQueryException.class )
    public void testGetGridValidationFailure()
    {
        SqlView sqlView = new SqlView( "Name", "select * from dataelement; delete from dataelement", SqlViewType.QUERY );
        
        sqlViewService.saveSqlView( sqlView );
        
        sqlViewService.getSqlViewGrid( sqlView, null, null, null, null );
    }
    
    @Test
    public void testValidateSuccessA()
    {
        SqlView sqlView = new SqlView( "Name", "select * from dataelement where valueType = '${valueType}'", SqlViewType.QUERY );
        
        Map<String, String> variables = new HashMap<>();
        variables.put( "valueType", "int" );
        
        sqlViewService.validateSqlView( sqlView, null, variables );
    }
    
    @Test
    public void testValidateSuccessB()
    {
        SqlView sqlView = new SqlView( "Name", "select ug.name from usergroup ug where ug.name ~* '^OU\\s(\\w.*)\\sAgency\\s(\\w.*)\\susers$'", SqlViewType.QUERY );
        
        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test
    public void testValidateSuccessC()
    {
        SqlView sqlView = new SqlView( "Name", "SELECT a.dataelementid as dsd_id,a.name as dsd_name,b.dataelementid as ta_id,b.ta_name FROM dataelement a", SqlViewType.QUERY );

        sqlViewService.validateSqlView( sqlView, null, null );
    }

    @Test
    public void testValidateSuccessD()
    {
        SqlView sqlView = new SqlView( "Name", "SELECT name, created, lastupdated FROM dataelement", SqlViewType.QUERY );

        sqlViewService.validateSqlView( sqlView, null, null );
    }
}
