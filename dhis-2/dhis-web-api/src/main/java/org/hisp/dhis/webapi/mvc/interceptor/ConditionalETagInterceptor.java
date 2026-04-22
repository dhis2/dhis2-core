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
package org.hisp.dhis.webapi.mvc.interceptor;

import static org.hisp.dhis.dml.DmlETagMetrics.ENDPOINT_COMPOSITE;
import static org.hisp.dhis.dml.DmlETagMetrics.ENDPOINT_METADATA;
import static org.hisp.dhis.dml.DmlETagMetrics.ENDPOINT_NAMED_KEY;
import static org.hisp.dhis.dml.DmlETagMetrics.ETAG_CACHE_ENDPOINT_TYPE;
import static org.hisp.dhis.dml.DmlETagMetrics.ETAG_CACHE_REQUESTS;
import static org.hisp.dhis.dml.DmlETagMetrics.RESULT_HIT;
import static org.hisp.dhis.dml.DmlETagMetrics.RESULT_MISS;
import static org.hisp.dhis.dml.DmlETagMetrics.RESULT_SKIP;
import static org.hisp.dhis.dml.DmlETagMetrics.TAG_ENDPOINT_TYPE;
import static org.hisp.dhis.dml.DmlETagMetrics.TAG_RESULT;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.cache.ETagObservedEntityTypes;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datastatistics.DataStatistics;
import org.hisp.dhis.datastatistics.DataStatisticsEvent;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.setting.SystemSetting;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserSetting;
import org.hisp.dhis.userdatastore.UserDatastoreEntry;
import org.hisp.dhis.webapi.service.ConditionalETagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Interceptor that provides automatic conditional ETag caching for metadata API endpoints.
 *
 * <p>Cacheable endpoints are determined by schema lookup: if the URL path maps to a metadata entity
 * type via {@link SchemaService}, the endpoint gets ETag caching based on its observed dependency
 * set.
 *
 * <p>Non-schema endpoints (e.g. {@code /api/me}, {@code /api/configuration}) are handled via a
 * static composite endpoint map that defines their entity-type dependencies.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Component
public class ConditionalETagInterceptor implements HandlerInterceptor {

  private static final String ETAG_ATTR = ConditionalETagInterceptor.class.getName() + ".etag";
  private static final Pattern API_PATH_PATTERN = Pattern.compile("^/api(?:/\\d{2})?(?:/(.*))?$");
  private static final Set<String> FORMAT_EXTENSIONS = Set.of(".json", ".xml", ".csv", ".xls");
  private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

  private static final List<PathPattern> UNCACHED_PATH_PATTERNS =
      compilePathPatterns(List.of("dashboards/search", "dashboards/q", "dashboards/q/*"));

  private static final Map<String, Set<Class<?>>> METADATA_ENDPOINT_OVERRIDES =
      Map.of(
          "visualizations", Set.of(OrganisationUnit.class),
          "maps", Set.of(OrganisationUnit.class, Attribute.class),
          "eventCharts", Set.of(OrganisationUnit.class),
          "eventReports", Set.of(OrganisationUnit.class),
          "eventVisualizations", Set.of(OrganisationUnit.class));

  /** Non-CRUD endpoints mapped to their entity-type dependencies. */
  private static final Map<String, Set<Class<?>>> COMPOSITE_ENDPOINTS =
      Map.ofEntries(
          Map.entry("me", Set.of(OrganisationUnit.class)),
          Map.entry(
              "systemSettings",
              Set.of(SystemSetting.class, DatastoreEntry.class, UserSetting.class)),
          Map.entry(
              "systemSettings/*",
              Set.of(SystemSetting.class, DatastoreEntry.class, UserSetting.class)),
          Map.entry("userSettings", Set.of(UserSetting.class, SystemSetting.class)),
          Map.entry("userSettings/*", Set.of(UserSetting.class, SystemSetting.class)),
          Map.entry("userDataStore/**", Set.of(UserDatastoreEntry.class)),
          Map.entry("me/settings", Set.of(UserSetting.class, SystemSetting.class)),
          Map.entry("me/settings/*", Set.of(UserSetting.class, SystemSetting.class)),
          Map.entry("me/authorization", Set.of(UserRole.class, UserGroup.class)),
          Map.entry("me/authorization/*", Set.of(UserRole.class, UserGroup.class)),
          Map.entry("me/authorities", Set.of(UserRole.class, UserGroup.class)),
          Map.entry("me/authorities/*", Set.of(UserRole.class, UserGroup.class)),
          Map.entry(
              "me/dataApprovalLevels",
              Set.of(DataApprovalLevel.class, OrganisationUnitLevel.class, OrganisationUnit.class)),
          Map.entry(
              "me/dataApprovalWorkflows",
              Set.of(
                  DataApprovalWorkflow.class,
                  DataApprovalLevel.class,
                  OrganisationUnit.class,
                  SystemSetting.class)),
          Map.entry(
              "dimensions",
              Set.of(
                  Category.class,
                  CategoryOptionGroupSet.class,
                  DataElementGroupSet.class,
                  OrganisationUnitGroupSet.class)),
          Map.entry("dimensions/constraints", Set.of(Category.class, CategoryOptionGroupSet.class)),
          Map.entry(
              "dimensions/dataSet/*",
              Set.of(DataSet.class, Category.class, CategoryOptionGroupSet.class)),
          Map.entry("dataStatistics", Set.of(DataStatistics.class)),
          Map.entry(
              "dataStatistics/favorites/*", Set.of(DataStatisticsEvent.class, SystemSetting.class)),
          Map.entry(
              "loginConfig",
              Set.of(SystemSetting.class, DatastoreEntry.class, Configuration.class)),
          Map.entry("dataStore/**", Set.of(DatastoreEntry.class)));

  /**
   * Named-key endpoints whose data is not tied to a single JPA entity type. These depend on a mix
   * of entity types (for user/role-based access) and named version keys (for non-DML events like
   * app install/uninstall).
   *
   * <p>An endpoint with empty entity types AND empty named keys is static per build — it only
   * changes on server restart.
   */
  private static final Map<String, NamedEndpointDeps> NAMED_KEY_ENDPOINTS =
      Map.of(
          "schemas",
          new NamedEndpointDeps(Set.of(), Set.of()),
          "apps",
          new NamedEndpointDeps(Set.of(UserRole.class), Set.of("installedApps")),
          "apps/menu",
          new NamedEndpointDeps(Set.of(UserRole.class), Set.of("installedApps")),
          "apps/**",
          new NamedEndpointDeps(Set.of(UserRole.class), Set.of("installedApps")),
          "staticContent/**",
          new NamedEndpointDeps(Set.of(), Set.of("staticContent")),
          "locales/db",
          new NamedEndpointDeps(Set.of(), Set.of()),
          "locales/ui",
          new NamedEndpointDeps(Set.of(), Set.of()),
          "periodTypes",
          new NamedEndpointDeps(Set.of(), Set.of()));

  private static final List<NamedEndpointPattern> NAMED_KEY_PATH_PATTERNS =
      compileNamedEndpointPatterns(NAMED_KEY_ENDPOINTS);

  private static final List<CompositeEndpointPattern> COMPOSITE_PATH_PATTERNS =
      compileCompositeEndpointPatterns(COMPOSITE_ENDPOINTS);

  private final ConditionalETagService conditionalETagService;
  private final SchemaService schemaService;

  // Metrics counters
  private final Counter cacheHit;
  private final Counter cacheMiss;
  private final Counter cacheSkip;
  private final Counter endpointComposite;
  private final Counter endpointMetadata;
  private final Counter endpointNamedKey;

  @Autowired
  public ConditionalETagInterceptor(
      ConditionalETagService conditionalETagService,
      SchemaService schemaService,
      @Autowired(required = false) MeterRegistry meterRegistry,
      @Autowired(required = false) DhisConfigurationProvider config) {
    this.conditionalETagService = conditionalETagService;
    this.schemaService = schemaService;
    MeterRegistry effectiveRegistry =
        config != null && config.isEnabled(ConfigurationKey.MONITORING_CACHE_ETAG_ENABLED)
            ? meterRegistry
            : null;

    if (effectiveRegistry != null) {
      cacheHit =
          Counter.builder(ETAG_CACHE_REQUESTS)
              .tag(TAG_RESULT, RESULT_HIT)
              .register(effectiveRegistry);
      cacheMiss =
          Counter.builder(ETAG_CACHE_REQUESTS)
              .tag(TAG_RESULT, RESULT_MISS)
              .register(effectiveRegistry);
      cacheSkip =
          Counter.builder(ETAG_CACHE_REQUESTS)
              .tag(TAG_RESULT, RESULT_SKIP)
              .register(effectiveRegistry);
      endpointComposite =
          Counter.builder(ETAG_CACHE_ENDPOINT_TYPE)
              .tag(TAG_ENDPOINT_TYPE, ENDPOINT_COMPOSITE)
              .register(effectiveRegistry);
      endpointMetadata =
          Counter.builder(ETAG_CACHE_ENDPOINT_TYPE)
              .tag(TAG_ENDPOINT_TYPE, ENDPOINT_METADATA)
              .register(effectiveRegistry);
      endpointNamedKey =
          Counter.builder(ETAG_CACHE_ENDPOINT_TYPE)
              .tag(TAG_ENDPOINT_TYPE, ENDPOINT_NAMED_KEY)
              .register(effectiveRegistry);
    } else {
      cacheHit = null;
      cacheMiss = null;
      cacheSkip = null;
      endpointComposite = null;
      endpointMetadata = null;
      endpointNamedKey = null;
    }
  }

  /**
   * Lazily initialized, thread-safe supplier for metadata endpoint types. This is the using the
   * Double-Checked Locking idiom for lazy initialization, from Effective Java Item 83: "Use lazy
   * initialization judiciously" (3rd edition), discussion and latest erreta:
   * https://stackoverflow.com/questions/3578604/how-to-solve-the-double-checked-locking-is-broken-declaration-in-java
   * https://x.com/joshbloch/status/964327677816532992
   *
   * <p>Reason why we are not using @PostContruct here is The SchemaService.getMetadataSchemas()
   * call depends on Hibernate metadata being fully initialized, and there's a known ordering issue
   * — @PostConstruct on an interceptor can fire before Hibernate's SessionFactory has finished
   * scanning all entity mappings. The schema list would be empty or incomplete at that point.
   */
  private final Supplier<Map<String, Set<Class<?>>>> metadataEndpointTypesSupplier =
      new Supplier<>() {
        private volatile Map<String, Set<Class<?>>> value;

        @Override
        public Map<String, Set<Class<?>>> get() {
          Map<String, Set<Class<?>>> result = value;
          if (result == null) {
            synchronized (this) {
              result = value;
              if (result == null) {
                result = buildMetadataEndpointTypes(schemaService.getMetadataSchemas());
                value = result;
                log.info(
                    "ETag interceptor initialized with {} metadata endpoint types", result.size());
              }
            }
          }
          return result;
        }
      };

  @Override
  public boolean preHandle(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull Object handler) {

    if (!conditionalETagService.isEnabled()) {
      if (cacheSkip != null) cacheSkip.increment();
      return true;
    }

    String method = request.getMethod();
    if (!"GET".equals(method) && !"HEAD".equals(method)) {
      if (cacheSkip != null) cacheSkip.increment();
      return true;
    }

    if (!CurrentUserUtil.hasCurrentUser()) {
      if (cacheSkip != null) cacheSkip.increment();
      return true;
    }

    String requestUri = getPathWithinApplication(request);
    String apiRelativePath = extractApiRelativePath(requestUri);

    UserDetails userDetails = CurrentUserUtil.getCurrentUserDetails();

    // Check uncached patterns first — these should never be cached regardless of other matches
    if (matchesAny(apiRelativePath, UNCACHED_PATH_PATTERNS)) {
      if (cacheSkip != null) cacheSkip.increment();
      return true;
    }

    // Check named-key endpoints (non-entity endpoints like schemas, app menu)
    NamedEndpointDeps namedDeps =
        resolveNamedEndpointDeps(apiRelativePath, NAMED_KEY_PATH_PATTERNS);
    if (namedDeps != null) {
      if (endpointNamedKey != null) endpointNamedKey.increment();
      return handleNamedKeyRequest(
          request, response, userDetails, namedDeps.entityTypes(), namedDeps.namedKeys());
    }

    // Check composite endpoint map (non-CRUD endpoints with multi-type dependencies)
    Set<Class<?>> compositeTypes =
        resolveCompositeEndpointTypes(apiRelativePath, COMPOSITE_PATH_PATTERNS);
    if (!compositeTypes.isEmpty()) {
      if (endpointComposite != null) endpointComposite.increment();
      return handleConditionalRequest(request, response, userDetails, compositeTypes);
    }

    String resourceName = extractResourceNameFromApiRelativePath(apiRelativePath);
    if (resourceName == null) {
      if (cacheSkip != null) cacheSkip.increment();
      return true;
    }

    Set<Class<?>> metadataTypes =
        resolveMetadataEndpointTypes(resourceName, metadataEndpointTypesSupplier.get());
    if (metadataTypes.isEmpty()) {
      if (cacheSkip != null) cacheSkip.increment();
      return true;
    }

    if (endpointMetadata != null) endpointMetadata.increment();
    return handleConditionalRequest(request, response, userDetails, metadataTypes);
  }

  /**
   * Extracts the API-relative path after /api/ and optional version prefix. e.g.
   * "/api/41/organisationUnits/abc123" → "organisationUnits/abc123", "/api/me?paging=false" → "me"
   */
  static String extractApiRelativePath(String uri) {
    if (uri == null) {
      return null;
    }

    String path = stripQueryAndFragment(uri);
    var matcher = API_PATH_PATTERN.matcher(path);
    if (!matcher.matches()) {
      return null;
    }

    return normalizeApiRelativePath(matcher.group(1));
  }

  /**
   * Extracts the resource name (first path segment after /api/ and optional version prefix). e.g.
   * "/api/41/organisationUnits/abc123" → "organisationUnits", "/api/me" → "me"
   */
  static String extractResourceName(String uri) {
    return extractResourceNameFromApiRelativePath(extractApiRelativePath(uri));
  }

  static Set<Class<?>> resolveCompositeEndpointTypes(String uri) {
    return resolveCompositeEndpointTypes(extractApiRelativePath(uri), COMPOSITE_PATH_PATTERNS);
  }

  static Map<String, Set<Class<?>>> buildMetadataEndpointTypes(List<Schema> metadataSchemas) {
    LinkedHashMap<String, Set<Class<?>>> metadataEndpointTypes = new LinkedHashMap<>();

    for (Schema schema : metadataSchemas) {
      String resourceName = normalizeApiRelativePath(schema.getPlural());
      if (resourceName == null) {
        continue;
      }

      LinkedHashSet<Class<?>> dependencyTypes = new LinkedHashSet<>();
      addObservedMetadataDependency(dependencyTypes, schema.getKlass());

      for (Class<?> referenceType : schema.getReferences()) {
        if (referenceType == User.class) continue;
        addObservedMetadataDependency(dependencyTypes, referenceType);
      }

      if (schema.isShareable() || schema.isDataShareable()) {
        dependencyTypes.add(UserGroup.class);
      }

      if (schema.hasAttributeValues()) {
        dependencyTypes.add(Attribute.class);
      }

      Set<Class<?>> overrides = METADATA_ENDPOINT_OVERRIDES.get(resourceName);
      if (overrides != null) {
        dependencyTypes.addAll(overrides);
      }

      if (!dependencyTypes.isEmpty()) {
        metadataEndpointTypes.put(resourceName, Set.copyOf(dependencyTypes));
      }
    }

    return Collections.unmodifiableMap(metadataEndpointTypes);
  }

  static Set<Class<?>> resolveMetadataEndpointTypes(
      String resourceName, Map<String, Set<Class<?>>> metadataEndpointTypes) {
    String normalizedResourceName = normalizeApiRelativePath(resourceName);
    if (normalizedResourceName == null) {
      return Set.of();
    }

    return metadataEndpointTypes.getOrDefault(normalizedResourceName, Set.of());
  }

  static Set<Class<?>> resolveCompositeEndpointTypes(
      String uri, Map<String, Set<Class<?>>> compositeEndpoints) {
    return resolveCompositeEndpointTypes(
        extractApiRelativePath(uri), compileCompositeEndpointPatterns(compositeEndpoints));
  }

  static String getStoredETag(HttpServletRequest request) {
    return (String) request.getAttribute(ETAG_ATTR);
  }

  private static String getPathWithinApplication(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();

    if (contextPath != null
        && !contextPath.isEmpty()
        && requestUri != null
        && requestUri.startsWith(contextPath)) {
      return requestUri.substring(contextPath.length());
    }

    return requestUri;
  }

  private static void storeETag(HttpServletRequest request, String currentETag) {
    request.setAttribute(ETAG_ATTR, currentETag);
  }

  private boolean handleConditionalRequest(
      HttpServletRequest request,
      HttpServletResponse response,
      UserDetails userDetails,
      Set<Class<?>> dependencyTypes) {

    String baseETag =
        dependencyTypes.size() == 1
            ? conditionalETagService.generateETag(userDetails, dependencyTypes.iterator().next())
            : conditionalETagService.generateETag(userDetails, dependencyTypes);

    // Include query string in the hash so different parameters produce different ETags.
    String currentETag = hashWithQuery(baseETag, request.getQueryString());

    if (conditionalETagService.checkNotModified(request, currentETag)) {
      log.debug("ETag match for {} - returning 304", request.getRequestURI());
      if (cacheHit != null) cacheHit.increment();
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      conditionalETagService.setETagHeaders(response, currentETag);
      return false;
    }

    if (cacheMiss != null) {
      cacheMiss.increment();
    }

    storeETag(request, currentETag);
    return true;
  }

  private boolean handleNamedKeyRequest(
      HttpServletRequest request,
      HttpServletResponse response,
      UserDetails userDetails,
      Set<Class<?>> entityTypes,
      Set<String> namedKeys) {

    String baseETag = conditionalETagService.generateETag(userDetails, entityTypes, namedKeys);
    String currentETag = hashWithQuery(baseETag, request.getQueryString());

    if (conditionalETagService.checkNotModified(request, currentETag)) {
      log.debug("ETag match for {} - returning 304", request.getRequestURI());
      if (cacheHit != null) cacheHit.increment();
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      conditionalETagService.setETagHeaders(response, currentETag);
      return false;
    }

    if (cacheMiss != null) cacheMiss.increment();
    storeETag(request, currentETag);
    return true;
  }

  private static final ThreadLocal<MessageDigest> SHA256_DIGEST =
      ThreadLocal.withInitial(
          () -> {
            try {
              return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
              throw new IllegalStateException("SHA-256 not available", e);
            }
          });

  /** Combines the base ETag with the query string into a single hash. */
  private static String hashWithQuery(String baseETag, String queryString) {
    if (queryString == null || queryString.isEmpty()) {
      return baseETag;
    }
    MessageDigest digest = SHA256_DIGEST.get();
    digest.reset();
    digest.update(baseETag.getBytes(StandardCharsets.UTF_8));
    digest.update((byte) '?');
    digest.update(queryString.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest.digest());
  }

  private static void addObservedMetadataDependency(
      LinkedHashSet<Class<?>> dependencyTypes, Class<?> dependencyType) {
    if (dependencyType != null && ETagObservedEntityTypes.isObservedType(dependencyType)) {
      dependencyTypes.add(dependencyType);
    }
  }

  private static String extractResourceNameFromApiRelativePath(String apiRelativePath) {
    if (apiRelativePath == null) {
      return null;
    }

    int slashIndex = apiRelativePath.indexOf('/');
    return slashIndex >= 0 ? apiRelativePath.substring(0, slashIndex) : apiRelativePath;
  }

  private static Set<Class<?>> resolveCompositeEndpointTypes(
      String apiRelativePath, List<CompositeEndpointPattern> compositeEndpointPatterns) {
    if (apiRelativePath == null) {
      return Set.of();
    }

    PathContainer pathContainer = PathContainer.parsePath("/" + apiRelativePath);

    for (CompositeEndpointPattern compositeEndpointPattern : compositeEndpointPatterns) {
      if (compositeEndpointPattern.pathPattern().matches(pathContainer)) {
        return compositeEndpointPattern.types();
      }
    }

    return Set.of();
  }

  private static boolean matchesAny(String apiRelativePath, List<PathPattern> patterns) {
    if (apiRelativePath == null) {
      return false;
    }

    PathContainer pathContainer = PathContainer.parsePath("/" + apiRelativePath);

    for (PathPattern pattern : patterns) {
      if (pattern.matches(pathContainer)) {
        return true;
      }
    }

    return false;
  }

  private static List<CompositeEndpointPattern> compileCompositeEndpointPatterns(
      Map<String, Set<Class<?>>> compositeEndpoints) {
    return compositeEndpoints.entrySet().stream()
        .map(
            entry -> {
              String normalizedPattern = normalizeApiRelativePath(entry.getKey());
              if (normalizedPattern == null) {
                throw new IllegalArgumentException(
                    "Composite endpoint pattern must not be blank: " + entry.getKey());
              }

              return new CompositeEndpointPattern(
                  normalizedPattern,
                  PATH_PATTERN_PARSER.parse("/" + normalizedPattern),
                  entry.getValue());
            })
        .sorted(
            Comparator.comparing(
                    CompositeEndpointPattern::pathPattern, PathPattern.SPECIFICITY_COMPARATOR)
                .thenComparing(CompositeEndpointPattern::pattern))
        .toList();
  }

  private static List<PathPattern> compilePathPatterns(List<String> patterns) {
    return patterns.stream().map(pattern -> PATH_PATTERN_PARSER.parse("/" + pattern)).toList();
  }

  private static String stripQueryAndFragment(String path) {
    int queryStart = path.indexOf('?');
    int fragmentStart = path.indexOf('#');
    int endIndex = path.length();

    if (queryStart >= 0) {
      endIndex = queryStart;
    }
    if (fragmentStart >= 0 && fragmentStart < endIndex) {
      endIndex = fragmentStart;
    }

    return path.substring(0, endIndex);
  }

  private static String normalizeApiRelativePath(String path) {
    if (path == null) {
      return null;
    }

    String normalizedPath = stripQueryAndFragment(path).strip();

    while (normalizedPath.startsWith("/")) {
      normalizedPath = normalizedPath.substring(1);
    }

    while (normalizedPath.endsWith("/")) {
      normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
    }

    // Strip format extensions (e.g. me.json → me, organisationUnits.xml → organisationUnits)
    for (String ext : FORMAT_EXTENSIONS) {
      if (normalizedPath.endsWith(ext)) {
        normalizedPath = normalizedPath.substring(0, normalizedPath.length() - ext.length());
        break;
      }
    }

    return normalizedPath.isEmpty() ? null : normalizedPath;
  }

  /** Returns the composite entity types for the given pattern key. Visible for testing. */
  static Set<Class<?>> getCompositeEndpointTypes(String pattern) {
    return COMPOSITE_ENDPOINTS.get(pattern);
  }

  private record CompositeEndpointPattern(
      String pattern, PathPattern pathPattern, Set<Class<?>> types) {}

  record NamedEndpointDeps(Set<Class<?>> entityTypes, Set<String> namedKeys) {}

  private record NamedEndpointPattern(
      String pattern, PathPattern pathPattern, NamedEndpointDeps deps) {}

  private static List<NamedEndpointPattern> compileNamedEndpointPatterns(
      Map<String, NamedEndpointDeps> endpoints) {
    return endpoints.entrySet().stream()
        .map(
            entry -> {
              String normalizedPattern = normalizeApiRelativePath(entry.getKey());
              if (normalizedPattern == null) {
                throw new IllegalArgumentException(
                    "Named endpoint pattern must not be blank: " + entry.getKey());
              }
              return new NamedEndpointPattern(
                  normalizedPattern,
                  PATH_PATTERN_PARSER.parse("/" + normalizedPattern),
                  entry.getValue());
            })
        .sorted(
            Comparator.comparing(
                    NamedEndpointPattern::pathPattern, PathPattern.SPECIFICITY_COMPARATOR)
                .thenComparing(NamedEndpointPattern::pattern))
        .toList();
  }

  private static NamedEndpointDeps resolveNamedEndpointDeps(
      String apiRelativePath, List<NamedEndpointPattern> patterns) {
    if (apiRelativePath == null) {
      return null;
    }
    PathContainer pathContainer = PathContainer.parsePath("/" + apiRelativePath);
    for (NamedEndpointPattern nep : patterns) {
      if (nep.pathPattern().matches(pathContainer)) {
        return nep.deps();
      }
    }
    return null;
  }

  /** Returns the named endpoint deps for the given pattern key. Visible for testing. */
  static NamedEndpointDeps getNamedEndpointDeps(String pattern) {
    return NAMED_KEY_ENDPOINTS.get(pattern);
  }
}
