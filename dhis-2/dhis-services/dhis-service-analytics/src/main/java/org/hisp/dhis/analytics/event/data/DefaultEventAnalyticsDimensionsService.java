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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.common.DimensionsServiceCommon.OperationType.AGGREGATE;
import static org.hisp.dhis.analytics.common.DimensionsServiceCommon.OperationType.QUERY;
import static org.hisp.dhis.analytics.common.DimensionsServiceCommon.collectDimensions;
import static org.hisp.dhis.analytics.common.DimensionsServiceCommon.filterByValueType;
import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.PrefixedDimensions.ofDataElements;
import static org.hisp.dhis.common.PrefixedDimensions.ofItemsWithProgram;
import static org.hisp.dhis.common.PrefixedDimensions.ofProgramIndicators;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.event.EventAnalyticsDimensionsService;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultEventAnalyticsDimensionsService implements EventAnalyticsDimensionsService {
  private final ProgramStageService programStageService;

  private final ProgramService programService;

  private final CategoryService categoryService;

  private final AclService aclService;

  @Override
  public List<PrefixedDimension> getQueryDimensionsByProgramStageId(
      String programId, String programStageId) {

    Set<ProgramStage> programStages = getProgramStages(programId, programStageId);

    if (CollectionUtils.isNotEmpty(programStages)) {
      return programStages.stream()
          .map(this::dimensions)
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    }
    return List.of();
  }

  private Set<ProgramStage> getProgramStages(String programId, String programStageId) {
    checkProgramStageIsInProgramIfNecessary(programId, programStageId);
    return Optional.ofNullable(programStageId)
        .filter(StringUtils::isNotBlank)
        .map(programStageService::getProgramStage)
        .map(Set::of)
        .orElseGet(
            () ->
                Optional.of(programId)
                    .filter(StringUtils::isNotBlank)
                    .map(programService::getProgram)
                    .map(Program::getProgramStages)
                    .orElse(Collections.emptySet()));
  }

  private void checkProgramStageIsInProgramIfNecessary(String programId, String programStageId) {
    if (StringUtils.isNotBlank(programStageId) && StringUtils.isNotBlank(programId)) {
      Optional<String> matchingProgramUid =
          Optional.of(programStageId)
              .map(programStageService::getProgramStage)
              .map(ProgramStage::getProgram)
              .map(Program::getUid)
              .filter(programId::equals);

      if (matchingProgramUid.isEmpty()) {
        throw new IllegalQueryException(
            new ErrorMessage(ErrorCode.E7236, programStageId, programId));
      }
    }
  }

  private List<PrefixedDimension> dimensions(ProgramStage programStage) {

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    return Optional.of(programStage)
        .map(ProgramStage::getProgram)
        .map(
            p ->
                collectDimensions(
                    List.of(
                        ofProgramIndicators(
                            p.getProgramIndicators().stream()
                                .filter(pi -> aclService.canRead(currentUserDetails, pi))
                                .collect(Collectors.toSet())),
                        filterByValueType(QUERY, ofDataElements(programStage)),
                        filterByValueType(
                            QUERY,
                            ofItemsWithProgram(p, getTeasIfRegistrationAndNotConfidential(p))),
                        ofItemsWithProgram(p, getCategories(p)),
                        ofItemsWithProgram(p, getAttributeCategoryOptionGroupSetsIfNeeded(p)))))
        .orElse(List.of());
  }

  @Override
  public List<PrefixedDimension> getAggregateDimensionsByProgramStageId(String programStageId) {
    return Optional.of(programStageId)
        .map(programStageService::getProgramStage)
        .map(
            ps ->
                collectDimensions(
                    List.of(
                        filterByValueType(AGGREGATE, ofDataElements(ps)),
                        filterByValueType(
                            AGGREGATE,
                            ofItemsWithProgram(
                                ps.getProgram(), ps.getProgram().getTrackedEntityAttributes())),
                        ofItemsWithProgram(ps.getProgram(), getCategories(ps.getProgram())),
                        ofItemsWithProgram(
                            ps.getProgram(),
                            getAttributeCategoryOptionGroupSetsIfNeeded(ps.getProgram())))))
        .orElse(List.of());
  }

  private List<CategoryOptionGroupSet> getAttributeCategoryOptionGroupSetsIfNeeded(
      Program program) {
    return Optional.of(program)
        .filter(Program::hasNonDefaultCategoryCombo)
        .map(
            unused ->
                categoryService.getAllCategoryOptionGroupSets().stream()
                    .filter(this::isTypeAttribute)
                    .collect(Collectors.toList()))
        .orElse(List.of());
  }

  private boolean isTypeAttribute(CategoryOptionGroupSet categoryOptionGroupSet) {
    return ATTRIBUTE == categoryOptionGroupSet.getDataDimensionType();
  }

  private List<Category> getCategories(Program program) {
    return Optional.of(program)
        .filter(Program::hasNonDefaultCategoryCombo)
        .map(Program::getCategoryCombo)
        .map(CategoryCombo::getCategories)
        .orElse(List.of());
  }

  private List<TrackedEntityAttribute> getTeasIfRegistrationAndNotConfidential(Program program) {
    return Optional.of(program)
        .filter(Program::isRegistration)
        .map(Program::getTrackedEntityAttributes)
        .orElse(List.of())
        .stream()
        .filter(this::isNotConfidential)
        .collect(Collectors.toList());
  }

  private boolean isNotConfidential(TrackedEntityAttribute trackedEntityAttribute) {
    return !trackedEntityAttribute.isConfidentialBool();
  }
}
