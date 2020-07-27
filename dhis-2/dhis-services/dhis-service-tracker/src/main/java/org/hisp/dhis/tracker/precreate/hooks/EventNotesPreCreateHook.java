package org.hisp.dhis.tracker.precreate.hooks;

import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleHook;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Luciano Fiandesio
 */
@Component
public class EventNotesPreCreateHook implements TrackerBundleHook
{
    private final TrackedEntityCommentService commentService;

    public EventNotesPreCreateHook( TrackedEntityCommentService commentService )
    {
        this.commentService = commentService;
    }

    public void preCreate( Class<?> klass, Object object, TrackerBundle bundle )
    {
        if ( klass.isAssignableFrom( Event.class ) )
        {
//            System.out.println( "hello world" );
//            Event event = (Event)object;
//            final List<Note> notes = event.getNotes();



        }
    }

}
