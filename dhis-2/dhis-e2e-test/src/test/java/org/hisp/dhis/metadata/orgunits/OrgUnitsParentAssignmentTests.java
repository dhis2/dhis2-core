package org.hisp.dhis.metadata.orgunits;



import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OrgUnitsParentAssignmentTests
    extends ApiTest
{
    private LoginActions loginActions;

    private OrgUnitActions orgUnitActions;

    @BeforeEach
    public void setUp()
    {
        loginActions = new LoginActions();
        orgUnitActions = new OrgUnitActions();

        loginActions.loginAsSuperUser();
    }

    @Test
    public void shouldAssignReferenceToBoth()
    {
        String orgUnitId = orgUnitActions.createOrgUnit();

        assertNotNull( orgUnitId, "Parent org unit wasn't created" );
        String childId = orgUnitActions.createOrgUnitWithParent( orgUnitId );

        assertNotNull( childId, "Child org unit wasn't created" );

        ApiResponse response = orgUnitActions.get( childId );
        response.validate()
            .statusCode( 200 )
            .body( "parent.id", Matchers.equalTo( orgUnitId ) );

        response = orgUnitActions.get( orgUnitId );
        response.validate()
            .statusCode( 200 )
            .body( "children", Matchers.not( Matchers.emptyArray() ) )
            .body( "children.id", Matchers.not( Matchers.emptyArray() ) )
            .body( "children.id[0]", Matchers.equalTo( childId ) );
    }

    @Test
    public void shouldAdjustTheOrgUnitTree()
    {
        String parentOrgUnitId = orgUnitActions.createOrgUnit( 1 );
        String intOrgUnit = orgUnitActions.createOrgUnitWithParent( parentOrgUnitId, 1 );
        String childOrgUnitId = orgUnitActions.createOrgUnitWithParent( intOrgUnit );

        orgUnitActions.get( intOrgUnit )
            .validate()
            .statusCode( 200 )
            .body( "level", equalTo( 2 ) );

        orgUnitActions.get( childOrgUnitId )
            .validate()
            .statusCode( 200 )
            .body( "level", equalTo( 3 ) );
    }
}
