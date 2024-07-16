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

  @keyframes squareToCircle {
      0% { transform: rotate(0deg); }
      25% { transform: rotate(45deg); }
      50% { transform: rotate(90deg); }
      75% { transform: rotate(45deg); }
      100% { transform: rotate(0deg); }
  }

  :root {
       --bg-page: white;
       --percent-op-bg-summary: 20%;
       --percent-op-bg-aside: 10%;
       --p-op-bg: 15%;
       --color-delete: tomato;
       --color-patch: darkolivegreen;
       --color-post: seagreen;
       --color-put: chocolate;
       --color-options: rosybrown;
       --color-get: steelblue;
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
  nav > summary { margin: 1em 0 0.5em 0; font-weight: normal; font-size: 85%; }

  h2 a[target="_blank"] { text-decoration: none; margin-left: 0.5em; }
  a[href^="#"] { text-decoration: none; }
  a[title="permalink"] { position: absolute;  right: 1em; display: inline-block; width: 24px; height: 24px;
    text-align: center; vertical-align: middle; border-radius: 50% 50% 50% 0; line-height: 24px; color: dimgray; }

  code {
    font-family: "Liberation Mono", monospace;
  }
  ul { list-style-type: none; margin: 0; padding-left: 1rem; }
  ul > li { margin-top: 0.75rem; }
  summary {
      padding: 2px;
      margin-top: 0.5em;
  }
  body > header:first-of-type {
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
      z-index: 100;
  }
  nav {
        position: fixed;
        top: 60px;
        width: 250px;
        text-align: left;
        display: inline-block;
        padding-right: 1rem;
        box-sizing: border-box;
  }
  body > section { margin-left: 250px; padding-top: 65px; padding-bottom: 1em; position: relative; }
  body > section > details { margin-top: 10px; }
  body > section > details > summary { padding: 0.5em 1em; }
  body > section h2:before { content: '⛁'; margin-right: 0.5rem; color: dimgray; }

  body > section details {
    margin-left: 2rem;
  }
  body > nav details {
    padding: 0.5rem 0 0 1rem;
  }
  body > nav > details > summary {
      background-color: #147cd7;
      color: snow;
      margin-left: -1rem;
      padding-left: 1rem;
  }
  body > section details > summary:before {
      content: '⊕';
      float: left;
      margin-left: calc(-1rem - 10px);
  }
  body > section details[open] > summary:before {
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
  code.http.content { margin-top: -2px; color: #aaa; display: inline-grid; grid-template-columns: 1fr 1fr 1fr; vertical-align: top; text-align: center; }
  code.http.content > span { font-size: 70%; font-weight: normal; padding: 0 0.25em; margin: 1px; }
  code.http.content > span.status4xx { background: color-mix(in srgb, tomato 75%, transparent); color: snow; }
  code.http.content > span.status2xx { background: color-mix(in srgb, seagreen 75%, transparent); color: snow; }
  code.http.content .on { color: black; }
  code.http.method { width: 4rem; text-align: right; color: dimgray; }
  code.md { background-color: #eee; }
  code.url { padding: 0.25em 0.5em; background-color: snow; }
  code.url.path { font-weight: bold; }
  code.url em, code.url.secondary { color: lightslategrey; font-style: normal; font-weight: normal; background: color-mix(in srgb, snow 70%, transparent); }
  code.url small { color: gray; }
  code.property { color: dimgray; margin-left: 2em; }
  code.property > span + span { color: darkmagenta; padding: 0.25em; background: color-mix(in srgb, ivory 65%, transparent); }
  code.secondary.type { color: dimgray; font-style: italic; }
  code.url.secondary + code.url.secondary { padding-left: 0; }
  code.request, code.response { padding: 0.25em 0.5em; color: dimgray; }
  code.mime { background-color: ivory; font-style: italic; padding: 0.25em 0.5em; }
  code.mime.secondary { background: color-mix(in srgb, ivory 70%, transparent); }

  code.status { padding: 0.25em 0.5em; font-weight: bold; }
  code.status2xx { background: color-mix(in srgb, seagreen 70%, transparent); color: snow; }
  code.status4xx { background: color-mix(in srgb, tomato 70%, transparent); color: snow; }

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

  /* target highlighting */
  details.op:target > summary > code.url.path { background-color: gold; }
  details.param:target > summary > code:first-of-type { background-color: gold; }
  details.response:target > summary > code:first-of-type { background-color: gold; color: inherit; }
  details:target > summary > a[title="permalink"] { background-color: gold; color: black; border: 2px solid snow;
    animation: squareToCircle 2s 1s infinite alternate; }

  /* opration background colors */
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

  /* operation visibility filters */
  #body[get-] .op.GET,
  #body[post-] .op.POST,
  #body[put-] .op.PUT,
  #body[patch-] .op.PATCH,
  #body[delete-] .op.DELETE,
  #body[status200-] .op.status200,
  #body[status201-] .op.status201,
  #body[status202-] .op.status202,
  #body[status204-] .op.status204,
  #body[json-] .op.json,
  #body[xml-] .op.xml,
  #body[csv-] .op.csv,
  #body[alpha-] .op.alpha,
  #body[beta-] .op.beta,
  #body[stable-] .op.stable,
  #body[open-] .op.open,
  #body[deprecated-] .op.deprecated,
  #body[request-] .op > summary > code.request,
  #body[response-] .op > summary > code.response,
  #body[content-] .op > summary > code.http.content { display: none; }

  nav button {
      border: none;
      background-color: transparent;
      font-weight: bold;
      border-left: 4px solid transparent;
      cursor: pointer;
      display: inline;
      margin: 2px;
  }
  nav button:before { content: '🞕'; margin-right: 0.5rem;font-weight: normal; }
  nav button.deprecated { background-color: var(--color-dep); }

  details.op button { background-color: #444; color: white; border: none; cursor: pointer; }

  #body[get-] button.GET:before,
  #body[post-] button.POST:before,
  #body[put-] button.PUT:before,
  #body[patch-] button.PATCH:before,
  #body[delete-] button.DELETE:before,
  #body[status200-] button.status200:before,
  #body[status201-] button.status201:before,
  #body[status202-] button.status202:before,
  #body[status204-] button.status204:before,
  #body[json-] button.json:before,
  #body[xml-] button.xml:before,
  #body[csv-] button.csv:before,
  #body[alpha-] button.alpha:before,
  #body[beta-] button.beta:before,
  #body[stable-] button.stable:before,
  #body[open-] button.open:before,
  #body[deprecated-] button.deprecated:before,
  #body[request-] button.request:before,
  #body[response-] button.response:before,
  #body:not([desc-]) button.desc:before,
  #body[content-] button.content:before { content: '🞏'; color: dimgray; }


  /* ~~~ highlights and annotations from inserted symbols ~~ */
  /* path markers */
  .op.alpha > summary > code.url.path:before { content: '🧪'; padding: 0.25em; }
  .op.beta > summary > code.url.path:before { content: '🔧'; padding: 0.25em; }
  .op.stable > summary > code.url.path:before { content: '🛡️'; padding: 0.25em; }
  .op.deprecated > summary > code.url.path:before,
   nav code.deprecated:before { content: '⚠️'; padding: 0.25em; font-family: sans-serif; }
  /* parameter markers */
  .param.required code.url:first-of-type:after { content: '*'; color: tomato; }
  .param.deprecated > summary > code.url:first-of-type:before { content: '⚠️'; font-family: sans-serif; display: inline-block; padding-right: 0.25rem; }
  /* +/- buttons for expand/collapse */
  .op aside > button.toggle:after { content: '⊝'; padding-left: 0.5rem; font-size: 16px; }
  .op:has(details[data-open]) aside > button.toggle:after { content: '⊕'; }

  article.desc { margin: 10px 30px 0 30px; color: #333; } /* note: margin is in pixels as the font-size changes */
  article.desc > p { margin: 0 0 10px 0; }
  article.desc > *:first-child { margin-top: 10px; }
  article.desc a[target="_blank"]:after { content: '🗗'; }
  body[desc-] article.desc:not(:hover) { font-size: 0.1rem; }
  body[desc-] article.desc:not(:hover):first-line { font-size: 1rem; }

  /* tooltips */
  .param.deprecated > summary > code.url:first-of-type:hover:after {
    content: 'This parameter is deprecated';
    position: absolute; background: var(--color-tooltip); color: var(--color-tooltiptext); padding: 0.25rem 0.5rem; }
  .op.deprecated > summary > code.url:hover:after {
    content: 'This operation is deprecated';
    position: absolute; background: var(--color-tooltip); color: var(--color-tooltiptext); padding: 0.25rem 0.5rem; }
  .op.alpha > summary > code.url.path:hover:after {
    content: 'This operation is alpha, consider it an experiment 🙀';
    position: absolute; background: var(--color-tooltip); color: var(--color-tooltiptext); padding: 0.25rem 0.5rem; }
  .op.beta > summary > code.url.path:hover:after {
    content: 'This operation is beta and still subject to change 😾';
    position: absolute; background: var(--color-tooltip); color: var(--color-tooltiptext); padding: 0.25rem 0.5rem; }
  .op.stable > summary > code.url.path:hover:after {
    content: 'This operation is stable 😸🎉';
    position: absolute; background: var(--color-tooltip); color: var(--color-tooltiptext); padding: 0.25rem 0.5rem; }

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
              Map.of("id", "body"),
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
          renderMenuGroup(
              "Filters",
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
                            "status status2xx status" + statusCode,
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
                    });

                renderMenuItem(
                    "Content",
                    () -> {
                      renderToggleButton("@Deprecated", "deprecated", "deprecated-", true);
                    });
              });

          renderMenuGroup(
              "View",
              () -> {
                renderMenuItem(
                    "Summaries",
                    () -> {
                      renderToggleButton("Show request info", "request", "request-", false);
                      renderToggleButton("Show response info", "response", "response-", false);
                      renderToggleButton("Show content info", "content", "content-", false);
                    });
                renderMenuItem(
                    "Details",
                    () -> renderToggleButton("Abbr. Descriptions", "desc", "desc-", false));
              });
        });
  }

  private void renderMenuGroup(String title, Runnable renderBody) {
    appendDetails(
        null,
        true,
        "",
        () -> {
          appendSummary(null, "", () -> appendRaw(title));
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

  private void renderPaths() {
    List<PackageItem> packages = groupPackages();
    appendTag("section", () -> packages.forEach(this::renderPathPackage));
  }

  private void renderPathPackage(PackageItem pkg) {
    String id = "-" + pkg.domain();
    appendDetails(
        id,
        false,
        "",
        () -> {
          appendSummary(
              id,
              null,
              () ->
                  appendTag(
                      "h2",
                      () -> {
                        appendRaw(toWords(pkg.domain()));
                        appendA(
                            "/api/openapi/openapi.html?domain=" + pkg.domain, true, "&#x1F5D7;");
                      }));
          pkg.groups().values().forEach(this::renderPathGroup);
        });
  }

  private void renderPathGroup(GroupItem group) {
    String id = "-" + group.domain() + "-" + group.group();
    appendDetails(
        id,
        true,
        "paths",
        () -> {
          appendSummary(
              id, null, () -> appendTag("h3", Map.of("class", group.group()), group.group()));
          group.operations().forEach(this::renderOperation);
        });
  }

  private static String toWords(String camelCase) {
    return camelCase.replaceAll(
        "(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])", " ");
  }

  private void renderOperation(OperationObject op) {
    if (!op.exists()) return;
    String id = op.operationId();
    appendDetails(
        id,
        false,
        operationStyle(op),
        () -> {
          appendSummary(id, op.summary(), () -> renderOperationSummary(op));
          renderOperationToolbar(op);
          appendTag("header", markdownToHTML(op.description(), op.parameterNames()));
          renderParameters(op);
          renderRequestBody(op);
          renderResponses(op);
        });
  }

  private static String operationStyle(OperationObject op) {
    StringBuilder style = new StringBuilder("op");
    style.append(" ").append(op.operationMethod().toUpperCase());
    style.append(" status").append(op.responseSuccessCode());
    for (String mime : op.responseMediaSubTypes()) style.append(" ").append(mime);
    if (op.deprecated()) style.append(" deprecated");
    String maturity = op.x_maturity();
    style.append(" ").append(maturity == null ? "open" : maturity);
    return style.toString();
  }

  private void renderOperationToolbar(OperationObject op) {
    Map<String, String> attrs =
        Map.ofEntries(
            entry("onclick", "toggleDescriptions(this)"),
            entry("title", "remembering expand/collapse"),
            entry("class", "toggle"));
    appendTag("aside", () -> appendTag("button", attrs, "All"));
  }

  private void renderOperationSummary(OperationObject op) {
    String method = op.operationMethod().toUpperCase();
    String path = op.operationPath();

    renderMediaSubTypesIndicator(op.responseMediaSubTypes());
    appendCode(
        "http content",
        () ->
            op.responseCodes().forEach(code -> appendSpan("status" + code.charAt(0) + "xx", code)));
    appendCode("http method", method);
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
      renderSchemaSummary("request secondary", request);
      appendCode("request secondary", "}");
    }
    List<SchemaObject> successOneOf = op.responseSuccessSchemas();
    if (!successOneOf.isEmpty()) {
      appendCode("response secondary", "::");
      renderSchemaSummary("response secondary", successOneOf);
    }
  }

  private void renderMediaSubTypesIndicator(Collection<String> subTypes) {
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
    String id = op.operationId() + "-" + p.name();
    appendDetails(
        id,
        true,
        style,
        () -> {
          appendSummary(id, null, () -> renderParameterSummary(p));
          String description = markdownToHTML(p.description(), parameterNames);
          appendTag("article", Map.of("class", "desc"), description);
        });
  }

  private void renderParameterSummary(ParameterObject p) {
    SchemaObject schema = p.schema();
    appendCode("url", p.name());
    appendCode("url secondary", "=");
    renderSchemaDetail("url secondary", schema);

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
    String id = op.operationId() + "-";
    appendDetails(
        id,
        true,
        style,
        () -> {
          appendSummary(id, null, () -> renderMediaTypes(requestBody.content()));
          String description = markdownToHTML(requestBody.description(), parameterNames);
          appendTag("article", Map.of("class", "desc"), description);
        });
  }

  private void renderMediaTypes(JsonMap<MediaTypeObject> mediaTypes) {
    if (mediaTypes.isUndefined()) return;
    if (mediaTypes.size() == 1) {
      renderMediaType(mediaTypes.entries().toList().get(0));
    } else {
      appendTag(
          "ul", () -> mediaTypes.entries().forEach(e -> appendTag("li", () -> renderMediaType(e))));
    }
  }

  private void renderMediaType(Map.Entry<String, MediaTypeObject> mediaType) {
    renderMediaType(mediaType.getKey(), mediaType.getValue().schema());
  }

  private void renderMediaType(String mediaType, SchemaObject type) {
    renderMediaType(mediaType, () -> renderSchemaDetail("mime secondary", type.resolve()));
  }

  private void renderMediaTypeSummary(List<SchemaObject> oneOf) {
    renderMediaType("*", () -> renderSchemaSummary("mime secondary", oneOf));
  }

  private void renderMediaType(String mediaType, Runnable renderType) {
    appendCode("mime", mediaType);
    appendCode("mime secondary", ":");
    renderType.run();
    appendRaw("<br/>");
  }

  private void renderResponses(OperationObject op) {
    JsonMap<ResponseObject> responses = op.responses();
    if (responses.isUndefined() || responses.isEmpty()) return;

    renderOperationSectionHeader("::", "Responses");
    responses.entries().forEach(e -> renderResponse(op, e.getKey(), e.getValue()));
  }

  private void renderResponse(OperationObject op, String code, ResponseObject response) {
    String id = op.operationId() + "!" + code;
    appendDetails(
        id,
        code.charAt(0) == '2',
        "response",
        () -> {
          appendSummary(id, null, () -> renderResponseSummary(code, response));
          JsonMap<MediaTypeObject> content = response.content();
          if (!content.isUndefined() && content.size() > 1) renderMediaTypes(content);
          String description = markdownToHTML(response.description(), op.parameterNames());
          appendTag("article", Map.of("class", "desc"), description);
        });
  }

  private void renderResponseSummary(String code, ResponseObject response) {
    String name = statusCodeName(Integer.parseInt(code));
    appendCode("status status" + code.charAt(0) + "xx status" + code, code + " " + name);
    appendCode("mime", "=");
    JsonMap<MediaTypeObject> content = response.content();

    if (content.isUndefined()) return;
    if (content.size() == 1) {
      renderMediaTypes(content);
    } else if (response.isUniform()) {
      renderMediaType("*", content.values().toList().get(0).schema());
    } else {
      // * : a | b | c
      renderMediaTypeSummary(content.values().map(MediaTypeObject::schema).toList());
    }
  }

  private void renderSchemaSummary(String style, List<SchemaObject> oneOf) {
    if (oneOf.isEmpty()) return;
    renterSchemaDescriptor(style, () -> renderTypeDescriptor(oneOf, true));
  }

  private void renderSchemaDetail(String style, SchemaObject schema) {
    renterSchemaDescriptor(style, () -> renderTypeDescriptor(schema, false));
  }

  private void renderSchemaSummary(String style, SchemaObject schema) {
    renterSchemaDescriptor(style, () -> renderTypeDescriptor(schema, true));
  }

  private void renterSchemaDescriptor(String style, Runnable renderType) {
    appendCode(
        style + " type",
        () -> {
          appendRaw("&lt;");
          renderType.run();
          appendRaw("&gt;");
        });
  }

  private void renderTypeDescriptor(List<SchemaObject> oneOf, boolean summary) {
    for (int i = 0; i < oneOf.size(); i++) {
      if (i > 0) appendRaw(" | ");
      renderTypeDescriptor(oneOf.get(0), summary);
    }
  }

  private void renderTypeDescriptor(SchemaObject schema, boolean summary) {
    if (schema.isShared()) {
      appendA("#" + schema.getSharedName(), false, schema.getSharedName());
      return;
    }
    String type = schema.$type();
    if (type == null) {
      SchemaObject resolved = schema.resolve();
      if (resolved.isShared()) {
        renderTypeDescriptor(resolved, summary);
        return;
      }
    }
    if ("array".equals(type)) {
      appendRaw("array[");
      renderTypeDescriptor(schema.items(), summary);
      appendRaw("]");
      return;
    }
    if ("object".equals(type)) {
      appendRaw("object");
      if ((schema.properties().isUndefined() || schema.properties().isEmpty())
          && schema.additionalProperties().exists()) {
        // this is a "map" where any not namely specified property has a certain same schema
        appendRaw(":");
        renderTypeDescriptor(schema.additionalProperties(), summary);
      }
      // TODO else: need to detail the object in full
      return;
    }
    appendRaw(type);
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
          if (id != null) appendA("#" + id, false, "#");
          body.run();
        });
  }

  private void appendA(String href, boolean blank, String text) {
    String target = blank ? "_blank" : "";
    String title = "#".equals(text) ? "permalink" : "";
    appendTag("a", Map.of("href", href, "target", target, "title", title), text);
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

  private static String statusCodeName(int code) {
    return switch (code) {
      case 200 -> "Ok";
      case 201 -> "Created";
      case 202 -> "Accepted";
      case 203 -> "Not Authoritative";
      case 204 -> "No Content";
      case 205 -> "Reset Content";
      case 206 -> "Partial Content";
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
