/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.cache.ETagVersionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests for the TTL time window flip behavior in ETag generation. Uses a fixed {@link Clock} to
 * deterministically control time and prove that ETags change when the time window bucket flips, and
 * remain stable within the same bucket.
 *
 * <p>The TTL window works by integer-dividing {@code clock.millis()} by the TTL in milliseconds.
 * With a 60-minute TTL, this produces buckets at epoch-aligned 60-minute boundaries (e.g., 12:00,
 * 13:00 UTC). All ETags include this bucket value, so they naturally expire when the bucket flips —
 * this is the safety net for mutations the DML observer might miss.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ETagTtlWindowFlipTest {

  private static final int TTL_MINUTES = 60;
  private static final long TTL_MILLIS = TimeUnit.MINUTES.toMillis(TTL_MINUTES);

  private static final Instant BUCKET_BOUNDARY =
      Instant.ofEpochMilli((System.currentTimeMillis() / TTL_MILLIS) * TTL_MILLIS);

  @Mock private ETagVersionService eTagVersionService;

  private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    User user = new User();
    user.setUid("testUser123");
    user.setUsername("testuser");
    userDetails = UserDetails.fromUser(user);

    when(eTagVersionService.getAllCacheVersion()).thenReturn(0L);
    when(eTagVersionService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(10L);
    when(eTagVersionService.getEntityTypeVersion(User.class)).thenReturn(20L);
    when(eTagVersionService.getEntityTypeVersion(UserRole.class)).thenReturn(30L);
    when(eTagVersionService.getTtlMinutes()).thenReturn(TTL_MINUTES);
  }

  // ── Global ETag ──

  @Test
  @DisplayName("Global ETag stays the same within the same TTL bucket")
  void globalETag_stableWithinBucket() {
    // 5 minutes after boundary — same bucket
    ConditionalETagService serviceAt5 = serviceAt(BUCKET_BOUNDARY.plusMillis(5 * 60_000));
    // 30 minutes after boundary — still same bucket
    ConditionalETagService serviceAt30 = serviceAt(BUCKET_BOUNDARY.plusMillis(30 * 60_000));

    String etag5 = serviceAt5.generateETag(userDetails);
    String etag30 = serviceAt30.generateETag(userDetails);

    assertEquals(etag5, etag30, "ETags within the same 60-min bucket must be identical");
  }

  @Test
  @DisplayName("Global ETag changes when the TTL bucket flips")
  void globalETag_changesOnBucketFlip() {
    // 1 millisecond before the next boundary — still current bucket
    ConditionalETagService serviceBefore = serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS - 1));
    // Exactly on the next boundary — new bucket
    ConditionalETagService serviceAfter = serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS));

    String etagBefore = serviceBefore.generateETag(userDetails);
    String etagAfter = serviceAfter.generateETag(userDetails);

    assertNotEquals(etagBefore, etagAfter, "ETag must change when the TTL bucket flips");
  }

  // ── Entity-type ETag ──

  @Test
  @DisplayName("Entity-type ETag stays the same within the same TTL bucket")
  void entityTypeETag_stableWithinBucket() {
    ConditionalETagService serviceAt5 = serviceAt(BUCKET_BOUNDARY.plusMillis(5 * 60_000));
    ConditionalETagService serviceAt55 = serviceAt(BUCKET_BOUNDARY.plusMillis(55 * 60_000));

    String etag5 = serviceAt5.generateETag(userDetails, OrganisationUnit.class);
    String etag55 = serviceAt55.generateETag(userDetails, OrganisationUnit.class);

    assertEquals(etag5, etag55, "Entity-type ETags within the same bucket must be identical");
  }

  @Test
  @DisplayName("Entity-type ETag changes when the TTL bucket flips")
  void entityTypeETag_changesOnBucketFlip() {
    ConditionalETagService serviceBefore = serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS - 1));
    ConditionalETagService serviceAfter = serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS));

    String etagBefore = serviceBefore.generateETag(userDetails, OrganisationUnit.class);
    String etagAfter = serviceAfter.generateETag(userDetails, OrganisationUnit.class);

    assertNotEquals(
        etagBefore, etagAfter, "Entity-type ETag must change when the TTL bucket flips");
  }

  // ── Composite ETag ──

  @Test
  @DisplayName("Composite ETag stays the same within the same TTL bucket")
  void compositeETag_stableWithinBucket() {
    Set<Class<?>> types = Set.of(OrganisationUnit.class, User.class, UserRole.class);

    ConditionalETagService serviceAt10 = serviceAt(BUCKET_BOUNDARY.plusMillis(10 * 60_000));
    ConditionalETagService serviceAt50 = serviceAt(BUCKET_BOUNDARY.plusMillis(50 * 60_000));

    String etag10 = serviceAt10.generateETag(userDetails, types);
    String etag50 = serviceAt50.generateETag(userDetails, types);

    assertEquals(etag10, etag50, "Composite ETags within the same bucket must be identical");
  }

  @Test
  @DisplayName("Composite ETag changes when the TTL bucket flips")
  void compositeETag_changesOnBucketFlip() {
    Set<Class<?>> types = Set.of(OrganisationUnit.class, User.class, UserRole.class);

    ConditionalETagService serviceBefore = serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS - 1));
    ConditionalETagService serviceAfter = serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS));

    String etagBefore = serviceBefore.generateETag(userDetails, types);
    String etagAfter = serviceAfter.generateETag(userDetails, types);

    assertNotEquals(etagBefore, etagAfter, "Composite ETag must change when the TTL bucket flips");
  }

  // ── Stale 304 simulation ──

  @Test
  @DisplayName(
      "Simulates the stale 304 scenario: unobserved mutation within the same TTL bucket"
          + " produces a stale match, but the bucket flip rescues it")
  void stale304_rescuedByBucketFlip() {
    // T=0: Client gets ETag at 5 minutes into the bucket
    ConditionalETagService serviceT0 = serviceAt(BUCKET_BOUNDARY.plusMillis(5 * 60_000));
    String clientETag = serviceT0.generateETag(userDetails, OrganisationUnit.class);

    // T=1: An unobserved mutation happens (direct SQL) — version is NOT bumped.
    // T=2: Client requests again at 30 minutes into the same bucket.
    // The version hasn't changed, so the ETag is the same → stale 304.
    ConditionalETagService serviceT2 = serviceAt(BUCKET_BOUNDARY.plusMillis(30 * 60_000));
    String serverETagT2 = serviceT2.generateETag(userDetails, OrganisationUnit.class);
    assertEquals(
        clientETag,
        serverETagT2,
        "Without a version bump, the ETag stays the same within the bucket (stale 304)");

    // T=3: The TTL bucket flips. Now the ETag changes even without a version bump.
    ConditionalETagService serviceT3 = serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS + 60_000));
    String serverETagT3 = serviceT3.generateETag(userDetails, OrganisationUnit.class);
    assertNotEquals(
        clientETag,
        serverETagT3,
        "After the TTL bucket flips, the client's stale ETag no longer matches → fresh 200");
  }

  @Test
  @DisplayName("Observed mutation invalidates immediately, regardless of TTL bucket")
  void observedMutation_invalidatesImmediately() {
    // T=0: Client gets ETag
    ConditionalETagService service = serviceAt(BUCKET_BOUNDARY.plusMillis(5 * 60_000));
    String clientETag = service.generateETag(userDetails, OrganisationUnit.class);

    // T=1: Mutation happens and observer bumps version (10 → 11)
    when(eTagVersionService.getEntityTypeVersion(OrganisationUnit.class)).thenReturn(11L);

    // T=2: Same moment, same bucket — but version changed
    String serverETag = service.generateETag(userDetails, OrganisationUnit.class);
    assertNotEquals(
        clientETag,
        serverETag,
        "Version bump invalidates immediately — no need to wait for TTL flip");
  }

  @Test
  @DisplayName("Worst case stale window is exactly TTL minutes when mutation is right after flip")
  void worstCaseStaleness_mutationRightAfterFlip() {
    // Mutation happens 1ms after the bucket boundary — worst case
    ConditionalETagService serviceAtFlip = serviceAt(BUCKET_BOUNDARY.plusMillis(1));
    String etagAtFlip = serviceAtFlip.generateETag(userDetails, OrganisationUnit.class);

    // Just before the NEXT flip — still same bucket, so unobserved mutation stays stale
    ConditionalETagService serviceBeforeNextFlip =
        serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS - 1));
    String etagBeforeNextFlip =
        serviceBeforeNextFlip.generateETag(userDetails, OrganisationUnit.class);
    assertEquals(
        etagAtFlip,
        etagBeforeNextFlip,
        "Worst case: stale for almost exactly TTL_MINUTES (minus 2ms)");

    // At the next flip — finally invalidated
    ConditionalETagService serviceAtNextFlip = serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS));
    String etagAtNextFlip = serviceAtNextFlip.generateETag(userDetails, OrganisationUnit.class);
    assertNotEquals(
        etagAtFlip, etagAtNextFlip, "After the next flip, the stale ETag is finally invalidated");
  }

  @Test
  @DisplayName("Best case stale window is near-zero when mutation is right before flip")
  void bestCaseStaleness_mutationRightBeforeFlip() {
    // Mutation happens 1ms before the bucket boundary — best case
    ConditionalETagService serviceBeforeFlip =
        serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS - 1));
    String etagBeforeFlip = serviceBeforeFlip.generateETag(userDetails, OrganisationUnit.class);

    // 1ms later the bucket flips — stale ETag is immediately invalidated
    ConditionalETagService serviceAfterFlip = serviceAt(BUCKET_BOUNDARY.plusMillis(TTL_MILLIS));
    String etagAfterFlip = serviceAfterFlip.generateETag(userDetails, OrganisationUnit.class);
    assertNotEquals(
        etagBeforeFlip, etagAfterFlip, "Best case: stale for only ~1ms before the bucket flips");
  }

  // ── Helper ──

  private ConditionalETagService serviceAt(Instant instant) {
    Clock fixedClock = Clock.fixed(instant, ZoneOffset.UTC);
    return new ConditionalETagService(eTagVersionService, fixedClock);
  }
}
