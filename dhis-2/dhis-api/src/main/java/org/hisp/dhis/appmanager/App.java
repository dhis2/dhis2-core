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
package org.hisp.dhis.appmanager;

import static java.util.stream.Collectors.toUnmodifiableSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.*;
import javax.annotation.Nonnull;
import lombok.Setter;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.datastore.DatastoreNamespace;
import org.hisp.dhis.util.ObjectUtils;

/**
 * @author Saptarshi
 */
@JacksonXmlRootElement(localName = "app", namespace = DxfNamespaces.DXF_2_0)
public class App implements Serializable {
  /** Determines if a de-serialized file is compatible with this class. */
  private static final long serialVersionUID = -6638197841892194228L;

  public static final String SEE_APP_AUTHORITY_PREFIX = "M_";

  /** Required. */
  @Setter private String version;

  @Setter private String name;

  @Setter private AppType appType = AppType.APP;

  private String basePath;

  @Setter private String launchPath;

  @Setter private String pluginLaunchPath;

  @Setter private String[] installsAllowedFrom;

  @Setter private String defaultLocale;

  @Setter private AppStorageSource appStorageSource;

  @Setter private String folderName;

  /** Optional. */
  @Setter private String shortName;

  @Setter private String description;

  @Setter private String appHubId;

  @Setter private AppIcons icons;

  @Setter private AppDeveloper developer;

  @Setter private String locales;

  @Setter private AppActivities activities;

  private String launchUrl;

  private String pluginLaunchUrl;

  @Setter private String pluginType;

  @Setter private String baseUrl;

  @Setter private Set<String> authorities = new HashSet<>();

  @Setter private AppSettings settings;

  @Setter private boolean coreApp = false;

  /** Generated. */
  @Setter private AppStatus appState = AppStatus.OK;

  @Setter private List<AppShortcut> shortcuts = new ArrayList<>();

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /**
   * Initializes the app. Sets the launchUrl property.
   *
   * @param contextPath the context path of this instance.
   */
  public void init(String contextPath) {
    String appPathPrefix =
        isBundled() ? AppManager.BUNDLED_APP_PREFIX : AppManager.INSTALLED_APP_PREFIX;

    this.basePath = ("/" + appPathPrefix + getUrlFriendlyName()).replaceAll("/+", "/");
    this.baseUrl = contextPath + basePath;

    if (contextPath != null && name != null && launchPath != null) {
      this.launchUrl = String.join("/", baseUrl, launchPath.replaceFirst("^/+", ""));
    }

    if (contextPath != null && name != null && pluginLaunchPath != null) {
      this.pluginLaunchUrl = String.join("/", baseUrl, pluginLaunchPath.replaceFirst("^/+", ""));
    }
  }

  /** Unique identifier for the app. Is based on app-name */
  @JsonProperty
  public String getKey() {
    return getUrlFriendlyName();
  }

  @JsonProperty
  public String getBasePath() {
    return this.basePath;
  }

  /** Determine if this app will overload a bundled app */
  @JsonProperty
  public boolean isBundled() {
    return AppManager.BUNDLED_APPS.contains(getShortName());
  }

  /** Determine if the app is configured as a coreApp (to be served at the root namespace) */
  @JsonProperty("core_app")
  @JacksonXmlProperty(localName = "core_app", namespace = DxfNamespaces.DXF_2_0)
  public boolean isCoreApp() {
    return coreApp;
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getVersion() {
    return version;
  }

  @JsonProperty("app_hub_id")
  @JacksonXmlProperty(localName = "app_hub_id", namespace = DxfNamespaces.DXF_2_0)
  public String getAppHubId() {
    return appHubId;
  }

  @JsonProperty("short_name")
  @JacksonXmlProperty(localName = "short_name", namespace = DxfNamespaces.DXF_2_0)
  public String getShortName() {
    if (shortName == null) {
      return name;
    }
    return shortName;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getName() {
    return name;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppType getAppType() {
    return appType;
  }

  @JsonProperty("launch_path")
  @JacksonXmlProperty(localName = "launch_path", namespace = DxfNamespaces.DXF_2_0)
  public String getLaunchPath() {
    return launchPath;
  }

  @JsonProperty("plugin_launch_path")
  @JacksonXmlProperty(localName = "plugin_launch_path", namespace = DxfNamespaces.DXF_2_0)
  public String getPluginLaunchPath() {
    return pluginLaunchPath;
  }

  @JsonProperty("plugin_type")
  @JacksonXmlProperty(localName = "plugin_type", namespace = DxfNamespaces.DXF_2_0)
  public String getPluginType() {
    return pluginType;
  }

  @JsonProperty("installs_allowed_from")
  @JacksonXmlProperty(localName = "installs_allowed_from", namespace = DxfNamespaces.DXF_2_0)
  public String[] getInstallsAllowedFrom() {
    return installsAllowedFrom;
  }

  @JsonProperty("default_locale")
  @JacksonXmlProperty(localName = "default_locale", namespace = DxfNamespaces.DXF_2_0)
  public String getDefaultLocale() {
    return defaultLocale;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppDeveloper getDeveloper() {
    return developer;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppIcons getIcons() {
    return icons;
  }

  @JsonIgnore
  public String getLocales() {
    return locales;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppActivities getActivities() {
    return activities;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFolderName() {
    return folderName;
  }

  @JsonProperty
  public String getLaunchUrl() {
    return launchUrl;
  }

  @JsonProperty
  public String getPluginLaunchUrl() {
    return pluginLaunchUrl;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getBaseUrl() {
    return baseUrl;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppStorageSource getAppStorageSource() {
    return appStorageSource;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public List<AppShortcut> getShortcuts() {
    return shortcuts;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Set<String> getAuthorities() {
    return authorities;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppStatus getAppState() {
    return appState;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppSettings getSettings() {
    return settings;
  }

  private List<AppManifestTranslation> manifestTranslations = new ArrayList<>();

  public void setManifestTranslations(
      List<AppManifestTranslation> translations, String userLocale) {
    if (translations == null) {
      return;
    }

    manifestTranslations = translations;

    if (!manifestTranslations.isEmpty()) {
      for (AppShortcut shortcut : this.shortcuts) {
        shortcut.setDisplayName(getTranslations(userLocale, shortcut.getName()));
      }
    }
  }

  private String getTranslations(String locale, String shortcutName) {
    String language = locale.split("[-_]")[0];
    Optional<AppManifestTranslation> matchingLocale =
        manifestTranslations.stream()
            .filter(tf -> Objects.equals(tf.getLocale().toLowerCase(), locale.toLowerCase()))
            .findFirst();

    Optional<AppManifestTranslation> matchingLanguage =
        manifestTranslations.stream()
            .filter(tf -> Objects.equals(tf.getLocale(), language))
            .findFirst();

    if (matchingLocale.isEmpty() && matchingLanguage.isEmpty()) {
      return shortcutName;
    }

    String key = "SHORTCUT_" + shortcutName;

    String result = null;
    if (matchingLocale.isPresent()) {
      result = matchingLocale.get().getTranslations().get(key);
    }

    if (result == null && matchingLanguage.isPresent()) {
      result = matchingLanguage.get().getTranslations().get(key);
    }

    return ObjectUtils.firstNonNull(result, shortcutName);
  }

  // -------------------------------------------------------------------------
  // hashCode, equals, toString
  // -------------------------------------------------------------------------

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 79 * hash + (this.getShortName() != null ? this.getShortName().hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    final App other = (App) obj;

    return this.getShortName() == null
        ? other.getShortName() == null
        : this.getShortName().equals(other.getShortName());
  }

  @Override
  public String toString() {
    return "{"
        + "\"version:\""
        + version
        + "\", "
        + "\"name:\""
        + name
        + "\", "
        + "\"shortName:\""
        + getShortName()
        + "\", "
        + "\"key:\""
        + getKey()
        + "\", "
        + "\"basePath:\""
        + getBasePath()
        + "\", "
        + "\"appType:\""
        + appType
        + "\", "
        + "\"baseUrl:\""
        + baseUrl
        + "\", "
        + "\"launchPath:\""
        + launchPath
        + "\" "
        + "\"pluginLaunchPath:\""
        + pluginLaunchPath
        + "\" "
        + "}";
  }

  public String getUrlFriendlyName() {
    if (getShortName() != null) {
      return getShortName().trim().replaceAll("[^A-Za-z0-9\\s-]", "").replaceAll("\\s+", "-");
    }

    return null;
  }

  public String getSeeAppAuthority() {
    if (isBundled()) {
      return SEE_APP_AUTHORITY_PREFIX + AppManager.BUNDLED_APP_PREFIX + getShortName();
    }

    return SEE_APP_AUTHORITY_PREFIX
        + getShortName().trim().replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "_");
  }

  public Boolean hasAppEntrypoint() {
    return (this.appType == AppType.APP) && (this.launchPath != null);
  }

  public Boolean hasPluginEntrypoint() {
    return (this.appType == AppType.APP) && (this.pluginLaunchPath != null);
  }

  @JsonIgnore
  @Nonnull
  public Set<String> getNamespaces() {
    AppDhis dhis = getActivities().getDhis();
    String namespace = dhis.getNamespace();
    List<DatastoreNamespace> additionalNamespaces = dhis.getAdditionalNamespaces();
    if (namespace == null && additionalNamespaces == null) return Set.of();
    if (namespace == null)
      return additionalNamespaces.stream()
          .map(DatastoreNamespace::getNamespace)
          .filter(Objects::nonNull)
          .collect(toUnmodifiableSet());
    if (additionalNamespaces == null || additionalNamespaces.isEmpty()) return Set.of(namespace);
    Set<String> namespaces = new HashSet<>();
    namespaces.add(namespace);
    additionalNamespaces.forEach(ns -> namespaces.add(ns.getNamespace()));
    return namespaces;
  }

  @JsonIgnore
  @Nonnull
  public Set<String> getAdditionalAuthorities() {
    AppDhis dhis = getActivities().getDhis();
    List<DatastoreNamespace> additionalNamespaces = dhis.getAdditionalNamespaces();
    if (additionalNamespaces == null || additionalNamespaces.isEmpty()) return Set.of();
    return additionalNamespaces.stream()
        .flatMap(ns -> ns.getAllAuthorities().stream())
        .collect(toUnmodifiableSet());
  }
}
