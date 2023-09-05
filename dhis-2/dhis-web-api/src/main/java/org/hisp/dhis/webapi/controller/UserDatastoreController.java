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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.datastore.DatastoreQuery.parseFields;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.Response.Status;
import org.hisp.dhis.datastore.DatastoreParams;
import org.hisp.dhis.datastore.DatastoreQuery;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.userdatastore.UserDatastoreEntry;
import org.hisp.dhis.userdatastore.UserDatastoreService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
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

/**
 * @author Stian Sandvold
 */
@OpenApi.Tags({"user", "query"})
@Controller
@RequestMapping("/userDataStore")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@AllArgsConstructor
public class UserDatastoreController extends AbstractDatastoreController {

  private final UserDatastoreService userDatastoreService;
  private final CurrentUserService currentUserService;
  private final UserService userService;

  /**
   * Returns a JSON array of strings representing the different namespaces used. If no namespaces
   * exist, an empty array is returned.
   */
  @GetMapping(value = "", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<String> getNamespaces(
      @RequestParam(required = false) String username, HttpServletResponse response) {
    setNoStore(response);

    return userDatastoreService.getNamespacesByUser(getUser(username));
  }

  /**
   * The path {@code /{namespace}} is clashing with {@code getEntries} therefore a collision free
   * alternative was added {@code /{namespace}/keys}.
   */
  @GetMapping(
      value = {"/{namespace}/keys"},
      produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<String> getKeysInNamespace(
      @PathVariable String namespace,
      @RequestParam(required = false) String username,
      HttpServletResponse response)
      throws NotFoundException {
    return getKeysInNamespaceLegacy(namespace, username, response);
  }

  /**
   * Returns a JSON array of strings representing the different keys used in a given namespace. If
   * no namespaces exist, an empty array is returned.
   */
  @GetMapping(value = "/{namespace}", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<String> getKeysInNamespaceLegacy(
      @PathVariable String namespace,
      @RequestParam(required = false) String username,
      HttpServletResponse response)
      throws NotFoundException {
    User user = getUser(username);

    setNoStore(response);

    List<String> keys = userDatastoreService.getKeysByUserAndNamespace(user, namespace);

    if (keys.isEmpty() && !userDatastoreService.isUsedNamespace(user, namespace)) {
      throw new NotFoundException(String.format("Namespace not found: '%s'", namespace));
    }

    return keys;
  }

  @OpenApi.Response(status = Status.OK, value = EntriesResponse.class)
  @GetMapping(value = "/{namespace}", params = "fields", produces = APPLICATION_JSON_VALUE)
  public void getEntries(
      @PathVariable String namespace,
      @RequestParam(required = false) String username,
      @RequestParam(required = true) String fields,
      @RequestParam(required = false, defaultValue = "false") boolean includeAll,
      DatastoreParams params,
      HttpServletResponse response)
      throws IOException, ConflictException {
    DatastoreQuery query =
        userDatastoreService.plan(
            DatastoreQuery.builder()
                .namespace(namespace)
                .fields(parseFields(fields))
                .includeAll(includeAll)
                .build()
                .with(params));

    User user = getUser(username);

    writeEntries(
        response, query, (q, entries) -> userDatastoreService.getEntries(user, q, entries::test));
  }

  /** Deletes all keys with the given user and namespace. */
  @DeleteMapping("/{namespace}")
  @ResponseBody
  public WebMessage deleteNamespace(
      @PathVariable String namespace, @RequestParam(required = false) String username)
      throws NotFoundException {
    User user = getUser(username);
    if (!userDatastoreService.isUsedNamespace(user, namespace)) {
      throw new NotFoundException(String.format("Namespace not found: '%s'", namespace));
    }

    userDatastoreService.deleteNamespace(user, namespace);

    return ok(String.format("Namespace deleted: '%s'", namespace));
  }

  /**
   * Retrieves the value of the KeyJsonValue represented by the given key and namespace from the
   * current user.
   */
  @GetMapping(value = "/{namespace}/{key}", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody String getEntry(
      @PathVariable String namespace,
      @PathVariable String key,
      @RequestParam(required = false) String username)
      throws NotFoundException {
    return getExistingEntry(username, namespace, key).getValue();
  }

  /**
   * Creates a new KeyJsonValue Object on the current user with the key, namespace and value
   * supplied.
   */
  @PostMapping(
      value = "/{namespace}/{key}",
      produces = APPLICATION_JSON_VALUE,
      consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage addEntry(
      @PathVariable String namespace,
      @PathVariable String key,
      @RequestParam(required = false) String username,
      @RequestBody String value,
      @RequestParam(defaultValue = "false") boolean encrypt)
      throws BadRequestException, ConflictException {
    User user = getUser(username);

    UserDatastoreEntry entry = new UserDatastoreEntry();
    entry.setKey(key);
    entry.setCreatedBy(user);
    entry.setNamespace(namespace);
    entry.setValue(value);
    entry.setEncrypted(encrypt);

    userDatastoreService.addEntry(entry);

    return created("Key '" + key + "' in namespace '" + namespace + "' created.");
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
  @PutMapping(
      value = "/{namespace}/{key}",
      produces = APPLICATION_JSON_VALUE,
      consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage putUserValue(
      @PathVariable String namespace,
      @PathVariable String key,
      @RequestParam(required = false) String username,
      @RequestBody String value,
      @RequestParam(defaultValue = "false") boolean encrypt)
      throws BadRequestException, ConflictException {
    UserDatastoreEntry userEntry =
        userDatastoreService.getUserEntry(getUser(username), namespace, key);

    return userEntry != null
        ? updateEntry(userEntry, key, value)
        : addEntry(namespace, key, username, value, encrypt);
  }

  /** Delete a key. */
  @DeleteMapping(value = "/{namespace}/{key}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage deleteEntry(
      @PathVariable String namespace,
      @PathVariable String key,
      @RequestParam(required = false) String username)
      throws NotFoundException {
    UserDatastoreEntry entry = getExistingEntry(username, namespace, key);
    userDatastoreService.deleteEntry(entry);

    return ok("Key '" + key + "' deleted from the namespace '" + namespace + "'.");
  }

  private UserDatastoreEntry getExistingEntry(String username, String namespace, String key)
      throws NotFoundException {
    UserDatastoreEntry entry = userDatastoreService.getUserEntry(getUser(username), namespace, key);
    if (entry == null) {
      throw new NotFoundException(
          String.format("Key '%s' not found in namespace '%s'", key, namespace));
    }
    return entry;
  }

  @Nonnull
  private User getUser(String username) {
    User currentUser = currentUserService.getCurrentUser();
    if (username == null || username.isBlank()) {
      return currentUser;
    }
    if (!currentUser.isSuper()) {
      throw new IllegalQueryException(
          "Only superusers can read or write other users data using the `username` parameter.");
    }
    User user = userService.getUserByUsername(username);
    if (user == null) {
      throw new IllegalQueryException("No user with username " + username + " exists.");
    }
    return user;
  }

  private WebMessage updateEntry(UserDatastoreEntry entry, String key, String value)
      throws BadRequestException {
    entry.setValue(value);
    userDatastoreService.updateEntry(entry);

    return ok(String.format("Key updated: '%s'", key));
  }
}
