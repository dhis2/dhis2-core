package org.hisp.dhis.tracker.bundle.persister;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.converter.TrackerSideEffectConverterService;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class EventPersisterTest
{
    private EventPersister persister;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerConverterService<Event, ProgramStageInstance> eventConverter;

    @Mock
    private TrackedEntityCommentService trackedEntityCommentService;

    @Mock
    private TrackerSideEffectConverterService sideEffectConverterService;

    @Mock
    private ReservedValueService reservedValueService;

    @Mock
    private TrackerPreheat preheat;

    @Before
    public void setUp()
    {
        persister = new EventPersister( new ArrayList<>(), reservedValueService, eventConverter,
            trackedEntityCommentService, sideEffectConverterService );
    }

    @Test
    public void t1()
    {
        // bundle.getPreheat().getEvent( TrackerIdScheme.UID, event.getEvent() );
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setStatus( EventStatus.SKIPPED );

        ProgramStageInstance psi = new ProgramStageInstance();
        psi.setStatus( EventStatus.COMPLETED );

        when( preheat.getEvent( TrackerIdScheme.UID, event.getEvent() ) ).thenReturn( psi );

        TrackerBundle bundle = TrackerBundle
            .builder()
            .preheat( preheat )
            .build();

        ProgramStageInstance converted = persister.convertForPatch( bundle, event );
        assertThat( converted.getStatus(), is( EventStatus.SKIPPED ) );
    }

}