/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
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
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMergeValidator implements MergeValidator {

  private final IdentifiableObjectManager manager;

  @Override
  public MergeRequest validateUIDs(
      @Nonnull MergeParams params, @Nonnull MergeReport mergeReport, @Nonnull MergeType mergeType) {
    log.info("Validating {} merge request", mergeType);
    mergeReport.setMergeType(mergeType);

    Set<UID> verifiedSources = verifySources(params.getSources(), mergeReport, mergeType);

    checkIsTargetInSources(verifiedSources, params.getTarget(), mergeReport, mergeType);

    return verifyTarget(mergeReport, verifiedSources, params, mergeType);
  }

  @Override
  public Set<UID> verifySources(
      Set<UID> paramSources, MergeReport mergeReport, MergeType mergeType) {
    Set<UID> verifiedSources = new HashSet<>();
    Optional.ofNullable(paramSources)
        .filter(CollectionUtils::isNotEmpty)
        .ifPresentOrElse(
            ids -> getSourcesAndVerify(ids, mergeReport, verifiedSources, mergeType),
            () ->
                mergeReport.addErrorMessage(
                    new ErrorMessage(ErrorCode.E1530, mergeType.getName())));
    return verifiedSources;
  }

  @Override
  public void checkIsTargetInSources(
      Set<UID> sources, UID target, MergeReport mergeReport, MergeType mergeType) {
    if (sources.contains(target))
      mergeReport.addErrorMessage(
          new ErrorMessage(ErrorCode.E1532, mergeType.getName(), mergeType.getName()));
  }

  @Override
  public MergeRequest verifyTarget(
      MergeReport mergeReport, Set<UID> sources, MergeParams params, MergeType mergeType) {
    return getAndVerify(params.getTarget(), mergeReport, MergeObjectType.TARGET, mergeType)
        .map(
            uid ->
                MergeRequest.builder()
                    .sources(sources)
                    .target(uid)
                    .deleteSources(params.isDeleteSources())
                    .dataMergeStrategy(params.getDataMergeStrategy())
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
   * @param mergeType {@link MergeType}
   * @return optional UID
   */
  private Optional<UID> getAndVerify(
      UID uid, MergeReport mergeReport, MergeObjectType mergeObjectType, MergeType mergeType) {

    if (uid != null) {
      return Optional.ofNullable(manager.get(mergeType.getClazz(), uid.getValue()))
          .map(i -> UID.of(i.getUid()))
          .or(reportError(uid, mergeReport, mergeObjectType, mergeType));
    } else {
      mergeReport.addErrorMessage(new ErrorMessage(ErrorCode.E1531, mergeType.getName()));
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
   * @param mergeType {@link MergeType}
   */
  private void getSourcesAndVerify(
      Set<UID> uids, MergeReport report, Set<UID> verifiedSources, MergeType mergeType) {
    uids.forEach(
        uid ->
            getAndVerify(uid, report, MergeObjectType.SOURCE, mergeType)
                .ifPresent(verifiedSources::add));
  }

  private Supplier<Optional<UID>> reportError(
      UID uid, MergeReport mergeReport, MergeObjectType mergeObjectType, MergeType mergeType) {
    return () -> {
      mergeReport.addErrorMessage(
          new ErrorMessage(ErrorCode.E1533, mergeObjectType.toString(), mergeType.getName(), uid));
      return Optional.empty();
    };
  }

  enum MergeObjectType {
    SOURCE,
    TARGET
  }
}
