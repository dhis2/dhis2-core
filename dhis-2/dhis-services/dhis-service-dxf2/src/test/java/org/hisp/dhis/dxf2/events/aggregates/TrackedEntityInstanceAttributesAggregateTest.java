package org.hisp.dhis.dxf2.events.aggregates;

import com.google.common.collect.Sets;
import org.hisp.dhis.dxf2.TrackerTest;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityInstanceAttributesAggregateTest extends TrackerTest
{
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private TrackedEntityInstanceAggregate trackedEntityInstanceAggregate;

    private TrackedEntityAttribute attributeA;

    private TrackedEntityAttribute attributeB;

    private TrackedEntityAttribute attributeC;

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
    public void testTrackedEntityInstanceAttributes()
    {
        doInTransaction( () -> {
            attributeA = createTrackedEntityAttribute( 'A' );
            attributeB = createTrackedEntityAttribute( 'B' );
            attributeC = createTrackedEntityAttribute( 'C' );

            attributeService.addTrackedEntityAttribute( attributeA );
            attributeService.addTrackedEntityAttribute( attributeB );
            attributeService.addTrackedEntityAttribute( attributeC );

            final org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance = persistTrackedEntityInstance();

            attributeValueService.addTrackedEntityAttributeValue(
                new TrackedEntityAttributeValue( attributeA, trackedEntityInstance, "A" ) );
            attributeValueService.addTrackedEntityAttributeValue(
                new TrackedEntityAttributeValue( attributeB, trackedEntityInstance, "B" ) );
            attributeValueService.addTrackedEntityAttributeValue(
                new TrackedEntityAttributeValue( attributeC, trackedEntityInstance, "C" ) );
        } );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        assertThat( trackedEntityInstances.get( 0 ).getAttributes(), hasSize( 3 ) );
    }
}
