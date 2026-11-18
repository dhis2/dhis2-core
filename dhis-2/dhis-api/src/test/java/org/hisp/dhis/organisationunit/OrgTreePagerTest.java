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
package org.hisp.dhis.organisationunit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author AI
 */
class OrgTreePagerTest {

  private static int pageCount(int total, int pageSize) {
    return new OrgTree.Pager(1, pageSize, total, List.of()).pageCount();
  }

  @Test
  void emptyResultStillHasOnePage() {
    assertEquals(1, pageCount(0, 10));
  }

  @Test
  void singleResultIsOnePage() {
    assertEquals(1, pageCount(1, 10));
  }

  @Test
  void exactMultipleDoesNotOvercount() {
    // regression guard for the old 1 + total/pageSize formula
    assertEquals(1, pageCount(10, 10));
    assertEquals(2, pageCount(20, 10));
    assertEquals(2, pageCount(4, 2));
    assertEquals(2, pageCount(100, 50));
    // but
    assertEquals(3, pageCount(101, 50));
  }

  @Test
  void partialLastPageRoundsUp() {
    assertEquals(2, pageCount(11, 10));
    assertEquals(3, pageCount(5, 2));
    assertEquals(4, pageCount(31, 10));
  }
}
