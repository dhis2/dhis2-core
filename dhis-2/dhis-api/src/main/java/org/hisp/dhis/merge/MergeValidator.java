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

import java.util.Set;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.MergeReport;

/**
 * @author david mackessy
 */
public interface MergeValidator {

  /**
   * Validates source & target {@link UID}s passed in the params. Validation used by all merges.
   *
   * @param params {@link MergeParams} that contain {@link UID}s to validate
   * @param mergeReport {@link MergeReport} to update
   * @param mergeType {@link MergeType}
   * @return {@link MergeRequest} to process
   */
  MergeRequest validateUIDs(
      @Nonnull MergeParams params, @Nonnull MergeReport mergeReport, @Nonnull MergeType mergeType);

  /**
   * Verifies whether the source {@link UID}s map to valid {@link IdentifiableObject}s. <br>
   * - If they are valid then they are added to verifiedSources param. <br>
   * - If any are not valid then the {@link MergeReport} is updated with an error.
   *
   * @param paramSources {@link UID}s
   * @param mergeReport to update if any error
   * @param mergeType {@link MergeType}
   * @return verified source {@link UID}s
   */
  Set<UID> verifySources(Set<UID> paramSources, MergeReport mergeReport, MergeType mergeType);

  /**
   * Checks whether the target is referenced in the sources collection <br>
   * - if the target is in the sources then an error is added to the {@link MergeReport}
   *
   * @param sources to check
   * @param target to check if in sources
   * @param mergeReport to update if any error
   * @param mergeType {@link MergeType}
   */
  void checkIsTargetInSources(
      Set<UID> sources, UID target, MergeReport mergeReport, MergeType mergeType);

  /**
   * Verifies whether the target {@link UID} maps to a valid {@link IdentifiableObject}. <br>
   * - If it's valid then a fully populated {@link MergeRequest} is returned. <br>
   * - If it's not valid then an empty {@link MergeRequest} is returned and the {@link MergeReport}
   * is updated with an error.
   *
   * @param mergeReport to update if any error
   * @param sources to return in merge request
   * @param params merge params with target to verify
   * @param mergeType {@link MergeType}
   * @return merge request
   */
  MergeRequest verifyTarget(
      MergeReport mergeReport, Set<UID> sources, MergeParams params, MergeType mergeType);
}
