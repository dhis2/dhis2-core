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
package org.hisp.dhis.tracker.imports.preprocess;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.User;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignedUserPreProcessorTest extends DhisConvenienceTest {
  private static final String UID = "User uid";

  private static final String USERNAME = "Username";

  private AssignedUserPreProcessor preProcessorToTest = new AssignedUserPreProcessor();

  @Mock private TrackerPreheat preheat;

  @Test
  void testPreprocessorWhenUserHasOnlyUidSet() {
    Event event = new Event();
    event.setAssignedUser(userWithOnlyUid());
    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    when(preheat.getUserByUid(UID)).thenReturn(Optional.of(completeUser()));

    preProcessorToTest.process(bundle);

    verify(preheat, times(0)).getUserByUsername(anyString());
    verify(preheat, times(1)).getUserByUid(anyString());

    MatcherAssert.assertThat(event.getAssignedUser().getUsername(), equalTo(USERNAME));
    MatcherAssert.assertThat(event.getAssignedUser().getUid(), equalTo(UID));
  }

  @Test
  void testPreprocessorWhenUserHasOnlyUsernameSet() {
    Event event = new Event();
    event.setAssignedUser(userWithOnlyUsername());
    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    when(preheat.getUserByUsername(USERNAME)).thenReturn(Optional.of(completeUser()));

    preProcessorToTest.process(bundle);

    verify(preheat, times(1)).getUserByUsername(anyString());
    verify(preheat, times(0)).getUserByUid(anyString());

    MatcherAssert.assertThat(event.getAssignedUser().getUsername(), equalTo(USERNAME));
    MatcherAssert.assertThat(event.getAssignedUser().getUid(), equalTo(UID));
  }

  @ParameterizedTest
  @MethodSource("userInfoProvider")
  void testPreprocessorDoNothing(User user) {
    Event event = new Event();
    event.setAssignedUser(user);
    TrackerBundle bundle =
        TrackerBundle.builder().events(Collections.singletonList(event)).preheat(preheat).build();

    preProcessorToTest.process(bundle);

    verify(preheat, times(0)).getUserByUsername(anyString());
    verify(preheat, times(0)).getUserByUid(anyString());
  }

  static Stream<User> userInfoProvider() {
    return Stream.of(userWithUidAndUsername(), userWithNoFields());
  }

  private static User userWithOnlyUid() {

    return User.builder().uid(UID).build();
  }

  private static User userWithOnlyUsername() {
    return User.builder().username(USERNAME).build();
  }

  private static User userWithUidAndUsername() {
    return User.builder().uid(UID).username(USERNAME).build();
  }

  private static org.hisp.dhis.user.User completeUser() {
    org.hisp.dhis.user.User user = new org.hisp.dhis.user.User();
    user.setUid(UID);
    user.setUsername(USERNAME);
    return user;
  }

  private static User userWithNoFields() {
    return User.builder().build();
  }
}
