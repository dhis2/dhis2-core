/*
 * Copyright (c) 2004-2021, University of Oslo
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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.loader.ParallelWebappClassLoader;
import org.apache.tomcat.util.compat.JreCompat;

/**
 * This code is a modified version of the original code from Spring Boot project.
 *
 * <p>Extension of Tomcat's {@link ParallelWebappClassLoader} that does not consider the {@link
 * ClassLoader#getSystemClassLoader() system classloader}. This is required to ensure that any
 * custom context class loader is always used (as is the case with some executable archives).
 *
 * @author Phillip Webb
 * @author Andy Clement
 */
@Slf4j
public class TomcatEmbeddedWebappClassLoader extends ParallelWebappClassLoader {

  static {
    if (!JreCompat.isGraalAvailable()) {
      ClassLoader.registerAsParallelCapable();
    }
  }

  public TomcatEmbeddedWebappClassLoader() {}

  public TomcatEmbeddedWebappClassLoader(ClassLoader parent) {
    super(parent);
  }

  @Override
  public URL findResource(String name) {
    return null;
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    return Collections.emptyEnumeration();
  }

  @Override
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (JreCompat.isGraalAvailable() ? this : getClassLoadingLock(name)) {
      Class<?> result = findExistingLoadedClass(name);
      result = (result != null) ? result : doLoadClass(name);
      if (result == null) {
        throw new ClassNotFoundException(name);
      }
      return resolveIfNecessary(result, resolve);
    }
  }

  private Class<?> findExistingLoadedClass(String name) {
    Class<?> resultClass = findLoadedClass0(name);
    resultClass =
        (resultClass != null || JreCompat.isGraalAvailable()) ? resultClass : findLoadedClass(name);
    return resultClass;
  }

  private Class<?> doLoadClass(String name) {
    if ((this.delegate || filter(name, true))) {
      Class<?> result = loadFromParent(name);
      return (result != null) ? result : findClassIgnoringNotFound(name);
    }
    Class<?> result = findClassIgnoringNotFound(name);
    return (result != null) ? result : loadFromParent(name);
  }

  private Class<?> resolveIfNecessary(Class<?> resultClass, boolean resolve) {
    if (resolve) {
      resolveClass(resultClass);
    }
    return (resultClass);
  }

  @Override
  protected void addURL(URL url) {
    // Ignore URLs added by the Tomcat 8 implementation (see gh-919)
    if (log.isTraceEnabled()) {
      log.trace("Ignoring request to add " + url + " to the tomcat classloader");
    }
  }

  private Class<?> loadFromParent(String name) {
    if (this.parent == null) {
      return null;
    }
    try {
      return Class.forName(name, false, this.parent);
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }

  private Class<?> findClassIgnoringNotFound(String name) {
    try {
      return findClass(name);
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }
}
