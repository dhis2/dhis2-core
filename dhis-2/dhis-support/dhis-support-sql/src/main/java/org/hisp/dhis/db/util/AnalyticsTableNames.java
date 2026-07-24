/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.db.util;

import java.util.Locale;
import java.util.Objects;
import org.hisp.dhis.program.Program;

/**
 * Constructs the names of program-scoped analytics tables ({@code analytics_event_<programUid>},
 * {@code analytics_enrollment_<programUid>}).
 *
 * <p>The program UID is lowercased with {@link Locale#ROOT} so the returned name matches the
 * physical analytics table on every supported engine, including those that preserve identifier case
 * (ClickHouse). Use this helper at every emission site instead of concatenating the prefix inline.
 */
public final class AnalyticsTableNames {

  public static final String EVENT_PREFIX = "analytics_event_";

  public static final String ENROLLMENT_PREFIX = "analytics_enrollment_";

  private AnalyticsTableNames() {}

  /** Returns the analytics event table name for the given program. */
  public static String eventTable(Program program) {
    Objects.requireNonNull(program, "program");
    return EVENT_PREFIX + program.getUid().toLowerCase(Locale.ROOT);
  }

  /** Returns the analytics enrollment table name for the given program. */
  public static String enrollmentTable(Program program) {
    Objects.requireNonNull(program, "program");
    return ENROLLMENT_PREFIX + program.getUid().toLowerCase(Locale.ROOT);
  }
}
