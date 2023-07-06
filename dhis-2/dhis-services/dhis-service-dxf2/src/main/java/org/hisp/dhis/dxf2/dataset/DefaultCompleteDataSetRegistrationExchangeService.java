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
package org.hisp.dhis.dxf2.dataset;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.notifications.DataSetNotificationEventPublisher;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.dataset.streaming.StreamingXmlCompleteDataSetRegistrations;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jdbc.batchhandler.CompleteDataSetRegistrationBatchHandler;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.hisp.staxwax.factory.XMLFactory;
import org.springframework.stereotype.Service;

/**
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@Service("org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistrationExchangeService")
public class DefaultCompleteDataSetRegistrationExchangeService
    implements CompleteDataSetRegistrationExchangeService {
  private static final int CACHE_MISS_THRESHOLD = 500;

  private static final Set<IdScheme> EXPORT_ID_SCHEMES =
      ImmutableSet.of(IdScheme.UID, IdScheme.NAME, IdScheme.CODE);

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final CompleteDataSetRegistrationExchangeStore cdsrStore;

  private final IdentifiableObjectManager idObjManager;

  private final OrganisationUnitService orgUnitService;

  private final Notifier notifier;

  private final I18nManager i18nManager;

  private final BatchHandlerFactory batchHandlerFactory;

  private final SystemSettingManager systemSettingManager;

  private final CategoryService categoryService;

  private final PeriodService periodService;

  private final CurrentUserService currentUserService;

  private final CompleteDataSetRegistrationService registrationService;

  private final InputUtils inputUtils;

  private final AggregateAccessManager accessManager;

  private final DataSetNotificationEventPublisher notificationPublisher;

  private final MessageService messageService;

  private final ObjectMapper jsonMapper;

  private final OrganisationUnitService organisationUnitService;

  public DefaultCompleteDataSetRegistrationExchangeService(
      CompleteDataSetRegistrationExchangeStore cdsrStore,
      IdentifiableObjectManager idObjManager,
      OrganisationUnitService orgUnitService,
      Notifier notifier,
      I18nManager i18nManager,
      BatchHandlerFactory batchHandlerFactory,
      SystemSettingManager systemSettingManager,
      CategoryService categoryService,
      PeriodService periodService,
      CurrentUserService currentUserService,
      CompleteDataSetRegistrationService registrationService,
      InputUtils inputUtils,
      AggregateAccessManager accessManager,
      DataSetNotificationEventPublisher notificationPublisher,
      MessageService messageService,
      ObjectMapper jsonMapper,
      OrganisationUnitService organisationUnitService) {
    checkNotNull(cdsrStore);
    checkNotNull(idObjManager);
    checkNotNull(orgUnitService);
    checkNotNull(notifier);
    checkNotNull(i18nManager);
    checkNotNull(batchHandlerFactory);
    checkNotNull(systemSettingManager);
    checkNotNull(systemSettingManager);
    checkNotNull(categoryService);
    checkNotNull(periodService);
    checkNotNull(currentUserService);
    checkNotNull(registrationService);
    checkNotNull(inputUtils);
    checkNotNull(accessManager);
    checkNotNull(notificationPublisher);
    checkNotNull(messageService);
    checkNotNull(jsonMapper);
    checkNotNull(organisationUnitService);

    this.cdsrStore = cdsrStore;
    this.idObjManager = idObjManager;
    this.orgUnitService = orgUnitService;
    this.notifier = notifier;
    this.i18nManager = i18nManager;
    this.batchHandlerFactory = batchHandlerFactory;
    this.systemSettingManager = systemSettingManager;
    this.categoryService = categoryService;
    this.periodService = periodService;
    this.currentUserService = currentUserService;
    this.registrationService = registrationService;
    this.inputUtils = inputUtils;
    this.accessManager = accessManager;
    this.notificationPublisher = notificationPublisher;
    this.messageService = messageService;
    this.jsonMapper = jsonMapper;
    this.organisationUnitService = organisationUnitService;
  }

  // -------------------------------------------------------------------------
  // CompleteDataSetRegistrationService implementation
  // -------------------------------------------------------------------------

  @Override
  public ExportParams paramsFromUrl(
      Set<String> dataSets,
      Set<String> orgUnits,
      Set<String> orgUnitGroups,
      Set<String> periods,
      Date startDate,
      Date endDate,
      boolean includeChildren,
      Date created,
      String createdDuration,
      Integer limit,
      IdSchemes idSchemes) {
    ExportParams params = new ExportParams();

    if (dataSets != null) {
      params
          .getDataSets()
          .addAll(idObjManager.getObjects(DataSet.class, IdentifiableProperty.UID, dataSets));
    }

    if (orgUnits != null) {
      params
          .getOrganisationUnits()
          .addAll(
              idObjManager.getObjects(OrganisationUnit.class, IdentifiableProperty.UID, orgUnits));
    }

    if (orgUnitGroups != null) {
      params
          .getOrganisationUnitGroups()
          .addAll(
              idObjManager.getObjects(
                  OrganisationUnitGroup.class, IdentifiableProperty.UID, orgUnitGroups));
    }

    if (periods != null && !periods.isEmpty()) {
      params.getPeriods().addAll(periodService.reloadIsoPeriods(new ArrayList<>(periods)));
    } else if (startDate != null && endDate != null) {
      params.setStartDate(startDate).setEndDate(endDate);
    }

    params
        .setIncludeChildren(includeChildren)
        .setCreated(created)
        .setCreatedDuration(createdDuration)
        .setLimit(limit)
        .setOutputIdSchemes(idSchemes);

    return params;
  }

  @Override
  public void writeCompleteDataSetRegistrationsXml(ExportParams params, OutputStream out) {
    decideAccess(params);
    validate(params);

    cdsrStore.writeCompleteDataSetRegistrationsXml(params, out);
  }

  @Override
  public void writeCompleteDataSetRegistrationsJson(ExportParams params, OutputStream out) {
    decideAccess(params);
    validate(params);

    cdsrStore.writeCompleteDataSetRegistrationsJson(params, out);
  }

  @Override
  public void writeCompleteDataSetRegistrationsJson(
      Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes) {
    cdsrStore.writeCompleteDataSetRegistrationsJson(lastUpdated, outputStream, idSchemes);
  }

  @Override
  public ImportSummary saveCompleteDataSetRegistrationsXml(
      InputStream in, ImportOptions importOptions) {
    return saveCompleteDataSetRegistrationsXml(in, importOptions, null);
  }

  @Override
  public ImportSummary saveCompleteDataSetRegistrationsXml(
      InputStream in, ImportOptions importOptions, JobConfiguration jobId) {
    try {
      in = StreamUtils.wrapAndCheckCompressionFormat(in);
      CompleteDataSetRegistrations completeDataSetRegistrations =
          new StreamingXmlCompleteDataSetRegistrations(XMLFactory.getXMLReader(in));

      return saveCompleteDataSetRegistrations(importOptions, jobId, completeDataSetRegistrations);
    } catch (Exception ex) {
      return handleImportError(jobId, ex);
    }
  }

  @Override
  public ImportSummary saveCompleteDataSetRegistrationsJson(
      InputStream in, ImportOptions importOptions) {
    return saveCompleteDataSetRegistrationsJson(in, importOptions, null);
  }

  @Override
  public ImportSummary saveCompleteDataSetRegistrationsJson(
      InputStream in, ImportOptions importOptions, JobConfiguration jobId) {
    try {
      in = StreamUtils.wrapAndCheckCompressionFormat(in);

      CompleteDataSetRegistrations completeDataSetRegistrations =
          jsonMapper.readValue(in, CompleteDataSetRegistrations.class);

      return saveCompleteDataSetRegistrations(importOptions, jobId, completeDataSetRegistrations);
    } catch (Exception ex) {
      return handleImportError(jobId, ex);
    }
  }

  public void validate(ExportParams params) throws IllegalQueryException {
    ErrorMessage error = null;

    if (params == null) {
      throw new IllegalQueryException(ErrorCode.E2000);
    }

    if (params.getDataSets().isEmpty()) {
      error = new ErrorMessage(ErrorCode.E2013);
    }

    if (!params.hasPeriods()
        && !params.hasStartEndDate()
        && !params.hasCreated()
        && !params.hasCreatedDuration()) {
      error = new ErrorMessage(ErrorCode.E2002);
    }

    if (params.hasPeriods() && params.hasStartEndDate()) {
      error = new ErrorMessage(ErrorCode.E2003);
    }

    if (params.hasStartEndDate() && params.getStartDate().after(params.getEndDate())) {
      error = new ErrorMessage(ErrorCode.E2004);
    }

    if (params.hasCreatedDuration() && DateUtils.getDuration(params.getCreatedDuration()) == null) {
      error = new ErrorMessage(ErrorCode.E2005);
    }

    if (!params.hasOrganisationUnits() && !params.hasOrganisationUnitGroups()) {
      error = new ErrorMessage(ErrorCode.E2006);
    }

    if (params.isIncludeChildren() && params.hasOrganisationUnitGroups()) {
      error = new ErrorMessage(ErrorCode.E2007);
    }

    if (params.isIncludeChildren() && !params.hasOrganisationUnits()) {
      error = new ErrorMessage(ErrorCode.E2008);
    }

    if (params.hasLimit() && params.getLimit() < 0) {
      error = new ErrorMessage(ErrorCode.E2009);
    }

    if (error != null) {
      log.warn(
          String.format(
              "Complete data set registration validation failed, code: '%s', message: '%s'",
              error.getErrorCode(), error.getMessage()));

      throw new IllegalQueryException(error);
    }

    limitToValidIdSchemes(params);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Limit valid IdSchemes for export to UID, CODE, NAME */
  private static void limitToValidIdSchemes(ExportParams params) {
    IdSchemes schemes = params.getOutputIdSchemes();

    // If generic IdScheme is set to ID -> override to UID, for others:
    // nullify field (inherits from generic scheme)

    if (!EXPORT_ID_SCHEMES.contains(schemes.getIdScheme())) {
      schemes.setIdScheme(IdScheme.UID.getIdentifiableString());
    }

    if (!EXPORT_ID_SCHEMES.contains(schemes.getDataSetIdScheme())) {
      schemes.setDataSetIdScheme(IdScheme.UID.getIdentifiableString());
    }

    if (!EXPORT_ID_SCHEMES.contains(schemes.getOrgUnitIdScheme())) {
      schemes.setOrgUnitIdScheme(IdScheme.UID.getIdentifiableString());
    }

    if (!EXPORT_ID_SCHEMES.contains(schemes.getAttributeOptionComboIdScheme())) {
      schemes.setAttributeOptionComboIdScheme(IdScheme.UID.getIdentifiableString());
    }

    params.setOutputIdSchemes(schemes);
  }

  private void decideAccess(ExportParams params) throws IllegalQueryException {
    for (OrganisationUnit ou : params.getOrganisationUnits()) {
      if (!orgUnitService.isInUserHierarchy(ou)) {
        throw new IllegalQueryException(new ErrorMessage(ErrorCode.E2012, ou.getUid()));
      }
    }
  }

  private ImportSummary handleImportError(JobConfiguration jobId, Throwable ex) {
    log.error(DebugUtils.getStackTrace(ex));
    notifier.notify(jobId, NotificationLevel.ERROR, "Process failed: " + ex.getMessage(), true);
    return new ImportSummary(ImportStatus.ERROR, "The import process failed: " + ex.getMessage());
  }

  private ImportSummary saveCompleteDataSetRegistrations(
      ImportOptions importOptions,
      JobConfiguration id,
      CompleteDataSetRegistrations completeRegistrations) {
    Clock clock =
        new Clock(log)
            .startClock()
            .logTime("Starting complete data set registration import, options: " + importOptions);
    notifier.clear(id).notify(id, "Process started");

    // Start here so we can access any outer attributes for the
    // configuration
    completeRegistrations.open();

    ImportSummary importSummary = new ImportSummary();

    // ---------------------------------------------------------------------
    // Set up import configuration
    // ---------------------------------------------------------------------

    importOptions = importOptions != null ? importOptions : ImportOptions.getDefaultImportOptions();

    log.info("Import options: " + importOptions);

    ImportConfig cfg =
        new ImportConfig(
            this.systemSettingManager, this.categoryService, completeRegistrations, importOptions);

    // ---------------------------------------------------------------------
    // Set up meta-data
    // ---------------------------------------------------------------------

    MetadataCaches caches = new MetadataCaches();
    MetadataCallables metaDataCallables =
        new MetadataCallables(cfg, this.idObjManager, this.periodService, this.categoryService);

    if (importOptions.isPreheatCacheDefaultFalse()) {
      caches.preheat(idObjManager, cfg);
    }

    // ---------------------------------------------------------------------
    // Perform import
    // ---------------------------------------------------------------------

    notifier.notify(id, "Importing complete data set registrations");

    int totalCount =
        batchImport(completeRegistrations, cfg, importSummary, metaDataCallables, caches);

    notifier
        .notify(id, NotificationLevel.INFO, "Import done", true)
        .addJobSummary(id, importSummary, ImportSummary.class);

    ImportCount count = importSummary.getImportCount();

    clock.logTime(
        String.format(
            "Complete data set registration import done, total: %d, imported: %d, updated: %d, deleted: %d",
            totalCount, count.getImported(), count.getUpdated(), count.getDeleted()));

    completeRegistrations.close();

    return importSummary;
  }

  /**
   * @return total number of processed CompleteDataSetRegistration objects
   */
  private int batchImport(
      CompleteDataSetRegistrations completeRegistrations,
      ImportConfig config,
      ImportSummary summary,
      MetadataCallables mdCallables,
      MetadataCaches mdCaches) {
    final User currentUser = currentUserService.getCurrentUser();
    final String currentUserName = currentUser.getUsername();
    final Set<OrganisationUnit> userOrgUnits = currentUserService.getCurrentUserOrganisationUnits();
    final I18n i18n = i18nManager.getI18n();

    BatchHandler<CompleteDataSetRegistration> batchHandler =
        batchHandlerFactory
            .createBatchHandler(CompleteDataSetRegistrationBatchHandler.class)
            .init();

    int importCount = 0, updateCount = 0, deleteCount = 0, totalCount = 0;

    Date now = new Date();

    while (completeRegistrations.hasNextCompleteDataSetRegistration()) {
      org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration cdsr =
          completeRegistrations.getNextCompleteDataSetRegistration();
      totalCount++;

      // ---------------------------------------------------------------------
      // Init meta-data properties against meta-data cache
      // ---------------------------------------------------------------------

      MetadataProperties mdProps = initMetaDataProperties(cdsr, mdCallables, mdCaches);

      heatCaches(mdCaches, config);

      // ---------------------------------------------------------------------
      // Meta-data validation
      // ---------------------------------------------------------------------

      String storedBy;
      String lastUpdatedBy;
      Boolean isCompleted;

      try {
        // Validate CDSR meta-data properties

        mdProps.validate(cdsr, config);
        validateOrgUnitInUserHierarchy(mdCaches, mdProps, userOrgUnits, currentUserName);

        // Constraints validation

        if (config.isStrictAttrOptionCombos()) {
          validateAocMatchesDataSetCc(mdProps);
        }

        validateAttrOptCombo(mdProps, mdCaches, config);

        if (config.isStrictPeriods()) {
          validateHasMatchingPeriodTypes(mdProps);
        }

        if (config.isStrictOrgUnits()) {
          validateDataSetIsAssignedToOrgUnit(mdProps);
        }

        storedBy = cdsr.getStoredBy();
        validateStoredBy(storedBy, i18n);
        storedBy = StringUtils.isBlank(storedBy) ? currentUserName : storedBy;

        lastUpdatedBy = cdsr.getLastUpdatedBy();
        validateStoredBy(lastUpdatedBy, i18n);
        lastUpdatedBy = StringUtils.isBlank(lastUpdatedBy) ? currentUserName : lastUpdatedBy;

        cdsr.setLastUpdatedBy(lastUpdatedBy);

        boolean DEFAULT_COMPLETENESS_STATUS = true;
        isCompleted = cdsr.getCompleted();
        isCompleted = (isCompleted == null) ? DEFAULT_COMPLETENESS_STATUS : isCompleted;
        cdsr.setCompleted(isCompleted);

        // TODO Check if Period is within range of data set?
      } catch (ImportConflictException ic) {
        summary.addConflict(ic.getImportConflict().getObject(), ic.getImportConflict().getValue());
        continue;
      }

      // ---------------------------------------------------------------------
      // Compulsory fields validation
      // ---------------------------------------------------------------------

      List<DataElementOperand> missingDataElementOperands =
          registrationService.getMissingCompulsoryFields(
              mdProps.dataSet, mdProps.period,
              mdProps.orgUnit, mdProps.attrOptCombo);

      if (!missingDataElementOperands.isEmpty()) {
        for (DataElementOperand dataElementOperand : missingDataElementOperands) {
          summary.addConflict(
              "dataElementOperand",
              dataElementOperand.getDisplayName() + " needs to be filled. It is compulsory.");
        }

        if (mdProps.dataSet.isCompulsoryFieldsCompleteOnly()) {
          continue;
        }
      }

      // ---------------------------------------------------------------------
      // Data Sharing check
      // ---------------------------------------------------------------------

      List<String> errors = validateDataAccess(currentUser, mdProps);
      if (!errors.isEmpty()) {
        errors.forEach(error -> summary.addConflict("dataSet", error));
        continue;
      }

      // -----------------------------------------------------------------
      // Create complete data set registration
      // -----------------------------------------------------------------

      CompleteDataSetRegistration internalCdsr =
          createCompleteDataSetRegistration(cdsr, mdProps, now, storedBy);

      CompleteDataSetRegistration existingCdsr =
          config.isSkipExistingCheck() ? null : batchHandler.findObject(internalCdsr);

      ImportStrategy strategy = config.getStrategy();

      boolean isDryRun = config.isDryRun();

      if (!config.isSkipExistingCheck() && existingCdsr != null) {
        // CDSR already exists

        if (strategy.isCreateAndUpdate() || strategy.isUpdate() || strategy.isSync()) {
          // Update existing CDSR

          updateCount++;

          if (!isDryRun) {
            batchHandler.updateObject(internalCdsr);
          }
        } else if (strategy.isDelete()) {
          // TODO Does 'delete' even make sense for CDSR?

          // Replace existing CDSR

          deleteCount++;

          if (!isDryRun) {
            batchHandler.deleteObject(internalCdsr);
          }
        }
      } else {
        // CDSR does not already exist

        if (strategy.isCreateAndUpdate() || strategy.isCreate() || strategy.isSync()) {
          if (existingCdsr != null) {
            // Already exists -> update

            importCount++;

            if (!isDryRun) {
              batchHandler.updateObject(internalCdsr);
            }
          } else {
            // Does not exist -> add new CDSR

            boolean added = false;

            if (!isDryRun) {
              added = batchHandler.addObject(internalCdsr);

              if (added) {
                sendNotifications(config, internalCdsr);
              }
            }

            if (isDryRun || added) {
              importCount++;
            }
          }
        }
      }
    }

    batchHandler.flush();

    finalizeSummary(summary, totalCount, importCount, updateCount, deleteCount);

    return totalCount;
  }

  private static void finalizeSummary(
      ImportSummary summary, int totalCount, int importCount, int updateCount, int deleteCount) {
    int ignores = totalCount - importCount - updateCount - deleteCount;

    summary.setImportCount(new ImportCount(importCount, updateCount, ignores, deleteCount));
    summary.setStatus(totalCount == ignores ? ImportStatus.ERROR : ImportStatus.SUCCESS);
    summary.setDescription("Import process complete.");
  }

  private static CompleteDataSetRegistration createCompleteDataSetRegistration(
      org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration cdsr,
      MetadataProperties mdProps,
      Date now,
      String storedBy) {
    Date date = cdsr.hasDate() ? DateUtils.parseDate(cdsr.getDate()) : now;

    return new CompleteDataSetRegistration(
        mdProps.dataSet,
        mdProps.period,
        mdProps.orgUnit,
        mdProps.attrOptCombo,
        date,
        storedBy,
        date,
        cdsr.getLastUpdatedBy(),
        cdsr.getCompleted());
  }

  /**
   * Check write permission for {@see DataSet} and {@see CategoryOptionCombo}
   *
   * @param user currently logged-in user
   * @param metaDataProperties {@see MetaDataProperties} containing the objects to check
   */
  private List<String> validateDataAccess(User user, MetadataProperties metaDataProperties) {
    List<String> errors = accessManager.canWrite(user, metaDataProperties.dataSet);
    errors.addAll(accessManager.canWrite(user, metaDataProperties.attrOptCombo));
    return errors;
  }

  private void validateOrgUnitInUserHierarchy(
      MetadataCaches mdCaches,
      MetadataProperties mdProps,
      final Set<OrganisationUnit> userOrgUnits,
      String currentUsername)
      throws ImportConflictException {
    boolean inUserHierarchy =
        mdCaches
            .getOrgUnitInHierarchyMap()
            .get(
                mdProps.orgUnit.getUid(),
                () -> organisationUnitService.isDescendant(mdProps.orgUnit, userOrgUnits));

    if (!inUserHierarchy) {
      throw new ImportConflictException(
          new ImportConflict(
              mdProps.orgUnit.getUid(),
              "Organisation unit is not in hierarchy of user: " + currentUsername));
    }
  }

  private void sendNotifications(ImportConfig config, CompleteDataSetRegistration registration) {
    if (!config.isSkipNotifications()) {
      if (registration.getDataSet() != null && registration.getDataSet().isNotifyCompletingUser()) {
        messageService.sendCompletenessMessage(registration);
      }

      notificationPublisher.publishEvent(registration);
    }
  }

  private void validateAttrOptCombo(
      MetadataProperties mdProps, MetadataCaches mdCaches, ImportConfig config)
      throws ImportConflictException {
    final Period pe = mdProps.period;

    if (mdProps.attrOptCombo == null) {
      if (config.isRequireAttrOptionCombos()) {
        throw new ImportConflictException(
            new ImportConflict(
                "Attribute option combo",
                "Attribute option combo is required but is not specified"));
      } else {
        mdProps.attrOptCombo = categoryService.getDefaultCategoryOptionCombo();
      }
    }

    final CategoryOptionCombo aoc = mdProps.attrOptCombo;
    DateRange range = aoc.getDateRange(mdProps.dataSet);

    if ((range.getStartDate() != null && range.getStartDate().compareTo(pe.getStartDate()) > 0)
        || (range.getEndDate() != null && range.getEndDate().compareTo(pe.getEndDate()) < 0)) {
      throw new ImportConflictException(
          new ImportConflict(
              mdProps.orgUnit.getUid(),
              String.format(
                  "Period: %s is not within range of attribute option combo: %s",
                  pe.getIsoDate(), aoc.getUid())));
    }

    final String aocOrgUnitKey = aoc.getUid() + mdProps.orgUnit.getUid();

    boolean isOrgUnitValidForAoc =
        mdCaches
            .getAttrOptComboOrgUnitMap()
            .get(
                aocOrgUnitKey,
                () -> {
                  Set<OrganisationUnit> aocOrgUnits = aoc.getOrganisationUnits();
                  return aocOrgUnits == null
                      || organisationUnitService.isDescendant(mdProps.orgUnit, aocOrgUnits);
                });

    if (!isOrgUnitValidForAoc) {
      throw new ImportConflictException(
          new ImportConflict(
              mdProps.orgUnit.getUid(),
              String.format(
                  "Organisation unit: %s is not valid for attribute option combo %s",
                  mdProps.orgUnit.getUid(), aoc.getUid())));
    }
  }

  private static void validateStoredBy(String storedBy, I18n i18n) throws ImportConflictException {
    String result = ValidationUtils.storedByIsValid(storedBy);

    if (result == null) {
      return;
    }

    throw new ImportConflictException(new ImportConflict(storedBy, i18n.getString(result)));
  }

  private static void validateAocMatchesDataSetCc(MetadataProperties mdProps)
      throws ImportConflictException {
    // TODO MdCache?
    CategoryCombo aocCC = mdProps.attrOptCombo.getCategoryCombo();
    CategoryCombo dsCc = mdProps.dataSet.getCategoryCombo();

    if (!aocCC.equals(dsCc)) {
      throw new ImportConflictException(
          new ImportConflict(
              aocCC.getUid(),
              String.format(
                  "Attribute option combo: %s must have category combo: %s",
                  aocCC.getUid(), dsCc.getUid())));
    }
  }

  private static void validateHasMatchingPeriodTypes(MetadataProperties props)
      throws ImportConflictException {
    // TODO MdCache?
    PeriodType dsPeType = props.dataSet.getPeriodType();
    PeriodType peType = props.period.getPeriodType();

    if (!dsPeType.equals(peType)) {
      throw new ImportConflictException(
          new ImportConflict(
              props.period.getUid(),
              String.format(
                  "Period type of period: %s is not equal to the period type of data set: %s",
                  props.period.getIsoDate(), props.dataSet.getPeriodType())));
    }
  }

  private static void validateDataSetIsAssignedToOrgUnit(MetadataProperties props)
      throws ImportConflictException {
    if (!props.orgUnit.getDataSets().contains(props.dataSet)) {
      throw new ImportConflictException(
          new ImportConflict(
              props.dataSet.getUid(),
              String.format(
                  "Data set %s is not assigned to organisation unit %s",
                  props.dataSet.getUid(), props.orgUnit.getUid())));
    }
  }

  private void heatCaches(MetadataCaches caches, ImportConfig config) {
    if (!caches.getDataSets().isCacheLoaded() && exceedsThreshold(caches.getDataSets())) {
      caches
          .getDataSets()
          .load(
              idObjManager.getAll(DataSet.class), ds -> ds.getPropertyValue(config.getDsScheme()));

      log.info("Data set cache heated after cache miss threshold reached");
    }

    if (!caches.getOrgUnits().isCacheLoaded() && exceedsThreshold(caches.getOrgUnits())) {
      caches
          .getOrgUnits()
          .load(
              idObjManager.getAll(OrganisationUnit.class),
              ou -> ou.getPropertyValue(config.getOuScheme()));

      log.info("Org unit cache heated after cache miss threshold reached");
    }

    // TODO Consider need for checking/re-heating attrOptCombo and period
    // caches

    if (!caches.getAttrOptionCombos().isCacheLoaded()
        && exceedsThreshold(caches.getAttrOptionCombos())) {
      caches
          .getAttrOptionCombos()
          .load(
              idObjManager.getAll(CategoryOptionCombo.class),
              aoc -> aoc.getPropertyValue(config.getAocScheme()));

      log.info("Attribute option combo cache heated after cache miss threshold reached");
    }

    if (!caches.getPeriods().isCacheLoaded() && exceedsThreshold(caches.getPeriods())) {
      caches.getPeriods().load(idObjManager.getAll(Period.class), pe -> pe.getPropertyValue(null));
    }
  }

  private MetadataProperties initMetaDataProperties(
      org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration cdsr,
      MetadataCallables callables,
      MetadataCaches cache) {
    String ds = StringUtils.trimToNull(cdsr.getDataSet());
    String pe = StringUtils.trimToNull(cdsr.getPeriod());
    String ou = StringUtils.trimToNull(cdsr.getOrganisationUnit());
    String aoc = StringUtils.trimToNull(cdsr.getAttributeOptionCombo());

    if (aoc == null) {
      CategoryOptionCombo attributeOptionCombo =
          inputUtils.getAttributeOptionCombo(cdsr.getCc(), cdsr.getCp(), false);
      aoc = attributeOptionCombo != null ? attributeOptionCombo.getUid() : aoc;
    }
    return new MetadataProperties(
        cache.getDataSets().get(ds, callables.getDataSetCallable().setId(ds)),
        cache.getPeriods().get(pe, callables.getPeriodCallable().setId(pe)),
        cache.getOrgUnits().get(ou, callables.getOrgUnitCallable().setId(ou)),
        cache.getAttrOptionCombos().get(aoc, callables.getOptionComboCallable().setId(aoc)));
  }

  private static boolean exceedsThreshold(CachingMap<?, ?> cachingMap) {
    return cachingMap.getCacheMissCount() > CACHE_MISS_THRESHOLD;
  }

  // -----------------------------------------------------------------
  // Internal classes
  // -----------------------------------------------------------------

  private static class MetadataProperties {
    final DataSet dataSet;

    final Period period;

    final OrganisationUnit orgUnit;

    CategoryOptionCombo attrOptCombo;

    MetadataProperties(
        DataSet dataSet,
        Period period,
        OrganisationUnit orgUnit,
        CategoryOptionCombo attrOptCombo) {
      this.dataSet = dataSet;
      this.period = period;
      this.orgUnit = orgUnit;
      this.attrOptCombo = attrOptCombo;
    }

    void validate(org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration cdsr, ImportConfig config)
        throws ImportConflictException {
      if (dataSet == null) {
        throw new ImportConflictException(
            new ImportConflict(cdsr.getDataSet(), "Data set not found or not accessible"));
      }

      if (period == null) {
        throw new ImportConflictException(new ImportConflict(cdsr.getPeriod(), "Period not valid"));
      }

      if (orgUnit == null) {
        throw new ImportConflictException(
            new ImportConflict(
                cdsr.getOrganisationUnit(), "Organisation unit not found or not accessible"));
      }

      // Ensure AOC is set is required, or is otherwise set to the default
      // COC

      if (attrOptCombo == null) {
        if (config.isRequireAttrOptionCombos()) {
          throw new ImportConflictException(
              new ImportConflict(
                  "Attribute option combo",
                  "Attribute option combo is required but is not specified"));
        } else {
          attrOptCombo = config.getFallbackCatOptCombo();
        }
      }
    }
  }

  private static class ImportConflictException extends RuntimeException {
    private final ImportConflict importConflict;

    ImportConflictException(@Nonnull ImportConflict importConflict) {
      this.importConflict = importConflict;
    }

    ImportConflict getImportConflict() {
      return importConflict;
    }
  }
}
