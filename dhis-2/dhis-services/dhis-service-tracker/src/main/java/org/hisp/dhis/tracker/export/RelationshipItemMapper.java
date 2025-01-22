/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 import org.mapstruct.Mapping;
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 @Mapper
 @Mapping(target = "relationship", ignore = true)
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.tracker.export;

import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.tracker.imports.preheat.mappers.EnrollmentMapper;
import org.hisp.dhis.tracker.imports.preheat.mappers.EventMapper;
import org.hisp.dhis.tracker.imports.preheat.mappers.RelationshipTypeMapper;
import org.hisp.dhis.tracker.imports.preheat.mappers.TrackedEntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

// TODO(DHIS2-18883) move this into the relationship service/store
// double-check that we only map whats needed!
@Mapper(uses={
    RelationshipTypeMapper.class,
    TrackedEntityMapper.class,
    EnrollmentMapper.class,
    EventMapper.class,
})
public interface RelationshipItemMapper {
  RelationshipItemMapper RELATIONSHIP_ITEM_MAPPER =
      Mappers.getMapper(RelationshipItemMapper.class);
  @Mapping(
      target = "from",
      source = "from",
      qualifiedByName = "mapRelationshipItemWithoutRelationship")
  @Mapping(
      target = "to",
      source = "to" ,
      qualifiedByName = "mapRelationshipItemWithoutRelationship")
  Relationship map(Relationship relationship);

  @Named("mapRelationshipItemWithoutRelationship")
  // we need to ignore relationship to break the cycle between relationship and relationshipItem
  @Mapping(target = "relationship", ignore = true)
  RelationshipItem mapRelationshipItemWithoutRelationship(RelationshipItem relationshipItem);

  /** Main mapper to detach a {@link RelationshipItem} from Hibernate. */
  default RelationshipItem map(
      RelationshipItem relationshipItem) {
    if (relationshipItem == null) {
      return null;
    }

    // TODO(DHIS2-18883) we are not mapping/detaching the relationshipItem itself as the view layer
    // accesses/maps the relationshipItem.getRelationship() and its getFrom()/getTo() relationshipItems.
    Relationship relationship = RELATIONSHIP_ITEM_MAPPER.map(relationshipItem.getRelationship());
    relationshipItem.setRelationship(relationship);
      return relationshipItem;
  }
}
