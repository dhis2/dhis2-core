package org.hisp.dhis.tracker.preheat.mappers;

import java.util.Set;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper( uses = { DebugMapper.class, UserCredentialsMapper.class, OrganisationUnitMapper.class } )
public interface FullUserMapper extends PreheatMapper<User>
{
    FullUserMapper INSTANCE = Mappers.getMapper( FullUserMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    @Mapping( target = "userCredentials" )
    @Mapping( target = "organisationUnits", qualifiedByName = "organisationUnits" )
    @Mapping( target = "teiSearchOrganisationUnits", qualifiedByName = "teiSearchOrganisationUnits" )
    User map( User user );

    @Named( "teiSearchOrganisationUnits" )
    Set<OrganisationUnit> teiSearchOrganisationUnits( Set<OrganisationUnit> organisationUnits );

    @Named( "organisationUnits" )
    Set<OrganisationUnit> organisationUnits( Set<OrganisationUnit> organisationUnits );
}
