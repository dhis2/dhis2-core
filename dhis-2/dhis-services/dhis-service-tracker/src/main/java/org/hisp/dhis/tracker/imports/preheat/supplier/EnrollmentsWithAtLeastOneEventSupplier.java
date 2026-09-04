/*
 * Copyright (c) 2004-2026, University of Oslo
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
import java.util.Objects;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * This supplier adds to the pre-heat object a List of all Enrollment UIDs that have at least ONE
 * Program Stage Instance that is not logically deleted ('deleted = true').
 *
 * <p>Only enrollments present in the import payload are checked: {@link
 * org.hisp.dhis.tracker.imports.validation.validator.enrollment.SecurityOwnershipValidator} is the
 * sole reader of this result, and it only exists for {@code Enrollment} tracker objects in the
 * payload. Sourcing from {@link TrackerPreheat#getEnrollments()} instead would also query for every
 * synthetic enrollment {@code EnrollmentSupplier} preheats for a program without registration, even
 * on imports that carry no enrollment at all (e.g. a single event import).
 *
 * @author Luciano Fiandesio
 */
@Component
public class EnrollmentsWithAtLeastOneEventSupplier extends JdbcAbstractPreheatSupplier {
  private static final String COLUMN = "uid";

  private static final String SQL =
      "select  "
          + COLUMN
          + " from enrollment "
          + "where exists( select eventid "
          + "from event "
          + "where enrollment.enrollmentid = event.enrollmentid "
          + "and enrollment.deleted = false) "
          + "and enrollmentid in (:ids)";

  protected EnrollmentsWithAtLeastOneEventSupplier(JdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  @Override
  public void preheatAdd(TrackerObjects trackerObjects, TrackerPreheat preheat) {
    List<Long> enrollmentIds =
        trackerObjects.getEnrollments().stream()
            .map(e -> preheat.getEnrollment(e.getUid()))
            .filter(Objects::nonNull)
            .map(IdentifiableObject::getId)
            .toList();

    if (!enrollmentIds.isEmpty()) {
      List<String> uids = new ArrayList<>();

      MapSqlParameterSource parameters = new MapSqlParameterSource();
      parameters.addValue("ids", enrollmentIds);
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
