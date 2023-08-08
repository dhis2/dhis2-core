/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.programstagefilter.DateFilterPeriod;
import org.hisp.dhis.programstagefilter.DatePeriodType;
import org.hisp.dhis.programstagefilter.EventDataFilter;
import org.hisp.dhis.programstageworkinglist.ProgramStageQueryCriteria;
import org.hisp.dhis.programstageworkinglist.ProgramStageWorkingList;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentityfilter.AttributeValueFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component("org.hisp.dhis.dxf2.metadata.objectbundle.hooks.ProgramStageWorkingListObjectBundleHook")
@RequiredArgsConstructor
public class ProgramStageWorkingListObjectBundleHook
    extends AbstractObjectBundleHook<ProgramStageWorkingList> {

  private final DataElementService dataElementService;

  private final OrganisationUnitService organisationUnitService;

  private final TrackedEntityAttributeService teaService;

  @Override
  public void validate(
      ProgramStageWorkingList workingList, ObjectBundle bundle, Consumer<ErrorReport> addReports) {

    ProgramStageQueryCriteria queryCriteria = workingList.getProgramStageQueryCriteria();

    List<ErrorReport> errorReports = new ArrayList<>();

    validateDateFilterPeriod(errorReports, "EnrollmentCreatedDate", queryCriteria.getEnrolledAt());
    validateDateFilterPeriod(
        errorReports, "EnrollmentIncidentDate", queryCriteria.getEnrollmentOccurredAt());
    validateDateFilterPeriod(errorReports, "EventCreatedDate", queryCriteria.getEventCreatedAt());
    validateDateFilterPeriod(errorReports, "EventOccurredDate", queryCriteria.getEventOccurredAt());
    validateDateFilterPeriod(
        errorReports, "EventScheduledDate", queryCriteria.getEventScheduledAt());

    validateAssignedUsers(
        errorReports, queryCriteria.getAssignedUsers(), queryCriteria.getAssignedUserMode());
    validateOrganisationUnit(errorReports, queryCriteria.getOrgUnit(), queryCriteria.getOuMode());
    validateAttributeValueFilters(
        errorReports,
        queryCriteria.getAttributeValueFilters(),
        teaService::getTrackedEntityAttribute);
    validateDataFilters(errorReports, queryCriteria.getDataFilters());

    errorReports.forEach(addReports);
  }

  private void validateDateFilterPeriod(
      List<ErrorReport> errorReports, String item, DateFilterPeriod dateFilterPeriod) {
    if (dateFilterPeriod != null
        && dateFilterPeriod.getType() != null
        && dateFilterPeriod.getType() == DatePeriodType.ABSOLUTE
        && dateFilterPeriod.getStartDate() == null
        && dateFilterPeriod.getEndDate() == null) {
      errorReports.add(new ErrorReport(ProgramStageWorkingList.class, ErrorCode.E4062, item));
    }
  }

  private void validateAssignedUsers(
      List<ErrorReport> errorReports,
      Set<String> assignedUsers,
      AssignedUserSelectionMode userMode) {
    if (CollectionUtils.isEmpty(assignedUsers) && userMode == AssignedUserSelectionMode.PROVIDED) {
      errorReports.add(new ErrorReport(ProgramStageWorkingList.class, ErrorCode.E4063));
    }
  }

  private void validateOrganisationUnit(
      List<ErrorReport> errorReports, String orgUnit, OrganisationUnitSelectionMode ouMode) {
    if (orgUnit != null) {
      OrganisationUnit ou = organisationUnitService.getOrganisationUnit(orgUnit);
      if (ou == null) {
        errorReports.add(new ErrorReport(ProgramStageWorkingList.class, ErrorCode.E7500, orgUnit));
        return;
      }
    }

    if (StringUtils.isEmpty(orgUnit)
        && (ouMode == OrganisationUnitSelectionMode.SELECTED
            || ouMode == OrganisationUnitSelectionMode.DESCENDANTS
            || ouMode == OrganisationUnitSelectionMode.CHILDREN)) {
      errorReports.add(new ErrorReport(ProgramStageWorkingList.class, ErrorCode.E4064, ouMode));
    }
  }

  private void validateDataFilters(
      List<ErrorReport> errorReports, List<EventDataFilter> dataFilters) {
    if (CollectionUtils.isEmpty(dataFilters)) {
      return;
    }

    dataFilters.forEach(
        f -> {
          if (StringUtils.isEmpty(f.getDataItem())) {
            errorReports.add(new ErrorReport(ProgramStageWorkingList.class, ErrorCode.E4065));
          } else {
            DataElement dataElement = dataElementService.getDataElement(f.getDataItem());
            if (dataElement == null) {
              errorReports.add(
                  new ErrorReport(ProgramStageWorkingList.class, ErrorCode.E4066, f.getDataItem()));
            }
          }

          validateDateFilterPeriod(errorReports, f.getDataItem(), f.getDateFilter());
        });
  }

  private void validateAttributeValueFilters(
      List<ErrorReport> errorReports,
      List<AttributeValueFilter> attributes,
      Function<String, TrackedEntityAttribute> attributeFetcher) {
    if (!CollectionUtils.isEmpty(attributes)) {
      attributes.forEach(
          avf -> {
            if (StringUtils.isEmpty(avf.getAttribute())) {
              errorReports.add(new ErrorReport(ProgramStageWorkingList.class, ErrorCode.E4067));
            } else {
              TrackedEntityAttribute tea = attributeFetcher.apply(avf.getAttribute());
              if (tea == null) {
                errorReports.add(
                    new ErrorReport(
                        ProgramStageWorkingList.class, ErrorCode.E4068, avf.getAttribute()));
              }
            }

            validateDateFilterPeriod(errorReports, avf.getAttribute(), avf.getDateFilter());
          });
    }
  }
}
