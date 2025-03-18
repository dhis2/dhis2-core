/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.validation;

import static org.hisp.dhis.security.Authorities.F_PERFORM_MAINTENANCE;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPreset;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultService;
import org.hisp.dhis.validation.ValidationResultsDeletionRequest;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.hisp.dhis.webapi.controller.AbstractFullReadOnlyController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Stian Sandvold
 */
@OpenApi.Document(entity = ValidationResult.class)
@OpenApi.EntityType(ValidationResult.class)
@RestController
@RequestMapping("/api/validationResults")
@ApiVersion({DhisApiVersion.ALL, DhisApiVersion.DEFAULT})
@RequiredArgsConstructor
public class ValidationResultController {

  private final FieldFilterService fieldFilterService;
  private final ValidationResultService validationResultService;

  @GetMapping
  @OpenApi.Response(AbstractFullReadOnlyController.GetObjectListResponse.class)
  public @ResponseBody RootNode getObjectList(
      ValidationResultQuery query, HttpServletResponse response) {
    List<String> fields = query.getFields();

    if (fields == null || fields.isEmpty()) {
      fields = FieldPreset.ALL.getFields();
    }

    List<ValidationResult> validationResults = validationResultService.getValidationResults(query);

    RootNode rootNode = NodeUtils.createMetadata();

    if (!query.isSkipPaging()) {
      long total = validationResultService.countValidationResults(query);
      rootNode.addChild(
          NodeUtils.createPager(new Pager(query.getPage(), total, query.getPageSize())));
    }

    rootNode.addChild(
        fieldFilterService.toCollectionNode(
            ValidationResult.class, new FieldFilterParams(validationResults, fields)));

    setNoStore(response);
    return rootNode;
  }

  @GetMapping(value = "/{id}")
  public @ResponseBody ValidationResult getObject(@PathVariable int id) throws NotFoundException {
    ValidationResult result = validationResultService.getById(id);
    if (result == null) throw new NotFoundException(ValidationResult.class, "" + id);
    return result;
  }

  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  @DeleteMapping(value = "/{id}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void delete(@PathVariable int id) throws NotFoundException {
    ValidationResult result = validationResultService.getById(id);
    if (result == null) throw new NotFoundException(ValidationResult.class, "" + id);
    validationResultService.deleteValidationResult(result);
  }

  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  @DeleteMapping
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void deleteValidationResults(ValidationResultsDeletionRequest request) {
    validationResultService.deleteValidationResults(request);
  }
}
