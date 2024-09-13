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
package org.hisp.dhis.webapi.openapi;

import static java.util.stream.Collectors.toSet;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Response.Status;
import org.hisp.dhis.jsontree.JsonObject;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Maturity.Beta
@OpenApi.Document(domain = OpenApi.class)
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class OpenApiController {

  private static final String APPLICATION_X_YAML = "application/x-yaml";

  private final ApplicationContext context;

  private static SoftReference<Api> fullApi;
  private static SoftReference<String> fullHtml;

  private static synchronized void updateFullHtml(String html) {
    fullHtml = new SoftReference<>(html);
  }

  private static synchronized void updateFullApi(Api api) {
    fullApi = new SoftReference<>(api);
  }

  /**
   * As long as the server is the same the response will be for the same request, so we just create
   * a constant that will have another value after a restart.
   */
  private static final String E_TAG = "v" + System.currentTimeMillis();

  @Data
  @OpenApi.Shared
  public static class OpenApiScopeParams {
    @OpenApi.Description("When specified only operations matching the path are included.")
    Set<String> path = Set.of();

    @OpenApi.Description("When specified only operations matching the domain are included.")
    Set<String> domain = Set.of();

    @OpenApi.Ignore
    boolean isCachable() {
      return path.isEmpty() && domain.isEmpty();
    }
  }

  @Data
  @OpenApi.Shared
  public static class OpenApiGenerationParams {
    @OpenApi.Description(
        """
      Regenerate the response even if a cached response is available.
      Mainly used during development to see effects of hot-swap code changes.""")
    boolean skipCache = false;

    @OpenApi.Description(
        """
      Shared schema names must be unique.
      To ensure this either a unique name is picked (`false`) or the generation fails (`true`)""")
    boolean failOnNameClash = false;

    @OpenApi.Description(
        """
      Declarations can be logically inconsistent; for example a parameter that is both required and has a default value.
      Either this is ignored and only logs a warning (`false`) or the generation fails (`true`)""")
    boolean failOnInconsistency = false;

    @OpenApi.Description(
        """
      Annotations that narrow the rendered type to one of the super-types of the type declared will be ignored
      and as a consequence types occur fully expanded (with all their properties).
      Note that this does not affect types that are re-defined as a different type using annotations.""")
    @Maturity.Beta
    @OpenApi.Since(42)
    boolean expandedRefs = false;

    @OpenApi.Ignore
    boolean isCachable() {
      return !skipCache && !expandedRefs;
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
      OpenApiScopeParams scope,
      OpenApiRenderer.OpenApiRenderingParams rendering,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (notModified(request, response, generation)) return;

    boolean cached = scope.isCachable() && generation.isCachable();
    if (cached && fullHtml != null) {
      String fullHtml = OpenApiController.fullHtml.get();
      if (fullHtml != null) {
        getWriter(response, TEXT_HTML_VALUE).get().write(fullHtml);
        return;
      }
    }

    StringWriter json = new StringWriter();
    writeDocument(
        request, generation, scope, () -> new PrintWriter(json), OpenApiGenerator::generateJson);

    String html = OpenApiRenderer.render(json.toString(), rendering);
    if (cached) updateFullHtml(html);
    getWriter(response, TEXT_HTML_VALUE).get().write(html);
  }

  @OpenApi.Description(
      """
    The HTML to browse (view) the DHIS2 API specification based on OpenAPI JSON for the path""")
  @OpenApi.Response(String.class)
  @GetMapping(value = "/{path}/openapi.html", produces = TEXT_HTML_VALUE)
  public void getPathOpenApiHtml(
      @PathVariable String path,
      OpenApiGenerationParams generation,
      OpenApiRenderer.OpenApiRenderingParams rendering,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (notModified(request, response, generation)) return;

    StringWriter json = new StringWriter();
    OpenApiScopeParams scope = new OpenApiScopeParams();
    scope.setPath(Set.of("/" + path));
    writeDocument(
        request, generation, scope, () -> new PrintWriter(json), OpenApiGenerator::generateJson);

    getWriter(response, TEXT_HTML_VALUE)
        .get()
        .write(OpenApiRenderer.render(json.toString(), rendering));
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

    OpenApiScopeParams scope = new OpenApiScopeParams();
    scope.setPath(Set.of("/" + path));
    writeDocument(
        request, generation, scope, getYamlWriter(response), OpenApiGenerator::generateYaml);
  }

  @OpenApi.Response(String.class)
  @GetMapping(
      value = {"/openapi.yaml", "/openapi/openapi.yaml"},
      produces = APPLICATION_X_YAML)
  public void getOpenApiYaml(
      OpenApiGenerationParams generation,
      OpenApiScopeParams scope,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (notModified(request, response, generation)) return;

    writeDocument(
        request, generation, scope, getYamlWriter(response), OpenApiGenerator::generateYaml);
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

    OpenApiScopeParams scope = new OpenApiScopeParams();
    scope.setPath(Set.of("/" + path));
    writeDocument(
        request, generation, scope, getJsonWriter(response), OpenApiGenerator::generateJson);
  }

  @OpenApi.Response(JsonObject.class)
  @GetMapping(
      value = {"/openapi.json", "/openapi/openapi.json"},
      produces = APPLICATION_JSON_VALUE)
  public void getOpenApiJson(
      OpenApiGenerationParams generation,
      OpenApiScopeParams scope,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (notModified(request, response, generation)) return;

    writeDocument(
        request, generation, scope, getJsonWriter(response), OpenApiGenerator::generateJson);
  }

  private Supplier<PrintWriter> getYamlWriter(HttpServletResponse response) {
    return getWriter(response, APPLICATION_X_YAML);
  }

  private Supplier<PrintWriter> getJsonWriter(HttpServletResponse response) {
    return getWriter(response, APPLICATION_JSON_VALUE);
  }

  private Supplier<PrintWriter> getWriter(HttpServletResponse response, String contentType) {
    return () -> {
      response.setContentType(contentType);
      response.setHeader("ETag", E_TAG);
      try {
        return response.getWriter();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    };
  }

  private void writeDocument(
      HttpServletRequest request,
      OpenApiGenerationParams params,
      OpenApiScopeParams scope,
      Supplier<PrintWriter> getWriter,
      BiFunction<Api, String, String> writer) {

    Api api = getApiCached(params, scope);

    getWriter.get().write(writer.apply(api, getServerUrl(request)));
  }

  @Nonnull
  private Api getApiCached(OpenApiGenerationParams generation, OpenApiScopeParams scope) {
    if (scope.isCachable() && generation.isCachable()) {
      Api api = fullApi == null ? null : fullApi.get();
      if (api == null) {
        api = getApiUncached(generation, scope);
        updateFullApi(api);
      }
      return api;
    }
    return getApiUncached(generation, scope);
  }

  @Nonnull
  private Api getApiUncached(OpenApiGenerationParams generation, OpenApiScopeParams scope) {
    Api api =
        ApiExtractor.extractApi(
            new ApiExtractor.Configuration(
                new ApiExtractor.Scope(getAllControllerClasses(), scope.path, scope.domain),
                generation.expandedRefs));
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
  private static String getServerUrl(HttpServletRequest request) {
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
