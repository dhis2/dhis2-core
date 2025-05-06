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

  private static final Pattern PSDE_PATTERN =
      Pattern.compile(
          "__PSDE_CTE_PLACEHOLDER__\\(psUid='([^']*)',\\s*deUid='([^']*)',\\s*offset='([^']*)',"
              + "\\s*boundaryHash='([^']*)',\\s*piUid='([^']*)'\\)");

  /**
   * Pattern to match simple filter expressions of the form: V{variableName} <operator>
   * 'literalValue'
   *
   * <p>Where: - V{...} : Matches a variable placeholder (e.g., V{event_date}) - <operator> : One or
   * two character comparison operators (=, !=, <, >, <=, >=, etc.) - 'literalValue' : A quoted
   * literal value (single quotes, no escaped quotes handled)
   *
   * <p>Example matches: V{due_date} = '2023-01-01' V{age} >= '15' V{status} != 'COMPLETED'
   *
   * <p>Capturing groups: 1: variableName (inside V{...}) 2: operator (=, !=, <, >, <=, >=, etc.) 3:
   * literalValue (without quotes)
   */
  private static final Pattern FILTER_PATTERN =
      Pattern.compile("V\\{([^}]+)\\}\\s*([=<>!]{1,2})\\s*'([^']+)'");

  private static final Pattern D2_FUNC_PATTERN =
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

  private static final Pattern VARIABLE_PATTERN =
      Pattern.compile(
          "FUNC_CTE_VAR\\(\\s*type='([^']*)',\\s*column='([^']*)',"
              + "\\s*piUid='([^']*)',\\s*psUid='([^']*)',\\s*offset='([^']*)'\\s*\\)");

  /** Result object for PS/DE placeholders. */
  public record PsDeFields(
      String psUid, String deUid, int offset, String boundaryHash, String piUid) {}

  public record FilterFields(String variableName, String operator, String literal) {}

  public record D2FuncFields(
      String raw, // entire matched placeholder
      String func,
      String psUid,
      String deUid,
      String argType,
      String valueSql, // decoded from Base-64
      String boundaryHash,
      String piUid) {}

  public record VariableFields(
      String type,
      String column,
      String piUid,
      String psUid, // Can be null
      int offset) {}

  /**
   * Attempts to parse a single placeholder string. Returns {@link Optional#empty()} when the string
   * is malformed.
   */
  public static Optional<PsDeFields> parsePsDe(String placeholder) {
    Matcher m = PSDE_PATTERN.matcher(placeholder);
    if (!m.matches()) return Optional.empty();
    return Optional.of(
        new PsDeFields(m.group(1), m.group(2), toInteger(m.group(3)), m.group(4), m.group(5)));
  }

  /**
   * Tries to parse a single simple-filter expression such as
   *
   * <pre>V{event_date} >= '2025-01-01'</pre>
   *
   * . Returns {@link Optional#empty()} when the string does **not** match the grammar.
   */
  public static Optional<FilterFields> parseFilter(String expr) {
    Matcher m = FILTER_PATTERN.matcher(expr);
    if (!m.matches()) {
      return Optional.empty();
    }
    return Optional.of(
        new FilterFields(
            m.group(1), // variableName
            m.group(2), // operator
            m.group(3))); // literal (without quotes)
  }

  /**
   * Parses a single `__D2FUNC__( … )__` token. Returns {@link Optional#empty()} when the string
   * does not match *or* when the Base-64 cannot be decoded.
   */
  public static Optional<D2FuncFields> parseD2Func(String placeholder) {
    Matcher m = D2_FUNC_PATTERN.matcher(placeholder);
    if (!m.matches()) {
      return Optional.empty();
    }
    try {
      return Optional.of(
          new D2FuncFields(
              m.group(0), // raw
              m.group(1), // func
              m.group(2), // psUid
              m.group(3), // deUid
              m.group(4), // argType
              m.group(5), // valueSql (encoded)
              m.group(6), // boundaryHash
              m.group(7))); // piUid
    } catch (IllegalArgumentException ex) {
      // Malformed Base-64 → treat as non-match
      return Optional.empty();
    }
  }

  public static Optional<VariableFields> parseVariable(String expr) {
    Matcher m = VARIABLE_PATTERN.matcher(expr);
    if (!m.matches()) {
      return Optional.empty();
    }
    return Optional.of(
        new VariableFields(
            m.group(1),
            m.group(2),
            m.group(3),
            "null".equals(m.group(4)) ? null : m.group(4),
            toInteger(m.group(5))));
  }

  /** Expose the pattern so callers can iterate through a SQL blob efficiently. */
  public static Pattern psDePattern() {
    return PSDE_PATTERN;
  }

  public static Pattern filterPattern() {
    return FILTER_PATTERN;
  }

  /** Expose the compiled pattern so factories can iterate efficiently. */
  public static Pattern d2FuncPattern() {
    return D2_FUNC_PATTERN;
  }

  public static Pattern variablePattern() {
    return VARIABLE_PATTERN;
  }

  private int toInteger(String stringAsInt) {
    try {
      return Integer.parseInt(stringAsInt);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Invalid integer value: " + stringAsInt, ex);
    }
  }
}
