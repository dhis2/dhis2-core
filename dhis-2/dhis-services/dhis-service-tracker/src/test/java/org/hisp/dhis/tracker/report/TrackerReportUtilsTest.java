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
package org.hisp.dhis.tracker.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrackerReportUtilsTest {

  private TrackerBundle bundle;

  @BeforeEach
  void setUp() {
    bundle = TrackerBundle.builder().build();
  }

  @Test
  void buildArgumentListShouldTurnInstantIntoArgument() {
    final Instant now = Instant.now();
    List<String> args = TrackerReportUtils.buildArgumentList(bundle, Arrays.asList(now));
    assertThat(args.size(), is(1));
    assertThat(args.get(0), is(DateUtils.getIso8601NoTz(DateUtils.fromInstant(now))));
  }

  @Test
  void buildArgumentListShouldTurnDateIntoArgument() {
    final Date now = Date.from(Instant.now());
    List<String> args = TrackerReportUtils.buildArgumentList(bundle, Arrays.asList(now));
    assertThat(args.size(), is(1));
    assertThat(args.get(0), is(DateFormat.getInstance().format(now)));
  }

  @Test
  void buildArgumentListShouldTurnStringsIntoArguments() {
    List<String> args = TrackerReportUtils.buildArgumentList(bundle, Arrays.asList("foo", "faa"));
    assertThat(args, contains("foo", "faa"));
  }
}
