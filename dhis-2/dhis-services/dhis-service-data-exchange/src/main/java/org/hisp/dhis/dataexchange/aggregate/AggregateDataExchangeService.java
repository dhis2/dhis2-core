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
import static org.hisp.dhis.common.DimensionConstants.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.collection.CollectionUtils.mapToList;
import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;
import static org.hisp.dhis.scheduling.RecordingJobProgress.transitory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataexchange.client.Dhis2Client;
import org.hisp.dhis.dataexchange.client.response.InternalImportSummaryResponse;
import org.hisp.dhis.datavalue.DataEntryGroup;
import org.hisp.dhis.datavalue.DataEntryPipeline;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.datavalue.DataExportGroup;
import org.hisp.dhis.datavalue.DataExportPipeline;
import org.hisp.dhis.datavalue.DataExportValue;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobProgress.FailurePolicy;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.UserDetails;
import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Main service class for aggregate data exchange.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregateDataExchangeService {
  private final AnalyticsService analyticsService;

  private final AggregateDataExchangeStore aggregateDataExchangeStore;

  private final DataQueryService dataQueryService;

  private final DataEntryPipeline dataEntryPipeline;
  private final DataExportPipeline dataExportPipeline;

  private final AclService aclService;

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
      Grid dataValues =
          analyticsService.getAggregatedDataValuesGrid(
              toDataQueryParams(request, new SourceDataQueryParams()));

      return exchange.getTarget().getType() == TargetType.INTERNAL
          ? pushToInternal(exchange, dataValues)
          : pushToExternal(exchange, dataValues);
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
  private ImportSummary pushToInternal(AggregateDataExchange exchange, Grid dataValues) {
    TargetRequest request = exchange.getTarget().getRequest();
    DataEntryGroup.Input group = toDataEntryGroup(dataValues);
    group = group.withIds(request.getEntryIds());
    // TODO set scope

    DataEntryGroup.Options options =
        new DataEntryGroup.Options(Boolean.TRUE.equals(request.getDryRun()), false, false);
    return dataEntryPipeline.importInputGroups(List.of(group), options, false, transitory());
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
  private ImportSummary pushToExternal(AggregateDataExchange exchange, Grid dataValues) {
    DataExportGroup.Output group = toDataExportGroup(dataValues);
    TargetRequest request = exchange.getTarget().getRequest();
    DataExportGroup.Ids ids = request.getExportIds();
    group = group.withIds(ids);
    // TODO set scope

    Dhis2Client client = getDhis2Client(exchange);

    UriComponentsBuilder uri = client.getResolvedUriBuilder("dataValueSets");
    if (ids.dataElements().isNotUID()) uri.queryParam("dataElementIdScheme", ids.dataElements());
    if (ids.orgUnits().isNotUID()) uri.queryParam("orgUnitIdScheme", ids.orgUnits());
    if (ids.categoryOptionCombos().isNotUID())
      uri.queryParam("categoryOptionComboIdScheme", ids.categoryOptionCombos());
    if (ids.attributeOptionCombos().isNotUID())
      uri.queryParam("attributeOptionComboIdScheme", ids.attributeOptionCombos());

    uri.queryParam("importStrategy", request.getImportStrategy());
    uri.queryParam("skipAudit", request.isSkipAuditOrDefault());
    uri.queryParam("dryRun", Boolean.TRUE.equals(request.getDryRun()));

    ByteArrayOutputStream json = new ByteArrayOutputStream();
    dataExportPipeline.exportAsJson(group, json);
    return client
        .executeJsonPostRequest(
            uri.build().toUri(), json.toString(), InternalImportSummaryResponse.class)
        .getBody()
        .getImportSummary();
  }

  /**
   * Retrieves and creates a {@link DataQueryParams} based on the given {@link SourceRequest}.
   *
   * @param request the {@link SourceRequest}.
   * @param params the {@link SourceDataQueryParams}.
   * @return the {@link DataQueryParams}.
   */
  DataQueryParams toDataQueryParams(SourceRequest request, SourceDataQueryParams params) {
    IdScheme inputIdScheme = IdScheme.of(request.getInputIdScheme());
    if (inputIdScheme == null) inputIdScheme = IdScheme.UID;

    IdScheme outputIdScheme = IdScheme.of(request.getOutputIdScheme());
    if (outputIdScheme == null) outputIdScheme = IdScheme.UID;
    IdScheme outputDataElementIdScheme = IdScheme.of(request.getOutputDataElementIdScheme());
    if (outputDataElementIdScheme == null) outputDataElementIdScheme = outputIdScheme;
    IdScheme outputOrgUnitIdScheme = IdScheme.of(request.getOutputOrgUnitIdScheme());
    if (outputOrgUnitIdScheme == null) outputOrgUnitIdScheme = outputIdScheme;
    IdScheme outputDataItemIdScheme = IdScheme.of(request.getOutputDataItemIdScheme());
    if (outputDataItemIdScheme == null) outputDataItemIdScheme = outputIdScheme;
    if (params.getOutputIdScheme() != null) {
      outputIdScheme = IdScheme.of(params.getOutputIdScheme());
      outputDataElementIdScheme = outputIdScheme;
      outputOrgUnitIdScheme = outputIdScheme;
      outputDataItemIdScheme = outputIdScheme;
    }

    IdScheme iis = inputIdScheme;
    List<DimensionalObject> filters =
        request.getFilters().stream().map(f -> toDimensionalObject(f, iis)).toList();

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

  public static DataEntryGroup.Input toDataEntryGroup(Grid grid) {

    int dxInx = grid.getIndexOfHeader(DATA_X_DIM_ID);
    int peInx = grid.getIndexOfHeader(PERIOD_DIM_ID);
    int ouInx = grid.getIndexOfHeader(ORGUNIT_DIM_ID);
    int coInx = grid.getIndexOfHeader(CATEGORYOPTIONCOMBO_DIM_ID);
    int aoInx = grid.getIndexOfHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID);
    int vlInx = grid.getHeaderWidth() - 1;

    List<List<Object>> rows = grid.getRows();
    List<DataEntryValue.Input> values = new ArrayList<>(rows.size());
    for (List<Object> row : rows) {
      Object coc = row.get(coInx);
      Object aoc = row.get(aoInx);

      DataEntryValue.Input dv =
          new DataEntryValue.Input(
              String.valueOf(row.get(dxInx)),
              String.valueOf(row.get(ouInx)),
              coc != null ? String.valueOf(coc) : null,
              aoc != null ? String.valueOf(aoc) : null,
              String.valueOf(row.get(peInx)),
              String.valueOf(row.get(vlInx)),
              "[aggregated]");

      values.add(dv);
    }

    return new DataEntryGroup.Input(values);
  }

  public static DataExportGroup.Output toDataExportGroup(Grid grid) {

    int dxInx = grid.getIndexOfHeader(DATA_X_DIM_ID);
    int peInx = grid.getIndexOfHeader(PERIOD_DIM_ID);
    int ouInx = grid.getIndexOfHeader(ORGUNIT_DIM_ID);
    int coInx = grid.getIndexOfHeader(CATEGORYOPTIONCOMBO_DIM_ID);
    int aoInx = grid.getIndexOfHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID);
    int vlInx = grid.getHeaderWidth() - 1;

    // by using streams we make sure to directly write to JSON later
    // without materializing the entire list in memory at once
    Stream<DataExportValue.Output> values =
        grid.getRows().stream()
            .map(
                row -> {
                  Object coc = row.get(coInx);
                  Object aoc = row.get(aoInx);

                  return new DataExportValue.Output(
                      String.valueOf(row.get(dxInx)),
                      String.valueOf(row.get(ouInx)),
                      coc != null ? String.valueOf(coc) : null,
                      aoc != null ? String.valueOf(aoc) : null,
                      String.valueOf(row.get(peInx)),
                      // unknown, but JSON serialisation does not care so anything is fine
                      ValueType.INTEGER,
                      String.valueOf(row.get(vlInx)),
                      "[aggregated]");
                });

    DataExportGroup.Ids ids = new DataExportGroup.Ids();
    return new DataExportGroup.Output(ids, null, null, null, null, null, null, values);
  }
}
