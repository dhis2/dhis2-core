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
package org.hisp.dhis.h2;

import static org.hisp.dhis.jsontree.JsonTypedAccess.GLOBAL;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.jsontree.JsonDocument.JsonNodeType;
import org.hisp.dhis.jsontree.JsonResponse;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.postgresql.util.PGobject;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class H2SqlFunction {
  public static void registerH2Functions(DataSource dataSource) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      createAliasForFunction(connection, "jsonb_extract_path_text");
      createAliasForFunction(connection, "jsonb_extract_path");
      createAliasForFunction(connection, "jsonb_typeof");
      createAliasForFunction(connection, "jsonb_has_user_id");
      createAliasForFunction(connection, "jsonb_check_user_access");
    } catch (SQLException exception) {
      log.info(
          "Failed to register custom H2Functions, probably already registered, ignoring this.",
          exception);
    }
  }

  private static void createAliasForFunction(Connection connection, String name)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          String.format(
              "CREATE ALIAS %s FOR \"%s.%s\"", name, H2SqlFunction.class.getName(), name));
    }
  }

  // Postgres inbuilt function
  public static String jsonb_typeof(PGobject json) {
    if (json == null) {
      return "null";
    }
    String content = json.getValue();
    if (content == null) {
      return "null";
    }
    return JsonValue.of(content).node().getType().name().toLowerCase();
  }

  // Postgres inbuilt function
  public static String jsonb_extract_path_text(PGobject json, String... paths) {
    try {
      String content = json.getValue();
      if (content == null) {
        return null;
      }
      JsonValue value = new JsonResponse(content, GLOBAL).get(toJsonPath(paths));
      if (!value.exists()) {
        return null;
      }
      if (value.node().getType() == JsonNodeType.STRING) {
        return value.as(JsonString.class).string();
      }
      return value.node().getDeclaration();
    } catch (Exception e) {
      log.error("Failed to extract path", e);
      throw e;
    }
  }

  // Postgres inbuilt function
  public static String jsonb_extract_path(PGobject json, String... paths) throws SQLException {
    try {
      String content = json.getValue();
      if (content == null) {
        return null;
      }
      JsonValue value = new JsonResponse(content, GLOBAL).get(toJsonPath(paths));
      if (!value.exists()) {
        return null;
      }
      return value.node().getDeclaration();
    } catch (Exception e) {
      log.error("Failed to extract path", e);
      throw e;
    }
  }

  private static String toJsonPath(String[] paths) {
    StringBuilder p = new StringBuilder();
    for (String path : paths) {
      if (path.matches("[0-9]+")) {
        p.append('[').append(path).append(']');
      } else {
        if (p.length() > 0) {
          p.append('.');
        }
        p.append(path);
      }
    }
    return p.toString();
  }

  // Custom DHIS2 sharing function
  public static boolean jsonb_has_user_id(PGobject input1, String input2) {
    try {
      String content = input1.getValue();
      Map<String, Object> retMap =
          new Gson().fromJson(content, new TypeToken<HashMap<String, Object>>() {}.getType());

      if (retMap != null) {
        final String s = (String) retMap.get("owner");
        return input2.equals(s);
      }
      throw new IllegalArgumentException("Wrong input to jsonb_has_user_id");
    } catch (Exception e) {
      log.error("Failed to check user id", e);
      throw e;
    }
  }

  // Custom DHIS2 sharing function
  public static boolean jsonb_check_user_access(PGobject input1, String input2, String input3) {
    try {
      String content = input1.getValue();
      Map<String, Object> retMap =
          new Gson().fromJson(content, new TypeToken<HashMap<String, Object>>() {}.getType());

      if (retMap != null) {
        final String ownerField = (String) retMap.get("owner");
        final boolean ownerMatches = ownerField.equals(input2);

        final String publicField = (String) retMap.get("public");
        final String lettersBeforePercent = input3.substring(0, input3.indexOf("%"));
        final boolean patternMatches = publicField.matches(lettersBeforePercent + ".*");

        return ownerMatches && patternMatches;
      }
      throw new IllegalArgumentException("Wrong input to jsonb_check_user_access");

    } catch (Exception e) {
      log.error("Failed to check user access", e);
      throw e;
    }
  }
}
