package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = { DebugMapper.class, UserGroupMapper.class } )
public interface UserAuthorityGroupMapper extends PreheatMapper<UserAuthorityGroup>
{
    UserAuthorityGroupMapper INSTANCE = Mappers.getMapper( UserAuthorityGroupMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "authorities" )
    UserAuthorityGroup map( UserAuthorityGroup userGroupAccess );
}
