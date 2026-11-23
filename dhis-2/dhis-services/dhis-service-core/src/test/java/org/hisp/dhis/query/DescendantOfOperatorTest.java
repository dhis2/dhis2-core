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
package org.hisp.dhis.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.query.operators.DescendantOfOperator;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs
 */
class DescendantOfOperatorTest {
  @Test
  void matchesRootsAndDescendantsOfAnyRoot() {
    DescendantOfOperator scope =
        new DescendantOfOperator(List.of("/top/first", "/top/second", "/top/first/nested"));

    assertTrue(scope.test("/top/first"));
    assertTrue(scope.test("/top/second/child"));
    assertTrue(scope.test("/top/first/nested/child"));
    assertFalse(scope.test("/top"));
    assertFalse(scope.test("/top/sibling"));
    assertFalse(scope.test("/other/first/child"));
    assertFalse(scope.test("/top/firstSibling/child"));
  }

  @Test
  void missingRootsAndMissingPathsDoNotMatch() {
    assertFalse(new DescendantOfOperator(List.of()).test("/top/first"));
    assertFalse(new DescendantOfOperator(List.of("/top")).test(null));
  }
}
