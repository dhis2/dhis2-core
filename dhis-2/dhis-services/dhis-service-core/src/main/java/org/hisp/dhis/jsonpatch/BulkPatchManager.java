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
package org.hisp.dhis.jsonpatch;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contains functions to apply {@link JsonPatch} to one or multiple object types. */
@Service
@AllArgsConstructor
public class BulkPatchManager {
  private final JsonPatchManager jsonPatchManager;

  private final BulkPatchValidatorService validatorFactory;

  /**
   * Apply one {@link JsonPatch} to multiple objects of same class.
   *
   * @param bulkJsonPatch {@link BulkJsonPatch} instance contains the data parsed from request
   *     payload.
   * @param patchParameters {@link BulkPatchParameters} contains all parameters used for patch
   *     function.
   * @return List of patched objects
   */
  @Transactional(readOnly = true)
  public List<IdentifiableObject> applyPatch(
      BulkJsonPatch bulkJsonPatch, BulkPatchParameters patchParameters) {
    PatchBundle bundle = validatorFactory.validate(bulkJsonPatch, patchParameters);

    if (bundle.isEmpty()) {
      return Collections.emptyList();
    }

    return bundle.getIds().stream()
        .map(
            id ->
                applySafely(
                    bundle.getJsonPatch(id),
                    bundle.getEntity(id),
                    patchParameters,
                    patched -> postApply(id, patched)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  /**
   * Apply multiple {@link JsonPatch} to multiple objects of different classes from given {@link
   * BulkJsonPatches}.
   *
   * <p>Each object has its own {@link JsonPatch}.
   */
  @Transactional(readOnly = true)
  public List<IdentifiableObject> applyPatches(
      BulkJsonPatches patches, BulkPatchParameters patchParameters) {
    PatchBundle bundle = validatorFactory.validate(patches, patchParameters);

    if (bundle.isEmpty()) {
      return Collections.emptyList();
    }

    return bundle.getIds().stream()
        .map(
            id ->
                applySafely(
                    bundle.getJsonPatch(id),
                    bundle.getEntity(id),
                    patchParameters,
                    patched -> postApply(id, patched)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  /**
   * Try to apply given {@link JsonPatch} to given entity by calling {@link JsonPatchManager#apply}.
   *
   * <p>If there is an error, add it to the given {@link ObjectReport} list and return {@link
   * Optional#empty()}.
   */
  private Optional<IdentifiableObject> applySafely(
      JsonPatch patch,
      IdentifiableObject entity,
      BulkPatchParameters parameters,
      Consumer<Optional<IdentifiableObject>> entityConsumer) {
    try {
      Optional<IdentifiableObject> patched =
          Optional.ofNullable(jsonPatchManager.apply(patch, entity));
      entityConsumer.accept(patched);
      return patched;
    } catch (JsonPatchException e) {
      parameters.addTypeReport(
          createTypeReport(
              entity.getClass(),
              entity.getUid(),
              new ErrorReport(entity.getClass(), ErrorCode.E6003, e.getMessage())));
      return Optional.empty();
    }
  }

  /** Apply some additional logics for patched object. */
  private void postApply(String id, Optional<IdentifiableObject> patchedObject) {
    if (!patchedObject.isPresent()) {
      return;
    }

    // we don't allow changing UIDs
    ((BaseIdentifiableObject) patchedObject.get()).setUid(id);

    // Only supports new Sharing format
    ((BaseIdentifiableObject) patchedObject.get()).clearLegacySharingCollections();
  }

  private static TypeReport createTypeReport(Class<?> klass, String id, ErrorReport errorReport) {
    TypeReport typeReport = new TypeReport(klass);
    ObjectReport objectReport = new ObjectReport(klass, 0, id);
    objectReport.addErrorReport(errorReport);
    typeReport.addObjectReport(objectReport);
    return typeReport;
  }
}
