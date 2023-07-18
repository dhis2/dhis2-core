/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.i18n.ui.resourcebundle;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.comparator.LocaleNameComparator;
import org.hisp.dhis.commons.util.PathUtils;
import org.hisp.dhis.i18n.locale.LocaleManager;

/**
 * @author Torgeir Lorange Ostby
 * @author Pham Thi Thuy
 * @author Nguyen Dang Quang
 */
@Slf4j
public class DefaultResourceBundleManager implements ResourceBundleManager {
  private static final String EXT_RESOURCE_BUNDLE = ".properties";

  private static final String GLOBAL_RESOURCE_BUNDLE_NAME = "i18n_global";

  private static final String SPECIFIC_RESOURCE_BUNDLE_NAME = "i18n_module";

  // -------------------------------------------------------------------------
  // Configuration
  // -------------------------------------------------------------------------

  public DefaultResourceBundleManager() {}

  // -------------------------------------------------------------------------
  // ResourceBundleManager implementation
  // -------------------------------------------------------------------------

  @Override
  public ResourceBundle getSpecificResourceBundle(String clazzName, Locale locale) {
    String path = PathUtils.getClassPath(clazzName);

    for (String dir = path; dir != null; dir = PathUtils.getParent(dir)) {
      String baseName = PathUtils.addChild(dir, SPECIFIC_RESOURCE_BUNDLE_NAME);

      try {
        return ResourceBundle.getBundle(baseName, locale);
      } catch (MissingResourceException ignored) {
      }
    }

    return null;
  }

  @Override
  public ResourceBundle getGlobalResourceBundle(Locale locale)
      throws ResourceBundleManagerException {
    try {
      return ResourceBundle.getBundle(GLOBAL_RESOURCE_BUNDLE_NAME, locale);
    } catch (MissingResourceException e) {
      throw new ResourceBundleManagerException("Failed to get global resource bundle");
    }
  }

  @Override
  public List<Locale> getAvailableLocales() throws ResourceBundleManagerException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    URL url = classLoader.getResource(GLOBAL_RESOURCE_BUNDLE_NAME + EXT_RESOURCE_BUNDLE);

    if (url == null) {
      throw new ResourceBundleManagerException("Failed to find global resource bundle");
    }

    List<Locale> locales;

    if (url.toExternalForm().startsWith("jar:")) {
      locales = new ArrayList<>(getAvailableLocalesFromJar(url));
    } else {
      String dirPath = new File(url.getFile()).getParent();

      locales = new ArrayList<>(getAvailableLocalesFromDir(dirPath));
    }

    locales.sort(LocaleNameComparator.INSTANCE);

    return locales;
  }

  private Collection<Locale> getAvailableLocalesFromJar(URL url)
      throws ResourceBundleManagerException {
    JarFile jar;

    Set<Locale> availableLocales = new HashSet<>();

    try {
      JarURLConnection connection = (JarURLConnection) url.openConnection();

      jar = connection.getJarFile();

      Enumeration<JarEntry> e = jar.entries();

      while (e.hasMoreElements()) {
        JarEntry entry = e.nextElement();

        String name = entry.getName();

        if (name.startsWith(GLOBAL_RESOURCE_BUNDLE_NAME) && name.endsWith(EXT_RESOURCE_BUNDLE)) {
          availableLocales.add(getLocaleFromName(name));
        }
      }
    } catch (IOException e) {
      throw new ResourceBundleManagerException("Failed to get jar file: " + url, e);
    }

    return availableLocales;
  }

  private Collection<Locale> getAvailableLocalesFromDir(String dirPath) {
    dirPath = convertURLToFilePath(dirPath);

    File dir = new File(dirPath);
    Set<Locale> availableLocales = new HashSet<>();

    File[] files =
        dir.listFiles(
            (dir1, name) ->
                name.startsWith(GLOBAL_RESOURCE_BUNDLE_NAME) && name.endsWith(EXT_RESOURCE_BUNDLE));

    if (files != null) {
      for (File file : files) {
        availableLocales.add(getLocaleFromName(file.getName()));
      }
    }

    return availableLocales;
  }

  /**
   * Retrieves a {@link Locale} from the given resource bundle name. The resource bundle naming
   * follows the Java locale format:
   *
   * <pre>
   * [2-3 letter language]_[2 letter country/region]_[variant]
   * </pre>
   *
   * @param name the resource bundle name.
   * @return a {@link Locale}.
   */
  private Locale getLocaleFromName(String name) {
    Pattern pattern =
        Pattern.compile(
            "^"
                + GLOBAL_RESOURCE_BUNDLE_NAME
                + "(?:_([a-z]{2,3})(?:_([A-Z]{2})(?:_(.+))?)?)?"
                + EXT_RESOURCE_BUNDLE
                + "$");

    Matcher matcher = pattern.matcher(name);

    if (matcher.matches()) {
      if (matcher.group(1) != null) {
        if (matcher.group(2) != null) {
          if (matcher.group(3) != null) {
            return new Locale(matcher.group(1), matcher.group(2), matcher.group(3));
          }

          return new Locale(matcher.group(1), matcher.group(2));
        }

        return new Locale(matcher.group(1));
      }
    }

    return LocaleManager.DEFAULT_LOCALE;
  }

  // -------------------------------------------------------------------------
  // Support method
  // -------------------------------------------------------------------------

  private String convertURLToFilePath(String url) {
    try {
      url = URLDecoder.decode(url, "iso-8859-1");
    } catch (Exception ex) {
      log.error("Failed to convert URL", ex);
    }

    return url;
  }
}
