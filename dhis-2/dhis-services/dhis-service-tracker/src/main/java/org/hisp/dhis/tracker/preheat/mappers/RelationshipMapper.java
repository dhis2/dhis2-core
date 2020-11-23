package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.relationship.Relationship;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = DebugMapper.class )
public interface RelationshipMapper extends PreheatMapper<Relationship>
{
    RelationshipMapper INSTANCE = Mappers.getMapper( RelationshipMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "relationshipType" )
    @Mapping( target = "from" )
    @Mapping( target = "to" )
    Relationship map( Relationship relationship );
}
