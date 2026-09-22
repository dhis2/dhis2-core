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

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

/** Just a helper class containing generic methods used during the simulation execution. */
public class SimulationHelper {

  /**
   * Scans the classpath under "javaPackage" and returns instances of every concrete class
   * implementing {@link AnalyticsSimulation}. Supports both directory- and JAR-based classpaths.
   * Results are sorted by fully-qualified class name for a deterministic run order.
   */
  public static List<AnalyticsSimulation> discoverSimulations(String javaPackage) {
    try {
      String resourcePath = javaPackage.replace('.', '/');
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      List<Class<?>> classes = new ArrayList<>();
      Enumeration<URL> resources = classLoader.getResources(resourcePath);
      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        if ("file".equals(resource.getProtocol())) {
          scanDirectory(new File(resource.toURI()), javaPackage, classes);
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
      throw new RuntimeException("Failed to discover simulations under " + javaPackage, e);
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

  private static AnalyticsSimulation instantiate(Class<?> clazz) {
    try {
      return (AnalyticsSimulation) clazz.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to instantiate " + clazz.getName(), e);
    }
  }
}
