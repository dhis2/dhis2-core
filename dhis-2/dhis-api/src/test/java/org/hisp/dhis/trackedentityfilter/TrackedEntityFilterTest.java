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
package org.hisp.dhis.trackedentityfilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.Test;

class TrackedEntityFilterTest {

  @Test
  void equalsWhenUidCodeAndNameMatchAlthoughOtherPropertiesDiffer() {
    TrackedEntityFilter a = filter();
    TrackedEntityFilter b = filter();
    b.setDescription("Other description");
    b.setSortOrder(2);
    b.setProgram(program("ProgramUidB"));
    b.setEntityQueryCriteria(new EntityQueryCriteria());

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void notEqualsWhenUidDiffers() {
    TrackedEntityFilter other = filter();
    other.setUid("TeFilter00B");

    assertNotEquals(filter(), other);
  }

  @Test
  void notEqualsWhenCodeDiffers() {
    TrackedEntityFilter other = filter();
    other.setCode("CODE_B");

    assertNotEquals(filter(), other);
  }

  @Test
  void notEqualsWhenNameDiffers() {
    TrackedEntityFilter other = filter();
    other.setName("Other filter");

    assertNotEquals(filter(), other);
  }

  private TrackedEntityFilter filter() {
    TrackedEntityFilter filter = new TrackedEntityFilter();
    filter.setUid("TeFilter00A");
    filter.setCode("CODE_A");
    filter.setName("Filter");
    filter.setDescription("Description");
    filter.setSortOrder(1);
    filter.setProgram(program("ProgramUidA"));
    return filter;
  }

  private static Program program(String uid) {
    Program program = new Program();
    program.setUid(uid);
    return program;
  }
}
