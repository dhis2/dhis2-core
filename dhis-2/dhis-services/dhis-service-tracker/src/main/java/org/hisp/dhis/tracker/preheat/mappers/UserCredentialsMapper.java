package org.hisp.dhis.tracker.preheat.mappers;

import java.util.Set;

import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = { DebugMapper.class, UserAuthorityGroupMapper.class } )
public interface UserCredentialsMapper extends PreheatMapper<UserCredentials>
{
    UserCredentialsMapper INSTANCE = Mappers.getMapper( UserCredentialsMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "username" )
    @Mapping( target = "userAuthorityGroups" )
    UserCredentials map( UserCredentials user );

    Set<UserAuthorityGroup> map( Set<UserAuthorityGroup> userAuthorityGroups );
}
