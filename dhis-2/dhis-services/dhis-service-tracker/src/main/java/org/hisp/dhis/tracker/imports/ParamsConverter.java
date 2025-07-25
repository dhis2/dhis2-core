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
package org.hisp.dhis.tracker.imports;

import java.util.List;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.SingleEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Luciano Fiandesio
 */
public class ParamsConverter {
  private ParamsConverter() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static TrackerBundle convert(
      TrackerImportParams params,
      TrackerObjects trackerObjects,
      UserDetails user,
      TrackerPreheat preheat) {

    return TrackerBundle.builder()
        .importMode(params.getImportMode())
        .importStrategy(params.getImportStrategy())
        .skipTextPatternValidation(params.isSkipPatternValidation())
        .skipSideEffects(params.isSkipSideEffects())
        .skipRuleEngine(params.isSkipRuleEngine())
        .flushMode(params.getFlushMode())
        .validationMode(params.getValidationMode())
        .trackedEntities(trackerObjects.getTrackedEntities())
        .enrollments(trackerObjects.getEnrollments())
        .trackerEvents(
            buildTrackerEvents(trackerObjects.getEvents(), preheat, params.getImportStrategy()))
        .singleEvents(
            buildSingleEvents(trackerObjects.getEvents(), preheat, params.getImportStrategy()))
        .relationships(trackerObjects.getRelationships())
        .user(user)
        .build();
  }

  private static List<TrackerEvent> buildTrackerEvents(
      List<Event> events, TrackerPreheat preheat, TrackerImportStrategy importStrategy) {
    if (importStrategy.isUpdateOrDelete()) {
      return events.stream()
          .filter(e -> preheat.getSingleEvent(e.getUid()) == null)
          .map(e -> (TrackerEvent) e)
          .toList();
    }
    return events.stream()
        .filter(
            e -> {
              Program program = preheat.getProgram(e.getProgram());
              ProgramStage programStage = preheat.getProgramStage(e.getProgramStage());
              return (program != null && program.isRegistration())
                  || (programStage != null && programStage.getProgram().isRegistration())
                  || (program == null && programStage == null);
            })
        .map(e -> (TrackerEvent) e)
        .toList();
  }

  private static List<SingleEvent> buildSingleEvents(
      List<Event> events, TrackerPreheat preheat, TrackerImportStrategy importStrategy) {
    if (importStrategy.isUpdateOrDelete()) {
      return events.stream()
          .filter(e -> preheat.getSingleEvent(e.getUid()) != null)
          .map(e -> SingleEvent.builderFromEvent(e).build())
          .toList();
    }
    return events.stream()
        .filter(
            e -> {
              Program program = preheat.getProgram(e.getProgram());
              ProgramStage programStage = preheat.getProgramStage(e.getProgramStage());
              return (program != null && program.isWithoutRegistration())
                  || (program == null
                      && programStage != null
                      && programStage.getProgram().isWithoutRegistration());
            })
        .map(e -> SingleEvent.builderFromEvent(e).build())
        .toList();
  }
}
