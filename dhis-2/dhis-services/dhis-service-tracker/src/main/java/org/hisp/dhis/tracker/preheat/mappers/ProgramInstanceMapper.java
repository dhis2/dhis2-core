package org.hisp.dhis.tracker.preheat.mappers;

import java.util.Set;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper( uses = { DebugMapper.class, UserGroupAccessMapper.class, UserAccessMapper.class, ProgramMapper.class,
    TrackedEntityInstanceMapper.class, OrganisationUnitMapper.class } )
public interface ProgramInstanceMapper extends PreheatMapper<ProgramInstance>
{
    ProgramInstanceMapper INSTANCE = Mappers.getMapper( ProgramInstanceMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    @Mapping( target = "user" )
    @Mapping( target = "publicAccess" )
    @Mapping( target = "externalAccess" )
    @Mapping( target = "userGroupAccesses", qualifiedByName = "userGroupAccessesPi" )
    @Mapping( target = "userAccesses", qualifiedByName = "userAccessesPi" )
    @Mapping( target = "program" )
    @Mapping( target = "entityInstance" )
    @Mapping( target = "organisationUnit" )
    @Mapping( target = "created" )
    @Mapping( target = "enrollmentDate" )
    @Mapping( target = "comments" )
    @Mapping( target = "programStageInstances" ) // FIXME this is a killer, because it's going to fetch
                                                 // all the events for this PI. This is needed in PreCheckOwnershipValidationHook
                                                 // boolean hasNonDeletedEvents = pi.getProgramStageInstances().stream().anyMatch( psi -> !psi.isDeleted() );
    ProgramInstance map( ProgramInstance programInstance );

    @Named( "userGroupAccessesPi" )
    Set<UserGroupAccess> userGroupAccesses( Set<UserGroupAccess> userGroupAccesses );

    @Named( "userAccessesPi" )
    Set<UserAccess> mapUserAccessProgramInstance( Set<UserAccess> userAccesses );
}
