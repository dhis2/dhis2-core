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

import java.util.Objects;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.schema.descriptors.CategoryComboSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags("metadata")
@Controller
@RequestMapping(value = CategoryComboSchemaDescriptor.API_ENDPOINT)
public class CategoryComboController extends AbstractCrudController<CategoryCombo> {
  @Autowired private CategoryService categoryService;

  @Autowired private DataValueService dataValueService;

  @GetMapping("/{uid}/metadata")
  public ResponseEntity<MetadataExportParams> getDataSetWithDependencies(
      @PathVariable("uid") String pvUid,
      @RequestParam(required = false, defaultValue = "false") boolean download)
      throws NotFoundException {
    CategoryCombo categoryCombo = categoryService.getCategoryCombo(pvUid);

    if (categoryCombo == null) {
      throw new NotFoundException(getEntityClass(), pvUid);
    }

    MetadataExportParams exportParams =
        exportService.getParamsFromMap(contextService.getParameterValuesMap());
    exportService.validate(exportParams);
    exportParams.setObjectExportWithDependencies(categoryCombo);

    return ResponseEntity.ok(exportParams);
  }

  @Override
  protected void preUpdateEntity(CategoryCombo entity, CategoryCombo newEntity)
      throws ConflictException {
    checkNoDataValueBecomesInaccessible(entity, newEntity);
  }

  @Override
  protected void prePatchEntity(CategoryCombo entity, CategoryCombo newEntity)
      throws ConflictException {
    checkNoDataValueBecomesInaccessible(entity, newEntity);
  }

  private void checkNoDataValueBecomesInaccessible(CategoryCombo entity, CategoryCombo newEntity)
      throws ConflictException {
    if (!Objects.equals(entity.getCategories(), newEntity.getCategories())
        && dataValueService.dataValueExists(entity)) {
      throw new ConflictException(ErrorCode.E1120);
    }
  }

  @Override
  public void postCreateEntity(CategoryCombo categoryCombo) {
    categoryService.updateOptionCombos(categoryCombo);
  }

  @Override
  public void postUpdateEntity(CategoryCombo categoryCombo) {
    categoryService.updateOptionCombos(categoryCombo);
  }
}
