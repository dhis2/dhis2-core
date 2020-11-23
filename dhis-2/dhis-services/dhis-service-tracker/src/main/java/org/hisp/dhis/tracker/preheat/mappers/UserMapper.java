package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.user.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = DebugMapper.class )
public interface UserMapper extends PreheatMapper<User>
{
    UserMapper INSTANCE = Mappers.getMapper( UserMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    User map( User user );
}
