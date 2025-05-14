/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.event.data.programindicator.ctefactory.placeholder;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

/**
 * Centralised parsing/validation of the various placeholder grammars so that every factory can
 * re-use the same pattern and record type.
 */
@UtilityClass
public class PlaceholderParser {
  /**
   * Data structure for the parsed fields of a PS/DE placeholder.
   *
   * @param psUid program stage UID
   * @param deUid data element UID
   * @param offset offset
   * @param boundaryHash hash containing the program indicator boundary
   * @param piUid program indicator UID
   */
  public record PsDeFields(
      String psUid, String deUid, int offset, String boundaryHash, String piUid) {}

  /**
   * Data structure for the parsed fields of a filter placeholder.
   *
   * @param variableName name of the variable
   * @param operator operator used in the filter
   * @param literal literal value used in the filter
   */
  public record FilterFields(String variableName, String operator, String literal) {}

  /**
   * Data structure for the parsed fields of a D2 function placeholder.
   *
   * @param raw the entire matched placeholder
   * @param func the function name
   * @param psUid program stage UID
   * @param deUid data element UID
   * @param argType argument type
   * @param valueSql SQL value (Base64 encoded)
   * @param boundaryHash hash containing the the program indicator boundary
   * @param piUid program indicator UID
   */
  public record D2FuncFields(
      String raw,
      String func,
      String psUid,
      String deUid,
      String argType,
      String valueSql,
      String boundaryHash,
      String piUid) {}

  /**
   * Data structure for the parsed fields of a variable placeholder.
   *
   * @param type type of the variable
   * @param column column name
   * @param piUid program indicator UID
   * @param psUid program stage UID (can be null)
   * @param offset offset
   */
  public record VariableFields(
      String type, String column, String piUid, String psUid, int offset) {}

  public static Pattern psDePattern() {
    return ProgramStageDataElementPlaceholderParser.PATTERN;
  }

  public static Pattern filterPattern() {
    return FilterPlaceholderParser.PATTERN;
  }

  public static Pattern d2FuncPattern() {
    return D2FunctionPlaceholderParser.PATTERN;
  }

  public static Pattern variablePattern() {
    return VariablePlaceholderParser.PATTERN;
  }

  // --- Public Parsers (Unchanged for API compatibility) ---

  public static Optional<PsDeFields> parsePsDe(String placeholder) {
    return ProgramStageDataElementPlaceholderParser.parse(placeholder);
  }

  public static Optional<FilterFields> parseFilter(String expr) {
    return FilterPlaceholderParser.parse(expr);
  }

  public static Optional<D2FuncFields> parseD2Func(String placeholder) {
    return D2FunctionPlaceholderParser.parse(placeholder);
  }

  public static Optional<VariableFields> parseVariable(String expr) {
    return VariablePlaceholderParser.parse(expr);
  }

  public static final class ProgramStageDataElementPlaceholderParser {
    private static final Pattern PATTERN =
        Pattern.compile(
            "__PSDE_CTE_PLACEHOLDER__\\(psUid='([^']*)',\\s*deUid='([^']*)',\\s*offset='([^']*)',"
                + "\\s*boundaryHash='([^']*)',\\s*piUid='([^']*)'\\)");

    private static final int GROUP_PS_UID = 1;
    private static final int GROUP_DE_UID = 2;
    private static final int GROUP_OFFSET = 3;
    private static final int GROUP_BOUNDARY_HASH = 4;
    private static final int GROUP_PI_UID = 5;

    private ProgramStageDataElementPlaceholderParser() {}

    private static Optional<PsDeFields> parse(String placeholder) {
      Matcher m = PATTERN.matcher(placeholder);
      if (!m.matches()) return Optional.empty();
      return Optional.of(
          new PsDeFields(
              m.group(GROUP_PS_UID),
              m.group(GROUP_DE_UID),
              toInteger(m.group(GROUP_OFFSET)),
              m.group(GROUP_BOUNDARY_HASH),
              m.group(GROUP_PI_UID)));
    }
  }

  public static final class D2FunctionPlaceholderParser {

    private static final Pattern PATTERN =
        Pattern.compile(
            // Match literal prefix and opening parenthesis
            "__D2FUNC__\\("
                +
                // Capture 'func' value
                "func='([^']*)',"
                +
                // Optional whitespace, capture 'ps' value
                "\\s*ps='([^']*)',"
                +
                // Optional whitespace, capture 'de' value
                "\\s*de='([^']*)',"
                +
                // argType (e.g., val64, condLit64, none)
                "\\s*argType='([^']*)',"
                +
                // Optional whitespace, capture 'val64' value (Base64 chars: A-Z, a-z, 0-9, +, /, =)
                "\\s*arg64='([A-Za-z0-9+/=]*)',"
                + // More specific capture for Base64
                // Optional whitespace, capture 'hash' value (Alphanumeric for SHA1/MD5 or includes
                // '_')
                "\\s*hash='([^']*)',"
                +
                // Optional whitespace, capture 'pi' value
                "\\s*pi='([^']*)'"
                +
                // Match literal closing parenthesis and suffix
                "\\)__");

    private static final int GROUP_FUNC = 1;
    private static final int GROUP_PS_UID = 2;
    private static final int GROUP_DE_UID = 3;
    private static final int GROUP_ARG_TYPE = 4;
    private static final int GROUP_VALUE_SQL = 5;
    private static final int GROUP_BOUNDARY_HASH = 6;
    private static final int GROUP_PI_UID = 7;

    private D2FunctionPlaceholderParser() {}

    private static Optional<D2FuncFields> parse(String placeholder) {
      Matcher m = PATTERN.matcher(placeholder);
      if (!m.matches()) return Optional.empty();
      try {
        return Optional.of(
            new D2FuncFields(
                m.group(0),
                m.group(GROUP_FUNC),
                m.group(GROUP_PS_UID),
                m.group(GROUP_DE_UID),
                m.group(GROUP_ARG_TYPE),
                m.group(GROUP_VALUE_SQL),
                m.group(GROUP_BOUNDARY_HASH),
                m.group(GROUP_PI_UID)));
      } catch (IllegalArgumentException ex) {
        return Optional.empty();
      }
    }
  }

  public static final class FilterPlaceholderParser {
    private static final Pattern PATTERN =
        Pattern.compile("V\\{([^}]+)\\}\\s*([=<>!]{1,2})\\s*'([^']+)'");

    private static final int GROUP_VARIABLE_NAME = 1;
    private static final int GROUP_OPERATOR = 2;
    private static final int GROUP_LITERAL = 3;

    private FilterPlaceholderParser() {}

    private static Optional<FilterFields> parse(String expr) {
      Matcher m = PATTERN.matcher(expr);
      if (!m.matches()) return Optional.empty();
      return Optional.of(
          new FilterFields(
              m.group(GROUP_VARIABLE_NAME), m.group(GROUP_OPERATOR), m.group(GROUP_LITERAL)));
    }
  }

  public static final class VariablePlaceholderParser {
    private static final Pattern PATTERN =
        Pattern.compile(
            "FUNC_CTE_VAR\\(\\s*type='([^']*)',\\s*column='([^']*)',"
                + "\\s*piUid='([^']*)',\\s*psUid='([^']*)',\\s*offset='([^']*)'\\s*\\)");

    private static final int GROUP_TYPE = 1;
    private static final int GROUP_COLUMN = 2;
    private static final int GROUP_PI_UID = 3;
    private static final int GROUP_PS_UID = 4;
    private static final int GROUP_OFFSET = 5;

    private VariablePlaceholderParser() {}

    private static Optional<VariableFields> parse(String expr) {
      Matcher m = PATTERN.matcher(expr);
      if (!m.matches()) return Optional.empty();
      return Optional.of(
          new VariableFields(
              m.group(GROUP_TYPE),
              m.group(GROUP_COLUMN),
              m.group(GROUP_PI_UID),
              "null".equals(m.group(GROUP_PS_UID)) ? null : m.group(GROUP_PS_UID),
              toInteger(m.group(GROUP_OFFSET))));
    }
  }

  private static int toInteger(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Invalid integer: " + value, ex);
    }
  }
}
