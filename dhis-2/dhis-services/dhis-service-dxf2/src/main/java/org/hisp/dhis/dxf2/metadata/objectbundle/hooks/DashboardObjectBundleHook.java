/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashboardObjectBundleHook extends AbstractObjectBundleHook<Dashboard> {

  /**
   * In rare occasions, a Dashboard might be seen as non-persisted and put into the non-persisted
   * list, but it is actually a persisted item. This is most likely due to read permissions. In
   * these scenarios we want to be able to check if a Dashboard should be considered persisted
   * instead. Check if it exists in the database and if it does, move it to be seen as persisted.
   *
   * @param klass the class type of the objects
   * @param nonPersistedObjects the objects seen as non persisted
   * @param bundle the current commit phase bundle
   */
  @Override
  public <E extends Dashboard> void preTypeImport(
      Class<E> klass, List<E> nonPersistedObjects, ObjectBundle bundle) {
    List<Dashboard> dashboards =
        nonPersistedObjects.stream().filter(Objects::nonNull).collect(Collectors.toList());

    for (Dashboard dashboard : dashboards) {
      Dashboard existingDashboard = manager.getNoAcl(Dashboard.class, dashboard.getUid());
      if (existingDashboard != null) {
        bundle.moveNonPersistedToPersisted(Dashboard.class, existingDashboard);
      }
    }
  }
}
