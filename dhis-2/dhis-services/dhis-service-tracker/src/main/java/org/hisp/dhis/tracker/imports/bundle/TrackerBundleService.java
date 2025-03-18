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
package org.hisp.dhis.tracker.imports.bundle;

import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.job.TrackerNotificationDataBundle;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface TrackerBundleService {
  /**
   * Creates and prepares tracker bundle.
   *
   * @param params Params object for this bundle.
   * @return Configured TrackerBundle instance(s) (if bundle splitting is enabled)
   */
  @Nonnull
  TrackerBundle create(
      @Nonnull TrackerImportParams params,
      @Nonnull TrackerObjects trackerObjects,
      @Nonnull UserDetails user);

  /**
   * Call rule engine for tracker bundle.
   *
   * @return Tracker bundle populated with rule effects
   */
  @Nonnull
  TrackerBundle runRuleEngine(@Nonnull TrackerBundle bundle);

  /**
   * Commits objects from bundle into persistence store if bundle mode COMMIT is enabled.
   *
   * @param bundle TrackerBundle to commit.
   */
  @Nonnull
  PersistenceReport commit(@Nonnull TrackerBundle bundle);

  /**
   * Carry out notifications for TrackerImporter.
   *
   * @param bundles {@link TrackerNotificationDataBundle} to hold data for notifications.
   */
  void sendNotifications(@Nonnull List<TrackerNotificationDataBundle> bundles);

  /**
   * Deletes objects in the bundle from persistence store if bundle mode DELETE is enabled.
   *
   * @param bundle TrackerBundle to delete.
   */
  @Nonnull
  PersistenceReport delete(@Nonnull TrackerBundle bundle)
      throws ForbiddenException, NotFoundException;

  /**
   * Finalize bundle objects
   *
   * @param bundle to process in post commit operations if any
   */
  void postCommit(@Nonnull TrackerBundle bundle);
}
