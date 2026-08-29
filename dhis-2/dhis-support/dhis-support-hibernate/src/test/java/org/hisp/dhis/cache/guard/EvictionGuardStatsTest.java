/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.cache.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EvictionGuardStats}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class EvictionGuardStatsTest {
  @Test
  void forRegionIsIdempotentAndCounts() {
    EvictionGuardStats stats = EvictionGuardStats.forRegion("r1");
    assertSame(stats, EvictionGuardStats.forRegion("r1"));
    stats.countRefused();
    stats.countRefused();
    stats.countSelfEvicted();
    stats.countStoredPut();
    stats.countStoredPut();
    stats.countStoredPut();
    assertEquals(2, stats.getRefused());
    assertEquals(1, stats.getSelfEvicted());
    assertEquals(3, stats.getStoredPuts());
    assertTrue(EvictionGuardStats.all().containsKey("r1"));
  }

  @Test
  void statsAreKeptPerRegion() {
    EvictionGuardStats first = EvictionGuardStats.forRegion("perRegionA");
    EvictionGuardStats second = EvictionGuardStats.forRegion("perRegionB");
    first.countRefused();
    assertEquals(1, first.getRefused());
    assertEquals(0, second.getRefused());
    assertEquals("perRegionA", first.getRegionName());
    assertEquals("perRegionB", second.getRegionName());
  }

  @Test
  void allIsUnmodifiable() {
    EvictionGuardStats stats = EvictionGuardStats.forRegion("unmodifiable");
    Map<String, EvictionGuardStats> all = EvictionGuardStats.all();
    assertSame(stats, all.get("unmodifiable"));
    assertThrows(UnsupportedOperationException.class, () -> all.remove("unmodifiable"));
  }
}
