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

import com.google.common.collect.ImmutableSet;
import java.util.regex.Pattern;

/**
 * @author Stian Sandvold
 */
public enum TextPatternMethod {
  /**
   * Text method is just a fixed text that is a part of the pattern. It starts and ends with a
   * quotation mark: " A Text can contain quotation marks, but they need to be escaped. Example
   * usage: "Hello world" "Hello \"world\""
   *
   * <p>This is the only method that has no keyword associated with it.
   */
  TEXT(new TextMethodType(Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*+)\""))),

  /**
   * Generator methods has a required param, that needs to be between 1 and 12 characters.
   * SEQUENTIAL only accepts #'s while RANDOM accepts #Xx's
   */
  RANDOM(new GeneratedMethodType(Pattern.compile("RANDOM\\(([#Xx\\*]{1,12})\\)"))),
  SEQUENTIAL(new GeneratedMethodType(Pattern.compile("SEQUENTIAL\\(([#]{1,12})\\)"))),

  /**
   * Variable methods has an optional param, that can: start with ^ have 1 or more . (representing a
   * character) end with $
   *
   * <p>^ will start the format form the start of the resolved value $ will start the format from
   * the end of the resolved value . will match a single character. At least 1 is required if a
   * param is supplied
   *
   * <p>Alternatively, an empty param means the entire resolved value will be returned.
   *
   * <p>Example usage assuming ORG_UNIT_CODE resolved to "Hello world": ORG_UNIT_CODE() = "Hello
   * world" ORG_UNIT_CODE(..) = "He" ORG_UNIT_CODE(^..) = "He" ORG_UNIT_CODE(..$) = "ld"
   */
  ORG_UNIT_CODE(new StringMethodType(Pattern.compile("ORG_UNIT_CODE\\((.{0}|[\\^]?[.]+?[$]?)\\)"))),

  /**
   * Date methods has a required param that will be used to format the date. The regex will match
   * any sequence of characters for now.
   *
   * <p>The param will be used directly as the format in SimpleDateFormat:
   * https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
   */
  CURRENT_DATE(new DateMethodType(Pattern.compile("CURRENT_DATE\\((.+?)\\)")));

  public static final ImmutableSet<TextPatternMethod> GENERATED =
      ImmutableSet.of(RANDOM, SEQUENTIAL);

  public static final ImmutableSet<TextPatternMethod> REQUIRED = ImmutableSet.of(ORG_UNIT_CODE);

  public static final ImmutableSet<TextPatternMethod> OPTIONAL =
      ImmutableSet.of(RANDOM, SEQUENTIAL);

  public static final ImmutableSet<TextPatternMethod> PERSIST = ImmutableSet.of(RANDOM);

  private MethodType type;

  TextPatternMethod(MethodType type) {
    this.type = type;
  }

  public MethodType getType() {
    return type;
  }

  public boolean isRequired() {
    return REQUIRED.contains(this);
  }

  public boolean isOptional() {
    return OPTIONAL.contains(this);
  }

  public boolean isGenerated() {
    return GENERATED.contains(this);
  }

  public boolean isPersistable() {
    return PERSIST.contains(this);
  }
}
