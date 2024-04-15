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
package org.hisp.dhis.webapi.controller.tracker.export;

import static java.util.Map.entry;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.webapi.controller.tracker.TrackerControllerSupport.RESOURCE_PATH;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamsValidator.validateOrderParams;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenApi.Tags("tracker")
@RestController
@RequestMapping(
    produces = APPLICATION_JSON_VALUE,
    value = RESOURCE_PATH + "/" + TrackerRelationshipsExportController.RELATIONSHIPS)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
public class TrackerRelationshipsExportController {
  protected static final String RELATIONSHIPS = "relationships";

  /**
   * Relationships can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.relationship.Relationship}.
   */
  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(entry("createdAt", "created"));

  private static final Set<String> ORDERABLE_FIELD_NAMES = ORDERABLE_FIELDS.keySet();

  private static final String DEFAULT_FIELDS_PARAM =
      "relationship,relationshipType,from[trackedEntity[trackedEntity],enrollment[enrollment],event[event]],to[trackedEntity[trackedEntity],enrollment[enrollment],event[event]]";

  private static final org.hisp.dhis.webapi.controller.tracker.export.RelationshipMapper
      RELATIONSHIP_MAPPER =
          Mappers.getMapper(
              org.hisp.dhis.webapi.controller.tracker.export.RelationshipMapper.class);

  @Nonnull private final TrackedEntityInstanceService trackedEntityInstanceService;

  @Nonnull private final ProgramInstanceService programInstanceService;

  @Nonnull private final ProgramStageInstanceService programStageInstanceService;

  @Nonnull private final RelationshipService relationshipService;

  @Nonnull private final FieldFilterService fieldFilterService;

  private Map<Class<?>, Function<String, ?>> objectRetrievers;

  @PostConstruct
  void setupMaps() {
    objectRetrievers =
        ImmutableMap.<Class<?>, Function<String, ?>>builder()
            .put(
                TrackedEntityInstance.class, trackedEntityInstanceService::getTrackedEntityInstance)
            .put(ProgramInstance.class, programInstanceService::getProgramInstance)
            .put(ProgramStageInstance.class, programStageInstanceService::getProgramStageInstance)
            .build();
  }

  @GetMapping(
      produces = APPLICATION_JSON_VALUE,
      headers = "Accept=text/html"
      // use the text/html Accept header to default to a Json response when a generic request comes
      // from a browser
      )
  ResponseEntity<PagingWrapper<ObjectNode>> getInstances(
      TrackerRelationshipCriteria criteria,
      @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM) List<FieldPath> fields)
      throws WebMessageException {

    String identifier = criteria.getIdentifierParam();
    String identifierName = criteria.getIdentifierName();
    List<org.hisp.dhis.webapi.controller.tracker.view.Relationship> relationships =
        tryGetRelationshipFrom(
            identifier,
            criteria.getIdentifierClass(),
            () -> notFound("No " + identifierName + " '" + identifier + "' found."),
            criteria,
            criteria.isIncludeDeleted());

    if (criteria.isPagingRequest()) {
      List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(relationships, fields);
      if (criteria.isTotalPages()) {
        long count =
            tryGetRelationshipFrom(
                    identifier,
                    criteria.getIdentifierClass(),
                    () -> notFound("No " + identifierName + " '" + identifier + "' found."),
                    null,
                    criteria.isIncludeDeleted())
                .size();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                PagingWrapper.withPager(
                    objectNodes,
                    criteria.getPageWithDefault(),
                    criteria.getPageSizeWithDefault(),
                    count));
      }

      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              PagingWrapper.withPager(
                  objectNodes, criteria.getPageWithDefault(), criteria.getPageSizeWithDefault()));
    }

    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(relationships, fields);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(PagingWrapper.withoutPager(objectNodes));
  }

  @GetMapping("{id}")
  public ResponseEntity<ObjectNode> getRelationship(
      @PathVariable String id,
      @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM) List<FieldPath> fields)
      throws NotFoundException {

    org.hisp.dhis.webapi.controller.tracker.view.Relationship relationship =
        RELATIONSHIP_MAPPER.from(relationshipService.findRelationshipByUid(id).orElse(null));
    if (relationship == null) {
      throw new NotFoundException(Relationship.class, id);
    }
    return ResponseEntity.ok(fieldFilterService.toObjectNode(relationship, fields));
  }

  @SneakyThrows
  private List<org.hisp.dhis.webapi.controller.tracker.view.Relationship> tryGetRelationshipFrom(
      String identifier,
      Class<?> type,
      Supplier<WebMessage> notFoundMessageSupplier,
      PagingAndSortingCriteriaAdapter pagingAndSortingCriteria,
      boolean includeDeleted) {

    if (pagingAndSortingCriteria != null) {
      // validate and translate from user facing field names to internal field names users can order
      // by avoiding field translation by PagingAndSortingCriteriaAdapter to validate user order
      // fields and to not affect old tracker.
      validateOrderParams(pagingAndSortingCriteria.getRawOrder(), ORDERABLE_FIELD_NAMES);
      List<OrderCriteria> orders = mapOrderParam(pagingAndSortingCriteria.getRawOrder());
      pagingAndSortingCriteria.setOrder(orders);
    }

    if (identifier != null) {
      Object object = getObjectRetriever(type).apply(identifier);
      if (object != null) {
        List<Relationship> relationships;
        if (object instanceof TrackedEntityInstance) {
          relationships =
              relationshipService.getRelationshipsByTrackedEntityInstance(
                  (TrackedEntityInstance) object, pagingAndSortingCriteria, false, includeDeleted);

        } else if (object instanceof ProgramInstance) {
          relationships =
              relationshipService.getRelationshipsByProgramInstance(
                  (ProgramInstance) object, pagingAndSortingCriteria, false, includeDeleted);
        } else {

          relationships =
              relationshipService.getRelationshipsByProgramStageInstance(
                  (ProgramStageInstance) object, pagingAndSortingCriteria, false, includeDeleted);
        }

        return RELATIONSHIP_MAPPER.fromCollection(relationships);
      } else {
        throw new WebMessageException(notFoundMessageSupplier.get());
      }
    }
    return Collections.emptyList();
  }

  private Function<String, ?> getObjectRetriever(Class<?> type) {
    return Optional.ofNullable(type)
        .map(objectRetrievers::get)
        .orElseThrow(
            () -> new IllegalArgumentException("Unable to detect object retriever from " + type));
  }

  private static List<OrderCriteria> mapOrderParam(List<OrderCriteria> orders) {
    if (orders == null || orders.isEmpty()) {
      return List.of();
    }

    List<OrderCriteria> result = new ArrayList<>();
    for (OrderCriteria order : orders) {
      if (ORDERABLE_FIELDS.containsKey(order.getField())) {
        result.add(OrderCriteria.of(ORDERABLE_FIELDS.get(order.getField()), order.getDirection()));
      }
    }
    return result;
  }
}
