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
package org.hisp.dhis.webapi.controller.tracker.export.relationship;

import static org.hisp.dhis.common.OpenApi.Response.Status;
import static org.hisp.dhis.webapi.controller.tracker.ControllerSupport.RESOURCE_PATH;
import static org.hisp.dhis.webapi.controller.tracker.export.relationship.RequestParams.DEFAULT_FIELDS_PARAM;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.export.relationship.Relationships;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.controller.tracker.export.OpenApiExport;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.mapstruct.factory.Mappers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.EntityType(Relationship.class)
@OpenApi.Tags("tracker")
@RestController
@RequestMapping(
    produces = APPLICATION_JSON_VALUE,
    value = RESOURCE_PATH + "/" + RelationshipsExportController.RELATIONSHIPS)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
class RelationshipsExportController {

  protected static final String RELATIONSHIPS = "relationships";

  private static final RelationshipMapper RELATIONSHIP_MAPPER =
      Mappers.getMapper(RelationshipMapper.class);

  private final RelationshipService relationshipService;

  private final RelationshipRequestParamsMapper mapper;

  private final FieldFilterService fieldFilterService;

  @OpenApi.Response(status = Status.OK, value = OpenApiExport.ListResponse.class)
  @GetMapping
  PagingWrapper<ObjectNode> getRelationships(RequestParams requestParams)
      throws NotFoundException, BadRequestException, ForbiddenException {

    RelationshipOperationParams operationParams = mapper.map(requestParams);
    Relationships relationships = relationshipService.getRelationships(operationParams);

    PagingWrapper<ObjectNode> pagingWrapper = new PagingWrapper<>();

    if (requestParams.isPagingRequest()) {
      pagingWrapper =
          pagingWrapper.withPager(
              PagingWrapper.Pager.fromLegacy(requestParams, relationships.getPager()));
    }

    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(
            RELATIONSHIP_MAPPER.fromCollection(relationships.getRelationships()),
            requestParams.getFields());
    return pagingWrapper.withInstances(objectNodes);
  }

  @GetMapping("/{uid}")
  ResponseEntity<ObjectNode> getRelationshipByUid(
      @OpenApi.Param({UID.class, Relationship.class}) @PathVariable UID uid,
      @OpenApi.Param(value = String[].class) @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM)
          List<FieldPath> fields)
      throws NotFoundException, ForbiddenException {
    Relationship relationship =
        RELATIONSHIP_MAPPER.from(relationshipService.getRelationship(uid.getValue()));

    return ResponseEntity.ok(fieldFilterService.toObjectNode(relationship, fields));
  }
}
