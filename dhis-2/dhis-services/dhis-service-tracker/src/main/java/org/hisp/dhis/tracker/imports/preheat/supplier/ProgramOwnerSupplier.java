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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerStore;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.mappers.OrganisationUnitMapper;
import org.springframework.stereotype.Component;

/**
 * @author Ameen Mohamed
 */
@RequiredArgsConstructor
@Component
public class ProgramOwnerSupplier extends AbstractPreheatSupplier {
  @Nonnull private final TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore;

  @Override
  public void preheatAdd(TrackerObjects trackerObjects, TrackerPreheat preheat) {
    final Map<UID, TrackedEntity> preheatedTrackedEntities = preheat.getTrackedEntities();
    final Map<UID, Enrollment> preheatedEnrollments = preheat.getEnrollments();
    final Map<UID, TrackerEvent> preheatedEvents = preheat.getTrackerEvents();
    Set<Long> teIds = new HashSet<>();
    for (org.hisp.dhis.tracker.imports.domain.Enrollment en : trackerObjects.getEnrollments()) {
      Enrollment enrollment = preheatedEnrollments.get(en.getEnrollment());
      TrackedEntity te =
          enrollment == null
              ? preheatedTrackedEntities.get(en.getTrackedEntity())
              : enrollment.getTrackedEntity();

      if (te != null) {
        teIds.add(te.getId());
      }
    }

    for (org.hisp.dhis.tracker.imports.domain.TrackerEvent ev : trackerObjects.getEvents()) {
      TrackerEvent event = preheatedEvents.get(ev.getEvent());
      Enrollment enrollment =
          event == null ? preheatedEnrollments.get(ev.getEnrollment()) : event.getEnrollment();
      if (enrollment != null && enrollment.getTrackedEntity() != null) {
        teIds.add(enrollment.getTrackedEntity().getId());
      }
    }
    List<TrackedEntityProgramOwnerOrgUnit> tepos =
        trackedEntityProgramOwnerStore.getTrackedEntityProgramOwnerOrgUnits(teIds);
    tepos =
        tepos.stream()
            .map(
                tepo ->
                    new TrackedEntityProgramOwnerOrgUnit(
                        tepo.getTrackedEntityId(),
                        tepo.getProgramId(),
                        OrganisationUnitMapper.INSTANCE.map(tepo.getOrganisationUnit())))
            .toList();

    preheat.addProgramOwners(tepos);
  }
}
