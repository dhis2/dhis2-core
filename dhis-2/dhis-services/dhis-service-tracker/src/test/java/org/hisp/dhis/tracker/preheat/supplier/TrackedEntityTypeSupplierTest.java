package org.hisp.dhis.tracker.preheat.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.cache.DefaultPreheatCacheService;
import org.hisp.dhis.tracker.preheat.cache.PreheatCacheService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.env.Environment;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityTypeSupplierTest {

    private TrackedEntityTypeSupplier supplier;

    @Mock
    private DhisConfigurationProvider conf;

    @Mock
    private Environment env;

    @Mock
    private IdentifiableObjectManager manager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Before
    public void setUp()
    {
        final PreheatCacheService cache = new DefaultPreheatCacheService( conf,  env );
        supplier = new TrackedEntityTypeSupplier( manager, cache );
        when( env.getActiveProfiles() ).thenReturn( new String[] {} );
    }
    @Test
    public void verifySupplier()
    {
        final List<TrackedEntityType> trackedEntityTypes = rnd.randomObjects( TrackedEntityType.class, 5 );
        when( manager.getAll( TrackedEntityType.class  ) ).thenReturn( trackedEntityTypes );

        final TrackerImportParams params = TrackerImportParams.builder().build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( params, preheat );

        assertThat( preheat.getAll( TrackedEntityType.class ), hasSize( 5 ) );
    }
}