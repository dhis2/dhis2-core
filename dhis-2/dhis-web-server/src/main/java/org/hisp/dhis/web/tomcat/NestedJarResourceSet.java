/*
 * Copyright (c) 2004-2024, University of Oslo
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
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.webresources.AbstractSingleArchiveResourceSet;
import org.apache.catalina.webresources.JarResource;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * This code is a modified version of the original code from Spring Boot project.
 *
 * <p>A {@link WebResourceSet} for a resource in a nested JAR.
 *
 * @author Phillip Webb
 */
class NestedJarResourceSet extends AbstractSingleArchiveResourceSet {

  private static final Name MULTI_RELEASE = new Name("Multi-Release");

  private final URL url;

  private JarFile archive = null;

  private long archiveUseCount = 0;

  private boolean useCaches;

  private volatile Boolean multiRelease;

  NestedJarResourceSet(URL url, WebResourceRoot root, String webAppMount, String internalPath)
      throws IllegalArgumentException {
    this.url = url;
    setRoot(root);
    setWebAppMount(webAppMount);
    setInternalPath(internalPath);
    setStaticOnly(true);
    if (getRoot().getState().isAvailable()) {
      try {
        start();
      } catch (LifecycleException ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

  @Override
  protected WebResource createArchiveResource(
      JarEntry jarEntry, String webAppPath, Manifest manifest) {
    return new JarResource(this, webAppPath, getBaseUrlString(), jarEntry);
  }

  @Override
  protected void initInternal() throws LifecycleException {
    try {
      JarURLConnection connection = connect();
      try {
        setManifest(connection.getManifest());
        setBaseUrl(connection.getJarFileURL());
      } finally {
        if (!connection.getUseCaches()) {
          connection.getJarFile().close();
        }
      }
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  protected JarFile openJarFile() throws IOException {
    synchronized (this.archiveLock) {
      if (this.archive == null) {
        JarURLConnection connection = connect();
        this.useCaches = connection.getUseCaches();
        this.archive = connection.getJarFile();
      }
      this.archiveUseCount++;
      return this.archive;
    }
  }

  @Override
  protected void closeJarFile() {
    synchronized (this.archiveLock) {
      this.archiveUseCount--;
    }
  }

  @Override
  protected boolean isMultiRelease() {
    if (this.multiRelease == null) {
      synchronized (this.archiveLock) {
        if (this.multiRelease == null) {
          // JarFile.isMultiRelease() is final so we must go to the manifest
          Manifest manifest = getManifest();
          Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
          this.multiRelease = (attributes != null) && attributes.containsKey(MULTI_RELEASE);
        }
      }
    }
    return this.multiRelease.booleanValue();
  }

  @Override
  public void gc() {
    synchronized (this.archiveLock) {
      if (this.archive != null && this.archiveUseCount == 0) {
        try {
          if (!this.useCaches) {
            this.archive.close();
          }
        } catch (IOException ex) {
          // Ignore
        }
        this.archive = null;
        this.archiveEntries = null;
      }
    }
  }

  private JarURLConnection connect() throws IOException {
    URLConnection connection = this.url.openConnection();
    ResourceUtils.useCachesIfNecessary(connection);
    Assert.state(
        connection instanceof JarURLConnection,
        () -> "URL '%s' did not return a JAR connection".formatted(this.url));
    connection.connect();
    return (JarURLConnection) connection;
  }
}
