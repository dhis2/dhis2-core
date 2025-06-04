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
package org.hisp.dhis.trackedentityattributevalue;

import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_TRACKER;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueChangeLogService")
public class DefaultTrackedEntityAttributeValueChangeLogService
    implements TrackedEntityAttributeValueChangeLogService {
  private final TrackedEntityAttributeValueChangeLogStore attributeValueChangeLogStore;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final DhisConfigurationProvider config;

  @Override
  public void addTrackedEntityAttributeValueChangLog(
      TrackedEntityAttributeValueChangeLog attributeValueChangeLog) {
    if (config.isEnabled(CHANGELOG_TRACKER)) {
      attributeValueChangeLogStore.addTrackedEntityAttributeValueChangeLog(attributeValueChangeLog);
    }
  }

  @Override
  public List<TrackedEntityAttributeValueChangeLog> getTrackedEntityAttributeValueChangeLogs(
      TrackedEntityAttributeValueChangeLogQueryParams params) {
    return aclFilter(attributeValueChangeLogStore.getTrackedEntityAttributeValueChangeLogs(params));
  }

  private List<TrackedEntityAttributeValueChangeLog> aclFilter(
      List<TrackedEntityAttributeValueChangeLog> attributeValueChangeLogs) {
    // Fetch all the Tracked Entity Attributes this user has access
    // to (only store UIDs). Not a very efficient solution, but at the
    // moment
    // we do not have ACL API to check TE attributes.

    Set<String> allUserReadableTrackedEntityAttributes =
        trackedEntityAttributeService
            .getAllUserReadableTrackedEntityAttributes(CurrentUserUtil.getCurrentUserDetails())
            .stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet());

    return attributeValueChangeLogs.stream()
        .filter(
            audit -> allUserReadableTrackedEntityAttributes.contains(audit.getAttribute().getUid()))
        .toList();
  }

  @Override
  public int countTrackedEntityAttributeValueChangeLogs(
      TrackedEntityAttributeValueChangeLogQueryParams params) {
    return attributeValueChangeLogStore.countTrackedEntityAttributeValueChangeLogs(params);
  }

  @Override
  public void deleteTrackedEntityAttributeValueChangeLogs(TrackedEntity trackedEntity) {
    attributeValueChangeLogStore.deleteTrackedEntityAttributeValueChangeLogs(trackedEntity);
  }
}
