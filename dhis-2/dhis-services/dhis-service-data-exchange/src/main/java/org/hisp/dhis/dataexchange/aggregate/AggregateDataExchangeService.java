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
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.collection.CollectionUtils.mapToList;
import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;
import static org.hisp.dhis.util.ObjectUtils.notNull;

import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataexchange.client.Dhis2Client;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
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

  private final DataValueSetService dataValueSetService;

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
    return dataValueSetService.importDataValueSet(dataValueSet, toImportOptions(exchange));
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
    return getDhis2Client(exchange).saveDataValueSet(dataValueSet, toImportOptions(exchange));
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
