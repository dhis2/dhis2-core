package org.hisp.dhis.webapi.controller.metadata;

import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.domain.JsonOrganisationUnit;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static org.junit.Assert.assertEquals;

public class CrudControllerPagingTest extends DhisControllerConvenienceTest
{
    @Autowired
    private OrganisationUnitService ouService;

    @Before
    public void setUp()
    {
        ouService.addOrganisationUnit( createOrganisationUnit( "A" ) );
        ouService.addOrganisationUnit( createOrganisationUnit( "B" ) );
        ouService.addOrganisationUnit( createOrganisationUnit( "C" ) );
        ouService.addOrganisationUnit( createOrganisationUnit( "D" ) );
        ouService.addOrganisationUnit( createOrganisationUnit( "E" ) );
    }

    @Test
    public void testPage1()
    {
        JsonList<JsonOrganisationUnit> ous = GET( "/organisationUnits?paging=true&pageSize=2&page=1" )
            .content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 2, ous.size() );
    }

    @Test
    public void testPage2AndOrderByDisplayName()
    {
        JsonList<JsonOrganisationUnit> ous = GET( "/organisationUnits?order=displayName&paging=true&pageSize=2&page=2" )
            .content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 2, ous.size() );
        assertEquals( "C", ous.get( 0 ).getDisplayName() );
        assertEquals( "D", ous.get( 1 ).getDisplayName() );
    }

    @Test
    public void testOrderByDisplayName()
    {
        JsonList<JsonOrganisationUnit> ous = GET(
            "/organisationUnits?order=displayName&paging=true&pageSize=2&page=2" )
            .content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 2, ous.size() );
        assertEquals( "C", ous.get( 0 ).getDisplayName() );
        assertEquals( "D", ous.get( 1 ).getDisplayName() );
    }

    @Test
    public void testOrderByDisplayNameDesc()
    {
        JsonList<JsonOrganisationUnit> ous = GET(
            "/organisationUnits?order=displayName:desc&paging=true&pageSize=2&page=2" )
            .content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 2, ous.size() );
        assertEquals( "C", ous.get( 0 ).getDisplayName() );
        assertEquals( "B", ous.get( 1 ).getDisplayName() );
    }

    @Test
    public void testFilterByDisplayName()
    {
        JsonList<JsonOrganisationUnit> ous = GET(
            "/organisationUnits?filter=displayName:in:[A,B,C]&paging=true&pageSize=2&page=2" ).content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 1, ous.size() );
        assertEquals( "C", ous.get( 0 ).getDisplayName() );
    }

    @Test
    public void testOrderByName()
    {
        JsonList<JsonOrganisationUnit> ous = GET( "/organisationUnits?order=name&paging=true&pageSize=2&page=2" )
            .content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 2, ous.size() );
        assertEquals( "C", ous.get( 0 ).getDisplayName() );
        assertEquals( "D", ous.get( 1 ).getDisplayName() );
    }

    @Test
    public void testFilterByName()
    {
        JsonList<JsonOrganisationUnit> ous = GET(
            "/organisationUnits?filter=name:in:[A,B,C]&paging=true&pageSize=2&page=2" ).content( HttpStatus.OK )
            .getList( "organisationUnits", JsonOrganisationUnit.class );
        assertEquals( 1, ous.size() );
        assertEquals( "C", ous.get( 0 ).getDisplayName() );
    }
}
