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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.event.DataValue;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.EventDataValueAggregator;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.DataValueFilteringTestSupport;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
class FilteredDataValueCheckTest {

  private FilteredDataValueCheck dataValueCheck;

  @BeforeEach
  void setUp() {
    dataValueCheck = new FilteredDataValueCheck();
  }

  @Test
  void testNotLinkedDataElementsAreReported() {
    Event event = new Event();
    event.setProgramStage(DataValueFilteringTestSupport.PROGRAMSTAGE);
    HashSet<DataValue> dataValues =
        Sets.newHashSet(
            new DataValue(DataValueFilteringTestSupport.DATA_ELEMENT_1, "whatever"),
            new DataValue(DataValueFilteringTestSupport.DATA_ELEMENT_2, "another value"));
    event.setDataValues(dataValues);
    WorkContext ctx =
        WorkContext.builder()
            .importOptions(ImportOptions.getDefaultImportOptions())
            .programsMap(DataValueFilteringTestSupport.getProgramMap())
            .eventDataValueMap(
                new EventDataValueAggregator()
                    .aggregateDataValues(
                        List.of(event),
                        Collections.emptyMap(),
                        ImportOptions.getDefaultImportOptions()))
            .build();
    ImportSummary importSummary = dataValueCheck.check(new ImmutableEvent(event), ctx);
    assertEquals(ImportStatus.WARNING, importSummary.getStatus());
  }
}
