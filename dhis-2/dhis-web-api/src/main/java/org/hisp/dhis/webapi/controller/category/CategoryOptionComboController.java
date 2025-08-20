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
package org.hisp.dhis.webapi.controller.category;

import static org.hisp.dhis.feedback.ErrorCode.E1129;
import static org.hisp.dhis.security.Authorities.F_CATEGORY_OPTION_COMBO_MERGE;
import static org.hisp.dhis.webapi.controller.CrudControllerAdvice.getHelpfulMessage;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.Maturity.Beta;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.MergeReport;
import org.hisp.dhis.merge.MergeParams;
import org.hisp.dhis.merge.MergeService;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/categoryOptionCombos")
@OpenApi.Document(classifiers = {"team:platform", "purpose:metadata"})
public class CategoryOptionComboController
    extends AbstractCrudController<CategoryOptionCombo, GetObjectListParams> {

  private final MergeService categoryOptionComboMergeService;

  @Beta
  @ResponseStatus(HttpStatus.OK)
  @RequiresAuthority(anyOf = F_CATEGORY_OPTION_COMBO_MERGE)
  @PostMapping(value = "/merge", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody WebMessage mergeCategoryOptionCombos(@RequestBody MergeParams params)
      throws ConflictException {
    log.info("CategoryOptionCombo merge received");

    MergeReport report;
    try {
      report = categoryOptionComboMergeService.processMerge(params);
    } catch (PersistenceException ex) {
      String helpfulMessage = getHelpfulMessage(ex);
      log.error("Error while processing CategoryOptionCombo merge: {}", helpfulMessage);
      throw ex;
    }

    log.info("CategoryOptionCombo merge processed with report: {}", report);
    return WebMessageUtils.mergeReport(report);
  }

  @Beta
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/projectedState", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody ProjectedCategoryOptionComboState projectedCategoryOptionComboState(
      @RequestBody ProvidedCategoryOptionComboState providedState) {
    log.info("CategoryOptionCombo projectedState request received: {}", providedState);

    CombinationGenerator<CategoryOptionDto> generator =
        CombinationGenerator.newInstance(getCosAsLists(providedState));
    List<List<CategoryOptionDto>> combinations = generator.getCombinations();

    Set<CategoryOptionComboDto> combinedOptionCombos = new HashSet<>();
    for (List<CategoryOptionDto> combination : combinations) {
      CategoryOptionComboDto coc1 =
          new CategoryOptionComboDto(providedState.categoryCombo.id, new HashSet<>(combination));
      combinedOptionCombos.add(coc1);
    }

    ProjectedCategoryOptionComboState generatedCategoryOptionComboState =
        new ProjectedCategoryOptionComboState(combinedOptionCombos);
    log.info("Generated CategoryOptionCombo state: {}", generatedCategoryOptionComboState);
    return generatedCategoryOptionComboState;
  }

  private List<List<CategoryOptionDto>> getCosAsLists(
      ProvidedCategoryOptionComboState providedState) {
    Set<CategoryDto> categories = providedState.categories;
    List<List<CategoryOptionDto>> categoryOptionLists = new ArrayList<>();

    for (CategoryDto category : categories) {
      categoryOptionLists.add(
          category.categoryOptions.stream().map(co -> new CategoryOptionDto(co.id)).toList());
    }
    return categoryOptionLists;
  }

  /**
   * Creating a single CategoryOptionCombo is not allowed. They should be either:
   *
   * <p>
   *
   * <ul>
   *   <li>left to be auto generated by the system
   *   <li>imported through the metadata endpoint
   * </ul>
   *
   * @param request request
   * @return WebMessage
   */
  @Override
  public WebMessage postJsonObject(HttpServletRequest request) {
    log.info("postJsonObject");
    return WebMessageUtils.conflict(E1129);
  }

  public record ProjectedCategoryOptionComboState(
      @JsonProperty Set<CategoryOptionComboDto> projectedCategoryOptionCombos) {}

  public record ProvidedCategoryOptionComboState(
      @JsonProperty CategoryComboDto categoryCombo,
      @JsonProperty Set<CategoryDto> categories,
      @JsonProperty Set<CategoryOptionDto> categoryOptions) {}

  public record CategoryDto(
      @JsonProperty UID id, @JsonProperty Set<CategoryOptionDto> categoryOptions) {}

  public record CategoryComboDto(@JsonProperty UID id, @JsonProperty Set<CategoryDto> categories) {}

  public record CategoryOptionDto(@JsonProperty UID id) {}

  public record CategoryOptionComboDto(
      @JsonProperty UID categoryCombo, @JsonProperty Set<CategoryOptionDto> categoryOptions) {}
}



