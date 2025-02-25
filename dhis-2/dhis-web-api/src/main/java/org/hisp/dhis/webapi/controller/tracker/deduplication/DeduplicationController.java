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
package org.hisp.dhis.webapi.controller.tracker.deduplication;

import static org.hisp.dhis.webapi.controller.tracker.RequestParamsValidator.validatePaginationParameters;
import static org.hisp.dhis.webapi.controller.tracker.export.FieldFilterRequestHandler.getRequestURL;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.deduplication.DeduplicationMergeParams;
import org.hisp.dhis.tracker.deduplication.DeduplicationService;
import org.hisp.dhis.tracker.deduplication.DeduplicationStatus;
import org.hisp.dhis.tracker.deduplication.MergeObject;
import org.hisp.dhis.tracker.deduplication.MergeStrategy;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicate;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicateConflictException;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicateCriteria;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicateForbiddenException;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;

@OpenApi.Document(
    entity = TrackedEntity.class,
    classifiers = {"team:tracker", "purpose:data"})
@RestController
@RequestMapping("/api/potentialDuplicates")
@ApiVersion(include = {DhisApiVersion.ALL, DhisApiVersion.DEFAULT})
@RequiredArgsConstructor
public class DeduplicationController {
  private final DeduplicationService deduplicationService;

  private final FieldFilterService fieldFilterService;

  private final TrackedEntityService trackedEntityService;

  private static final String DEFAULT_FIELDS_PARAM =
      "id,created,lastUpdated,original,duplicate,status";

  @OpenApi.Response(PotentialDuplicate[].class)
  @GetMapping
  ResponseEntity<Page<ObjectNode>> getPotentialDuplicates(
      PotentialDuplicateRequestParams requestParams,
      HttpServletResponse response,
      @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM) List<FieldPath> fields,
      HttpServletRequest request)
      throws BadRequestException {
    validatePaginationParameters(requestParams);

    PotentialDuplicateCriteria criteria = new PotentialDuplicateCriteria();
    criteria.setStatus(requestParams.getStatus());
    criteria.setTrackedEntities(requestParams.getTrackedEntities());
    criteria.setOrder(requestParams.getOrder());
    if (requestParams.isPaging()) {
      PageParams pageParams =
          new PageParams(requestParams.getPage(), requestParams.getPageSize(), false);
      org.hisp.dhis.tracker.Page<PotentialDuplicate> page =
          deduplicationService.getPotentialDuplicates(criteria, pageParams);
      List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(page.getItems(), fields);

      // TODO(ivo) set header on response entity
      setNoStore(response);

      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              Page.withFullPager(
                  "potentialDuplicates", page.withItems(objectNodes), getRequestURL(request)));
    }

    List<PotentialDuplicate> potentialDuplicates =
        deduplicationService.getPotentialDuplicates(criteria);
    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(potentialDuplicates, fields);

    // TODO(ivo) set header on response entity
    setNoStore(response);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(Page.withoutPager("potentialDuplicates", objectNodes));
  }

  @GetMapping(value = "/{uid}")
  public PotentialDuplicate getPotentialDuplicateById(@PathVariable UID uid)
      throws NotFoundException, HttpStatusCodeException {
    return deduplicationService.getPotentialDuplicate(uid);
  }

  @PostMapping
  @ResponseStatus(value = HttpStatus.OK)
  public PotentialDuplicate postPotentialDuplicate(
      @RequestBody PotentialDuplicate potentialDuplicate)
      throws ForbiddenException,
          ConflictException,
          NotFoundException,
          PotentialDuplicateConflictException,
          BadRequestException {
    validatePotentialDuplicate(potentialDuplicate);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    return potentialDuplicate;
  }

  @PutMapping(value = "/{uid}")
  @ResponseStatus(value = HttpStatus.OK)
  public void updatePotentialDuplicate(
      @PathVariable UID uid, @RequestParam(value = "status") DeduplicationStatus status)
      throws NotFoundException, BadRequestException {
    PotentialDuplicate potentialDuplicate = deduplicationService.getPotentialDuplicate(uid);

    checkDbAndRequestStatus(potentialDuplicate, status);

    potentialDuplicate.setStatus(status);
    deduplicationService.updatePotentialDuplicate(potentialDuplicate);
  }

  @PostMapping(value = "/{uid}/merge")
  @ResponseStatus(value = HttpStatus.OK)
  public void mergePotentialDuplicate(
      @PathVariable UID uid,
      @RequestParam(defaultValue = "AUTO") MergeStrategy mergeStrategy,
      @RequestBody(required = false) MergeObject mergeObject)
      throws NotFoundException,
          PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException,
          ForbiddenException,
          BadRequestException {
    PotentialDuplicate potentialDuplicate = deduplicationService.getPotentialDuplicate(uid);

    if (potentialDuplicate.getOriginal() == null || potentialDuplicate.getDuplicate() == null) {
      throw new PotentialDuplicateConflictException(
          "PotentialDuplicate is missing references and cannot be merged.");
    }

    TrackedEntity original =
        trackedEntityService.getTrackedEntity(potentialDuplicate.getOriginal());
    TrackedEntity duplicate =
        trackedEntityService.getTrackedEntity(potentialDuplicate.getDuplicate());

    if (mergeObject == null) {
      mergeObject = new MergeObject();
    }

    DeduplicationMergeParams params =
        DeduplicationMergeParams.builder()
            .potentialDuplicate(potentialDuplicate)
            .mergeObject(mergeObject)
            .original(original)
            .duplicate(duplicate)
            .build();

    if (MergeStrategy.MANUAL.equals(mergeStrategy)) {
      deduplicationService.manualMerge(params);
    } else {
      deduplicationService.autoMerge(params);
    }
  }

  private void checkDbAndRequestStatus(
      PotentialDuplicate potentialDuplicate, DeduplicationStatus deduplicationStatus)
      throws BadRequestException {
    if (deduplicationStatus == DeduplicationStatus.MERGED)
      throw new BadRequestException(
          "Can't update a potential duplicate to " + DeduplicationStatus.MERGED.name());
    if (potentialDuplicate.getStatus() == DeduplicationStatus.MERGED)
      throw new BadRequestException(
          "Can't update a potential duplicate that is already "
              + DeduplicationStatus.MERGED.name());
  }

  private void validatePotentialDuplicate(PotentialDuplicate potentialDuplicate)
      throws ForbiddenException,
          ConflictException,
          NotFoundException,
          PotentialDuplicateConflictException,
          BadRequestException {
    checkValidTrackedEntity(potentialDuplicate.getOriginal(), "original");
    checkValidTrackedEntity(potentialDuplicate.getDuplicate(), "duplicate");
    trackedEntityService.getTrackedEntity(potentialDuplicate.getOriginal());
    trackedEntityService.getTrackedEntity(potentialDuplicate.getDuplicate());
    checkAlreadyExistingDuplicate(potentialDuplicate);
  }

  private void checkAlreadyExistingDuplicate(PotentialDuplicate potentialDuplicate)
      throws ConflictException, PotentialDuplicateConflictException {
    if (deduplicationService.exists(potentialDuplicate)) {
      throw new ConflictException(
          "'"
              + potentialDuplicate.getOriginal()
              + "' "
              + "and '"
              + potentialDuplicate.getDuplicate()
              + " is already marked as a potential duplicate");
    }
  }

  private void checkValidTrackedEntity(UID trackedEntity, String trackedEntityFieldName)
      throws BadRequestException {
    if (trackedEntity == null) {
      throw new BadRequestException(
          "Missing required input property '" + trackedEntityFieldName + "'");
    }
  }
}
