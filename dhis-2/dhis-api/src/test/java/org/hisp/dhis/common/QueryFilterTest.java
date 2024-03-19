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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QueryFilterTest {

  @Test
  void testUnderscoreIsEscaped() {
    QueryFilter queryFilter = new QueryFilter();
    queryFilter.setOperator(QueryOperator.LIKE);
    assertThat(queryFilter.getSqlFilter("_"), is("'%\\_%'"));
  }

  @Test
  void testPercentageSignIsEscaped() {
    QueryFilter queryFilter = new QueryFilter();
    queryFilter.setOperator(QueryOperator.LIKE);
    assertThat(queryFilter.getSqlFilter("%"), is("'%\\%%'"));
  }

  @Test
  @DisplayName("When value substitution allowed and NV provided, equals operator returns null")
  void testEqualsAndNullValueReturnNullStringWhenAllowed() {
    QueryFilter queryFilter = new QueryFilter();
    queryFilter.setOperator(QueryOperator.EQ);
    assertThat(queryFilter.getSqlFilter("NV", true), is("null"));
  }

  @Test
  @DisplayName("When value substitution not allowed and NV provided, equals operator returns NV")
  void testNullValueReturnedIfSubstitutionNotAllowed() {
    QueryFilter queryFilter = new QueryFilter();
    queryFilter.setOperator(QueryOperator.EQ);
    assertThat(queryFilter.getSqlFilter("NV", false), is("'NV'"));
  }

  @Test
  @DisplayName(
      "When value substitution allowed and any random text provided, equals operator returns text")
  void testRandomTextReturnedIfSubstitutionAllowed() {
    QueryFilter queryFilter = new QueryFilter();
    queryFilter.setOperator(QueryOperator.EQ);
    assertThat(queryFilter.getSqlFilter("NVA", true), is("'NVA'"));
  }

  @Test
  void testOperatorIsNotReplacedWhenNotAllowed() {
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EQ, "NV");
    assertThat(queryFilter.getSqlOperator(), is("="));
  }

  @Test
  void testOperatorIsReplacedWhenAllowed() {
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EQ, "NV");
    assertThat(queryFilter.getSqlOperator(true), is("is"));
  }

  @Test
  void testOperatorIsNotReplacedWhenAllowedButRandomTextProvided() {
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EQ, "NVA");
    assertThat(queryFilter.getSqlOperator(true), is("="));
  }
}
