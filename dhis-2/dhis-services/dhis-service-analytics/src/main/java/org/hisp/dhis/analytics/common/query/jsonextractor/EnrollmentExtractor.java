/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.query.jsonextractor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;

/**
 * This enum represents a mapping between static dimensions and their corresponding getter method
 */
@RequiredArgsConstructor
enum EnrollmentExtractor {
  ENROLLMENTDATE(
      DimensionParam.StaticDimension.ENROLLMENTDATE,
      a -> JsonExtractorUtils.getFormattedDate(a.getEnrollmentDate())),
  INCIDENTDATE(
      DimensionParam.StaticDimension.INCIDENTDATE,
      a -> JsonExtractorUtils.getFormattedDate(a.getIncidentDate())),
  OUNAME(DimensionParam.StaticDimension.OUNAME, JsonEnrollment::getOrgUnitName),
  OUCODE(DimensionParam.StaticDimension.OUCODE, JsonEnrollment::getOrgUnitCode),
  OUNAMEHIERARCHY(
      DimensionParam.StaticDimension.OUNAMEHIERARCHY, JsonEnrollment::getOrgUnitNameHierarchy),
  ENROLLMENT_STATUS(
      List.of(StaticDimension.ENROLLMENT_STATUS, StaticDimension.PROGRAM_STATUS),
      JsonEnrollment::getEnrollmentStatus);

  // The static dimensions that this extractor is responsible for
  private final List<DimensionParam.StaticDimension> dimensions;

  // The function that extracts the value of the dimension from the enrollment
  @Getter private final Function<JsonEnrollment, Object> extractor;

  EnrollmentExtractor(
      StaticDimension staticDimension, Function<JsonEnrollment, Object> getOrgUnitNameHierarchy) {
    this(List.of(staticDimension), getOrgUnitNameHierarchy);
  }

  static EnrollmentExtractor byDimension(DimensionParam.StaticDimension dimension) {
    return Arrays.stream(values())
        .filter(
            enrollmentExtractor ->
                enrollmentExtractor.dimensions.stream().anyMatch(dimension::equals))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No enrollment extractor is defined for static dimension " + dimension));
  }
}
