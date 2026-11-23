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
package org.hisp.dhis.interpretation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

class InterpretationCommentTest {

  @Test
  void testEqualsAndHashCodeCompareUidAndRealClass() {
    InterpretationComment a = comment("commentUidA", "Text A");
    InterpretationComment sameUid = comment("commentUidA", "Other text");
    InterpretationComment otherUid = comment("commentUidB", "Text A");
    InterpretationComment subclass = new InterpretationComment() {};
    subclass.setUid("commentUidA");

    assertEquals(a, sameUid);
    assertEquals(a.hashCode(), sameUid.hashCode());
    assertNotEquals(a, otherUid);
    assertNotEquals(a, subclass);
  }

  /** Code and name are always null, so comments without a uid are equal, as they were before. */
  @Test
  void testCommentsWithoutUidAreEqual() {
    InterpretationComment a = new InterpretationComment("Text A");
    InterpretationComment b = new InterpretationComment("Text B");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testUnsupportedPropertiesIgnoreWrites() {
    InterpretationComment comment = comment("commentUidA", "Text");
    Sharing sharing = new Sharing();
    sharing.setOwner("ownerUid123");

    comment.setCode("CODE");
    comment.setName("Name");
    comment.setSharing(sharing);
    comment.setOwner("ownerUid123");
    comment.setLastUpdatedBy(new User());

    assertNull(comment.getCode());
    assertNull(comment.getName());
    assertNull(comment.getDisplayName());
    assertNull(comment.getLastUpdatedBy());
    assertNull(comment.getSharing().getOwner());
    assertNotSame(comment.getSharing(), comment.getSharing());
  }

  @Test
  void testSetUserSetsCreatedByOnlyWhenAbsent() {
    User first = new User();
    User second = new User();
    InterpretationComment comment = comment("commentUidA", "Text");

    comment.setUser(first);
    assertNull(comment.getSharing().getOwner());
    comment.setUser(second);

    assertSame(first, comment.getCreatedBy());
    assertSame(first, comment.getUser());
  }

  @Test
  void testTranslationsAreKeptInMemory() {
    InterpretationComment comment = comment("commentUidA", "Text");
    Set<Translation> translations = Set.of(new Translation(Locale.of("fr"), "NAME", "x"));

    comment.setTranslations(translations);

    assertEquals(translations, comment.getTranslations());
  }

  private static InterpretationComment comment(String uid, String text) {
    InterpretationComment comment = new InterpretationComment(text);
    comment.setUid(uid);
    return comment;
  }
}
