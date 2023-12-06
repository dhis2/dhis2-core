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
package org.hisp.dhis.dxf2.metadata;

import static org.hisp.dhis.dxf2.metadata.objectbundle.EventReportCompatibilityGuard.handleDeprecationIfEventReport;

import com.google.common.base.Enums;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataMergeMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultMetadataImportService implements MetadataImportService {
  private final CurrentUserService currentUserService;

  private final ObjectBundleService objectBundleService;
  private final ObjectBundleValidationService objectBundleValidationService;
  private final UserService userService;
  private final AclService aclService;
  private final Notifier notifier;

  @Override
  @Transactional
  public ImportReport importMetadata(
      @Nonnull MetadataImportParams params, @Nonnull MetadataObjects objects) {
    return importMetadata(params, objects, NoopJobProgress.INSTANCE);
  }

  @Override
  @Transactional
  public ImportReport importMetadata(
      @Nonnull MetadataImportParams params,
      @Nonnull MetadataObjects objects,
      @Nonnull JobProgress progress) {

    ObjectBundleParams bundleParams = toObjectBundleParams(params);
    bundleParams.setObjects(objects.getObjects());

    progress.startingStage("Running preCreateBundle");
    progress.runStage(() -> preCreateBundle(bundleParams));
    handleDeprecationIfEventReport(bundleParams);

    progress.startingStage("Creating bundle");
    ObjectBundle bundle = progress.runStage(() -> objectBundleService.create(bundleParams));

    progress.startingStage("Running postCreateBundle");
    progress.runStage(() -> postCreateBundle(bundle, bundleParams));

    progress.startingStage("Validating bundle");
    ObjectBundleValidationReport validationReport =
        progress.runStage(() -> objectBundleValidationService.validate(bundle));
    ImportReport report = new ImportReport();
    report.setImportParams(params);
    report.setStatus(Status.OK);
    report.addTypeReports(validationReport);

    if (!validationReport.hasErrorReports() || AtomicMode.NONE == bundle.getAtomicMode()) {
      Timer commitTimer = new SystemTimer().start();

      ObjectBundleCommitReport commitReport = objectBundleService.commit(bundle, progress);
      report.addTypeReports(commitReport);

      if (report.hasErrorReports()) {
        report.setStatus(Status.WARNING);
      }

      log.info("(" + bundle.getUsername() + ") Import:Commit took " + commitTimer.toString());
    } else {
      report.getStats().ignored();
      report.getTypeReports().forEach(tr -> tr.getStats().ignored());
      report.setStatus(Status.ERROR);
    }

    if (ObjectBundleMode.VALIDATE == bundleParams.getObjectBundleMode()) {
      return report;
    }

    report.clean();
    report.forEachTypeReport(
        typeReport -> {
          ImportReportMode mode = params.getImportReportMode();
          if (ImportReportMode.ERRORS == mode) {
            typeReport.clean();
          }
          if (ImportReportMode.DEBUG != mode) {
            typeReport
                .getObjectReports()
                .forEach(objectReport -> objectReport.setDisplayName(null));
          }
        });

    return report;
  }

  @Override
  public MetadataImportParams getParamsFromMap(Map<String, List<String>> parameters) {
    MetadataImportParams params = new MetadataImportParams();
    params.setSkipSharing(getBooleanWithDefault(parameters, "skipSharing", false));
    params.setSkipTranslation(getBooleanWithDefault(parameters, "skipTranslation", false));
    params.setSkipValidation(getBooleanWithDefault(parameters, "skipValidation", false));
    params.setUserOverrideMode(
        getEnumWithDefault(
            UserOverrideMode.class, parameters, "userOverrideMode", UserOverrideMode.NONE));
    params.setImportMode(
        getEnumWithDefault(
            ObjectBundleMode.class, parameters, "importMode", ObjectBundleMode.COMMIT));
    params.setPreheatMode(
        getEnumWithDefault(PreheatMode.class, parameters, "preheatMode", PreheatMode.REFERENCE));
    params.setIdentifier(
        getEnumWithDefault(
            PreheatIdentifier.class, parameters, "identifier", PreheatIdentifier.UID));
    params.setImportStrategy(
        getEnumWithDefault(
            ImportStrategy.class, parameters, "importStrategy", ImportStrategy.CREATE_AND_UPDATE));
    params.setAtomicMode(
        getEnumWithDefault(AtomicMode.class, parameters, "atomicMode", AtomicMode.ALL));
    params.setMergeMode(
        getEnumWithDefault(
            MetadataMergeMode.class, parameters, "mergeMode", MetadataMergeMode.REPLACE));
    params.setFlushMode(
        getEnumWithDefault(FlushMode.class, parameters, "flushMode", FlushMode.AUTO));
    params.setImportReportMode(
        getEnumWithDefault(
            ImportReportMode.class, parameters, "importReportMode", ImportReportMode.ERRORS));
    params.setFirstRowIsHeader(getBooleanWithDefault(parameters, "firstRowIsHeader", true));
    params.setAsync(getBooleanWithDefault(parameters, "async", false));

    if (params.getUserOverrideMode() == UserOverrideMode.SELECTED) {
      UID overrideUser = null;

      if (parameters.containsKey("overrideUser")) {
        List<String> overrideUsers = parameters.get("overrideUser");
        overrideUser = UID.of(overrideUsers.get(0));
      }

      if (overrideUser == null) {
        throw new MetadataImportException(
            "UserOverrideMode.SELECTED is enabled, but overrideUser parameter does not point to a valid user.");
      }
      params.setOverrideUser(overrideUser);
    }

    return params;
  }

  // -----------------------------------------------------------------------------------
  // Utility Methods
  // -----------------------------------------------------------------------------------

  public ObjectBundleParams toObjectBundleParams(MetadataImportParams importParams) {
    ObjectBundleParams params = new ObjectBundleParams();
    params.setUserOverrideMode(importParams.getUserOverrideMode());
    params.setSkipSharing(importParams.isSkipSharing());
    params.setSkipTranslation(importParams.isSkipTranslation());
    params.setSkipValidation(importParams.isSkipValidation());
    params.setImportStrategy(importParams.getImportStrategy());
    params.setAtomicMode(importParams.getAtomicMode());
    params.setPreheatIdentifier(importParams.getIdentifier());
    params.setPreheatMode(importParams.getPreheatMode());
    params.setObjectBundleMode(importParams.getImportMode());
    params.setMetadataMergeMode(importParams.getMergeMode());
    params.setFlushMode(importParams.getFlushMode());
    params.setImportReportMode(importParams.getImportReportMode());
    params.setMetadataSyncImport(importParams.isMetadataSyncImport());
    params.setUser(
        importParams.getUser() == null
            ? currentUserService.getCurrentUser()
            : userService.getUser(importParams.getUser().getValue()));
    params.setOverrideUser(
        importParams.getOverrideUser() == null
            ? null
            : userService.getUser(importParams.getOverrideUser().getValue()));
    if (params.getUserOverrideMode() == UserOverrideMode.CURRENT) {
      params.setOverrideUser(currentUserService.getCurrentUser());
    }
    return params;
  }

  private boolean getBooleanWithDefault(
      Map<String, List<String>> parameters, String key, boolean defaultValue) {
    if (parameters == null || parameters.get(key) == null || parameters.get(key).isEmpty()) {
      return defaultValue;
    }

    String value = String.valueOf(parameters.get(key).get(0));

    return "true".equals(value.toLowerCase());
  }

  private <T extends Enum<T>> T getEnumWithDefault(
      Class<T> enumKlass, Map<String, List<String>> parameters, String key, T defaultValue) {
    if (parameters == null || parameters.get(key) == null || parameters.get(key).isEmpty()) {
      return defaultValue;
    }

    String value = String.valueOf(parameters.get(key).get(0));

    return Enums.getIfPresent(enumKlass, value).or(defaultValue);
  }

  private void preCreateBundle(ObjectBundleParams params) {
    if (params.getUser() == null) {
      return;
    }

    for (Class<? extends IdentifiableObject> klass : params.getObjects().keySet()) {
      params
          .getObjects()
          .get(klass)
          .forEach(o -> preCreateBundleObject((BaseIdentifiableObject) o, params));
    }
  }

  private void preCreateBundleObject(BaseIdentifiableObject object, ObjectBundleParams params) {
    if (StringUtils.isEmpty(object.getSharing().getPublicAccess())) {
      aclService.resetSharing(object, params.getUser());
    }

    object.setLastUpdatedBy(params.getUser());
  }

  private void postCreateBundle(ObjectBundle bundle, ObjectBundleParams params) {
    if (bundle.getUser() == null) {
      return;
    }

    bundle.forEach(object -> postCreateBundleObject(object, bundle, params));
  }

  private void postCreateBundleObject(
      IdentifiableObject object, ObjectBundle bundle, ObjectBundleParams params) {
    if (object.getCreatedBy() == null) {
      return;
    }

    IdentifiableObject userByReference =
        bundle
            .getPreheat()
            .get(
                params.getPreheatIdentifier(),
                User.class,
                params.getPreheatIdentifier().getIdentifier(object.getCreatedBy()));

    if (userByReference != null) {
      object.setCreatedBy((User) userByReference);
    }
  }
}
