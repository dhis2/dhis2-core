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
package org.hisp.dhis.webapi.controller.category;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboService;
import org.hisp.dhis.category.CategoryOptionComboUpdateDto;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.schema.descriptors.CategoryOptionComboSchemaDescriptor;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags("metadata")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = CategoryOptionComboSchemaDescriptor.API_ENDPOINT)
public class CategoryOptionComboController extends AbstractCrudController<CategoryOptionCombo> {

  private final CategoryOptionComboService categoryOptionComboService;

  /**
   * {@link CategoryOptionCombo} needs a very specific update implementation. Only 3 fields are
   * updatable through the PUT endpoint: <br>
   * - attributeValues <br>
   * - code <br>
   * - ignoreApproval <br>
   * Metadata import endpoint has very different behaviour for importing {@link
   * CategoryOptionCombo}s and is not suitable for individual updates.
   */
  @Override
  public WebMessage putJsonObject(
      @PathVariable String uid, @CurrentUser User currentUser, HttpServletRequest request)
      throws NotFoundException, ForbiddenException, ConflictException, IOException {
    List<CategoryOptionCombo> persisted = getEntity(uid);
    if (persisted.isEmpty()) {
      throw new NotFoundException(getEntityClass(), uid);
    }
    CategoryOptionCombo coc = persisted.get(0);
    updatePermissionCheck(currentUser, coc);

    CategoryOptionComboUpdateDto cocUpdate =
        jsonMapper.readValue(request.getInputStream(), CategoryOptionComboUpdateDto.class);
    categoryOptionComboService.updateCoc(coc, cocUpdate);
    return WebMessageUtils.ok();
  }

  /**
   * {@link CategoryOptionCombo} needs a very specific update implementation. Only 3 fields are
   * updatable through the PATCH endpoint: <br>
   * - attributeValues <br>
   * - code <br>
   * - ignoreApproval <br>
   * Metadata import has very different behaviour for importing {@link CategoryOptionCombo}s and is
   * not suitable for individual updates.
   */
  @Override
  public WebMessage patchObject(
      @PathVariable String uid,
      Map<String, String> rpParameters,
      @CurrentUser User currentUser,
      HttpServletRequest request)
      throws NotFoundException,
          ForbiddenException,
          ConflictException,
          IOException,
          JsonPatchException {
    List<CategoryOptionCombo> persisted = getEntity(uid);
    if (persisted.isEmpty()) {
      throw new NotFoundException(getEntityClass(), uid);
    }
    CategoryOptionCombo coc = persisted.get(0);
    updatePermissionCheck(currentUser, coc);

    JsonPatch patch = jsonMapper.readValue(request.getInputStream(), JsonPatch.class);
    if (patch.getOperations().stream()
        .map(op -> op.getPath().getMatchingProperty())
        .anyMatch(
            property -> !Set.of("attributeValues", "code", "ignoreApproval").contains(property))) {
      throw new ConflictException(ErrorCode.E1134);
    }

    CategoryOptionCombo categoryOptionCombo = doPatch(patch, coc);
    CategoryOptionComboUpdateDto cocUpdate =
        jsonMapper.convertValue(categoryOptionCombo, CategoryOptionComboUpdateDto.class);
    categoryOptionComboService.updateCoc(coc, cocUpdate);
    return WebMessageUtils.ok();
  }
}
