package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper()
public interface OrganisationUnitMapper
    extends PreheatMapper<OrganisationUnit>
{
    OrganisationUnitMapper INSTANCE = Mappers.getMapper( OrganisationUnitMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    @Mapping( target = "user" )
    @Mapping( target = "publicAccess" )
    @Mapping( target = "externalAccess" )
    @Mapping( target = "userGroupAccesses" )
    @Mapping( target = "userAccesses" )
    @Mapping( target = "programs" )
    OrganisationUnit map( OrganisationUnit organisationUnit );
}
