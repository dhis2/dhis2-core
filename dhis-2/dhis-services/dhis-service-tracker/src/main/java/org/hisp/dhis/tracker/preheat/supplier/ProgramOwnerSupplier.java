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
package org.hisp.dhis.tracker.preheat.supplier;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerStore;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.mappers.OrganisationUnitMapper;
import org.springframework.stereotype.Component;

/**
 * @author Ameen Mohamed
 */
@RequiredArgsConstructor
@Component
public class ProgramOwnerSupplier extends AbstractPreheatSupplier {
  @Nonnull private final TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore;

  @Override
  public void preheatAdd(TrackerImportParams params, TrackerPreheat preheat) {
    final Map<String, TrackedEntityInstance> preheatedTrackedEntities =
        preheat.getTrackedEntities();
    final Map<String, ProgramInstance> preheatedEnrollments = preheat.getEnrollments();
    Set<Long> teiIds = new HashSet<>();
    for (Enrollment en : params.getEnrollments()) {
      TrackedEntityInstance tei = preheatedTrackedEntities.get(en.getTrackedEntity());
      if (tei != null) {
        teiIds.add(tei.getId());
      }
    }

    for (Event ev : params.getEvents()) {
      ProgramInstance pi = preheatedEnrollments.get(ev.getEnrollment());
      if (pi != null && pi.getEntityInstance() != null) {
        teiIds.add(pi.getEntityInstance().getId());
      }
    }

    List<TrackedEntityProgramOwnerOrgUnit> tepos =
        trackedEntityProgramOwnerStore.getTrackedEntityProgramOwnerOrgUnits(teiIds);

    tepos =
        tepos.stream()
            .map(
                tepo ->
                    new TrackedEntityProgramOwnerOrgUnit(
                        tepo.getTrackedEntityInstanceId(),
                        tepo.getProgramId(),
                        OrganisationUnitMapper.INSTANCE.map(tepo.getOrganisationUnit())))
            .collect(Collectors.toList());

    preheat.addProgramOwners(tepos);
  }
}
