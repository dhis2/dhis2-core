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
package org.hisp.dhis.web.tomcat;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class GracefulShutdownTest {

  @Test
  void stopDuringStartupReturnsWithoutWaitingForStartup(@TempDir Path baseDir) throws Exception {
    CountDownLatch startupEntered = new CountDownLatch(1);
    CountDownLatch finishStartup = new CountDownLatch(1);
    Tomcat tomcat =
        newTomcat(
            baseDir,
            (classes, servletContext) -> {
              startupEntered.countDown();
              await(finishStartup);
            });
    Thread startThread = new Thread(() -> start(tomcat), "tomcat-start");
    startThread.start();
    try {
      assertTrue(startupEntered.await(30, SECONDS));

      assertTimeoutPreemptively(Duration.ofSeconds(5), () -> Main.stopTomcat(tomcat));
      assertEquals(LifecycleState.STARTING, tomcat.getServer().getState());
    } finally {
      finishStartup.countDown();
      startThread.join();
      if (tomcat.getServer().getState() == LifecycleState.STARTED) {
        tomcat.stop();
        tomcat.destroy();
      }
    }
  }

  @Test
  void stopAfterStartupClosesWebApplication(@TempDir Path baseDir) throws Exception {
    AtomicBoolean contextDestroyed = new AtomicBoolean();
    Tomcat tomcat =
        newTomcat(
            baseDir,
            (classes, servletContext) ->
                servletContext.addListener(
                    new ServletContextListener() {
                      @Override
                      public void contextDestroyed(ServletContextEvent event) {
                        contextDestroyed.set(true);
                      }
                    }));
    tomcat.start();

    Main.stopTomcat(tomcat);

    assertTrue(contextDestroyed.get());
    assertEquals(LifecycleState.DESTROYED, tomcat.getServer().getState());
  }

  private static Tomcat newTomcat(Path baseDir, ServletContainerInitializer initializer) {
    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(baseDir.toString());
    Context context = tomcat.addContext("", baseDir.toString());
    context.addServletContainerInitializer(initializer, null);
    return tomcat;
  }

  private static void start(Tomcat tomcat) {
    try {
      tomcat.start();
    } catch (LifecycleException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(30, SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
