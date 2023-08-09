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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentsExportControllerUnitTest {

  @Mock private EnrollmentService enrollmentService;

  @Mock private EnrollmentRequestParamsMapper paramsMapper;

  @Mock private FieldFilterService fieldFilterService;

  @Mock private EnrollmentFieldsParamMapper fieldsMapper;

  @Test
  void shouldFailInstantiatingControllerIfAnyOrderableFieldIsUnsupported() {
    // pretend the service does not support 2 of the orderable fields the web advocates
    Iterator<Entry<String, String>> iterator =
        EnrollmentMapper.ORDERABLE_FIELDS.entrySet().stream().iterator();
    Entry<String, String> missing1 = iterator.next();
    Entry<String, String> missing2 = iterator.next();
    Map<String, String> orderableFields = new HashMap<>(EnrollmentMapper.ORDERABLE_FIELDS);
    orderableFields.remove(missing1.getKey());
    orderableFields.remove(missing2.getKey());
    when(enrollmentService.getOrderableFields())
        .thenReturn(new HashSet<>(orderableFields.values()));

    Exception exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                new EnrollmentsExportController(
                    enrollmentService, paramsMapper, fieldFilterService, fieldsMapper));

    assertAll(
        () ->
            assertStartsWith("enrollment controller supports ordering by", exception.getMessage()),
        () -> assertContains(missing1.getKey(), exception.getMessage()),
        () -> assertContains(missing2.getKey(), exception.getMessage()));
  }
}
