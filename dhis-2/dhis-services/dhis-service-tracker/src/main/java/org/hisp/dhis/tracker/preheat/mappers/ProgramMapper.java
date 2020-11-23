package org.hisp.dhis.tracker.preheat.mappers;

import java.util.Set;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = { DebugMapper.class, OrganisationUnitMapper.class, UserGroupAccessMapper.class,
    UserAccessMapper.class } )
public interface ProgramMapper extends PreheatMapper<Program>
{
    ProgramMapper INSTANCE = Mappers.getMapper( ProgramMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    @Mapping( target = "trackedEntityType" )
    @Mapping( target = "publicAccess" )
    @Mapping( target = "externalAccess" )
    @Mapping( target = "userGroupAccesses" )
    @Mapping( target = "userAccesses" )
    @Mapping( target = "organisationUnits" )
    @Mapping( target = "programType" )
    @Mapping( target = "name" )
    @Mapping( target = "programAttributes" )
    @Mapping( target = "onlyEnrollOnce" )
    @Mapping( target = "featureType" )
    @Mapping( target = "categoryCombo" )
    Program map( Program program );

    Set<UserGroupAccess> userGroupAccessesProgram( Set<UserGroupAccess> userGroupAccesses );

    Set<UserAccess> mapUserAccessProgramInstanceProgram( Set<UserAccess> userAccesses );

    Set<OrganisationUnit> mapOrganisationUnits( Set<OrganisationUnit> organisationUnits );

}
