/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import java.util.Collections;
import javax.annotation.Nonnull;
import org.hibernate.Session;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.converter.TrackerConverterService;
import org.hisp.dhis.tracker.imports.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class TrackedEntityPersister
    extends AbstractTrackerPersister<
        org.hisp.dhis.tracker.imports.domain.TrackedEntity, TrackedEntity> {
  @Nonnull
  private final TrackerConverterService<
          org.hisp.dhis.tracker.imports.domain.TrackedEntity, TrackedEntity>
      teConverter;

  public TrackedEntityPersister(
      ReservedValueService reservedValueService,
      TrackerConverterService<org.hisp.dhis.tracker.imports.domain.TrackedEntity, TrackedEntity>
          teConverter,
      TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService) {
    super(reservedValueService, trackedEntityAttributeValueAuditService);
    this.teConverter = teConverter;
  }

  @Override
  protected void updateAttributes(
      Session session,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackerDto,
      TrackedEntity te) {
    handleTrackedEntityAttributeValues(session, preheat, trackerDto.getAttributes(), te);
  }

  @Override
  protected void updateDataValues(
      Session session,
      TrackerPreheat preheat,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackerDto,
      TrackedEntity te) {
    // DO NOTHING - TE HAVE NO DATA VALUES
  }

  @Override
  protected void persistComments(TrackerPreheat preheat, TrackedEntity trackedEntity) {
    // DO NOTHING - TE HAVE NO COMMENTS
  }

  @Override
  protected void updatePreheat(TrackerPreheat preheat, TrackedEntity dto) {
    preheat.putTrackedEntities(Collections.singletonList(dto));
  }

  @Override
  protected TrackedEntity convert(
      TrackerBundle bundle, org.hisp.dhis.tracker.imports.domain.TrackedEntity trackerDto) {
    return teConverter.from(bundle.getPreheat(), trackerDto);
  }

  @Override
  protected TrackerType getType() {
    return TrackerType.TRACKED_ENTITY;
  }

  @Override
  protected boolean isNew(TrackerPreheat preheat, String uid) {
    return preheat.getTrackedEntity(uid) == null;
  }

  @Override
  protected TrackerSideEffectDataBundle handleSideEffects(
      TrackerBundle bundle, TrackedEntity entity) {
    return TrackerSideEffectDataBundle.builder().build();
  }

  @Override
  protected void persistOwnership(TrackerPreheat preheat, TrackedEntity entity) {
    // DO NOTHING, Tei alone does not have ownership records

  }

  @Override
  protected String getUpdatedTrackedEntity(TrackedEntity entity) {
    return null; // We don't need to keep track, Tei has already been
    // updated
  }
}
