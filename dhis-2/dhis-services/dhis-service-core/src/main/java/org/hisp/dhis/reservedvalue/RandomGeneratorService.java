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

import static org.hisp.dhis.util.Constants.RANDOM_GENERATION_CHUNK;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Setter;
import org.springframework.stereotype.Service;

/**
 * @author Luca Cambi <luca@dhis2.org>
 *     <p>Generate random values from a pattern using BigInteger for numbers and random UUID for
 *     alphanumerics.
 *     <p>x = lower case
 *     <p>X = upper case
 *     <p># = digit
 *     <p>* = digit or lower case or upper case
 *     <p>see {@link RandomPatternBuilder}
 */
@Service
@Setter(AccessLevel.PROTECTED)
public class RandomGeneratorService implements Callable<List<String>> {

  private String segmentParameter;

  @Override
  public List<String> call() throws Exception {
    LinkedList<String> patterns = new LinkedList<>();

    List<String> randomList = new ArrayList<>();

    Pattern randomPattern = Pattern.compile("[X]+|[x]+|[#]+|[*]+");
    Matcher matcher = randomPattern.matcher(segmentParameter);
    SecureRandom secureRandom = new SecureRandom();

    while (matcher.find()) {
      patterns.add(segmentParameter.substring(matcher.start(), matcher.end()));
    }

    RandomPatternBuilder patternBuilder = new RandomPatternBuilder(segmentParameter);

    for (int j = 0; j < RANDOM_GENERATION_CHUNK; j++) {
      for (String pattern : patterns) {
        switch (pattern.charAt(0)) {
          case '*':
            patternBuilder.setForRandomAll(pattern);
            break;
          case '#':
            patternBuilder.setForRandomDigits(pattern, secureRandom);
            break;
          case 'X':
            patternBuilder.setForRandomUpperCase(pattern);
            break;
          case 'x':
            patternBuilder.setForRandomLowerCase(pattern);
            break;
          default:
            break;
        }
      }

      patternBuilder.getPatternBuilders().forEach(pb -> randomList.add(pb.toString()));

      patternBuilder.resetPatternBuilder();
    }

    return randomList;
  }
}
