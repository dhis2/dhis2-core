/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static lombok.AccessLevel.PRIVATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.SUPPORTED_EVENT_STATIC_DIMENSIONS;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamObjectType.ORGANISATION_UNIT;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.feedback.ErrorCode;

/**
 * Validates that stage-specific headers have matching stage-specific dimensions. When a header
 * references an event-level static dimension (EVENT_DATE, SCHEDULED_DATE, OU, EVENT_STATUS) for a
 * specific program stage, there must be a corresponding dimension with the same program stage UID
 * and same static dimension type.
 */
@NoArgsConstructor(access = PRIVATE)
public class StageSpecificHeaderValidator {

  /**
   * Validates that all stage-specific headers have matching dimensions.
   *
   * @param parsedHeaders the parsed header dimension identifiers
   * @param dimensionIdentifiers the parsed dimension identifiers
   * @throws org.hisp.dhis.common.IllegalQueryException if a stage-specific header doesn't have a
   *     matching dimension
   */
  public static void validate(
      Set<DimensionIdentifier<DimensionParam>> parsedHeaders,
      List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers) {

    for (DimensionIdentifier<DimensionParam> header : parsedHeaders) {
      if (isStageSpecificStaticDimension(header)) {
        validateMatchingDimensionExists(header, dimensionIdentifiers);
      }
    }
  }

  /**
   * Checks if the dimension identifier is a stage-specific static dimension (has program stage and
   * is one of the supported event-level static dimensions).
   */
  private static boolean isStageSpecificStaticDimension(DimensionIdentifier<DimensionParam> dimId) {
    if (!dimId.isEventDimension()) {
      return false;
    }

    DimensionParam param = dimId.getDimension();
    if (!param.isStaticDimension()) {
      return false;
    }

    StaticDimension staticDim = param.getStaticDimension();
    return SUPPORTED_EVENT_STATIC_DIMENSIONS.contains(staticDim);
  }

  /**
   * Validates that a matching dimension exists for the given header. A matching dimension must have
   * the same program stage UID and the same static dimension type.
   */
  private static void validateMatchingDimensionExists(
      DimensionIdentifier<DimensionParam> header,
      Collection<DimensionIdentifier<DimensionParam>> dimensions) {

    String headerStageUid = header.getProgramStage().getElement().getUid();
    StaticDimension headerStaticDim = header.getDimension().getStaticDimension();

    boolean hasMatchingDimension =
        dimensions.stream()
            .filter(DimensionIdentifier::isEventDimension)
            .anyMatch(dim -> matchesDimension(dim, headerStageUid, headerStaticDim));

    if (!hasMatchingDimension) {
      throwIllegalQueryEx(
          ErrorCode.E7252,
          headerStageUid + "." + headerStaticDim.getHeaderName(),
          headerStageUid,
          headerStaticDim.name());
    }
  }

  /**
   * Checks if a dimension matches the header's stage UID and static dimension type. For OU
   * dimensions, this also matches DimensionalObject-based OU dimensions (which occur when OU items
   * are resolved through dataQueryService).
   */
  private static boolean matchesDimension(
      DimensionIdentifier<DimensionParam> dim,
      String headerStageUid,
      StaticDimension headerStaticDim) {
    String dimStageUid = dim.getProgramStage().getElement().getUid();
    if (!headerStageUid.equals(dimStageUid)) {
      return false;
    }

    DimensionParam dimParam = dim.getDimension();

    // For static dimensions, compare directly
    if (dimParam.isStaticDimension()) {
      return headerStaticDim == dimParam.getStaticDimension();
    }

    // For OU header, also match DimensionalObject-based OU dimensions
    // (these occur when OU items like USER_ORGUNIT or specific UIDs are resolved)
    if (headerStaticDim == StaticDimension.OU) {
      return dimParam.getDimensionParamObjectType() == ORGANISATION_UNIT;
    }

    return false;
  }
}
