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
import static java.util.Map.entry;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.webapi.openapi.OpenApiMarkdown.markdownToHTML;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonNodeType;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.webapi.openapi.OpenApiObject.MediaTypeObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.OperationObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.ParameterObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.RequestBodyObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.ResponseObject;
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
       --percent-op-bg-summary: 30%;
       --percent-op-bg-aside: 15%;
       --p-op-bg: 20%;
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
  h1 { margin: 0.5rem; color: rgb(33, 41, 52); font-size: 110%; font-weight: normal; text-align: left; }
  h2 { display: inline; font-size: 110%; font-weight: normal; }
  h3 { font-size: 105%; display: inline; text-transform: capitalize; font-weight: normal; }
  h4 { font-weight: normal; padding: 0 1em; }
  h5 { margin: 1em 0 0.5em 0; font-weight: normal; }

  h2 a[target="_blank"] { float: right; text-decoration: none; }
  a[href^="#"] { text-decoration: none; }
  .op > summary > a {
       float: right;
       visibility: hidden;
       text-decoration: none;
  }
  .op > summary > a:after {
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
  body > header:first-child {
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
  nav {
        position: fixed;
        top: 60px;
        width: 220px;
        text-align: left;
        display: inline-block;
        background-color: aliceblue;
        padding: 1rem;
        box-sizing: border-box;
  }
  body > section { margin-left: 250px; padding-top: 65px; padding-bottom: 1em; }
  body > section > details { margin-top: 10px; }
  body > section > details > summary { padding: 0.5em 1em; }
  body > section h2:before { content: '⛁'; margin-right: 0.5rem; color: dimgray; }

  details {
    margin-left: 2rem;
  }
  details > summary:before {
      content: '⊕';
      float: left;
      margin-left: calc(-1rem - 10px);
  }
  details[open] > summary:before {
    content: '⊝';
  }
  body > section > details[open] > summary {
       margin-top: 0;
  }
  body > section > details[open] > summary h2 {
        font-weight: bold;
  }
  details > summary {
      list-style-type: none;
      cursor: pointer;
  }
  details.op[open] { padding-bottom: 1rem; }
  details.op > summary { padding: 0.5rem; }
  details.op > header { padding: 0.5rem 1rem; font-size: 95%; }
  details.op > aside { padding: 0.5rem 1rem; }

  /* colors and emphasis effects */
  code.http { display: inline-block; padding: 0 0.5em; font-weight: bold; }
  code.http.content { color: #aaa; display: inline-grid; grid-template-columns: 1fr 1fr; vertical-align: top; }
  code.http.content > span { font-size: 70%; font-weight: normal; padding-right: 1em; }
  code.http.content.error > span { color: tomato; min-width: 1.8em;}
  code.http.content .on { color: black; }
  code.http.method { width: 4rem; text-align: right; color: dimgray; }
  code.http.status2xx { color: seagreen;  }
  code.http.status2xx:after { content: '⮠'; color: #666; font-weight: normal; padding-left: 0.5rem; }
  code.md { background-color: #eee; }
  code.url { padding: 0.25em 0.5em; background-color: snow; }
  code.url.path { font-weight: bold; }
  code.url em, code.url.secondary { color: lightslategrey; font-style: normal; font-weight: normal; background: color-mix(in srgb, snow 70%, transparent); }
  code.url small { color: gray; }
  code.property { color: dimgray; margin-left: 2em; }
  code.property > span + span { color: darkmagenta; padding: 0.25em; background: color-mix(in srgb, ivory 65%, transparent); }
  code.url.secondary.type { color: dimgray; font-style: italic; }
  code.url.secondary + code.url.secondary { padding-left: 0; }
  code.mime { background-color: ivory; font-style: italic; padding: 0.25em 0.5em; margin-bottom: 0.25em; display: inline-block; }
  code.mime.secondary { background: color-mix(in srgb, ivory 70%, transparent); }

  .response code.status { padding: 0.125em 0.25em; font-weight: bold; }
  .response.status2xx code.status { background: color-mix(in srgb, seagreen 70%, transparent); color: snow; }
  .response.status4xx code.status { background: color-mix(in srgb, tomato 70%, transparent); color: snow; }

  .deprecated summary > code.url { background-color: var(--color-dep); color: #666; }
  .deprecated summary > code.url.secondary { background: color-mix(in srgb, var(--color-dep) 70%, transparent); }

  .op:not([open]) code.url small > span { font-size: 2px; }
  .op:not([open]) code.url small:hover > span { font-size: inherit; }

  .GET > summary, button.GET { background: color-mix(in srgb, var(--color-get) var(--percent-op-bg-summary), transparent); }
  .POST > summary, button.POST { background: color-mix(in srgb, var(--color-post) var(--percent-op-bg-summary), transparent); }
  .PUT > summary, button.PUT { background: color-mix(in srgb, var(--color-put) var(--percent-op-bg-summary), transparent); }
  .PATCH > summary, button.PATCH { background: color-mix(in srgb, var(--color-patch) var(--percent-op-bg-summary), transparent); }
  .DELETE > summary, button.DELETE { background: color-mix(in srgb, var(--color-delete) var(--percent-op-bg-summary), transparent); }
  .OPTIONS > summary { background: color-mix(in srgb, var(--color-options) var(--percent-op-bg-summary), transparent); }
  .HEAD > summary { background: color-mix(in srgb, var(--color-head) var(--percent-op-bg-summary), transparent); }
  .TRACE > summary { background: color-mix(in srgb, var(--color-trace) var(--percent-op-bg-summary), transparent); }
  details.op:target > summary { border-bottom: 5px solid gold; }

  details[open].GET { background: color-mix(in srgb, var(--color-get) var(--p-op-bg), transparent); }
  details[open].POST { background: color-mix(in srgb, var(--color-post) var(--p-op-bg), transparent); }
  details[open].PUT { background: color-mix(in srgb, var(--color-put) var(--p-op-bg), transparent); }
  details[open].PATCH { background: color-mix(in srgb, var(--color-patch) var(--p-op-bg), transparent); }
  details[open].DELETE { background: color-mix(in srgb, var(--color-delete) var(--p-op-bg), transparent); }

  details[open].GET > aside { background: color-mix(in srgb, var(--color-get) var(--percent-op-bg-aside), transparent); }
  details[open].POST > aside { background: color-mix(in srgb, var(--color-post) var(--percent-op-bg-aside), transparent); }
  details[open].PUT > aside { background: color-mix(in srgb, var(--color-put) var(--percent-op-bg-aside), transparent); }
  details[open].PATCH > aside { background: color-mix(in srgb, var(--color-patch) var(--percent-op-bg-aside), transparent); }
  details[open].DELETE > aside { background: color-mix(in srgb, var(--color-delete) var(--percent-op-bg-aside), transparent); }

  #body[get-] details.GET,
  #body[post-] details.POST,
  #body[put-] details.PUT,
  #body[patch-] details.PATCH,
  #body[delete-] details.DELETE,
  #body[deprecated-] details.deprecated { display: none; }

  nav button {
      border: none;
      background-color: transparent;
      font-weight: bold;
      border-left: 4px solid transparent;
      cursor: pointer;
      display: inline;
      margin: 2px;
  }
  nav button:before {
      content: '🞕';
      margin-right: 0.5rem;
      font-weight: normal;
  }
  nav button.deprecated { background-color: var(--color-dep); }

  details.op button { background-color: #444; color: white; border: none; cursor: pointer; }

  #body[get-] button.GET:before,
  #body[post-] button.POST:before,
  #body[put-] button.PUT:before,
  #body[patch-] button.PATCH:before,
  #body[delete-] button.DELETE:before,
  #body[deprecated-] button.deprecated:before,
  #body[desc-] button.desc:before { content: '🞏'; color: dimgray; }


  .param.required code.url:first-child:after {
    content: '*';
    color: tomato;
  }
  .param.deprecated > summary > code.url:first-child:before {
      content: '⚠️';
      font-family: sans-serif; /* reset font */
      display: inline-block; /* reset text-decorations */
      padding-right: 0.25rem;
  }
  .param.deprecated > summary > code.url:first-child:hover:after {
    content: 'This parameter is deprecated';
    position: absolute;
    background: var(--color-tooltip);
    color: var(--color-tooltiptext);
    padding: 0.25rem 0.5rem;
  }
  .op.deprecated > summary > code.url:hover:after {
    content: 'This operation is deprecated';
    position: absolute;
    background: var(--color-tooltip);
    color: var(--color-tooltiptext);
    padding: 0.25rem 0.5rem;
  }
  .op aside > button.toggle:after { content: '⇱'; padding-left: 0.5rem; }
  .op:has(details[data-open]) aside > button.toggle:after { content: '⇲'; }

  article.desc { margin: 10px 30px 0 30px; color: #333; } /* note: margin is in pixels as the font-size changes */
  article.desc > p { margin: 0 0 10px 0; }
  article.desc > *:first-child { margin-top: 10px; }
  article.desc a[target="_blank"]:after { content: '🗗'; }
  body[desc-] article.desc:not(:hover) { font-size: 0.1rem; }
  body[desc-] article.desc:not(:hover):first-line { font-size: 1rem; }
  """;

  @Language("js")
  private static final String GLOBAL_JS =
      """
  function toggleDescriptions(element) {
    let allDetails = element.closest('details.op').querySelectorAll('details');
    const isRestore = Array.from(allDetails.values()).some(e => e.hasAttribute('data-open'));
    const set = isRestore ? 'open' : 'data-open';
    const remove = isRestore ? 'data-open' : 'open';
    allDetails.forEach(details => {
        if (details.hasAttribute(remove)) {
            details.setAttribute(set, '');
            details.removeAttribute(remove);
        }
    });
  }
  """;

  /*
  Reorganizing...
   */
  record PackageItem(String domain, Map<String, GroupItem> groups) {}

  record GroupItem(String domain, String group, List<OperationObject> operations) {}

  private static final Comparator<OperationObject> SORT_BY_METHOD =
      comparing(OperationObject::operationMethod)
          .thenComparing(OperationObject::operationPath)
          .thenComparing(OperationObject::operationId);

  private List<PackageItem> groupPackages() {
    Map<String, PackageItem> packages = new TreeMap<>();
    Consumer<OperationObject> add =
        op -> {
          String domain = op.x_package();
          String group = op.x_group();
          packages
              .computeIfAbsent(domain, pkg -> new PackageItem(pkg, new TreeMap<>()))
              .groups()
              .computeIfAbsent(group, g -> new GroupItem(domain, g, new ArrayList<>()))
              .operations()
              .add(op);
        };
    api.operations().forEach(add);
    if (params.sortEndpointsByMethod)
      packages
          .values()
          .forEach(p -> p.groups().values().forEach(g -> g.operations().sort(SORT_BY_METHOD)));
    return List.copyOf(packages.values());
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
    appendRaw("<!doctype html>");
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
                renderPageHeader();
                renderPageMenu();
                renderPaths();
              });
        });
  }

  private void renderPageHeader() {
    appendTag("header", () -> appendTag("h1", api.info().title() + " " + api.info().version()));
  }

  private void renderPageMenu() {
    appendTag(
        "nav",
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
          renderToggleButton("&#127299; full texts", "desc", "desc-");
          renderToggleButton("deprecated", "deprecated", "deprecated-");
        });
  }

  private void renderToggleButton(String text, String className, String toggle) {
    String js = "document.getElementById('body').toggleAttribute('" + toggle + "')";
    appendTag("button", Map.of("onclick", js, "class", className), () -> appendRaw(text));
  }

  private void renderPaths() {
    List<PackageItem> packages = groupPackages();
    appendTag("section", () -> packages.forEach(this::renderPathPackage));
  }

  private void renderPathPackage(PackageItem pkg) {
    appendDetails(
        "-" + pkg.domain(),
        false,
        "",
        () -> {
          renderPathGroupSummary(pkg);
          pkg.groups().values().forEach(this::renderPathGroup);
        });
  }

  private void renderPathGroupSummary(PackageItem pkg) {
    appendSummary(
        null,
        () ->
            appendTag(
                "h2",
                () -> {
                  appendRaw(toWords(pkg.domain()));
                  appendA("/api/openapi/openapi.html?domain=" + pkg.domain, true, "&#x1F5D7;");
                }));
  }

  private void renderPathGroup(GroupItem group) {
    appendDetails(
        "-" + group.domain() + "-" + group.group(),
        true,
        "paths",
        () -> {
          appendSummary(null, () -> appendTag("h3", Map.of("class", group.group()), group.group()));
          group.operations().forEach(this::renderOperation);
        });
  }

  private static String toWords(String camelCase) {
    return camelCase.replaceAll(
        "(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])", " ");
  }

  private void renderOperation(OperationObject op) {
    if (!op.exists()) return;
    String style = op.operationMethod().toUpperCase() + " op";
    if (op.deprecated()) style += " deprecated";
    appendDetails(
        op.operationId(),
        false,
        style,
        () -> {
          String summary = op.summary();
          appendSummary(summary, () -> renderOperationSummary(op));
          renderOperationToolbar(op);
          appendTag("header", markdownToHTML(op.description(), op.parameterNames()));
          renderParameters(op);
          renderRequestBody(op);
          renderResponses(op);
        });
  }

  private void renderOperationToolbar(OperationObject op) {
    Map<String, String> attrs =
        Map.ofEntries(
            entry("onclick", "toggleDescriptions(this)"),
            entry("title", "show/hide description text"),
            entry("class", "toggle"));
    appendTag("aside", () -> appendTag("button", attrs, "&#127299;"));
  }

  private void renderOperationSummary(OperationObject op) {
    String method = op.operationMethod().toUpperCase();
    String path = op.operationPath();
    List<String> responseCodes = op.responses().keys().toList();
    String successCode =
        responseCodes.stream().filter(code -> code.startsWith("2")).findFirst().orElse("?");
    List<String> errorCodes =
        responseCodes.stream().filter(code -> code.startsWith("4")).sorted().toList();
    Set<String> subTypes =
        getMediaSubTypes(op.responses().values().flatMap(r -> r.content().keys()));

    renderMediaSubTypesIndicator(subTypes);
    appendCode("http status" + successCode.charAt(0) + "xx", successCode);
    appendCode(
        "http error content",
        () -> {
          for (String errorCode : errorCodes) appendSpan("http status" + errorCode, errorCode);
          if (errorCodes.isEmpty()) appendSpan(" ");
        });
    appendCode("http method", method);
    appendCode("url path", getUrlPathInSections(path));
    List<ParameterObject> queryParams = op.parameters(ParameterObject.In.query);
    if (!queryParams.isEmpty()) {
      String query = "?";
      List<String> requiredParams =
          queryParams.stream().filter(ParameterObject::required).map(p -> p.name() + "=").toList();
      query += String.join("&", requiredParams);
      if (queryParams.size() > requiredParams.size()) query += "…";
      appendCode("url query secondary", query);
    }
    appendA("#" + op.operationId(), false, "permalink");
  }

  private static Set<String> getMediaSubTypes(Stream<String> mediaTypes) {
    return mediaTypes
        .map(type -> type.substring(type.indexOf('/') + 1))
        .collect(toUnmodifiableSet());
  }

  private void renderMediaSubTypesIndicator(Set<String> subTypes) {
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
    JsonList<ParameterObject> params = op.parameters();
    if (params.isUndefined() || params.isEmpty()) return;

    // TODO header and cookie (if we need it)
    renderParameters(op, ParameterObject.In.path, "/.../");
    renderParameters(op, ParameterObject.In.query, "?...");
  }

  private void renderParameters(OperationObject op, ParameterObject.In in, String text) {
    List<ParameterObject> params = op.parameters(in);
    if (params.isEmpty()) return;
    renderOperationSectionHeader(text, "Parameters in " + in.name().toLowerCase());
    Set<String> parameterNames = op.parameterNames();
    params.stream()
        .map(ParameterObject::resolve)
        .forEach(p -> renderParameter(op, p, parameterNames));
  }

  private void renderParameter(OperationObject op, ParameterObject p, Set<String> parameterNames) {
    String style = "param";
    if (p.deprecated()) style += " deprecated";
    if (p.required()) style += " required";
    appendDetails(
        op.operationId() + "-" + p.name(),
        true,
        style,
        () -> {
          appendSummary(null, () -> renderParameterSummary(p));
          String description = markdownToHTML(p.description(), parameterNames);
          appendTag("article", Map.of("class", "desc"), description);
        });
  }

  private void renderParameterSummary(ParameterObject p) {
    SchemaObject schema = p.schema();
    appendCode("url", p.name());
    appendCode("url secondary", "=");
    renderSchemaType(schema, "url");

    JsonValue defaultValue = p.$default();
    if (defaultValue.exists()) {
      String value =
          defaultValue.type() == JsonNodeType.STRING
              ? defaultValue.as(JsonString.class).string()
              : defaultValue.toJson();
      renderProperty("default", value);
    }
    if (!schema.isShared()) {
      renderProperty("format", schema.format());
      renderProperty("minLength", schema.minLength());
      renderProperty("maxLength", schema.maxLength());
      renderProperty("pattern", schema.pattern());
      renderProperty("enum", schema.$enum());
    }
  }

  private void renderProperty(String name, Object value) {
    if (value == null) return;
    if (value instanceof Collection<?> l && l.isEmpty()) return;
    String str = value.toString();
    appendCode(
        "property",
        () -> {
          appendSpan(name + ":");
          appendSpan(str);
        });
  }

  private void renderRequestBody(OperationObject op) {
    RequestBodyObject requestBody = op.requestBody();
    if (requestBody.isUndefined()) return;
    renderOperationSectionHeader("{...}", "Request Body");
    String style = "requests";
    if (requestBody.required()) style += " required";
    Set<String> parameterNames = op.parameterNames();
    appendDetails(
        op.operationId() + "-",
        true,
        style,
        () -> {
          appendSummary(null, () -> requestBody.content().entries().forEach(this::renderMediaType));
          String description = markdownToHTML(requestBody.description(), parameterNames);
          appendTag("article", Map.of("class", "desc"), description);
        });
  }

  private void renderMediaType(Map.Entry<String, MediaTypeObject> mediaType) {
    appendCode("mime", mediaType.getKey());
    appendCode("mime secondary", "=");
    renderSchemaType(mediaType.getValue().schema().resolve(), "mime");
    appendRaw("<br/>");
  }

  private void renderResponses(OperationObject op) {
    JsonMap<ResponseObject> responses = op.responses();
    if (responses.isUndefined() || responses.isEmpty()) return;

    renderOperationSectionHeader("&#11168;...", "Responses");
    responses.entries().forEach(e -> renderResponse(op, e.getKey(), e.getValue()));
  }

  private void renderResponse(OperationObject op, String code, ResponseObject response) {
    String style = "response status" + code.charAt(0) + "xx status" + code;
    appendDetails(
        op.operationId() + "!",
        code.charAt(0) == '2',
        style,
        () -> {
          appendSummary(null, () -> renderResponseSummary(code, response));
          response.content().entries().forEach(this::renderMediaType);
          String description = markdownToHTML(response.description(), op.parameterNames());
          appendTag("article", Map.of("class", "desc"), description);
        });
  }

  private void renderResponseSummary(String code, ResponseObject response) {
    appendCode("status", code);
    renderMediaSubTypesIndicator(getMediaSubTypes(response.content().keys()));
  }

  private void renderSchemaType(SchemaObject schema, String style) {
    Runnable type =
        schema.isShared()
            ? () -> {
              appendRaw("&lt;");
              appendA("#" + schema.getSharedName(), false, schema.getSharedName());
              appendRaw("&gt;");
            }
            : () -> appendRaw("&lt;" + schema.$type() + "&gt;");

    appendCode(style + " secondary type", type);
  }

  private void appendDetails(String id, boolean open, String style, Runnable body) {
    Map<String, String> attrs =
        Map.of("class", style, "id", id == null ? "" : id, open ? "open" : "", "");
    appendTag("details", attrs, body);
  }

  private void appendSummary(String title, Runnable body) {
    Map<String, String> attrs = Map.of("title", title == null ? "" : title);
    appendTag("summary", attrs, body);
  }

  private void appendA(String href, boolean blank, String text) {
    appendTag("a", Map.of("href", href, "target", blank ? "_blank" : ""), text);
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
    if (!emptyValue) out.append('=').append('"').append(value).append('"');
  }

  private void appendRaw(String text) {
    out.append(text);
  }
}
