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
package org.hisp.dhis.webapi.controller.deduplication;

import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.deduplication.DeduplicationMergeParams;
import org.hisp.dhis.deduplication.DeduplicationService;
import org.hisp.dhis.deduplication.DeduplicationStatus;
import org.hisp.dhis.deduplication.MergeObject;
import org.hisp.dhis.deduplication.MergeStrategy;
import org.hisp.dhis.deduplication.PotentialDuplicate;
import org.hisp.dhis.deduplication.PotentialDuplicateConflictException;
import org.hisp.dhis.deduplication.PotentialDuplicateCriteria;
import org.hisp.dhis.deduplication.PotentialDuplicateForbiddenException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
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

@OpenApi.Tags("tracker")
@RestController
@RequestMapping(value = "/potentialDuplicates")
@ApiVersion(include = {DhisApiVersion.ALL, DhisApiVersion.DEFAULT})
@RequiredArgsConstructor
public class DeduplicationController {
  private final DeduplicationService deduplicationService;

  private final TrackedEntityInstanceService trackedEntityInstanceService;

  private final TrackerAccessManager trackerAccessManager;

  private final CurrentUserService currentUserService;

  private final FieldFilterService fieldFilterService;

  private static final String DEFAULT_FIELDS_PARAM =
      "created, lastUpdated, original, duplicate, status";

  @OpenApi.Response(PotentialDuplicate[].class)
  @GetMapping
  public PagingWrapper<ObjectNode> getPotentialDuplicates(
      PotentialDuplicateCriteria potentialDuplicateCriteria,
      HttpServletResponse response,
      @RequestParam(defaultValue = DEFAULT_FIELDS_PARAM) List<FieldPath> fields) {
    PagingWrapper<ObjectNode> pagingWrapper = new PagingWrapper<>("potentialDuplicates");

    if (potentialDuplicateCriteria.isPagingRequest()) {
      pagingWrapper =
          pagingWrapper.withPager(
              PagingWrapper.Pager.builder()
                  .page(potentialDuplicateCriteria.getPage())
                  .pageSize(potentialDuplicateCriteria.getPageSize())
                  .build());
    }

    List<PotentialDuplicate> potentialDuplicates =
        deduplicationService.getPotentialDuplicates(potentialDuplicateCriteria);

    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(potentialDuplicates, fields);

    setNoStore(response);

    return pagingWrapper.withInstances(objectNodes);
  }

  @GetMapping(value = "/{id}")
  public PotentialDuplicate getPotentialDuplicateById(@PathVariable String id)
      throws NotFoundException, HttpStatusCodeException {
    return getPotentialDuplicateBy(id);
  }

  @PostMapping
  @ResponseStatus(value = HttpStatus.OK)
  public PotentialDuplicate postPotentialDuplicate(
      @RequestBody PotentialDuplicate potentialDuplicate)
      throws ForbiddenException,
          ConflictException,
          NotFoundException,
          BadRequestException,
          PotentialDuplicateConflictException {
    validatePotentialDuplicate(potentialDuplicate);
    deduplicationService.addPotentialDuplicate(potentialDuplicate);
    return potentialDuplicate;
  }

  @PutMapping(value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void updatePotentialDuplicate(
      @PathVariable String id, @RequestParam(value = "status") DeduplicationStatus status)
      throws NotFoundException, BadRequestException {
    PotentialDuplicate potentialDuplicate = getPotentialDuplicateBy(id);

    checkDbAndRequestStatus(potentialDuplicate, status);

    potentialDuplicate.setStatus(status);
    deduplicationService.updatePotentialDuplicate(potentialDuplicate);
  }

  @PostMapping(value = "/{id}/merge")
  @ResponseStatus(value = HttpStatus.OK)
  public void mergePotentialDuplicate(
      @PathVariable String id,
      @RequestParam(defaultValue = "AUTO") MergeStrategy mergeStrategy,
      @RequestBody(required = false) MergeObject mergeObject)
      throws NotFoundException,
          PotentialDuplicateConflictException,
          PotentialDuplicateForbiddenException {
    PotentialDuplicate potentialDuplicate = getPotentialDuplicateBy(id);

    if (potentialDuplicate.getOriginal() == null || potentialDuplicate.getDuplicate() == null) {
      throw new PotentialDuplicateConflictException(
          "PotentialDuplicate is missing references and cannot be merged.");
    }

    TrackedEntityInstance original = getTei(potentialDuplicate.getOriginal());
    TrackedEntityInstance duplicate = getTei(potentialDuplicate.getDuplicate());

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

  private PotentialDuplicate getPotentialDuplicateBy(String id) throws NotFoundException {
    return Optional.ofNullable(deduplicationService.getPotentialDuplicateByUid(id))
        .orElseThrow(
            () ->
                new NotFoundException("No potentialDuplicate records found with id '" + id + "'."));
  }

  private void validatePotentialDuplicate(PotentialDuplicate potentialDuplicate)
      throws ForbiddenException,
          ConflictException,
          NotFoundException,
          BadRequestException,
          PotentialDuplicateConflictException {
    checkValidTei(potentialDuplicate.getOriginal(), "original");

    checkValidTei(potentialDuplicate.getDuplicate(), "duplicate");

    canReadTei(getTei(potentialDuplicate.getOriginal()));

    canReadTei(getTei(potentialDuplicate.getDuplicate()));

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

  private void checkValidTei(String tei, String teiFieldName) throws BadRequestException {
    if (tei == null) {
      throw new BadRequestException("Missing required input property '" + teiFieldName + "'");
    }

    if (!CodeGenerator.isValidUid(tei)) {
      throw new BadRequestException(
          "'" + tei + "' is not valid value for property '" + teiFieldName + "'");
    }
  }

  private TrackedEntityInstance getTei(String tei) throws NotFoundException {
    return Optional.ofNullable(trackedEntityInstanceService.getTrackedEntityInstance(tei))
        .orElseThrow(
            () -> new NotFoundException("No tracked entity instance found with id '" + tei + "'."));
  }

  private void canReadTei(TrackedEntityInstance trackedEntityInstance) throws ForbiddenException {
    if (!trackerAccessManager
        .canRead(currentUserService.getCurrentUser(), trackedEntityInstance)
        .isEmpty()) {
      throw new ForbiddenException(
          "You don't have read access to '" + trackedEntityInstance.getUid() + "'.");
    }
  }
}
