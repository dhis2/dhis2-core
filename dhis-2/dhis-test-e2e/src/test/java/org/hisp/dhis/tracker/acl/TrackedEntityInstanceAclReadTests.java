/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.tracker.acl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.hamcrest.Matchers;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.Me;
import org.hisp.dhis.dto.OrgUnit;
import org.hisp.dhis.dto.UserGroup;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.models.User;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Stian Sandvold
 */
public class TrackedEntityInstanceAclReadTests
    extends TrackerApiTest
{
    private static final String _DATAREAD = "..r.*";

    private MetadataActions metadataActions;

    private UserActions userActions;

    private static final List<User> users = new ArrayList<>();

    @BeforeAll
    public void before()
        throws Exception
    {
        metadataActions = new MetadataActions();
        userActions = new UserActions();

        // Setup as SuperUser
        new LoginActions().loginAsDefaultUser();

        // Set up metadata (Import twice to connect all references)
        metadataActions.importAndValidateMetadata( new File( "src/test/resources/tracker/acl/metadata.json" ) );
        metadataActions.importAndValidateMetadata( new File( "src/test/resources/tracker/acl/metadata.json" ) );

        // Import test data
        teiActions.postFile( new File( "src/test/resources/tracker/acl/data.json" ) );

        // Set up all users for testing
        users.add( new User( "User A", "O2PajOxjJSa", "UserA!123" ) );
        users.add( new User( "User B", "aDy67f9ijOe", "UserB!123" ) );
        users.add( new User( "User C", "CKrrGm5Be8O", "UserC!123" ) );
        users.add( new User( "User D", "Lpa5INiC3Qf", "UserD!123" ) );
        users.add( new User( "User ALL", "GTqb3WOZMop", "UserALL!123" ) );

        // Update passwords, so we can log in as them
        // Set AllAuth if user has it and ou scopes.
        // Map metadata and data sharing
        users.forEach( this::setupUser );
    }

    /**
     * Takes a User object and retrieves information about the users from the
     * api. Updates the password of the user to allow access.
     *
     * @param user to setup
     */
    private void setupUser( User user )
    {
        userActions.updateUserPassword( user.getUid(), user.getPassword() );

        new LoginActions().loginAsUser( user.getUsername(), user.getPassword() );

        // Get User information from /me
        ApiResponse apiResponse = new RestApiActions( "/me" ).get();
        String asString = apiResponse.getAsString();
        Me me = apiResponse.as( Me.class );

        // Add userGroups
        user.setGroups( me.getUserGroups().stream().map( UserGroup::getId ).collect( Collectors.toList() ) );

        // Add search-scope ous
        user.setSearchScope(
            me.getTeiSearchOrganisationUnits().stream().map( OrgUnit::getId ).collect( Collectors.toList() ) );

        // Add capture-scope ous
        user.setCaptureScope(
            me.getOrganisationUnits().stream().map( OrgUnit::getId ).collect( Collectors.toList() ) );

        // Add hasAllAuthority if user has ALL authority
        user.setAllAuthority( me.getAuthorities().contains( "ALL" ) );

        // Setup map to decide what data can and cannot be read.
        setupAccessMap( user );
    }

    /**
     * Finds metadata a user has access to and determines what data can be read
     * or not based on sharing.
     *
     * @param user the user to setup
     */
    private void setupAccessMap( User user )
    {
        Map<String, List<String>> dataRead = new HashMap<>();

        // Configure params to only return metadata we care about
        String params = (new QueryParamsBuilder())
            .add( "trackedEntityTypes=true" )
            .add( "dataElements=true" )
            .add( "relationshipTypes=true" )
            .add( "programs=true" )
            .add( "trackedEntityAttributes=true" )
            .add( "programStages=true" )
            .add( "fields=id,userAccesses,publicAccess,userGroupAccesses" )
            .build();

        ApiResponse response = metadataActions.get( params );

        // Build map
        response.getBody().entrySet().forEach( ( entry ) -> {

            // Skip the System property.
            if ( !entry.getKey().equals( "system" ) )
            {
                dataRead.put( entry.getKey(), new ArrayList<>() );

                entry.getValue().getAsJsonArray().forEach( obj -> {
                    JsonObject object = obj.getAsJsonObject();

                    boolean hasDataRead = false;

                    if ( object.has( "publicAccess" )
                        && object.get( "publicAccess" ).getAsString().matches( _DATAREAD ) )
                    {
                        hasDataRead = true;
                    }
                    else
                    {
                        JsonArray userAccesses = object.getAsJsonArray( "userAccesses" ).getAsJsonArray();
                        JsonArray userGroupAccess = object.getAsJsonArray( "userGroupAccesses" ).getAsJsonArray();

                        for ( JsonElement access : userAccesses )
                        {
                            if ( access.getAsJsonObject().get( "userUid" ).getAsString().equals( user.getUid() ) &&
                                access.getAsJsonObject().get( "access" ).getAsString().matches( _DATAREAD ) )
                            {
                                hasDataRead = true;
                            }
                        }

                        if ( !hasDataRead )
                        {
                            for ( JsonElement access : userGroupAccess )
                            {
                                if ( user.getGroups()
                                    .contains( access.getAsJsonObject().get( "userGroupUid" ).getAsString() ) &&
                                    access.getAsJsonObject().get( "access" ).getAsString().matches( _DATAREAD ) )
                                {
                                    hasDataRead = true;
                                }
                            }
                        }
                    }

                    if ( hasDataRead )
                    {
                        dataRead.get( entry.getKey() ).add( obj.getAsJsonObject().get( "id" ).getAsString() );
                    }

                } );
            }
        } );

        user.setDataRead( dataRead );
    }

    @ParameterizedTest
    @ValueSource( strings = { "O2PajOxjJSa", "aDy67f9ijOe", "CKrrGm5Be8O", "Lpa5INiC3Qf", "GTqb3WOZMop" } )
    public void testUserDataAndOrgUnitScopeReadAccess( String userUid )
    {
        User user = users.stream()
            .filter( _user -> _user.getUid().equals( userUid ) )
            .findFirst()
            .orElseThrow( () -> new RuntimeException( "User UID not found for test" ) );

        new LoginActions().loginAsUser( user.getUsername(), user.getPassword() );

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.addAll( "filter=pyNnf3UaOOg:NE:zz", "trackedEntityType=YDzXLdCvV4h", "ouMode=ACCESSIBLE",
            "fields=*" );
        ApiResponse response = teiActions.get( "/", queryParamsBuilder );

        response.validate().statusCode( 200 );

        response.validate().body( "trackedEntityInstances", Matchers.not( Matchers.emptyArray() ) );

        JsonObject json = response.getBody();

        json.getAsJsonArray( "trackedEntityInstances" ).iterator()
            .forEachRemaining( ( teiJson ) -> assertTrackedEntityInstance( user, teiJson.getAsJsonObject() ) );

    }

    @Test
    void shouldReturnEventsWhenExplicitFieldsAreProvided()
    {
        User user = users.stream().findFirst()
            .orElseThrow( () -> new RuntimeException( "User UID not found for test" ) );
        new LoginActions().loginAsUser( user.getUsername(), user.getPassword() );

        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.addAll( "trackedEntityInstance=VROP0n2v145", "fields=enrollments[events[dueDate]]" );

        ApiResponse response = teiActions.get( "/", queryParamsBuilder );

        response.validate().statusCode( 200 );
        response.validate().body( "trackedEntityInstances", Matchers.not( Matchers.emptyArray() ) );

        JsonObject tei = response.getBody().getAsJsonArray( "trackedEntityInstances" ).get( 0 ).getAsJsonObject();
        assertTrue( tei.has( "enrollments" ) );

        JsonArray enrollmentsArray = tei.getAsJsonArray( "enrollments" );
        assertEquals( 1, enrollmentsArray.size() );
        assertTrue( enrollmentsArray.get( 0 ).getAsJsonObject().has( "events" ) );

        JsonArray eventsArray = enrollmentsArray.get( 0 ).getAsJsonObject().getAsJsonArray( "events" );
        assertEquals( 2, eventsArray.size() );
        assertTrue( eventsArray.get( 0 ).getAsJsonObject().has( "dueDate" ) );
        assertEquals( "2020-04-24T00:00:00.000",
            eventsArray.get( 0 ).getAsJsonObject().get( "dueDate" ).getAsString() );
        assertTrue( eventsArray.get( 1 ).getAsJsonObject().has( "dueDate" ) );
        assertEquals( "2020-04-24T00:00:00.000",
            eventsArray.get( 1 ).getAsJsonObject().get( "dueDate" ).getAsString() );
    }

    /* Helper methods */

    /**
     * Asserts that the trackedEntityInstance follows the expectations.
     *
     * @param user the user(username) we are testing as
     * @param tei the trackedEntityInstance we are testing
     */
    private void assertTrackedEntityInstance( User user, JsonObject tei )
    {
        String trackedEntityType = tei.get( "trackedEntityType" ).getAsString();
        List<String> ous = Lists.newArrayList( tei.getAsJsonObject().get( "orgUnit" ).getAsString() );
        tei.getAsJsonObject().getAsJsonArray( "programOwners" )
            .forEach(
                ( programOwner ) -> ous.add( programOwner.getAsJsonObject().get( "ownerOrgUnit" ).getAsString() ) );

        if ( !user.hasAllAuthority() )
        {
            assertStringIsInWhitelist( user.getDataRead().get( "trackedEntityTypes" ), trackedEntityType );
        }
        assertWithinOuScope( user.getScopes(), ous );
        assertNotDeleted( tei );

        assertTrue( tei.has( "enrollments" ) );

        tei.getAsJsonArray( "enrollments" )
            .forEach( enrollmentJson -> assertEnrollment( user, enrollmentJson.getAsJsonObject(), tei ) );
    }

    /**
     * Asserts that the enrollment follows the expectations.
     *
     * @param user the user(username) we are testing as
     * @param enrollment the enrollment we are testing
     * @param tei the tei wrapped around the enrollment
     */
    private void assertEnrollment( User user, JsonObject enrollment, JsonObject tei )
    {
        String program = enrollment.get( "program" ).getAsString();
        String orgUnit = enrollment.get( "orgUnit" ).getAsString();

        if ( !user.hasAllAuthority() )
        {
            assertStringIsInWhitelist( user.getDataRead().get( "programs" ), program );
        }
        assertSameValueForProperty( tei, enrollment, "trackedEntityInstance" );
        assertWithinOuScope( user.getScopes(), Lists.newArrayList( orgUnit ) );
        assertNotDeleted( enrollment );

        assertTrue( enrollment.has( "events" ) );

        enrollment.get( "events" ).getAsJsonArray()
            .forEach( eventJson -> assertEvent( user, eventJson.getAsJsonObject(), enrollment ) );
    }

    /**
     * Asserts that the event follows the expectations.
     *
     * @param user the user(username) we are testing as
     * @param event the event we are testing
     * @param enrollment the enrollment wrapped around the event
     */
    private void assertEvent( User user, JsonObject event, JsonObject enrollment )
    {
        String programStage = event.get( "programStage" ).getAsString();
        String orgUnit = event.get( "orgUnit" ).getAsString();

        if ( !user.hasAllAuthority() )
        {
            assertStringIsInWhitelist( user.getDataRead().get( "programStages" ), programStage );
        }
        assertWithinOuScope( user.getScopes(), Lists.newArrayList( orgUnit ) );
        assertSameValueForProperty( enrollment, event, "enrollment" );
        assertSameValueForProperty( enrollment, event, "trackedEntityInstance" );
        assertNotDeleted( event );
    }

    /**
     * Asserts that the given JsonObject does not have a property "deleted" that
     * is true.
     *
     * @param object the object to check
     */
    private void assertNotDeleted( JsonObject object )
    {
        assertTrue( object.has( "deleted" ) && !object.get( "deleted" ).getAsBoolean(),
            String.format( "Deleted object found: '%s'", object ) );
    }

    /**
     * Asserts that two JsonObject share the same value for a given property
     *
     * @param a First JsonObject to test
     * @param b Second JsonObject to test
     * @param property The property to test
     */
    private void assertSameValueForProperty( JsonObject a, JsonObject b, String property )
    {
        assertTrue( a.has( property ) && b.has( property ),
            String.format( "Property '%s' is not not present in both objects.", property ) );
        assertEquals( a.get( property ), b.get( property ), String
            .format( "Property '%s' expected to be the same, but is different: %s != %s", property, a.get( property ),
                b.get( property ) ) );
    }

    /**
     * Assert that a list, other, of OrgUnit uids contains at least one Uid
     * matching the inScope list of OrgUnit uids.
     *
     * @param inScope OrgUnit uids in the scope
     * @param other OrgUnits to test
     */
    private void assertWithinOuScope( List<String> inScope, List<String> other )
    {
        assertFalse( ListUtils.intersection( inScope, other ).isEmpty(),
            String.format( "OrganisationUnit [%s] is not within user's capture or search scope [%s]",
                String.join( ",", other ), String.join( ",", inScope ) ) );
    }

    /**
     * Assert that a given String, str, is part of a whitelist.
     *
     * @param whitelist list of strings we allow
     * @param str the string to test
     */
    private void assertStringIsInWhitelist( List<String> whitelist, String str )
    {
        assertTrue( whitelist.contains( str ),
            String.format( "User should not have access to data based on metadata with uid '%s'", str ) );
    }

}
