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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * @author Stian Sandvold
 */
@Service("org.hisp.dhis.textpattern.TextPatternService")
public class DefaultTextPatternService implements TextPatternService {
  @Override
  public String resolvePattern(TextPattern pattern, Map<String, String> values)
      throws TextPatternGenerationException {
    StringBuilder resolvedPattern = new StringBuilder();

    for (TextPatternSegment segment : pattern.getSegments()) {
      if (isRequired(segment)) {
        resolvedPattern.append(handleRequiredValue(segment, getSegmentValue(segment, values)));
      } else if (isOptional(segment)) {
        resolvedPattern.append(handleOptionalValue(segment, getSegmentValue(segment, values)));
      } else {
        resolvedPattern.append(handleFixedValues(segment));
      }
    }

    return resolvedPattern.toString();
  }

  @Override
  public Map<String, List<String>> getRequiredValues(TextPattern pattern) {
    return Map.of(
        "REQUIRED",
            pattern.getSegments().stream()
                .filter(this::isRequired)
                .map(segment -> segment.getMethod().name())
                .collect(Collectors.toList()),
        "OPTIONAL",
            pattern.getSegments().stream()
                .filter(this::isOptional)
                .map(segment -> segment.getMethod().name())
                .collect(Collectors.toList()));
  }

  @Override
  public boolean validate(TextPattern textPattern, String text) {
    return TextPatternValidationUtils.validateTextPatternValue(textPattern, text);
  }

  private String handleFixedValues(TextPatternSegment segment) {
    if (TextPatternMethod.CURRENT_DATE.getType().validatePattern(segment.getRawSegment())) {
      return new SimpleDateFormat(segment.getParameter()).format(new Date());
    } else {
      return segment.getParameter();
    }
  }

  private String handleOptionalValue(TextPatternSegment segment, String value)
      throws TextPatternGenerationException {
    if (value != null && !TextPatternValidationUtils.validateSegmentValue(segment, value)) {
      throw new TextPatternGenerationException("Supplied optional value is invalid");
    } else if (value != null) {
      return getFormattedValue(segment, value);
    } else {
      return segment.getRawSegment();
    }
  }

  private String handleRequiredValue(TextPatternSegment segment, String value)
      throws TextPatternGenerationException {
    if (value == null) {
      throw new TextPatternGenerationException(
          "Missing required value '" + segment.getMethod().name() + "'");
    }

    String res = getFormattedValue(segment, value);

    if (res == null || !TextPatternValidationUtils.validateSegmentValue(segment, res)) {
      throw new TextPatternGenerationException(
          "Value is invalid: " + segment.getMethod().name() + " -> " + value);
    }

    return res;
  }

  private String getFormattedValue(TextPatternSegment segment, String value) {
    MethodType methodType = segment.getMethod().getType();

    return methodType.getFormattedText(segment.getParameter(), value);
  }

  private String getSegmentValue(TextPatternSegment segment, Map<String, String> values) {
    return values.get(segment.getMethod().name());
  }

  private boolean isRequired(TextPatternSegment segment) {
    return segment.getMethod().isRequired();
  }

  private boolean isOptional(TextPatternSegment segment) {
    return segment.getMethod().isOptional();
  }
}
