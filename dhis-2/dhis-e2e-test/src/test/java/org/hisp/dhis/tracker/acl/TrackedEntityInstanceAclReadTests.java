package org.hisp.dhis.tracker.acl;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.collections.ListUtils;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.tracker.EventActions;
import org.hisp.dhis.actions.tracker.TEIActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sun.rmi.runtime.Log;

import java.io.File;
import java.util.ArrayList;
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

    private static final String _ATTRIBUTES = "attributes";

    private static final String _ATTRIBUTE = "attribute";

    private JsonObject object;

    private MetadataActions metadataActions;

    private UserActions userActions;

    private TEIActions teiActions;

    private static final List<User> users = new ArrayList<>();

    private class User
    {
        private String username;

        private String uid;

        private String password;

        private Map<String, List<String>> access;

        public User( String username, String uid, String password )
        {
            this.username = username;
            this.uid = uid;
            this.password = password;
        }

        public String getUsername()
        {
            return username;
        }

        public void setUsername( String username )
        {
            this.username = username;
        }

        public String getUid()
        {
            return uid;
        }

        public void setUid( String uid )
        {
            this.uid = uid;
        }

        public String getPassword()
        {
            return password;
        }

        public void setPassword( String password )
        {
            this.password = password;
        }

        public Map<String, List<String>> getAccess()
        {
            return access;
        }

        public void setAccess( Map<String, List<String>> access )
        {
            this.access = access;
        }
    }

    @BeforeAll
    public void before()
        throws Exception
    {
        teiActions = new TEIActions();
        metadataActions = new MetadataActions();
        userActions = new UserActions();

        // Setup as SuperUser
        new LoginActions().loginAsDefaultUser();

        // Set up metadata (Import twice to connect all references)
        metadataActions.importMetadata( new File( "src/test/resources/tracker/acl/metadata.json" ) );
        metadataActions.importMetadata( new File( "src/test/resources/tracker/acl/metadata.json" ) );

        // Import test data
        JsonObject trackerData = new FileReaderUtils().read( new File( "src/test/resources/tracker/acl/data.json" ) )
            .get( JsonObject.class );
        teiActions.post( trackerData );

        // Set up all users for testing
        User admin = new User( "admin", "", "district" );
        users.add( new User( "User A", "O2PajOxjJSa", "UserA!123" ) );
        users.add( new User( "User B", "aDy67f9ijOe", "UserB!123" ) );
        users.add( new User( "User C", "CKrrGm5Be8O", "UserC!123" ) );
        users.add( new User( "User D", "Lpa5INiC3Qf", "UserD!123" ) );
        users.add( new User( "User ALL", "GTqb3WOZMop", "UserALL!123" ) );

        // Update passwords, so we can log in as them
        users.forEach( this::setupUser );

        // Update their access maps
        setupAccessMap( admin );
        users.forEach( this::setupAccessMap );

        System.out.println( users );
    }

    public void setupUser( User user )
    {
        userActions.updateUserPassword( user.getUid(), user.getPassword() );
    }

    public void setupAccessMap( User user )
    {
        Map<String, List<String>> userAccess = new HashMap<>();
        new LoginActions().loginAsUser( user.getUsername(), user.getPassword() );

        // Configure params to only return metadata we care about
        String params = (new QueryParamsBuilder())
            .add( "trackedEntityTypes=true" )
            .add( "dataElements=true" )
            .add( "relationshipTypes=true" )
            .add( "programs=true" )
            .add( "trackedEntityAttributes=true" )
            .add( "programStages=true" )
            .add( "fields=id,userAccesses" )
            .build();

        ApiResponse response = metadataActions.get( params );

        // Build map
        response.getBody().entrySet().forEach( ( entry ) -> {

            // Skip the System property.
            if ( !entry.getKey().equals( "system" ) )
            {
                userAccess.put( entry.getKey(), new ArrayList<>() );

                entry.getValue().getAsJsonArray().forEach( obj -> {
                    JsonObject object = obj.getAsJsonObject();
                    JsonArray userAccesses = object.getAsJsonArray( "userAccesses" ).getAsJsonArray();

                    boolean hasUserAccess = false;
                    for ( JsonElement access : userAccesses )
                    {
                        hasUserAccess = hasUserAccess ||
                            access.getAsJsonObject().get( "userUid" ).getAsString().equals( user.getUid() );
                    }

                    if ( hasUserAccess )
                    {
                        userAccess.get( entry.getKey() ).add( obj.getAsJsonObject().get( "id" ).getAsString() );
                    }
                } );

            }
        } );
    }

    @ParameterizedTest
    @ValueSource( strings = { "O2PajOxjJSa", "aDy67f9ijOe", "CKrrGm5Be8O", "Lpa5INiC3Qf", "GTqb3WOZMop" } )
    public void test( String userUid )
    {
        User user = users.stream()
            .filter( _user -> _user.getUid().equals( userUid ) )
            .findFirst()
            .orElseThrow( () -> new RuntimeException( "User UID not found for test" ) );

        new LoginActions().loginAsUser( user.getUsername(), user.getPassword() );

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
    private void assertTrackedEntityInstance( User user, JsonObject tei )
    {
        String trackedEntityType = tei.get( _TET ).getAsString();
        List<String> ous = Lists.newArrayList( tei.getAsJsonObject().get( _OU ).getAsString() );
        tei.getAsJsonObject().getAsJsonArray( _PROGRAMOWNERS )
            .forEach(
                ( programOwner ) -> ous.add( programOwner.getAsJsonObject().get( _OWNEROU ).getAsString() ) );

        assertStringIsInWhitelistOrNotInBlacklist( user.getAccess().get( "trackedEntityTypes" ),
            new ArrayList<>(), trackedEntityType );
        assertWithinOuScope( new ArrayList<>(), ous );
        assertNotDeleted( tei );

        assertTrue( tei.has( _ENROLLMENTS ) );

        tei.getAsJsonArray( _ATTRIBUTES )
            .forEach( attributeJson -> assertAttribute( user, attributeJson.getAsJsonObject() ) );

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
    private void assertEnrollment( User user, JsonObject enrollment, JsonObject tei )
    {
        String program = enrollment.get( _PROGRAM ).getAsString();
        String orgUnit = enrollment.get( _OU ).getAsString();

        assertStringIsInWhitelistOrNotInBlacklist( user.getAccess().get( "programs" ),
            new ArrayList<>(), program );
        assertSameValueForProperty( tei, enrollment, _TEI );
        assertWithinOuScope( new ArrayList<>(), Lists.newArrayList( orgUnit ) );
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
    private void assertEvent( User user, JsonObject event, JsonObject enrollment )
    {
        String programStage = event.get( _PROGRAMSTAGE ).getAsString();
        String orgUnit = event.get( _OU ).getAsString();

        assertStringIsInWhitelistOrNotInBlacklist( user.getAccess().get( "programStages" ),
            new ArrayList<>(), programStage );
        assertWithinOuScope( new ArrayList<>(), Lists.newArrayList( orgUnit ) );
        assertSameValueForProperty( enrollment, event, _ENROLLMENT );
        assertSameValueForProperty( enrollment, event, _TEI );
        assertNotDeleted( event );
    }

    private void assertAttribute( User user, JsonObject attribute )
    {
        String attributeUid = attribute.get( _ATTRIBUTE ).getAsString();

        assertStringIsInWhitelistOrNotInBlacklist( user.getAccess().get( "trackedEntityAttributes" ),
            new ArrayList<>(), attributeUid );
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
