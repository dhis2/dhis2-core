/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.web.tomcat;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;

/**
 * This code is a modified version of the original code from Spring Boot project.
 *
 * <p>Simple server-independent abstraction for mime mappings. Roughly equivalent to the {@literal
 * &lt;mime-mapping&gt;} element traditionally found in web.xml.
 *
 * @author Phillip Webb
 * @author Guirong Hu
 * @since 2.0.0
 */
public sealed class MimeMappings implements Iterable<MimeMappings.Mapping> {

  /** Default mime mapping commonly used. */
  public static final MimeMappings DEFAULT = new DefaultMimeMappings();

  private final Map<String, Mapping> map;

  /** Create a new empty {@link MimeMappings} instance. */
  public MimeMappings() {
    this.map = new LinkedHashMap<>();
  }

  /**
   * Create a new {@link MimeMappings} instance from the specified mappings.
   *
   * @param mappings the source mappings
   */
  public MimeMappings(MimeMappings mappings) {
    this(mappings, true);
  }

  /**
   * Create a new {@link MimeMappings} from the specified mappings.
   *
   * @param mappings the source mappings with extension as the key and mime-type as the value
   */
  public MimeMappings(Map<String, String> mappings) {
    Assert.notNull(mappings, "Mappings must not be null");
    this.map = new LinkedHashMap<>();
    mappings.forEach(this::add);
  }

  /**
   * Internal constructor.
   *
   * @param mappings source mappings
   * @param mutable if the new object should be mutable.
   */
  MimeMappings(MimeMappings mappings, boolean mutable) {
    Assert.notNull(mappings, "Mappings must not be null");
    this.map =
        (mutable ? new LinkedHashMap<>(mappings.map) : Collections.unmodifiableMap(mappings.map));
  }

  /**
   * Add a new mime mapping.
   *
   * @param extension the file extension (excluding '.')
   * @param mimeType the mime type to map
   * @return any previous mapping or {@code null}
   */
  public String add(String extension, String mimeType) {
    Assert.notNull(extension, "Extension must not be null");
    Assert.notNull(mimeType, "MimeType must not be null");
    Mapping previous =
        this.map.put(extension.toLowerCase(Locale.ENGLISH), new Mapping(extension, mimeType));
    return (previous != null) ? previous.getMimeType() : null;
  }

  /**
   * Remove an existing mapping.
   *
   * @param extension the file extension (excluding '.')
   * @return the removed mime mapping or {@code null} if no item was removed
   */
  public String remove(String extension) {
    Assert.notNull(extension, "Extension must not be null");
    Mapping previous = this.map.remove(extension.toLowerCase(Locale.ENGLISH));
    return (previous != null) ? previous.getMimeType() : null;
  }

  /**
   * Get a mime mapping for the given extension.
   *
   * @param extension the file extension (excluding '.')
   * @return a mime mapping or {@code null}
   */
  public String get(String extension) {
    Assert.notNull(extension, "Extension must not be null");
    Mapping mapping = this.map.get(extension.toLowerCase(Locale.ENGLISH));
    return (mapping != null) ? mapping.getMimeType() : null;
  }

  /**
   * Returns all defined mappings.
   *
   * @return the mappings.
   */
  public Collection<Mapping> getAll() {
    return this.map.values();
  }

  @Override
  public final Iterator<Mapping> iterator() {
    return getAll().iterator();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj instanceof MimeMappings other) {
      return getMap().equals(other.map);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getMap().hashCode();
  }

  Map<String, Mapping> getMap() {
    return this.map;
  }

  /**
   * Create a new unmodifiable view of the specified mapping. Methods that attempt to modify the
   * returned map will throw {@link UnsupportedOperationException}s.
   *
   * @param mappings the mappings
   * @return an unmodifiable view of the specified mappings.
   */
  public static MimeMappings unmodifiableMappings(MimeMappings mappings) {
    Assert.notNull(mappings, "Mappings must not be null");
    return new MimeMappings(mappings, false);
  }

  /**
   * Create a new lazy copy of the given mappings that will only copy entries if the mappings are
   * mutated.
   *
   * @param mappings the source mappings
   * @return a new mappings instance
   * @since 3.0.0
   */
  public static MimeMappings lazyCopy(MimeMappings mappings) {
    Assert.notNull(mappings, "Mappings must not be null");
    return new LazyMimeMappingsCopy(mappings);
  }

  /** A single mime mapping. */
  public static final class Mapping {

    private final String extension;

    private final String mimeType;

    public Mapping(String extension, String mimeType) {
      Assert.notNull(extension, "Extension must not be null");
      Assert.notNull(mimeType, "MimeType must not be null");
      this.extension = extension;
      this.mimeType = mimeType;
    }

    public String getExtension() {
      return this.extension;
    }

    public String getMimeType() {
      return this.mimeType;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      if (obj instanceof Mapping other) {
        return this.extension.equals(other.extension) && this.mimeType.equals(other.mimeType);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.extension.hashCode();
    }

    @Override
    public String toString() {
      return "Mapping [extension=" + this.extension + ", mimeType=" + this.mimeType + "]";
    }
  }

  /**
   * {@link MimeMappings} implementation used for {@link MimeMappings#DEFAULT}. Provides in-memory
   * access for common mappings and lazily loads the complete set when necessary.
   */
  static final class DefaultMimeMappings extends MimeMappings {

    static final String MIME_MAPPINGS_PROPERTIES = "mime-mappings.properties";

    private static final MimeMappings COMMON;

    static {
      MimeMappings mappings = new MimeMappings();
      mappings.add("avi", "video/x-msvideo");
      mappings.add("bin", "application/octet-stream");
      mappings.add("body", "text/html");
      mappings.add("class", "application/java");
      mappings.add("css", "text/css");
      mappings.add("dtd", "application/xml-dtd");
      mappings.add("gif", "image/gif");
      mappings.add("gtar", "application/x-gtar");
      mappings.add("gz", "application/x-gzip");
      mappings.add("htm", "text/html");
      mappings.add("html", "text/html");
      mappings.add("jar", "application/java-archive");
      mappings.add("java", "text/x-java-source");
      mappings.add("jnlp", "application/x-java-jnlp-file");
      mappings.add("jpe", "image/jpeg");
      mappings.add("jpeg", "image/jpeg");
      mappings.add("jpg", "image/jpeg");
      mappings.add("js", "text/javascript");
      mappings.add("json", "application/json");
      mappings.add("otf", "font/otf");
      mappings.add("pdf", "application/pdf");
      mappings.add("png", "image/png");
      mappings.add("ps", "application/postscript");
      mappings.add("tar", "application/x-tar");
      mappings.add("tif", "image/tiff");
      mappings.add("tiff", "image/tiff");
      mappings.add("ttf", "font/ttf");
      mappings.add("txt", "text/plain");
      mappings.add("xht", "application/xhtml+xml");
      mappings.add("xhtml", "application/xhtml+xml");
      mappings.add("xls", "application/vnd.ms-excel");
      mappings.add("xml", "application/xml");
      mappings.add("xsl", "application/xml");
      mappings.add("xslt", "application/xslt+xml");
      mappings.add("wasm", "application/wasm");
      mappings.add("zip", "application/zip");
      COMMON = unmodifiableMappings(mappings);
    }

    private volatile Map<String, Mapping> loaded;

    DefaultMimeMappings() {
      super(new MimeMappings(), false);
    }

    @Override
    public Collection<Mapping> getAll() {
      return load().values();
    }

    @Override
    public String get(String extension) {
      Assert.notNull(extension, "Extension must not be null");
      extension = extension.toLowerCase(Locale.ENGLISH);
      Map<String, Mapping> loaded = this.loaded;
      if (loaded != null) {
        return get(loaded, extension);
      }
      String commonMimeType = COMMON.get(extension);
      if (commonMimeType != null) {
        return commonMimeType;
      }
      loaded = load();
      return get(loaded, extension);
    }

    private String get(Map<String, Mapping> mappings, String extension) {
      Mapping mapping = mappings.get(extension);
      return (mapping != null) ? mapping.getMimeType() : null;
    }

    @Override
    Map<String, Mapping> getMap() {
      return load();
    }

    private Map<String, Mapping> load() {
      Map<String, Mapping> loaded = this.loaded;
      if (loaded != null) {
        return loaded;
      }
      try {
        loaded = new LinkedHashMap<>();
        for (Entry<?, ?> entry :
            PropertiesLoaderUtils.loadProperties(
                    new ClassPathResource(MIME_MAPPINGS_PROPERTIES, getClass()))
                .entrySet()) {
          loaded.put(
              (String) entry.getKey(),
              new Mapping((String) entry.getKey(), (String) entry.getValue()));
        }
        loaded = Collections.unmodifiableMap(loaded);
        this.loaded = loaded;
        return loaded;
      } catch (IOException ex) {
        throw new IllegalArgumentException("Unable to load the default MIME types", ex);
      }
    }
  }

  /**
   * {@link MimeMappings} implementation used to create a lazy copy only when the mappings are
   * mutated.
   */
  static final class LazyMimeMappingsCopy extends MimeMappings {

    private final MimeMappings source;

    private final AtomicBoolean copied = new AtomicBoolean();

    LazyMimeMappingsCopy(MimeMappings source) {
      this.source = source;
    }

    @Override
    public String add(String extension, String mimeType) {
      copyIfNecessary();
      return super.add(extension, mimeType);
    }

    @Override
    public String remove(String extension) {
      copyIfNecessary();
      return super.remove(extension);
    }

    private void copyIfNecessary() {
      if (this.copied.compareAndSet(false, true)) {
        this.source.forEach((mapping) -> add(mapping.getExtension(), mapping.getMimeType()));
      }
    }

    @Override
    public String get(String extension) {
      return !this.copied.get() ? this.source.get(extension) : super.get(extension);
    }

    @Override
    public Collection<Mapping> getAll() {
      return !this.copied.get() ? this.source.getAll() : super.getAll();
    }

    @Override
    Map<String, Mapping> getMap() {
      return !this.copied.get() ? this.source.getMap() : super.getMap();
    }
  }
}
