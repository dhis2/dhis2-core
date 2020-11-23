package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.user.UserGroupAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = { DebugMapper.class, UserGroupMapper.class } )
public interface UserGroupAccessMapper extends PreheatMapper<UserGroupAccess>
{
    UserGroupAccessMapper INSTANCE = Mappers.getMapper( UserGroupAccessMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "userGroup" )
    UserGroupAccess map( UserGroupAccess userGroupAccess );
}
