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

import static org.hisp.dhis.webapi.controller.tracker.ControllerSupport.assertUserOrderableFieldsAreSupported;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validatePaginationParameters;
import static org.hisp.dhis.webapi.controller.tracker.export.enrollment.EnrollmentRequestParams.DEFAULT_FIELDS_PARAM;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.List;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Response.Status;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.EntityType(Enrollment.class)
@OpenApi.Document(
    entity = org.hisp.dhis.program.Enrollment.class,
    classifiers = {"team:tracker", "purpose:data"})
@RestController
@RequestMapping("/api/tracker/enrollments")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
class EnrollmentsExportController {
  protected static final String ENROLLMENTS = "enrollments";

  private static final EnrollmentMapper ENROLLMENT_MAPPER =
      Mappers.getMapper(EnrollmentMapper.class);

  private final EnrollmentService enrollmentService;

  private final EnrollmentRequestParamsMapper paramsMapper;

  private final FieldFilterService fieldFilterService;

  private final EnrollmentFieldsParamMapper fieldsMapper;

  public EnrollmentsExportController(
      EnrollmentService enrollmentService,
      EnrollmentRequestParamsMapper paramsMapper,
      FieldFilterService fieldFilterService,
      EnrollmentFieldsParamMapper fieldsMapper) {
    this.enrollmentService = enrollmentService;
    this.paramsMapper = paramsMapper;
    this.fieldFilterService = fieldFilterService;
    this.fieldsMapper = fieldsMapper;

    assertUserOrderableFieldsAreSupported(
        "enrollment", EnrollmentMapper.ORDERABLE_FIELDS, enrollmentService.getOrderableFields());
  }

  @OpenApi.Response(status = Status.OK, value = Page.class)
  @GetMapping(
      produces = APPLICATION_JSON_VALUE,
      headers = "Accept=text/html"
      // use the text/html Accept header to default to a Json response when a generic request comes
      // from a browser
      )
  ResponseEntity<Page<ObjectNode>> getEnrollments(EnrollmentRequestParams requestParams)
      throws BadRequestException, ForbiddenException, NotFoundException {
    validatePaginationParameters(requestParams);
    EnrollmentOperationParams operationParams = paramsMapper.map(requestParams);

    if (requestParams.isPaged()) {
      PageParams pageParams =
          new PageParams(
              requestParams.getPage(), requestParams.getPageSize(), requestParams.getTotalPages());

      org.hisp.dhis.tracker.export.Page<org.hisp.dhis.program.Enrollment> enrollmentsPage =
          enrollmentService.getEnrollments(operationParams, pageParams);
      List<ObjectNode> objectNodes =
          fieldFilterService.toObjectNodes(
              ENROLLMENT_MAPPER.fromCollection(enrollmentsPage.getItems()),
              requestParams.getFields());

      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(Page.withPager(ENROLLMENTS, enrollmentsPage.withItems(objectNodes)));
    }

    Collection<org.hisp.dhis.program.Enrollment> enrollments =
        enrollmentService.getEnrollments(operationParams);
    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(
            ENROLLMENT_MAPPER.fromCollection(enrollments), requestParams.getFields());

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(Page.withoutPager(ENROLLMENTS, objectNodes));
  }

  @OpenApi.Response(OpenApi.EntityType.class)
  @GetMapping(value = "/{uid}")
  public ResponseEntity<ObjectNode> getEnrollmentByUid(
      @OpenApi.Param({UID.class, org.hisp.dhis.program.Enrollment.class}) @PathVariable UID uid,
      @OpenApi.Param(value = String[].class) @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM)
          List<FieldPath> fields)
      throws NotFoundException, ForbiddenException {
    EnrollmentParams enrollmentParams = fieldsMapper.map(fields);
    Enrollment enrollment =
        ENROLLMENT_MAPPER.from(enrollmentService.getEnrollment(uid, enrollmentParams, false));
    return ResponseEntity.ok(fieldFilterService.toObjectNode(enrollment, fields));
  }
}
