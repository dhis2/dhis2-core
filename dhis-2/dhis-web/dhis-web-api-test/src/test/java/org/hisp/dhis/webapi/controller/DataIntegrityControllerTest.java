package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.Future;

import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonDocument.JsonNodeType;
import org.hisp.dhis.webapi.json.JsonObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class DataIntegrityControllerTest extends DhisControllerConvenienceTest
{

    @Autowired
    private SchedulingManager schedulingManager;

    @Test
    public void testDataIntegrity_NoErrors()
    {
        // if the report does not have any strings in the JSON there are no
        // errors
        assertEquals( 0, getDataIntegrityReport().node().count( JsonNodeType.STRING ) );
    }

    @Test
    public void testDataIntegrity_x()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits", "{'name':'test', 'shortName':'test', 'openingDate':'2021'}" ) );
        System.out.println( GET( "/organisationUnits/{id}", id ).content() );

        JsonObject report = getDataIntegrityReport();
        System.out.println( report );
        assertEquals( 0, report.node().count( JsonNodeType.STRING ) );
    }

    private JsonObject getDataIntegrityReport()
    {
        JsonObject response = POST( "/dataIntegrity" ).content().getObject( "response" );

        String id = response.getString( "id" ).string();
        Future<?> job = schedulingManager.getTask( id );
        try
        {
            job.get(); // wait until the job is done
        }
        catch ( Exception ex )
        {
            fail( "Data integrity check failed: " + ex.getMessage() );
        }

        String jobType = response.getString( "jobType" ).string();
        JsonObject report = GET( "/system/taskSummaries/{type}/{id}", jobType, id ).content();

        // OBS! we do this after the GET just so that some time passed
        // and the job had a chance to remove itself
        // BTW this test has nothing to do with data integrity but it is a good
        // chance to also cover the manager logic
        assertNull( "job has not removed itself on completion", schedulingManager.getTask( id ) );

        return report;
    }
}
