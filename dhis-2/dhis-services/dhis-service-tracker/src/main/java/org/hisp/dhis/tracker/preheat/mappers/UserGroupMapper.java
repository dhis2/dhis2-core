package org.hisp.dhis.tracker.preheat.mappers;

import java.util.Set;

import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = { DebugMapper.class, UserMapper.class } )
public interface UserGroupMapper extends PreheatMapper<UserGroup>
{
    UserGroupMapper INSTANCE = Mappers.getMapper( UserGroupMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    @Mapping( target = "members" )
    UserGroup map( UserGroup userGroupAccess );

    Set<User> members( Set<User> members );
}
