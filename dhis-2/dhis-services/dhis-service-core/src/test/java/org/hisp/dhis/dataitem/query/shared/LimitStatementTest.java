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
package org.hisp.dhis.dataitem.query.shared;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.MAX_LIMIT;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Unit tests for CommonStatement.
 *
 * @author maikel arabori
 */
class LimitStatementTest {

  @Test
  void testMaxLimitWhenItIsPresentInParameters() {
    // Given
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(MAX_LIMIT, 20);
    final String expectedStatement = " limit :" + MAX_LIMIT;
    // When
    final String actualStatement = maxLimit(theParameterSource);
    // Then
    assertThat(actualStatement, is(expectedStatement));
  }

  @Test
  void testMaxLimitWhenItIsNotPresentInParameters() {
    // Given
    final MapSqlParameterSource noParameterSource = new MapSqlParameterSource();
    // When
    final String actualStatement = maxLimit(noParameterSource);
    // Then
    assertThat(actualStatement, is(EMPTY));
  }

  @Test
  void testMaxLimitWhenParameterSourceIsNull() {
    // Given
    final MapSqlParameterSource nullParameterSource = new MapSqlParameterSource();
    // When
    final String actualStatement = maxLimit(nullParameterSource);
    // Then
    assertThat(actualStatement, is(EMPTY));
  }

  @Test
  void testMaxLimitWhenItIsSetToNull() {
    // Given
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(MAX_LIMIT, null);
    // When
    final String actualStatement = maxLimit(theParameterSource);
    // Then
    assertThat(actualStatement, is(EMPTY));
  }

  @Test
  void testMaxLimitWhenItIsSetToEmpty() {
    // Given
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(MAX_LIMIT, "");
    // When
    final String actualStatement = maxLimit(theParameterSource);
    // Then
    assertThat(actualStatement, is(EMPTY));
  }
}
