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
  @import url('https://fonts.googleapis.com/css2?family=Mulish:ital,wght@0,200..1000;1,200..1000&family=Noto+Sans+Mono:wght@100..900&display=swap');

  @keyframes spin { 0% { transform: rotate(360deg); } 100% { transform: rotate(0deg); } }

  :root {
       --bg-page: white;
       --percent-op-bg-summary: 20%;
       --percent-op-bg-aside: 10%;
       --p-op-bg: 15%;
       --color-delete: tomato;
       --color-patch: orchid;
       --color-post: mediumpurple;
       --color-put: olivedrab;
       --color-options: rosybrown;
       --color-get: royalblue;
       --color-trace: palevioletred;
       --color-head: thistle;
       --color-dep: khaki;
       --color-schema: #4E4F4E;
       --color-tooltip: #444;
       --color-tooltiptext: #eee;
       --color-tooltipborder: lightgray;
       --color-target: blue;
       --width-nav: 360px;
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
    font-family: "JetBrains Mono", monospace;
    font-size: 16px;
    font-weight: 200;
    text-rendering: optimizespeed;
  }
  nav h1 { font-size: 110%; text-align: right; }
  h2 { display: inline; font-size: 110%; font-weight: normal; text-transform: capitalize; }
  h3 { font-size: 105%; display: inline-block; text-transform: capitalize; font-weight: normal; min-width: 21rem; margin: 0; }

  h4 { font-weight: normal; padding: 0 1em; }
  nav > summary { margin: 1em 0 0.5em 0; font-weight: normal; font-size: 85%; }

  h2 a[onclick] { text-decoration: none; margin-right: 2em; float: right; }
  a[href^="#"] { text-decoration: none; }
  a[title="permalink"] { position: absolute;  right: 1em; display: inline-block; width: 24px; height: 24px;
    text-align: center; vertical-align: middle; border-radius: 50%; line-height: 24px; color: dimgray; margin-top: -0.125rem; }
  a:not([href]) { color: blue; cursor: pointer; }
  a.gh { display: inline-block; width: 24px; height: 24px; color: transparent; background-size: 24px 24px;
  background-image: url(data:image/png;base64,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); }
  #hotkeys a { color: #eee; display: block; margin-top: 2px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; font-size: 75%; }
  #hotkeys a:visited { color: #eee; }
  button, input { font-family: inherit; }

  pre { background-color: honeydew; color: #222; margin-right: 2em; padding: 0.5rem;
    font-variant-ligatures: contextual;
    font-feature-settings: "calt";
    overflow: auto;
  }
  pre, code { font-family: "JetBrains Mono", monospace; font-weight: normal; }
  code + b { padding: 0 0.5em; }

  dt { font-weight: bold; margin-top: 0.5em; }
  dd { display: inline-block; }

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
      text-align: right;
      background-image: url(data:image/png;base64,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);
      background-repeat: no-repeat;
      padding-left: 60px;
      background-position: 10px 10px;
  }
  nav {
        position: fixed;
        width: var(--width-nav);
        text-align: left;
        display: inline-block;
        padding-right: 1rem;
        box-sizing: border-box;
        background-color: #4E4F4E;
        color: #ddd;
        min-height: 100%;
  }
  #scope div { display: inline-grid; grid-template-columns: 4fr 6fr 1fr 1fr; padding: 0.25em 0.5em; }
  #scope select { max-width: 140px; }
  #scope label { text-transform: capitalize; }
  #scope input { margin-left: 0.5em; }

  body > section { margin-left: var(--width-nav); position: relative; }
  body > section > header { padding-left: 4rem; padding-bottom: 0.5rem; border-bottom: 7px solid #4E4F4E; color: #666; }
  body > section > header > h1 { margin-top: 0; padding-top: 1em; }
  body > section > details { margin-top: 10px; }
  body > section > details > summary { padding: 0.5em 1em; }
  body > section h2:before { content: '⛁'; margin-right: 0.5rem; color: dimgray; }
  body > section .kind h2:before { content: '⎐';  }
  body > section + section { padding-top: 2em; }

  body > section details {
    margin-left: 2rem;
  }
  body > nav details {
    padding: 0.5rem 0 0 0.5rem;
  }
  body > nav > details > summary {
      background-color: #2A303A;
      color: snow;
      margin-left: -1rem;
      padding-left: 1rem;
      border-radius: 0 4px 4px 0;
  }
  body > section details:not(.button) > summary:before {
      content: '⊕';
      float: left;
      margin-left: calc(-1rem - 10px);
  }
  body > section details.op:not(.button) > summary:before, body > section details.schema:not(.button) > summary:before {
      margin-left: calc(-1rem - 20px);
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
  details.op, details.schema { margin-bottom: 1rem;  border-style: solid; border-width: 0; border-top-width: 2px; }
  details.op[open], details.schema[open] { padding-bottom: 1rem; }
  details.op > summary, details.schema > summary { padding: 0.5rem; padding-left: 0; margin-top: 0; }
  details > header { padding: 0.5rem 1rem; font-size: 95%; }
  details > aside { padding: 0.5rem 1rem; margin-bottom: 0.5rem; margin-left: 5em; background-color: #eee; }

  /* colors and emphasis effects */
  code.http { display: inline-block; padding: 0 0.5em; font-weight: bold; }
  code.http.content { margin-top: -2px; color: #aaa; display: inline-grid; grid-template-columns: 1fr 1fr 1fr; vertical-align: top; text-align: center; }
  code.http.content > span { font-size: 70%; font-weight: normal; padding: 0 0.25em; margin: 1px; }
  code.http.content > span.status4xx { background: color-mix(in srgb, tomato 75%, transparent); color: snow; }
  code.http.content > span.status2xx { color: black; }
  body:not([content-]) code.http.content > span.status2xx { background: color-mix(in srgb, seagreen 75%, transparent); color: snow; }
  code.http.content .on { color: black; }
  code.http.method { width: 4rem; text-align: left; color: white; padding: 5px 0.5em; position: relative; top: -10px; border-radius: 0 0 4px 4px; }
  .desc code { background: color-mix(in srgb, snow 70%, transparent); padding: 0.125em 0.25em; }
  code.property { padding: 0.25em 0.5em; background-color:  #eee; }
  code.property.secondary, code.property.secondary ~ code.type { background-color: #f6f6f6; }
  code.url, .desc code.keyword { padding: 0.25em 0.5em; }
  code.url.path { font-weight: bold; border-radius: 4px; }
  code.url em, code.url.secondary { color: teal; font-style: normal; font-weight: normal; }
  code.url small { color: gray; }
  code.tag { color: dimgray; margin-left: 2em; }
  code.tag > span + span { color: navy; padding: 0.25em; }
  code.tag.columns { display: inline-block; padding-left: 100px; }
  code.tag.columns > span:first-of-type { margin-left: -100px; padding-right: 1em; }
  code.secondary ~ code.type { color: darkmagenta; padding: 0.25em 0.5em; }
  code.url.secondary + code.url.secondary { padding-left: 0; }
  code.request, code.response { padding: 0.25em 0.5em; color: dimgray; font-weight: bold; }
  code.mime { font-style: italic; padding: 0.25em 0.5em; }
  code.mime.secondary, code.mime.secondary + code.type {  }

  code.status { padding: 0.25em 0.5em; }
  code.status2xx { background: color-mix(in srgb, seagreen 10%, transparent); color: seagreen; }
  code.status4xx { background: color-mix(in srgb, tomato 10%, transparent); color: tomato; }

  .deprecated summary > code.url { background-color: var(--color-dep); color: #666; }
  .deprecated summary > code.url.secondary { background: color-mix(in srgb, var(--color-dep) 70%, transparent); }

  .schema > summary > code.type { min-width: 20em; display: inline-block; }

  .op:not([open]) code.url small > span { font-size: 2px; }
  .op:not([open]) code.url small:hover > span { font-size: inherit; }

  .GET, button.GET, code.GET { border-color: var(--color-get); }
  .POST, button.POST, code.POST { border-color: var(--color-post); }
  .PUT, button.PUT, code.PUT { border-color: var(--color-put); }
  .PATCH, button.PATCH, code.PATCH { border-color: var(--color-patch); }
  .DELETE, button.DELETE, code.DELETE { border-color: var(--color-delete); }
  .OPTIONS, code.OPTIONS { border-color: var(--color-options); }
  .HEAD, code.HEAD { border-color: var(--color-head); }
  .TRACE, code.TRACE { border-color: var(--color-trace); }
  .schema { border-color: var(--color-schema); }


  /* target highlighting */
  details.op:target > summary code.url.path,
  details.schema:target > summary code.type,
  details.param:target > summary > code:first-of-type,
  details.property:target > summary > code:first-of-type,
  details.request:target > summary > code:first-of-type,
  details.response:target > summary > code:first-of-type,
  details.source:target > pre {  border: 2px solid var(--color-target); }

  details:target > summary > a[title="permalink"] { background-color: var(--color-target); color: snow; border: 2px solid snow;
    animation: spin 2s linear 0s infinite reverse; font-weight: bold; }

  /* operation background colors */
  details.GET > summary .http.method { background-color: color-mix(in srgb, var(--color-get) 20%, transparent); color: var(--color-get); }
  details.POST > summary .http.method { background-color: color-mix(in srgb, var(--color-post) 20%, transparent); color: var(--color-post); }
  details.PUT > summary .http.method { background-color: color-mix(in srgb, var(--color-put) 20%, transparent); color: var(--color-put); }
  details.PATCH > summary .http.method { background-color: color-mix(in srgb, var(--color-patch) 20%, transparent); color: var(--color-patch); }
  details.DELETE > summary .http.method { background-color: color-mix(in srgb, var(--color-delete) 20%, transparent); color: var(--color-delete); }
  details.schema > summary .http.method { background-color: color-mix(in srgb, var(--color-schema) 20%, transparent); color: var(--color-schema); }

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
  #body[content-] .op > summary > code.http.content:first-of-type,
  #body[content-] .op > summary > code.http.content:not(:first-of-type) span:not(:first-child) { display: none; }
  #body[content-] .op > summary > code.http.content { display: inline-block; }

  nav button {
      border: none;
      background-color: transparent;
      color: #eee;
      font-weight: bold;
      border-left: 4px solid transparent;
      cursor: pointer;
      display: inline;
      margin: 2px;
  }
  nav button:before { content: '🞕'; margin-right: 0.5rem;font-weight: normal; }

  details.box aside button,
  details.box aside .button { background-color: #2A303A; color: white; border: none; display: inline-block;
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
   nav code.deprecated:before { content: '⚠️'; padding: 0.25em; }
  /* parameter markers */
  .required > summary > code:first-of-type { font-weight: bold; }
  .required > summary > code:first-of-type:after { content: '*'; color: tomato; }
  .deprecated > summary > code:first-of-type:before { content: '⚠️'; display: inline-block; padding-right: 0.25rem; }
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
  article.desc > p { margin: 0 0 0.5em 0; font-size: 90%; }
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

  function setLocationPathnameFile(name) {
    const path = window.location.pathname;
    const base = path.substring(0, path.lastIndexOf('/'))
    window.location.pathname = base + '/'+name;
  }

  function setLocationSearch(name, value, blank) {
    const searchParams = new URLSearchParams(window.location.search)
    if (value === '' || value == null) {
      searchParams.delete(name);
    } else {
      searchParams.set(name, value);
    }
    setLocationSearchParams(searchParams, blank);
  }

  function removeLocationSearch(name, value) {
    const searchParams = new URLSearchParams(window.location.search)
    searchParams.delete(name, value);
    setLocationSearchParams(searchParams);
  }

  function modifyLocationSearch(button) {
    const select = button.parentElement.querySelector('select');
    const value = select.value;
    if (value === '') return;
    const key = select.getAttribute("data-key");
    const params = new URLSearchParams(window.location.search);
    const scope = key + ':' + value
    if (button.value === '+') {
      if (!params.has('scope', scope)) params.append('scope', scope);
    } else {
      params.set('scope', scope);
    }
    window.location.hash = '';
    setLocationSearchParams(params);
  }

  function setLocationSearchParams(params, blank) {
    // undo : and / escaping for URL readability
    const search = params.toString().replaceAll('%3A', ':').replaceAll('%2F', '/');
    if (blank) {
      let currentUrl = window.location.href;
      let updatedUrl = new URL(currentUrl);
      updatedUrl.search = search;
      window.open(updatedUrl.href, '_blank');
    } else {
      window.location.search = search;
    }
  }

  function addHashHotkey() {
    const id = 'hk_'+location.hash.substring(1);
    if (document.getElementById(id) != null) return;
    const a = document.createElement('a');
    const hotkeys = document.getElementById("hotkeys");
    hotkeys.appendChild(a);
    const fn = document.createElement('kbd');
    let n = hotkeys.childNodes.length-1;
    if (n > 9) {
      hotkeys.firstChild.nextSibling.remove();
      n = 1;
    }
    fn.appendChild(document.createTextNode("Ctrl+"+ n));
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
                appendTag("style", CSS);
                appendTag("script", GLOBAL_JS);
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

    appendCode("http method", method);
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
    appendCode(
        "http content",
        () ->
            op.responseCodes().forEach(code -> appendSpan("status" + code.charAt(0) + "xx", code)));
    renderMediaSubTypesIndicator(op.responseMediaSubTypes());
    List<SchemaObject> successOneOf = op.responseSuccessSchemas();
    if (!successOneOf.isEmpty()) {
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
