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
package org.hisp.dhis.dxf2.events.importer.context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
class ProgramSupplierTest extends AbstractSupplierTest<Program> {

  private ProgramSupplier subject;

  @Mock private CacheProvider cacheProvider;

  @Mock private Cache cache;

  private static Stream<Arguments> data() {
    return Stream.of(
        arguments(IdScheme.UID.name()),
        arguments(IdScheme.ID.name()),
        arguments(IdScheme.CODE.name()),
        arguments(IdScheme.NAME.name()));
  }

  @BeforeEach
  void setUp() {
    when(cacheProvider.createProgramCache()).thenReturn(cache);
    when(cache.get(anyString())).thenReturn(Optional.empty());
    this.subject = new ProgramSupplier(jdbcTemplate, cacheProvider);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void verifySupplier(String idScheme) throws SQLException {
    when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
    when(mockResultSet.getLong("id")).thenReturn(100L);
    when(mockResultSet.getString("uid")).thenReturn("abcded");
    when(mockResultSet.getString("code")).thenReturn("ALFA");
    when(mockResultSet.getString("name")).thenReturn("My Program");
    when(mockResultSet.getString("type")).thenReturn(ProgramType.WITHOUT_REGISTRATION.getValue());
    when(mockResultSet.getObject("program_sharing"))
        .thenReturn(generateSharing(null, "rw------", false));
    when(mockResultSet.getInt("opendaysaftercoenddate")).thenReturn(42);
    when(mockResultSet.getLong("catcombo_id")).thenReturn(200L);
    when(mockResultSet.getString("catcombo_uid")).thenReturn("389dh83");
    when(mockResultSet.getString("catcombo_code")).thenReturn("BETA");
    when(mockResultSet.getString("catcombo_name")).thenReturn("My CatCombo");
    when(mockResultSet.getLong("ps_id")).thenReturn(5L, 6L);
    when(mockResultSet.getString("ps_uid")).thenReturn("abcd5", "abcd6");
    when(mockResultSet.getString("ps_code")).thenReturn("cod5", "cod6");
    when(mockResultSet.getString("ps_name")).thenReturn("name5", "name6");
    when(mockResultSet.getInt("sort_order")).thenReturn(1, 2);
    when(mockResultSet.getObject("ps_sharing"))
        .thenReturn(generateSharing(null, "rw------", false));
    when(mockResultSet.getString("ps_feature_type")).thenReturn(null, "POINT");
    when(mockResultSet.getBoolean("ps_repeatable")).thenReturn(true, false);
    when(mockResultSet.getString("validationstrategy")).thenReturn("ON_COMPLETE");
    when(mockResultSet.getObject("uid")).thenReturn("abcded");
    when(mockResultSet.getObject("id")).thenReturn(100L);
    when(mockResultSet.getObject("name")).thenReturn("My Program");
    when(mockResultSet.getObject("code")).thenReturn("ALFA");
    // mock resultset extraction
    mockResultSetExtractorWithoutParameters(mockResultSet);
    ImportOptions importOptions = ImportOptions.getDefaultImportOptions();
    importOptions.getIdSchemes().setProgramIdScheme(idScheme);

    final Map<String, Program> map = subject.get(importOptions, null);

    Program program = map.get(getIdByScheme(idScheme));
    assertThat(program, is(notNullValue()));
    assertThat(program.getProgramStages(), hasSize(2));
    assertThat(program.getId(), is(100L));
    assertThat(program.getCode(), is("ALFA"));
    assertThat(program.getName(), is("My Program"));
    assertThat(program.getProgramType(), is(ProgramType.WITHOUT_REGISTRATION));
    assertThat(program.getSharing().getPublicAccess(), is("rw------"));
    assertThat(program.getOpenDaysAfterCoEndDate(), is(42));
    assertThat(program.getCategoryCombo(), is(notNullValue()));
    assertThat(program.getCategoryCombo().getId(), is(200L));
    assertThat(program.getCategoryCombo().getUid(), is("389dh83"));
    assertThat(program.getCategoryCombo().getName(), is("My CatCombo"));
    assertThat(program.getCategoryCombo().getCode(), is("BETA"));
    // TODO assert more data
  }

  private String getIdByScheme(String idScheme) {
    if (idScheme.equals(IdScheme.UID.name())) {
      return "abcded";
    } else if (idScheme.equals(IdScheme.ID.name())) {
      return "100";
    } else if (idScheme.equals(IdScheme.CODE.name())) {
      return "ALFA";
    } else if (idScheme.equals(IdScheme.NAME.name())) {
      return "My Program";
    }
    return null;
  }

  private String generateSharing(String owner, String publicAccess, boolean external) {
    return "{\"owner\": \""
        + owner
        + "\", \"public\": \""
        + publicAccess
        + "\", \"external\": "
        + external
        + "}";
  }
}
