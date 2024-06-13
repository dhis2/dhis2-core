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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.webapi.openapi.OpenApiObject.OperationObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.ParameterObject;
import org.hisp.dhis.webapi.openapi.OpenApiObject.PathItemObject;
import org.intellij.lang.annotations.Language;

/**
 * A tool that can take a OpenAPI JSON document and render it as HTML.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
@RequiredArgsConstructor
public class OpenApiRenderer {

  @Language("css")
  private static final String CSS =
      """
          :root {
            --bg-page: #f1f1f9;
            --bg-open: white;
            --color-delete: #F72585;
            --color-patch: #7209B7;
            --color-post: #3F37C9;
            --color-put: #560BAD;
            --color-options: #B5179E;
            --color-get: #4895EF;
            --color-trace: #4CC9F0;
            --color-head: #3A0CA3;
          }
          html {
            background-color: var(--bg-open);
            height: 100%;
          }
          body {
            background-color: var(--bg-page);
            margin: 0;
            padding-right: 50px;
            min-height: 100%;
            font-family: Inter, sans-serif;
            font-size: 16px;
            text-rendering: optimizespeed;
          }
          body[desc-] p {
            display: none;
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
              background-color: #c5e3fc;
              width: 100%;
              height: 60px;
              box-sizing: border-box;
              padding: 10px;
              text-align: center;
              border-bottom: 5px solid #147cd7;
              background-image: url('/../favicon.ico');
              background-repeat: no-repeat;
              padding-left: 100px;
              background-position: 5px 5px;
          }
          .filters {
               position: fixed;
               top: 55px;
               left: 50%;
               margin-left: -300px;
               width: 600px;
               text-align: center;
               display: inline-block;
               background-color: #c5e3fc;
               padding: 5px;
               border: 4px solid #147cd7;
               border-top-width: 0;
           }
          .domains {
            padding-top: 65px;
            max-width: 100rem;
          }
          .domains > details {
            margin-bottom: 5px;
          }
          .domains > details > summary {
               padding: 5px 0 5px 20px;
               display: inline-block;
               width: 240px;
               background-color: var(--bg-open);
          }
          details > summary:after {
              content: '+';
              float: right;
              font-weight: bold;
              font-family: monospace;
              margin-right: 1rem;
          }
          details[open] > summary:after {
            content: '-';
          }
          .domains > details[open] {
            border-top: 4px solid #2196f3;
          }
          .domains > details[open] > summary {
               background-color: #2196f3;
               color: white;
               margin-top: 0;
          }
          details.op {
              padding: 5px 0;
              margin: 2px 0;
          }
          details.op, .domains > details {
            background-color: var(--bg-page);
          }
          details.op[open], .domains > details[open] {
            background-color: var(--bg-open);
          }
          details > summary {
              list-style-type: none;
              cursor: pointer;
          }
          .paths {
            margin-left: 260px;
          }
          details.op > summary > code:first-child {
            width: 6em;
            display: inline-block;
            padding: 0 0.5em;
            border-width: 0 0 0 4px;
            border-style: solid;
          }
          .GET > summary > code:first-child, button.GET { border-color: var(--color-get); color: var(--color-get); }
          .POST > summary > code:first-child, button.POST { border-color: var(--color-post); color: var(--color-post); }
          .PUT > summary > code:first-child, button.PUT { border-color: var(--color-put); color: var(--color-put); }
          .PATCH > summary > code:first-child, button.PATCH { border-color: var(--color-patch); color: var(--color-patch); }
          .DELETE > summary > code:first-child, button.DELETE { border-color: var(--color-delete); color: var(--color-delete); }
          .OPTIONS > summary > code:first-child { border-color: var(--color-options); color: var(--color-options); }
          .HEAD > summary > code:first-child { border-color: var(--color-head); color: var(--color-head); }
          .TRACE > summary > code:first-child { border-color: var(--color-trace); color: var(--color-trace); }
          #body[get-] details.GET,
          #body[post-] details.POST,
          #body[put-] details.PUT,
          #body[patch-] details.PATCH,
          #body[delete-] details.DELETE { display: none; }

          button {
              border: none;
              background-color: transparent;
              font-weight: bold;
              display: inline-block;
              border-left: 4px solid transparent;
              cursor: pointer;
              margin-left: 1em;
          }
          #body[get-] button.GET,
          #body[post-] button.POST,
          #body[put-] button.PUT,
          #body[patch-] button.PATCH,
          #body[delete-] button.DELETE,
          #body[desc-] button.desc { text-decoration: line-through; color: #777777 }

          details[open] > summary {

          }
          dl {
            margin: 0.5em 0;
          }
          dt {
              margin: 0.5em 0;
          }
          header, p {
            line-height: 1.5em;
          }
          dt code {
            padding: 0.125em 0.5em;
            background-color: antiquewhite;
          }
          """;

  /*
  Reorganizing...
   */
  record DomainItem(String domain, Map<String, GroupItem> groups) {}

  record GroupItem(String group, List<OperationItem> operations) {}

  record OperationItem(String path, String method, OperationObject operation) {}

  private List<DomainItem> getDomainSections() {
    Map<String, DomainItem> domains = new TreeMap<>();
    BiConsumer<String, OperationObject> add =
        (path, op) -> {
          if (!op.exists()) return;
          String domain = op.x_domain();
          String group = op.x_group();
          String method = op.path().substring(op.path().lastIndexOf('.') + 1).toUpperCase();
          domains
              .computeIfAbsent(domain, d -> new DomainItem(d, new TreeMap<>()))
              .groups()
              .computeIfAbsent(group, g -> new GroupItem(g, new ArrayList<>()))
              .operations()
              .add(new OperationItem(path, method, op));
        };
    api.paths()
        .forEach(
            (path, item) -> {
              add.accept(path, item.get());
              add.accept(path, item.post());
              add.accept(path, item.put());
              add.accept(path, item.patch());
              add.accept(path, item.delete());
              add.accept(path, item.head());
              add.accept(path, item.trace());
              add.accept(path, item.options());
            });
    return List.copyOf(domains.values());
  }

  /*
  Rendering...
   */

  public static String render(String json) {
    OpenApiRenderer html = new OpenApiRenderer(JsonValue.of(json).as(OpenApiObject.class));
    return html.render();
  }

  private final OpenApiObject api;
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
                appendTag("title", "DHIS2 OpenAPI");
                appendTag("link", Map.of("rel", "icon", "href", "./favicon.ico"), null);
                appendTag("style", CSS);
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
    appendTag("aside", Map.of("class", "nav"), () -> appendPlain("DHIS2 API Documentation"));
    appendTag(
        "aside",
        Map.of("class", "filters"),
        () -> {
          appendPlain("\uD83C\uDF13 Filters ");
          renderToggleButton("&#128195; descriptions", "desc", "desc-");
          renderToggleButton("GET", "GET", "get-");
          renderToggleButton("POST", "POST", "post-");
          renderToggleButton("PUT", "PUT", "put-");
          renderToggleButton("PATCH", "PATCH", "patch-");
          renderToggleButton("DELETE", "DELETE", "delete-");
        });
  }

  private void renderToggleButton(String text, String className, String toggle) {
    String js = "document.getElementById('body').toggleAttribute('" + toggle + "')";
    appendTag("button", Map.of("onclick", js, "class", className), () -> appendPlain(text));
  }

  private void renderPaths() {
    List<DomainItem> domains = getDomainSections();
    if (true) {
      appendTag(
          "section",
          Map.of("class", "domains"),
          () -> {
            for (DomainItem domain : domains) {
              appendTag(
                  "details",
                  () -> {
                    appendTag("summary", domain.domain());
                    for (GroupItem group : domain.groups().values()) {
                      appendTag(
                          "section",
                          Map.of("class", "paths"),
                          () -> {
                            appendTag(
                                "details",
                                Map.of("open", ""),
                                () -> {
                                  appendTag("summary", group.group());
                                  for (OperationItem op : group.operations()) {
                                    renderOperation(op.path(), op.method(), op.operation());
                                  }
                                });
                          });
                    }
                  });
            }
          });
    } else {
      appendTag(
          "section", Map.of("class", "paths"), () -> api.paths().forEach(this::renderPathItem));
    }
  }

  private void renderPathItem(String path, PathItemObject item) {
    renderOperation(path, "GET", item.get());
    renderOperation(path, "POST", item.post());
    renderOperation(path, "DELETE", item.delete());
    renderOperation(path, "PATCH", item.patch());
    renderOperation(path, "HEAD", item.head());
    renderOperation(path, "TRACE", item.trace());
    renderOperation(path, "OPTIONS", item.options());
  }

  private void renderOperation(String path, String method, OperationObject op) {
    if (!op.exists()) return;
    appendTag(
        "details",
        Map.of("class", method + " op"),
        () -> {
          appendTag(
              "summary",
              () -> {
                appendTag("code", method);
                appendTag("code", path);
              });
          appendTag("header", op.description());
          renderParameters(op);
        });
  }

  private void renderParameters(OperationObject op) {
    JsonList<ParameterObject> params = op.parameters();
    if (params.isUndefined() || params.isEmpty()) return;
    appendTag(
        "details",
        Map.of("open", ""),
        () -> {
          appendTag("summary", "Parameters");
          appendTag(
              "dl",
              () ->
                  params.stream()
                      .map(ParameterObject::resolve)
                      .forEach(
                          p -> {
                            appendTag("dt", () -> appendTag("code", p.name()));
                            appendTag("dd", () -> appendTag("p", p.description()));
                          }));
        });
  }

  private void appendTag(String name, String text) {
    if (text != null && !text.isEmpty()) appendTag(name, () -> appendPlain(text));
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

  private void appendAttr(String name, String value) {
    out.append(' ').append(name);
    if (value != null && !value.isEmpty()) out.append('=').append('"').append(value).append('"');
  }

  private void appendPlain(String text) {
    out.append(text);
  }
}
