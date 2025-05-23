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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.datastore.DatastoreQuery.parseFields;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.M_DHIS_WEB_APP_MANAGEMENT;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Response.Status;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreParams;
import org.hisp.dhis.datastore.DatastoreQuery;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Stian Sandvold
 */
@OpenApi.Document(
    entity = DatastoreEntry.class,
    classifiers = {"team:platform", "purpose:data"})
@Controller
@RequestMapping("/api/dataStore")
@RequiredArgsConstructor
public class DatastoreController extends AbstractDatastoreController {

  private final DatastoreService service;
  private final AclService aclService;

  /**
   * Returns a JSON array of strings representing the different namespaces used. If no namespaces
   * exist, an empty array is returned.
   */
  @GetMapping(value = "", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<String> getNamespaces(HttpServletResponse response) {
    setNoStore(response);

    return service.getNamespaces();
  }

  /**
   * The path {@code /{namespace}} is clashing with {@link #getEntries(String, String, boolean,
   * DatastoreParams, HttpServletResponse)} therefore a collision free alternative was added {@code
   * /{namespace}/keys}.
   */
  @GetMapping(
      value = {"/{namespace}/keys"},
      produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<String> getKeysInNamespace(
      @RequestParam(required = false) Date lastUpdated,
      @PathVariable String namespace,
      HttpServletResponse response)
      throws NotFoundException, ForbiddenException {
    return getKeysInNamespaceLegacy(lastUpdated, namespace, response);
  }

  /** Returns a list of strings representing keys in the given namespace. */
  @OpenApi.Ignore
  @GetMapping(value = "/{namespace}", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<String> getKeysInNamespaceLegacy(
      @RequestParam(required = false) Date lastUpdated,
      @PathVariable String namespace,
      HttpServletResponse response)
      throws NotFoundException, ForbiddenException {
    setNoStore(response);

    List<String> keys = service.getKeysInNamespace(namespace, lastUpdated);

    if (keys.isEmpty() && !service.isUsedNamespace(namespace)) {
      throw new NotFoundException(String.format("Namespace not found: '%s'", namespace));
    }

    return keys;
  }

  @RequiresAuthority(anyOf = M_DHIS_WEB_APP_MANAGEMENT)
  @GetMapping(value = "/protections", params = "namespace", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody DatastoreNamespaceProtection getNamespaceProtection(
      @RequestParam String namespace) {
    return service.getProtection(namespace);
  }

  @RequiresAuthority(anyOf = M_DHIS_WEB_APP_MANAGEMENT)
  @GetMapping(value = "/protections", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<DatastoreNamespaceProtection> getNamespaceProtections() {
    return service.getProtections();
  }

  @OpenApi.Response(status = Status.OK, value = EntriesResponse.class)
  @GetMapping(value = "/{namespace}", params = "fields", produces = APPLICATION_JSON_VALUE)
  public void getEntries(
      @PathVariable String namespace,
      @RequestParam(required = true) String fields,
      @RequestParam(required = false, defaultValue = "false") boolean includeAll,
      DatastoreParams params,
      HttpServletResponse response)
      throws IOException, ConflictException, ForbiddenException {
    DatastoreQuery query =
        service.plan(
            DatastoreQuery.builder()
                .namespace(namespace)
                .fields(parseFields(fields))
                .includeAll(includeAll)
                .build()
                .with(params));

    writeEntries(response, query, (q, entries) -> service.getEntries(q, entries::test));
  }

  /** Deletes all keys with the given namespace. */
  @ResponseBody
  @DeleteMapping("/{namespace}")
  public WebMessage deleteNamespace(@PathVariable String namespace)
      throws NotFoundException, ForbiddenException {
    if (!service.isUsedNamespace(namespace)) {
      throw new NotFoundException(String.format("Namespace not found: '%s'", namespace));
    }

    service.deleteNamespace(namespace);

    return ok(String.format("Namespace deleted: '%s'", namespace));
  }

  /**
   * Retrieves the value of the KeyJsonValue represented by the given key from the given namespace.
   */
  @GetMapping(value = "/{namespace}/{key}", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody String getEntry(@PathVariable String namespace, @PathVariable String key)
      throws NotFoundException, ForbiddenException {
    return getExistingEntry(namespace, key).getValue();
  }

  /** Retrieves the KeyJsonValue represented by the given key from the given namespace. */
  @GetMapping(value = "/{namespace}/{key}/metaData", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody DatastoreEntry getEntryMetaData(
      @PathVariable String namespace,
      @PathVariable String key,
      @CurrentUser UserDetails currentUser)
      throws NotFoundException,
          InvocationTargetException,
          IllegalAccessException,
          ForbiddenException {
    DatastoreEntry entry = getExistingEntry(namespace, key);

    DatastoreEntry metaData = new DatastoreEntry();
    BeanUtils.copyProperties(metaData, entry);
    metaData.setValue(null);
    metaData.setJbPlainValue(null);
    metaData.setEncryptedValue(null);
    metaData.setAccess(aclService.getAccess(entry, currentUser));
    return metaData;
  }

  /** Creates a new KeyJsonValue Object on the given namespace with the key and value supplied. */
  @ResponseBody
  @PostMapping(
      value = "/{namespace}/{key}",
      produces = APPLICATION_JSON_VALUE,
      consumes = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public WebMessage addEntry(
      @PathVariable String namespace,
      @PathVariable String key,
      @RequestBody String value,
      @RequestParam(defaultValue = "false") boolean encrypt)
      throws ConflictException, BadRequestException, ForbiddenException {
    DatastoreEntry entry = new DatastoreEntry();
    entry.setKey(key);
    entry.setNamespace(namespace);
    entry.setValue(value);
    entry.setEncrypted(encrypt);

    service.addEntry(entry);

    return created(String.format("Key created: '%s'", key));
  }

  /**
   * Create or update a key in the given namespace <br>
   * <br>
   *
   * <p>If the key or namespace do not exist then a create will be attempted
   *
   * <p>If the key and namespace exist then an update will be attempted
   */
  @OpenApi.Response(
      status = {Status.CREATED, Status.OK},
      value = WebMessage.class)
  @ResponseBody
  @PutMapping(
      value = "/{namespace}/{key}",
      produces = APPLICATION_JSON_VALUE,
      consumes = APPLICATION_JSON_VALUE)
  public WebMessage putEntry(
      @PathVariable String namespace,
      @PathVariable String key,
      @RequestBody(required = false) String value,
      @RequestParam(required = false) String path,
      @RequestParam(required = false) Integer roll,
      @RequestParam(defaultValue = "false") boolean encrypt)
      throws BadRequestException, ConflictException, ForbiddenException {
    DatastoreEntry dataEntry = service.getEntry(namespace, key);

    if (dataEntry == null) return addEntry(namespace, key, value, encrypt);

    service.updateEntry(namespace, key, value, path, roll);
    return ok(String.format("Key updated: '%s'", key));
  }

  /** Delete a key from the given namespace. */
  @ResponseBody
  @DeleteMapping(value = "/{namespace}/{key}", produces = APPLICATION_JSON_VALUE)
  public WebMessage deleteEntry(@PathVariable String namespace, @PathVariable String key)
      throws NotFoundException, ForbiddenException {
    DatastoreEntry entry = getExistingEntry(namespace, key);
    service.deleteEntry(entry);

    return ok(String.format("Key '%s' deleted from namespace '%s'", key, namespace));
  }

  private DatastoreEntry getExistingEntry(String namespace, String key)
      throws NotFoundException, ForbiddenException {
    DatastoreEntry entry = service.getEntry(namespace, key);

    if (entry == null) {
      throw new NotFoundException(
          String.format("Key '%s' not found in namespace '%s'", key, namespace));
    }

    return entry;
  }
}
