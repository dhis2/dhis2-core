package org.hisp.dhis.tracker.preheat.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.relationship.RelationshipType;
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
public class RelationshipTypeSupplierTest
{
    private RelationshipTypeSupplier supplier;

    @Mock
    private IdentifiableObjectManager manager;

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
        final PreheatCacheService cache = new DefaultPreheatCacheService( conf,  env );
        supplier = new RelationshipTypeSupplier( manager, cache );
        when( env.getActiveProfiles() ).thenReturn( new String[] {} );
    }

    @Test
    public void verifySupplier()
    {
        final List<RelationshipType> relationshipTypes = rnd.randomObjects( RelationshipType.class, 5 );
        when( manager.getAll( RelationshipType.class ) ).thenReturn( relationshipTypes );

        final TrackerImportParams params = TrackerImportParams.builder().build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( params, preheat );

        assertThat( preheat.getAll( RelationshipType.class ), hasSize( 5 ) );
    }
}