package org.hisp.dhis.tracker.preheat.mappers;

import java.util.Set;

import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper( uses = DebugMapper.class )
public interface TrackedEntityInstanceMapper extends PreheatMapper<TrackedEntityInstance>
{
    TrackedEntityInstanceMapper INSTANCE = Mappers.getMapper( TrackedEntityInstanceMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    @Mapping( target = "user" )
    @Mapping( target = "publicAccess" )
    @Mapping( target = "externalAccess" )
    @Mapping( target = "userGroupAccesses", qualifiedByName = "userGroupAccesses" )
    @Mapping( target = "userAccesses", qualifiedByName = "userAccesses" )
    @Mapping( target = "organisationUnit" )
    @Mapping( target = "trackedEntityType" )
    @Mapping( target = "inactive" )
    @Mapping( target = "programInstances" )
    @Mapping( target = "created" )
    @Mapping( target = "trackedEntityAttributeValues" )
    TrackedEntityInstance map( TrackedEntityInstance trackedEntityInstance );

    @Named( "userGroupAccesses" )
    Set<UserGroupAccess> userGroupAccesses(Set<UserGroupAccess> userGroupAccesses );

    @Named( "userAccesses" )
    Set<UserAccess> mapUserAccessProgramInstance(Set<UserAccess> userAccesses );


}
