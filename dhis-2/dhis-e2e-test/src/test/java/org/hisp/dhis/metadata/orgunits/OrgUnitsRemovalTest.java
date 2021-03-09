

package org.hisp.dhis.metadata.orgunits;

import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.metadata.OrgUnitActions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OrgUnitsRemovalTest
    extends ApiTest
{
    private OrgUnitActions orgUnitActions;

    private MetadataActions metadataActions;

    private RestApiActions orgUnitGroupActions;

    private RestApiActions orgUnitSetActions;

    private String parentId = "PIBHO8qBw9o";

    private String groupId = "IViMsXfUyWn";

    private String setId = "XcKGktFuGFj";

    @BeforeEach
    public void beforeAll()
    {
        orgUnitActions = new OrgUnitActions();
        orgUnitGroupActions = new RestApiActions( "/organisationUnitGroups" );
        orgUnitSetActions = new RestApiActions( "/organisationUnitGroupSets" );
        metadataActions = new MetadataActions();

        new LoginActions().loginAsSuperUser();

        metadataActions.importAndValidateMetadata( new File( "src/test/resources/metadata/orgunits/orgUnitDeletion.json" ) );

    }

    @Test
    public void shouldRemoveGroupReferenceWhenGroupIsDeleted()
    {
        orgUnitGroupActions.delete( groupId )
            .validate()
            .statusCode( 200 );

        orgUnitActions.get( parentId )
            .validate()
            .statusCode( 200 )
            .body( "organisationUnitGroups.id", is( Matchers.empty() ) );
    }

    @Test
    public void shouldRemoveSetReferenceWhenSetIsDeleted()
    {
        orgUnitSetActions.delete( setId )
            .validate()
            .statusCode( 200 );

        orgUnitActions.get( parentId )
            .validate()
            .statusCode( 200 )
            .body( "organisationUnitGroups.id", Matchers.contains( groupId ) );

        orgUnitGroupActions.get( groupId )
            .validate()
            .statusCode( 200 )
            .body( "groupSets.id", is( Matchers.empty() ) );
    }

    @Test
    public void shouldNotRemoveParentOrgUnit()
    {
        orgUnitActions.delete( parentId )
            .validate()
            .statusCode( 409 )
            .body( "message", containsStringIgnoringCase( "Object could not be deleted because it is associated with another object: OrganisationUnit" ) )
            .body( "errorCode", equalTo( "E4030" ) );

    }

}
