package org.hisp.dhis.tracker.preheat.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.cache.DefaultPreheatCacheService;
import org.hisp.dhis.tracker.preheat.cache.PreheatCacheService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.env.Environment;

/**
 * @author Luciano Fiandesio
 */
public class PeriodTypeSupplierTest
{
    private PeriodTypeSupplier supplier;

    @Mock
    private PeriodStore periodStore;

    @Mock
    private DhisConfigurationProvider conf;

    @Mock
    private Environment env;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Before
    public void setUp()
    {
        final PreheatCacheService cache = new DefaultPreheatCacheService( conf, env );
        supplier = new PeriodTypeSupplier( periodStore, cache );
        when( env.getActiveProfiles() ).thenReturn( new String[] {} );
    }

    @Test
    public void verifySupplier()
    {
        final List<Period> periods = rnd.randomObjects( Period.class, 20 );
        when( periodStore.getAll() ).thenReturn( periods );

        final TrackerImportParams params = TrackerImportParams.builder().build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( params, preheat );

        assertThat( preheat.getPeriodMap().values(), hasSize( 20 ) );
    }
}