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
package org.hisp.dhis.tracker.imports.bundle.persister;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.fileresource.FileResourceStore;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.bundle.TrackerObjectsMapper;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class RelationshipPersister
    extends AbstractTrackerPersister<Relationship, org.hisp.dhis.tracker.model.Relationship> {

  public RelationshipPersister(
      DataSource dataSource, FileResourceStore fileResourceStore, ObjectMapper objectMapper) {
    super(dataSource, fileResourceStore, objectMapper);
  }

  @Override
  protected String sequenceName() {
    // The 2 RelationshipItem ids per Relationship are allocated separately from
    // relationshipitem_sequence inside EntityWriteBatch.insertRelationshipItems.
    return "relationship_sequence";
  }

  @Override
  protected void assignId(org.hisp.dhis.tracker.model.Relationship entity, long id) {
    entity.setId(id);
  }

  @Override
  protected void stageInsert(
      org.hisp.dhis.tracker.model.Relationship entity, EntityWriteBatch batch) {
    batch.stageInsert(entity);
  }

  @Override
  protected void stageUpdate(
      org.hisp.dhis.tracker.model.Relationship entity, EntityWriteBatch batch) {
    throw new UnsupportedOperationException("Relationships are not updated");
  }

  @Override
  protected org.hisp.dhis.tracker.model.Relationship convert(
      TrackerBundle bundle, Relationship trackerDto) {
    if (bundle.getStrategy(trackerDto) == TrackerImportStrategy.UPDATE) {
      return null;
    }

    return TrackerObjectsMapper.map(bundle.getPreheat(), trackerDto, bundle.getUser());
  }

  @Override
  protected void updateAttributes(
      TrackerPreheat preheat,
      Relationship trackerDto,
      org.hisp.dhis.tracker.model.Relationship hibernateEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs,
      EntityWriteBatch batch,
      Map<Long, Map<MetadataIdentifier, TrackedEntityAttributeValue>> existingAttributeValues) {
    // NOTHING TO DO
  }

  @Override
  protected void updatePreheat(
      TrackerPreheat preheat, org.hisp.dhis.tracker.model.Relationship convertedDto) {
    // NOTHING TO DO
  }

  @Override
  protected TrackerType getType() {
    return TrackerType.RELATIONSHIP;
  }

  @Override
  protected List<Relationship> getByType(TrackerBundle bundle) {
    return bundle.getRelationships();
  }

  @Override
  protected void persistOwnership(
      TrackerBundle bundle,
      Relationship trackerDto,
      org.hisp.dhis.tracker.model.Relationship entity,
      EntityWriteBatch batch) {
    // NOTHING TO DO

  }

  @Override
  protected void updateDataValues(
      TrackerPreheat preheat,
      Relationship trackerDto,
      org.hisp.dhis.tracker.model.Relationship payloadEntity,
      org.hisp.dhis.tracker.model.Relationship currentEntity,
      UserDetails user,
      ChangeLogAccumulator changeLogs) {
    // DO NOTHING - RELATIONSHIPS HAVE NO DATA VALUES
  }

  @Override
  protected Set<UID> getUpdatedTrackedEntities(org.hisp.dhis.tracker.model.Relationship entity) {
    return entity.getTrackedEntityOrigins();
  }

  @Override
  protected org.hisp.dhis.tracker.model.Relationship cloneEntityProperties(
      TrackerPreheat preheat, Relationship trackerDto) {
    return null;
    // NO NEED TO CLONE RELATIONSHIP PROPERTIES
  }
}
