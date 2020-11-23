package org.hisp.dhis.tracker.preheat.mappers;

import java.util.Set;

import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper( uses = { DebugMapper.class, UserGroupAccessMapper.class, UserGroupAccessMapper.class, ProgramMapper.class } )
public interface ProgramStageMapper extends PreheatMapper<ProgramStage>
{
    ProgramStageMapper INSTANCE = Mappers.getMapper( ProgramStageMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    @Mapping( target = "user" )
    @Mapping( target = "publicAccess" )
    @Mapping( target = "externalAccess" )
    @Mapping( target = "userGroupAccesses", qualifiedByName = "userGroupAccesses" )
    @Mapping( target = "userAccesses", qualifiedByName = "userAccesses" )
    @Mapping( target = "program" )
    @Mapping( target = "name" )
    @Mapping( target = "repeatable" )
    @Mapping( target = "programStageDataElements" )
    ProgramStage map( ProgramStage programStage );

    @Named( "userGroupAccesses" )
    Set<UserGroupAccess> mapUserGroupAccessPsi( Set<UserGroupAccess> userGroupAccesses );

    @Named( "userAccesses" )
    Set<UserAccess> mapUserAccessPsi( Set<UserAccess> userAccesses );
}
