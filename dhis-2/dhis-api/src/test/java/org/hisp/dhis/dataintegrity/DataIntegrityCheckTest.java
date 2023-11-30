/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dataintegrity;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Jan Bernitt
 */
class DataIntegrityCheckTest {

  @Test
  void testSortingFastToSlow_NoExecutionTime() {
    List<DataIntegrityCheck> checks =
        List.of(
            DataIntegrityCheck.builder().name("a").isSlow(true).build(),
            DataIntegrityCheck.builder().name("b").isSlow(false).build(),
            DataIntegrityCheck.builder().name("c").isSlow(true).build());
    List<DataIntegrityCheck> actual =
        checks.stream().sorted(DataIntegrityCheck.FAST_TO_SLOW).collect(toList());
    assertEquals(
        List.of("b", "a", "c"), actual.stream().map(DataIntegrityCheck::getName).collect(toList()));
  }

  @Test
  void testSortingFastToSlow() {
    List<DataIntegrityCheck> checks =
        List.of(
            DataIntegrityCheck.builder().name("a").isSlow(true).build().addExecution(12345),
            DataIntegrityCheck.builder().name("b").isSlow(false).build().addExecution(20),
            DataIntegrityCheck.builder().name("c").isSlow(true).build().addExecution(500));
    List<DataIntegrityCheck> actual =
        checks.stream().sorted(DataIntegrityCheck.FAST_TO_SLOW).collect(toList());
    assertEquals(
        List.of("b", "c", "a"), actual.stream().map(DataIntegrityCheck::getName).collect(toList()));
  }

  @Test
  void testSortingFastToSlow_MixedExecutionTime() {
    List<DataIntegrityCheck> checks =
        List.of(
            DataIntegrityCheck.builder().name("a").isSlow(true).build().addExecution(12345),
            DataIntegrityCheck.builder().name("b").isSlow(false).build(),
            DataIntegrityCheck.builder().name("c").isSlow(true).build().addExecution(500));
    List<DataIntegrityCheck> actual =
        checks.stream().sorted(DataIntegrityCheck.FAST_TO_SLOW).collect(toList());
    assertEquals(
        List.of("c", "b", "a"), actual.stream().map(DataIntegrityCheck::getName).collect(toList()));
  }

  @Test
  void testAddExecution() {
    DataIntegrityCheck check = DataIntegrityCheck.builder().name("a").build();

    check.addExecution(1000).addExecution(1000); // sum is 2000, 2 times

    assertEquals(1000L, check.getAverageExecutionTime());

    check.addExecution(4000); // sum is now 6000, 3 times

    assertEquals(2000L, check.getAverageExecutionTime());
  }
}
