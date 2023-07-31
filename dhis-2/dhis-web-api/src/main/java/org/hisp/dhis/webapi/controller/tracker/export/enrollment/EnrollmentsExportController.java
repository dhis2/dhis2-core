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
package org.hisp.dhis.webapi.controller.tracker.export.enrollment;

import static org.hisp.dhis.webapi.controller.tracker.ControllerSupport.RESOURCE_PATH;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.validateDeprecatedUidsParameter;
import static org.hisp.dhis.webapi.controller.tracker.export.enrollment.RequestParams.DEFAULT_FIELDS_PARAM;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Response.Status;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.enrollment.Enrollments;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.controller.tracker.export.OpenApiExport;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.EntityType(Enrollment.class)
@OpenApi.Tags("tracker")
@RestController
@RequestMapping(value = RESOURCE_PATH + "/" + EnrollmentsExportController.ENROLLMENTS)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
class EnrollmentsExportController {
  protected static final String ENROLLMENTS = "enrollments";

  private static final EnrollmentMapper ENROLLMENT_MAPPER =
      Mappers.getMapper(EnrollmentMapper.class);

  private final EnrollmentRequestParamsMapper operationParamsMapper;

  private final EnrollmentService enrollmentService;

  private final FieldFilterService fieldFilterService;

  private final EnrollmentFieldsParamMapper fieldsMapper;

  @OpenApi.Response(status = Status.OK, value = OpenApiExport.ListResponse.class)
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  PagingWrapper<ObjectNode> getEnrollments(RequestParams requestParams)
      throws BadRequestException, ForbiddenException, NotFoundException {
    PagingWrapper<ObjectNode> pagingWrapper = new PagingWrapper<>();

    List<org.hisp.dhis.program.Enrollment> enrollmentList;

    EnrollmentOperationParams operationParams = operationParamsMapper.map(requestParams);

    Set<UID> enrollmentUids =
        validateDeprecatedUidsParameter(
            "enrollment",
            requestParams.getEnrollment(),
            "enrollments",
            requestParams.getEnrollments());
    if (enrollmentUids.isEmpty()) {
      Enrollments enrollments = enrollmentService.getEnrollments(operationParams);

      if (requestParams.isPagingRequest()) {
        pagingWrapper =
            pagingWrapper.withPager(
                PagingWrapper.Pager.fromLegacy(requestParams, enrollments.getPager()));
      }

      enrollmentList = enrollments.getEnrollments();
    } else {
      List<org.hisp.dhis.program.Enrollment> list = new ArrayList<>();
      for (UID uid : enrollmentUids) {
        list.add(
            enrollmentService.getEnrollment(
                uid.getValue(),
                operationParams.getEnrollmentParams(),
                operationParams.isIncludeDeleted()));
      }
      enrollmentList = list;
    }

    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(
            ENROLLMENT_MAPPER.fromCollection(enrollmentList), requestParams.getFields());
    return pagingWrapper.withInstances(objectNodes);
  }

  @OpenApi.Response(OpenApi.EntityType.class)
  @GetMapping(value = "/{uid}")
  public ResponseEntity<ObjectNode> getEnrollmentByUid(
      @OpenApi.Param({UID.class, Enrollment.class}) @PathVariable UID uid,
      @OpenApi.Param(value = String[].class) @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM)
          List<FieldPath> fields)
      throws NotFoundException, ForbiddenException {
    EnrollmentParams enrollmentParams = fieldsMapper.map(fields);
    Enrollment enrollment =
        ENROLLMENT_MAPPER.from(
            enrollmentService.getEnrollment(uid.getValue(), enrollmentParams, false));
    return ResponseEntity.ok(fieldFilterService.toObjectNode(enrollment, fields));
  }
}
