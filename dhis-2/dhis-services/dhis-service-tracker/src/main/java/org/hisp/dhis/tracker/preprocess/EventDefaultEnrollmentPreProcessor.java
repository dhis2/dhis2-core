package org.hisp.dhis.tracker.preprocess;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.springframework.stereotype.Component;

/**
 * This preprocessor is responsible for setting the TrackedEntityInstance UID on
 * an Event, inferring it from the Event's parent enrollment. If the Event
 * already has a TrackedEntityInstance UID set, this preprocessor does not
 * process the Event
 *
 * @author Luciano Fiandesio
 */
@Component
public class EventDefaultEnrollmentPreProcessor implements BundlePreProcessor
{
    @Override
    public void process( TrackerBundle bundle )
    {
        for ( Event event : bundle.getEvents() )
        {
            // If the event enrollment is missing, it will be captured later by validation
            if ( StringUtils.isEmpty( event.getTrackedEntity() ) && StringUtils.isNotEmpty( event.getEnrollment() ) )
            {
                event.setTrackedEntity(
                    getFromPreheat( bundle.getPreheat(), event )
                        .orElseGet( () -> getFromRef( bundle, event ) ) );
            }
        }
    }

    private String getFromRef( TrackerBundle bundle, Event event )
    {
        final Optional<ReferenceTrackerEntity> ref = bundle.getPreheat().getReference( event.getEvent() );

        return ref
            .map( rte -> getTrackedEntityFromEnrollment( bundle.getEnrollments(), rte.getParentUid() ) )
            .orElse( null );
    }

    private Optional<String> getFromPreheat( TrackerPreheat preheat, Event event )
    {
        return Optional.ofNullable( preheat.getEnrollment( TrackerIdScheme.UID, event.getEnrollment() ) )
            .map( e -> {
                if ( e.getEntityInstance() != null )
                {
                    preheat.putTrackedEntities( TrackerIdScheme.UID,
                        Collections.singletonList( e.getEntityInstance() ) );
                    return e.getEntityInstance().getUid();
                }
                return null;
            } );
    }

    private String getTrackedEntityFromEnrollment( List<Enrollment> enrollments, String enrollment )
    {
        return enrollments.stream().filter( e -> e.getEnrollment().equals( enrollment ) ).findFirst()
            .map( Enrollment::getTrackedEntity ).orElse( null );

    }

}
