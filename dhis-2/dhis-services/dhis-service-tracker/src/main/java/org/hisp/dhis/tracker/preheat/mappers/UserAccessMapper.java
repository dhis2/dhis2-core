package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.user.UserAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = { DebugMapper.class, UserMapper.class } )
public interface UserAccessMapper extends PreheatMapper<UserAccess>
{
    UserAccessMapper INSTANCE = Mappers.getMapper( UserAccessMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "access" )
    @Mapping( target = "user" )
    UserAccess map( UserAccess userAccess );
}
