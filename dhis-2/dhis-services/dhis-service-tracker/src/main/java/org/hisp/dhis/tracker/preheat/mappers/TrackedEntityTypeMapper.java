package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper( uses = DebugMapper.class )
public interface TrackedEntityTypeMapper extends PreheatMapper<TrackedEntityType>
{
    TrackedEntityTypeMapper INSTANCE = Mappers.getMapper( TrackedEntityTypeMapper.class );

    TrackedEntityType map( TrackedEntityType trackedEntityType );
}
