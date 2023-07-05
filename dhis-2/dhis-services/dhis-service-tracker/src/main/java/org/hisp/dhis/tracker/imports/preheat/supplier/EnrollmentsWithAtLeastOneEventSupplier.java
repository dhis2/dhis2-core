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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * This supplier adds to the pre-heat object a List of all Enrollment UIDs that have at least ONE
 * Program Stage Instance that is not logically deleted ('deleted = true').
 *
 * @author Luciano Fiandesio
 */
@Component
public class EnrollmentsWithAtLeastOneEventSupplier extends JdbcAbstractPreheatSupplier {
  private static final String COLUMN = "uid";

  private static final String SQL =
      "select  "
          + COLUMN
          + " from programinstance "
          + "where exists( select programstageinstanceid "
          + "from programstageinstance "
          + "where programinstance.programinstanceid = programstageinstance.programinstanceid "
          + "and programinstance.deleted = false) "
          + "and programinstanceid in (:ids)";

  protected EnrollmentsWithAtLeastOneEventSupplier(JdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  @Override
  public void preheatAdd(TrackerImportParams params, TrackerPreheat preheat) {
    final Map<String, Enrollment> enrollments = preheat.getEnrollments();
    List<Long> programStageIds =
        enrollments.values().stream().map(IdentifiableObject::getId).collect(Collectors.toList());

    if (!programStageIds.isEmpty()) {
      List<String> uids = new ArrayList<>();

      MapSqlParameterSource parameters = new MapSqlParameterSource();
      parameters.addValue("ids", programStageIds);
      jdbcTemplate.query(
          SQL,
          parameters,
          rs -> {
            uids.add(rs.getString(COLUMN));
          });
      preheat.setEnrollmentsWithOneOrMoreNonDeletedEvent(uids);
    }
  }
}
