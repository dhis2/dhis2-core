package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.WebClient.Header;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Tests {@link MaintenanceController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
public class MaintenanceControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testPruneDataByOrganisationUnit()
    {
        String ougId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );

        assertWebMessage( "OK", 200, "OK", "Data was pruned successfully",
            POST( "/maintenance/dataPruning/organisationUnits/" + ougId ).content() );
    }

    @Test
    public void testPruneDataByOrganisationUnit_DoesNotExist()
    {
        String ouId = CodeGenerator.generateUid();
        assertEquals( "Organisation unit does not exist: " + ouId,
            POST( "/maintenance/dataPruning/organisationUnits/" + ouId ).error( HttpStatus.CONFLICT ).getMessage() );
    }

    @Test
    public void testPruneDataByOrganisationUnit_MissingAuthority()
    {
        switchToNewUser( "guest" );
        assertEquals( "Access is denied",
            POST( "/maintenance/dataPruning/organisationUnits/xzy" ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    public void testPruneDataByDataElement()
    {
        String ccId = GET(
            "/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default" )
                .content().getObject( 0 ).getString( "id" ).string();

        String deId = assertStatus( HttpStatus.CREATED, POST( "/dataElements/",
            "{'name':'My data element', 'shortName':'DE1', 'code':'DE1', 'valueType':'INTEGER', " +
                "'aggregationType':'SUM', 'zeroIsSignificant':false, 'domainType':'AGGREGATE', " +
                "'categoryCombo': {'id': '" + ccId + "'}}" ) );

        assertWebMessage( "OK", 200, "OK", "Data was pruned successfully",
            POST( "/maintenance/dataPruning/dataElements/" + deId ).content() );
    }

    @Test
    public void testPruneDataByDataElement_DoesNotExist()
    {
        String deId = CodeGenerator.generateUid();
        assertEquals( "Data element does not exist: " + deId,
            POST( "/maintenance/dataPruning/dataElements/" + deId ).error( HttpStatus.CONFLICT ).getMessage() );
    }

    @Test
    public void testPruneDataByDataElement_MissingAuthority()
    {
        switchToNewUser( "guest" );
        assertEquals( "Access is denied",
            POST( "/maintenance/dataPruning/dataElements/xzy" ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    public void testAppReload()
    {
        assertWebMessage( "OK", 200, "OK", "Apps reloaded",
            GET( "/maintenance/appReload" ).content() );
    }

    @Test
    public void testAppReload_MissingAuthority()
    {
        switchToNewUser( "guest" );
        assertEquals( "Access is denied",
            GET( "/maintenance/appReload" ).error( HttpStatus.FORBIDDEN ).getMessage() );
    }

    @Test
    public void testUpdateCategoryOptionCombos()
    {
        String ccId = GET(
            "/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default" )
                .content().getObject( 0 ).getString( "id" ).string();

        assertWebMessage( "OK", 200, "OK", "Import was successful.",
            POST( "/maintenance/categoryOptionComboUpdate/categoryCombo/" + ccId ).content() );
    }

    @Test
    public void testUpdateCategoryOptionCombos_XML_AcceptHeader()
    {
        String ccId = GET(
            "/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default" )
                .content().getObject( 0 ).getString( "id" ).string();

        String xmlResponse = POST( "/maintenance/categoryOptionComboUpdate/categoryCombo/" + ccId,
            Header( "Accept", MediaType.APPLICATION_XML_VALUE ) ).content( MediaType.APPLICATION_XML );
        assertTrue( xmlResponse.startsWith( "<webMessage" ) );
    }

    @Test
    public void testUpdateCategoryOptionCombos_XML_PathExtension()
    {
        String ccId = GET(
            "/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default" )
                .content().getObject( 0 ).getString( "id" ).string();

        String xmlResponse = POST( "/maintenance/categoryOptionComboUpdate/categoryCombo/" + ccId + ".xml" )
            .content( MediaType.APPLICATION_XML );
        assertTrue( xmlResponse.startsWith( "<webMessage" ) );
    }

    @Test
    public void testUpdateCategoryOptionCombos_DoesNotExist()
    {
        assertEquals( "CategoryCombo does not exist: xyz",
            POST( "/maintenance/categoryOptionComboUpdate/categoryCombo/xyz" )
                .error( HttpStatus.CONFLICT ).getMessage() );
    }

    @Test
    public void testUpdateCategoryOptionCombos_MissingAuthority()
    {
        switchToNewUser( "guest" );
        assertEquals( "Access is denied",
            POST( "/maintenance/categoryOptionComboUpdate/categoryCombo/xyz" )
                .error( HttpStatus.FORBIDDEN ).getMessage() );
    }
}
