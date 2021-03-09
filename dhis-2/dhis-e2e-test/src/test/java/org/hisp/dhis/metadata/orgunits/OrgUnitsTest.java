

package org.hisp.dhis.metadata.orgunits;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.OrgUnit;
import org.hisp.dhis.helpers.ResponseValidationHelper;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OrgUnitsTest
    extends ApiTest
{
    private LoginActions loginActions;

    private UserActions userActions;

    private OrgUnitActions orgUnitActions;

    @BeforeEach
    public void setUp()
    {
        loginActions = new LoginActions();
        userActions = new UserActions();
        orgUnitActions = new OrgUnitActions();

        loginActions.loginAsSuperUser();
    }

    @Test
    public void shouldNotCreateWithoutPermissions()
    {
        String userName = DataGenerator.randomString();
        String psw = "!XPTOqwerty1";

        userActions.addUser( userName, psw );
        loginActions.loginAsUser( userName, psw );

        ApiResponse response = orgUnitActions.postDummyOrgUnit();

        response.validate()
            .statusCode( 403 )
            .body( "message", equalTo( "You don't have the proper permissions to create this object." ) );
    }

    @Test
    public void shouldAddWithoutLevel()
    {
        OrgUnit orgUnit = orgUnitActions.generateDummy();
        orgUnit.setLevel( null );

        ApiResponse response = orgUnitActions.post( orgUnit );
        ResponseValidationHelper.validateObjectCreation( response );

        String uid = response.extractUid();
        assertNotNull( uid, "Org unit id was not returned." );

        response = orgUnitActions.get( uid );

        // todo create validation helper to check the similarity.
        response.validate().statusCode( 200 )
            .body( "shortName", equalTo( orgUnit.getShortName() ) )
            .body( "name", equalTo( orgUnit.getName() ) )
            .body( "openingDate", equalTo( orgUnit.getOpeningDate() ) );
    }

    @Test
    public void shouldUpdate()
    {
        OrgUnit orgUnit = orgUnitActions.generateDummy();

        // create
        ApiResponse response = orgUnitActions.post( orgUnit );
        String uid = response.extractUid();
        assertNotNull( uid, "Org unit uid was not returned" );

        response = orgUnitActions.get( uid );
        String lastUpdatedDate = response.extractString( "lastUpdated" );

        // update

        orgUnit.setName( orgUnit.getName() + " updated" );
        orgUnit.setShortName( orgUnit.getShortName() + " updated" );
        orgUnit.setOpeningDate( "2017-09-10T00:00:00.000" );

        response = orgUnitActions.update( uid, orgUnit );
        assertEquals( 200, response.statusCode(), "Org unit wasn't updated" );

        // validate
        response = orgUnitActions.get( uid );

        response.validate().statusCode( 200 )
            .body( "shortName", equalTo( orgUnit.getShortName() ) )
            .body( "name", equalTo( orgUnit.getName() ) )
            .body( "openingDate", equalTo( orgUnit.getOpeningDate() ) )
            .body( "lastUpdated", not( equalTo( lastUpdatedDate ) ) );

    }
}
