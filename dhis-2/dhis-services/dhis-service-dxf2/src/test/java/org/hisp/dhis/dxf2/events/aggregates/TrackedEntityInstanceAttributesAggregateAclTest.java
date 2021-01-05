package org.hisp.dhis.dxf2.events.aggregates;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.security.acl.AccessStringHelper.DATA_READ;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.TrackerTest;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityInstanceAttributesAggregateAclTest extends TrackerTest
{
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService;

    @Autowired
    private TrackedEntityInstanceAggregate trackedEntityInstanceAggregate;


    @Override
    protected void mockCurrentUserService()
    {
        User user = createUser( "testUser" );

        setUserAuthorityToNonSuper( user );

        currentUserService = new MockCurrentUserService( user );

        ReflectionTestUtils.setField( trackedEntityInstanceAggregate, "currentUserService", currentUserService );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "currentUserService", currentUserService );
        ReflectionTestUtils.setField( teiService, "currentUserService", currentUserService );
    }

    @Test
    public void verifyTeiCantBeAccessedNoPublicAccessOnTrackedEntityType()
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
                .getTrackedEntityInstances( queryParams, params, false );

        assertThat( trackedEntityInstances, hasSize( 0 ) );
    }

    @Test
    public void verifyTeiCanBeAccessedWhenDATA_READPublicAccessOnTrackedEntityType()
    {
        doInTransaction( () -> {

            TrackedEntityType trackedEntityTypeZ = createTrackedEntityType( 'Z' );
            trackedEntityTypeZ.setUid( CodeGenerator.generateUid() );
            trackedEntityTypeZ.setName( "TrackedEntityTypeZ" + trackedEntityTypeZ.getUid() );
            trackedEntityTypeService.addTrackedEntityType( trackedEntityTypeZ );

            // When saving the trackedEntityType using addTrackedEntityType, the public access value is ignored
            // therefore we need to update the previously saved TeiType
            final TrackedEntityType trackedEntityType = trackedEntityTypeService.getTrackedEntityType(trackedEntityTypeZ.getUid());
            trackedEntityType.setPublicAccess( DATA_READ );
            trackedEntityTypeService.updateTrackedEntityType( trackedEntityType );

            this.persistTrackedEntityInstance( ImmutableMap.of( "trackedEntityType", trackedEntityType ) );
            this.persistTrackedEntityInstance( ImmutableMap.of( "trackedEntityType", trackedEntityType ) );
            this.persistTrackedEntityInstance();
            this.persistTrackedEntityInstance();
        } );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
                .getTrackedEntityInstances( queryParams, params, false );

        assertThat( trackedEntityInstances, hasSize( 2 ) );
    }

    protected void setUserAuthorityToNonSuper( User user )
    {
        UserCredentials userCredentials = new UserCredentials();
        UserAuthorityGroup userAuthorityGroup = new UserAuthorityGroup();
        userAuthorityGroup.setUid( CodeGenerator.generateUid() );
        userAuthorityGroup
            .setAuthorities( new HashSet<>( Collections.singletonList( "user" ) ) );
        userCredentials.setUserAuthorityGroups( Sets.newHashSet( userAuthorityGroup ) );
        user.setUserCredentials( userCredentials );
    }
}
