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
package org.hisp.dhis.maintenance;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.hisp.dhis.dataapproval.DataApprovalAuditService;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.tracker.export.event.EventChangeLogService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.maintenance.MaintenanceService")
public class DefaultMaintenanceService implements MaintenanceService {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final MaintenanceStore maintenanceStore;

  private final UserService userService;

  private final TransactionTemplate transactionTemplate;

  private final DataValueService dataValueService;

  private final DataValueAuditService dataValueAuditService;

  private final CompleteDataSetRegistrationService completeRegistrationService;

  private final DataApprovalService dataApprovalService;

  private final DataApprovalAuditService dataApprovalAuditService;

  private final ApplicationEventPublisher eventPublisher;

  private final EventChangeLogService eventChangeLogService;

  // -------------------------------------------------------------------------
  // MaintenanceService implementation
  // -------------------------------------------------------------------------

  @Override
  public int deleteZeroDataValues() {
    int result = maintenanceStore.deleteZeroDataValues();

    log.info("Deleted zero data values: " + result);

    return result;
  }

  @Override
  public int deleteSoftDeletedDataValues() {
    int result = maintenanceStore.deleteSoftDeletedDataValues();

    log.info("Permanently deleted soft deleted data values: " + result);

    return result;
  }

  @Override
  public int deleteSoftDeletedEvents() {
    int result = maintenanceStore.deleteSoftDeletedEvents();

    log.info("Permanently deleted soft deleted events: " + result);

    return result;
  }

  @Override
  public int deleteSoftDeletedRelationships() {
    int result = maintenanceStore.deleteSoftDeletedRelationships();

    log.info("Permanently deleted soft deleted relationships: " + result);

    return result;
  }

  @Override
  public int deleteSoftDeletedEnrollments() {
    int result = maintenanceStore.deleteSoftDeletedEnrollments();

    log.info("Permanently deleted soft deleted enrollments: " + result);

    return result;
  }

  @Override
  public int deleteSoftDeletedTrackedEntities() {
    int result = maintenanceStore.deleteSoftDeletedTrackedEntities();

    log.info("Permanently deleted soft deleted tracked entities: {}", result);

    return result;
  }

  @Override
  @Transactional
  public void prunePeriods() {
    maintenanceStore.prunePeriods();
  }

  @Override
  @Transactional
  public boolean pruneData(OrganisationUnit organisationUnit) {
    if (!CurrentUserUtil.getCurrentUserDetails().isSuper()) {
      return false;
    }

    dataApprovalService.deleteDataApprovals(organisationUnit);
    dataApprovalAuditService.deleteDataApprovalAudits(organisationUnit);
    completeRegistrationService.deleteCompleteDataSetRegistrations(organisationUnit);
    dataValueAuditService.deleteDataValueAudits(organisationUnit);
    dataValueService.deleteDataValues(organisationUnit);

    log.info("Pruned data for organisation unit: " + organisationUnit);

    return true;
  }

  @Override
  @Transactional
  public boolean pruneData(DataElement dataElement) {
    if (!CurrentUserUtil.getCurrentUserDetails().isSuper()) {
      return false;
    }

    eventChangeLogService.deleteEventChangeLog(dataElement);
    dataValueAuditService.deleteDataValueAudits(dataElement);
    dataValueService.deleteDataValues(dataElement);

    log.info("Pruned data for data element: " + dataElement);

    return true;
  }

  @Override
  @IndirectTransactional
  public int removeExpiredInvitations() {
    UserQueryParams params = new UserQueryParams();
    params.setInvitationStatus(UserInvitationStatus.EXPIRED);
    List<UID> expired;
    try {
      expired = userService.getUserIds(params, null);
    } catch (ConflictException ex) {
      log.warn("Could not look up expired invitations: {}", ex.getMessage());
      return 0;
    }

    int removed = 0;
    for (UID uid : expired) {
      try {
        Boolean deleted = transactionTemplate.execute(status -> removeExpiredInvitation(uid));
        if (Boolean.TRUE.equals(deleted)) {
          removed++;
        }
      } catch (DeleteNotAllowedException ex) {
        log.warn("Could not remove expired invitation of user {}: {}", uid, ex.getMessage());
      }
    }

    log.info("Removed {} of {} expired invitations", removed, expired.size());
    return removed;
  }

  private boolean removeExpiredInvitation(UID uid) {
    User user = userService.getUser(uid.getValue());
    if (user == null) {
      return false;
    }
    if (user.getLastLogin() != null) {
      log.warn("Not removing expired invitation of user {} because the user has logged in", uid);
      return false;
    }
    userService.deleteUser(user);
    return true;
  }

  @Override
  public void clearApplicationCaches() {
    eventPublisher.publishEvent(new ApplicationCacheClearedEvent());
  }
}
