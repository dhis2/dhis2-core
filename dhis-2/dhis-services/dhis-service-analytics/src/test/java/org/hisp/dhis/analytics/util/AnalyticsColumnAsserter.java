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
package org.hisp.dhis.analytics.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;

/**
 * @author Luciano Fiandesio
 */
@Setter
public class AnalyticsColumnAsserter {
  private AnalyticsTableColumn actual;

  public void verify(AnalyticsTableColumn expected) {
    assertThat("Column name does not match", expected.getName(), is(actual.getName()));
    assertThat(
        "Column " + actual.getName() + " expression does not match!",
        expected.getSelectExpression(),
        is(actual.getSelectExpression()));
    assertThat(
        String.format("Column %s skip index does not match", expected.getName()),
        expected.getSkipIndex(),
        is(actual.getSkipIndex()));
    assertThat(
        "Column creation date does not match", expected.getCreated(), is(actual.getCreated()));
    assertThat(expected.getDataType(), is(actual.getDataType()));
    assertThat(
        String.format("Column %s index type does not match", expected.getName()),
        expected.getIndexType(),
        is(actual.getIndexType()));
  }

  @RequiredArgsConstructor
  public static class Builder {
    final AnalyticsTableColumn _column;

    public AnalyticsColumnAsserter build() {
      AnalyticsColumnAsserter asserter = new AnalyticsColumnAsserter();
      asserter.setActual(_column);
      return asserter;
    }
  }
}
