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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Executor;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.Tomcat.FixContextListener;
import org.apache.catalina.util.LifecycleBase;
import org.apache.catalina.webresources.AbstractResourceSet;
import org.apache.catalina.webresources.EmptyResource;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

/**
 * This code is a modified version of the original code from Spring Boot project. It serves as the
 * main entry point for the embedded server. It starts an embedded Tomcat server
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class Main {

  private static final Integer DEFAULT_HTTP_PORT = 8080;

  private static int getPort() {
    return Integer.parseInt(
        ObjectUtils.firstNonNull(
            System.getProperty("server.port"),
            System.getenv("SERVER_PORT"),
            Integer.toString(DEFAULT_HTTP_PORT)));
  }

  private static String getContextPath() {
    return ObjectUtils.firstNonNull(
        System.getProperty("server.servlet.context.path"),
        System.getenv("SERVER_SERVLET_CONTEXT_PATH"),
        "");
  }

  private static String getSameSite() {
    return ObjectUtils.firstNonNull(
            System.getProperty("same.site.cookies"),
            System.getenv("SAME_SITE_COOKIES"),
            "Strict");
  }

  public static void main(String[] args) throws Exception {
    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(createTempDir());
    int port = getPort();
    tomcat.setPort(port);

    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setThrowOnFailure(true);
    tomcat.getService().addConnector(connector);
    connector.setPort(port);
    connector.setProperty("relaxedQueryChars", "\\ { } | [ ]");
    tomcat.setConnector(connector);
    registerConnectorExecutor(tomcat, connector);

    Host host = tomcat.getHost();
    host.setAutoDeploy(false);

    TomcatEmbeddedContext context = new TomcatEmbeddedContext();
    TomcatStarter starter = new TomcatStarter();
    context.setStarter(starter);
    context.addServletContainerInitializer(starter, Collections.emptySet());
    context.setName("/");
    context.setDisplayName("/");
    context.setPath(getContextPath());
    context.setMimeMappings(MimeMappings.lazyCopy(MimeMappings.DEFAULT));

    Rfc6265CookieProcessor cookieProcessor = new Rfc6265CookieProcessor();
    //cookieProcessor.setSameSiteCookies(getSameSite());
    cookieProcessor.setSameSiteCookies("None");
    context.setCookieProcessor(cookieProcessor);

    context.setResources(new LoaderHidingResourceRoot(context));
    context.addLifecycleListener(new FixContextListener());
    ClassLoader parentClassLoader = Main.class.getClassLoader();
    context.setParentClassLoader(parentClassLoader);
    configureTldPatterns(context);
    WebappLoader loader = new WebappLoader();
    loader.setLoaderInstance(new TomcatEmbeddedWebappClassLoader(parentClassLoader));
    loader.setDelegate(true);
    context.setLoader(loader);

    addDefaultServlet(context);

    context.addLifecycleListener(new StaticResourceConfigurer(context));

    Thread.currentThread().setContextClassLoader(context.getParentClassLoader());

    host.addChild(context);

    tomcat.start();

    Thread awaitThread =
        new Thread("container-" + (1)) {
          @Override
          public void run() {
            performDeferredLoadOnStartup(tomcat);
            tomcat.getServer().await();
          }
        };
    awaitThread.setContextClassLoader(Main.class.getClassLoader());
    awaitThread.setDaemon(false);
    awaitThread.start();
  }

  private static void configureTldPatterns(TomcatEmbeddedContext context) {
    StandardJarScanFilter filter = new StandardJarScanFilter();
    filter.setTldSkip(
        StringUtils.collectionToCommaDelimitedString(
            new LinkedHashSet<>(TldPatterns.DEFAULT_SKIP)));
    filter.setTldScan(
        StringUtils.collectionToCommaDelimitedString(
            new LinkedHashSet<>(TldPatterns.DEFAULT_SCAN)));
    context.getJarScanner().setJarScanFilter(filter);
  }

  private static void performDeferredLoadOnStartup(Tomcat tomcat) {
    try {
      for (Container child : tomcat.getHost().findChildren()) {
        if (child instanceof TomcatEmbeddedContext embeddedContext) {
          embeddedContext.deferredLoadOnStartup();
        }
      }
    } catch (Exception ex) {
      if (ex instanceof WebServerException webServerException) {
        throw webServerException;
      }
      throw new WebServerException("Unable to start embedded Tomcat connectors", ex);
    }
  }

  private static void registerConnectorExecutor(Tomcat tomcat, Connector connector) {
    if (connector.getProtocolHandler().getExecutor() instanceof Executor executor) {
      tomcat.getService().addExecutor(executor);
    }
  }

  private static void addDefaultServlet(Context context) {
    Wrapper defaultServlet = context.createWrapper();
    defaultServlet.setName("default");
    defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
    defaultServlet.addInitParameter("debug", "0");
    defaultServlet.addInitParameter("listings", "false");
    defaultServlet.setLoadOnStartup(1);
    // Otherwise the default location of a Spring DispatcherServlet cannot be set
    defaultServlet.setOverridable(true);
    context.addChild(defaultServlet);
    context.addServletMappingDecoded("/", "default");
  }

  private static String createTempDir() {
    try {
      File tempDir = File.createTempFile("tomcat.", "." + CodeGenerator.generateCode(8));
      tempDir.delete();
      tempDir.mkdir();
      tempDir.deleteOnExit();
      return tempDir.getAbsolutePath();
    } catch (IOException ex) {
      throw new RuntimeException(
          "Unable to create tempDir. java.io.tmpdir is set to "
              + System.getProperty("java.io.tmpdir"),
          ex);
    }
  }

  private static final class LoaderHidingResourceRoot extends StandardRoot {
    private LoaderHidingResourceRoot(TomcatEmbeddedContext context) {
      super(context);
    }

    @Override
    protected WebResourceSet createMainResourceSet() {
      return new LoaderHidingWebResourceSet(super.createMainResourceSet());
    }
  }

  private static final class LoaderHidingWebResourceSet extends AbstractResourceSet {

    private final WebResourceSet delegate;

    private final Method initInternal;

    private LoaderHidingWebResourceSet(WebResourceSet delegate) {
      this.delegate = delegate;
      try {
        this.initInternal = LifecycleBase.class.getDeclaredMethod("initInternal");
        this.initInternal.setAccessible(true);
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }

    @Override
    public WebResource getResource(String path) {
      if (path.startsWith("/org/springframework/boot")) {
        return new EmptyResource(getRoot(), path);
      }
      return this.delegate.getResource(path);
    }

    @Override
    public String[] list(String path) {
      return this.delegate.list(path);
    }

    @Override
    public Set<String> listWebAppPaths(String path) {
      return this.delegate.listWebAppPaths(path).stream()
          .filter(webAppPath -> !webAppPath.startsWith("/org/springframework/boot"))
          .collect(Collectors.toSet());
    }

    @Override
    public boolean mkdir(String path) {
      return this.delegate.mkdir(path);
    }

    @Override
    public boolean write(String path, InputStream is, boolean overwrite) {
      return this.delegate.write(path, is, overwrite);
    }

    @Override
    public URL getBaseUrl() {
      return this.delegate.getBaseUrl();
    }

    @Override
    public void setReadOnly(boolean readOnly) {
      this.delegate.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() {
      return this.delegate.isReadOnly();
    }

    @Override
    public void gc() {
      this.delegate.gc();
    }

    @Override
    public void setAllowLinking(boolean b) {
      // no
    }

    @Override
    public boolean getAllowLinking() {
      return false;
    }

    @Override
    protected void initInternal() throws LifecycleException {
      if (this.delegate instanceof LifecycleBase) {
        try {
          ReflectionUtils.invokeMethod(this.initInternal, this.delegate);
        } catch (Exception ex) {
          throw new LifecycleException(ex);
        }
      }
    }
  }

  protected static final List<URL> getUrlsOfJarsWithMetaInfResources() {
    return staticResourceJars.getUrls();
  }

  private static final StaticResourceJars staticResourceJars = new StaticResourceJars();

  private static final class StaticResourceConfigurer implements LifecycleListener {

    private static final String WEB_APP_MOUNT = "/";

    private static final String INTERNAL_PATH = "/META-INF/resources";

    private final Context context;

    private StaticResourceConfigurer(Context context) {
      this.context = context;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
      if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
        addResourceJars(getUrlsOfJarsWithMetaInfResources());
      }
    }

    private void addResourceJars(List<URL> resourceJarUrls) {
      for (URL url : resourceJarUrls) {
        String path = url.getPath();
        if (path.endsWith(".jar") || path.endsWith(".jar!/")) {
          String jar = url.toString();
          if (!jar.startsWith("jar:")) {
            // A jar file in the file system. Convert to Jar URL.
            jar = "jar:" + jar + "!/";
          }
          addResourceSet(jar);
        } else {
          addResourceSet(url.toString());
        }
      }
    }

    private void addResourceSet(String resource) {
      try {
        if (isInsideClassicNestedJar(resource)) {
          addClassicNestedResourceSet(resource);
          return;
        }
        WebResourceRoot root = this.context.getResources();
        URL url = new URL(resource);
        if (isInsideNestedJar(resource)) {
          root.addJarResources(new NestedJarResourceSet(url, root, WEB_APP_MOUNT, INTERNAL_PATH));
        } else {
          root.createWebResourceSet(
              ResourceSetType.RESOURCE_JAR, WEB_APP_MOUNT, url, INTERNAL_PATH);
        }
      } catch (Exception ex) {
        // Ignore (probably not a directory)
      }
    }

    private void addClassicNestedResourceSet(String resource) throws MalformedURLException {
      // It's a nested jar but we now don't want the suffix because Tomcat
      // is going to try and locate it as a root URL (not the resource
      // inside it)
      URL url = new URL(resource.substring(0, resource.length() - 2));
      this.context
          .getResources()
          .createWebResourceSet(ResourceSetType.RESOURCE_JAR, WEB_APP_MOUNT, url, INTERNAL_PATH);
    }

    private boolean isInsideClassicNestedJar(String resource) {
      return !isInsideNestedJar(resource) && resource.indexOf("!/") < resource.lastIndexOf("!/");
    }

    private boolean isInsideNestedJar(String resource) {
      return resource.startsWith("jar:nested:");
    }
  }
}
