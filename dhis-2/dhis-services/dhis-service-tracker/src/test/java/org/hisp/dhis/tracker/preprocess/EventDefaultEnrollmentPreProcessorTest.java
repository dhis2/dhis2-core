package org.hisp.dhis.tracker.preprocess;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collections;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
public class EventDefaultEnrollmentPreProcessorTest
{
    private EventDefaultEnrollmentPreProcessor preProcessor;

    private BeanRandomizer rnd = new BeanRandomizer();

    @Before
    public void setUp()
    {
        preProcessor = new EventDefaultEnrollmentPreProcessor();
    }

    @Test
    public void verifyTrackedEntityValueIsNotModifiedWhenPresent()
    {
        Event event = new Event();
        event.setTrackedEntity( CodeGenerator.generateUid() );
        TrackerBundle bundle = TrackerBundle.builder().events( Collections.singletonList( event ) ).build();

        preProcessor.process( bundle );
        assertThat( event.getTrackedEntity(), is( bundle.getEvents().get( 0 ).getTrackedEntity() ) );
    }

    @Test
    public void verifyTrackedEntityValueIsNotModifiedWhenTrackedEntityValueAndEnrollmentValueAreMissing()
    {
        Event event = new Event();
        TrackerBundle bundle = TrackerBundle.builder().events( Collections.singletonList( event ) ).build();

        preProcessor.process( bundle );

        assertThat( event.getTrackedEntity(), is( nullValue() ) );
    }

    @Test
    public void verifyTrackedEntityValueIsSetToParentEnrollmentTeiValue()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        String teiUid = CodeGenerator.generateUid();

        ProgramInstance programInstance = rnd.randomObject( ProgramInstance.class );
        programInstance.setUid( enrollmentUid );
        TrackedEntityInstance trackedEntityInstance = rnd.randomObject( TrackedEntityInstance.class );
        trackedEntityInstance.setUid( teiUid );
        programInstance.setEntityInstance( trackedEntityInstance );

        TrackerPreheat preheat = new TrackerPreheat();
        preheat.setEnrollments( ImmutableMap.of( TrackerIdScheme.UID, ImmutableMap.of( enrollmentUid, programInstance)));

        Event event = new Event();
        event.setEnrollment( enrollmentUid );

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( preheat )
            .events( Collections.singletonList( event ) ).build();

        preProcessor.process( bundle );

        assertThat( event.getTrackedEntity(), is( teiUid ) );
    }

    @Test
    public void verifyTrackedEntityValueIsSetToParentEnrollmentTeiValueFromRef()
    {
        String teiUid = CodeGenerator.generateUid();

        // Create an enrollment with a reference to a parent tei
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( CodeGenerator.generateUid() );
        enrollment.setTrackedEntity( teiUid );

        // create an event without a tei reference
        Event event = new Event();
        event.setEnrollment( enrollment.getEnrollment() );

        // Add to preheat and make sure the enrollment and event uid are added to the reference tree
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.putEnrollments( TrackerIdScheme.UID, new ArrayList<>(), Collections.singletonList( enrollment ) );
        preheat.putEvents( TrackerIdScheme.UID, new ArrayList<>(), Collections.singletonList( event ) );

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( preheat )
            .enrollments( Collections.singletonList( enrollment ))
            .events( Collections.singletonList( event ) )
            .build();

        preProcessor.process( bundle );

        assertThat( event.getTrackedEntity(), is( teiUid ) );
    }

}