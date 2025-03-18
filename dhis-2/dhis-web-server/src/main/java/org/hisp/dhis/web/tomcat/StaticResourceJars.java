/*
 * Copyright (c) 2004-2023, University of Oslo
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

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * This code is a modified version of the original code from Spring Boot project.
 *
 * <p>Logic to extract URLs of static resource jars (those containing {@code "META-INF/resources"}
 * directories).
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class StaticResourceJars {

  List<URL> getUrls() {
    ClassLoader classLoader = getClass().getClassLoader();
    if (classLoader instanceof URLClassLoader urlClassLoader) {
      return getUrlsFrom(urlClassLoader.getURLs());
    } else {
      return getUrlsFrom(
          Stream.of(ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator))
              .map(this::toUrl)
              .toArray(URL[]::new));
    }
  }

  List<URL> getUrlsFrom(URL... urls) {
    List<URL> resourceJarUrls = new ArrayList<>();
    for (URL url : urls) {
      addUrl(resourceJarUrls, url);
    }
    return resourceJarUrls;
  }

  private URL toUrl(String classPathEntry) {
    try {
      return new File(classPathEntry).toURI().toURL();
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException(
          "URL could not be created from '" + classPathEntry + "'", ex);
    }
  }

  private File toFile(URL url) {
    try {
      return new File(url.toURI());
    } catch (URISyntaxException ex) {
      throw new IllegalStateException("Failed to create File from URL '" + url + "'");
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private void addUrl(List<URL> urls, URL url) {
    try {
      if (!"file".equals(url.getProtocol())) {
        addUrlConnection(urls, url, url.openConnection());
      } else {
        File file = toFile(url);
        if (file != null) {
          addUrlFile(urls, url, file);
        } else {
          addUrlConnection(urls, url, url.openConnection());
        }
      }
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void addUrlFile(List<URL> urls, URL url, File file) {
    if ((file.isDirectory() && new File(file, "META-INF/resources").isDirectory())
        || isResourcesJar(file)) {
      urls.add(url);
    }
  }

  private void addUrlConnection(List<URL> urls, URL url, URLConnection connection) {
    if (connection instanceof JarURLConnection jarURLConnection
        && isResourcesJar(jarURLConnection)) {
      urls.add(url);
    }
  }

  private boolean isResourcesJar(JarURLConnection connection) {
    try {
      return isResourcesJar(connection.getJarFile(), !connection.getUseCaches());
    } catch (IOException ex) {
      return false;
    }
  }

  private boolean isResourcesJar(File file) {
    try {
      return isResourcesJar(new JarFile(file), true);
    } catch (IOException | InvalidPathException ex) {
      return false;
    }
  }

  private boolean isResourcesJar(JarFile jarFile, boolean closeJarFile) throws IOException {
    try {
      return jarFile.getName().endsWith(".jar")
          && (jarFile.getJarEntry("META-INF/resources") != null);
    } finally {
      if (closeJarFile) {
        jarFile.close();
      }
    }
  }
}
