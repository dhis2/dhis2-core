/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker;

import static org.hisp.dhis.feedback.Assertions.assertNoErrors;
import static org.hisp.dhis.webapi.controller.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Setup metadata and tracker data for tests.
 *
 * <p>Keep the copies in dhis-test-integration and dhis-test-web-api in sync! We can currently not
 * share test code between these modules without introducing cycles or adding test code to the main
 * module.
 */
@Component
public class TestSetup {
  @Autowired private RenderService renderService;

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private TrackerImportService trackerImportService;

  /**
   * Import the base metadata used by most tracker tests.
   *
   * <p>Use {@link #importMetadata(String)} (String)}
   *
   * <ul>
   *   <li>instead if your test only needs a subset of what the tracker base metadata contains
   *   <li>in addition if your test needs some additional metadata that not all tracker tests need
   * </ul>
   */
  public ObjectBundle importMetadata() throws IOException {
    return importMetadata("tracker/base_metadata.json", null);
  }

  public ObjectBundle importMetadata(String path) throws IOException {
    return importMetadata(path, null);
  }

  public ObjectBundle importMetadata(String path, User user) throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(new ClassPathResource(path).getInputStream(), RenderFormat.JSON);
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(metadata);
    params.setUserDetails(UserDetails.fromUser(user));
    ObjectBundle bundle = objectBundleService.create(params);
    assertNoErrors(objectBundleValidationService.validate(bundle));
    objectBundleService.commit(bundle);
    return bundle;
  }

  /**
   * Import the base tracker data used by most tracker tests.
   *
   * <p>Use {@link #importTrackerData(String)}
   *
   * <ul>
   *   <li>instead if your test only needs a subset of what the tracker base data contains
   *   <li>in addition if your test needs some additional data that not all tracker tests need
   * </ul>
   */
  public TrackerObjects importTrackerData() throws IOException {
    return importTrackerData("tracker/base_data.json");
  }

  /**
   * Import tracker data from a JSON fixture using the default import parameters. Use {@link
   * #importTrackerData(String, TrackerImportParams)} if you need non-default import parameters.
   */
  public TrackerObjects importTrackerData(String path) throws IOException {
    return importTrackerData(path, TrackerImportParams.builder().build());
  }

  public TrackerObjects importTrackerData(String path, TrackerImportParams params)
      throws IOException {
    TrackerObjects trackerObjects = fromJson(path);
    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));
    return trackerObjects;
  }

  public TrackerObjects fromJson(String path) throws IOException {
    return renderService.fromJson(
        new ClassPathResource(path).getInputStream(), TrackerObjects.class);
  }

  @Nonnull
  public TrackedEntity getTrackedEntity(
      @Nonnull TrackerObjects trackerObjects, @Nonnull String uid) {
    Optional<TrackedEntity> trackedEntity = trackerObjects.findTrackedEntity(UID.of(uid));
    assertTrue(
        trackedEntity.isPresent(),
        () -> String.format("TrackedEntity with uid '%s' should have been created", uid));
    return trackedEntity.orElse(null);
  }
}
