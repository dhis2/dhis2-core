package org.hisp.dhis.tracker.preheat.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class RelationshipTypeSupplierTest
{
    @InjectMocks
    private RelationshipTypeSupplier supplier;

    @Mock
    private IdentifiableObjectManager manager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Test
    public void verifySupplier()
    {
        final List<RelationshipType> relationshipTypes = rnd.randomObjects( RelationshipType.class, 5 );
        when( manager.getAll( RelationshipType.class ) ).thenReturn( relationshipTypes );

        final TrackerImportParams params = TrackerImportParams.builder().build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( params, preheat );

        assertThat( preheat.getAll( TrackerIdScheme.UID, RelationshipType.class ), hasSize( 5 ) );
    }
}