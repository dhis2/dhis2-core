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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.webapi.utils.FileResourceUtils.resizeAvatarToDefaultSize;
import static org.hisp.dhis.webapi.utils.FileResourceUtils.resizeIconToDefaultSize;
import static org.hisp.dhis.webapi.utils.FileResourceUtils.resizeOrgToDefaultSize;
import static org.hisp.dhis.webapi.utils.FileResourceUtils.validateCustomIconFile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.function.BiPredicate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceOwner;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.gist.GistObjectListParams;
import org.hisp.dhis.gist.GistObjectParams;
import org.hisp.dhis.gist.GistObjectPropertyParams;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.GetObjectParams;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.tracker.export.FileResourceStream;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Halvdan Hoem Grelland
 */
@RestController
@RequestMapping("/api/fileResources")
@Slf4j
@AllArgsConstructor
@OpenApi.Document(classifiers = {"team:platform", "purpose:metadata"})
public class FileResourceController
    extends AbstractFullReadOnlyController<FileResource, GetObjectListParams> {
  private final FileResourceService fileResourceService;

  private final FileResourceUtils fileResourceUtils;

  private final DhisConfigurationProvider dhisConfig;

  /**
   * Overridden to only use it when {@code fields} parameter is present.
   *
   * <p>Authorizes the owning object as {@link #getFileResource} does. This route returns the very
   * metadata that gate protects, so it must not become a way around it.
   */
  @OpenApi.Ignore
  @Override
  @GetMapping(value = "/{uid}", params = "fields")
  public ResponseEntity<?> getObject(
      @PathVariable UID uid,
      GetObjectParams params,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request,
      HttpServletResponse response)
      throws ForbiddenException, NotFoundException {
    getFileResourceWithReadableMetadata(uid, currentUser);
    return super.getObject(uid, params, currentUser, request, response);
  }

  @Override
  @GetMapping("/{uid:[a-zA-Z0-9]{11}}/{property}")
  @OpenApi.Document(group = OpenApi.Document.GROUP_QUERY)
  public @ResponseBody ResponseEntity<ObjectNode> getObjectProperty(
      @PathVariable("uid") UID uid,
      @OpenApi.Param(OpenApi.PropertyNames.class) @PathVariable("property") String property,
      @RequestParam(required = false) List<String> fields,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws ForbiddenException, NotFoundException {
    getFileResourceWithReadableMetadata(uid, currentUser);
    return super.getObjectProperty(uid, property, fields, currentUser, response);
  }

  @Override
  @OpenApi.Response(OpenApi.EntityType.class)
  @OpenApi.Document(group = OpenApi.Document.GROUP_QUERY)
  @GetMapping(value = "/{uid}/gist", produces = "application/json")
  public void getObjectGist(
      @PathVariable("uid") UID uid, GistObjectParams params, HttpServletResponse response)
      throws NotFoundException, BadRequestException {
    checkGistAccess(uid);
    super.getObjectGist(uid, params, response);
  }

  @Override
  @OpenApi.Response(String.class)
  @OpenApi.Document(group = OpenApi.Document.GROUP_QUERY)
  @GetMapping(
      value = {"/{uid}/gist", "/{uid}/gist.csv"},
      produces = "text/csv")
  public void getObjectGistAsCsv(
      @PathVariable("uid") UID uid, GistObjectParams params, HttpServletResponse response)
      throws BadRequestException, NotFoundException {
    if (checkGistCsvAccess(uid, response)) {
      super.getObjectGistAsCsv(uid, params, response);
    }
  }

  @Override
  @OpenApi.Response(JsonValue.class)
  @OpenApi.Document(group = OpenApi.Document.GROUP_QUERY)
  @GetMapping(value = "/{uid}/{property}/gist", produces = "application/json")
  public void getObjectPropertyGist(
      @PathVariable("uid") UID uid,
      @OpenApi.Param(OpenApi.PropertyNames.class) @PathVariable("property") String property,
      GistObjectPropertyParams params,
      HttpServletRequest request,
      HttpServletResponse response)
      throws BadRequestException, NotFoundException {
    checkGistAccess(uid);
    super.getObjectPropertyGist(uid, property, params, request, response);
  }

  @Override
  @OpenApi.Response(String.class)
  @OpenApi.Document(group = OpenApi.Document.GROUP_QUERY)
  @GetMapping(
      value = {"/{uid}/{property}/gist", "/{uid}/{property}/gist.csv"},
      produces = "text/csv")
  public void getObjectPropertyGistAsCsv(
      @PathVariable("uid") UID uid,
      @OpenApi.Param(OpenApi.PropertyNames.class) @PathVariable("property") String property,
      GistObjectPropertyParams params,
      HttpServletRequest request,
      HttpServletResponse response)
      throws BadRequestException, NotFoundException {
    if (checkGistCsvAccess(uid, response)) {
      super.getObjectPropertyGistAsCsv(uid, property, params, request, response);
    }
  }

  @GetMapping(value = "/{uid}")
  public FileResource getFileResource(
      @PathVariable UID uid,
      @RequestParam(required = false) ImageFileDimension dimension,
      @CurrentUser UserDetails currentUser)
      throws NotFoundException, ForbiddenException {
    FileResource fileResource = getFileResourceWithReadableMetadata(uid, currentUser);

    FileResourceUtils.setImageFileDimensions(
        fileResource, MoreObjects.firstNonNull(dimension, ImageFileDimension.ORIGINAL));

    return fileResource;
  }

  @GetMapping(value = "/{uid}/data")
  public void getFileResourceData(
      @PathVariable UID uid,
      HttpServletResponse response,
      @RequestParam(required = false) ImageFileDimension dimension,
      @CurrentUser UserDetails currentUser)
      throws NotFoundException, ForbiddenException, WebMessageException {
    FileResource fileResource =
        getReadableFileResource(uid, currentUser, fileResourceService::canRead);

    // Serving a data value file from this endpoint would not apply the data sharing of the owning
    // data value, so this domain is out of scope here for everyone, ownership or authority
    // regardless. Data value files are read through the data value API instead.
    if (fileResource.getDomain() == FileResourceDomain.DATA_VALUE) {
      throw new ForbiddenException(
          String.format("File resource not found or not accessible: %s", uid));
    }

    FileResourceUtils.setImageFileDimensions(
        fileResource, MoreObjects.firstNonNull(dimension, ImageFileDimension.ORIGINAL));

    response.setContentType(fileResource.getContentType());

    response.setHeader(
        HttpHeaders.CONTENT_LENGTH,
        String.valueOf(fileResourceService.getFileResourceContentLength(fileResource)));
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName());

    HeaderUtils.setSecurityHeaders(
        response, dhisConfig.getProperty(ConfigurationKey.CSP_HEADER_VALUE));

    try {
      fileResourceService.copyFileResourceContent(fileResource, response.getOutputStream());
    } catch (IOException ex) {
      log.error(String.format("Could not retrieve file for file resource: '%s'", uid), ex);
      throw new WebMessageException(
          error(FileResourceStream.EXCEPTION_IO, FileResourceStream.EXCEPTION_IO_DEV));
    }
  }

  @PostMapping
  public WebMessage saveFileResource(
      @RequestParam MultipartFile file,
      @RequestParam(defaultValue = "DATA_VALUE") FileResourceDomain domain,
      @RequestParam(required = false) UID uid)
      throws IOException, ConflictException {

    FileResourceUtils.validateFileSize(
        file, Long.parseLong(dhisConfig.getProperty(ConfigurationKey.MAX_FILE_UPLOAD_SIZE_BYTES)));

    FileResource fileResource;
    String id = uid == null ? null : uid.getValue();
    if (domain.equals(FileResourceDomain.ICON)) {
      validateCustomIconFile(file);
      fileResource = fileResourceUtils.saveFileResource(id, resizeIconToDefaultSize(file), domain);

    } else if (domain.equals(FileResourceDomain.USER_AVATAR)) {
      fileResourceUtils.validateUserAvatar(file);
      fileResource =
          fileResourceUtils.saveFileResource(id, resizeAvatarToDefaultSize(file), domain);

    } else if (domain.equals(FileResourceDomain.ORG_UNIT)) {
      fileResourceUtils.validateOrgUnitImage(file);
      fileResource = fileResourceUtils.saveFileResource(id, resizeOrgToDefaultSize(file), domain);

    } else {
      fileResource = fileResourceUtils.saveFileResource(id, file, domain);
    }

    WebMessage webMessage = new WebMessage(Status.OK, HttpStatus.ACCEPTED);
    webMessage.setResponse(new FileResourceWebMessageResponse(fileResource));

    return webMessage;
  }

  @RequiresAuthority(anyOf = ALL)
  @GetMapping("/owners")
  public List<FileResourceOwner> getFileResourceOwners(@RequestParam String storageKey) {
    return fileResourceService.findOwnersByStorageKey(storageKey);
  }

  private FileResource getFileResourceWithReadableMetadata(UID uid, UserDetails currentUser)
      throws NotFoundException, ForbiddenException {
    return getReadableFileResource(uid, currentUser, fileResourceService::canReadMetadata);
  }

  /**
   * The inherited gist handlers have no {@link CurrentUser} parameter, and gist reports a denied
   * read with an {@link AccessDeniedException}, so this follows both conventions.
   */
  private void checkGistAccess(UID uid) throws NotFoundException {
    try {
      getFileResourceWithReadableMetadata(uid, CurrentUserUtil.getCurrentUserDetails());
    } catch (ForbiddenException ex) {
      throw new AccessDeniedException(ex.getMessage());
    }
  }

  /**
   * A {@code text/csv} response cannot carry the JSON error body of a denied read, so the denial is
   * a bare 403.
   *
   * @return true when the current user may read the file resource
   */
  private boolean checkGistCsvAccess(UID uid, HttpServletResponse response)
      throws NotFoundException {
    try {
      checkGistAccess(uid);
      return true;
    } catch (AccessDeniedException ex) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return false;
    }
  }

  /*
   * The inherited generic read endpoints below are restricted to superusers.
   *
   * FileResource is not a shareable type and its schema declares no authorities, so
   * aclService.canRead(user, FileResource.class) - the only check the generic endpoints of
   * AbstractFullReadOnlyController apply - holds for every authenticated user, and the Gist
   * endpoints apply no check at all. Left open, the collection endpoints let any user page every
   * file resource uid and domain, which is the reconnaissance half of an attack on the per-object
   * endpoints above.
   *
   * Listing and Gist have no known client for this type; 2.40 never exposed either. Filtering them
   * per row instead would mean resolving the owner of every file resource on every page, and again
   * in the pager count query, so an authority is both the cheaper and the more predictable answer.
   * The Gist methods also cannot report a denial any other way, since they may not throw the
   * checked ForbiddenException.
   */

  @Override
  @RequiresAuthority(anyOf = ALL)
  @GetMapping
  public ResponseEntity<StreamingJsonRoot<FileResource>> getObjectList(
      GetObjectListParams params,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, ConflictException {
    return super.getObjectList(params, response, currentUser);
  }

  /**
   * Already unreachable because {@link FileResource} is not a metadata type, but gated explicitly
   * so that the endpoint does not silently reopen if that schema flag ever changes.
   */
  @Override
  @RequiresAuthority(anyOf = ALL)
  @GetMapping(produces = {"text/csv", "application/text"})
  public ResponseEntity<String> getObjectListCsv(
      GetObjectListParams params,
      @CurrentUser UserDetails currentUser,
      @RequestParam(defaultValue = ",") char separator,
      @RequestParam(defaultValue = ";") String arraySeparator,
      @RequestParam(defaultValue = "false") boolean skipHeader,
      HttpServletResponse response)
      throws IOException,
          NotFoundException,
          ConflictException,
          ForbiddenException,
          BadRequestException {
    return super.getObjectListCsv(
        params, currentUser, separator, arraySeparator, skipHeader, response);
  }

  @Override
  @RequiresAuthority(anyOf = ALL)
  @GetMapping(value = "/gist", produces = "application/json")
  public void getObjectListGist(
      GistObjectListParams params, HttpServletRequest request, HttpServletResponse response)
      throws BadRequestException {
    super.getObjectListGist(params, request, response);
  }

  @Override
  @RequiresAuthority(anyOf = ALL)
  @GetMapping(
      value = {"/gist", "/gist.csv"},
      produces = "text/csv")
  public void getObjectListGistAsCsv(
      GistObjectListParams params, HttpServletRequest request, HttpServletResponse response)
      throws BadRequestException {
    super.getObjectListGistAsCsv(params, request, response);
  }

  /**
   * Resolves the file resource and authorizes the current user against the object that owns it.
   *
   * <p>{@link FileResource} is not a shareable type, so the decision cannot come from the file
   * resource itself; {@link FileResourceService} resolves the owning document, message conversation
   * or job and applies its access rule.
   */
  private FileResource getReadableFileResource(
      UID uid, UserDetails currentUser, BiPredicate<UserDetails, FileResource> canRead)
      throws NotFoundException, ForbiddenException {
    FileResource fileResource = fileResourceService.getFileResource(uid.getValue());

    if (fileResource == null) {
      throw new NotFoundException(FileResource.class, uid);
    }

    if (!canRead.test(currentUser, fileResource)) {
      throw new ForbiddenException(
          String.format("File resource not found or not accessible: %s", uid));
    }

    return fileResource;
  }
}
