/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static java.util.Map.entry;

import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.webapi.controller.tracker.view.InstantMapper;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.hisp.dhis.webapi.controller.tracker.view.RelationshipItem;
import org.hisp.dhis.webapi.controller.tracker.view.UIDMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = {RelationshipItemMapper.class, UIDMapper.class, InstantMapper.class})
public abstract class RelationshipMapper {

  /**
   * Relationships can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.relationship.Relationship}.
   */
  static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(entry("createdAt", "created"), entry("createdAtClient", "createdAtClient"));

  @Mapping(target = "relationship", source = "uid")
  @Mapping(target = "relationshipType", source = "relationshipType.uid")
  @Mapping(target = "relationshipName", source = "relationshipType.name")
  @Mapping(target = "bidirectional", source = "relationshipType.bidirectional")
  @Mapping(target = "createdAt", source = "created")
  @Mapping(target = "createdAtClient", source = "createdAtClient")
  @Mapping(target = "updatedAt", source = "lastUpdated")
  public abstract Relationship map(org.hisp.dhis.relationship.Relationship relationship);

  /**
   * Maps a {@link org.hisp.dhis.relationship.RelationshipItem} to a {@link
   * org.hisp.dhis.relationship.Relationship} which is then mapped by {@link
   * #map(org.hisp.dhis.relationship.Relationship)}.
   */
  public org.hisp.dhis.relationship.Relationship map(
      org.hisp.dhis.relationship.RelationshipItem relationshipItem) {
    if (relationshipItem == null) {
      return null;
    }

    return relationshipItem.getRelationship();
  }

  @AfterMapping
  protected Relationship afterMapping(
      @MappingTarget Relationship.RelationshipBuilder builder,
      org.hisp.dhis.relationship.Relationship relationshipDb) {
    Relationship relationship = builder.build();
    RelationshipItem from = relationship.getFrom();
    RelationshipItem to = relationship.getTo();

    RelationshipType relationshipType = relationshipDb.getRelationshipType();

    RelationshipConstraint fromConstraint = relationshipType.getFromConstraint();
    RelationshipConstraint toConstraint = relationshipType.getToConstraint();

    return builder
        .from(withConstraints(from, fromConstraint))
        .to(withConstraints(to, toConstraint))
        .build();
  }

  private RelationshipItem withConstraints(
      RelationshipItem item, RelationshipConstraint constraint) {

    // nothing to check
    if (item == null || constraint == null || constraint.getTrackerDataView() == null) {
      return item;
    }

    if (item.getTrackedEntity() != null) {
      item.getTrackedEntity()
          .setAttributes(
              item.getTrackedEntity().getAttributes().stream()
                  .filter(
                      a ->
                          constraint
                              .getTrackerDataView()
                              .getAttributes()
                              .contains(a.getAttribute()))
                  .toList());
      return item;
    }

    if (item.getEnrollment() != null) {
      item.getEnrollment()
          .setAttributes(
              item.getEnrollment().getAttributes().stream()
                  .filter(
                      a ->
                          constraint
                              .getTrackerDataView()
                              .getAttributes()
                              .contains(a.getAttribute()))
                  .toList());
      return item;
    }

    if (item.getEvent() != null) {
      item.getEvent()
          .setDataValues(
              item.getEvent().getDataValues().stream()
                  .filter(
                      d ->
                          constraint
                              .getTrackerDataView()
                              .getDataElements()
                              .contains(d.getDataElement()))
                  .collect(Collectors.toSet()));
      return item;
    }

    return item;
  }
}
