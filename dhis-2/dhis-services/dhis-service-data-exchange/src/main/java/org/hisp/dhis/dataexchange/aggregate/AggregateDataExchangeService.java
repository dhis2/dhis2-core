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
package org.hisp.dhis.dataexchange.aggregate;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.common.DimensionConstants.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.collection.CollectionUtils.mapToList;
import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;
import static org.hisp.dhis.scheduling.RecordingJobProgress.transitory;
import static org.hisp.dhis.util.ObjectUtils.notNull;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataexchange.client.Dhis2Client;
import org.hisp.dhis.datavalue.DataEntryGroup;
import org.hisp.dhis.datavalue.DataEntryPipeline;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobProgress.FailurePolicy;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.UserDetails;
import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Main service class for aggregate data exchange.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregateDataExchangeService {
  private static final long RESET_MAX_VALUES = 500_000L;

  private final AnalyticsService analyticsService;

  private final AggregateDataExchangeStore aggregateDataExchangeStore;

  private final DataQueryService dataQueryService;

  private final DataEntryPipeline dataEntryPipeline;

  private final AclService aclService;

  private final IdentifiableObjectManager idObjectManager;

  private final PeriodService periodService;

  @Qualifier(AES_128_STRING_ENCRYPTOR)
  private final PBEStringCleanablePasswordEncryptor encryptor;

  /**
   * Retrieves an {@link AggregateDataExchange} by identifier. Throws an exception if no object with
   * the given identifier exists.
   *
   * @param uid the UID.
   * @return an {@link AggregateDataExchange}.
   * @throws IllegalQueryException if no object with the given identifier exists.
   */
  @Nonnull
  @Transactional(readOnly = true)
  public AggregateDataExchange loadByUid(String uid) {
    return aggregateDataExchangeStore.loadByUid(uid);
  }

  /**
   * Runs the analytics data exchange with the given identifier.
   *
   * @param uid the {@link AggregateDataExchange} identifier.
   * @param progress {@link JobProgress} to track progress when running in a job context
   * @return an {@link ImportSummaries}.
   */
  @Transactional
  public ImportSummaries exchangeData(UserDetails userDetails, String uid, JobProgress progress) {
    AggregateDataExchange exchange = aggregateDataExchangeStore.loadByUid(uid);

    return exchangeData(userDetails, exchange, progress);
  }

  /**
   * Runs the given analytics data exchange.
   *
   * @param userDetails CurrentUser who executing the exchange.
   * @param exchange the {@link AggregateDataExchange}.
   * @param progress {@link JobProgress} to track progress when running in a job context
   * @return an {@link ImportSummaries}.
   */
  @Transactional
  public ImportSummaries exchangeData(
      UserDetails userDetails, AggregateDataExchange exchange, JobProgress progress) {
    ImportSummaries summaries = new ImportSummaries();

    if (!aclService.canDataWrite(userDetails, exchange)) {
      summaries.addImportSummary(
          new ImportSummary(
              ImportStatus.ERROR,
              String.format(
                  "User has no data write access for AggregateDataExchange: %s",
                  exchange.getDisplayName())));
      return summaries;
    }

    progress.startingStage(toStageDescription(exchange), FailurePolicy.SKIP_ITEM);
    progress.runStage(
        exchange.getSource().getRequests().stream(),
        AggregateDataExchangeService::toItemDescription,
        AggregateDataExchangeService::toItemSummary,
        request -> {
          ImportSummary summary = exchangeData(exchange, request);
          summaries.addImportSummary(summary);
          return summary;
        },
        (success, failed) -> toStageSummary(success, failed, exchange));

    return summaries;
  }

  /**
   * Performs a reset of target data as defined by the given exchange without importing data
   * afterward.
   *
   * @param userDetails CurrentUser executing the reset.
   * @param exchange the {@link AggregateDataExchange}.
   * @return an {@link ImportSummary} describing the outcome of the reset.
   */
  @Transactional
  public ImportSummaries resetData(UserDetails userDetails, AggregateDataExchange exchange) {
    if (!aclService.canDataWrite(userDetails, exchange)) {
      ImportSummaries summaries = new ImportSummaries();
      summaries.addImportSummary(
          new ImportSummary(
              ImportStatus.ERROR,
              String.format(
                  "User has no data write access for AggregateDataExchange: %s",
                  exchange.getDisplayName())));
      return summaries;
    }

    ImportSummaries summaries = new ImportSummaries();
    ImportSummary summary =
        new ImportSummary(ImportStatus.SUCCESS, "Target data reset.", new ImportCount());

    for (SourceRequest sourceRequest : exchange.getSource().getRequests()) {
      ImportSummary result = resetTargetData(exchange, sourceRequest);
      if (result.getStatus() == ImportStatus.ERROR) {
        summaries.addImportSummary(result);
        return summaries;
      }

      ImportCount total = summary.getImportCount();
      ImportCount current = result.getImportCount();
      total.incrementImported(current.getImported());
      total.incrementUpdated(current.getUpdated());
      total.incrementIgnored(current.getIgnored());
      total.incrementDeleted(current.getDeleted());
    }

    summaries.addImportSummary(summary);
    return summaries;
  }

  /**
   * Returns the source data for the analytics data exchange with the given identifier.
   *
   * @param uid the {@link AggregateDataExchange} identifier.
   * @param params the {@link SourceDataQueryParams}.
   * @return the source data for the analytics data exchange.
   */
  public List<Grid> getSourceData(UserDetails userDetails, String uid, SourceDataQueryParams params)
      throws ForbiddenException {

    AggregateDataExchange exchange = aggregateDataExchangeStore.loadByUid(uid);

    if (!aclService.canDataRead(userDetails, exchange)) {
      throw new ForbiddenException(ErrorCode.E3012, exchange);
    }

    return mapToList(
        exchange.getSource().getRequests(),
        request -> analyticsService.getAggregatedDataValues(toDataQueryParams(request, params)));
  }

  /**
   * Returns the source data value sets for the analytics data exchange with the given identifier.
   *
   * @param uid the {@link AggregateDataExchange} identifier.
   * @param params the {@link SourceDataQueryParams}.
   * @return the source data value sets for the analytics data exchange.
   */
  public List<DataValueSet> getSourceDataValueSets(
      UserDetails userDetails, String uid, SourceDataQueryParams params) throws ForbiddenException {
    AggregateDataExchange exchange = aggregateDataExchangeStore.loadByUid(uid);

    if (!aclService.canDataRead(userDetails, exchange)) {
      throw new ForbiddenException(ErrorCode.E3012, exchange);
    }

    return mapToList(
        exchange.getSource().getRequests(),
        request -> analyticsService.getAggregatedDataValueSet(toDataQueryParams(request, params)));
  }

  /**
   * Exchanges data from the source as defined by the given {@link SourceRequest} to the target as
   * defined by the given {@link AggregateDataExchange}.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @param request the {@link SourceRequest}.
   * @return an {@link ImportSummary} describing the outcome of the exchange.
   */
  private ImportSummary exchangeData(AggregateDataExchange exchange, SourceRequest request) {
    try {
      TargetRequest targetRequest =
          exchange.getTarget() != null ? exchange.getTarget().getRequest() : null;
      if (targetRequest != null && targetRequest.isResetBeforeExchangeOrDefault()) {
        ImportSummary resetSummary = resetTargetData(exchange, request);
        if (resetSummary.getStatus() == ImportStatus.ERROR) {
          return resetSummary;
        }
      }

      DataValueSet dataValueSet =
          analyticsService.getAggregatedDataValueSet(
              toDataQueryParams(request, new SourceDataQueryParams()));

      return exchange.getTarget().getType() == TargetType.INTERNAL
          ? pushToInternal(exchange, dataValueSet)
          : pushToExternal(exchange, dataValueSet);
    } catch (HttpClientErrorException ex) {
      String message =
          format("Data import to target instance failed with status: '%s'", ex.getStatusCode());

      log.error(message, ex);

      return new ImportSummary(ImportStatus.ERROR, message);
    } catch (Exception ex) {
      return new ImportSummary(ImportStatus.ERROR, ex.getMessage());
    }
  }

  /**
   * Imports the given {@link DataValueSet} to this instance of DHIS 2.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @param dataValueSet the {@link DataValueSet}.
   * @return an {@link ImportSummary} describing the outcome of the exchange.
   */
  private ImportSummary pushToInternal(AggregateDataExchange exchange, DataValueSet dataValueSet) {
    return dataEntryPipeline.importInputGroups(
        List.of(toDataEntryGroup(dataValueSet)), toImportOptions(exchange), transitory());
  }

  private static DataEntryGroup.Input toDataEntryGroup(DataValueSet set) {
    return new DataEntryGroup.Input(
        null,
        set.getDataSet(),
        null,
        null,
        set.getOrgUnit(),
        set.getPeriod(),
        set.getAttributeOptionCombo(),
        null,
        set.getDataValues().stream().map(AggregateDataExchangeService::toDataEntryValue).toList());
  }

  private static DataEntryValue.Input toDataEntryValue(DataValue v) {
    return new DataEntryValue.Input(
        v.getDataElement(),
        v.getOrgUnit(),
        v.getCategoryOptionCombo(),
        null,
        v.getAttributeOptionCombo(),
        null,
        null,
        v.getPeriod(),
        v.getValue(),
        v.getComment(),
        v.getFollowup(),
        v.getDeleted());
  }

  /**
   * Exchanges the given {@link DataValueSet} to an external instance of DHIS 2. The location and
   * credentials of the target DHIS 2 instance and the import options to use for the data exchange
   * are specified by the target API of the given {@link AggregateDataExchange}.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @param dataValueSet the {@link DataValueSet}.
   * @return an {@link ImportSummary} describing the outcome of the exchange.
   */
  private ImportSummary pushToExternal(AggregateDataExchange exchange, DataValueSet dataValueSet) {
    return pushToExternal(exchange, dataValueSet, toImportOptions(exchange));
  }

  private ImportSummary pushToExternal(
      AggregateDataExchange exchange, DataValueSet dataValueSet, ImportOptions options) {
    return getDhis2Client(exchange).saveDataValueSet(dataValueSet, options);
  }

  /**
   * Performs a reset of target data as defined by the given exchange and request.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @param request the {@link SourceRequest}.
   * @return an {@link ImportSummary} describing the outcome of the exchange.
   */
  ImportSummary resetTargetData(AggregateDataExchange exchange, SourceRequest request) {
    DataValueSet resetSet = buildResetDataValueSet(exchange, request);

    if (resetSet.getDataValues().isEmpty()) {
      return new ImportSummary(
          ImportStatus.ERROR,
          "Reset payload is empty; no data values were generated for the given dx/pe/ou.");
    }

    ImportOptions resetOptions = toImportOptions(exchange);
    // Use NEW_AND_UPDATES so delete is driven by per-value deleted=true during upsert.
    resetOptions.setImportStrategy(ImportStrategy.NEW_AND_UPDATES);

    ImportSummary summary =
        exchange.getTarget().getType() == TargetType.INTERNAL
            ? dataEntryPipeline.importInputGroups(
                List.of(toDataEntryGroup(resetSet)), resetOptions, transitory())
            : pushToExternal(exchange, resetSet, resetOptions);

    if (summary.getStatus() == ImportStatus.SUCCESS) {
      summary.setDescription(
          format("Target data reset (payload values: %d).", resetSet.getDataValues().size()));
    }

    return summary;
  }

  /**
   * Builds a {@link DataValueSet} payload for resetting data based on the given exchange and
   * request.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @param request the {@link SourceRequest}.
   * @return a {@link DataValueSet} payload for resetting data.
   */
  DataValueSet buildResetDataValueSet(AggregateDataExchange exchange, SourceRequest request) {
    IdScheme inputIdScheme = toIdSchemeOrDefault(request.getInputIdScheme());

    // Note: request filters are intentionally ignored for reset. Filters only restrict analytics
    // output, not the exchange scope; reset should cover the full dx/pe/ou definition.
    DimensionalObject dxDimension =
        toDimensionalObject(DATA_X_DIM_ID, request.getDx(), inputIdScheme);
    DimensionalObject peDimension =
        toDimensionalObject(PERIOD_DIM_ID, request.getPe(), inputIdScheme);
    DimensionalObject ouDimension =
        toDimensionalObject(ORGUNIT_DIM_ID, request.getOu(), inputIdScheme);

    List<String> dxItems =
        dxDimension.getItems().stream().map(item -> item.getDimensionItem()).toList();
    List<String> peItems =
        peDimension.getItems().stream().map(item -> item.getDimensionItem()).toList();
    List<String> ouItems =
        ouDimension.getItems().stream().map(item -> item.getDimensionItem()).toList();

    long rowCount = (long) dxItems.size() * peItems.size() * ouItems.size();
    if (rowCount > RESET_MAX_VALUES) {
      throw new IllegalQueryException(
          format(
              "Reset payload too large (%d rows), exceeds limit of %d",
              rowCount, RESET_MAX_VALUES));
    }

    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader(DATA_X_DIM_ID, DATA_X_DIM_ID, ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID, PERIOD_DIM_ID, ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID, ORGUNIT_DIM_ID, ValueType.TEXT, false, true));
    grid.addHeader(
        new GridHeader(
            DataQueryParams.VALUE_ID, DataQueryParams.VALUE_ID, ValueType.TEXT, false, false));

    for (String dx : dxItems) {
      for (String pe : peItems) {
        for (String ou : ouItems) {
          grid.addRow().addValuesVar(dx, pe, ou, null);
        }
      }
    }

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(dxDimension)
            .addDimension(peDimension)
            .addDimension(ouDimension)
            .build();

    AnalyticsUtils.handleGridForDataValueSet(params, grid);

    String queryOutputIdScheme = request.getOutputIdScheme();
    IdScheme outputDataElementIdScheme =
        toIdScheme(request.getOutputDataElementIdScheme(), queryOutputIdScheme);
    IdScheme outputOrgUnitIdScheme =
        toIdScheme(request.getOutputOrgUnitIdScheme(), queryOutputIdScheme);
    IdScheme outputDataItemIdScheme =
        toIdScheme(request.getOutputDataItemIdScheme(), queryOutputIdScheme);
    IdScheme outputIdScheme = toIdScheme(queryOutputIdScheme, request.getOutputIdScheme());

    applyDimensionItemIdScheme(
        grid,
        DATA_X_DIM_ID,
        dxDimension,
        outputDataElementIdScheme,
        outputDataItemIdScheme,
        outputIdScheme);
    applyDimensionItemIdScheme(
        grid, ORGUNIT_DIM_ID, ouDimension, outputOrgUnitIdScheme, null, outputIdScheme);
    applyIdSchemeToGrid(grid, DATA_X_DIM_ID, DataElement.class, outputDataElementIdScheme);

    TargetRequest targetRequest =
        exchange.getTarget() != null ? exchange.getTarget().getRequest() : null;
    if (targetRequest != null) {
      IdScheme cocScheme =
          toIdScheme(targetRequest.getCategoryOptionComboIdScheme(), targetRequest.getIdScheme());
      if (cocScheme != null && !cocScheme.isNull()) {
        applyCocSchemeForNonIndicatorRows(
            grid, dxDimension, outputDataItemIdScheme, outputIdScheme, cocScheme);
      }
    }

    DataValueSet dataValueSet = AnalyticsUtils.getDataValueSet(params, grid);
    dataValueSet
        .getDataValues()
        .forEach(
            value -> {
              value.setValue(null);
              value.setStoredBy(null);
              value.setComment("Deleted by ADEX");
              value.setDeleted(true);
            });
    return dataValueSet;
  }

  /**
   * Applies the given ID scheme to the specified column of the given grid.
   *
   * @param grid the {@link Grid}.
   * @param column the column name.
   * @param type the type of {@link IdentifiableObject}.
   * @param idScheme the {@link IdScheme}.
   */
  private <T extends IdentifiableObject> void applyIdSchemeToGrid(
      Grid grid, String column, Class<T> type, IdScheme idScheme) {
    if (idScheme == null || idScheme.isNull()) {
      return;
    }

    int index = grid.getIndexOfHeader(column);
    if (index == -1) {
      return;
    }

    Map<String, String> cache = new HashMap<>();

    List<List<Object>> rows = grid.getRows();
    for (List<Object> row : rows) {
      Object value = row.get(index);
      if (value == null) {
        continue;
      }

      String uid = String.valueOf(value);
      String mapped = cache.get(uid);
      if (mapped == null) {
        T object = idObjectManager.get(type, uid);
        if (object != null) {
          mapped = object.getPropertyValue(idScheme);
        }
        if (mapped == null) {
          mapped = uid;
        }
        cache.put(uid, mapped);
      }

      row.set(index, mapped);
    }
  }

  /**
   * Applies the given dimension item ID schemes to the specified column of the given grid.
   *
   * @param grid the {@link Grid}.
   * @param column the column name.
   * @param dimension the {@link DimensionalObject}.
   * @param dataElementScheme the {@link IdScheme} for data elements.
   * @param dataItemScheme the {@link IdScheme} for data items.
   * @param generalScheme the general {@link IdScheme}.
   */
  private void applyDimensionItemIdScheme(
      Grid grid,
      String column,
      DimensionalObject dimension,
      IdScheme dataElementScheme,
      IdScheme dataItemScheme,
      IdScheme generalScheme) {

    if (allSchemesEmpty(dataElementScheme, dataItemScheme, generalScheme)) {
      return;
    }

    int index = grid.getIndexOfHeader(column);
    if (index == -1) {
      return;
    }

    Map<String, String> map = new HashMap<>();
    dimension
        .getItems()
        .forEach(
            item -> {
              IdScheme schemeToUse = generalScheme;
              if (item instanceof DataElement
                  && dataElementScheme != null
                  && !dataElementScheme.isNull()) {
                schemeToUse = dataElementScheme;
              } else if (dataItemScheme != null && !dataItemScheme.isNull()) {
                schemeToUse = dataItemScheme;
              }

              String mapped =
                  schemeToUse != null && !schemeToUse.isNull()
                      ? item.getDimensionItem(schemeToUse)
                      : item.getDimensionItem();
              map.put(item.getDimensionItem(), mapped);
            });

    List<List<Object>> rows = grid.getRows();
    for (List<Object> row : rows) {
      Object value = row.get(index);
      if (value == null) {
        continue;
      }
      String mapped = map.get(String.valueOf(value));
      if (mapped != null) {
        row.set(index, mapped);
      }
    }
  }

  private boolean allSchemesEmpty(IdScheme... schemes) {
    for (IdScheme scheme : schemes) {
      if (scheme != null && !scheme.isNull()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Applies the given category option combo ID scheme to non-indicator rows of the given grid. This
   * is for situations where indicators use specific category option combos for aggregation, and
   * these should not be altered as they are defined on the indicator itself. However, for data
   * elements with category option combos, the given scheme should be applied.
   *
   * @param grid the {@link Grid}.
   * @param dxDimension the data X {@link DimensionalObject}.
   * @param dataItemScheme the {@link IdScheme} for data items.
   * @param generalScheme the general {@link IdScheme}.
   * @param cocScheme the {@link IdScheme} for category option combos.
   */
  private void applyCocSchemeForNonIndicatorRows(
      Grid grid,
      DimensionalObject dxDimension,
      IdScheme dataItemScheme,
      IdScheme generalScheme,
      IdScheme cocScheme) {
    int dxIndex = grid.getIndexOfHeader(DATA_X_DIM_ID);
    int cocIndex = grid.getIndexOfHeader(CATEGORYOPTIONCOMBO_DIM_ID);
    if (dxIndex == -1 || cocIndex == -1) {
      return;
    }

    Set<String> indicatorIds =
        getIndicatorDimensionItems(dxDimension, dataItemScheme, generalScheme);
    if (indicatorIds.isEmpty()) {
      applyIdSchemeToGrid(grid, CATEGORYOPTIONCOMBO_DIM_ID, CategoryOptionCombo.class, cocScheme);
      return;
    }

    Map<String, String> cache = new HashMap<>();
    for (List<Object> row : grid.getRows()) {
      mapCocForNonIndicatorRow(row, dxIndex, cocIndex, indicatorIds, cocScheme, cache);
    }
  }

  /**
   * Retrieves the set of dimension item IDs from the given dimension that are indicators or program
   * indicators, using the specified ID schemes. This is used to identify which dimension items
   * should not have their category option combos remapped during reset operations.
   *
   * @param dimension the {@link DimensionalObject}.
   * @param dataItemScheme the {@link IdScheme} for data items.
   * @param generalScheme the general {@link IdScheme}.
   * @return a set of dimension item IDs that are indicators or program indicators.
   */
  private Set<String> getIndicatorDimensionItems(
      DimensionalObject dimension, IdScheme dataItemScheme, IdScheme generalScheme) {
    Set<String> ids = new HashSet<>();
    for (DimensionalItemObject item : dimension.getItems()) {
      if (!(item instanceof Indicator || item instanceof ProgramIndicator)) {
        continue;
      }
      IdScheme schemeToUse =
          dataItemScheme != null && !dataItemScheme.isNull() ? dataItemScheme : generalScheme;
      if (schemeToUse != null && !schemeToUse.isNull()) {
        ids.add(item.getDimensionItem(schemeToUse));
      }
      ids.add(item.getDimensionItem());
    }
    return ids;
  }

  private void mapCocForNonIndicatorRow(
      List<Object> row,
      int dxIndex,
      int cocIndex,
      Set<String> indicatorIds,
      IdScheme cocScheme,
      Map<String, String> cache) {
    Object dxValue = row.get(dxIndex);
    Object cocValue = row.get(cocIndex);
    boolean indicatorRow =
        dxValue != null && indicatorIds.contains(String.valueOf(dxValue));
    if (indicatorRow || cocValue == null) {
      return;
    }

    String uid = String.valueOf(cocValue);
    String mapped = cache.get(uid);
    if (mapped == null) {
      CategoryOptionCombo coc = idObjectManager.get(CategoryOptionCombo.class, uid);
      if (coc != null) {
        mapped = coc.getPropertyValue(cocScheme);
      }
      if (mapped == null) {
        mapped = uid;
      }
      cache.put(uid, mapped);
    }

    row.set(cocIndex, mapped);
  }

  /**
   * Converts the {@link TargetRequest} of the given {@link AggregateDataExchange} to an {@link
   * ImportOptions}.
   *
   * <p>Note that the data value set service does not using {@code null} values as specific ID
   * schemes. When it does, this method can be simplified.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @return an {@link ImportOptions}.
   */
  ImportOptions toImportOptions(AggregateDataExchange exchange) {
    TargetRequest request = exchange.getTarget().getRequest();

    ImportOptions options = new ImportOptions();

    if (isNotBlank(request.getDataElementIdScheme())) {
      options.setDataElementIdScheme(request.getDataElementIdScheme());
    }

    if (isNotBlank(request.getOrgUnitIdScheme())) {
      options.setOrgUnitIdScheme(request.getOrgUnitIdScheme());
    }

    if (isNotBlank(request.getCategoryOptionComboIdScheme())) {
      options.setCategoryOptionComboIdScheme(request.getCategoryOptionComboIdScheme());
    }

    if (isNotBlank(request.getIdScheme())) {
      options.setIdScheme(request.getIdScheme());
    }

    if (notNull(request.getImportStrategy())) {
      options.setImportStrategy(request.getImportStrategy());
    }

    options.setSkipAudit(request.isSkipAuditOrDefault());

    if (notNull(request.getDryRun())) {
      options.setDryRun(request.getDryRun());
    }

    return options;
  }

  /**
   * Retrieves and creates a {@link DataQueryParams} based on the given {@link SourceRequest}.
   *
   * @param request the {@link SourceRequest}.
   * @param params the {@link SourceDataQueryParams}.
   * @return the {@link DataQueryParams}.
   */
  DataQueryParams toDataQueryParams(SourceRequest request, SourceDataQueryParams params) {
    String queryOutputIdScheme = params.getOutputIdScheme();

    IdScheme inputIdScheme = toIdSchemeOrDefault(request.getInputIdScheme());

    IdScheme outputDataElementIdScheme =
        toIdScheme(queryOutputIdScheme, request.getOutputDataElementIdScheme());
    IdScheme outputOrgUnitIdScheme =
        toIdScheme(queryOutputIdScheme, request.getOutputOrgUnitIdScheme());
    IdScheme outputDataItemIdScheme =
        toIdScheme(queryOutputIdScheme, request.getOutputDataItemIdScheme());
    IdScheme outputIdScheme = toIdScheme(queryOutputIdScheme, request.getOutputIdScheme());

    List<DimensionalObject> filters =
        mapToList(request.getFilters(), f -> toDimensionalObject(f, inputIdScheme));

    return DataQueryParams.newBuilder()
        .addDimension(toDimensionalObject(DATA_X_DIM_ID, request.getDx(), inputIdScheme))
        .addDimension(toDimensionalObject(PERIOD_DIM_ID, request.getPe(), inputIdScheme))
        .addDimension(toDimensionalObject(ORGUNIT_DIM_ID, request.getOu(), inputIdScheme))
        .addFilters(filters)
        .withAggregationType(toAnalyticsAggregationType(request.getAggregationType()))
        .withOutputDataElementIdScheme(outputDataElementIdScheme)
        .withOutputOrgUnitIdScheme(outputOrgUnitIdScheme)
        .withOutputDataItemIdScheme(outputDataItemIdScheme)
        .withOutputIdScheme(outputIdScheme)
        .build();
  }

  /**
   * Creates a dimensional object based on the given dimension, items and input ID scheme.
   *
   * @param dimension the dimension.
   * @param items the list of dimension items.
   * @param inputIdScheme the {@link IdScheme}.
   * @return a {@link DimensionalObject}.
   */
  private DimensionalObject toDimensionalObject(
      String dimension, List<String> items, IdScheme inputIdScheme) {
    return dataQueryService.getDimension(
        dimension, items, new Date(), null, false, null, inputIdScheme);
  }

  /**
   * Creates a dimensional object based on the given filter and ID scheme.
   *
   * @param filter the {@link Filter}.
   * @param inputIdScheme the {@link IdScheme}.
   * @return a {@link DimensionalObject}.
   */
  private DimensionalObject toDimensionalObject(Filter filter, IdScheme inputIdScheme) {
    return dataQueryService.getDimension(
        filter.getDimension(), filter.getItems(), new Date(), null, false, null, inputIdScheme);
  }

  /**
   * Returns the {@link AnalyticsAggregationType} based on the given {@link AggregationType}.
   *
   * @param aggregationType the {@link AggregationType}.
   * @return the {@link AnalyticsAggregationType}.
   */
  AnalyticsAggregationType toAnalyticsAggregationType(AggregationType aggregationType) {
    return aggregationType != null
        ? AnalyticsAggregationType.fromAggregationType(aggregationType)
        : null;
  }

  /**
   * Returns the {@link IdScheme} based on the given ID scheme strings. The first non-null value
   * will be used. Returns null if all of the given ID scheme strings are null.
   *
   * @param idSchemes the ID scheme strings.
   * @return the given ID scheme, or null.
   */
  IdScheme toIdScheme(String... idSchemes) {
    String idScheme = ObjectUtils.firstNonNull(idSchemes);
    return idScheme != null ? IdScheme.from(idScheme) : null;
  }

  /**
   * Returns the {@link IdScheme} based on the given ID scheme string, or the default ID scheme if
   * the given ID scheme string is null.
   *
   * @param idScheme the ID scheme string.
   * @return the given ID scheme, or the default ID scheme if null.
   */
  IdScheme toIdSchemeOrDefault(String idScheme) {
    return idScheme != null ? IdScheme.from(idScheme) : IdScheme.UID;
  }

  /**
   * Returns a {@link Dhis2Client} based on the given {@link AggregateDataExchange}.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @return a {@link Dhis2Client}.
   * @throws IllegalStateException if authentication is not specified.
   */
  Dhis2Client getDhis2Client(AggregateDataExchange exchange) {
    Api api = exchange.getTarget().getApi();

    if (api.isAccessTokenAuth()) {
      return Dhis2Client.withAccessTokenAuth(api.getUrl(), decryptAccessToken(exchange));
    } else if (api.isBasicAuth()) {
      return Dhis2Client.withBasicAuth(api.getUrl(), api.getUsername(), decryptPassword(exchange));
    }

    throw new IllegalStateException("DHIS 2 client authentication not specified");
  }

  /**
   * Returns the decrypted access token of the given exchange target API. Note that the access token
   * is assumed to be encrypted if the exchange is persisted, and plain text if the exchange is
   * transient and thus not decrypted.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @return the decrypted access token.
   */
  private String decryptAccessToken(AggregateDataExchange exchange) {
    String accessToken = exchange.getTarget().getApi().getAccessToken();
    return isPersisted(exchange) ? encryptor.decrypt(accessToken) : accessToken;
  }

  /**
   * Returns the decrypted password of the given exchange target API. Note that the password is
   * assumed to be encrypted if the exchange is persisted, and plain text if the exchange is
   * transient and thus not decrypted.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @return the decrypted password.
   */
  private String decryptPassword(AggregateDataExchange exchange) {
    String password = exchange.getTarget().getApi().getPassword();
    return isPersisted(exchange) ? encryptor.decrypt(password) : password;
  }

  /**
   * Returns a stage description.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @return a stage description.
   */
  private static String toStageDescription(AggregateDataExchange exchange) {
    Target target = exchange.getTarget();
    Api api = target.getApi();
    return format(
        "exchange aggregate data %s to %s target %s",
        exchange.getName(), target.getType().name(), api == null ? "(local)" : api.getUrl());
  }

  /**
   * Returns an item description.
   *
   * @param request the {@link SourceRequest}.
   * @return an item description.
   */
  private static String toItemDescription(SourceRequest request) {
    return format(
        "dx: %s; pe: %s; ou: %s",
        join(",", request.getDx()), join(",", request.getPe()), join(",", request.getOu()));
  }

  /**
   * Returns an item summary.
   *
   * @param summary the {@link ImportSummary}.
   * @return an item summary.
   */
  private static String toItemSummary(ImportSummary summary) {
    return summary.getStatus() == ImportStatus.ERROR && summary.getConflictCount() == 0
        ? summary.getDescription()
        : summary.toCountString();
  }

  /**
   * Returns a stage summary.
   *
   * @param success the number of successful operations.
   * @param failed the number of failed operations.
   * @param exchange the {@link AggregateDataExchange}.
   * @return a stage summary.
   */
  private static String toStageSummary(int success, int failed, AggregateDataExchange exchange) {
    return format(
        "Aggregate data exchange completed (%d/%d): '%s', type: '%s'",
        success, (success + failed), exchange.getUid(), exchange.getTarget().getType());
  }

  /**
   * Indicates whether the given {@link AggregateDataExchange} is persisted.
   *
   * @param exchange the {@link AggregateDataExchange}.
   * @return true if persisted.
   */
  boolean isPersisted(AggregateDataExchange exchange) {
    return exchange != null && exchange.getId() > 0;
  }
}
