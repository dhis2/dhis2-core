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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RepeatableStageParams}. */
class RepeatableStageParamsTest {

  @Test
  void equalityTest() {
    // arrange
    Set<RepeatableStageParams> repeatableStageParamsSet = new HashSet<>();

    RepeatableStageParams repeatableStageParams1 = new RepeatableStageParams();
    RepeatableStageParams repeatableStageParams2 = new RepeatableStageParams();
    RepeatableStageParams repeatableStageParams3 = new RepeatableStageParams();
    RepeatableStageParams repeatableStageParams4 = new RepeatableStageParams();
    RepeatableStageParams repeatableStageParams5 = new RepeatableStageParams();

    repeatableStageParams1.setIndex(0);
    repeatableStageParams1.setStartDate(DateUtils.parseDate("2022-01-01"));
    repeatableStageParams1.setEndDate(DateUtils.parseDate("2022-01-31"));

    repeatableStageParams2.setIndex(0);
    repeatableStageParams2.setStartDate(DateUtils.parseDate("2022-01-01"));
    repeatableStageParams2.setEndDate(DateUtils.parseDate("2022-01-31"));

    repeatableStageParams3.setIndex(10);
    repeatableStageParams3.setStartDate(DateUtils.parseDate("2022-03-01"));
    repeatableStageParams3.setEndDate(DateUtils.parseDate("2022-03-31"));

    repeatableStageParams4.setIndex(0);
    repeatableStageParams5.setIndex(0);

    // assert
    assertTrue(repeatableStageParamsSet.add(repeatableStageParams1));
    assertFalse(repeatableStageParamsSet.add(repeatableStageParams2));
    assertTrue(repeatableStageParamsSet.add(repeatableStageParams3));
    assertTrue(repeatableStageParamsSet.add(repeatableStageParams4));
    assertFalse(repeatableStageParamsSet.add(repeatableStageParams5));

    assertEquals(0, repeatableStageParams1.getIndex());
    assertEquals(DateUtils.parseDate("2022-01-01"), repeatableStageParams1.getStartDate());
    assertEquals(DateUtils.parseDate("2022-01-31"), repeatableStageParams1.getEndDate());

    assertEquals(repeatableStageParams1, repeatableStageParams2);
  }
}
