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

import jakarta.servlet.ServletException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.Setter;
import org.apache.catalina.Container;
import org.apache.catalina.Manager;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.session.ManagerBase;
import org.springframework.util.ClassUtils;

/**
 * This code is a modified version of the original code from Spring Boot project.
 *
 * <p>Tomcat {@link StandardContext} initialization.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class TomcatEmbeddedContext extends StandardContext {

  @Setter private MimeMappings mimeMappings;

  private TomcatStarter starter;

  @Override
  public boolean loadOnStartup(Container[] children) {
    // deferred until later (see deferredLoadOnStartup)
    return true;
  }

  @Override
  public void setManager(Manager manager) {
    if (manager instanceof ManagerBase) {
      manager.setSessionIdGenerator(new LazySessionIdGenerator());
    }
    super.setManager(manager);
  }

  void deferredLoadOnStartup() {
    doWithThreadContextClassLoader(
        getLoader().getClassLoader(),
        () -> getLoadOnStartupWrappers(findChildren()).forEach(this::load));
  }

  private Stream<Wrapper> getLoadOnStartupWrappers(Container[] children) {
    Map<Integer, List<Wrapper>> grouped = new TreeMap<>();
    for (Container child : children) {
      Wrapper wrapper = (Wrapper) child;
      int order = wrapper.getLoadOnStartup();
      if (order >= 0) {
        grouped.computeIfAbsent(order, o -> new ArrayList<>()).add(wrapper);
      }
    }
    return grouped.values().stream().flatMap(List::stream);
  }

  private void load(Wrapper wrapper) {
    try {
      wrapper.load();
    } catch (ServletException ex) {
      String message =
          sm.getString("standardContext.loadOnStartup.loadException", getName(), wrapper.getName());
      if (getComputedFailCtxIfServletStartFails()) {
        throw new WebServerException(message, ex);
      }
      getLogger().error(message, StandardWrapper.getRootCause(ex));
    }
  }

  /**
   * Some older Servlet frameworks (e.g. Struts, BIRT) use the Thread context class loader to create
   * servlet instances in this phase. If they do that and then try to initialize them later the
   * class loader may have changed, so wrap the call to loadOnStartup in what we think is going to
   * be the main webapp classloader at runtime.
   *
   * @param classLoader the class loader to use
   * @param code the code to run
   */
  private void doWithThreadContextClassLoader(ClassLoader classLoader, Runnable code) {
    ClassLoader existingLoader =
        (classLoader != null) ? ClassUtils.overrideThreadContextClassLoader(classLoader) : null;
    try {
      code.run();
    } finally {
      if (existingLoader != null) {
        ClassUtils.overrideThreadContextClassLoader(existingLoader);
      }
    }
  }

  void setStarter(TomcatStarter starter) {
    this.starter = starter;
  }

  TomcatStarter getStarter() {
    return this.starter;
  }

  @Override
  public String[] findMimeMappings() {
    List<String> mappings = new ArrayList<>();
    mappings.addAll(Arrays.asList(super.findMimeMappings()));
    if (this.mimeMappings != null) {
      this.mimeMappings.forEach((mapping) -> mappings.add(mapping.getExtension()));
    }
    return mappings.toArray(String[]::new);
  }

  @Override
  public String findMimeMapping(String extension) {
    String mimeMapping = super.findMimeMapping(extension);
    if (mimeMapping != null) {
      return mimeMapping;
    }
    return (this.mimeMappings != null) ? this.mimeMappings.get(extension) : null;
  }
}
