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
package org.hisp.dhis.dxf2.events.importer.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.dxf2.events.importer.EventTestUtils.createBaseEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.ServiceDelegator;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseValidationTest {
  protected final IdScheme programStageIdScheme =
      ImportOptions.getDefaultImportOptions().getIdSchemes().getProgramStageIdScheme();

  @Mock protected WorkContext workContext;

  @Mock protected ServiceDelegator serviceDelegator;

  protected Event event;

  protected Map<String, DataElement> dataElementMap = new HashMap<>();

  protected Map<String, Set<EventDataValue>> eventDataValueMap = new HashMap<>();

  protected ObjectMapper objectMapper = new ObjectMapper();

  @Mock protected ProgramInstanceStore programInstanceStore;

  @BeforeEach
  public void superSetUp() {
    event = createBaseEvent();
    when(workContext.getImportOptions()).thenReturn(ImportOptions.getDefaultImportOptions());
    when(workContext.getDataElementMap()).thenReturn(dataElementMap);
    when(workContext.getEventDataValueMap()).thenReturn(eventDataValueMap);
    when(workContext.getServiceDelegator()).thenReturn(serviceDelegator);

    // Service delegator
    when(serviceDelegator.getJsonMapper()).thenReturn(objectMapper);
    when(serviceDelegator.getProgramInstanceStore()).thenReturn(programInstanceStore);
  }

  protected void assertNoError(ImportSummary summary) {
    assertThat(summary.getStatus(), is(ImportStatus.SUCCESS));
    assertThat(summary, is(notNullValue()));
    assertThat(
        "Expecting 0 events ignored, but got " + summary.getImportCount().getIgnored(),
        summary.getImportCount().getIgnored(),
        is(0));
  }

  protected void assertHasError(ImportSummary summary, Event event, String description) {
    assertThat(summary.getStatus(), is(ImportStatus.ERROR));
    assertThat(summary, is(notNullValue()));
    assertThat(summary.getImportCount().getIgnored(), is(1));
    assertThat(summary.getReference(), is(event.getUid()));
    assertThat(summary.getDescription(), is(description));
  }

  protected static void assertHasConflict(
      ImportSummary summary, String expectedValue, String expectedObject) {
    assertEquals(1, summary.getConflictCount());
    ImportConflict conflict = summary.getConflicts().iterator().next();
    assertEquals(expectedValue, conflict.getValue());
    assertEquals(expectedObject, conflict.getObject());
  }

  protected static void assertHasConflict(ImportConflicts summary, Event event, String conflict) {
    if (!summary.hasConflict(c -> c.getValue().equals(conflict))) {
      fail("Conflict string [" + conflict + "] not found");
    }
  }

  protected DataElement addToDataElementMap(DataElement de) {
    this.dataElementMap.put(de.getUid(), de);
    return de;
  }

  protected void addToDataValueMap(String eventUid, EventDataValue... eventDataValue) {
    this.eventDataValueMap.put(eventUid, new HashSet<>(Arrays.asList(eventDataValue)));
  }
}
