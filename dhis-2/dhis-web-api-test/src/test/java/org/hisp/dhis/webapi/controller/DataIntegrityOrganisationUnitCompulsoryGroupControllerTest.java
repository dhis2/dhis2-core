package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataIntegrityOrganisationUnitCompulsoryGroupControllerTest extends DhisControllerIntegrationTest {
    @Test
    void testOrgUnitInCompulsoryGroup()
    {

        //Create an orgunit group
        String test_orgunitgroup = assertStatus( HttpStatus.CREATED,
                POST( "/organisationUnitGroups", "{'name': 'Type A', 'shortName': 'Type A'}" ) );
        //Add it to a group set
        String myougs = assertStatus( HttpStatus.CREATED,
                POST( "/organisationUnitGroupSets", "{'name': 'Type', 'shortName': 'Type', 'compulsory' : 'true' , 'organisationUnitGroups' :[{'id' : '" + test_orgunitgroup + "'}]}" ) );

        JsonIdentifiableObject test_ougs = GET( "/organisationUnitGroupSets/" + myougs ).content().as(JsonIdentifiableObject.class);
        //Create an orgunit, but do not add it to the compulsory group
        assertStatus( HttpStatus.CREATED,
                POST( "/organisationUnits", "{ 'name': 'Fish District', 'shortName': 'Fish District', 'openingDate' : '2022-01-01'}" ) );

        postSummary( "orgunit_compulsory_group_count" );
        JsonDataIntegritySummary summary = GET( "/dataIntegrity/orgunit_compulsory_group_count/summary" ).content()
                .as( JsonDataIntegritySummary.class );
        assertTrue( summary.exists() );
        assertTrue( summary.isObject() );
        assertEquals( 1, summary.getCount() );
        assertEquals( 100, summary.getPercentage().intValue() );
    }

    protected final void postSummary( String check )
    {
        HttpResponse trigger = POST( "/dataIntegrity/summary?checks=" + check );
        assertEquals( "http://localhost/dataIntegrity/summary?checks=" + check, trigger.location() );
        assertTrue( trigger.content().isA( JsonWebMessage.class ) );
    }
}
