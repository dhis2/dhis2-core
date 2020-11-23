package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.relationship.RelationshipType;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper( uses = DebugMapper.class )
public interface RelationshipTypeMapper extends PreheatMapper<RelationshipType>
{
    RelationshipTypeMapper INSTANCE = Mappers.getMapper( RelationshipTypeMapper.class );

    RelationshipType map( RelationshipType relationshipType );
}
