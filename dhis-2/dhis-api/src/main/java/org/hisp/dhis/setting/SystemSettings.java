/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.setting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import javax.annotation.Nonnull;
import org.hisp.dhis.analytics.AnalyticsCacheTtlMode;
import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.common.DigitGroupSeparator;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.common.cache.Cacheability;
import org.hisp.dhis.fileresource.FileResourceRetentionStrategy;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.security.LoginPageLayout;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.translation.Translatable;

/**
 * A complete set of system settings.
 *
 * <p>{@link SystemSettings} are immutable value objects. An interface is used to decouple any using
 * code from the implementation and to allow mocking.
 *
 * <p>General principles for all settings: 1. settings are key-value pairs with simple values
 * (strings, booleans, numbers) 2. settings are never null/undefined (define a default in case they
 * are not set) 3. settings are an open set (no predefined set of names) 4. confidential setting
 * values are write only from web API perspective (except for ALL admin)
 *
 * @author Jan Bernitt
 */
@OpenApi.Ignore
public non-sealed interface SystemSettings extends Settings {

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Confidential {}

  @Nonnull
  static SystemSettings of(@Nonnull Map<String, String> settings) {
    return LazySettings.of(SystemSettings.class, settings);
  }

  /**
   * Creates settings from raw key-value pairs.
   *
   * @param settings raw values, potentially encoded
   * @param decoder applied to all {@link #isConfidential(String)} values
   * @return a new instance of settings
   */
  @Nonnull
  static SystemSettings of(
      @Nonnull Map<String, String> settings, @Nonnull BinaryOperator<String> decoder) {
    return LazySettings.of(SystemSettings.class, settings, decoder);
  }

  /**
   * @return a set of all key names that have defaults as defined by accessor methods in {@link
   *     SystemSettings}
   */
  @Nonnull
  static Set<String> keysWithDefaults() {
    return LazySettings.keysWithDefaults(SystemSettings.class);
  }

  /**
   * @param key a settings name
   * @return true if the setting is private and its value should not be exposed in APIs unless a
   *     user has the proper authority
   */
  static boolean isConfidential(@Nonnull String key) {
    return LazySettings.isConfidential(key);
  }

  /**
   * @param key a setting name
   * @return true, if it can have a translation, false otherwise
   */
  static boolean isTranslatable(@Nonnull String key) {
    return LazySettings.isTranslatable(key);
  }

  /* Settings used in core - all names have to start with "get" */

  default Locale getUiLocale() {
    return asLocale("keyUiLocale", LocaleManager.DEFAULT_LOCALE);
  }

  default Locale getDbLocale() {
    return asLocale("keyDbLocale", LocaleManager.DEFAULT_LOCALE);
  }

  default DisplayProperty getAnalysisDisplayProperty() {
    return asEnum("keyAnalysisDisplayProperty", DisplayProperty.NAME);
  }

  default DigitGroupSeparator getAnalysisDigitGroupSeparator() {
    return asEnum("keyAnalysisDigitGroupSeparator", DigitGroupSeparator.SPACE);
  }

  default String getCurrentDomainType() {
    return asString("keyCurrentDomainType", "");
  }

  default String getTrackerDashboardLayout() {
    return asString("keyTrackerDashboardLayout", "");
  }

  @Translatable
  default String getApplicationTitle() {
    return asString("applicationTitle", "DHIS 2");
  }

  @Translatable
  default String getApplicationIntro() {
    return asString("keyApplicationIntro", "");
  }

  @Translatable
  default String getApplicationNotification() {
    return asString("keyApplicationNotification", "");
  }

  @Translatable
  default String getApplicationFooter() {
    return asString("keyApplicationFooter", "");
  }

  @Translatable
  default String getApplicationRightFooter() {
    return asString("keyApplicationRightFooter", "");
  }

  default String getFlag() {
    return asString("keyFlag", "dhis2");
  }

  default String getFlagImage() {
    return asString("keyFlagImage", "");
  }

  default String getStartModule() {
    return asString("startModule", "dhis-web-dashboard");
  }

  default boolean getStartModuleEnableLightweight() {
    return asBoolean("startModuleEnableLightweight", false);
  }

  default double getFactorOfDeviation() {
    return asDouble("factorDeviation", 2d);
  }

  default String getEmailHostName() {
    return asString("keyEmailHostName", "");
  }

  default int getEmailPort() {
    return asInt("keyEmailPort", 587);
  }

  default String getEmailUsername() {
    return asString("keyEmailUsername", "");
  }

  default boolean getEmailTls() {
    return asBoolean("keyEmailTls", true);
  }

  default String getEmailSender() {
    return asString("keyEmailSender", "");
  }

  @Confidential
  default String getEmailPassword() {
    return asString("keyEmailPassword", "");
  }

  default int getMinPasswordLength() {
    return asInt("minPasswordLength", 8);
  }

  default int getMaxPasswordLength() {
    return asInt("maxPasswordLength", 72);
  }

  default int getSmsMaxLength() {
    return asInt("keySmsMaxLength", 1071);
  }

  default CacheStrategy getCacheStrategy() {
    return asEnum("keyCacheStrategy", CacheStrategy.CACHE_1_MINUTE);
  }

  default Cacheability getCacheability() {
    return asEnum("keyCacheability", Cacheability.PUBLIC);
  }

  default AnalyticsFinancialYearStartKey getAnalyticsFinancialYearStart() {
    return asEnum(
        "analyticsFinancialYearStart", AnalyticsFinancialYearStartKey.FINANCIAL_YEAR_OCTOBER);
  }

  default String getPhoneNumberAreaCode() {
    return asString("phoneNumberAreaCode", "");
  }

  default boolean getAccountRecoveryEnabled() {
    return asBoolean("keyAccountRecovery", false);
  }

  default boolean getLockMultipleFailedLogins() {
    return asBoolean("keyLockMultipleFailedLogins", false);
  }

  default String getGoogleAnalyticsUA() {
    return asString("googleAnalyticsUA", "");
  }

  default int getCredentialsExpires() {
    return asInt("credentialsExpires", 0);
  }

  default boolean getCredentialsExpiryAlert() {
    return asBoolean("credentialsExpiryAlert", false);
  }

  default int getCredentialsExpiresReminderInDays() {
    return asInt("credentialsExpiresReminderInDays", 28);
  }

  default int getAccountExpiresInDays() {
    return asInt("accountExpiresInDays", 7);
  }

  default boolean getAccountExpiryAlert() {
    return asBoolean("accountExpiryAlert", false);
  }

  default boolean getSelfRegistrationNoRecaptcha() {
    return asBoolean("keySelfRegistrationNoRecaptcha", false);
  }

  @Confidential
  default String getRecaptchaSecret() {
    return asString("recaptchaSecret", "");
  }

  @Confidential
  default String getRecaptchaSite() {
    return asString("recaptchaSite", "6LcVwT0UAAAAAAkO_EGPiYOiymIszZUeHfqWIYX5");
  }

  default boolean getCanGrantOwnUserRoles() {
    return asBoolean("keyCanGrantOwnUserAuthorityGroups", false);
  }

  default int getIgnoreAnalyticsApprovalYearThreshold() {
    return asInt("keyIgnoreAnalyticsApprovalYearThreshold", -1);
  }

  default int getAnalyticsMaxLimit() {
    return asInt("keyAnalyticsMaxLimit", 100000);
  }

  default int getDataQualityMaxLimit() {
    return asInt("keyDataQualityMaxLimit", 500);
  }

  default boolean getIncludeZeroValuesInAnalytics() {
    return asBoolean("keyIncludeZeroValuesInAnalytics", false);
  }

  default boolean getEmbeddedDashboardsEnabled() {
    return asBoolean("keyEmbeddedDashboardsEnabled", false);
  }

  default int getSqlViewMaxLimit() {
    return asInt("keySqlViewMaxLimit", -1);
  }

  default boolean getRespectMetaDataStartEndDatesInAnalyticsTableExport() {
    return asBoolean("keyRespectMetaDataStartEndDatesInAnalyticsTableExport", false);
  }

  default boolean getSkipDataTypeValidationInAnalyticsTableExport() {
    return asBoolean("keySkipDataTypeValidationInAnalyticsTableExport", false);
  }

  default int getParallelJobsInAnalyticsTableExport() {
    return asInt("keyParallelJobsInAnalyticsTableExport", -1);
  }

  default boolean getCustomLoginPageLogo() {
    return asBoolean("keyCustomLoginPageLogo", false);
  }

  default boolean getCustomTopMenuLogo() {
    return asBoolean("keyCustomTopMenuLogo", false);
  }

  default int getDatabaseServerCpus() {
    return asInt("keyDatabaseServerCpus", 0);
  }

  default String getLastSuccessfulAnalyticsTablesRuntime() {
    return asString("keyLastSuccessfulAnalyticsTablesRuntime", "");
  }

  default String getLastSuccessfulLatestAnalyticsPartitionRuntime() {
    return asString("keyLastSuccessfulLatestAnalyticsPartitionRuntime", "");
  }

  default Date getLastMonitoringRun() {
    return asDate("keyLastMonitoringRun", new Date(0L));
  }

  default Date getLastSuccessfulDataSynch() {
    return asDate("keyLastSuccessfulDataSynch", new Date(0L));
  }

  default Date getLastSuccessfulEventsDataSynch() {
    return asDate("keyLastSuccessfulEventsDataSynch", new Date(0L));
  }

  default Date getLastCompleteDataSetRegistrationSyncSuccess() {
    return asDate("keyLastCompleteDataSetRegistrationSyncSuccess", new Date(0L));
  }

  default Date getSyncSkipSyncForDataChangedBefore() {
    return asDate("syncSkipSyncForDataChangedBefore", new Date(0L));
  }

  default Date getLastSuccessfulAnalyticsTablesUpdate() {
    return asDate("keyLastSuccessfulAnalyticsTablesUpdate", new Date(0L));
  }

  default Date getLastSuccessfulLatestAnalyticsPartitionUpdate() {
    return asDate("keyLastSuccessfulLatestAnalyticsPartitionUpdate", new Date(0L));
  }

  default Date getLastSuccessfulResourceTablesUpdate() {
    return asDate("keyLastSuccessfulResourceTablesUpdate", new Date(0L));
  }

  default Date getLastSuccessfulSystemMonitoringPush() {
    return asDate("keyLastSuccessfulSystemMonitoringPush", new Date(0L));
  }

  default Date getLastSuccessfulMonitoring() {
    return asDate("keyLastSuccessfulMonitoring", new Date(0L));
  }

  default Date getNextAnalyticsTableUpdate() {
    return asDate("keyNextAnalyticsTableUpdate", new Date(0L));
  }

  default String getHelpPageLink() {
    return asString(
        "helpPageLink",
        "https://dhis2.github.io/dhis2-docs/master/en/user/html/dhis2_user_manual_en.html");
  }

  default boolean getAcceptanceRequiredForApproval() {
    return asBoolean("keyAcceptanceRequiredForApproval", false);
  }

  default String getSystemNotificationsEmail() {
    return asString("keySystemNotificationsEmail", "");
  }

  default RelativePeriodEnum getAnalysisRelativePeriod() {
    return asEnum("keyAnalysisRelativePeriod", RelativePeriodEnum.LAST_12_MONTHS);
  }

  default boolean getRequireAddToView() {
    return asBoolean("keyRequireAddToView", false);
  }

  default boolean getAllowObjectAssignment() {
    return asBoolean("keyAllowObjectAssignment", false);
  }

  default boolean getUseCustomLogoFront() {
    return asBoolean("keyUseCustomLogoFront", false);
  }

  default boolean getUseCustomLogoBanner() {
    return asBoolean("keyUseCustomLogoBanner", false);
  }

  default String getMetaDataRepoUrl() {
    return asString(
        "keyMetaDataRepoUrl",
        "https://raw.githubusercontent.com/dhis2/dhis2-metadata-repo/master/repo/221/index.json");
  }

  default boolean getDataImportStrictPeriods() {
    return asBoolean("keyDataImportStrictPeriods", false);
  }

  /**
   * @since 2.43
   * @return true, when input groups without DS should be split into groups by DS
   */
  default boolean getDataEntryAutoGroup() {
    return asBoolean("dataEntryAutoGroup", false);
  }

  default boolean getDataImportStrictOrganisationUnits() {
    return asBoolean("keyDataImportStrictOrganisationUnits", false);
  }

  default boolean getDataImportStrictAttributeOptionCombos() {
    return asBoolean("keyDataImportStrictAttributeOptionCombos", false);
  }

  default boolean getDataImportRequireAttributeOptionCombo() {
    return asBoolean("keyDataImportRequireAttributeOptionCombo", false);
  }

  default String getCustomJs() {
    return asString("keyCustomJs", "");
  }

  default String getCustomCss() {
    return asString("keyCustomCss", "");
  }

  default String getCalendar() {
    return asString("keyCalendar", "iso8601");
  }

  default String getDateFormat() {
    return asString("keyDateFormat", "yyyy-MM-dd");
  }

  default String getStyle() {
    return asString("keyStyle", "light_blue/light_blue.css");
  }

  default String getRemoteInstanceUrl() {
    return asString("keyRemoteInstanceUrl", "");
  }

  default String getRemoteInstanceUsername() {
    return asString("keyRemoteInstanceUsername", "");
  }

  @Confidential
  default String getRemoteInstancePassword() {
    return asString("keyRemoteInstancePassword", "");
  }

  default String getGoogleMapsApiKey() {
    return asString("keyGoogleMapsApiKey", "");
  }

  default String getBingMapsApiKey() {
    return asString("keyBingMapsApiKey", "");
  }

  default String getAzureMapsApiKey() {
    return asString("keyAzureMapsApiKey", "");
  }

  default Date getLastMetaDataSyncSuccess() {
    return asDate("keyLastMetaDataSyncSuccess", new Date(0));
  }

  default boolean getVersionEnabled() {
    return asBoolean("keyVersionEnabled", false);
  }

  default String getMetadataFailedVersion() {
    return asString("keyMetadataFailedVersion", "");
  }

  default Date getMetadataLastFailedTime() {
    return asDate("keyMetadataLastFailedTime", new Date(0L));
  }

  default Date getLastSuccessfulScheduledProgramNotifications() {
    return asDate("keyLastSuccessfulScheduledProgramNotifications", new Date(0L));
  }

  default Date getLastSuccessfulScheduledDataSetNotifications() {
    return asDate("keyLastSuccessfulScheduledDataSetNotifications", new Date(0L));
  }

  default String getRemoteMetadataVersion() {
    return asString("keyRemoteMetadataVersion", "");
  }

  default String getSystemMetadataVersion() {
    return asString("keySystemMetadataVersion", "");
  }

  default boolean getStopMetadataSync() {
    return asBoolean("keyStopMetadataSync", false);
  }

  default FileResourceRetentionStrategy getFileResourceRetentionStrategy() {
    return asEnum("keyFileResourceRetentionStrategy", FileResourceRetentionStrategy.NONE);
  }

  default int getSyncMaxRemoteServerAvailabilityCheckAttempts() {
    return asInt("syncMaxRemoteServerAvailabilityCheckAttempts", 3);
  }

  default int getSyncMaxAttempts() {
    return asInt("syncMaxAttempts", 3);
  }

  default int getSyncDelayBetweenRemoteServerAvailabilityCheckAttempts() {
    return asInt("syncDelayBetweenRemoteServerAvailabilityCheckAttempts", 500);
  }

  default Date getLastSuccessfulDataStatistics() {
    return asDate("lastSuccessfulDataStatistics", new Date(0L));
  }

  default boolean getHideDailyPeriods() {
    return asBoolean("keyHideDailyPeriods", false);
  }

  default boolean getHideWeeklyPeriods() {
    return asBoolean("keyHideWeeklyPeriods", false);
  }

  default boolean getHideBiWeeklyPeriods() {
    return asBoolean("keyHideBiWeeklyPeriods", false);
  }

  default boolean getHideMonthlyPeriods() {
    return asBoolean("keyHideMonthlyPeriods", false);
  }

  default boolean getHideBiMonthlyPeriods() {
    return asBoolean("keyHideBiMonthlyPeriods", false);
  }

  default boolean getGatherAnalyticalObjectStatisticsInDashboardViews() {
    return asBoolean("keyGatherAnalyticalObjectStatisticsInDashboardViews", false);
  }

  default boolean getCountPassiveDashboardViewsInUsageAnalytics() {
    return asBoolean("keyCountPassiveDashboardViewsInUsageAnalytics", false);
  }

  default boolean getDashboardContextMenuItemSwitchViewType() {
    return asBoolean("keyDashboardContextMenuItemSwitchViewType", true);
  }

  default boolean getDashboardContextMenuItemOpenInRelevantApp() {
    return asBoolean("keyDashboardContextMenuItemOpenInRelevantApp", true);
  }

  default boolean getDashboardContextMenuItemShowInterpretationsAndDetails() {
    return asBoolean("keyDashboardContextMenuItemShowInterpretationsAndDetails", true);
  }

  default boolean getDashboardContextMenuItemViewFullscreen() {
    return asBoolean("keyDashboardContextMenuItemViewFullscreen", true);
  }

  default String getDefaultBaseMap() {
    return asString("keyDefaultBaseMap", "");
  }

  default boolean getRuleEngineAssignOverwrite() {
    return asBoolean("ruleEngineAssignOverwrite", false);
  }

  /**
   * @return A job that has not been updating its "alive" timestamp for this number of minutes is
   *     reset to initial state of being scheduled by the heartbeat job. The run that was in
   *     progress is considered a failed run.
   */
  default int getJobsRescheduleAfterMinutes() {
    return asInt("jobsRescheduleAfterMinutes", 10);
  }

  /**
   * @return A job that only runs once (typical an import or manual request) is deleted after this
   *     number of minutes after it is finished by the heartbeat job.
   */
  default int getJobsCleanupAfterMinutes() {
    return asInt("jobsCleanupAfterMinutes", 24 * 60);
  }

  /**
   * @return The maximum number of hours a CRON based job may trigger on the same day after it has
   *     missed its intended time of the day to trigger. If time has passed past this point the
   *     execution for that day is skipped, and it will trigger on the intended time the day after.
   */
  default int getJobsMaxCronDelayHours() {
    return asInt("jobsMaxCronDelayHours", JobConfiguration.MAX_CRON_DELAY_HOURS);
  }

  /**
   * @return A job running with a smaller delay than the given value is logged on debug level
   *     instead of info to not spam the logs.
   */
  default int getJobsLogDebugBelowSeconds() {
    return asInt("jobsLogDebugBelowSeconds", 180);
  }

  /**
   * @return Progressive caching factor for the analytics API. To enable, the {@link
   *     #getAnalyticsCacheTtlMode()} must be set to PROGRESSIVE.
   */
  default int getAnalyticsCacheProgressiveTtlFactor() {
    return asInt("keyAnalyticsCacheProgressiveTtlFactor", 160);
  }

  /**
   * @return The cache time to live value for the analytics API.
   */
  default AnalyticsCacheTtlMode getAnalyticsCacheTtlMode() {
    return asEnum("keyAnalyticsCacheTtlMode", AnalyticsCacheTtlMode.FIXED);
  }

  /**
   * @return The offset of years used during period generation during the analytics export process.
   */
  default int getAnalyticsPeriodYearsOffset() {
    return asInt("keyAnalyticsPeriodYearsOffset", -1);
  }

  /**
   * @return The limit of combinations of columns and rows allowed in downloads.
   */
  default int getAnalyticsDownloadCombinationLimit() {
    return asInt("keyAnalyticsDownloadCombinationLimit", 20000);
  }

  /**
   * @return Max tracked entity records that can be retrieved from database.
   */
  default int getTrackedEntityMaxLimit() {
    return asInt("KeyTrackedEntityMaxLimit", 50000);
  }

  @Translatable
  default String getLoginPopup() {
    return asString("loginPopup", "");
  }

  default String getHtmlPushAnalyticsUrl() {
    return asString("keyHtmlPushAnalyticsUrl", "");
  }

  /**
   * @return The layout of the LoginPage, value is the enum {@link LoginPageLayout}
   */
  default LoginPageLayout getLoginPageLayout() {
    return asEnum("loginPageLayout", LoginPageLayout.DEFAULT);
  }

  /**
   * @return The HTML string which is used for displaying LoginPage when {@link
   *     #getLoginPageLayout()} is {@link LoginPageLayout#CUSTOM}.
   */
  default String getLoginPageTemplate() {
    return asString("loginPageTemplate", "");
  }

  /**
   * @return The app to serve as the global app shell. Global app shell is disabled if this is NULL
   *     or if the app does not exist *
   */
  default String getGlobalShellAppName() {
    return asString("globalShellAppName", "global-shell");
  }

  /**
   * @return true if apps should be served within a global shell.
   */
  default boolean getGlobalShellEnabled() {
    return asBoolean("globalShellEnabled", true);
  }

  /**
   * @return true if email verification is enforced for all users.
   */
  default boolean getEnforceVerifiedEmail() {
    return asBoolean("enforceVerifiedEmail", false);
  }

  /**
   * @since 2.42
   * @return the minimum level required to include a notification in the list for notifications
   *     forwarded from scheduling.
   */
  default NotificationLevel getNotifierLogLevel() {
    return asEnum("notifierLogLevel", NotificationLevel.DEBUG);
  }

  /**
   * @since 2.42
   * @return the maximum number of messages kept for each job. When the limit is exceeded the oldest
   *     message is dropped (FIFO).
   */
  default int getNotifierMaxMessagesPerJob() {
    return asInt("notifierMaxMessagesPerJob", 500);
  }

  /**
   * @since 2.42
   * @return notifications and summaries older than this number of days are discarded automatically
   */
  default int getNotifierMaxAgeDays() {
    return asInt("notifierMaxAgeDays", 7);
  }

  /**
   * @since 2.42
   * @return notifications and summaries from this number of jobs per job type are kept (youngest
   *     remain). The oldest jobs exceeding this number per type are discarded.
   */
  default int getNotifierMaxJobsPerType() {
    return asInt("notifierMaxJobsPerType", 500);
  }

  /**
   * @since 2.42
   * @return when true, only the first and last message are included in the message stack when
   *     listing multiple jobs in the single or all job types overview.
   */
  default boolean getNotifierGistOverview() {
    return asBoolean("notifierGistOverview", true);
  }

  /**
   * @since 2.42
   * @return the duration the notifier has to be idle to run an automatic cleanup cycle
   */
  default long getNotifierCleanAfterIdleTime() {
    return asInt("notifierCleanAfterIdleTime", 60_0000); // 1 minute
  }

  /**
   * @since 2.42
   * @return true if the experimental analytics query engine should be used for analytics queries.
   *     This engine is only required when using ClickHouse or Doris as the analytics database.
   */
  default boolean getUseExperimentalAnalyticsQueryEngine() {
    return asBoolean("experimentalAnalyticsSqlEngineEnabled", false);
  }

  /**
   * @since 2.40
   * @return if true, the analytics event tables are created with a centroid value for each Data
   *     Element or TEA of type OU or ougeometry
   */
  default boolean getOrgUnitCentroidsInEventsAnalytics() {
    return asBoolean("orgUnitCentroidsInEventsAnalytics", false);
  }

  /**
   * @return true if emails for invited users should be automatically verified.
   */
  default boolean getAutoVerifyInvitedUserEmail() {
    return asBoolean("autoVerifyInvitedUserEmail", true);
  }

  /* Combinators based on several settings. */

  default boolean isEmailConfigured() {
    return !getEmailHostName().isBlank() && !getEmailUsername().isBlank();
  }

  default boolean isHideUnapprovedDataInAnalytics() {
    // -1 means approval is disabled
    return getIgnoreAnalyticsApprovalYearThreshold() >= 0;
  }
}
