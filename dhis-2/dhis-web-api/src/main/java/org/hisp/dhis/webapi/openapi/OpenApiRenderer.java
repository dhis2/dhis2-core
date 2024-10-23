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
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonNodeType;
import org.hisp.dhis.jsontree.JsonObject;
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
  @OpenApi.Shared
  public static class OpenApiRenderingParams {
    boolean sortEndpointsByMethod = true; // make this a sortEndpointsBy=method,path,id thing?

    @OpenApi.Description(
        "Include the JSON source in the operations and schemas (for debug purposes)")
    boolean source = false;

    @OpenApi.Description(
        """
      Values of a shared enum with less than the limit values will show the first n values up to limit
      directly where the type is used""")
    int inlineEnumsLimit = 0;
  }

  @Language("css")
  private static final String CSS =
      """
  @import url('https://fonts.googleapis.com/css2?family=Mulish:ital,wght@0,200..1000;1,200..1000&family=Noto+Sans+Mono:wght@100..900&display=swap');

  @keyframes spin { 0% { transform: rotate(360deg); } 100% { transform: rotate(0deg); } }

  :root {
       --bg-page: white;
       --percent-op-bg-summary: 20%;
       --percent-op-bg-aside: 10%;
       --p-op-bg: 15%;
       --color-delete: tomato;
       --color-patch: mediumorchid;
       --color-post: seagreen;
       --color-put: darkcyan;
       --color-options: rosybrown;
       --color-get: steelblue;
       --color-trace: palevioletred;
       --color-head: thistle;
       --color-dep: khaki;
       --color-schema: slategray;
       --color-tooltip: #444;
       --color-tooltiptext: #eee;
       --color-tooltipborder: lightgray;
       --color-target: black;
       --width-nav: 320px;
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
    font-family: "Mulish", sans-serif;
    font-size: 16px;
    text-rendering: optimizespeed;
  }
  h1 { margin: 0.5rem; color: rgb(33, 41, 52); font-size: 110%; text-align: left; }
  h2 { display: inline; font-size: 110%; font-weight: normal; text-transform: capitalize; }
  h3 { font-size: 105%; display: inline-block; text-transform: capitalize; font-weight: normal; min-width: 21rem; margin: 0; }

  h4 { font-weight: normal; padding: 0 1em; }
  nav > summary { margin: 1em 0 0.5em 0; font-weight: normal; font-size: 85%; }

  h2 a[target="_blank"] { text-decoration: none; margin-left: 0.5em; }
  a[href^="#"] { text-decoration: none; }
  a[title="permalink"] { position: absolute;  right: 1em; display: inline-block; width: 24px; height: 24px;
    text-align: center; vertical-align: middle; border-radius: 50%; line-height: 24px; color: dimgray; margin-top: -0.125rem; }
  #hotkeys a { color: black; display: block; margin-top: 2px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; font-size: 75%; }
  #hotkeys a:visited { color: black; }

  pre { background-color: floralwhite; color: #222; margin-right: 2em; padding: 0.5rem; }
  pre, code { font-family: "Noto Sans Mono", "Liberation Mono", monospace; }
  code + b { padding: 0 0.5em; }

  kbd {
      background-color: #eee;
      border-radius: 3px;
      border: 1px solid #b4b4b4;
      box-shadow: 0 1px 1px rgba(0, 0, 0, 0.2), 0 2px 0 0 rgba(255, 255, 255, 0.7) inset;
      color: #333;
      display: inline-block;
      font-size: 0.85em;
      font-weight: 700;
      line-height: 1;
      padding: 2px 4px;
      white-space: nowrap;
  }

  summary {
      padding: 2px;
      margin-top: 0.5em;
  }
  body > nav > header {
      width: 100%;
      height: 60px;
      box-sizing: border-box;
      padding: 10px;
      text-align: center;
      border-bottom: 5px solid #147cd7;
      background-color: snow;
      background-image: url('/../favicon.ico');
      background-repeat: no-repeat;
      padding-left: 60px;
      background-position: 5px 5px;
  }
  nav {
        position: fixed;
        width: var(--width-nav);
        text-align: left;
        display: inline-block;
        padding-right: 1rem;
        box-sizing: border-box;
  }
  body > section { margin-left: var(--width-nav); padding-top: 65px; padding-bottom: 1em; position: relative; }
  body > section > details { margin-top: 10px; }
  body > section > details > summary { padding: 0.5em 1em; }
  body > section h2:before { content: '⛁'; margin-right: 0.5rem; color: dimgray; }
  body > section .kind h2:before { content: '⎐';  }

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
  body > section details:not(.button) > summary:before {
      content: '⊕';
      float: left;
      margin-left: calc(-1rem - 10px);
  }
  body > section details > summary:last-child:before { content: ''; }

  body > section details[open]:not(.button) > summary:before { content: '⊝'; }

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
  details.op[open], details.schema[open] { padding-bottom: 1rem; }
  details.op > summary, details.schema > summary { padding: 0.5rem; }
  details > header { padding: 0.5rem 1rem; font-size: 95%; }
  details > aside { padding: 0.5rem 1rem; margin-bottom: 1em; }

  /* colors and emphasis effects */
  code.http { display: inline-block; padding: 0 0.5em; font-weight: bold; }
  code.http.content { margin-top: -2px; color: #aaa; display: inline-grid; grid-template-columns: 1fr 1fr 1fr; vertical-align: top; text-align: center; }
  code.http.content > span { font-size: 70%; font-weight: normal; padding: 0 0.25em; margin: 1px; }
  code.http.content > span.status4xx { background: color-mix(in srgb, tomato 75%, transparent); color: snow; }
  code.http.content > span.status2xx { background: color-mix(in srgb, seagreen 75%, transparent); color: snow; }
  code.http.content .on { color: black; }
  code.http.method { width: 4rem; text-align: right; color: dimgray; }
  .desc code { background: color-mix(in srgb, snow 70%, transparent); padding: 0.125em 0.25em; }
  code.property { padding: 0.25em 0.5em; background: color-mix(in srgb, powderblue 70%, transparent); }
  code.property.secondary, code.property.secondary ~ code.type { background: color-mix(in srgb, lemonchiffon 70%, transparent); }
  code.url, .desc code.keyword { padding: 0.25em 0.5em; background-color: snow; }
  code.url.path { font-weight: bold; }
  code.url em, code.url.secondary, code.url.secondary + code.type { color: darkslateblue; font-style: normal; font-weight: normal; background: color-mix(in srgb, snow 70%, transparent); }
  code.url small { color: gray; }
  code.tag { color: dimgray; margin-left: 2em; }
  code.tag > span + span { color: darkblue; padding: 0.25em; background: color-mix(in srgb, lemonchiffon 65%, transparent); }
  code.tag.columns { display: inline-block; padding-left: 100px; }
  code.tag.columns > span:first-of-type { margin-left: -100px; padding-right: 1em; }
  code.secondary ~ code.type { color: darkslateblue; padding: 0.25em 0.5em; }
  code.url.secondary + code.url.secondary { padding-left: 0; }
  code.request, code.response { padding: 0.25em 0.5em; color: dimgray; font-weight: bold; }
  code.mime { background-color: ivory; font-style: italic; padding: 0.25em 0.5em; }
  code.mime.secondary, code.mime.secondary + code.type { background: color-mix(in srgb, ivory 70%, transparent); }

  code.status { padding: 0.25em 0.5em; font-weight: bold; }
  code.status2xx { background: color-mix(in srgb, seagreen 70%, transparent); color: snow; }
  code.status4xx { background: color-mix(in srgb, tomato 70%, transparent); color: snow; }

  .deprecated summary > code.url { background-color: var(--color-dep); color: #666; }
  .deprecated summary > code.url.secondary { background: color-mix(in srgb, var(--color-dep) 70%, transparent); }

  .schema > summary > code.type { min-width: 20em; display: inline-block; }

  .op:not([open]) code.url small > span { font-size: 2px; }
  .op:not([open]) code.url small:hover > span { font-size: inherit; }

  .GET > summary, button.GET, code.GET { background: color-mix(in srgb, var(--color-get) var(--percent-op-bg-summary), transparent); }
  .POST > summary, button.POST, code.POST { background: color-mix(in srgb, var(--color-post) var(--percent-op-bg-summary), transparent); }
  .PUT > summary, button.PUT, code.PUT { background: color-mix(in srgb, var(--color-put) var(--percent-op-bg-summary), transparent); }
  .PATCH > summary, button.PATCH, code.PATCH { background: color-mix(in srgb, var(--color-patch) var(--percent-op-bg-summary), transparent); }
  .DELETE > summary, button.DELETE, code.DELETE { background: color-mix(in srgb, var(--color-delete) var(--percent-op-bg-summary), transparent); }
  .OPTIONS > summary, code.OPTIONS { background: color-mix(in srgb, var(--color-options) var(--percent-op-bg-summary), transparent); }
  .HEAD > summary, code.HEAD { background: color-mix(in srgb, var(--color-head) var(--percent-op-bg-summary), transparent); }
  .TRACE > summary, code.TRACE { background: color-mix(in srgb, var(--color-trace) var(--percent-op-bg-summary), transparent); }
  .schema > summary { background: color-mix(in srgb, var(--color-schema) var(--percent-op-bg-summary), transparent); }


  /* target highlighting */
  details.op:target > summary,
  details.schema:target > summary,
  details.param:target > summary > code:first-of-type,
  details.property:target > summary > code:first-of-type,
  details.request:target > summary > code:first-of-type,
  details.response:target > summary > code:first-of-type,
  details.source:target > pre {  border: 2px solid var(--color-target); }

  details:target > summary > a[title="permalink"] { background-color: var(--color-target); color: snow; border: 2px solid snow;
    animation: spin 2s linear 0s infinite reverse; font-weight: bold; }

  /* operation background colors */
  details[open].GET { background: color-mix(in srgb, var(--color-get) var(--p-op-bg), transparent); }
  details[open].POST { background: color-mix(in srgb, var(--color-post) var(--p-op-bg), transparent); }
  details[open].PUT { background: color-mix(in srgb, var(--color-put) var(--p-op-bg), transparent); }
  details[open].PATCH { background: color-mix(in srgb, var(--color-patch) var(--p-op-bg), transparent); }
  details[open].DELETE { background: color-mix(in srgb, var(--color-delete) var(--p-op-bg), transparent); }
  details[open].schema { background: color-mix(in srgb, var(--color-schema) var(--p-op-bg), transparent); }

  details[open].GET > aside { background: color-mix(in srgb, var(--color-get) var(--percent-op-bg-aside), transparent); }
  details[open].POST > aside { background: color-mix(in srgb, var(--color-post) var(--percent-op-bg-aside), transparent); }
  details[open].PUT > aside { background: color-mix(in srgb, var(--color-put) var(--percent-op-bg-aside), transparent); }
  details[open].PATCH > aside { background: color-mix(in srgb, var(--color-patch) var(--percent-op-bg-aside), transparent); }
  details[open].DELETE > aside { background: color-mix(in srgb, var(--color-delete) var(--percent-op-bg-aside), transparent); }
  details[open].schema > aside { background: color-mix(in srgb, var(--color-schema) var(--percent-op-bg-aside), transparent); }

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
  #body[request-] .op > summary > code.request + code.type,
  #body[response-] .op > summary > code.response,
  #body[response-] .op > summary > code.response + code.type,
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

  details.box aside button,
  details.box aside .button { background-color: #444; color: white; border: none; display: inline-block;
    cursor: pointer; margin: 0 1em 0 0; padding: 0.25em 0.5em; font-size: 90%;
    box-shadow: 1px 1px 0 0, 2px 2px 0 0, 3px 3px 0 0, 4px 4px 0 0, 5px 5px 0 0;}
  details.box aside .button > summary { display: inline-block; margin: 0; padding: 0; }
  details.box aside .button a { display: block; }
  details.box aside .button div { position: absolute; background-color: floralwhite; padding: 0.5em 1em; margin-top: 3px;
    box-shadow: 5px 5px #444444aa; max-height: 20em; overflow-y: scroll; }

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
  .required > summary > code:first-of-type { font-weight: bold; }
  .required > summary > code:first-of-type:after { content: '*'; color: tomato; }
  .deprecated > summary > code:first-of-type:before { content: '⚠️'; font-family: sans-serif; display: inline-block; padding-right: 0.25rem; }
  /* +/- buttons for expand/collapse */
  .box aside > button.toggle { pointer-events: none; opacity: 0.65; }
  .box aside > button.toggle:after { content: '⊝'; padding-left: 0.5rem; }
  .box:has(details[data-open]) aside > button.toggle:after { content: '⊕'; }
  .box:has(details[open]) aside > button.toggle, .box:has(details[data-open]) aside > button.toggle { pointer-events: inherit; opacity: inherit; }
  /* schema type markers */
  .schema > summary > code:first-of-type:before { padding: 0 1rem; color: dimgray; }
  .schema.object > summary > code:first-of-type:before { content: '{}'; }
  .schema.array > summary > code:first-of-type:before { content: '[]'; }
  .schema.string > summary > code:first-of-type:before { content: '""'; }
  .schema.number > summary > code:first-of-type:before { content: '#.'; }
  .schema.boolean > summary > code:first-of-type:before { content: '01'; }

  article.desc { margin: 0.25em 2.5em; color: #333; } /* note: margin is in pixels as the font-size changes */
  article.desc > p { margin: 0 0 0.5em 0; }
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
  function openToggleDown1(element) {
    const allDetails = element.closest('details.box').querySelectorAll('details');
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

  function openRecursiveUp(element) {
    while (element != null && element.tagName.toLowerCase() === 'details') {
      element.setAttribute('open', '');
      element = element.parentElement.closest('details');
    }
  }

  function addUrlParameter(name, value) {
    const searchParams = new URLSearchParams(window.location.search)
    searchParams.set(name, value)
    window.location.search = searchParams.toString()
  }

  function addHashHotkey() {
    const id = 'hk_'+location.hash.substring(1);
    if (document.getElementById(id) != null) return;
    const a = document.createElement('a');
    a.appendChild(document.createTextNode(" + "))
    const hotkeys = document.getElementById("hotkeys");
    hotkeys.appendChild(a);
    const fn = document.createElement('kbd');
    let n = hotkeys.childNodes.length-1;
    if (n > 9) {
      hotkeys.firstChild.nextSibling.remove();
      n = 1;
    }
    fn.appendChild(document.createTextNode(""+ n));
    fn.id = 'hk'+n;
    a.appendChild(fn);
    a.appendChild(document.createTextNode(" "+location.hash+" "));
    a.href = location.hash;
    a.id = id;
    a.title = location.hash;
  }

  function schemaUsages(details) {
    if (details.getElementsByTagName('div').length > 0) {
      return;
    }
    // fill...
    const id = details.parentNode.closest('details').id;
    const links = document.querySelectorAll('section a[href="#'+id+'"]');
    const box = document.createElement('div');
    details.appendChild(box);
    const targets = Array.from(links).map(node => node.closest('[id]'));
    const uniqueTargets = targets.filter((t, index) => targets.indexOf(t) === index);
    uniqueTargets.forEach(target => {
      if (target.id !== id) {
        const a = document.createElement('a');
        a.appendChild(document.createTextNode(target.id));
        a.href = '#'+target.id;
        box.appendChild(a);
      }
    });
  }

  window.addEventListener('hashchange', (e) => {
      openRecursiveUp(document.getElementById(location.hash.substring(1)));
      addHashHotkey();
    }, false);

  document.addEventListener("DOMContentLoaded", (e) => {
    if (!location.hash) return;
    openRecursiveUp(document.getElementById(location.hash.substring(1)));
  });

  window.addEventListener('keydown', function(e) {
    if (e.ctrlKey && e.key.match(/[0-9]/)) {
      var t = document.getElementById("hk"+e.key);
      if (t != null)  {
        t.parentElement.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
        e.stopPropagation();
        e.preventDefault();
      }
    }
  });
  """;

  /*
  Reorganizing...
   */
  record PackageItem(String domain, Map<String, GroupItem> groups) {}

  record GroupItem(String domain, String group, List<OperationObject> operations) {}

  record KindItem(String kind, List<SchemaObject> schemas) {}

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

  private List<KindItem> groupKinds() {
    Map<String, KindItem> kinds = new TreeMap<>();
    api.components()
        .schemas()
        .forEach(
            (name, schema) -> {
              String kind = schema.x_kind();
              kinds
                  .computeIfAbsent(kind, key -> new KindItem(key, new ArrayList<>()))
                  .schemas()
                  .add(schema);
            });
    return List.copyOf(kinds.values());
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
                renderPageMenu();
                renderPaths();
                renderComponentsSchemas();
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
          renderPageHeader();
          renderMenuGroup(
              "hotkeys",
              () -> {
                appendTag("kbd", "Ctrl");
                appendRaw(" Hotkeys");
              },
              () -> {});
          renderMenuGroup(
              null,
              () -> appendRaw("Filters"),
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
                      renderToggleButton("@Deprecated", "deprecated", "deprecated-", true);
                    });
              });

          renderMenuGroup(
              null,
              () -> appendRaw("View"),
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
    // TODO bring back tags as actual tags usable as filters
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
          appendSummary(id, null, () -> renderPathPackageSummary(pkg));
          pkg.groups().values().forEach(this::renderPathGroup);
        });
  }

  private void renderPathPackageSummary(PackageItem pkg) {
    appendTag(
        "h2",
        () -> {
          appendRaw(toWords(pkg.domain()));
          appendA("/api/openapi/openapi.html?domain=" + pkg.domain, true, "&#x1F5D7;");
          appendRaw(" | ");
          appendA(
              "/api/openapi/openapi.html?source=true&domain=" + pkg.domain,
              true,
              "&#x1F5D7; + &#128435;");
        });
  }

  private void renderPathGroup(GroupItem group) {
    String id = "-" + group.domain() + "-" + group.group();
    appendDetails(
        id,
        true,
        "paths",
        () -> {
          appendSummary(id, null, () -> renderPathGroupSummary(group));
          group.operations().forEach(this::renderOperation);
        });
  }

  private void renderPathGroupSummary(GroupItem group) {
    appendTag("h3", Map.of("class", group.group()), group.group());

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
          renderBoxToolbar(null);
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
      renderSchemaSignature(request);
      appendCode("request secondary", "}");
    }
    List<SchemaObject> successOneOf = op.responseSuccessSchemas();
    if (!successOneOf.isEmpty()) {
      appendCode("response secondary", "::");
      renderSchemaSignature(successOneOf);
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
      appendA("#" + schema.getSharedName(), false, schema.getSharedName());
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
          for (KindItem kind : groupKinds()) {
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
