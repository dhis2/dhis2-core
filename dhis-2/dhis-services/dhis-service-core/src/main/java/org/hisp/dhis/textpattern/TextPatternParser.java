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
package org.hisp.dhis.textpattern;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Stian Sandvold
 */
public class TextPatternParser {
  private static final String METHOD_REGEX = "(?<MethodName>[A-Z_]+?)\\(.*?\\)";

  private static final String JOIN_REGEX = "(?<Join>[\\s]*(?<JoinValue>\\+)[\\s]*)";

  private static final String TEXT_REGEX = "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"";

  private static final Pattern EXPRESSION_REGEX =
      Pattern.compile(
          String.format(
              "[\\s]*(?<Segment>(?<Method>%s|%s)|%s)+?[\\s]*",
              TEXT_REGEX, METHOD_REGEX, JOIN_REGEX));

  /**
   * Parses an expression, identifying segments and builds an IDExpression. throws exception if
   * syntax is invalid
   *
   * @param pattern the expression to parse
   * @return IDExpression representing the expression
   */
  public static TextPattern parse(String pattern) throws TextPatternParsingException {
    List<TextPatternSegment> segments = new ArrayList<>();

    // True if we just parsed a Segment, False if we parsed a join or
    // haven't parsed anything.
    boolean segment = false;

    boolean invalidExpression = true;

    Matcher m;

    if (pattern != null && !pattern.isEmpty()) {
      m = EXPRESSION_REGEX.matcher(pattern);
    } else {
      throw new TextPatternParsingException("Supplied expression was null or empty.", -1);
    }

    /*
     * We go trough all matches. Matches can be one of the following:
     *
     * <ul> <li>a TEXT method ("..")</li> <li>any TextPatternMethod
     * (Excluding TEXT) (method(param))</li> <li>a join ( + )</li> </ul>
     *
     * Matches that are invalid includes methods with unknown method names
     */
    while (m.find()) {
      invalidExpression = false;

      // This returns the entire method syntax, including params
      String method = m.group("Method");

      // This means we found a match for method syntax
      if (method != null) {

        // This returns only the name of the method (see
        // TextPatternMethod for valid names)
        String methodName = m.group("MethodName");

        // This means we encountered the syntax for TEXT method
        if (methodName == null) // Text
        {

          // Only add if valid syntax, else it will throw exception
          // after if-else.
          if (TextPatternMethod.TEXT.getType().validatePattern(method)) {
            segment = true;
            segments.add(new TextPatternSegment(TextPatternMethod.TEXT, method));
            continue;
          }

        }

        // Catch all other methods
        else {
          // Attempt to find a matching method name in
          // TextPatternMethod
          try {
            TextPatternMethod textPatternMethod = TextPatternMethod.valueOf(methodName);

            // Only add if valid syntax, else it will throw
            // exception after if-else.
            if (textPatternMethod.getType().validatePattern(method)) {
              segment = true;
              segments.add(new TextPatternSegment(textPatternMethod, method));
              continue;
            }
          } catch (Exception e) {
            // Ignore, throw exception after if-else if we get here.
          }
        }

        // If we are here, that means we found no matching methods, so
        // throw an exception
        throw new TextPatternParsingException(
            "Failed to parse the following method: '" + method + "'", m.start("Method"));
      }

      // Handle Join
      else if (m.group("Join") != null) {
        // Join should only be after a Segment
        if (!segment) {
          throw new TextPatternParsingException("Unexpected '+'", m.start("JoinValue"));
        } else {
          segment = false;
        }
      }
    }

    // If the matcher had no matches
    if (invalidExpression) {
      throw new TextPatternParsingException("The expression is invalid", -1);
    }

    // An expression should not end on a Join
    if (!segment) {
      throw new TextPatternParsingException("Unexpected '+' at the end of the expression", -1);
    }

    return new TextPattern(segments);
  }

  public static class TextPatternParsingException extends Exception {
    TextPatternParsingException(String message, int position) {
      super(
          "Could not parse expression: "
              + message
              + (position != -1 ? " at position " + (position + 1) : ""));
    }
  }
}
