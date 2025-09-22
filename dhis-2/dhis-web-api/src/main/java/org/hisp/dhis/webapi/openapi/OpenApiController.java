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
package org.hisp.dhis.webapi.openapi;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.webapi.openapi.OpenApiRenderer.renderHTML;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hisp.dhis.cache.SoftCache;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Response.Status;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.webapi.openapi.JsonGenerator.Format;
import org.hisp.dhis.webapi.openapi.JsonGenerator.Language;
import org.hisp.dhis.webapi.openapi.OpenApiGenerator.Info;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Maturity.Beta
@OpenApi.Document(entity = OpenApi.class)
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class OpenApiController {

  private static final String APPLICATION_X_YAML = "application/x-yaml";

  private final ApplicationContext context;

  private static final SoftCache<Api> API_CACHE = new SoftCache<>();
  private static final SoftCache<String> JSON_CACHE = new SoftCache<>();
  private static final SoftCache<String> HTML_CACHE = new SoftCache<>();

  /**
   * As long as the server is the same the response will be for the same request, so we just create
   * a constant that will have another value after a restart.
   */
  private static final String E_TAG = "v" + System.currentTimeMillis();

  @Data
  @Accessors(chain = true)
  @OpenApi.Shared
  public static class OpenApiScopingParams {
    @OpenApi.Description(
        "When given only operations in controllers matching the scope are considered")
    Set<String> scope = Set.of();

    @OpenApi.Ignore
    String getCacheKey() {
      return String.join("+", scope) + ":";
    }
  }

  /*
   * HTML
   */

  @OpenApi.Since(42)
  @OpenApi.Description(
      """
    The HTML to browse (view) the DHIS2 API specification based on OpenAPI JSON
    either in its entirety or scoped to the path(s) or domain(s) requested.""")
  @OpenApi.Response(status = Status.OK, value = String.class)
  @GetMapping(
      value = {"/openapi.html", "/openapi/openapi.html"},
      produces = TEXT_HTML_VALUE)
  public void getOpenApiHtml(
      OpenApiGenerationParams generation,
      OpenApiScopingParams scoping,
      OpenApiRenderingParams rendering,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (notModified(request, response, generation)) return;

    // for HTML X-properties must be included
    generation.setIncludeXProperties(true);

    // auto-set context path
    rendering.setContextPath(request.getContextPath());

    getHtmlWriter(response).write(renderCached(scoping, generation, rendering));
  }

  @OpenApi.Description(
      """
    The HTML to browse (view) the DHIS2 API specification based on OpenAPI JSON for the path""")
  @OpenApi.Response(String.class)
  @GetMapping(value = "/{path}/openapi.html", produces = TEXT_HTML_VALUE)
  public void getPathOpenApiHtml(
      @PathVariable String path,
      OpenApiGenerationParams generation,
      OpenApiRenderingParams rendering,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (notModified(request, response, generation)) return;

    // for HTML X-properties must be included
    generation.setIncludeXProperties(true);

    // auto-set context path
    rendering.setContextPath(request.getContextPath());

    OpenApiScopingParams scope = new OpenApiScopingParams().setScope(Set.of("path:/api/" + path));
    getHtmlWriter(response).write(renderCached(scope, generation, rendering));
  }

  /*
   * YAML
   */

  @OpenApi.Response(String.class)
  @GetMapping(value = "/{path}/openapi.yaml", produces = APPLICATION_X_YAML)
  public void getPathOpenApiYaml(
      @PathVariable String path,
      OpenApiGenerationParams generation,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (notModified(request, response, generation)) return;

    OpenApiScopingParams scope = new OpenApiScopingParams().setScope(Set.of("path:/api/" + path));
    getYamlWriter(response).write(generateCached(Language.YAML, scope, generation));
  }

  @OpenApi.Response(String.class)
  @GetMapping(
      value = {"/openapi.yaml", "/openapi/openapi.yaml"},
      produces = APPLICATION_X_YAML)
  public void getOpenApiYaml(
      OpenApiGenerationParams generation,
      OpenApiScopingParams scoping,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (notModified(request, response, generation)) return;

    getYamlWriter(response).write(generateCached(Language.YAML, scoping, generation));
  }

  /*
   * JSON
   */

  @OpenApi.Response(OpenApiObject.class)
  @GetMapping(value = "/{path}/openapi.json", produces = APPLICATION_JSON_VALUE)
  public void getPathOpenApiJson(
      @PathVariable String path,
      OpenApiGenerationParams generation,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (notModified(request, response, generation)) return;

    OpenApiScopingParams scope = new OpenApiScopingParams().setScope(Set.of("path:/api/" + path));
    getJsonWriter(response).write(generateCached(Language.JSON, scope, generation));
  }

  @OpenApi.Response(JsonObject.class)
  @GetMapping(
      value = {"/openapi.json", "/openapi/openapi.json"},
      produces = APPLICATION_JSON_VALUE)
  public void getOpenApiJson(
      OpenApiGenerationParams generation,
      OpenApiScopingParams scoping,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (notModified(request, response, generation)) return;

    getJsonWriter(response).write(generateCached(Language.JSON, scoping, generation));
  }

  private PrintWriter getHtmlWriter(HttpServletResponse response) {
    return getWriter(response, TEXT_HTML_VALUE);
  }

  private PrintWriter getYamlWriter(HttpServletResponse response) {
    return getWriter(response, APPLICATION_X_YAML);
  }

  private PrintWriter getJsonWriter(HttpServletResponse response) {
    return getWriter(response, APPLICATION_JSON_VALUE);
  }

  private PrintWriter getWriter(HttpServletResponse response, String contentType) {
    response.setContentType(contentType);
    response.setHeader("ETag", E_TAG);
    try {
      return response.getWriter();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Nonnull
  private String renderCached(
      OpenApiScopingParams scoping,
      OpenApiGenerationParams generation,
      OpenApiRenderingParams rendering) {
    String cacheKey =
        generation.isSkipCache()
            ? null
            : scoping.getCacheKey() + generation.getDocumentCacheKey() + rendering.getCacheKey();
    return HTML_CACHE.get(
        cacheKey,
        () -> {
          String json = generateCached(Language.JSON, scoping, generation);
          Api partial = extractCached(scoping, generation);
          Api full = extractCached(new OpenApiScopingParams(), new OpenApiGenerationParams());
          ApiStatistics stats = new ApiStatistics(full, partial);
          return renderHTML(json, rendering, stats);
        });
  }

  @Nonnull
  private String generateCached(
      Language language, OpenApiScopingParams scoping, OpenApiGenerationParams params) {
    String cacheKey =
        params.isSkipCache() ? null : scoping.getCacheKey() + params.getDocumentCacheKey();
    if (cacheKey != null) cacheKey += "-" + (language == Language.JSON ? "json" : "yaml");
    Api api = extractCached(scoping, params);
    Info info = Info.DEFAULT.toBuilder().serverUrl(getServerUrl()).build();
    return JSON_CACHE.get(
        cacheKey,
        () -> OpenApiGenerator.generate(language, api, Format.PRETTY_PRINT, info, params));
  }

  @Nonnull
  private Api extractCached(OpenApiScopingParams scoping, OpenApiGenerationParams generation) {
    String apiCacheKey =
        generation.isSkipCache() ? null : scoping.getCacheKey() + generation.getApiCacheKey();
    return API_CACHE.get(apiCacheKey, () -> extractUncached(scoping, generation));
  }

  @Nonnull
  private Api extractUncached(OpenApiScopingParams scoping, OpenApiGenerationParams generation) {
    Map<String, Set<String>> filters = new HashMap<>();
    for (String s : scoping.scope) {
      int split = s.indexOf(':');
      if (split > 0) {
        String key = s.substring(0, split);
        String value = s.substring(split + 1);
        filters.computeIfAbsent(key, k -> new HashSet<>()).add(value);
      }
    }
    Set<Class<?>> controllers = getAllControllerClasses();
    Api.Scope scope =
        new Api.Scope(controllers, filters, ApiClassifications.matches(controllers, filters));
    Api api =
        ApiExtractor.extractApi(scope, new ApiExtractor.Configuration(generation.expandedRefs));
    ApiIntegrator.integrateApi(
        api,
        ApiIntegrator.Configuration.builder()
            .failOnNameClash(generation.failOnNameClash)
            .failOnInconsistency(generation.failOnInconsistency)
            .build());
    return api;
  }

  private Set<Class<?>> getAllControllerClasses() {
    return Stream.concat(
            context.getBeansWithAnnotation(RestController.class).values().stream(),
            context.getBeansWithAnnotation(Controller.class).values().stream())
        .map(Object::getClass)
        .map(OpenApiController::deProxyClass)
        .collect(toSet());
  }

  /** In case the bean class is a spring-enhanced proxy this resolves the source class. */
  private static Class<?> deProxyClass(Class<?> c) {
    return !c.isAnnotationPresent(RestController.class) && !c.isAnnotationPresent(Controller.class)
        ? c.getSuperclass()
        : c;
  }

  /**
   * This has to work with 3 types of URLs
   *
   * <pre>
   *     http://localhost/openapi.json
   *     http://localhost:8080/api/openapi.json
   *     https://play.dhis2.org/dev/api/openapi.json
   * </pre>
   *
   * And any of the variants when it comes to the path the controller allows to query an OpenAPI
   * document.
   */
  private static String getServerUrl() {
    RequestAttributes requestAttributes =
        requireNonNull(RequestContextHolder.getRequestAttributes());
    HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
    StringBuffer url = request.getRequestURL();
    String servletPath = request.getServletPath();
    servletPath = servletPath.substring(servletPath.indexOf("/api") + 1);
    int apiStart = url.indexOf("/api/");
    String root =
        apiStart < 0 ? url.substring(0, url.indexOf("/", 10)) : url.substring(0, apiStart);
    return root + "/" + servletPath;
  }

  private static boolean notModified(
      HttpServletRequest request,
      HttpServletResponse response,
      OpenApiGenerationParams generation) {
    String etag = request.getHeader("If-None-Match");
    if (generation.isSkipCache()) return false;
    if (E_TAG.equals(etag)) {
      response.setStatus(304);
      return true;
    }
    return false;
  }
}
