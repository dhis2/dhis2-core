/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.organisationunit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadParameterException;
import org.hisp.dhis.feedback.ErrorCode;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link OrgTreeParams} validation
 *
 * @author AI (based on code and requirements docs)
 */
class OrgTreeParamsTest {

  private static final Locale LOCALE = Locale.ENGLISH;
  private static final UID ROOT = UID.of("aaaaaaaaaaa");
  private static final UID ROOT_2 = UID.of("bbbbbbbbbbb");
  private static final UID PROGRAM = UID.of("ppppppppppp");
  private static final UID DATASET = UID.of("ddddddddddd");

  // ---------- helpers ----------

  /** Main-params-only variant: optional filters are null/false. */
  private static OrgTreeParams params(
      int page, int pageSize, List<OrgUnitPath> cursors, List<UID> roots, List<UID> groups) {
    return params(
        page, pageSize, cursors, roots, groups, null, null, null, null, null, false, null);
  }

  private static OrgTreeParams params(
      int page,
      int pageSize,
      List<OrgUnitPath> cursors,
      List<UID> roots,
      List<UID> groups,
      UID program,
      UID dataset,
      Integer level,
      Boolean currentlyOpen,
      String search,
      boolean shortName,
      Integer depth) {
    return new OrgTreeParams(
        page,
        pageSize,
        cursors,
        false,
        LOCALE,
        roots,
        groups,
        program,
        dataset,
        level,
        currentlyOpen,
        search,
        shortName,
        depth);
  }

  /** A list of N nulls – fine for the ctor since only {@code .size()} is used. */
  private static List<OrgUnitPath> dummyCursors(int n) {
    return Arrays.asList(new OrgUnitPath[n]);
  }

  // ---------- happy path ----------

  @Test
  void validMinimalParamsDoNotThrow() {
    assertDoesNotThrow(() -> params(1, 10, List.of(), List.of(ROOT), List.of()));
  }

  @Test
  void validParamsWithAllOptionalFiltersDoNotThrow() {
    assertDoesNotThrow(
        () ->
            params(
                1,
                10,
                List.of(),
                List.of(ROOT),
                List.of(ROOT_2),
                PROGRAM,
                null,
                2,
                true,
                "foo",
                true,
                null));
  }

  // ---------- requireNonNull: programming errors ----------

  @Test
  void nullLocaleIsProgrammerError() {
    assertThrows(
        NullPointerException.class,
        () ->
            new OrgTreeParams(
                1,
                10,
                List.of(),
                false,
                null,
                List.of(ROOT),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                false,
                null));
  }

  @Test
  void nullRootsIsProgrammerError() {
    assertThrows(
        NullPointerException.class,
        () ->
            new OrgTreeParams(
                1, 10, List.of(), false, LOCALE, null, List.of(), null, null, null, null, null,
                false, null));
  }

  @Test
  void nullGroupsIsProgrammerError() {
    assertThrows(
        NullPointerException.class,
        () ->
            new OrgTreeParams(
                1,
                10,
                List.of(),
                false,
                LOCALE,
                List.of(ROOT),
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null));
  }

  @Test
  void nullPageCursorsIsProgrammerError() {
    assertThrows(
        NullPointerException.class,
        () ->
            new OrgTreeParams(
                1,
                10,
                null,
                false,
                LOCALE,
                List.of(ROOT),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                false,
                null));
  }

  // ---------- page (E8301) ----------

  @Test
  void pageZeroIsRejected() {
    BadParameterException ex =
        assertThrows(
            BadParameterException.class, () -> params(0, 10, List.of(), List.of(ROOT), List.of()));
    assertEquals(ErrorCode.E8301, ex.getCode());
    assertArrayEquals(new Object[] {"page", 0}, ex.getArgs());
  }

  @Test
  void negativePageIsRejected() {
    BadParameterException ex =
        assertThrows(
            BadParameterException.class, () -> params(-1, 10, List.of(), List.of(ROOT), List.of()));
    assertEquals(ErrorCode.E8301, ex.getCode());
    assertArrayEquals(new Object[] {"page", -1}, ex.getArgs());
  }

  // ---------- pageSize (E8301) ----------

  @Test
  void pageSizeZeroIsRejected() {
    BadParameterException ex =
        assertThrows(
            BadParameterException.class, () -> params(1, 0, List.of(), List.of(ROOT), List.of()));
    assertEquals(ErrorCode.E8301, ex.getCode());
    assertArrayEquals(new Object[] {"pageSize", 0}, ex.getArgs());
  }

  @Test
  void negativePageSizeIsRejected() {
    BadParameterException ex =
        assertThrows(
            BadParameterException.class, () -> params(1, -5, List.of(), List.of(ROOT), List.of()));
    assertEquals(ErrorCode.E8301, ex.getCode());
    assertArrayEquals(new Object[] {"pageSize", -5}, ex.getArgs());
  }

  // ---------- level (E8301) ----------

  @Test
  void levelZeroIsRejected() {
    BadParameterException ex =
        assertThrows(
            BadParameterException.class,
            () ->
                params(
                    1,
                    10,
                    List.of(),
                    List.of(ROOT),
                    List.of(),
                    null,
                    null,
                    0,
                    null,
                    null,
                    false,
                    null));
    assertEquals(ErrorCode.E8301, ex.getCode());
    assertArrayEquals(new Object[] {"level", 0}, ex.getArgs());
  }

  @Test
  void levelNullIsAllowed() {
    assertDoesNotThrow(() -> params(1, 10, List.of(), List.of(ROOT), List.of()));
  }

  @Test
  void levelOneIsAllowed() {
    assertDoesNotThrow(
        () ->
            params(
                1,
                10,
                List.of(),
                List.of(ROOT),
                List.of(),
                null,
                null,
                1,
                null,
                null,
                false,
                null));
  }

  // ---------- depth (E8301) ----------

  @Test
  void depthZeroIsRejected() {
    BadParameterException ex =
        assertThrows(
            BadParameterException.class,
            () ->
                params(
                    1,
                    10,
                    List.of(),
                    List.of(ROOT),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    0));
    assertEquals(ErrorCode.E8301, ex.getCode());
    assertArrayEquals(new Object[] {"depth", 0}, ex.getArgs());
  }

  @Test
  void depthNullIsAllowed() {
    assertDoesNotThrow(() -> params(1, 10, List.of(), List.of(ROOT), List.of()));
  }

  @Test
  void depthOneIsAllowed() {
    assertDoesNotThrow(
        () ->
            params(
                1,
                10,
                List.of(),
                List.of(ROOT),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                false,
                1));
  }

  // ---------- program vs dataset (E8304) ----------

  @Test
  void programAndDatasetTogetherAreRejected() {
    BadParameterException ex =
        assertThrows(
            BadParameterException.class,
            () ->
                params(
                    1,
                    10,
                    List.of(),
                    List.of(ROOT),
                    List.of(),
                    PROGRAM,
                    DATASET,
                    null,
                    null,
                    null,
                    false,
                    null));
    assertEquals(ErrorCode.E8304, ex.getCode());
    assertArrayEquals(new Object[] {"program", "dataset"}, ex.getArgs());
  }

  @Test
  void programAloneIsAllowed() {
    assertDoesNotThrow(
        () ->
            params(
                1,
                10,
                List.of(),
                List.of(ROOT),
                List.of(),
                PROGRAM,
                null,
                null,
                null,
                null,
                false,
                null));
  }

  @Test
  void datasetAloneIsAllowed() {
    assertDoesNotThrow(
        () ->
            params(
                1,
                10,
                List.of(),
                List.of(ROOT),
                List.of(),
                null,
                DATASET,
                null,
                null,
                null,
                false,
                null));
  }

  // ---------- depth vs search (E8304) ----------

  @Test
  void depthAndSearchTogetherAreRejected() {
    BadParameterException ex =
        assertThrows(
            BadParameterException.class,
            () ->
                params(
                    1,
                    10,
                    List.of(),
                    List.of(ROOT),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    "findme",
                    false,
                    2));
    assertEquals(ErrorCode.E8304, ex.getCode());
    assertArrayEquals(new Object[] {"depth", "search"}, ex.getArgs());
  }

  @Test
  void searchWithoutDepthIsAllowed() {
    assertDoesNotThrow(
        () ->
            params(
                1,
                10,
                List.of(),
                List.of(ROOT),
                List.of(),
                null,
                null,
                null,
                null,
                "findme",
                false,
                null));
  }

  @Test
  void depthWithoutSearchIsAllowed() {
    assertDoesNotThrow(
        () ->
            params(
                1,
                10,
                List.of(),
                List.of(ROOT),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                false,
                2));
  }

  // ---------- empty roots (E8305) ----------

  @Test
  void emptyRootsAreRejected() {
    List<UID> emptyRoots = List.of();
    BadParameterException ex =
        assertThrows(
            BadParameterException.class, () -> params(1, 10, List.of(), emptyRoots, List.of()));
    assertEquals(ErrorCode.E8305, ex.getCode());
    assertArrayEquals(new Object[] {"roots", 1, emptyRoots}, ex.getArgs());
  }

  // ---------- page <-> pageCursors (E8305) ----------

  @Test
  void pageOneDoesNotRequireCursors() {
    assertDoesNotThrow(() -> params(1, 10, List.of(), List.of(ROOT), List.of()));
  }

  @Test
  void pageTwoWithoutCursorIsRejected() {
    List<OrgUnitPath> emptyCursors = List.of();
    BadParameterException ex =
        assertThrows(
            BadParameterException.class,
            () -> params(2, 10, emptyCursors, List.of(ROOT), List.of()));
    assertEquals(ErrorCode.E8305, ex.getCode());
    assertArrayEquals(new Object[] {"pageCursor", 1, emptyCursors}, ex.getArgs());
  }

  @Test
  void pageTwoWithOneCursorIsAllowed() {
    assertDoesNotThrow(() -> params(2, 10, dummyCursors(1), List.of(ROOT), List.of()));
  }

  @Test
  void pageThreeWithOneCursorIsRejected() {
    List<OrgUnitPath> oneCursor = dummyCursors(1);
    BadParameterException ex =
        assertThrows(
            BadParameterException.class, () -> params(3, 10, oneCursor, List.of(ROOT), List.of()));
    assertEquals(ErrorCode.E8305, ex.getCode());
    assertArrayEquals(new Object[] {"pageCursor", 2, oneCursor}, ex.getArgs());
  }

  @Test
  void pageThreeWithTwoCursorsIsAllowed() {
    assertDoesNotThrow(() -> params(3, 10, dummyCursors(2), List.of(ROOT), List.of()));
  }
}
