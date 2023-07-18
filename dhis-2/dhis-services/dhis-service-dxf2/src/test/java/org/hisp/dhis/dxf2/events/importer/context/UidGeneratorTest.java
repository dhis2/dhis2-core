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
package org.hisp.dhis.dxf2.events.importer.context;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.events.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class UidGeneratorTest {

  private UidGenerator subject;

  @BeforeEach
  void setUp() {
    subject = new UidGenerator();
  }

  @Test
  void verifyEventsGetUidAssigned() {
    final String uid = CodeGenerator.generateUid();
    // Given
    Event event1 = new Event();
    Event event2 = new Event();
    Event event3 = new Event();
    Event event4 = new Event();
    event4.setEvent(uid);
    // When
    List<Event> events =
        subject.assignUidToEvents(Lists.newArrayList(event1, event2, event3, event4));
    // Then
    assertThat(events, hasSize(4));
    events.forEach(e -> assertNotNull(e.getUid()));
    // make sure we got 4 distinct UIDs
    assertThat(
        events.stream().collect(Collectors.toMap(Event::getUid, Function.identity())).keySet(),
        hasSize(4));
    // make sure Event4 has retained the original UID
    assertThat(event4.getUid(), is(uid));
    assertThat(event4.getEvent(), is(uid));
  }
}
