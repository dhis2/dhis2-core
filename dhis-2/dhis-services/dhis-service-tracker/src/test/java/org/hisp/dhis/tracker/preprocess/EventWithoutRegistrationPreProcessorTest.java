package org.hisp.dhis.tracker.preprocess;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author Luciano Fiandesio
 */
public class EventWithoutRegistrationPreProcessorTest
{
    private EventWithoutRegistrationPreProcessor preProcessorToTest;

    @Before
    public void setUp()
    {
        this.preProcessorToTest = new EventWithoutRegistrationPreProcessor();
    }

    @Test
    public void testEnrollmentIsAddedIntoEventWhenItBelongsToProgramWithoutRegistration()
    {
        //Given
        Event event = new Event();
        event.setProgram( "programUid" );
        TrackerBundle bundle = TrackerBundle.builder().events( Collections.singletonList( event ) ).build();
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( "programInstanceUid" );
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.putProgramInstancesWithoutRegistration( "programUid", programInstance );
        bundle.setPreheat( preheat );

        //When
        preProcessorToTest.process( bundle );

        //Then
        assertEquals( "programInstanceUid", bundle.getEvents().get( 0 ).getEnrollment() );
    }
}