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
package org.hisp.dhis.tracker.export.trackedentity;

import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateOrgUnitMode;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateUser;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps {@link TrackedEntityOperationParams} to {@link TrackedEntityQueryParams} which is used to
 * fetch tracked entities from the DB.
 */
@Component
@RequiredArgsConstructor
class TrackedEntityOperationParamsMapper {
  @Nonnull private final OrganisationUnitService organisationUnitService;

  @Nonnull private final ProgramService programService;

  @Nonnull private final TrackedEntityTypeService trackedEntityTypeService;

  @Nonnull private final TrackedEntityAttributeService attributeService;

  @Nonnull private final AclService aclService;

  @Transactional(readOnly = true)
  public TrackedEntityQueryParams map(TrackedEntityOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    Program program = validateProgram(operationParams.getProgramUid());
    ProgramStage programStage = validateProgramStage(operationParams, program);
    TrackedEntityType trackedEntityType =
        validateTrackedEntityType(operationParams.getTrackedEntityTypeUid());

    User user = operationParams.getUser();
    Set<OrganisationUnit> requestedOrgUnits =
        validateRequestedOrgUnit(operationParams.getOrganisationUnits());
    validateUser(
        user, program, programStage, requestedOrgUnits, aclService, organisationUnitService);

    validateOrgUnitMode(operationParams.getOrgUnitMode(), user, program);

    TrackedEntityQueryParams params = new TrackedEntityQueryParams();
    mapAttributeFilters(params, operationParams.getFilters());

    mapOrderParam(params, operationParams.getOrder());

    params
        .setProgram(program)
        .setProgramStage(programStage)
        .setProgramStatus(operationParams.getProgramStatus())
        .setFollowUp(operationParams.getFollowUp())
        .setLastUpdatedStartDate(operationParams.getLastUpdatedStartDate())
        .setLastUpdatedEndDate(operationParams.getLastUpdatedEndDate())
        .setLastUpdatedDuration(operationParams.getLastUpdatedDuration())
        .setProgramEnrollmentStartDate(operationParams.getProgramEnrollmentStartDate())
        .setProgramEnrollmentEndDate(operationParams.getProgramEnrollmentEndDate())
        .setProgramIncidentStartDate(operationParams.getProgramIncidentStartDate())
        .setProgramIncidentEndDate(operationParams.getProgramIncidentEndDate())
        .setTrackedEntityType(trackedEntityType)
        .setOrgUnits(requestedOrgUnits)
        .setOrgUnitMode(operationParams.getOrgUnitMode())
        .setEventStatus(operationParams.getEventStatus())
        .setEventStartDate(operationParams.getEventStartDate())
        .setEventEndDate(operationParams.getEventEndDate())
        .setAssignedUserQueryParam(operationParams.getAssignedUserQueryParam())
        .setUser(user)
        .setTrackedEntityUids(operationParams.getTrackedEntityUids())
        .setPage(operationParams.getPage())
        .setPageSize(operationParams.getPageSize())
        .setTotalPages(operationParams.isTotalPages())
        .setSkipPaging(operationParams.isSkipPaging())
        .setIncludeDeleted(operationParams.isIncludeDeleted())
        .setPotentialDuplicate(operationParams.getPotentialDuplicate());

    return params;
  }

  private void mapAttributeFilters(
      TrackedEntityQueryParams params, Map<String, List<QueryFilter>> attributeFilters)
      throws BadRequestException {
    for (Map.Entry<String, List<QueryFilter>> attributeFilter : attributeFilters.entrySet()) {
      TrackedEntityAttribute tea =
          attributeService.getTrackedEntityAttribute(attributeFilter.getKey());
      if (tea == null) {
        throw new BadRequestException(
            String.format(
                "attribute filters are invalid. Tracked entity attribute '%s' does not exist.",
                attributeFilter.getKey()));
      }

      if (attributeFilter.getValue().isEmpty()) {
        params.filterBy(tea);
      }

      for (QueryFilter filter : attributeFilter.getValue()) {
        params.filterBy(tea, filter);
      }
    }
  }

  private Set<OrganisationUnit> validateRequestedOrgUnit(Set<String> orgUnitIds)
      throws BadRequestException {
    Set<OrganisationUnit> orgUnits = new HashSet<>();
    for (String orgUnitUid : orgUnitIds) {
      OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(orgUnitUid);

      if (orgUnit == null) {
        throw new BadRequestException("Organisation unit does not exist: " + orgUnitUid);
      }

      orgUnits.add(orgUnit);
    }

    return orgUnits;
  }

  private Program validateProgram(String uid) throws BadRequestException {
    if (uid == null) {
      return null;
    }

    Program program = programService.getProgram(uid);
    if (program == null) {
      throw new BadRequestException("Program is specified but does not exist: " + uid);
    }

    return program;
  }

  private TrackedEntityType validateTrackedEntityType(String uid) throws BadRequestException {
    if (uid == null) {
      return null;
    }

    TrackedEntityType trackedEntityType = trackedEntityTypeService.getTrackedEntityType(uid);
    if (trackedEntityType == null) {
      throw new BadRequestException("Tracked entity type is specified but does not exist: " + uid);
    }

    return trackedEntityType;
  }

  private ProgramStage validateProgramStage(
      TrackedEntityOperationParams requestParams, Program program) throws BadRequestException {

    ProgramStage ps =
        requestParams.getProgramStageUid() != null
            ? getProgramStageFromProgram(program, requestParams.getProgramStageUid())
            : null;
    if (requestParams.getProgramStageUid() != null && ps == null) {
      throw new BadRequestException(
          "Program does not contain the specified programStage: "
              + requestParams.getProgramStageUid());
    }
    return ps;
  }

  private ProgramStage getProgramStageFromProgram(Program program, String programStage) {
    if (program == null) {
      return null;
    }

    return program.getProgramStages().stream()
        .filter(ps -> ps.getUid().equals(programStage))
        .findFirst()
        .orElse(null);
  }

  private void mapOrderParam(TrackedEntityQueryParams params, List<Order> orders)
      throws BadRequestException {
    if (orders == null || orders.isEmpty()) {
      return;
    }

    for (Order order : orders) {
      if (order.getField() instanceof String field) {
        params.orderBy(field, order.getDirection());
      } else if (order.getField() instanceof UID uid) {
        TrackedEntityAttribute tea = attributeService.getTrackedEntityAttribute(uid.getValue());
        if (tea == null) {
          throw new BadRequestException(
              "Cannot order by '"
                  + uid.getValue()
                  + "' as its not a tracked entity attribute. Tracked entities can be ordered by fields and tracked entity attributes.");
        }

        params.orderBy(tea, order.getDirection());
      } else {
        throw new IllegalArgumentException(
            "Cannot order by '"
                + order.getField()
                + "'. Tracked entities can be ordered by fields and tracked entity attributes.");
      }
    }
  }
}
