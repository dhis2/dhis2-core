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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;

class RepeatableStageParamsTest {
  @Test
  void equalityTest() {
    // arrange
    RepeatableStageParams repeatableStageParams_1 = new RepeatableStageParams();
    RepeatableStageParams repeatableStageParams_2 = new RepeatableStageParams();
    RepeatableStageParams repeatableStageParams_3 = new RepeatableStageParams();
    RepeatableStageParams repeatableStageParams_4 = new RepeatableStageParams();
    RepeatableStageParams repeatableStageParams_5 = new RepeatableStageParams();
    RepeatableStageParams repeatableStageParams_6 = new RepeatableStageParams();

    repeatableStageParams_1.setStartIndex(0);
    repeatableStageParams_1.setCount(100);
    repeatableStageParams_1.setStartDate(DateUtils.parseDate("2022-01-01"));
    repeatableStageParams_1.setEndDate(DateUtils.parseDate("2022-01-31"));

    repeatableStageParams_2.setStartIndex(0);
    repeatableStageParams_2.setCount(100);
    repeatableStageParams_2.setStartDate(DateUtils.parseDate("2022-01-01"));
    repeatableStageParams_2.setEndDate(DateUtils.parseDate("2022-01-31"));

    repeatableStageParams_3.setStartIndex(10);
    repeatableStageParams_3.setCount(10);
    repeatableStageParams_3.setStartDate(DateUtils.parseDate("2022-03-01"));
    repeatableStageParams_3.setEndDate(DateUtils.parseDate("2022-03-31"));

    repeatableStageParams_4.setStartIndex(0);
    repeatableStageParams_4.setCount(2);

    repeatableStageParams_5.setStartIndex(0);
    repeatableStageParams_5.setCount(3);

    repeatableStageParams_6.setStartIndex(0);
    repeatableStageParams_6.setCount(1);

    // act
    Set<RepeatableStageParams> repeatableStageParamsSet = new HashSet<>();

    repeatableStageParamsSet.add(repeatableStageParams_1);

    // assert
    assertTrue(repeatableStageParamsSet.add(repeatableStageParams_3));
    assertFalse(repeatableStageParamsSet.add(repeatableStageParams_2));
    assertTrue(repeatableStageParamsSet.add(repeatableStageParams_4));
    assertTrue(repeatableStageParamsSet.add(repeatableStageParams_5));

    assertEquals(0, repeatableStageParams_1.getStartIndex());
    assertEquals(100, repeatableStageParams_1.getCount());
    assertEquals(DateUtils.parseDate("2022-01-01"), repeatableStageParams_1.getStartDate());
    assertEquals(DateUtils.parseDate("2022-01-31"), repeatableStageParams_1.getEndDate());

    assertEquals(repeatableStageParams_1, repeatableStageParams_2);
    assertFalse(repeatableStageParams_1.simpleStageValueExpected());
    assertTrue(repeatableStageParams_6.simpleStageValueExpected());
  }
}
