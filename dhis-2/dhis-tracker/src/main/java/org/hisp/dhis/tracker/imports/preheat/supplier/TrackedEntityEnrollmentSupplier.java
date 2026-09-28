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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.model.Enrollment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * Adds to the preheat a Map of Tracked Entities to related enrollments
 *
 * @author Luca Cambi
 */
@Component
public class TrackedEntityEnrollmentSupplier extends JdbcAbstractPreheatSupplier {

  private static final String PR_UID_COLUMN = "pr.uid";

  private static final String PR_UID_COLUMN_ALIAS = "pruid";

  private static final String PI_UID_COLUMN = "en.uid";

  private static final String PI_UID_COLUMN_ALIAS = "piuid";

  private static final String PI_STATUS_COLUMN = "en.status";

  private static final String PI_STATUS_COLUMN_ALIAS = "status";

  private static final String TE_UID_COLUMN = "te.uid";

  private static final String TE_UID_COLUMN_ALIAS = "teuid";

  private static final String SQL =
      "select  "
          + PR_UID_COLUMN
          + " as "
          + PR_UID_COLUMN_ALIAS
          + ", "
          + PI_UID_COLUMN
          + " as "
          + PI_UID_COLUMN_ALIAS
          + ", "
          + PI_STATUS_COLUMN
          + " as "
          + PI_STATUS_COLUMN_ALIAS
          + ", "
          + TE_UID_COLUMN
          + " as "
          + TE_UID_COLUMN_ALIAS
          + " from enrollment en "
          + " join trackedentity te on en.trackedentityid = te.trackedentityid "
          + " join program pr on pr.programid = en.programid "
          + " where en.deleted = false "
          + " and te.uid = any(:teuids)"
          + " and pr.uid = any(:pruids)";

  protected TrackedEntityEnrollmentSupplier(JdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  @Override
  public void preheatAdd(TrackerObjects trackerObjects, TrackerPreheat preheat) {
    if (trackerObjects.getEnrollments().isEmpty()) return;

    List<Program> programs = preheat.getAll(Program.class);
    if (programs.isEmpty()) return;

    String[] trackedEntityUids =
        trackerObjects.getEnrollments().stream()
            .map(org.hisp.dhis.tracker.imports.domain.Enrollment::getTrackedEntity)
            .map(UID::getValue)
            .toArray(String[]::new);
    String[] programUids = programs.stream().map(IdentifiableObject::getUid).toArray(String[]::new);

    Map<UID, List<Enrollment>> trackedEntityToEnrollmentMap = new HashMap<>();
    queryTeAndAddToMap(trackedEntityToEnrollmentMap, trackedEntityUids, programUids);

    preheat.setTrackedEntityToEnrollmentMap(trackedEntityToEnrollmentMap);
  }

  private void queryTeAndAddToMap(
      Map<UID, List<Enrollment>> trackedEntityToEnrollmentMap,
      String[] trackedEntityUids,
      String[] programUids) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("teuids", trackedEntityUids);
    parameters.addValue("pruids", programUids);

    jdbcTemplate.query(
        SQL,
        parameters,
        resultSet -> {
          UID te = UID.of(resultSet.getString(TE_UID_COLUMN_ALIAS));

          Enrollment newPi = new Enrollment();
          newPi.setUid(resultSet.getString(PI_UID_COLUMN_ALIAS));
          newPi.setStatus(EnrollmentStatus.valueOf(resultSet.getString(PI_STATUS_COLUMN_ALIAS)));

          Program program = new Program();
          program.setUid(resultSet.getString(PR_UID_COLUMN_ALIAS));
          newPi.setProgram(program);

          List<Enrollment> piList =
              trackedEntityToEnrollmentMap.getOrDefault(te, new ArrayList<>());

          piList.add(newPi);

          trackedEntityToEnrollmentMap.put(te, piList);
        });
  }
}
