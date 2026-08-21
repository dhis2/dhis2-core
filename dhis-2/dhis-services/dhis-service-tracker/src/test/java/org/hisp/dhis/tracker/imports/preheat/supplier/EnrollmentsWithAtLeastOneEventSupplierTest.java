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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;

/**
 * A program without registration (single event) has a synthetic {@link
 * org.hisp.dhis.program.Enrollment} that {@link EnrollmentSupplier} preheats wholesale for every
 * import touching that program, regardless of whether the import payload carries any enrollment at
 * all. This supplier must only query for enrollments that are actually part of the payload (the
 * only case its result, {@link
 * org.hisp.dhis.tracker.imports.validation.validator.enrollment.SecurityOwnershipValidator}'s
 * enrollment-delete check, is ever read) - not for every enrollment preheat happens to hold.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentsWithAtLeastOneEventSupplierTest {

  @Mock private JdbcTemplate jdbcTemplate;

  private EnrollmentsWithAtLeastOneEventSupplier supplier;

  private TrackerPreheat preheat;

  private org.hisp.dhis.program.Enrollment preheatedEnrollment;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    supplier = new EnrollmentsWithAtLeastOneEventSupplier(jdbcTemplate);

    preheatedEnrollment = new org.hisp.dhis.program.Enrollment();
    preheatedEnrollment.setId(42L);
    preheatedEnrollment.setUid("synthEnrol1");

    preheat = new TrackerPreheat();
    preheat.putEnrollment(preheatedEnrollment.getUid(), preheatedEnrollment);
  }

  @Test
  void doesNotQueryWhenPayloadHasNoEnrollments() {
    supplier.preheatAdd(TrackerObjects.builder().build(), preheat);

    verifyNoInteractions(jdbcTemplate);
    assertTrue(preheat.getEnrollmentsWithOneOrMoreNonDeletedEvent().isEmpty());
  }

  @Test
  void queriesOnlyForEnrollmentsPresentInThePayload() {
    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .enrollments(
                List.of(
                    org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
                        .enrollment(preheatedEnrollment.getUid())
                        .build()))
            .build();

    supplier.preheatAdd(trackerObjects, preheat);

    verify(jdbcTemplate).query(any(PreparedStatementCreator.class), any(RowCallbackHandler.class));
  }
}
