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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.dxf2.metadata.objectbundle.EventReportCompatibilityGuard.handleDeprecationIfEventReport;
import static org.hisp.dhis.eventhook.EventUtils.metadataCreate;
import static org.hisp.dhis.eventhook.EventUtils.metadataDelete;
import static org.hisp.dhis.eventhook.EventUtils.metadataUpdate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.metadata.FlushMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.eventhook.EventHookPublisher;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatParams;
import org.hisp.dhis.preheat.PreheatService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.schema.MergeParams;
import org.hisp.dhis.schema.MergeService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultObjectBundleService implements ObjectBundleService {

  private final CurrentUserService currentUserService;
  private final PreheatService preheatService;
  private final SchemaService schemaService;
  private final SessionFactory sessionFactory;
  private final IdentifiableObjectManager manager;
  private final DbmsManager dbmsManager;
  private final HibernateCacheManager cacheManager;
  private final MergeService mergeService;
  private final ObjectBundleHooks objectBundleHooks;
  private final EventHookPublisher eventHookPublisher;

  @Override
  @Transactional(readOnly = true)
  public ObjectBundle create(ObjectBundleParams params) {
    PreheatParams preheatParams = params.getPreheatParams();

    if (params.getUser() == null) {
      params.setUser(currentUserService.getCurrentUser());
    }

    preheatParams.setUser(params.getUser());
    preheatParams.setObjects(params.getObjects());

    Preheat preheat = preheatService.preheat(preheatParams);

    ObjectBundle bundle = new ObjectBundle(params, preheat, params.getObjects());
    bundle.setObjectBundleStatus(ObjectBundleStatus.CREATED);
    bundle.setObjectReferences(preheatService.collectObjectReferences(params.getObjects()));

    return bundle;
  }

  @Override
  @Transactional
  public ObjectBundleCommitReport commit(ObjectBundle bundle) {
    return commit(bundle, NoopJobProgress.INSTANCE);
  }

  @Override
  @Transactional
  public ObjectBundleCommitReport commit(ObjectBundle bundle, JobProgress progress) {

    Map<Class<?>, TypeReport> typeReports = new HashMap<>();
    ObjectBundleCommitReport commitReport = new ObjectBundleCommitReport(typeReports);

    if (ObjectBundleMode.VALIDATE == bundle.getObjectBundleMode()) {
      return commitReport; // skip if validate only
    }

    List<Class<? extends IdentifiableObject>> klasses = getSortedClasses(bundle);
    Session session = sessionFactory.getCurrentSession();

    List<ObjectBundleHook<?>> commitHooks = objectBundleHooks.getCommitHooks(klasses);
    commitHooks.forEach(hook -> hook.preCommit(bundle));

    for (Class<? extends IdentifiableObject> klass : klasses) {
      commitObjectType(bundle, typeReports, session, klass, progress);
    }

    if (!bundle.getImportMode().isDelete()) {
      commitHooks.forEach(hook -> hook.postCommit(bundle));
    }

    dbmsManager.clearSession();
    cacheManager.clearCache();

    bundle.setObjectBundleStatus(ObjectBundleStatus.COMMITTED);

    return commitReport;
  }

  private <T extends IdentifiableObject> void commitObjectType(
      ObjectBundle bundle,
      Map<Class<?>, TypeReport> typeReports,
      Session session,
      Class<T> klass,
      JobProgress progress) {
    List<T> nonPersistedObjects = bundle.getObjects(klass, false);
    List<T> persistedObjects = bundle.getObjects(klass, true);

    List<ObjectBundleHook<T>> hooks = objectBundleHooks.getTypeImportHooks(klass);
    hooks.forEach(hook -> hook.preTypeImport(klass, nonPersistedObjects, bundle));

    if (bundle.getImportMode().isCreateAndUpdate()) {
      TypeReport report = new TypeReport(klass);
      report.merge(handleCreates(session, klass, nonPersistedObjects, bundle, progress));
      report.merge(handleUpdates(session, klass, persistedObjects, bundle, progress));
      typeReports.put(klass, report);
    } else if (bundle.getImportMode().isCreate()) {
      typeReports.put(klass, handleCreates(session, klass, nonPersistedObjects, bundle, progress));
    } else if (bundle.getImportMode().isUpdate()) {
      typeReports.put(klass, handleUpdates(session, klass, persistedObjects, bundle, progress));
    } else if (bundle.getImportMode().isDelete()) {
      typeReports.put(klass, handleDeletes(session, klass, persistedObjects, bundle, progress));
    }

    hooks.forEach(hook -> hook.postTypeImport(klass, persistedObjects, bundle));

    if (FlushMode.AUTO == bundle.getFlushMode()) {
      session.flush();
    }
  }

  // -----------------------------------------------------------------------------------
  // Utility Methods
  // -----------------------------------------------------------------------------------

  private <T extends IdentifiableObject> TypeReport handleCreates(
      Session session, Class<T> klass, List<T> objects, ObjectBundle bundle, JobProgress progress) {
    TypeReport typeReport = new TypeReport(klass);

    handleDeprecationIfEventReport(klass, objects);

    if (objects.isEmpty()) {
      return typeReport;
    }

    progress.startingStage("Running preCreate bundle hooks", objects.size());
    progress.runStage(
        objects,
        IdentifiableObject::getName,
        object ->
            objectBundleHooks
                .getObjectHooks(object)
                .forEach(hook -> hook.preCreate(object, bundle)));

    session.flush();

    String message =
        "Creating %s object(s) of type %s (%s)"
            .formatted(
                objects.size(), objects.get(0).getClass().getSimpleName(), bundle.getUsername());
    progress.startingStage(message, objects.size());
    progress.runStage(
        objects,
        IdentifiableObject::getName,
        object -> {
          ObjectReport objectReport = new ObjectReport(object, bundle);
          objectReport.setDisplayName(IdentifiableObjectUtils.getDisplayName(object));
          typeReport.addObjectReport(objectReport);

          preheatService.connectReferences(
              object, bundle.getPreheat(), bundle.getPreheatIdentifier());

          if (bundle.getOverrideUser() != null) {
            object.setCreatedBy(bundle.getOverrideUser());

            if (object instanceof User) {
              (object).setCreatedBy(bundle.getOverrideUser());
            }
          }

          session.save(object);

          bundle.getPreheat().replace(bundle.getPreheatIdentifier(), object);

          if (log.isDebugEnabled()) {
            String msg =
                "(%s) Created object '%s'"
                    .formatted(
                        bundle.getUsername(),
                        bundle.getPreheatIdentifier().getIdentifiersWithName(object));
            log.debug(msg);
          }

          if (FlushMode.OBJECT == bundle.getFlushMode()) {
            session.flush();
          }
        });

    session.flush();

    progress.startingStage("Running postCreate bundle hooks");
    progress.runStage(
        objects,
        IdentifiableObject::getName,
        object -> {
          objectBundleHooks.getObjectHooks(object).forEach(hook -> hook.postCreate(object, bundle));
          eventHookPublisher.publishEvent(metadataCreate((BaseIdentifiableObject) object));
        });

    return typeReport;
  }

  private <T extends IdentifiableObject> TypeReport handleUpdates(
      Session session, Class<T> klass, List<T> objects, ObjectBundle bundle, JobProgress progress) {
    TypeReport typeReport = new TypeReport(klass);

    if (objects.isEmpty()) {
      return typeReport;
    }

    List<ObjectBundleHook<T>> hooks = objectBundleHooks.getTypeImportHooks(klass);

    progress.startingStage("Running preUpdate bundle hooks", objects.size());
    progress.runStage(
        objects,
        IdentifiableObject::getName,
        object -> {
          T persistedObject = bundle.getPreheat().get(bundle.getPreheatIdentifier(), object);
          hooks.forEach(hook -> hook.preUpdate(object, persistedObject, bundle));
        });

    session.flush();

    String message =
        "Updating %d object(s) of type %s (%s)"
            .formatted(
                objects.size(), objects.get(0).getClass().getSimpleName(), bundle.getUsername());
    progress.startingStage(message, objects.size());
    progress.runStage(
        objects,
        IdentifiableObject::getName,
        object -> {
          T persistedObject = bundle.getPreheat().get(bundle.getPreheatIdentifier(), object);

          ObjectReport objectReport = new ObjectReport(object, bundle);
          objectReport.setDisplayName(IdentifiableObjectUtils.getDisplayName(object));
          typeReport.addObjectReport(objectReport);

          preheatService.connectReferences(
              object, bundle.getPreheat(), bundle.getPreheatIdentifier());

          if (bundle.getMergeMode() != MergeMode.NONE) {
            mergeService.merge(
                new MergeParams<>(object, persistedObject)
                    .setMergeMode(bundle.getMergeMode())
                    .setSkipSharing(bundle.isSkipSharing())
                    .setSkipTranslation(bundle.isSkipTranslation()));
          }

          if (bundle.getOverrideUser() != null) {
            persistedObject.setCreatedBy(bundle.getOverrideUser());

            if (object instanceof User) {
              (object).setCreatedBy(bundle.getOverrideUser());
            }
          }

          session.update(persistedObject);

          bundle.getPreheat().replace(bundle.getPreheatIdentifier(), persistedObject);

          if (log.isDebugEnabled()) {
            String msg =
                "(%s) Updated object '%s'"
                    .formatted(
                        bundle.getUsername(),
                        bundle.getPreheatIdentifier().getIdentifiersWithName(persistedObject));
            log.debug(msg);
          }

          if (FlushMode.OBJECT == bundle.getFlushMode()) {
            session.flush();
          }
        });

    session.flush();

    progress.startingStage("Running postUpdate bundle hooks", objects.size());
    progress.runStage(
        objects,
        IdentifiableObject::getName,
        object -> {
          T persistedObject = bundle.getPreheat().get(bundle.getPreheatIdentifier(), object);
          hooks.forEach(hook -> hook.postUpdate(persistedObject, bundle));
          eventHookPublisher.publishEvent(metadataUpdate((BaseIdentifiableObject) object));
        });

    return typeReport;
  }

  private <T extends IdentifiableObject> TypeReport handleDeletes(
      Session session, Class<T> klass, List<T> objects, ObjectBundle bundle, JobProgress progress) {
    TypeReport typeReport = new TypeReport(klass);

    if (objects.isEmpty()) {
      return typeReport;
    }

    List<T> persistedObjects = bundle.getPreheat().getAll(bundle.getPreheatIdentifier(), objects);
    List<ObjectBundleHook<T>> hooks = objectBundleHooks.getTypeImportHooks(klass);

    String message =
        "Deleting %d object(s) of type %s (%s) "
            .formatted(
                objects.size(), objects.get(0).getClass().getSimpleName(), bundle.getUsername());
    progress.startingStage(message, persistedObjects.size());
    progress.runStage(
        persistedObjects,
        IdentifiableObject::getName,
        object -> {
          ObjectReport objectReport = new ObjectReport(object, bundle);
          objectReport.setDisplayName(IdentifiableObjectUtils.getDisplayName(object));
          typeReport.addObjectReport(objectReport);

          hooks.forEach(hook -> hook.preDelete(object, bundle));
          manager.delete(object, bundle.getUser());

          bundle.getPreheat().remove(bundle.getPreheatIdentifier(), object);

          if (log.isDebugEnabled()) {
            String msg =
                "(%s) Deleted object '%s'"
                    .formatted(
                        bundle.getUsername(),
                        bundle.getPreheatIdentifier().getIdentifiersWithName(object));
            log.debug(msg);
          }

          if (FlushMode.OBJECT == bundle.getFlushMode()) {
            session.flush();
          }
        });

    progress.startingStage("Publish deletion event for %s objects".formatted(objects.size()));
    progress.runStage(
        () ->
            objects.forEach(
                object -> eventHookPublisher.publishEvent(metadataDelete(klass, object.getUid()))));

    return typeReport;
  }

  @SuppressWarnings("unchecked")
  private List<Class<? extends IdentifiableObject>> getSortedClasses(ObjectBundle bundle) {
    return schemaService.getMetadataSchemas().stream()
        .map(schema -> (Class<? extends IdentifiableObject>) schema.getKlass())
        .filter(bundle::hasObjects)
        .collect(toList());
  }
}
