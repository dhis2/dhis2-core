package org.hisp.dhis.tracker.preheat.supplier.classStrategy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class TrackerEntityInstanceStrategyTest
{
    @InjectMocks
    private TrackerEntityInstanceStrategy strategy;

    @Mock
    private TrackedEntityInstanceStore trackedEntityInstanceStore;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Test
    public void verifyStrategyFiltersOutNonRootTei()
    {
        // Create preheat params
        final List<TrackedEntity> trackedEntities = rnd.randomObjects( TrackedEntity.class, 2 );
        final TrackerImportParams params = TrackerImportParams.builder().trackedEntities( trackedEntities ).build();

        // Preheat
        TrackerPreheat preheat = new TrackerPreheat();

        final List<String> rootUids = trackedEntities.stream().map( TrackedEntity::getTrackedEntity )
            .collect( Collectors.toList() );
        // Add uid of non-root tei
        rootUids.add( "noroottei" );

        List<List<String>> uids = new ArrayList<>();
        uids.add( rootUids );

        // when
        strategy.add( params, uids, preheat );

        assertTrue( preheat.getReference( trackedEntities.get( 0 ).getTrackedEntity() ).isPresent() );
        assertTrue( preheat.getReference( trackedEntities.get( 1 ).getTrackedEntity() ).isPresent() );
        assertFalse( preheat.getReference( "noroottei" ).isPresent() );
    }

    @Test
    public void verifyStrategyIgnoresPersistedTei()
    {
        // Create preheat params
        final List<TrackedEntity> trackedEntities = rnd.randomObjects( TrackedEntity.class, 2 );
        final TrackerImportParams params = TrackerImportParams.builder().trackedEntities( trackedEntities ).build();

        // Preheat
        User user = new User();
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.setUser( user );

        final List<String> rootUids = trackedEntities.stream().map( TrackedEntity::getTrackedEntity )
            .collect( Collectors.toList() );
        // Add uid of non-root tei
        rootUids.add( "noroottei" );

        List<List<String>> uids = new ArrayList<>();
        uids.add( rootUids );

        when( trackedEntityInstanceStore.getIncludingDeleted( rootUids ) ).thenReturn( Lists.newArrayList(
            new TrackedEntityInstance()
            {{
                setUid( trackedEntities.get( 0 ).getTrackedEntity() );
            }}
        ) );

        // when
        strategy.add( params, uids, preheat );

        assertFalse( preheat.getReference( trackedEntities.get( 0 ).getTrackedEntity() ).isPresent() );
        assertTrue( preheat.getReference( trackedEntities.get( 1 ).getTrackedEntity() ).isPresent() );
        assertFalse( preheat.getReference( "noroottei" ).isPresent() );
    }
}