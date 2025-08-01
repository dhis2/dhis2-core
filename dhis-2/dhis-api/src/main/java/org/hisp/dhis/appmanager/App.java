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
package org.hisp.dhis.appmanager;

import static java.util.stream.Collectors.toUnmodifiableSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.datastore.DatastoreNamespace;

/**
 * @author Saptarshi
 */
@JacksonXmlRootElement(localName = "app", namespace = DxfNamespaces.DXF_2_0)
public class App implements Serializable {
  /** Determines if a de-serialized file is compatible with this class. */
  private static final long serialVersionUID = -6638197841892194228L;

  public static final String SEE_APP_AUTHORITY_PREFIX = "M_";

  /** Required. */
  private String version;

  private String name;

  private String displayName;

  private String displayDescription;

  private AppType appType = AppType.APP;

  private String basePath;

  private String launchPath;

  private String pluginLaunchPath;

  private String[] installsAllowedFrom;

  private String defaultLocale;

  private AppStorageSource appStorageSource;

  private String folderName;

  /** Optional. */
  private String shortName;

  private String description;

  private String appHubId;

  private AppIcons icons;

  private AppDeveloper developer;

  private String locales;

  private AppActivities activities = new AppActivities();

  private String launchUrl;

  private String pluginLaunchUrl;

  private String pluginType;

  private String baseUrl;

  private Set<String> authorities = new HashSet<>();

  private AppSettings settings;

  private boolean coreApp = false;

  private boolean bundled = false;

  /** Generated. */
  private AppStatus appState = AppStatus.OK;

  private List<AppShortcut> shortcuts = new ArrayList<>();

  private boolean isLocalised = false;

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /**
   * Initializes the app. Sets the launchUrl property.
   *
   * @param contextPath the context path of this instance.
   */
  public void init(@CheckForNull String contextPath) {
    String prefix =
        this.isBundled() ? AppManager.BUNDLED_APP_PREFIX : AppManager.INSTALLED_APP_PREFIX;
    this.basePath = ("/" + prefix + getUrlFriendlyName()).replaceAll("/+", "/");
    this.baseUrl = contextPath + basePath;

    if (contextPath != null && name != null && launchPath != null) {
      this.launchUrl = String.join("/", baseUrl, launchPath.replaceFirst("^/+", ""));
    }

    if (contextPath != null && name != null && pluginLaunchPath != null) {
      this.pluginLaunchUrl = String.join("/", baseUrl, pluginLaunchPath.replaceFirst("^/+", ""));
    }
  }

  /**
   * A mapper used for App serialisation and de-serialisation A mapper is often created and
   * configured when reading from the app manifests during app install and discovery. This provides
   * one such mapper for convenience and consistency.
   */
  public static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
    return this.bundled;
  }

  public void setBundled(boolean bundled) {
    this.bundled = bundled;
  }

  /** Determine if the app is configured as a coreApp (to be served at the root namespace) */
  @JsonProperty("core_app")
  @JacksonXmlProperty(localName = "core_app", namespace = DxfNamespaces.DXF_2_0)
  public boolean isCoreApp() {
    return coreApp;
  }

  public void setCoreApp(boolean coreApp) {
    this.coreApp = coreApp;
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @JsonProperty("app_hub_id")
  @JacksonXmlProperty(localName = "app_hub_id", namespace = DxfNamespaces.DXF_2_0)
  public String getAppHubId() {
    return appHubId;
  }

  public void setAppHubId(String appHubId) {
    this.appHubId = appHubId;
  }

  @JsonProperty("short_name")
  @JacksonXmlProperty(localName = "short_name", namespace = DxfNamespaces.DXF_2_0)
  public String getShortName() {
    if (shortName == null) {
      return name;
    }
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDisplayName() {
    if (displayName == null || displayName.isEmpty()) {
      return name;
    }
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppType getAppType() {
    return appType;
  }

  public void setAppType(AppType appType) {
    this.appType = appType;
  }

  @JsonProperty("launch_path")
  @JacksonXmlProperty(localName = "launch_path", namespace = DxfNamespaces.DXF_2_0)
  public String getLaunchPath() {
    return launchPath;
  }

  public void setLaunchPath(String launchPath) {
    this.launchPath = launchPath;
  }

  @JsonProperty("plugin_launch_path")
  @JacksonXmlProperty(localName = "plugin_launch_path", namespace = DxfNamespaces.DXF_2_0)
  public String getPluginLaunchPath() {
    return pluginLaunchPath;
  }

  public void setPluginLaunchPath(String pluginLaunchPath) {
    this.pluginLaunchPath = pluginLaunchPath;
  }

  @JsonProperty("plugin_type")
  @JacksonXmlProperty(localName = "plugin_type", namespace = DxfNamespaces.DXF_2_0)
  public String getPluginType() {
    return pluginType;
  }

  public void setPluginType(String pluginType) {
    this.pluginType = pluginType;
  }

  @JsonProperty("installs_allowed_from")
  @JacksonXmlProperty(localName = "installs_allowed_from", namespace = DxfNamespaces.DXF_2_0)
  public String[] getInstallsAllowedFrom() {
    return installsAllowedFrom;
  }

  public void setInstallsAllowedFrom(String[] installsAllowedFrom) {
    this.installsAllowedFrom = installsAllowedFrom;
  }

  @JsonProperty("default_locale")
  @JacksonXmlProperty(localName = "default_locale", namespace = DxfNamespaces.DXF_2_0)
  public String getDefaultLocale() {
    return defaultLocale;
  }

  public void setDefaultLocale(String defaultLocale) {
    this.defaultLocale = defaultLocale;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDisplayDescription() {
    if (displayDescription == null || displayDescription.isEmpty()) {
      return description;
    }
    return displayDescription;
  }

  public void setDisplayDescription(String displayDescription) {
    this.displayDescription = displayDescription;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppDeveloper getDeveloper() {
    return developer;
  }

  public void setDeveloper(AppDeveloper developer) {
    this.developer = developer;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppIcons getIcons() {
    return icons;
  }

  public void setIcons(AppIcons icons) {
    this.icons = icons;
  }

  @JsonIgnore
  public String getLocales() {
    return locales;
  }

  public void setLocales(String locales) {
    this.locales = locales;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Nonnull
  public AppActivities getActivities() {
    return activities;
  }

  public void setActivities(@Nonnull AppActivities activities) {
    this.activities = activities;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getFolderName() {
    return folderName;
  }

  public void setFolderName(String folderName) {
    this.folderName = folderName;
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

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
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

  public void setAppStorageSource(AppStorageSource appStorageSource) {
    this.appStorageSource = appStorageSource;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Set<String> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<String> authorities) {
    this.authorities = authorities;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppStatus getAppState() {
    return appState;
  }

  public void setAppState(AppStatus appState) {
    this.appState = appState;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public AppSettings getSettings() {
    return settings;
  }

  public void setSettings(AppSettings settings) {
    this.settings = settings;
  }

  public void setShortcuts(List<AppShortcut> shortcuts) {
    this.shortcuts = shortcuts;
  }

  public boolean getIsLocalised() {
    return this.isLocalised;
  }

  private void setIsLocalised(boolean localised) {
    this.isLocalised = localised;
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

  private transient List<AppManifestTranslation> manifestTranslations = new ArrayList<>();

  public void setManifestTranslations(List<AppManifestTranslation> translations) {
    if (translations == null) {
      return;
    }

    manifestTranslations = translations;
  }

  private boolean hasManifestTranslations(AppManifestTranslation manifestTranslations) {
    return !manifestTranslations.getShortcuts().isEmpty()
        || !StringUtils.isEmpty(manifestTranslations.getTitle());
  }

  public App localise(Locale userLocale) {
    App localisedApp = SerializationUtils.clone(this);
    var translationsToUse = getTranslationToUse(userLocale);
    if (hasManifestTranslations(translationsToUse)) {
      translateShortcuts(localisedApp, translationsToUse);

      translateAppInfo(localisedApp, translationsToUse);

      localisedApp.setIsLocalised(true);
      return localisedApp;
    }
    return localisedApp;
  }

  private static void translateShortcuts(
      App localisedApp, AppManifestTranslation translationsToUse) {
    for (AppShortcut shortcut : localisedApp.shortcuts) {
      var shortcutTranslation = translationsToUse.getShortcuts().get(shortcut.getName());
      if (shortcutTranslation != null) {
        shortcut.setDisplayName(shortcutTranslation);
      }
    }
  }

  private static void translateAppInfo(App localisedApp, AppManifestTranslation translationsToUse) {
    if (!StringUtils.isEmpty(translationsToUse.getTitle())) {
      localisedApp.setDisplayName(translationsToUse.getTitle());
    }
    if (!StringUtils.isEmpty(translationsToUse.getDescription())) {
      localisedApp.setDisplayDescription(translationsToUse.getDescription());
    }
  }

  private AppManifestTranslation getTranslationToUse(Locale locale) {
    String language = locale.getLanguage();
    String country = locale.getCountry();
    String script = locale.getScript();

    AppManifestTranslation matchingLocale = getMatchingLocale(language, country, script);
    AppManifestTranslation matchingLanguage = getMatchingLanguage(language);

    matchingLocale.merge(matchingLanguage);

    return matchingLocale;
  }

  private AppManifestTranslation getMatchingLanguage(String language) {
    return manifestTranslations.stream()
        .filter(tf -> language.equals(tf.getLanguageCode()))
        .findFirst()
        .orElse(new AppManifestTranslation());
  }

  private AppManifestTranslation getMatchingLocale(String language, String country, String script) {
    return manifestTranslations.stream()
        .filter(
            translation ->
                language.equals(translation.getLanguageCode())
                    && country.equals(translation.getCountryCode())
                    && (script.isEmpty() || script.equals(translation.getScriptCode())))
        .findFirst()
        .orElse(new AppManifestTranslation());
  }
}
