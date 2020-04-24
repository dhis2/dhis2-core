package org.hisp.dhis.tracker.acl;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.collections.ListUtils;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.actions.tracker.TEIActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.rmi.runtime.Log;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TrackedEntityInstanceAclReadTests
    extends ApiTest
{
    private static final String _TEIS = "trackedEntityInstances";

    private static final String _ENROLLMENTS = "enrollments";

    private static final String _EVENTS = "events";

    private static final String _TET = "trackedEntityType";

    private static final String _PROGRAM = "program";

    private static final String _PROGRAMSTAGE = "programStage";

    private static final String _OU = "orgUnit";

    private static final String _TEI = "trackedEntityInstance";

    private static final String _ENROLLMENT = "enrollment";

    private static final String _PROGRAMOWNERS = "programOwners";

    private static final String _OWNEROU = "ownerOrgUnit";

    private static final String _DELETED = "deleted";

    private JsonObject object;

    private RestApiActions metadataActions;

    private UserActions userActions;

    private TEIActions teiActions;

    private static Map<String, String> userPasswordMap = new HashMap<>();

    private static Map<String, List<String>> userOrganisationUnitAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userOrganisationUnitNotAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userTrackedEntityTypeAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userTrackedEntityTypeNotAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userProgramAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userProgramNotAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userProgramStageAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userProgramStageNotAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userRelationshipTypeAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userRelationshipTypeNotAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userTrackedEntityAttributeAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userTrackedEntityAttributeNotAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userTrackedEntityDateElementAllowedMap = new HashMap<>();

    private static Map<String, List<String>> userTrackedEntityDataElementNotAllowedMap = new HashMap<>();

    @BeforeAll
    public void before()
        throws Exception
    {
        teiActions = new TEIActions();
        metadataActions = new RestApiActions( "/metadata" );
        userActions = new UserActions();

        // Setup as SuperUser
        new LoginActions().loginAsDefaultUser();

        // Set up metadata (Import twice to connect all references)
        JsonObject metadata = new FileReaderUtils().read( new File( "src/test/resources/tracker/acl/metadata.json" ) )
            .get( JsonObject.class );

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.addAll( "async=false" );
        metadataActions.post( metadata, queryParamsBuilder );
        metadataActions.post( metadata, queryParamsBuilder );

        // Import test data
        JsonObject trackerData = new FileReaderUtils().read( new File( "src/test/resources/tracker/acl/data.json" ) )
            .get( JsonObject.class );
        teiActions.post( trackerData );

        // Set up user A
        String user = "User A";
        userPasswordMap.put( user, "UserA123!" );
        userOrganisationUnitAllowedMap.put( user, Lists.newArrayList( "siyOVWFPeS5", "tAXoecmVen9", "MyResqR17xm" ) );
        userOrganisationUnitNotAllowedMap.put( user, Lists.newArrayList( "fKg9cOzw3qJ", "OUQ3Ny4FaF5" ) );
        userTrackedEntityTypeAllowedMap.put( user, Lists.newArrayList( "YDzXLdCvV4h", "RttzawN27Pi" ) );
        userTrackedEntityTypeNotAllowedMap.put( user, Lists.newArrayList( "QPi1HImFAmE" ) );
        userProgramAllowedMap
            .put( user, Lists.newArrayList( "akJ8bT2029n", "RttzawN27Pi", "Get7eJdT3ge", "BZJg3CVLSWo" ) );
        userProgramNotAllowedMap.put( user, Lists.newArrayList( "EJhjfdU9zI8", "BiBTvMh3kku" ) );
        userProgramStageAllowedMap.put( user, Lists.newArrayList( "dFzGNUmbnvL", "BaTDILVlipb", "caZoL1LKwG0" ) );
        userProgramStageNotAllowedMap.put( user, Lists.newArrayList() );
        userRelationshipTypeAllowedMap.put( user, Lists.newArrayList() );
        userRelationshipTypeNotAllowedMap.put( user, Lists.newArrayList() );
        userTrackedEntityAttributeAllowedMap.put( user, Lists.newArrayList() );
        userTrackedEntityAttributeNotAllowedMap.put( user, Lists.newArrayList() );
        userTrackedEntityDateElementAllowedMap.put( user, Lists.newArrayList() );
        userTrackedEntityDataElementNotAllowedMap.put( user, Lists.newArrayList() );

        userActions.updateUserPassword( "O2PajOxjJSa", userPasswordMap.get( user ) );

        // Set up user B
    }

    @Test
    public void test()
    {
        String user = "User A";

        new LoginActions().loginAsUser( user, userPasswordMap.get( user ) );

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.addAll( "ouMode=ACCESSIBLE", "fields=*" );
        ApiResponse response = teiActions.get( "/", queryParamsBuilder );

        response.validate().statusCode( 200 );

        JsonObject json = response.getBody();

        assertTrue( json.has( _TEIS ) );

        json.getAsJsonArray( _TEIS ).iterator()
            .forEachRemaining( ( teiJson ) -> assertTrackedEntityInstance( user, teiJson.getAsJsonObject() ) );

    }


    /* Helper methods */

    /**
     * Asserts that the trackedEntityInstance follows the expectations.
     *
     * @param user the user(username) we are testing as
     * @param tei  the trackedEntityInstance we are testing
     */
    private void assertTrackedEntityInstance( String user, JsonObject tei )
    {
        String trackedEntityType = tei.get( _TET ).getAsString();
        List<String> ous = Lists.newArrayList( tei.getAsJsonObject().get( _OU ).getAsString() );
        tei.getAsJsonObject().getAsJsonArray( _PROGRAMOWNERS )
            .forEach(
                ( programOwner ) -> ous.add( programOwner.getAsJsonObject().get( _OWNEROU ).getAsString() ) );

        assertStringIsInWhitelistOrNotInBlacklist( userTrackedEntityTypeAllowedMap.get( user ),
            userTrackedEntityTypeNotAllowedMap.get( user ), trackedEntityType );
        assertWithinOuScope( userOrganisationUnitAllowedMap.get( user ), ous );
        assertNotDeleted( tei );

        assertTrue( tei.has( _ENROLLMENTS ) );

        tei.getAsJsonArray( _ENROLLMENTS )
            .forEach( enrollmentJson -> assertEnrollment( user, enrollmentJson.getAsJsonObject(), tei ) );
    }

    /**
     * Asserts that the enrollment follows the expectations.
     *
     * @param user       the user(username) we are testing as
     * @param enrollment the enrollment we are testing
     * @param tei        the tei wrapped around the enrollment
     */
    private void assertEnrollment( String user, JsonObject enrollment, JsonObject tei )
    {
        String program = enrollment.get( _PROGRAM ).getAsString();
        String orgUnit = enrollment.get( _OU ).getAsString();

        assertStringIsInWhitelistOrNotInBlacklist( userProgramAllowedMap.get( user ),
            userProgramNotAllowedMap.get( user ), program );
        assertSameValueForProperty( tei, enrollment, _TEI );
        assertWithinOuScope( userOrganisationUnitAllowedMap.get( user ), Lists.newArrayList( orgUnit ) );
        assertNotDeleted( enrollment );

        assertTrue( enrollment.has( _EVENTS ) );

        enrollment.get( _EVENTS ).getAsJsonArray()
            .forEach( eventJson -> assertEvent( user, eventJson.getAsJsonObject(), enrollment ) );
    }

    /**
     * Asserts that the event follows the expectations.
     *
     * @param user       the user(username) we are testing as
     * @param event      the event we are testing
     * @param enrollment the enrollment wrapped around the event
     */
    private void assertEvent( String user, JsonObject event, JsonObject enrollment )
    {
        String programStage = event.get( _PROGRAMSTAGE ).getAsString();
        String orgUnit = event.get( _OU ).getAsString();

        assertStringIsInWhitelistOrNotInBlacklist( userProgramStageAllowedMap.get( user ),
            userProgramStageNotAllowedMap.get( user ), programStage );
        assertWithinOuScope( userOrganisationUnitAllowedMap.get( user ), Lists.newArrayList( orgUnit ) );
        assertSameValueForProperty( enrollment, event, _ENROLLMENT );
        assertSameValueForProperty( enrollment, event, _TEI );
        assertNotDeleted( event );
    }

    /**
     * Asserts that the given JsonObject does not have a property "deleted" that is true.
     *
     * @param object the object to check
     */
    private void assertNotDeleted( JsonObject object )
    {
        assertTrue( object.has( _DELETED ) && !object.get( _DELETED ).getAsBoolean() );
    }

    /**
     * Asserts that two JsonObject share the same value for a given property
     *
     * @param a        First JsonObject to test
     * @param b        Second JsonObject to test
     * @param property The property to test
     */
    private void assertSameValueForProperty( JsonObject a, JsonObject b, String property )
    {
        assertTrue( a.has( property ) && b.has( property ) );
        assertEquals( a.get( property ), b.get( property ) );
    }

    /**
     * Assert that a list, other, of OrgUnit uids contains at least one Uid matching the inScope list of OrgUnit uids.
     *
     * @param inScope OrgUnit uids in the scope
     * @param other   OrgUnits to test
     */
    private void assertWithinOuScope( List<String> inScope, List<String> other )
    {
        assertFalse( ListUtils.intersection( inScope, other ).isEmpty() );
    }

    /**
     * Assert that a given String, str, either belongs to the whitelist, or is not in the blacklist.
     *
     * @param whitelist list of strings we allow
     * @param blacklist list of strings we dont allow
     * @param str       the string to test
     */
    private void assertStringIsInWhitelistOrNotInBlacklist( List<String> whitelist, List<String> blacklist, String str )
    {
        assertTrue( whitelist.contains( str ) || !blacklist.contains( str ) );
    }

}
