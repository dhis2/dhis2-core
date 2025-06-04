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
package org.hisp.dhis.trackedentitydatavalue;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_TRACKER;

import java.util.List;
import java.util.function.Predicate;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntityDataValueChangeLogQueryParams;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service("org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueChangeLogService")
public class DefaultTrackedEntityDataValueChangeLogService
    implements TrackedEntityDataValueChangeLogService {
  private final TrackedEntityDataValueChangeLogStore trackedEntityDataValueChangeLogStore;

  private final Predicate<TrackedEntityDataValueChangeLog> aclFilter;

  private final DhisConfigurationProvider config;

  public DefaultTrackedEntityDataValueChangeLogService(
      TrackedEntityDataValueChangeLogStore trackedEntityDataValueChangeLogStore,
      TrackerAccessManager trackerAccessManager,
      UserService userService,
      DhisConfigurationProvider config) {
    checkNotNull(trackedEntityDataValueChangeLogStore);
    checkNotNull(trackerAccessManager);
    checkNotNull(userService);

    this.trackedEntityDataValueChangeLogStore = trackedEntityDataValueChangeLogStore;
    this.config = config;

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    aclFilter =
        changeLog ->
            trackerAccessManager
                .canRead(currentUser, changeLog.getEvent(), changeLog.getDataElement(), false)
                .isEmpty();
  }

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void addTrackedEntityDataValueChangeLog(
      TrackedEntityDataValueChangeLog trackedEntityDataValueChangeLog) {
    if (config.isEnabled(CHANGELOG_TRACKER)) {
      trackedEntityDataValueChangeLogStore.addTrackedEntityDataValueChangeLog(
          trackedEntityDataValueChangeLog);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityDataValueChangeLog> getTrackedEntityDataValueChangeLogs(
      TrackedEntityDataValueChangeLogQueryParams params) {

    return trackedEntityDataValueChangeLogStore.getTrackedEntityDataValueChangeLogs(params).stream()
        .filter(aclFilter)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public int countTrackedEntityDataValueChangeLogs(
      TrackedEntityDataValueChangeLogQueryParams params) {
    return trackedEntityDataValueChangeLogStore.countTrackedEntityDataValueChangeLogs(params);
  }

  @Override
  @Transactional
  public void deleteTrackedEntityDataValueChangeLog(DataElement dataElement) {
    trackedEntityDataValueChangeLogStore.deleteTrackedEntityDataValueChangeLog(dataElement);
  }

  @Override
  @Transactional
  public void deleteTrackedEntityDataValueChangeLog(Event event) {
    trackedEntityDataValueChangeLogStore.deleteTrackedEntityDataValueChangeLog(event);
  }
}
