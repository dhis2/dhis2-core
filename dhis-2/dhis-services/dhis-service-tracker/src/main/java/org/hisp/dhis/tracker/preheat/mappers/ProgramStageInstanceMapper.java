package org.hisp.dhis.tracker.preheat.mappers;

import java.util.Set;

import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = { DebugMapper.class, UserGroupAccessMapper.class, UserAccessMapper.class, ProgramStageMapper.class,
    OrganisationUnitMapper.class, ProgramInstanceMapper.class } )
public interface ProgramStageInstanceMapper extends PreheatMapper<ProgramStageInstance>
{
    ProgramStageInstanceMapper INSTANCE = Mappers.getMapper( ProgramStageInstanceMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    @Mapping( target = "user" )
    @Mapping( target = "publicAccess" )
    @Mapping( target = "externalAccess" )
    @Mapping( target = "userGroupAccesses" )
    @Mapping( target = "userAccesses" )
    @Mapping( target = "programStage" )
    @Mapping( target = "status" )
    @Mapping( target = "organisationUnit" )
    @Mapping( target = "created" )
    @Mapping( target = "programInstance" )
    @Mapping( target = "eventDataValues" )
    @Mapping( target = "comments" )
    ProgramStageInstance map( ProgramStageInstance programStageInstance );

    Set<UserGroupAccess> mapUserGroupAccessPsi( Set<UserGroupAccess> userGroupAccesses );

    Set<UserAccess> mapUserAccessPsi( Set<UserAccess> userAccesses );
}
