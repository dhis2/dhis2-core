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
package org.hisp.dhis.dxf2.metadata;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsonpatch.JsonPatchManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultPatchService implements PatchService {
  private final IdentifiableObjectManager manager;
  private final MetadataImportService importService;
  private final JsonPatchManager jsonPatchManager;

  @Transactional
  public <T extends IdentifiableObject> ImportReport doPatch(
      UserDetails currentUser,
      JsonPatch patch,
      T persistedObject,
      MetadataImportParams params,
      Consumer<T> prePatch)
      throws JsonPatchException {

    final T patchedObject = doPatchInternal(patch, persistedObject);

    // Do not allow changing IDs
    ((BaseIdentifiableObject) patchedObject).setId(persistedObject.getId());

    // Do not allow changing UIDs
    ((BaseIdentifiableObject) patchedObject).setUid(persistedObject.getUid());

    prePatch.accept(patchedObject);

    params.setUser(UID.of(currentUser)).setImportStrategy(ImportStrategy.UPDATE);

    ImportReport importReport =
        importService.importMetadata(params, new MetadataObjects().addObject(patchedObject));

    return importReport;
  }

  private <T extends IdentifiableObject> T doPatchInternal(JsonPatch patch, T persistedObject)
      throws JsonPatchException {

    final T patchedObject = jsonPatchManager.apply(patch, persistedObject);

    if (patchedObject instanceof User) {
      // Reset to avoid non owning properties (here UserGroups) to be
      // operated on in the import.
      manager.resetNonOwnerProperties(patchedObject);
    }

    return patchedObject;
  }
}
