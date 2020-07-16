package org.hisp.dhis.dxf2.events.aggregates;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.TrackerTest;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityInstanceAggregateTest extends TrackerTest
{
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackedEntityInstanceAggregate trackedEntityInstanceAggregate;

    @Override
    protected void mockCurrentUserService()
    {
        User user = createUser( "testUser" );

        makeUserSuper( user );

        currentUserService = new MockCurrentUserService( user );
    }

    @Before
    public void setUp()
    {
        ReflectionTestUtils.setField( trackedEntityInstanceAggregate, "currentUserService", currentUserService );
    }

    @Test
    public void testFetchTrackedEntityInstances()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
        } );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        assertThat( trackedEntityInstances, hasSize( 4 ) );
        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 0 ) );

    }

    @Test
    public void testFetchTrackedEntityInstancesAndEnrollments()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
        } );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();
        params.setIncludeEnrollments( true );

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        assertThat( trackedEntityInstances, hasSize( 4 ) );

        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments(), hasSize( 1 ) );

    }

    @Test
    public void testFetchTrackedEntityInstancesWithoutEnrollments()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
            this.persistTrackedEntityInstanceWithEnrollment();
        } );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();
        params.setIncludeEnrollments( false );

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        assertThat( trackedEntityInstances, hasSize( 4 ) );

        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments(), hasSize( 0 ) );

    }

    @Test
    public void testFetchTrackedEntityInstancesWithEvents()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
        } );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();
        params.setIncludeEnrollments( true );
        params.setIncludeEvents( true );

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        assertThat( trackedEntityInstances, hasSize( 4 ) );

        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments(), hasSize( 1 ) );

        assertThat( trackedEntityInstances.get( 0 ).getEnrollments().get( 0 ).getEvents(), hasSize( 5 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ).getEvents(), hasSize( 5 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments().get( 0 ).getEvents(), hasSize( 5 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments().get( 0 ).getEvents(), hasSize( 5 ) );
    }

    @Test
    public void testFetchTrackedEntityInstancesWithoutEvents()
    {
        doInTransaction( () -> {
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
            this.persistTrackedEntityInstanceWithEnrollmentAndEvents();
        } );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();
        params.setIncludeEnrollments( true );
        params.setIncludeEvents( false );

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        assertThat( trackedEntityInstances, hasSize( 4 ) );

        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments(), hasSize( 1 ) );

        assertThat( trackedEntityInstances.get( 0 ).getEnrollments().get( 0 ).getEvents(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 1 ).getEnrollments().get( 0 ).getEvents(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 2 ).getEnrollments().get( 0 ).getEvents(), hasSize( 0 ) );
        assertThat( trackedEntityInstances.get( 3 ).getEnrollments().get( 0 ).getEvents(), hasSize( 0 ) );
    }

    @Test
    public void testMapping()
    {
        long time = System.currentTimeMillis();

        doInTransaction( this::persistTrackedEntityInstanceWithEnrollmentAndEvents );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();
        params.setIncludeEnrollments( true );
        params.setIncludeEvents( true );

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );

        System.out.println( trackedEntityInstance );

        assertThat( trackedEntityInstance.getTrackedEntityType(), is( trackedEntityTypeA.getUid() ) );
        assertTrue( CodeGenerator.isValidUid( trackedEntityInstance.getTrackedEntityInstance() ) );
        assertThat( trackedEntityInstance.getOrgUnit(), is( organisationUnitA.getUid() ) );
        assertThat( trackedEntityInstance.isInactive(), is( false ) );
        assertThat( trackedEntityInstance.isDeleted(), is( false ) );
        assertThat( trackedEntityInstance.getFeatureType(), is( FeatureType.NONE ) );
        assertThat( trackedEntityInstance.getStoredBy(), is( nullValue() ) );

        //trackedEntityInstance.getCreated()
    }

    private void makeUserSuper( User user )
    {
        UserCredentials userCredentials = new UserCredentials();
        UserAuthorityGroup userAuthorityGroup1Super = new UserAuthorityGroup();
        userAuthorityGroup1Super.setUid( "uid4" );
        userAuthorityGroup1Super
            .setAuthorities( new HashSet<>( Arrays.asList( "z1", UserAuthorityGroup.AUTHORITY_ALL ) ) );
        userCredentials.setUserAuthorityGroups( Sets.newHashSet( userAuthorityGroup1Super ) );
        user.setUserCredentials( userCredentials );
    }

}