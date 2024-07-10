/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.webapi.openapi.OpenApiMarkdown.markdownToHTML;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.webapi.openapi.OpenApiObject.OperationObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.ParameterObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.SchemaObject;
import org.intellij.lang.annotations.Language;

/**
 * A tool that can take a OpenAPI JSON document and render it as HTML.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@RequiredArgsConstructor
public class OpenApiRenderer {

  @Data
  public static class OpenApiRenderingParams {
    boolean sortEndpointsByMethod = true; // make this a sortEndpointsBy=method,path,id thing?
    // TODO inline enum types
  }

  @Language("css")
  private static final String CSS =
      """
  @import url('https://fonts.cdnfonts.com/css/futura-std-4');
  :root {
       --bg-page: white;
       --p-op-bg-header: 30%;
       --p-op-bg: 12%;
       --color-delete: tomato;
       --color-patch: plum;
       --color-post: darkseagreen;
       --color-put: burlywood;
       --color-options: rosybrown;
       --color-get: lightsteelblue;
       --color-trace: palevioletred;
       --color-head: thistle;
       --color-dep: lemonchiffon;
       --color-tooltip: #444;
       --color-tooltiptext: #eee;
       --color-tooltipborder: lightgray;
   }
  html {
    background-color: var(--bg-page);
    height: 100%;
  }
  body {
    background-color: var(--bg-page);
    margin: 0;
    padding-right: 40px;
    min-height: 100%;
    font-family: 'Futura Std', Inter, sans-serif;
    font-size: 16px;
    text-rendering: optimizespeed;
  }
  h1 {
       margin: 0.5rem;
       color: rgb(33, 41, 52);
       font-size: 110%;
       font-weight: normal;
       text-align: left;
   }
  h2 {
      display: inline;
      font-size: 110%;
      font-weight: normal;
  }
  h3 {
      font-size: 105%;
      display: inline;
      text-transform: capitalize;
      font-weight: normal;
  }
  .op h4 { font-weight: normal; padding: 0 1em; }
  h5 {
      margin: 1em 0 0.5em 0;
      font-weight: normal;
  }
  a[target="_blank"] { float: right; text-decoration: none; }
  a[href^="#"] { text-decoration: none; }
  a.permalink {
       float: right;
       visibility: hidden;
       text-decoration: none;
  }
  a.permalink:after {
      content: '🔗';
      visibility: visible;
      font-size: 80%;
  }
  code {
    font-family: "Liberation Mono", monospace;
  }
  summary {
      padding: 2px;
      margin-top: 0.5em;
  }
  .nav {
      position: fixed;
      width: 100%;
      height: 60px;
      box-sizing: border-box;
      padding: 10px;
      text-align: center;
      border-bottom: 5px solid #147cd7;
      background-color: white;
      background-image: url('/../favicon.ico');
      background-repeat: no-repeat;
      padding-left: 100px;
      background-position: 5px 5px;
  }
  .filters {
        position: fixed;
        top: 65px;
        width: 220px;
        text-align: left;
        display: inline-block;
        background-color: var(--bg-page);
        padding: 0.5rem 1rem;
  }
  .domains {
    margin-left: 250px;
    padding-top: 65px;
    max-width: 100rem;
  }
  .domains > details {
    margin-top: 10px;
  }
  .domains > details > summary {
       padding: 0.5em 1em;
  }
  .domains > details > summary:before {
      content: '⛁';
      margin-right: 0.5rem;
      color: #888;
  }
  details {
    margin-left: 2rem;
  }
  details > summary:after {
      content: '⊕';
      float: left;
      margin-left: calc(-1rem - 10px);
  }
  details[open] > summary:after {
    content: '⊝';
  }
  .domains > details[open] > summary {
       margin-top: 0;
  }
  .domains > details[open] > summary h2 {
        font-weight: bold;
  }
  details > summary {
      list-style-type: none;
      cursor: pointer;
  }
  details.op > summary { padding: 0.5rem; }
  details.op > header { padding: 1rem; font-size: 95%; }

  /* colors and emphasis effects */
  code.http { display: inline-block; padding: 0 0.5em; font-weight: bold; }
  code.http.content { color: #aaa; display: inline-grid; grid-template-columns: 1fr 1fr; vertical-align: top; }
  code.http.content > span { font-size: 70%; font-weight: normal; padding-right: 1em; }
  code.http.content .on { color: black; }
  code.http.method { width: 4rem; text-align: right; color: dimgray; }
  code.http.status2xx { color: seagreen;  }
  code.http.status2xx:after { content: '⮠'; color: #666; font-weight: normal; padding-right: 0.5rem; }
  code.md { background-color: #eee; }
  code.url { padding: 0.25em 0.5em; background-color: snow; }
  code.url.path { font-weight: bold; }
  code.url em, code.url.secondary { color: lightslategrey; font-style: normal; font-weight: normal; background: color-mix(in srgb, snow 70%, transparent); }
  code.url small { color: gray; }
  code.value { color: darkslateblue; }
  code.type { color: dimgray; }

  .op:not([open]) code.url small > span { font-size: 2px; }
  .op:not([open]) code.url small:hover > span { font-size: inherit; }
  .dep code.url { background-color: var(--color-dep); color: #666; }

  .GET > summary, button.GET { background: color-mix(in srgb, var(--color-get) var(--p-op-bg-header), transparent); }
  .POST > summary, button.POST { background: color-mix(in srgb, var(--color-post) var(--p-op-bg-header), transparent); }
  .PUT > summary, button.PUT { background: color-mix(in srgb, var(--color-put) var(--p-op-bg-header), transparent); }
  .PATCH > summary, button.PATCH { background: color-mix(in srgb, var(--color-patch) var(--p-op-bg-header), transparent); }
  .DELETE > summary, button.DELETE { background: color-mix(in srgb, var(--color-delete) var(--p-op-bg-header), transparent); }
  .OPTIONS > summary { background: color-mix(in srgb, var(--color-options) var(--p-op-bg-header), transparent); }
  .HEAD > summary { background: color-mix(in srgb, var(--color-head) var(--p-op-bg-header), transparent); }
  .TRACE > summary { background: color-mix(in srgb, var(--color-trace) var(--p-op-bg-header), transparent); }

  details[open].GET { background: color-mix(in srgb, var(--color-get) var(--p-op-bg), transparent); }
  details[open].POST { background: color-mix(in srgb, var(--color-post) var(--p-op-bg), transparent); }
  details[open].PUT { background: color-mix(in srgb, var(--color-put) var(--p-op-bg), transparent); }
  details[open].PATCH { background: color-mix(in srgb, var(--color-patch) var(--p-op-bg), transparent); }
  details[open].DELETE { background: color-mix(in srgb, var(--color-delete) var(--p-op-bg), transparent); }

  #body[get-] details.GET,
  #body[post-] details.POST,
  #body[put-] details.PUT,
  #body[patch-] details.PATCH,
  #body[delete-] details.DELETE,
  #body[dep-] details.dep,
  #body[dep-] dt.dep, #body[dep-] dt.dep + dd { display: none; }

  aside button {
      border: none;
      background-color: transparent;
      font-weight: bold;
      border-left: 4px solid transparent;
      cursor: pointer;
      display: inline;
      margin: 2px;
  }
  aside button:before {
      content: '🞕';
      margin-right: 0.5rem;
      font-weight: normal;
  }
  aside button.dep { background-color: var(--color-dep); }

  #body[get-] button.GET:before,
  #body[post-] button.POST:before,
  #body[put-] button.PUT:before,
  #body[patch-] button.PATCH:before,
  #body[delete-] button.DELETE:before,
  #body[dep-] button.dep:before,
  #body[desc-] button.desc:before { content: '🞏'; color: dimgray; }


  .param.required > code:after {
    content: '*';
    color: tomato;
  }
  .param.dep > code:before {
      content: '⚠️';
      font-family: sans-serif; /* reset font */
      display: inline-block; /* reset text-decorations */
      padding-right: 0.25rem;
  }
  .param.dep > code:hover:after {
    content: 'This parameter is deprecated';
    position: absolute;
    background: var(--color-tooltip);
    color: var(--color-tooltiptext);
    padding: 0.25rem 0.5rem;
  }
  .op.dep > summary code.url:hover:after {
    content: 'This operation is deprecated';
    position: absolute;
    background: var(--color-tooltip);
    color: var(--color-tooltiptext);
    padding: 0.25rem 0.5rem;
  }
  dl {
    margin: 0.5em 1em 0.5em 3em;
    display: grid;
    grid-template-columns: 1fr 1fr 1fr 3fr;
  }
  dt, dd {
      margin: 0.5rem 0;
  }
  dd {
    margin-left: 1rem;
  }
  dd p {
    margin: 0;
  }
  dd.desc { margin-right: 2rem; }
  body[desc-] dd.desc:not(:hover) { font-size: 0.1rem; }
  body[desc-] dd.desc:not(:hover):first-line { font-size: 1rem; }
  """;

  @Language("js")
  private static final String GLOBAL_JS = """
  """;

  /*
  Reorganizing...
   */
  record PackageItem(String domain, Map<String, GroupItem> groups) {}

  record GroupItem(String group, List<OperationObject> operations) {}

  private static final Comparator<OperationObject> SORT_BY_METHOD =
      comparing(OperationObject::operationMethod)
          .thenComparing(OperationObject::operationPath)
          .thenComparing(OperationObject::operationId);

  private List<PackageItem> getPackages() {
    Map<String, PackageItem> domains = new TreeMap<>();
    Consumer<OperationObject> add =
        op -> {
          String domain = op.x_package();
          String group = op.x_group();
          domains
              .computeIfAbsent(domain, d -> new PackageItem(d, new TreeMap<>()))
              .groups()
              .computeIfAbsent(group, g -> new GroupItem(g, new ArrayList<>()))
              .operations()
              .add(op);
        };
    api.operations().forEach(add);
    if (params.sortEndpointsByMethod)
      domains
          .values()
          .forEach(p -> p.groups().values().forEach(g -> g.operations().sort(SORT_BY_METHOD)));
    return List.copyOf(domains.values());
  }

  /*
  Rendering...
   */

  public static String render(String json, OpenApiRenderingParams params) {
    OpenApiRenderer html = new OpenApiRenderer(JsonValue.of(json).as(OpenApiObject.class), params);
    return html.render();
  }

  private final OpenApiObject api;
  private final OpenApiRenderingParams params;
  private final StringBuilder out = new StringBuilder();

  @Override
  public String toString() {
    return out.toString();
  }

  public String render() {
    renderDocument();
    return toString();
  }

  private void renderDocument() {
    appendPlain("<!doctype html>");
    appendTag(
        "html",
        Map.of("lang", "en"),
        () -> {
          appendTag(
              "head",
              () -> {
                appendTag("title", api.info().title() + " " + api.info().version());
                appendTag("link", Map.of("rel", "icon", "href", "./favicon.ico"), "");
                appendTag("style", CSS);
                appendTag("script", GLOBAL_JS);
              });
          appendTag(
              "body",
              Map.of("id", "body", "desc-", ""),
              () -> {
                renderMenu();
                renderPaths();
              });
        });
  }

  private void renderMenu() {
    appendTag(
        "aside",
        Map.of("class", "nav"),
        () -> appendTag("h1", api.info().title() + " " + api.info().version()));
    appendTag(
        "aside",
        Map.of("class", "filters"),
        () -> {
          appendTag("h5", "HTTP Methods");
          renderToggleButton("GET", "GET", "get-");
          renderToggleButton("POST", "POST", "post-");
          renderToggleButton("PUT", "PUT", "put-");
          renderToggleButton("PATCH", "PATCH", "patch-");
          renderToggleButton("DELETE", "DELETE", "delete-");

          appendTag("h5", "HTTP Status");
          renderToggleButton("200", "status200", "status200-");
          renderToggleButton("201", "status201", "status201-");
          renderToggleButton("202", "status202", "status202-");
          renderToggleButton("204", "status204", "status204-");

          appendTag("h5", "HTTP Content-Type");
          renderToggleButton("JSON", "json", "json-");
          renderToggleButton("XML", "xml", "xml-");
          renderToggleButton("CSV", "csv", "csv-");
          renderToggleButton("Other", "other", "other-");

          appendTag("h5", "Content");
          renderToggleButton("&#127481; full texts", "desc", "desc-");
          renderToggleButton("deprecated", "dep", "dep-");
        });
  }

  private void renderToggleButton(String text, String className, String toggle) {
    String js = "document.getElementById('body').toggleAttribute('" + toggle + "')";
    appendTag("button", Map.of("onclick", js, "class", className), () -> appendPlain(text));
  }

  private void renderPaths() {
    List<PackageItem> packages = getPackages();
    appendTag(
        "section",
        Map.of("class", "domains"),
        () -> {
          for (PackageItem pkg : packages) {
            appendTag(
                "details",
                () -> {
                  appendTag(
                      "summary",
                      () ->
                          appendTag(
                              "h2",
                              () -> {
                                appendPlain(toWords(pkg.domain()));
                                appendTag(
                                    "a",
                                    Map.of(
                                        "target",
                                        "_blank",
                                        "href",
                                        "/api/openapi/openapi.html?domain=" + pkg.domain),
                                    "&#x1F5D7;");
                              }));
                  for (GroupItem group : pkg.groups().values()) {
                    appendTag(
                        "section",
                        Map.of("class", "paths"),
                        () -> {
                          appendTag(
                              "details",
                              Map.of("open", ""),
                              () -> {
                                appendTag("summary", () -> appendTag("h3", group.group()));
                                group.operations().forEach(this::renderOperation);
                              });
                        });
                  }
                });
          }
        });
  }

  private static String toWords(String camelCase) {
    return camelCase.replaceAll(
        "(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])", " ");
  }

  private void renderOperation(OperationObject op) {
    if (!op.exists()) return;
    appendTag(
        "details",
        Map.of(
            "id",
            op.operationId(),
            "class",
            op.operationMethod().toUpperCase() + " op " + (op.deprecated() ? "dep" : "")),
        () -> {
          String summary = op.summary();
          appendTag(
              "summary",
              Map.of("title", summary == null ? "" : summary),
              () -> renderOperationSummary(op));
          appendTag("header", markdownToHTML(op.description(), op.parameterNames()));
          renderParameters(op);
        });
  }

  private void renderOperationSummary(OperationObject op) {
    String method = op.operationMethod().toUpperCase();
    String path = op.operationPath();
    List<String> responseCodes = op.responses().keys().toList();
    String successCode =
        responseCodes.stream().filter(code -> code.startsWith("2")).findFirst().orElse("?");
    Set<String> mediaTypes =
        op.responses().values().flatMap(r -> r.content().keys()).collect(toUnmodifiableSet());
    Set<String> subTypes =
        mediaTypes.stream()
            .map(type -> type.substring(type.indexOf('/') + 1))
            .collect(toUnmodifiableSet());

    appendTag(
        "code",
        Map.of("class", "http content"),
        () -> {
          appendTag("span", Map.of("class", subTypes.contains("json") ? "on" : ""), "JSON");
          appendTag("span", Map.of("class", subTypes.contains("xml") ? "on" : ""), "XML");
          appendTag("span", Map.of("class", subTypes.contains("csv") ? "on" : ""), "CSV");
          appendTag(
              "span",
              Map.of(
                  "class", subTypes.stream().anyMatch(t -> !t.matches("xml|json|csv")) ? "on" : ""),
              "*");
        });
    appendTag("code", Map.of("class", "http status" + successCode.charAt(0) + "xx"), successCode);
    appendTag("code", Map.of("class", "http method"), method);
    String url =
        path.replaceAll("/(\\{[^/]+)(?<=})(?=/|$)", "/<em>$1</em>")
            .replaceAll("#([a-zA-Z0-9_]+)", "<small>#<span>$1</span></small>");
    appendTag("code", Map.of("class", "url path"), url);
    List<ParameterObject> queryParams = op.parameters(Api.Parameter.In.QUERY);
    if (!queryParams.isEmpty()) {
      String query = "?";
      List<String> requiredParams =
          queryParams.stream().filter(ParameterObject::required).map(p -> p.name() + "=").toList();
      query += String.join("&", requiredParams);
      if (queryParams.size() > requiredParams.size()) query += "…";
      appendTag("code", Map.of("class", "url query secondary"), query);
    }
    // TODO render nicely: appendTag("code", responseCodes.toString());
    appendTag("a", Map.of("href", "#" + op.operationId(), "class", "permalink"), "permalink");
  }

  private void renderParameters(OperationObject op) {
    JsonList<ParameterObject> params = op.parameters();
    if (params.isUndefined() || params.isEmpty()) return;

    renderParameters(op, Api.Parameter.In.PATH, "/.../");
    renderParameters(op, Api.Parameter.In.QUERY, "?...");
    renderParameters(op, Api.Parameter.In.BODY, "{...}");
  }

  private void renderParameters(OperationObject op, Api.Parameter.In in, String text) {
    List<ParameterObject> params = op.parameters(in);
    if (params.isEmpty()) return;
    appendTag(
        "h4",
        () ->
            appendTag(
                "code",
                Map.of(
                    "class", "url secondary", "title", "Parameters in " + in.name().toLowerCase()),
                text));
    Set<String> parameterNames = op.parameterNames();
    appendTag(
        "dl",
        () ->
            params.stream()
                .map(ParameterObject::resolve)
                .forEach(p -> renderParameter(p, parameterNames)));
  }

  private void renderParameter(ParameterObject p, Set<String> parameterNames) {
    String css = "param";
    if (p.deprecated()) css += " dep";
    if (p.required()) css += " required";
    SchemaObject schema = p.schema();
    Runnable type =
        schema.isShared()
            ? () -> {
              appendTag("a", Map.of("href", "#" + schema.getSharedName()), schema.getSharedName());
            }
            : () -> appendPlain(schema.$type());
    appendTag(
        "dt", Map.of("class", css), () -> appendTag("code", Map.of("class", "url"), p.name()));
    appendTag("dd", () -> appendTag("code", Map.of("class", "type"), type));
    JsonValue defaultValue = p.$default();
    appendTag(
        "dd",
        () ->
            appendTag(
                "code",
                Map.of("class", "value"),
                defaultValue.exists() ? defaultValue.toJson() : ""));
    String description = markdownToHTML(p.description(), parameterNames);
    if (description == null || description.isEmpty()) description = " "; // force the dd tag
    appendTag("dd", Map.of("class", "desc"), description);
  }

  private void appendTag(String name, String text) {
    appendTag(name, Map.of(), text);
  }

  private void appendTag(String name, Map<String, String> attributes, String text) {
    if (text != null && !text.isEmpty()) appendTag(name, attributes, () -> appendPlain(text));
  }

  private void appendTag(String name, Runnable body) {
    appendTag(name, Map.of(), body);
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

  private void appendNonemptyTag(String name, Runnable body) {
    // TODO write the tag
    // render body
    // check if that appended anything, if so complete, otherwise undo the opening tag and
    // attributes
  }

  private static final Set<String> ATTR_NAMES_IGNORE_WHEN_EMPTY = Set.of("class", "title");

  private void appendAttr(String name, String value) {
    if (name == null || name.isEmpty()) return;
    boolean emptyValue = value == null || value.isEmpty();
    if (emptyValue && ATTR_NAMES_IGNORE_WHEN_EMPTY.contains(name))
      return; // optimisation to prevent rendering `class` without a value
    out.append(' ').append(name);
    if (!emptyValue) out.append('=').append('"').append(value).append('"');
  }

  private void appendPlain(String text) {
    out.append(text);
  }
}
