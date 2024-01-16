/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.springframework.stereotype.Component;

/**
 * Class that performs initial generic validation of a merge. {@link MergeParams} use {@link UID}s
 * which allows validation using the {@link IdentifiableObjectManager}. This should be suitable for
 * most initial validations of a merge. Any additional validation can be performed within its
 * specific implementation.
 *
 * @author david mackessy
 */
@Component
@RequiredArgsConstructor
public class DefaultMergeValidator implements MergeValidator {

  private final IdentifiableObjectManager manager;
  private static final String INDICATOR_TYPE = "IndicatorType";
  private static final String INDICATOR = "Indicator";
  private static final String MERGE_ERROR = "Unexpected value retrieving merge error code: ";

  @Override
  public <T extends IdentifiableObject> void verifySources(
      Set<UID> paramSources, Set<UID> verifiedSources, MergeReport mergeReport, Class<T> clazz) {
    Optional.ofNullable(paramSources)
        .filter(CollectionUtils::isNotEmpty)
        .ifPresentOrElse(
            ids -> getSourcesAndVerify(ids, mergeReport, verifiedSources, clazz),
            () ->
                mergeReport.addErrorMessage(
                    new ErrorMessage(missingSourceErrorCode(clazz.getSimpleName()))));
  }

  @Override
  public <T extends IdentifiableObject> void checkIsTargetInSources(
      Set<UID> sources, UID target, MergeReport mergeReport, Class<T> clazz) {
    if (sources.contains(target))
      mergeReport.addErrorMessage(
          new ErrorMessage(targetNotSourceErrorCode(clazz.getSimpleName())));
  }

  @Override
  public <T extends IdentifiableObject> MergeRequest verifyTarget(
      MergeReport mergeReport, Set<UID> sources, MergeParams params, Class<T> clazz) {
    return getAndVerify(params.getTarget(), mergeReport, MergeObjectType.TARGET.name(), clazz)
        .map(
            t ->
                MergeRequest.builder()
                    .sources(sources)
                    .target(t)
                    .deleteSources(params.isDeleteSources())
                    .build())
        .orElse(MergeRequest.empty());
  }

  /**
   * Verifies whether the {@link UID} maps to a valid {@link IdentifiableObject}. <br>
   * - If a valid {@link IdentifiableObject} is found then an {@link Optional} of the {@link UID} is
   * returned. <br>
   * - If a valid {@link IdentifiableObject} is not found then an empty {@link Optional} is returned
   * and an error is added to the {@link MergeReport}.
   *
   * @param uid to verify
   * @param mergeReport to update
   * @param mergeObjectType indicating whether this is a source or target
   * @param clazz {@link IdentifiableObject} type
   * @return optional UID
   */
  private <T extends IdentifiableObject> Optional<UID> getAndVerify(
      UID uid, MergeReport mergeReport, String mergeObjectType, Class<T> clazz) {

    if (uid != null) {
      return Optional.ofNullable(manager.get(clazz, uid.getValue()))
          .map(i -> UID.of(i.getUid()))
          .or(reportError(uid, mergeReport, mergeObjectType, clazz));
    } else {
      mergeReport.addErrorMessage(
          new ErrorMessage(noTargetErrorCode(clazz.getSimpleName()), mergeObjectType, uid));
      return Optional.empty();
    }
  }

  /**
   * Verifies whether the source {@link UID}s map to a valid {@link IdentifiableObject}. <br>
   * - If they are valid then they are added to the sources set. <br>
   * - If they are not valid then the {@link MergeReport} is updated with an error for each one.
   *
   * @param uids to verify
   * @param report to update with any error
   * @param verifiedSources to update with verified uids
   * @param clazz {@link IdentifiableObject} type
   */
  private <T extends IdentifiableObject> void getSourcesAndVerify(
      Set<UID> uids, MergeReport report, Set<UID> verifiedSources, Class<T> clazz) {
    uids.forEach(
        uid ->
            getAndVerify(uid, report, MergeObjectType.SOURCE.name(), clazz)
                .ifPresent(verifiedSources::add));
  }

  private <T extends IdentifiableObject> Supplier<Optional<UID>> reportError(
      UID uid, MergeReport mergeReport, String mergeObjectType, Class<T> clazz) {
    return () -> {
      mergeReport.addErrorMessage(
          new ErrorMessage(doesNotExistErrorCode(clazz.getSimpleName()), mergeObjectType, uid));
      return Optional.empty();
    };
  }

  /**
   * Methods to get the appropriate error code based on the Object type
   *
   * @param clazz class name
   * @return error code
   */
  private ErrorCode missingSourceErrorCode(String clazz) {
    return switch (clazz) {
      case INDICATOR_TYPE -> ErrorCode.E1530;
      case INDICATOR -> ErrorCode.E1540;
      default -> throw new IllegalStateException(MERGE_ERROR + clazz);
    };
  }

  private ErrorCode noTargetErrorCode(String clazz) {
    return switch (clazz) {
      case INDICATOR_TYPE -> ErrorCode.E1531;
      case INDICATOR -> ErrorCode.E1541;
      default -> throw new IllegalStateException(MERGE_ERROR + clazz);
    };
  }

  private ErrorCode targetNotSourceErrorCode(String clazz) {
    return switch (clazz) {
      case INDICATOR_TYPE -> ErrorCode.E1532;
      case INDICATOR -> ErrorCode.E1542;
      default -> throw new IllegalStateException(MERGE_ERROR + clazz);
    };
  }

  private ErrorCode doesNotExistErrorCode(String clazz) {
    return switch (clazz) {
      case INDICATOR_TYPE -> ErrorCode.E1533;
      case INDICATOR -> ErrorCode.E1543;
      default -> throw new IllegalStateException(MERGE_ERROR + clazz);
    };
  }

  enum MergeObjectType {
    SOURCE,
    TARGET
  }
}
