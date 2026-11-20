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
package org.hisp.dhis.webapi.security.session;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.hisp.dhis.security.DhisAuthenticationToken;
import org.hisp.dhis.security.DhisAuthenticationToken.LoginMethod;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsChangeTracker;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * Runs parallel requests of one in-memory session through the {@link UserDetailsRefreshFilter}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class UserDetailsRefreshConcurrencyTest {

  private static final String USERNAME = "alice";

  private static final String USER_UID = "aliceUid001";

  private UserDetailsChangeTracker tracker;

  private UserService userService;

  private UserDetailsRefreshFilter filter;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    tracker = mock(UserDetailsChangeTracker.class);
    userService = mock(UserService.class);
    filter =
        new UserDetailsRefreshFilter(
            new UserDetailsSessionRefresher(tracker, userService, Duration.ZERO));
    session = new MockHttpSession();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void parallelRequestsThatSeeTheSameChangeRebuildOnce() throws Exception {
    int parallel = 20;
    Set<Thread> requestsThatSawTheChange = ConcurrentHashMap.newKeySet();
    CountDownLatch allSawTheChange = new CountDownLatch(parallel);
    when(tracker.getChangeToken(USERNAME))
        .thenAnswer(
            call -> {
              if (requestsThatSawTheChange.add(Thread.currentThread())) {
                allSawTheChange.countDown();
              }
              return "token1";
            });
    AtomicInteger rebuilds = new AtomicInteger();
    when(userService.createUserDetailsSafe(USER_UID))
        .thenAnswer(
            call -> {
              rebuilds.incrementAndGet();
              // every request has seen the change before the first rebuild is stored
              assertTrue(allSawTheChange.await(10, SECONDS));
              return user("F_NEW");
            });
    Authentication login = passwordLogin(user("F_OLD"));

    ExecutorService pool = Executors.newFixedThreadPool(parallel);
    List<Future<Authentication>> requests = new ArrayList<>();
    try {
      for (int i = 0; i < parallel; i++) {
        requests.add(pool.submit(() -> request(login)));
      }
      for (Future<Authentication> request : requests) {
        UserDetails principal = (UserDetails) request.get(30, SECONDS).getPrincipal();
        assertTrue(principal.isAuthorized("F_NEW"));
      }
    } finally {
      pool.shutdownNow();
    }

    assertEquals(1, rebuilds.get());
  }

  @Test
  void waitingRequestNeverStoresANewerTokenWithAnOlderPrincipal() throws Exception {
    AtomicReference<String> currentToken = new AtomicReference<>("T1");
    when(tracker.getChangeToken(USERNAME)).thenAnswer(call -> currentToken.get());
    AtomicReference<Thread> olderRequest = new AtomicReference<>();
    CountDownLatch olderIsRebuilding = new CountDownLatch(1);
    CountDownLatch releaseOlder = new CountDownLatch(1);
    when(userService.createUserDetailsSafe(USER_UID))
        .thenAnswer(
            call -> {
              if (Thread.currentThread() != olderRequest.get()) {
                return user("F_STATE_AFTER_CHANGE_2");
              }
              olderIsRebuilding.countDown();
              assertTrue(releaseOlder.await(10, SECONDS));
              return user("F_STATE_AFTER_CHANGE_1");
            });
    Authentication login = passwordLogin(user("F_OLD"));
    AtomicReference<Throwable> failure = new AtomicReference<>();

    olderRequest.set(requestThread(login, failure));
    olderRequest.get().start();
    assertTrue(olderIsRebuilding.await(10, SECONDS));
    // change 2 commits while the older request still rebuilds for change 1
    currentToken.set("T2");
    Thread newerRequest = requestThread(login, failure);
    newerRequest.start();
    awaitWaiting(newerRequest);
    releaseOlder.countDown();
    olderRequest.get().join(10_000);
    newerRequest.join(10_000);

    assertNull(failure.get());
    assertEquals("T2", UserDetailsRefreshState.of(session).changeToken());
    assertTrue(savedPrincipal().isAuthorized("F_STATE_AFTER_CHANGE_2"));

    request(savedAuthentication());
    verify(userService, times(2)).createUserDetailsSafe(USER_UID);
    assertTrue(savedPrincipal().isAuthorized("F_STATE_AFTER_CHANGE_2"));
  }

  /**
   * A parallel request refreshes the session right before this request reads the refresh state. The
   * state then says the change is applied, but this request still holds the old authentication.
   */
  @Test
  void requestThatSeesTheStateOfAParallelRefreshContinuesWithItsPrincipal() throws Exception {
    when(tracker.getChangeToken(USERNAME)).thenReturn("token1");
    when(userService.createUserDetailsSafe(USER_UID)).thenReturn(user("F_NEW"));
    Authentication login = passwordLogin(user("F_OLD"));
    Thread thisRequest = Thread.currentThread();
    AtomicBoolean refreshedInParallel = new AtomicBoolean();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    session =
        new MockHttpSession() {
          @Override
          public Object getAttribute(String name) {
            if (Thread.currentThread() == thisRequest
                && !name.equals(SPRING_SECURITY_CONTEXT_KEY)
                && refreshedInParallel.compareAndSet(false, true)) {
              runParallelRequest(login, failure);
            }
            return super.getAttribute(name);
          }
        };

    Authentication continuedWith = request(login);

    assertNull(failure.get());
    assertTrue(refreshedInParallel.get());
    assertTrue(((UserDetails) continuedWith.getPrincipal()).isAuthorized("F_NEW"));
    verify(userService, times(1)).createUserDetailsSafe(USER_UID);
  }

  private void runParallelRequest(
      Authentication authentication, AtomicReference<Throwable> failure) {
    Thread parallel = requestThread(authentication, failure);
    parallel.start();
    try {
      parallel.join(10_000);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      failure.compareAndSet(null, ex);
    }
  }

  /** Sends a request of the session and returns the authentication it continued with. */
  private Authentication request(Authentication authentication) throws Exception {
    SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    try {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setSession(session);
      AtomicReference<Authentication> continuedWith = new AtomicReference<>();
      filter.doFilter(
          request,
          new MockHttpServletResponse(),
          (req, res) -> continuedWith.set(SecurityContextHolder.getContext().getAuthentication()));
      return continuedWith.get();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private Thread requestThread(Authentication authentication, AtomicReference<Throwable> failure) {
    return new Thread(
        () -> {
          try {
            request(authentication);
          } catch (Throwable ex) {
            failure.compareAndSet(null, ex);
          }
        });
  }

  private static void awaitWaiting(Thread thread) throws InterruptedException {
    long deadline = System.nanoTime() + SECONDS.toNanos(10);
    while (thread.getState() != Thread.State.WAITING) {
      if (System.nanoTime() > deadline) {
        fail("Request did not wait for the refresh of the other request");
      }
      Thread.sleep(1);
    }
  }

  private Authentication savedAuthentication() {
    return ((SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY))
        .getAuthentication();
  }

  private UserDetails savedPrincipal() {
    return (UserDetails) savedAuthentication().getPrincipal();
  }

  private static Authentication passwordLogin(UserDetails user) {
    return new DhisAuthenticationToken(
        user, null, new ArrayList<>(user.getAuthorities()), LoginMethod.LOCAL);
  }

  private static UserDetails user(String authority) {
    return UserDetails.empty()
        .uid(USER_UID)
        .username(USERNAME)
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .authorities(List.of(new SimpleGrantedAuthority(authority)))
        .allAuthorities(Set.of(authority))
        .build();
  }
}
