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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.hisp.dhis.common.OpenApi.Response.Status;
import static org.hisp.dhis.webapi.controller.tracker.ControllerSupport.assertUserOrderableFieldsAreSupported;
import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validatePaginationParameters;
import static org.hisp.dhis.webapi.controller.tracker.export.relationship.RelationshipRequestParams.DEFAULT_FIELDS_PARAM;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.relationship.RelationshipFields;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.webapi.controller.tracker.RequestHandler;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.EntityType(org.hisp.dhis.relationship.Relationship.class)
@OpenApi.Document(
    entity = org.hisp.dhis.relationship.Relationship.class,
    classifiers = {"team:tracker", "purpose:data"})
@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE, value = "/api/tracker/relationships")
class RelationshipsExportController {

  protected static final String RELATIONSHIPS = "relationships";

  private static final RelationshipMapper RELATIONSHIP_MAPPER =
      Mappers.getMapper(RelationshipMapper.class);

  private final RelationshipService relationshipService;

  private final RelationshipRequestParamsMapper mapper;

  private final RequestHandler requestHandler;

  private final FieldFilterService fieldFilterService;

  public RelationshipsExportController(
      RelationshipService relationshipService,
      RelationshipRequestParamsMapper mapper,
      RequestHandler requestHandler,
      FieldFilterService fieldFilterService) {
    this.relationshipService = relationshipService;
    this.requestHandler = requestHandler;
    this.mapper = mapper;
    this.fieldFilterService = fieldFilterService;

    assertUserOrderableFieldsAreSupported(
        "relationship",
        RelationshipMapper.ORDERABLE_FIELDS,
        relationshipService.getOrderableFields());
  }

  @OpenApi.Response(status = Status.OK, value = Page.class)
  @GetMapping(
      produces = APPLICATION_JSON_VALUE,
      headers = "Accept=text/html"
      // use the text/html Accept header to default to a Json response when a generic request comes
      // from a browser
      )
  ResponseEntity<Page<ObjectNode>> getRelationships(
      RelationshipRequestParams requestParams, HttpServletRequest request)
      throws NotFoundException, BadRequestException, ForbiddenException {
    validatePaginationParameters(requestParams);
    RelationshipOperationParams operationParams = mapper.map(requestParams);

    if (requestParams.isPaging()) {
      PageParams pageParams =
          PageParams.of(
              requestParams.getPage(), requestParams.getPageSize(), requestParams.isTotalPages());
      org.hisp.dhis.tracker.Page<org.hisp.dhis.relationship.Relationship> relationshipsPage =
          relationshipService.findRelationships(operationParams, pageParams);

      org.hisp.dhis.tracker.Page<Relationship> page =
          relationshipsPage.withMappedItems(RELATIONSHIP_MAPPER::map);

      return requestHandler.serve(request, RELATIONSHIPS, page, requestParams);
    }

    List<Relationship> relationships =
        relationshipService.findRelationships(operationParams).stream()
            .map(RELATIONSHIP_MAPPER::map)
            .toList();

    return requestHandler.serve(RELATIONSHIPS, relationships, requestParams);
  }

  @GetMapping("/{uid}")
  @OpenApi.Response(Relationship.class)
  ResponseEntity<ObjectNode> getRelationshipByUid(
      @OpenApi.Param({UID.class, org.hisp.dhis.relationship.Relationship.class}) @PathVariable
          UID uid,
      @OpenApi.Param(value = String[].class) @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM)
          List<FieldPath> fields)
      throws NotFoundException, ForbiddenException {
    RelationshipFields relationshipFields =
        RelationshipFields.of(
            f -> fieldFilterService.filterIncludes(Relationship.class, fields, f),
            FieldPath.FIELD_PATH_SEPARATOR);

    Relationship relationship =
        RELATIONSHIP_MAPPER.map(relationshipService.getRelationship(uid, relationshipFields));

    return requestHandler.serve(relationship, fields);
  }
}
