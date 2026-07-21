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
package org.hisp.dhis.user.hibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

/**
 * Pins {@link UserSummaryRowMapper}'s transient {@link User} projection (the DHIS2-21860 SQL
 * bypass) to the same wire shape {@link UserPropertyTransformer.JacksonSerialize} produces for a
 * real, Hibernate-loaded {@link User}. If either the SQL column list ({@link
 * UserPropertyTransformer#SUMMARY_SQL_COLUMNS}) or the DTO field set drifts out of sync, this fails
 * instead of silently serving a stale or incomplete summary.
 */
class UserSummaryRowMapperTest {

  @Test
  void selectColumnsIsDerivedFromTheSharedDtoColumnList() {
    assertEquals(
        "u.uid, u.code, u.username, u.firstname, u.surname, u.name",
        UserSummaryRowMapper.SELECT_COLUMNS);
  }

  @Test
  void summaryUserRendersIdenticallyToDtoContract() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString("uid")).thenReturn("abcdefghijk");
    when(rs.getString("code")).thenReturn("CODE1");
    when(rs.getString("username")).thenReturn("jdoe");
    when(rs.getString("firstname")).thenReturn("Jane");
    when(rs.getString("surname")).thenReturn("Doe");
    when(rs.getString("name")).thenReturn(null);

    User summary = UserSummaryRowMapper.INSTANCE.mapRow(rs, 0);

    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addSerializer(User.class, new UserPropertyTransformer.JacksonSerialize());
    mapper.registerModule(module);

    JsonNode json = mapper.valueToTree(summary);

    assertEquals(5, json.size(), "summary User must render as exactly the UserDto field set");
    assertEquals("abcdefghijk", json.get("id").asText());
    assertEquals("CODE1", json.get("code").asText());
    assertEquals("jdoe", json.get("username").asText());
    // name column was null: getName() must fall back to "firstname surname"
    assertEquals("Jane Doe", json.get("name").asText());
    assertEquals("Jane Doe", json.get("displayName").asText());
  }
}
