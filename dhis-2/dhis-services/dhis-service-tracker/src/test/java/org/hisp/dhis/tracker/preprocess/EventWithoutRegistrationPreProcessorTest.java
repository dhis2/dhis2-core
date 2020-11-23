package org.hisp.dhis.tracker.preprocess;

import com.google.common.collect.Lists;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static reactor.core.publisher.Mono.when;

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