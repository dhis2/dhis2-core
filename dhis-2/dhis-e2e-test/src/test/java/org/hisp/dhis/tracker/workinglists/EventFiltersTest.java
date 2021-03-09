package org.hisp.dhis.tracker.workinglists;



import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EventFiltersTest
    extends ApiTest
{
    private RestApiActions eventFiltersActions;

    private String pathToFile = "src/test/resources/tracker/workinglists/eventFilters.json";

    @BeforeAll
    public void beforeAll()
    {
        eventFiltersActions = new RestApiActions( "/eventFilters" );

        new LoginActions().loginAsSuperUser();
    }

    @Test
    public void eventFilterCanBeSaved()
        throws Exception
    {
        JsonObject body = new FileReaderUtils().readJsonAndGenerateData( new File( pathToFile ) );

        ApiResponse response = eventFiltersActions.post( body );

        ResponseValidationHelper.validateObjectCreation( response );
    }
}
