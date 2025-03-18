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

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.webapi.openapi.JsonGenerator.Format.PRETTY_PRINT;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.openapi.JsonGenerator.Language;

/**
 * A classic command line application to generate DHIS2 OpenAPI documents.
 *
 * <p>The application is also provided as {@link ToolProvider} to be more accessible in CI build
 * chains.
 *
 * <p>Options:
 *
 * <pre>
 *     [--group]
 *     [--print]
 *     [--scope domain...]
 * </pre>
 *
 * @author Jan Bernitt
 */
public class OpenApiTool implements ToolProvider {

  public static void main(String[] args) {
    int errorCode = new OpenApiTool().run(System.out, System.err, args);
    if (errorCode != 0) System.exit(errorCode);
  }

  @Override
  public String name() {
    return "openapi";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length == 0) {
      out.println("Usage: [<options>] [<path or tag>...] <output-file-or-dir>");
      out.println("--group (flag)");
      out.println("  generate multiple files where controllers are grouped by tag");
      return -1;
    }
    Set<Class<?>> controllers = findControllerClasses(err);
    if (controllers == null) return -1;
    if (controllers.isEmpty()) {
      err.println("Controller classes need to be compiled first");
      return -1;
    }
    Map<String, Set<String>> filters = new HashMap<>();
    boolean group = false;
    for (int i = 0; i < args.length - 1; i++) {
      String arg = args[i];
      if (arg.startsWith("--")) {
        switch (arg) {
          case "--group":
            group = true;
            break;
          default:
            err.println("Unknown option: " + arg);
        }
      } else if (arg.startsWith("/")) {
        filters.computeIfAbsent("path", k -> new HashSet<>()).add(arg);
      } else {
        filters.computeIfAbsent("entity", k -> new HashSet<>()).add(arg);
      }
    }
    out.println("Generated Documents");
    String filename = args[args.length - 1];

    Api.Scope scope =
        new Api.Scope(controllers, filters, ApiClassifications.matches(controllers, filters));
    if (!group) {
      return generateDocument(filename, out, err, scope);
    }
    AtomicInteger errorCode = generateDocumentsFromDocumentAnnotation(filename, out, err, scope);
    return errorCode.get() < 0 ? -1 : 0;
  }

  @CheckForNull
  private static Set<Class<?>> findControllerClasses(PrintWriter err) {
    String root =
        AbstractCrudController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
    try (Stream<Path> files = Files.walk(Path.of(root))) {
      return files
          .filter(f -> f.getFileName().toString().endsWith("Controller.class"))
          .map(OpenApiTool::toClassName)
          .map(OpenApiTool::toClass)
          .filter(
              c ->
                  !Modifier.isAbstract(c.getModifiers()) && !c.isMemberClass() && !c.isLocalClass())
          .collect(toSet());
    } catch (IOException ex) {
      ex.printStackTrace(err);
      return null;
    }
  }

  private AtomicInteger generateDocumentsFromDocumentAnnotation(
      String to, PrintWriter out, PrintWriter err, Api.Scope scope) {
    Map<String, Set<Class<?>>> byDoc = new TreeMap<>();
    scope
        .matches()
        .forEach(
            cls -> byDoc.computeIfAbsent(getDocumentName(cls), key -> new HashSet<>()).add(cls));
    return generateDocumentsFromGroups(to, out, err, scope, byDoc);
  }

  private AtomicInteger generateDocumentsFromGroups(
      String to,
      PrintWriter out,
      PrintWriter err,
      Api.Scope scope,
      Map<String, Set<Class<?>>> groups) {

    String dir =
        to.endsWith("/")
            ? to.substring(0, to.length() - 1)
            : to.replace(".json", "").replace(".yaml", "");
    String fileExtension = to.endsWith(".yaml") ? ".yaml" : ".json";

    AtomicInteger errorCode = new AtomicInteger(0);
    groups.forEach(
        (name, classes) -> {
          String filename = dir + "/openapi-" + name + fileExtension;
          errorCode.addAndGet(
              generateDocument(
                  filename,
                  out,
                  err,
                  new Api.Scope(
                      classes,
                      scope.filters(),
                      ApiClassifications.matches(classes, scope.filters()))));
        });
    return errorCode;
  }

  private Integer generateDocument(
      String filename, PrintWriter out, PrintWriter err, Api.Scope scope) {
    try {
      Api api = ApiExtractor.extractApi(scope, new ApiExtractor.Configuration(false));

      ApiIntegrator.integrateApi(
          api, ApiIntegrator.Configuration.builder().failOnNameClash(true).build());

      Path file = Path.of(filename);
      String title =
          file.getFileName()
              .toString()
              .replace("openapi-", "")
              .replace('_', ' ')
              .replace(".json", "");
      OpenApiGenerator.Info info =
          OpenApiGenerator.Info.DEFAULT.toBuilder().title("DHIS2 API - " + title).build();
      OpenApiGenerationParams params = new OpenApiGenerationParams();
      Language language = filename.endsWith(".json") ? Language.JSON : Language.YAML;
      String doc = OpenApiGenerator.generate(language, api, PRETTY_PRINT, info, params);
      Path output = Files.writeString(file, doc);
      int controllers = api.getControllers().size();
      int endpoints = api.getControllers().stream().mapToInt(c -> c.getEndpoints().size()).sum();
      int schemas = api.getComponents().getSchemas().size();
      int parameters =
          api.getComponents().getParameters().values().stream().mapToInt(List::size).sum();
      out.printf(
          "  %-40s [%3d controllers, %3d endpoints, %3d schemas, %3d parameters]%n",
          output.getFileName(), controllers, endpoints, schemas, parameters);
    } catch (Exception ex) {
      ex.printStackTrace(err);
      return -1;
    }
    return 0;
  }

  private static String getDocumentName(Class<?> controller) {
    if (controller.isAnnotationPresent(OpenApi.Document.class)) {
      OpenApi.Document doc = controller.getAnnotation(OpenApi.Document.class);
      if (!doc.name().isEmpty()) return doc.name();
      if (doc.entity() != OpenApi.EntityType.class) return getTypeName(doc.entity());
    }
    return getTypeName(OpenApiAnnotations.getEntityType(controller));
  }

  @Nonnull
  private static String getTypeName(@CheckForNull Class<?> type) {
    if (type == null) return "Misc";
    if (type.isAnnotationPresent(OpenApi.Shared.class)) {
      OpenApi.Shared shared = type.getAnnotation(OpenApi.Shared.class);
      if (!shared.name().isEmpty()) return shared.name();
    }
    return type.getSimpleName();
  }

  private static String toClassName(Path f) {
    return f.toString()
        .substring(f.toString().indexOf("org/hisp/"))
        .replace(".class", "")
        .replace('/', '.');
  }

  private static Class<?> toClass(String className) {
    try {
      return Class.forName(className);
    } catch (Exception ex) {
      throw new IllegalArgumentException("failed loading: " + className, ex);
    }
  }
}
