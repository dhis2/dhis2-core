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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackedEntitiesExportControllerUnitTest {
  @Mock private TrackedEntityService trackedEntityService;

  @Test
  void shouldFailInstantiatingControllerIfAnyOrderableFieldIsUnsupported() {
    // pretend the service does not support 2 of the orderable fields the web advocates
    Iterator<Entry<String, String>> iterator =
        TrackedEntityMapper.ORDERABLE_FIELDS.entrySet().stream().iterator();
    Entry<String, String> missing1 = iterator.next();
    Entry<String, String> missing2 = iterator.next();
    Map<String, String> orderableFields = new HashMap<>(TrackedEntityMapper.ORDERABLE_FIELDS);
    orderableFields.remove(missing1.getKey());
    orderableFields.remove(missing2.getKey());
    when(trackedEntityService.getOrderableFields())
        .thenReturn(new HashSet<>(orderableFields.values()));

    Exception exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                new TrackedEntitiesExportController(
                    trackedEntityService, null, null, null, null, null));

    assertAll(
        () ->
            assertStartsWith(
                "tracked entity controller supports ordering by", exception.getMessage()),
        () -> assertContains(missing1.getKey(), exception.getMessage()),
        () -> assertContains(missing2.getKey(), exception.getMessage()));
  }
}
