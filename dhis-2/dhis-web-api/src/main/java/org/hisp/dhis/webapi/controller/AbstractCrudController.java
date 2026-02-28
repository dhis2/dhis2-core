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

import static java.util.Collections.singletonList;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importReport;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.objectReport;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.typeReport;
import static org.hisp.dhis.scheduling.RecordingJobProgress.transitory;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.FavoritableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjects;
import org.hisp.dhis.common.Maturity.Beta;
import org.hisp.dhis.common.Maturity.Stable;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.PropertyNames;
import org.hisp.dhis.common.SubscribableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.MetadataObjects;
import org.hisp.dhis.dxf2.metadata.collection.CollectionService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.TranslationsCheck;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.eventhook.EventHookPublisher;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsonpatch.BulkJsonPatch;
import org.hisp.dhis.jsonpatch.BulkPatchManager;
import org.hisp.dhis.jsonpatch.BulkPatchParameters;
import org.hisp.dhis.jsonpatch.JsonPatchManager;
import org.hisp.dhis.jsonpatch.validator.BulkPatchValidatorFactory;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.MetadataMergeService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.sharing.SharingService;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Stable
@OpenApi.Document(group = OpenApi.Document.GROUP_MANAGE)
public abstract class AbstractCrudController<
        T extends IdentifiableObject, P extends GetObjectListParams>
    extends AbstractFullReadOnlyController<T, P> {
  @Autowired protected SchemaValidator schemaValidator;

  @Autowired protected RenderService renderService;

  @Autowired protected MetadataImportService importService;

  @Autowired protected MetadataExportService exportService;

  @Autowired protected HibernateCacheManager hibernateCacheManager;

  @Autowired protected CollectionService collectionService;

  @Autowired protected MetadataMergeService metadataMergeService;

  @Autowired protected JsonPatchManager jsonPatchManager;

  @Autowired
  @Qualifier("xmlMapper")
  protected ObjectMapper xmlMapper;

  @Autowired protected ObjectMapper jsonMapper;

  @Autowired protected UserService userService;

  @Autowired protected SharingService sharingService;

  @Autowired protected BulkPatchManager bulkPatchManager;

  @Autowired private TranslationsCheck translationsCheck;

  @Autowired protected EventHookPublisher eventHookPublisher;

  // --------------------------------------------------------------------------
  // PATCH
  // --------------------------------------------------------------------------

  /**
   * Adds support for HTTP Patch using JSON Patch (RFC 6902), updated object is run through normal
   * metadata importer and internally looks like a normal PUT (after the JSON Patch has been
   * applied).
   *
   * <p>For now, we only support the official mimetype "application/json-patch+json" but in future
   * releases we might also want to support "application/json" after the old patch behavior has been
   * removed.
   */
  @Beta
  @OpenApi.Param(JsonPatch.class)
  @ResponseBody
  @PatchMapping(path = "/{uid}", consumes = "application/json-patch+json")
  public WebMessage patchObject(
      @OpenApi.Param(UID.class) @PathVariable("uid") UID uid,
      @RequestParam Map<String, String> rpParameters,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request)
      throws ForbiddenException,
          NotFoundException,
          IOException,
          JsonPatchException,
          ConflictException {
    final T persistedObject = getEntity(uid);

    updatePermissionCheck(currentUser, persistedObject);

    manager.resetNonOwnerProperties(persistedObject);

    JsonPatch patch = jsonMapper.readValue(request.getInputStream(), JsonPatch.class);

    final T patchedObject = doPatch(patch, persistedObject);

    // Do not allow changing IDs
    patchedObject.setId(persistedObject.getId());

    // Do not allow changing UIDs
    patchedObject.setUid(persistedObject.getUid());

    prePatchEntity(persistedObject, patchedObject);

    Map<String, List<String>> parameterValuesMap = contextService.getParameterValuesMap();

    if (!parameterValuesMap.containsKey("importReportMode")) {
      parameterValuesMap.put("importReportMode", Collections.singletonList("ERRORS_NOT_OWNER"));
    }

    MetadataImportParams params = importService.getParamsFromMap(parameterValuesMap);

    params.setUser(UID.of(currentUser)).setImportStrategy(ImportStrategy.UPDATE);

    ImportReport importReport =
        importService.importMetadata(params, new MetadataObjects().addObject(patchedObject));
    WebMessage webMessage = objectReport(importReport);

    if (importReport.getStatus() == Status.OK) {
      T entity = manager.get(getEntityClass(), uid);

      postPatchEntity(patch, entity);
    } else {
      webMessage.setStatus(Status.ERROR);
    }

    return webMessage;
  }

  protected T doPatch(JsonPatch patch, T persistedObject) throws JsonPatchException {

    final T patchedObject = jsonPatchManager.apply(patch, persistedObject);

    if (patchedObject instanceof User) {
      // Reset to avoid non owning properties (here UserGroups) to be
      // operated on in the import.
      manager.resetNonOwnerProperties(patchedObject);
    }

    return patchedObject;
  }

  @OpenApi.Param(BulkJsonPatch.class)
  @ResponseBody
  @PatchMapping(
      path = "/sharing",
      consumes = "application/json-patch+json",
      produces = APPLICATION_JSON_VALUE)
  public WebMessage bulkSharing(
      @RequestParam(required = false, defaultValue = "false") boolean atomic,
      HttpServletRequest request)
      throws IOException {
    final BulkJsonPatch bulkJsonPatch =
        jsonMapper.readValue(request.getInputStream(), BulkJsonPatch.class);

    BulkPatchParameters patchParams =
        BulkPatchParameters.builder().validators(BulkPatchValidatorFactory.SHARING).build();

    List<IdentifiableObject> patchedObjects =
        bulkPatchManager.applyPatch(bulkJsonPatch, patchParams);

    if (patchedObjects.isEmpty() || (atomic && patchParams.hasErrorReports())) {
      ImportReport importReport = new ImportReport();
      importReport.addTypeReports(patchParams.getTypeReports());
      importReport.setStatus(Status.ERROR);
      return importReport(importReport);
    }

    Map<String, List<String>> parameterValuesMap = contextService.getParameterValuesMap();

    MetadataImportParams params = importService.getParamsFromMap(parameterValuesMap);

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    params.setUser(UID.of(currentUser)).setImportStrategy(ImportStrategy.UPDATE);

    ImportReport importReport =
        importService.importMetadata(params, new MetadataObjects().addObjects(patchedObjects));

    if (patchParams.hasErrorReports()) {
      importReport.addTypeReports(patchParams.getTypeReports());
      importReport.setStatus(
          importReport.getStatus() == Status.OK ? Status.WARNING : importReport.getStatus());
    }

    return importReport(importReport);
  }

  // --------------------------------------------------------------------------
  // POST
  // --------------------------------------------------------------------------

  @Stable
  @OpenApi.Params(MetadataImportParams.class)
  @OpenApi.Param(OpenApi.EntityType.class)
  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  @SuppressWarnings("java:S1130")
  public WebMessage postJsonObject(HttpServletRequest request)
      throws IOException,
          ForbiddenException,
          ConflictException,
          HttpRequestMethodNotSupportedException,
          NotFoundException {
    return postObject(deserializeJsonEntity(request));
  }

  private WebMessage postObject(T parsed) throws ForbiddenException, ConflictException {

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    if (!aclService.canCreate(currentUserDetails, getEntityClass())) {
      throw new ForbiddenException("You don't have the proper permissions to create this object.");
    }

    parsed.getTranslations().clear();

    preCreateEntity(parsed);

    MetadataImportParams params =
        importService
            .getParamsFromMap(contextService.getParameterValuesMap())
            .setImportReportMode(ImportReportMode.FULL)
            .setUser(UID.of(currentUserDetails))
            .setImportStrategy(ImportStrategy.CREATE);

    return postObject(
        getObjectReport(
            importService.importMetadata(params, new MetadataObjects().addObject(parsed))));
  }

  protected final WebMessage postObject(ObjectReport objectReport) {
    WebMessage webMessage = objectReport(objectReport);

    if (objectReport != null && webMessage.getStatus() == Status.OK) {
      webMessage.setHttpStatus(HttpStatus.CREATED);
      webMessage.setLocation(getSchema().getRelativeApiEndpoint() + "/" + objectReport.getUid());
      T entity = manager.get(getEntityClass(), objectReport.getUid());
      postCreateEntity(entity);
    } else {
      webMessage.setStatus(Status.ERROR);
    }

    return webMessage;
  }

  private ObjectReport getObjectReport(ImportReport importReport) {
    return importReport.getFirstObjectReport();
  }

  @OpenApi.Filter(
      includes = {
        Dashboard.class,
        EventVisualization.class,
        org.hisp.dhis.mapping.Map.class,
        Visualization.class
      })
  @PostMapping(value = "/{uid}/favorite")
  @ResponseBody
  public WebMessage setAsFavorite(
      @PathVariable("uid") UID uid, @CurrentUser UserDetails currentUser)
      throws ConflictException, NotFoundException {

    if (!getSchema().isFavoritable()) {
      throw new ConflictException("Objects of this class cannot be set as favorite");
    }

    T object = getEntity(uid);
    if (object instanceof FavoritableObject favoritableObject) {
      favoritableObject.setAsFavorite(currentUser);
      manager.updateNoAcl(object);
      return ok(
          String.format(
              "Object '%s' set as favorite for user '%s'", uid, currentUser.getUsername()));
    } else {
      throw new ConflictException("Objects of this class cannot be set as favorite");
    }
  }

  @OpenApi.Filter(
      includes = {EventVisualization.class, org.hisp.dhis.mapping.Map.class, Visualization.class})
  @PostMapping(value = "/{uid}/subscriber")
  @ResponseBody
  public WebMessage subscribe(@PathVariable("uid") UID uid, @CurrentUser User currentUser)
      throws ConflictException, NotFoundException {

    if (!getSchema().isSubscribable()) {
      throw new ConflictException("Objects of this class cannot be subscribed to");
    }
    SubscribableObject object = (SubscribableObject) getEntity(uid);

    object.subscribe(currentUser);
    manager.updateNoAcl(object);

    return ok(String.format("User '%s' subscribed to object '%s'", currentUser.getUsername(), uid));
  }

  // --------------------------------------------------------------------------
  // PUT
  // --------------------------------------------------------------------------

  @OpenApi.Params(MetadataImportParams.class)
  @OpenApi.Param(OpenApi.EntityType.class)
  @PutMapping(value = "/{uid}", consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  @SuppressWarnings("java:S1130")
  public WebMessage putJsonObject(
      @PathVariable("uid") UID uid,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request)
      throws NotFoundException,
          ForbiddenException,
          IOException,
          ConflictException,
          HttpRequestMethodNotSupportedException {
    T persisted = getEntity(uid);

    updatePermissionCheck(currentUser, persisted);

    T parsed = deserializeJsonEntity(request);
    parsed.setUid(uid.getValue());

    preUpdateEntity(persisted, parsed);

    MetadataImportParams params =
        importService.getParamsFromMap(contextService.getParameterValuesMap());

    params.setUser(UID.of(currentUser)).setImportStrategy(ImportStrategy.UPDATE);

    // default to FULL unless ERRORS_NOT_OWNER has been requested
    if (ImportReportMode.ERRORS_NOT_OWNER != params.getImportReportMode()) {
      params.setImportReportMode(ImportReportMode.FULL);
    }

    ImportReport importReport =
        importService.importMetadata(params, new MetadataObjects().addObject(parsed), transitory());
    WebMessage webMessage = objectReport(importReport);

    if (importReport.getStatus() == Status.OK) {
      T entity = manager.get(getEntityClass(), uid);
      postUpdateEntity(entity);
    } else {
      webMessage.setStatus(Status.ERROR);
    }

    return webMessage;
  }

  @OpenApi.Param(object = @OpenApi.Property(name = "translations", value = Translation[].class))
  @PutMapping(value = "/{uid}/translations")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  public WebMessage replaceTranslations(
      @PathVariable("uid") UID uid,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request)
      throws NotFoundException, ForbiddenException, IOException {
    T persistedObject = getEntity(uid);

    updatePermissionCheck(currentUser, persistedObject);

    T inputObject = renderService.fromJson(request.getInputStream(), getEntityClass());

    HashSet<Translation> translations = new HashSet<>(inputObject.getTranslations());

    persistedObject.setTranslations(translations);
    List<ObjectReport> objectReports = new ArrayList<>();
    translationsCheck.run(
        persistedObject, getEntityClass(), objectReports::add, getSchema(), 0, null);

    if (objectReports.isEmpty()) {
      manager.update(persistedObject);
      return null;
    }

    return objectReport(objectReports.get(0));
  }

  // --------------------------------------------------------------------------
  // DELETE
  // --------------------------------------------------------------------------

  @DeleteMapping(value = "/{uid}")
  @ResponseBody
  @SuppressWarnings("java:S1130")
  public WebMessage deleteObject(
      @PathVariable("uid") UID uid,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request,
      HttpServletResponse response)
      throws NotFoundException,
          ForbiddenException,
          ConflictException,
          HttpRequestMethodNotSupportedException {
    T persistedObject = getEntity(uid);
    if (!aclService.canDelete(currentUser, persistedObject)) {
      throw new ForbiddenException("You don't have the proper permissions to delete this object.");
    }

    preDeleteEntity(persistedObject);

    MetadataImportParams params =
        new MetadataImportParams()
            .setImportReportMode(ImportReportMode.FULL)
            .setUser(UID.of(currentUser))
            .setImportStrategy(ImportStrategy.DELETE);

    ImportReport importReport =
        importService.importMetadata(params, new MetadataObjects().addObject(persistedObject));

    postDeleteEntity(uid);

    return objectReport(importReport);
  }

  @OpenApi.Filter(
      includes = {
        Dashboard.class,
        EventVisualization.class,
        org.hisp.dhis.mapping.Map.class,
        Visualization.class
      })
  @DeleteMapping(value = "/{uid}/favorite")
  @ResponseBody
  public WebMessage removeAsFavorite(
      @PathVariable("uid") UID uid, @CurrentUser UserDetails currentUser)
      throws NotFoundException, ConflictException {

    if (!getSchema().isFavoritable()) {
      throw new ConflictException("Objects of this class cannot be set as favorite");
    }

    T object = getEntity(uid);
    if (object instanceof FavoritableObject favoritableObject) {
      favoritableObject.removeAsFavorite(currentUser);
      manager.updateNoAcl(object);

      return ok(
          String.format(
              "Object '%s' removed as favorite for user '%s'", uid, currentUser.getUsername()));
    } else {
      throw new ConflictException("Objects of this class cannot be set as favorite");
    }
  }

  @OpenApi.Filter(
      includes = {EventVisualization.class, org.hisp.dhis.mapping.Map.class, Visualization.class})
  @DeleteMapping(value = "/{uid}/subscriber")
  @ResponseBody
  public WebMessage unsubscribe(@PathVariable("uid") UID uid, @CurrentUser User currentUser)
      throws NotFoundException, ConflictException {

    if (!getSchema().isSubscribable()) {
      throw new ConflictException("Objects of this class cannot be subscribed to");
    }

    SubscribableObject object = (SubscribableObject) getEntity(uid);

    object.unsubscribe(currentUser);
    manager.updateNoAcl(object);

    return ok(
        String.format(
            "User '%s' removed as subscriber of object '%s'", currentUser.getUsername(), uid));
  }

  // --------------------------------------------------------------------------
  // Identifiable object collections add, delete
  // --------------------------------------------------------------------------

  @OpenApi.Param(IdentifiableObjects.class)
  @PostMapping(value = "/{uid}/{property}", consumes = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public WebMessage addCollectionItemsJson(
      @PathVariable("uid") UID uid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String property,
      HttpServletRequest request)
      throws IOException,
          ForbiddenException,
          ConflictException,
          NotFoundException,
          BadRequestException {
    return addCollectionItems(
        property,
        getEntity(uid),
        renderService.fromJson(request.getInputStream(), IdentifiableObjects.class));
  }

  private WebMessage addCollectionItems(String property, T object, IdentifiableObjects items)
      throws ConflictException, ForbiddenException, NotFoundException, BadRequestException {
    preUpdateItems(object, items);
    TypeReport report = collectionService.mergeCollectionItems(object, property, items);
    postUpdateItems(object, items);
    hibernateCacheManager.clearCache();
    return typeReport(report);
  }

  @OpenApi.Param(IdentifiableObjects.class)
  @PutMapping(value = "/{uid}/{property}", consumes = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public WebMessage replaceCollectionItemsJson(
      @PathVariable("uid") UID uid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String property,
      HttpServletRequest request)
      throws IOException,
          ForbiddenException,
          ConflictException,
          NotFoundException,
          BadRequestException {
    return replaceCollectionItems(
        property,
        getEntity(uid),
        renderService.fromJson(request.getInputStream(), IdentifiableObjects.class));
  }

  private WebMessage replaceCollectionItems(String property, T object, IdentifiableObjects items)
      throws ConflictException, ForbiddenException, NotFoundException, BadRequestException {
    preUpdateItems(object, items);
    TypeReport report =
        collectionService.replaceCollectionItems(object, property, items.getIdentifiableObjects());
    postUpdateItems(object, items);
    hibernateCacheManager.clearCache();
    return typeReport(report);
  }

  @PostMapping(value = "/{uid}/{property}/{itemId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public WebMessage addCollectionItem(
      @PathVariable("uid") UID uid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String property,
      @PathVariable("itemId") UID itemId)
      throws NotFoundException, ConflictException, ForbiddenException, BadRequestException {
    T object = getEntity(uid);
    IdentifiableObjects items = new IdentifiableObjects();
    items.setAdditions(singletonList(new BaseIdentifiableObject(itemId.getValue(), "", "")));

    preUpdateItems(object, items);
    TypeReport report =
        collectionService.addCollectionItems(object, property, items.getIdentifiableObjects());
    postUpdateItems(object, items);
    hibernateCacheManager.clearCache();
    return typeReport(report);
  }

  @OpenApi.Param(IdentifiableObjects.class)
  @DeleteMapping(value = "/{uid}/{property}", consumes = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public WebMessage deleteCollectionItemsJson(
      @PathVariable("uid") UID uid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String property,
      HttpServletRequest request)
      throws IOException,
          ForbiddenException,
          ConflictException,
          NotFoundException,
          BadRequestException {
    return deleteCollectionItems(
        property,
        getEntity(uid),
        renderService.fromJson(request.getInputStream(), IdentifiableObjects.class));
  }

  private WebMessage deleteCollectionItems(String property, T object, IdentifiableObjects items)
      throws ForbiddenException, ConflictException, NotFoundException, BadRequestException {
    preUpdateItems(object, items);
    TypeReport report =
        collectionService.delCollectionItems(object, property, items.getIdentifiableObjects());
    postUpdateItems(object, items);
    hibernateCacheManager.clearCache();
    return typeReport(report);
  }

  @DeleteMapping(value = "/{uid}/{property}/{itemId}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public WebMessage deleteCollectionItem(
      @PathVariable("uid") UID uid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String property,
      @PathVariable("itemId") UID itemId,
      HttpServletResponse response)
      throws NotFoundException, ForbiddenException, ConflictException, BadRequestException {
    IdentifiableObjects items = new IdentifiableObjects();
    items.setIdentifiableObjects(List.of(new BaseIdentifiableObject(itemId.getValue(), "", "")));
    return deleteCollectionItems(property, getEntity(uid), items);
  }

  @OpenApi.Param(Sharing.class)
  @PutMapping(value = "/{uid}/sharing", consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public WebMessage setSharing(
      @PathVariable("uid") UID uid,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request)
      throws IOException, ForbiddenException, NotFoundException {
    T entity = manager.get(getEntityClass(), uid);

    if (entity == null) {
      throw new NotFoundException(getEntityClass(), uid);
    }

    updatePermissionCheck(currentUser, entity);

    Sharing sharingObject = renderService.fromJson(request.getInputStream(), Sharing.class);

    TypeReport typeReport = new TypeReport(Sharing.class);

    typeReport.addObjectReport(sharingService.saveSharing(getEntityClass(), entity, sharingObject));

    if (typeReport.hasErrorReports()) {
      return typeReport(typeReport);
    }
    return null;
  }

  // --------------------------------------------------------------------------
  // Hooks
  // --------------------------------------------------------------------------

  protected T deserializeJsonEntity(HttpServletRequest request) throws IOException {
    return renderService.fromJson(request.getInputStream(), getEntityClass());
  }

  protected T deserializeXmlEntity(HttpServletRequest request) throws IOException {
    return renderService.fromXml(request.getInputStream(), getEntityClass());
  }

  protected void preCreateEntity(T entity) throws ConflictException {}

  protected void postCreateEntity(T entity) {}

  protected void preUpdateEntity(T entity, T newEntity) throws ConflictException {}

  protected void postUpdateEntity(T entity) {}

  protected void preDeleteEntity(T entity) throws ConflictException {}

  protected void postDeleteEntity(UID entityUid) {}

  protected void prePatchEntity(T entity, T newEntity) throws ConflictException {}

  protected void postPatchEntity(JsonPatch patch, T entityAfter) {}

  protected void preUpdateItems(T entity, IdentifiableObjects items) throws ConflictException {}

  protected void postUpdateItems(T entity, IdentifiableObjects items) {}

  protected void updatePermissionCheck(UserDetails currentUser, T persistedObject)
      throws ForbiddenException {
    if (!aclService.canUpdate(currentUser, persistedObject)) {
      throw new ForbiddenException("You don't have the proper permissions to update this object.");
    }
  }
}
