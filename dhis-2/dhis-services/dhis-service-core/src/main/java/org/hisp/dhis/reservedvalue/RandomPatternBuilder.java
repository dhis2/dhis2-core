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
package org.hisp.dhis.reservedvalue;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Luca Cambi <luca@dhis2.org>
 *     <p>Pattern builder class. Provides string builders used to append segments of a random
 *     pattern.
 *     <p>For alphanumeric, we have some corner cases:
 *     <p>- '*' : it shuffles between lower and upper case and gets a number when it finds it in
 *     UUID random string
 *     <p>- 'x' or 'X' : in UUID random string gets the letter ( and make it upper case for 'X' )
 *     but if we find a number, it gets the alphabetic char at that position in order to maintain
 *     random pattern
 *     <p>For numeric patterns that are longer than 1 digit, we also have builders that contains all
 *     zeros or partial zeros because random digits are generated from BigInteger that has no
 *     leading zeros, so we need to consider zero leading cases.
 *     <p>For digits:
 *     <p>- '#': in case of at least 2 digits pattern inside the segment, we also create a second
 *     dimension that contains leading zeros ( numbers of zero is randomly determined ) and a third
 *     dimension whit only zeros. If the segment has only digits, we generate an all zero pattern
 *     only once, otherwise we append to it for only iteration.
 *     <p>In case of 1 digit, we just generate random numbers in range 0 - 9
 */
public class RandomPatternBuilder {
  private final List<Character> lowercase =
      IntStream.rangeClosed('a', 'z').mapToObj(c -> (char) c).collect(Collectors.toList());

  private final List<Character> uppercase =
      IntStream.rangeClosed('A', 'Z').mapToObj(c -> (char) c).collect(Collectors.toList());

  private StringBuilder zeroPrefixDigitPatternBuilder;

  private StringBuilder allZeroDigitPatternBuilder;

  private StringBuilder basicPatternBuilder = new StringBuilder();

  private final boolean isOneOrZeroDigit;

  private final boolean hasOnlyDigits;

  public RandomPatternBuilder(String segmentParameter) {
    this.isOneOrZeroDigit = !segmentParameter.contains("##");

    this.hasOnlyDigits =
        segmentParameter.chars().filter(ch -> ch == '#').count() == segmentParameter.length();

    if (!this.isOneOrZeroDigit) {
      this.allZeroDigitPatternBuilder = new StringBuilder();
      this.zeroPrefixDigitPatternBuilder = new StringBuilder();

      if (this.hasOnlyDigits)
        this.allZeroDigitPatternBuilder.append(StringUtils.repeat('0', segmentParameter.length()));
    }
  }

  public List<StringBuilder> getPatternBuilders() {
    return Stream.of(
            this.basicPatternBuilder,
            this.zeroPrefixDigitPatternBuilder,
            this.allZeroDigitPatternBuilder)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public void setForRandomLowerCase(String pattern) {
    String randomUUIDForLower = getRandomUUIDFor(pattern);
    List<StringBuilder> stringBuilders = this.getPatternBuilders();

    int i = 0;
    while (i < pattern.length()) {
      if (Character.isLetter(randomUUIDForLower.charAt(i))) {
        for (StringBuilder builder : stringBuilders) {
          builder.append(Character.toLowerCase(randomUUIDForLower.charAt(i)));
        }
      } else if (Character.isDigit(randomUUIDForLower.charAt(i))) {
        for (StringBuilder builder : stringBuilders) {
          builder.append(lowercase.get(Character.getNumericValue(randomUUIDForLower.charAt(i))));
        }
      }

      i++;
    }
  }

  public void setForRandomUpperCase(String pattern) {
    String randomUUIDForUpper = getRandomUUIDFor(pattern);
    List<StringBuilder> stringBuilders = getPatternBuilders();

    int i = 0;
    while (i < pattern.length()) {
      if (Character.isLetter(randomUUIDForUpper.charAt(i))) {
        for (StringBuilder builder : stringBuilders) {
          builder.append(Character.toUpperCase(randomUUIDForUpper.charAt(i)));
        }
      } else if (Character.isDigit(randomUUIDForUpper.charAt(i))) {
        for (StringBuilder builder : stringBuilders) {
          builder.append(uppercase.get(Character.getNumericValue(randomUUIDForUpper.charAt(i))));
        }
      }

      i++;
    }
  }

  public void setForRandomAll(String pattern) {
    boolean isUpper = false;
    String randomUUIDForAll = getRandomUUIDFor(pattern);
    List<StringBuilder> stringBuilders = getPatternBuilders();

    int i = 0;
    while (i < pattern.length()) {
      char c;
      if (Character.isLetter(randomUUIDForAll.charAt(i))) {
        if (isUpper) {
          c = Character.toUpperCase(randomUUIDForAll.charAt(i));
          stringBuilders.forEach(sb -> sb.append(c));
          isUpper = false;
        } else {
          c = randomUUIDForAll.charAt(i);
          stringBuilders.forEach(sb -> sb.append(c));
          isUpper = true;
        }

      } else if (Character.isDigit(randomUUIDForAll.charAt(i))) {
        c = randomUUIDForAll.charAt(i);
        stringBuilders.forEach(sb -> sb.append(c));
      }

      i++;
    }
  }

  public void setForRandomDigits(String pattern, SecureRandom secureRandom) {
    if (this.isOneOrZeroDigit) {
      basicPatternBuilder.append(secureRandom.nextInt(10));
      return;
    }

    String bigIntegerString = new BigInteger(256, secureRandom).abs().toString();

    int zeros = secureRandom.nextInt(pattern.length() - 1) + 1;

    this.zeroPrefixDigitPatternBuilder.append(
        (StringUtils.repeat('0', zeros) + bigIntegerString.substring(zeros)), 0, pattern.length());

    this.basicPatternBuilder.append(bigIntegerString, 0, pattern.length());

    if (!this.hasOnlyDigits)
      this.allZeroDigitPatternBuilder.append(StringUtils.repeat('0', pattern.length()));
  }

  public void resetPatternBuilder() {
    this.basicPatternBuilder = new StringBuilder();
    this.allZeroDigitPatternBuilder = null;

    if (!this.isOneOrZeroDigit) {
      zeroPrefixDigitPatternBuilder = new StringBuilder();

      if (!hasOnlyDigits) allZeroDigitPatternBuilder = new StringBuilder();
    }
  }

  private String getRandomUUIDFor(String pattern) {

    StringBuilder randomUUIDString = new StringBuilder();

    while (randomUUIDString.length() < pattern.length()) {
      randomUUIDString.append(UUID.randomUUID().toString().replace("-", ""));
    }

    return randomUUIDString.toString();
  }
}
