/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.trackedentity.query.context.querybuilder.OffsetHelper.Offset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests for {@link OffsetHelper}. */
class OffsetHelperTest {

  @ParameterizedTest(name = "testGetItemBasedOnOffset - {index}")
  @CsvSource({"2,d", "1,e", "0,a", "-1,b", "-2,c"})
  void testGetItemBasedOnOffset(String offsetParam, String expectedResponse) {
    // Given
    Stream<String> stream = Stream.of("a", "b", "c", "d", "e");
    Comparator<String> comparator = Comparator.naturalOrder();
    int offset = Integer.parseInt(offsetParam);

    // When
    Optional<String> result = OffsetHelper.getItemBasedOnOffset(stream, comparator, offset);

    // Then
    Assertions.assertTrue(result.isPresent());
    Assertions.assertEquals(expectedResponse, result.get());
  }

  @ParameterizedTest
  @CsvSource({"1,1,asc", "2,2,asc", "0,1,desc", "-1,2,desc", "-2,3,desc"})
  void testGetOffset(String offsetParam, String expectedOffset, String expectedDirection) {
    // When
    Offset offset = OffsetHelper.getOffset(Integer.parseInt(offsetParam));

    // Then
    Assertions.assertEquals(expectedOffset, offset.offset());
    Assertions.assertEquals(expectedDirection, offset.direction());
  }
}
