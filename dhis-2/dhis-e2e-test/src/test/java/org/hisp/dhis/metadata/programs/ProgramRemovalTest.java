

package org.hisp.dhis.metadata.programs;

import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ProgramRemovalTest
    extends ApiTest
{
    private ProgramActions programActions;

    private RestApiActions relationshipTypeActions;

    private String programId;

    private String relationshipTypeId;

    @BeforeEach
    public void beforeEach()
        throws Exception
    {
        programActions = new ProgramActions();
        relationshipTypeActions = new RestApiActions( "/relationshipTypes" );

        new LoginActions().loginAsSuperUser();
        setupData();
    }

    @Test
    public void shouldRemoveRelationshipTypesWhenProgramIsRemoved()
    {
        programActions.delete( programId )
            .validate().statusCode( 200 );

        relationshipTypeActions.get( relationshipTypeId )
            .validate().statusCode( 404 );
    }

    private void setupData()
        throws Exception
    {
        programId = programActions.createProgram( "WITH_REGISTRATION" ).extractUid();
        assertNotNull( programId, "Failed to create program" );

        JsonObject relationshipType = new FileReaderUtils()
            .read( new File( "src/test/resources/tracker/relationshipTypes.json" ) )
            .replacePropertyValuesWithIds( "id" )
            .get( JsonObject.class ).getAsJsonArray( "relationshipTypes" ).get( 0 )
            .getAsJsonObject();

        JsonObject constraint = new JsonObject();

        constraint.addProperty( "relationshipEntity", "PROGRAM_STAGE_INSTANCE" );

        JsonObject program = new JsonObject();
        program.addProperty( "id", programId );

        constraint.add( "program", program );
        relationshipType.add( "toConstraint", constraint );

        relationshipTypeId = relationshipTypeActions.create( relationshipType );
        assertNotNull( relationshipTypeId, "Failed to create relationshipType" );

    }
}
