/*
 * Copyright (c) 2004-2025, University of Oslo
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.mappers.ProgramStageMapper;
import org.springframework.stereotype.Component;

/**
 * Adds to the preheat cache all {@code ProgramStage}s that may be scheduled as a result of {@link
 * ProgramRuleActionType#CREATEEVENT} program rule actions.
 *
 * @author Zubair Asghar
 */
@RequiredArgsConstructor
@Component
public class ScheduledEventProgramStageSupplier extends AbstractPreheatSupplier {
  @Nonnull private final EntityManager entityManager;

  @Override
  public void preheatAdd(TrackerObjects trackerObjects, TrackerPreheat preheat) {
    List<String> programStageUids = getProgramStageIds(preheat);
    if (programStageUids.isEmpty()) {
      return;
    }

    getProgramStages(programStageUids).stream()
        .map(ProgramStageMapper.INSTANCE::map)
        .filter(Objects::nonNull)
        .forEach(preheat::put);
  }

  private List<ProgramStage> getProgramStages(List<String> programStageUids) {
    TypedQuery<ProgramStage> query =
        entityManager.createQuery(
            "select ps from ProgramStage ps where ps.uid in :uids", ProgramStage.class);
    query.setParameter("uids", programStageUids);
    return query.getResultList();
  }

  private List<String> getProgramStageIds(TrackerPreheat preheat) {
    List<Program> programs = preheat.getAll(Program.class);
    if (programs.isEmpty()) {
      return Collections.emptyList();
    }

    Set<String> programUids = programs.stream().map(Program::getUid).collect(Collectors.toSet());

    String jpql =
        """
            select distinct ps.uid
            from ProgramRuleAction pra
            join pra.programStage ps
            join pra.programRule pr
            join pr.program p
            where pra.programRuleActionType = :actionType and pra.programStage is not null
            and p.uid in :programUids
        """;

    TypedQuery<String> query = entityManager.createQuery(jpql, String.class);
    query.setParameter("programUids", programUids);
    query.setParameter("actionType", ProgramRuleActionType.CREATEEVENT);

    return query.getResultList();
  }
}
