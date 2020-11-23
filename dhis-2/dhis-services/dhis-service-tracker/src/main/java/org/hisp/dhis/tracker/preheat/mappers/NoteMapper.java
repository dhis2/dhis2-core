package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper( uses = DebugMapper.class )
public interface NoteMapper extends PreheatMapper<TrackedEntityComment>
{
    NoteMapper INSTANCE = Mappers.getMapper( NoteMapper.class );

    TrackedEntityComment map( TrackedEntityComment trackedEntityComment );
}
