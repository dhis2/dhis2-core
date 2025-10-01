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
package org.hisp.dhis.period.comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class DescendingPeriodComparatorTest {

  @Test
  void testSort() {
    PeriodDimension m03 = new PeriodDimension(MonthlyPeriodType.getPeriodFromIsoString("201603"));
    PeriodDimension m04 = new PeriodDimension(MonthlyPeriodType.getPeriodFromIsoString("201604"));
    PeriodDimension m05 = new PeriodDimension(MonthlyPeriodType.getPeriodFromIsoString("201605"));
    PeriodDimension m06 = new PeriodDimension(MonthlyPeriodType.getPeriodFromIsoString("201606"));
    List<PeriodDimension> periods = Lists.newArrayList(m04, m03, m06, m05);
    List<PeriodDimension> expected = Lists.newArrayList(m06, m05, m04, m03);
    List<PeriodDimension> sortedPeriods =
        periods.stream().sorted(new DescendingPeriodComparator()).collect(Collectors.toList());
    assertEquals(expected, sortedPeriods);
  }

  @Test
  void testMin() {
    PeriodDimension m03 = new PeriodDimension(MonthlyPeriodType.getPeriodFromIsoString("201603"));
    PeriodDimension m04 = new PeriodDimension(MonthlyPeriodType.getPeriodFromIsoString("201604"));
    PeriodDimension m05 = new PeriodDimension(MonthlyPeriodType.getPeriodFromIsoString("201605"));
    PeriodDimension m06 = new PeriodDimension(MonthlyPeriodType.getPeriodFromIsoString("201606"));
    List<PeriodDimension> periods = Lists.newArrayList(m04, m03, m06, m05);
    Optional<PeriodDimension> latest = periods.stream().min(DescendingPeriodComparator.INSTANCE);
    assertEquals(m06, latest.get());
  }
}
