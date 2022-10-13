package org.hisp.dhis.tracker.teis;

import com.google.gson.JsonObject;
import org.hisp.dhis.Constants;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

class TEIExportTest extends TrackerApiTest {
    JsonObject object;

    private static final String ORG_UNIT = "O6uvpzGd5pu";
    private static final String PROGRAM = "f1AyMswryyQ";

    @BeforeAll
    public void before()
            throws Exception
    {
        loginActions.loginAsSuperUser();

        object = new FileReaderUtils()
                .read( new File( "src/test/resources/tracker/teis/teisWithEventsAndEnrollments.json" ) )
                .get( JsonObject.class );
        teiActions.post( object ).validate().statusCode( 200 );
    }

    @Test
    void trackedEntityInstanceReturnsAssignedUserInfo( )
    {
        ApiResponse response = teiActions.get("?program="+ PROGRAM +"&ou="+ORG_UNIT+"&ouMode=SELECTED&fields=trackedEntityInstance,enrollments[events]");
        System.out.println("Response body: " + response.getBody());

        Assertions.assertEquals(200, response.statusCode());
        JsonObject jsonObject = response.getBody().get("trackedEntityInstances").getAsJsonArray().get(0).getAsJsonObject().get("enrollments").getAsJsonArray().get(0).getAsJsonObject().get("events").getAsJsonArray().get(0).getAsJsonObject();

        Assertions.assertAll(() -> Assertions.assertEquals("TA", jsonObject.get("assignedUserFirstName").getAsString()),
                () -> Assertions.assertEquals("TA Superuser", jsonObject.get("assignedUserDisplayName").getAsString()),
                () -> Assertions.assertEquals("Superuser", jsonObject.get("assignedUserSurname").getAsString()),
                () -> Assertions.assertEquals("PQD6wXJ2r5j", jsonObject.get("assignedUser").getAsString()));
    }
}
