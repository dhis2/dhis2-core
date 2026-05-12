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
package org.hisp.dhis.test.analytics;

import static org.hisp.dhis.test.analytics.TestDefinitions.simpleUsersRumpUp;
import static org.hisp.dhis.test.analytics.TestHelper.buildHttpProtocol;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;
import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class HmisSimulationsRunner extends Simulation {

  private static final String SIMULATIONS_PACKAGE = "org.hisp.dhis.test.analytics.hmis";

  public HmisSimulationsRunner() {
    // How users should enter the scenarios.
    OpenInjectionStep defaultInjectionStep = simpleUsersRumpUp(1, 10);

    // Discover and instantiate every AnalyticsSimulation under the hmis package.
    List<AnalyticsSimulation> simulations = discoverSimulations();

    // Build scenarios and assertions from the discovered simulations.
    List<PopulationBuilder> scenarios = new ArrayList<>();
    List<Assertion> assertions = new ArrayList<>();
    for (AnalyticsSimulation simulation : simulations) {
      scenarios.add(simulation.buildPopulation(defaultInjectionStep));
      assertions.addAll(simulation.buildAssertions());
    }

    // Execute and assert all scenarios.
    setUp(scenarios).protocols(buildHttpProtocol("/api/ping")).assertions(assertions);
  }

  /**
   * Scans the classpath under {@link #SIMULATIONS_PACKAGE} and returns instances of every concrete
   * class implementing {@link AnalyticsSimulation}. Supports both directory- and JAR-based
   * classpaths. Results are sorted by fully-qualified class name for a deterministic run order.
   */
  private static List<AnalyticsSimulation> discoverSimulations() {
    try {
      String resourcePath = SIMULATIONS_PACKAGE.replace('.', '/');
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      List<Class<?>> classes = new ArrayList<>();
      Enumeration<URL> resources = classLoader.getResources(resourcePath);
      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        if ("file".equals(resource.getProtocol())) {
          scanDirectory(new File(resource.toURI()), SIMULATIONS_PACKAGE, classes);
        } else if ("jar".equals(resource.getProtocol())) {
          scanJar(resource, resourcePath, classes);
        }
      }

      List<AnalyticsSimulation> simulations = new ArrayList<>();
      classes.stream()
          .filter(AnalyticsSimulation.class::isAssignableFrom)
          .filter(c -> !c.isInterface() && !Modifier.isAbstract(c.getModifiers()))
          .sorted(Comparator.comparing(Class::getName))
          .forEach(c -> simulations.add(instantiate(c)));
      return simulations;
    } catch (Exception e) {
      throw new RuntimeException("Failed to discover simulations under " + SIMULATIONS_PACKAGE, e);
    }
  }

  private static void scanDirectory(File directory, String packageName, List<Class<?>> out)
      throws ClassNotFoundException {
    if (!directory.exists()) {
      return;
    }
    File[] files = directory.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (file.isDirectory()) {
        scanDirectory(file, packageName + "." + file.getName(), out);
      } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
        String className =
            packageName + "." + file.getName().substring(0, file.getName().length() - 6);
        out.add(Class.forName(className));
      }
    }
  }

  private static void scanJar(URL jarUrl, String resourcePath, List<Class<?>> out)
      throws Exception {
    String path = jarUrl.getPath();
    String jarFilePath =
        URLDecoder.decode(path.substring(5, path.indexOf('!')), StandardCharsets.UTF_8);
    try (JarFile jar = new JarFile(jarFilePath)) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        String entryName = entries.nextElement().getName();
        if (entryName.startsWith(resourcePath)
            && entryName.endsWith(".class")
            && !entryName.contains("$")) {
          String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
          out.add(Class.forName(className));
        }
      }
    }
  }

  private static AnalyticsSimulation instantiate(Class<?> clazz) {
    try {
      return (AnalyticsSimulation) clazz.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to instantiate " + clazz.getName(), e);
    }
  }
}
