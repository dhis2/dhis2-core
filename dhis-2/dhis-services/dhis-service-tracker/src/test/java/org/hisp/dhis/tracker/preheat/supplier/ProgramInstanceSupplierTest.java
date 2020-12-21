package org.hisp.dhis.tracker.preheat.supplier;

import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.random.BeanRandomizer;
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
public class ProgramInstanceSupplierTest
{
    @InjectMocks
    private ProgramInstanceSupplier supplier;

    @Mock
    private ProgramInstanceStore store;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Test
    public void verifySupplier()
    {
        final List<ProgramInstance> programInstances = rnd.randomObjects( ProgramInstance.class, 5 );
        // set the OrgUnit parent to null to avoid recursive errors when mapping
        programInstances.forEach( p -> p.getOrganisationUnit().setParent( null ) );
        when( store.getByType( WITHOUT_REGISTRATION ) ).thenReturn( programInstances );

        final TrackerImportParams params = TrackerImportParams.builder().build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( params, preheat );

        final List<String> programUids = programInstances.stream().map( pi -> pi.getProgram().getUid() )
            .collect( Collectors.toList() );
        for ( String programUid : programUids )
        {
            assertNotNull( preheat.getProgramInstancesWithoutRegistration( programUid ) );
        }
    }

}
