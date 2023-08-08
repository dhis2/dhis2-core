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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.context;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryCombo;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryOption;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionCombo;
import static org.hisp.dhis.dxf2.deprecated.tracker.importer.context.AttributeOptionComboLoader.SQL_GET_CATEGORYOPTIONCOMBO;
import static org.hisp.dhis.dxf2.deprecated.tracker.importer.context.AttributeOptionComboLoader.SQL_GET_CATEGORYOPTIONCOMBO_BY_CATEGORYIDS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class AttributeOptionComboLoaderTest {
  @Mock protected JdbcTemplate jdbcTemplate;

  @Captor private ArgumentCaptor<String> sqlCaptor;

  private AttributeOptionComboLoader subject;

  @BeforeEach
  public void setUp() {
    subject = new AttributeOptionComboLoader(jdbcTemplate);
  }

  @Test
  void verifyGetDefaultCategoryOptionCombo() {
    when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class)))
        .thenReturn(new CategoryOptionCombo());

    CategoryOptionCombo categoryOptionCombo = subject.getDefault();

    assertThat(categoryOptionCombo, is(notNullValue()));
    verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), any(RowMapper.class));

    final String sql = sqlCaptor.getValue();

    assertThat(
        sql,
        is(
            replace(
                SQL_GET_CATEGORYOPTIONCOMBO,
                "key",
                "categoryoptioncomboid",
                "resolvedScheme",
                "name = 'default'")));
  }

  @Test
  void verifyGetCategoryOption() {
    get(IdScheme.ID, "12345", "categoryoptioncomboid = 12345");
    get(IdScheme.UID, "abcdef", "uid = 'abcdef'");
    get(IdScheme.NAME, "alfa", "name = 'alfa'");
  }

  @Test
  void verifyGetAttributeOptionComboWithNullCategoryCombo() {
    assertThrows(
        IllegalQueryException.class,
        () -> subject.getAttributeOptionCombo(null, "", "", IdScheme.UID),
        "Illegal category combo");
  }

  @Test
  void verifyGetAttributeOptionComboWithNonExistingCategoryOption() {
    CategoryCombo cc = new CategoryCombo();

    assertThrows(
        IllegalQueryException.class,
        () -> subject.getAttributeOptionCombo(cc, "abcdef", "", IdScheme.UID),
        "Illegal category option identifier: abcdef");
  }

  @Test
  void verifyGetAttributeOptionCombo() {
    // prepare data
    CategoryCombo cc = createCategoryCombo('B');
    CategoryOption categoryOption = createCategoryOption('A');
    categoryOption.setId(100L);

    when(jdbcTemplate.queryForObject(
            eq(
                "select categoryoptionid, uid, code, name, startdate, enddate, sharing from dataelementcategoryoption where uid = 'abcdef'"),
            any(RowMapper.class)))
        .thenReturn(categoryOption);

    when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
        .thenReturn(singletonList(createCategoryOptionCombo(cc, categoryOption)));

    // method under test
    CategoryOptionCombo categoryOptionCombo =
        subject.getAttributeOptionCombo(cc, "abcdef", "", IdScheme.UID);

    // assertions
    assertThat(categoryOptionCombo, is(notNullValue()));
    verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class));

    final String sql = sqlCaptor.getValue();
    assertThat(
        sql,
        is(
            replace(
                SQL_GET_CATEGORYOPTIONCOMBO_BY_CATEGORYIDS,
                "resolvedScheme",
                "uid = '" + cc.getUid() + "'",
                "option_ids",
                "'100'")));
  }

  private void get(IdScheme idScheme, String key, String resolvedId) {
    when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class)))
        .thenReturn(new CategoryOptionCombo());

    CategoryOptionCombo categoryOptionCombo = subject.getCategoryOptionCombo(idScheme, key);

    assertThat(categoryOptionCombo, is(notNullValue()));
    verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), any(RowMapper.class));

    final String sql = sqlCaptor.getValue();

    assertThat(
        sql,
        is(
            replace(
                SQL_GET_CATEGORYOPTIONCOMBO,
                "key",
                "categoryoptioncomboid",
                "resolvedScheme",
                resolvedId)));
    reset(jdbcTemplate);
  }

  private String replace(String sql, String... keyVal) {

    Map<String, String> vals = new HashMap<>();

    for (int i = 0; i < keyVal.length - 1; i++) {
      vals.put(keyVal[i], keyVal[i + 1]);
    }
    StrSubstitutor sub = new StrSubstitutor(vals);
    return sub.replace(sql);
  }
}
