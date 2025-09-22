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
package org.hisp.dhis.webapi.openapi;

import static java.util.Comparator.comparing;
import static java.util.Map.entry;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.webapi.openapi.OpenApiHtmlUtils.escapeHtml;
import static org.hisp.dhis.webapi.openapi.OpenApiHtmlUtils.stripHtml;
import static org.hisp.dhis.webapi.openapi.OpenApiMarkdown.markdownToHTML;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonNodeType;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.webapi.openapi.ApiClassifications.Classifier;
import org.hisp.dhis.webapi.openapi.OpenApiObject.MediaTypeObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.OperationObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.ParameterObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.RequestBodyObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.ResponseObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.SchemaObject;

/**
 * A tool that can take a OpenAPI JSON document and render it as HTML.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@RequiredArgsConstructor
public class OpenApiRenderer {

  /*
  Reorganizing...
   */
  record OperationsItem(String entity, Map<String, OperationsGroupItem> groups) {}

  record OperationsGroupItem(String entity, String group, List<OperationObject> operations) {}

  record SchemasItem(String kind, List<SchemaObject> schemas) {}

  private static final Comparator<OperationObject> SORT_BY_METHOD =
      comparing(OperationObject::operationMethod)
          .thenComparing(OperationObject::operationPath)
          .thenComparing(OperationObject::operationId);

  private List<OperationsItem> groupedOperations() {
    Map<String, OperationsItem> byEntity = new TreeMap<>();
    Consumer<OperationObject> add =
        op -> {
          String entity = op.x_entity();
          String group = op.x_group();
          byEntity
              .computeIfAbsent(entity, e -> new OperationsItem(e, new TreeMap<>()))
              .groups()
              .computeIfAbsent(group, g -> new OperationsGroupItem(entity, g, new ArrayList<>()))
              .operations()
              .add(op);
        };
    api.operations().forEach(add);
    if (params.sortEndpointsByMethod)
      byEntity
          .values()
          .forEach(p -> p.groups().values().forEach(g -> g.operations().sort(SORT_BY_METHOD)));
    return List.copyOf(byEntity.values());
  }

  private List<SchemasItem> groupedSchemas() {
    Map<String, SchemasItem> byKind = new TreeMap<>();
    api.components()
        .schemas()
        .forEach(
            (name, schema) -> {
              String kind = schema.x_kind();
              byKind
                  .computeIfAbsent(kind, k -> new SchemasItem(k, new ArrayList<>()))
                  .schemas()
                  .add(schema);
            });
    return List.copyOf(byKind.values());
  }

  /*
  Rendering...
   */

  public static String renderHTML(String json, OpenApiRenderingParams params, ApiStatistics stats) {
    OpenApiRenderer renderer =
        new OpenApiRenderer(JsonValue.of(json).as(OpenApiObject.class), params, stats);
    return renderer.renderHTML();
  }

  private final OpenApiObject api;
  private final OpenApiRenderingParams params;
  private final ApiStatistics stats;
  private final StringBuilder out = new StringBuilder();

  @Override
  public String toString() {
    return out.toString();
  }

  public String renderHTML() {
    renderDocument();
    return toString();
  }

  private void renderDocument() {
    appendRaw("<!doctype html>");
    appendTag(
        "html",
        Map.of("lang", "en"),
        () -> {
          appendTag(
              "head",
              () -> {
                appendRaw(
                    """
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:ital,wght@0,100..800;1,100..800&display=swap" rel="stylesheet">
                """);
                appendTag("title", api.info().title() + " " + api.info().version());
                appendTag("link", Map.of("rel", "icon", "href", "./favicon.ico"), "");
                String cp = params.contextPath;
                appendRaw(
                    "<link rel=\"stylesheet\" type=\"text/css\" href=\"%s/css/openapi.css\">"
                        .formatted(cp));
                appendRaw("<script src=\"%s/js/openapi.js\"></script>".formatted(cp));
              });
          appendTag(
              "body",
              Map.of("id", "body", "content-", ""),
              () -> {
                renderPageMenu();
                renderPageHeader();
                renderPathOperations();
                renderComponentsSchemas();
              });
        });
  }

  private void renderPageMenu() {
    appendTag(
        "nav",
        () -> {
          renderMenuHeader();
          renderMenuScope();
          renderMenuDisplay();
          renderMenuHotkeys();
        });
  }

  private void renderMenuHeader() {
    appendTag("header", () -> appendTag("h1", api.info().version()));
  }

  private void renderMenuScope() {
    renderMenuGroup(
        "scope",
        () -> appendRaw("Scope"),
        () -> stats.classifications().classifiers().forEach(this::renderScopeMenuItem));
  }

  private void renderScopeMenuItem(String classifier, List<Classifier> classifiers) {
    appendTag(
        "div",
        () -> {
          appendTag("label", classifier);
          appendTag(
              "select",
              Map.of("data-key", classifier),
              () -> {
                appendTag("option", Map.of("value", ""), "(select and click go or +)");
                classifiers.forEach(this::renderScopeMenuOption);
              });
          appendInputButton("go", "modifyLocationSearch(this)");
          appendInputButton("+", "modifyLocationSearch(this)");
        });
  }

  private void renderScopeMenuOption(Classifier c) {
    int p = c.percentage();
    appendTag(
        "option", Map.of("value", c.value()), c.value() + (p < 1 ? "" : " (~%d%%)".formatted(p)));
  }

  private void renderMenuHotkeys() {
    renderMenuGroup("hotkeys", () -> appendRaw(" Hotkeys"), () -> {});
  }

  private void renderMenuDisplay() {
    renderMenuGroup(
        null,
        () -> appendRaw("Display"),
        () -> {
          renderMenuItem(
              "HTTP Methods",
              () -> {
                renderToggleButton("GET", "GET", "get-", false);
                renderToggleButton("POST", "POST", "post-", false);
                renderToggleButton("PUT", "PUT", "put-", false);
                renderToggleButton("PATCH", "PATCH", "patch-", false);
                renderToggleButton("DELETE", "DELETE", "delete-", false);
              });

          renderMenuItem(
              "HTTP Status",
              () -> {
                for (int statusCode : new int[] {200, 201, 202, 204}) {
                  renderToggleButton(
                      statusCode + " " + statusCodeName(statusCode),
                      "",
                      "status" + statusCode + "-",
                      true);
                }
              });

          renderMenuItem(
              "HTTP Content-Type",
              () -> {
                for (String mime : new String[] {"json", "xml", "csv"})
                  renderToggleButton(mime.toUpperCase(), mime, mime + "-", true);
              });

          renderMenuItem(
              "Maturity",
              () -> {
                renderToggleButton("&#129514; Alpha", "alpha", "alpha-", false);
                renderToggleButton("&#128295; Beta", "beta", "beta-", false);
                renderToggleButton("&#128737;&#65039; Stable", "stable", "stable-", false);
                renderToggleButton("Unspecified", "open", "open-", false);
                renderToggleButton("@Deprecated", "deprecated", "deprecated-", true);
              });
          renderMenuItem(
              "Summaries",
              () -> {
                renderToggleButton("Show request info", "request", "request-", false);
                renderToggleButton("Show response info", "response", "response-", false);
                renderToggleButton("Show content info", "content", "content-", false);
              });
          renderMenuItem(
              "Details", () -> renderToggleButton("Abbr. Descriptions", "desc", "desc-", false));
        });
  }

  private void renderMenuGroup(String id, Runnable renderSummary, Runnable renderBody) {
    appendDetails(
        id,
        true,
        "",
        () -> {
          appendSummary(null, "", renderSummary::run);
          renderBody.run();
        });
  }

  private void renderMenuItem(String title, Runnable renderBody) {
    appendDetails(
        null,
        true,
        "",
        () -> {
          appendSummary(null, "", () -> appendRaw(title));
          renderBody.run();
        });
  }

  private void renderToggleButton(String text, String style, String toggle, boolean code) {
    String js = "document.getElementById('body').toggleAttribute('" + toggle + "')";
    Runnable body = code ? () -> appendCode(style, text) : () -> appendRaw(text);
    appendTag("button", Map.of("onclick", js, "class", style), body);
  }

  private void renderPageHeader() {
    appendTag(
        "section",
        () -> {
          Map<String, Set<String>> filters = stats.partial().getScope().filters();
          String title = filters.isEmpty() ? "DHIS2 Full API (Single Page)" : "DHIS2 Partial API";
          appendTag(
              "header",
              () -> {
                appendTag(
                    "h1",
                    () -> {
                      if (!filters.isEmpty()) {
                        appendRaw("[");
                        appendA(
                            "setLocationSearch('scope', '')",
                            "Full",
                            "View full API as single page document");
                        appendRaw("] ");
                      }
                      appendRaw(title);
                      appendRaw(" [");
                      appendA(
                          "setLocationPathnameFile('openapi.json')",
                          "JSON",
                          "View this document as JSON source");
                      appendRaw("] [");
                      appendA(
                          "setLocationPathnameFile('openapi.yaml')",
                          "YAML",
                          "View this document as YAML source");
                      appendRaw("] ");
                      appendA(
                          "setLocationSearch('source', 'true')",
                          "+ &#128435;",
                          "Add JSON source to this document");
                    });

                stats.compute().forEach(this::renderPageStats);
                if (!filters.isEmpty())
                  appendTag("dl", () -> filters.forEach(this::renderPageHeaderSelection));
              });
        });
  }

  private void renderPageStats(ApiStatistics.Ratio ratio) {
    appendRaw(ratio.name() + ": ");
    appendTag("b", ratio.count() + "");
    int p = ratio.percentage();
    appendRaw("/%d (%s%%) &nbsp; ".formatted(ratio.total(), p < 1 ? "<1" : p));
  }

  private void renderPageHeaderSelection(String name, Set<String> values) {
    appendTag("dt", name + "s");
    values.forEach(v -> appendTag("dd", () -> renderPageHeaderSelectionItems(name, v)));
  }

  private void renderPageHeaderSelectionItems(String name, String value) {
    appendA(
        "setLocationSearch('scope', '%s:%s')".formatted(name, value),
        value,
        "View a document containing only this scope");
    appendRaw(" ");
    appendA(
        "removeLocationSearch('scope', '%s:%s')".formatted(name, value),
        "[-]",
        "Remove this scope from this document");
  }

  private void renderPathOperations() {
    appendTag("section", () -> groupedOperations().forEach(this::renderPathOperation));
  }

  private void renderPathOperation(OperationsItem op) {
    String id = "-" + op.entity();
    appendDetails(
        id,
        false,
        "",
        () -> {
          appendSummary(id, null, () -> renderPathOperationSummary(op));
          op.groups().values().forEach(this::renderPathGroup);
        });
  }

  private void renderPathOperationSummary(OperationsItem op) {
    appendTag(
        "h2",
        () -> {
          appendRaw(toWords(op.entity()));
          appendA(
              "setLocationSearch('scope', 'entity:%s', true)".formatted(op.entity),
              "&#x1F5D7;",
              "");
        });
  }

  private void renderPathGroup(OperationsGroupItem group) {
    String id = "-" + group.entity() + "-" + group.group();
    appendDetails(
        id,
        true,
        "paths",
        () -> {
          appendSummary(id, null, () -> renderPathGroupSummary(group));
          group.operations().forEach(this::renderOperation);
        });
  }

  private void renderPathGroupSummary(OperationsGroupItem group) {
    appendTag("h3", Map.of("class", group.group()), group.group());

    // TODO run this into "Query /api/x/... [12][24]" with numbers indicating GETs, PUTs and so on
    // just by color
    group.operations().stream()
        .collect(groupingBy(OperationObject::operationMethod, counting()))
        .forEach(
            (method, count) -> {
              appendCode(method.toUpperCase() + " http", method.toUpperCase());
              appendTag("b", " x " + count);
            });
  }

  private static String toWords(String camelCase) {
    return camelCase.replaceAll(
        "(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])", " ");
  }

  private static String toUrlHash(@CheckForNull String value) {
    if (value == null) return null;
    return stripHtml(value).replaceAll("[^-_.a-zA-Z0-9@]", "_");
  }

  private void renderOperation(OperationObject op) {
    if (!op.exists()) return;
    String id = toUrlHash(op.operationId());
    appendDetails(
        id,
        false,
        operationStyle(op),
        () -> {
          appendSummary(id, op.summary(), () -> renderOperationSummary(op));
          renderBoxToolbar(
              () -> {
                String declaringClass = op.x_class();
                if (declaringClass != null) {
                  String url =
                      "https://github.com/dhis2/dhis2-core/blob/master/dhis-2/dhis-web-api/src/main/java/%s.java"
                          .formatted(declaringClass.replace('.', '/'));
                  appendTag("a", Map.of("href", url, "target", "_blank", "class", "gh"), "GH");
                }
              });
          appendTag("header", markdownToHTML(op.description(), op.parameterNames()));
          renderLabelledValue("operationId", op.operationId());
          renderLabelledValue("since", op.x_since());
          renderLabelledValue("requires-authority", op.x_auth());
          renderLabelledValue("tags", op.tags(), "", 0);
          renderParameters(op);
          renderRequestBody(op);
          renderResponses(op);
          if (params.isSource()) renderSource("@" + op.operationId(), op);
        });
  }

  private static String operationStyle(OperationObject op) {
    StringBuilder style = new StringBuilder("op box");
    style.append(" ").append(op.operationMethod().toUpperCase());
    style.append(" status").append(op.responseSuccessCode());
    for (String mime : op.responseMediaSubTypes()) style.append(" ").append(mime);
    if (op.deprecated()) style.append(" deprecated");
    String maturity = op.x_maturity();
    style.append(" ").append(maturity == null ? "open" : maturity);
    return style.toString();
  }

  private void renderBoxToolbar(Runnable renderButtons) {
    Map<String, String> attrs =
        Map.ofEntries(
            entry("onclick", "openToggleDown1(this)"),
            entry("title", "remembering expand/collapse"),
            entry("class", "toggle"));
    appendTag(
        "aside",
        () -> {
          appendTag("button", attrs, "All");
          if (renderButtons != null) renderButtons.run();
        });
  }

  private void renderOperationSummary(OperationObject op) {
    String method = op.operationMethod().toUpperCase();
    String path = op.operationPath();
    List<String> codes = op.responseCodes();
    appendCode(
        "http method",
        () -> {
          appendRaw(method);
          if (!codes.isEmpty()) {
            String code = codes.get(0);
            code = codes.stream().filter(c -> c.startsWith("2")).findFirst().orElse(code);
            appendSpan("http status status" + code.charAt(0) + "xx", code);
          }
        });
    // TODO the /api should be greyed out as it is common for all
    // FIXME endpoints with a # (merged?) start with api/ instead of /api/ (bug in merge?)
    appendCode("url path", getUrlPathInSections(path));
    List<ParameterObject> queryParams = op.parameters(ParameterObject.In.query);
    if (!queryParams.isEmpty()) {
      String query = "?";
      List<String> requiredParams =
          queryParams.stream()
              .filter(ParameterObject::required)
              .map(p -> p.name() + "=&blank;")
              .toList();
      query += String.join("&", requiredParams);
      if (queryParams.size() > requiredParams.size()) query += "&hellip;";
      appendCode("url query secondary", query);
    }
    List<SchemaObject> request = op.requestSchemas();
    if (!request.isEmpty()) {
      appendCode("request secondary", "{");
      renderSchemaSignature(request);
      appendCode("request secondary", "}");
    }
    appendCode("response secondary", "::");
    renderMediaSubTypesIndicator(op.responseMediaSubTypes());
    List<SchemaObject> successOneOf = op.responseSuccessSchemas();
    if (!successOneOf.isEmpty()) {
      renderSchemaSignature(successOneOf);
    }
  }

  private void renderMediaSubTypesIndicator(Collection<String> subTypes) {
    if (subTypes.isEmpty()) return;
    appendCode(
        "http content",
        () -> {
          appendSpan(subTypes.contains("json") ? "on" : "", "JSON");
          appendSpan(subTypes.contains("xml") ? "on" : "", "XML");
          appendSpan(subTypes.contains("csv") ? "on" : "", "CSV");
          appendSpan(subTypes.stream().anyMatch(t -> !t.matches("xml|json|csv")) ? "on" : "", "*");
        });
  }

  private static String getUrlPathInSections(String path) {
    return path.replaceAll("/(\\{[^/]+)(?<=})(?=/|$)", "/<em>$1</em>")
        .replaceAll("#([a-zA-Z0-9_]+)", "<small>#<span>$1</span></small>");
  }

  private void renderOperationSectionHeader(String text, String title) {
    Map<String, String> attrs =
        Map.ofEntries(entry("class", "url secondary"), entry("title", title));
    appendTag("h4", () -> appendTag("code", attrs, text));
  }

  private void renderParameters(OperationObject op) {
    JsonList<ParameterObject> opParams = op.parameters();
    if (opParams.isUndefined() || opParams.isEmpty()) return;

    // TODO header and cookie (if we need it)
    renderParameters(op, ParameterObject.In.path, "/.../");
    renderParameters(op, ParameterObject.In.query, "?...");
  }

  private void renderParameters(OperationObject op, ParameterObject.In in, String text) {
    List<ParameterObject> opParams = op.parameters(in);
    if (opParams.isEmpty()) return;
    renderOperationSectionHeader(text, "Parameters in " + in.name().toLowerCase());
    Set<String> parameterNames = op.parameterNames();
    opParams.stream()
        .map(ParameterObject::resolve)
        .forEach(p -> renderParameter(op, p, parameterNames));
  }

  private void renderParameter(OperationObject op, ParameterObject p, Set<String> parameterNames) {
    String style = "param";
    if (p.deprecated()) style += " deprecated";
    if (p.required()) style += " required";
    String id = toUrlHash(op.operationId() + "." + p.name());
    appendDetails(
        id,
        true,
        style,
        () -> {
          appendSummary(id, null, () -> renderParameterSummary(p));
          String description = markdownToHTML(p.description(), parameterNames);
          appendTag("article", Map.of("class", "desc"), description);
          renderLabelledValue("since", p.x_since());
          renderSchemaDetails(p.schema(), false, true);
        });
  }

  private void renderParameterSummary(ParameterObject p) {
    SchemaObject schema = p.schema();
    appendCode("url", p.name());
    appendCode("url secondary", "=");
    renderSchemaSummary(schema, false);

    // parameter default uses schema default as fallback
    renderLabelledValue("default", p.$default());
  }

  private void renderLabelledValue(String label, Object value) {
    renderLabelledValue(label, value, "", 0);
  }

  private void renderLabelledValue(String label, Object value, String style, int limit) {
    if (value == null) return;
    if (value instanceof Collection<?> l && l.isEmpty()) return;
    if (value instanceof JsonValue json) value = jsonToDisplayValue(json);
    if (value == null) return;
    Object val = value;
    appendCode(
        style + " tag",
        () -> {
          appendSpan(label + ":");
          if (val instanceof Collection<?> c) {
            int maxSize = limit <= 0 ? Integer.MAX_VALUE : limit;
            appendSpan(
                c.stream()
                    .limit(maxSize)
                    .map(e -> escapeHtml(e.toString()))
                    .collect(joining("</span>, <span>")));
            if (c.size() > maxSize) appendRaw("...");
          } else {
            appendSpan(escapeHtml(val.toString()));
          }
        });
  }

  private String jsonToDisplayValue(JsonValue value) {
    if (value.isUndefined()) return null;
    return value.type() == JsonNodeType.STRING
        ? value.as(JsonString.class).string()
        : value.toJson();
  }

  private void renderRequestBody(OperationObject op) {
    RequestBodyObject requestBody = op.requestBody();
    if (requestBody.isUndefined()) return;
    renderOperationSectionHeader("{...}", "Request Body");

    JsonMap<MediaTypeObject> content = requestBody.content();
    String style = "request";
    if (requestBody.required()) style += " required";
    renderMediaTypes(op.operationId(), style, content);

    renderMarkdown(requestBody.description(), op.parameterNames());
  }

  private void renderMarkdown(String text, Set<String> keywords) {
    appendTag("article", Map.of("class", "desc"), markdownToHTML(text, keywords));
  }

  private void renderMediaTypes(
      String idPrefix, String style, JsonMap<MediaTypeObject> mediaTypes) {
    if (mediaTypes.isUndefined() || mediaTypes.isEmpty()) return;
    mediaTypes.forEach(
        (mediaType, schema) -> renderMediaType(idPrefix, style, mediaType, schema.schema()));
  }

  private void renderMediaType(String idPrefix, String style, String mediaType, SchemaObject type) {
    String id = idPrefix == null ? null : toUrlHash(idPrefix + "-" + mediaType);
    appendDetails(
        id,
        false,
        style,
        () -> {
          appendSummary(
              id,
              "",
              () -> {
                appendCode("mime", mediaType);
                appendCode("mime secondary", ":");
                renderSchemaSummary(type, false);
              });
          renderSchemaDetails(type, false, false);
        });
  }

  private void renderResponses(OperationObject op) {
    JsonMap<ResponseObject> responses = op.responses();
    if (responses.isUndefined() || responses.isEmpty()) return;

    renderOperationSectionHeader("::", "Responses");
    responses.entries().forEach(e -> renderResponse(op, e.getKey(), e.getValue()));
  }

  private void renderResponse(OperationObject op, String code, ResponseObject response) {
    String id = toUrlHash(op.operationId() + "-" + code);
    boolean open = code.charAt(0) == '2' || !response.isUniform();
    appendDetails(
        id,
        open,
        "response",
        () -> {
          appendSummary(id, null, () -> renderResponseSummary(code, response));
          renderMediaTypes(id, "response", response.content());
          renderMarkdown(response.description(), op.parameterNames());
        });
  }

  private void renderResponseSummary(String code, ResponseObject response) {
    String name = statusCodeName(Integer.parseInt(code));
    appendCode("status status" + code.charAt(0) + "xx status" + code, code + " " + name);

    JsonMap<MediaTypeObject> content = response.content();
    if (content.isUndefined() || content.isEmpty()) return;

    appendCode("mime", "=");

    if (content.size() == 1) {
      Map.Entry<String, MediaTypeObject> common = content.entries().toList().get(0);
      appendCode("mime secondary", common.getKey());
      appendCode("mime secondary", ":");
      renderSchemaSignature(common.getValue().schema());
    } else if (response.isUniform()) {
      // they all share the same schema
      appendCode("mime secondary", "*");
      appendCode("mime secondary", ":");
      SchemaObject common = content.values().limit(1).toList().get(0).schema();
      renderSchemaSignature(common);
    } else {
      // they are different, only list media types in summary
      List<String> mediaTypes = content.keys().toList();
      for (int i = 0; i < mediaTypes.size(); i++) {
        if (i > 0) appendCode("mime secondary", "|");
        appendCode("mime secondary", mediaTypes.get(i));
      }
    }
  }

  private void renderSchemaSignature(List<SchemaObject> oneOf) {
    if (oneOf.isEmpty()) return;
    renderSchemaSignature(() -> renderSchemaSignatureType(oneOf));
  }

  private void renderSchemaSignature(SchemaObject schema) {
    renderSchemaSignature(() -> renderSchemaSignatureType(schema));
  }

  private void renderSchemaSignature(Runnable renderType) {
    appendCode(
        "type",
        () -> {
          appendRaw("&lt;");
          renderType.run();
          appendRaw("&gt;");
        });
  }

  private void renderSchemaSignatureType(List<SchemaObject> oneOf) {
    for (int i = 0; i < oneOf.size(); i++) {
      if (i > 0) appendRaw(" | ");
      renderSchemaSignatureType(oneOf.get(i));
    }
  }

  private void renderSchemaSignatureType(SchemaObject schema) {
    if (schema.isShared()) {
      appendA("#" + schema.getSharedName(), schema.getSharedName());
      return;
    }
    renderSchemaSignatureTypeAny(schema);
  }

  private void renderSchemaSignatureTypeAny(JsonList<SchemaObject> options, String separator) {
    for (int i = 0; i < options.size(); i++) {
      if (i > 0) appendRaw(separator);
      renderSchemaSignatureType(options.get(i));
    }
  }

  private void renderSchemaSignatureTypeAny(SchemaObject schema) {
    if (schema.isRef()) {
      renderSchemaSignatureType(schema.resolve());
      return;
    }
    if (schema.isAnyType()) {
      appendRaw("*");
      return;
    }
    if (schema.isArrayType()) {
      appendRaw("array[");
      renderSchemaSignatureType(schema.items());
      appendRaw("]");
      return;
    }
    if (schema.isObjectType()) {
      renderSchemaSignatureTypeObject(schema);
      return;
    }
    if (schema.isStringType() && schema.isEnum()) {
      appendRaw("enum");
      return;
    }
    String type = schema.$type();
    if (type != null) {
      appendRaw(type);
      return;
    }
    // must be a composer then
    if (schema.oneOf().exists()) {
      renderSchemaSignatureTypeAny(schema.oneOf(), " | ");
      return;
    }
    if (schema.anyOf().exists()) {
      renderSchemaSignatureTypeAny(schema.anyOf(), " || ");
      return;
    }
    if (schema.allOf().exists()) {
      renderSchemaSignatureTypeAny(schema.allOf(), " &amp; ");
      return;
    }
    if (schema.not().exists()) {
      appendRaw("!");
      renderSchemaSignatureType(schema.not());
      return;
    }
    // we don't know/understand this yet
    appendRaw("?");
  }

  /**
   * For readability’s sake we attempt to describe an object more specific than just {code object}.
   * When it has the quality of a map or just a single property or is a paged list the structure is
   * further described in the signature.
   */
  private void renderSchemaSignatureTypeObject(SchemaObject schema) {
    appendRaw("object");
    if (schema.isMap()) {
      appendRaw("{*:");
      // TODO allow x-keys - syntax is *(type) if x-keys is defined
      renderSchemaSignatureType(schema.additionalProperties());
      appendRaw("}");
    } else if (schema.isWrapper()) {
      Map.Entry<String, SchemaObject> p0 = schema.properties().entries().limit(1).toList().get(0);
      appendRaw("{");
      appendEscaped(p0.getKey());
      appendRaw(":");
      renderSchemaSignatureType(p0.getValue());
      appendRaw("}");
    } else if (schema.isEnvelope()) {
      Map.Entry<String, SchemaObject> values =
          schema
              .properties()
              .entries()
              .filter(e -> e.getValue().isArrayType())
              .findFirst()
              .orElse(null);
      if (values != null) {
        appendRaw("{#,"); // # short for the pager, comma for next property
        appendEscaped(values.getKey());
        appendRaw(":");
        renderSchemaSignatureType(values.getValue());
        appendRaw("}");
      }
    }
  }

  private void renderComponentsSchemas() {
    appendTag(
        "section",
        () -> {
          for (SchemasItem kind : groupedSchemas()) {
            String id = "--" + kind.kind;
            appendDetails(
                id,
                false,
                "kind",
                () -> {
                  String words = toWords(kind.kind);
                  String plural = words.endsWith("s") ? words : words + "s";
                  appendSummary(id, "", () -> appendTag("h2", plural));
                  kind.schemas.forEach(this::renderComponentSchema);
                });
          }
        });
  }

  private void renderComponentSchema(SchemaObject schema) {
    String id = toUrlHash(schema.getSharedName());
    appendDetails(
        id,
        false,
        "schema box " + schema.$type(),
        () -> {
          appendSummary(schema.getSharedName(), "", () -> renderComponentSchemaSummary(schema));
          renderBoxToolbar(
              () -> {
                Map<String, String> attrs =
                    Map.of("class", "button", "ontoggle", "schemaUsages(this)");
                appendTag("details", attrs, () -> appendTag("summary", "Usages"));
              });
          renderSchemaDetails(schema, true, false);
          if (params.isSource()) renderSource("@" + id, schema);
        });
  }

  private void renderComponentSchemaSummary(SchemaObject schema) {
    appendCode("type", schema.getSharedName());
    renderSchemaSummary(schema, true);
    if (schema.isObjectType()) renderLabelledValue("required", schema.required(), "", 5);
    if (schema.isEnum()) renderLabelledValue("enum", schema.$enum(), "", 5);
  }

  private void renderSchemaSummary(SchemaObject schema, boolean isDeclaration) {
    if (!isDeclaration) renderSchemaSignature(schema);
    if (schema.isRef() || (!isDeclaration && schema.isShared())) {
      if (params.inlineEnumsLimit > 0 && schema.isEnum())
        renderLabelledValue("enum", schema.$enum(), "", params.inlineEnumsLimit);
      return;
    }
    if (schema.isReadOnly()) renderLabelledValue("readOnly", true);
    renderLabelledValue("format", schema.format());
    renderLabelledValue("minLength", schema.minLength());
    renderLabelledValue("maxLength", schema.maxLength());
    renderLabelledValue("pattern", schema.pattern());
  }

  private void renderSchemaDetails(
      SchemaObject schema, boolean isDeclaration, boolean skipDefault) {
    if (schema.isRef() || (!isDeclaration && schema.isShared())) {
      return; // summary already gave all that is needed
    }
    if (schema.isFlat()) return;
    if (schema.$type() != null) {
      Set<String> names =
          schema.isObjectType()
              ? schema.properties().keys().collect(toUnmodifiableSet())
              : Set.of();
      appendTag("header", markdownToHTML(schema.description(), names));
      if (!skipDefault) renderLabelledValue("default", schema.$default(), "columns", 0);
      renderLabelledValue("enum", schema.$enum(), "columns", 0);

      if (schema.isObjectType()) {
        List<String> required = schema.required();
        schema
            .properties()
            .forEach((n, s) -> renderSchemaProperty(schema, n, s, required.contains(n)));
        SchemaObject additionalProperties = schema.additionalProperties();
        if (!additionalProperties.isUndefined()) {
          renderSchemaProperty(schema, "<additionalProperties>", additionalProperties, false);
        }
        return;
      }
      if (schema.isArrayType()) {
        renderSchemaProperty(schema, "<items>", schema.items(), false);
      }
      return;
    }
    if (schema.oneOf().exists()) {
      schema.oneOf().forEach(s -> renderSchemaProperty(schema, "<oneOf>", s, false));
      return;
    }
    if (schema.anyOf().exists()) {
      schema.anyOf().forEach(s -> renderSchemaProperty(schema, "<anyOf>", s, false));
      return;
    }
    if (schema.allOf().exists()) {
      schema.allOf().forEach(s -> renderSchemaProperty(schema, "<allOf>", s, false));
      return;
    }
    if (schema.not().exists()) {
      renderSchemaProperty(schema, "<not>", schema.not(), false);
    }
  }

  private void renderSchemaProperty(
      SchemaObject parent, String name, SchemaObject type, boolean required) {
    String id = parent.isShared() ? toUrlHash(parent.getSharedName() + "." + name) : null;
    String style = "property";
    if (required) style += " required";
    boolean open = type.isObjectType() || type.isArrayType() && type.items().isObjectType();
    appendDetails(
        id,
        open,
        style,
        () -> {
          appendSummary(
              id,
              "",
              () -> {
                appendCode("property", () -> appendEscaped(name));
                appendCode("property secondary", ":");
                renderSchemaSummary(type, false);
              });
          appendTag("header", markdownToHTML(type.description(), Set.of()));
          renderLabelledValue("since", type.x_since());
          renderSchemaDetails(type, false, false);
        });
  }

  private void renderSource(String id, JsonObject op) {
    appendDetails(
        id,
        false,
        "source",
        () -> {
          appendSummary(id, "", () -> appendRaw("&#128435; Source"));
          appendTag("pre", () -> appendEscaped(op.toJson()));
        });
  }

  private void appendDetails(@CheckForNull String id, boolean open, String style, Runnable body) {
    Map<String, String> attrs =
        Map.of("class", style, "id", id == null ? "" : id, open ? "open" : "", "");
    appendTag("details", attrs, body);
  }

  private void appendSummary(@CheckForNull String id, String title, Runnable body) {
    Map<String, String> attrs = Map.of("title", title == null ? "" : title);
    appendTag(
        "summary",
        attrs,
        () -> {
          if (id != null) appendA("#" + id, "#");
          body.run();
        });
  }

  private void appendInputButton(String text, String onclick) {
    appendTag("input", Map.of("type", "button", "value", text, "onclick", onclick));
  }

  private void appendA(String href, String text) {
    String title = "#".equals(text) ? "permalink" : "";
    appendTag("a", Map.of("href", href, "title", title), text);
  }

  private void appendA(String onclick, String text, String title) {
    appendTag("a", Map.of("onclick", onclick, "title", title), text);
  }

  private void appendSpan(String text) {
    appendSpan("", text);
  }

  private void appendSpan(String style, String text) {
    appendTag("span", Map.of("class", style), text);
  }

  private void appendCode(String style, String text) {
    appendTag("code", Map.of("class", style), text);
  }

  private void appendCode(String style, Runnable body) {
    appendTag("code", Map.of("class", style), body);
  }

  private void appendTag(String name, String text) {
    appendTag(name, Map.of(), text);
  }

  private void appendTag(String name, Map<String, String> attributes, String text) {
    if (text != null && !text.isEmpty()) appendTag(name, attributes, () -> appendRaw(text));
  }

  private void appendTag(String name, Runnable body) {
    appendTag(name, Map.of(), body);
  }

  private void appendTag(String name, Map<String, String> attributes) {
    appendTag(name, attributes, () -> {});
  }

  private void appendTag(String name, Map<String, String> attributes, Runnable body) {
    out.append('<').append(name);
    attributes.forEach(this::appendAttr);
    if (body == null) {
      out.append("/>");
      return;
    }
    out.append('>');
    body.run();
    out.append("</").append(name).append('>');
  }

  /**
   * To avoid lots of conditions when constructing attribute maps this allows to put an empty string
   * for certain attributes to have them ignored since it is known that they only make sense with a
   * value.
   */
  private static final Set<String> ATTR_NAMES_IGNORE_WHEN_EMPTY =
      Set.of("class", "title", "target", "id");

  private void appendAttr(String name, String value) {
    if (name == null || name.isEmpty()) return;
    boolean emptyValue = value == null || value.isEmpty();
    if (emptyValue && ATTR_NAMES_IGNORE_WHEN_EMPTY.contains(name))
      return; // optimisation to prevent rendering `class` without a value
    out.append(' ').append(name);
    if (!emptyValue) {
      out.append('=').append('"');
      appendEscaped(value);
      out.append('"');
    }
  }

  private void appendEscaped(String text) {
    appendRaw(escapeHtml(text));
  }

  private void appendRaw(String text) {
    out.append(text);
  }

  private static String statusCodeName(int code) {
    return switch (code) {
      case 200 -> "Ok";
      case 201 -> "Created";
      case 202 -> "Accepted";
      case 203 -> "Not Authoritative";
      case 204 -> "No Content";
      case 205 -> "Reset Content";
      case 206 -> "Partial Content";
      case 302 -> "Found";
      case 304 -> "Not Modified";
      case 400 -> "Bad Request";
      case 401 -> "Unauthorized";
      case 402 -> "Payment Required";
      case 403 -> "Forbidden";
      case 404 -> "Not Found";
      case 405 -> "Bad Method";
      case 406 -> "Not Acceptable";
      case 407 -> "proxy_auth";
      case 408 -> "client_timeout";
      case 409 -> "Conflict";
      case 410 -> "Gone";
      case 411 -> "Length_required";
      case 412 -> "Precondition Failed";
      case 413 -> "Entity Too Large";
      case 414 -> "Request Too Long";
      case 415 -> "Unsupported Type";
      default -> String.valueOf(code);
    };
  }
}
