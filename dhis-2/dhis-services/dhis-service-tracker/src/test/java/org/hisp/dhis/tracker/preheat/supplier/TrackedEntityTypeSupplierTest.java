package org.hisp.dhis.tracker.preheat.supplier;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityTypeSupplierTest {

    @InjectMocks
    private TrackedEntityTypeSupplier supplier;

    @Mock
    private IdentifiableObjectManager manager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Test
    public void verifySupplier()
    {
        final List<TrackedEntityType> trackedEntityTypes = rnd.randomObjects( TrackedEntityType.class, 5 );
        when( manager.getAll( TrackedEntityType.class  ) ).thenReturn( trackedEntityTypes );

        final TrackerPreheatParams preheatParams = TrackerPreheatParams.builder().build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( preheatParams, preheat );

        assertThat( preheat.getAll( TrackerIdScheme.UID, TrackedEntityType.class ), hasSize( 5 ) );
    }
}