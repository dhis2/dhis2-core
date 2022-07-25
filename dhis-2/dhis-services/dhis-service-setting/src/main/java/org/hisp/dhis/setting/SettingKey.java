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
package org.hisp.dhis.setting;

import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Stream;

import org.apache.commons.lang3.LocaleUtils;
import org.hisp.dhis.analytics.AnalyticsCacheTtlMode;
import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.common.DigitGroupSeparator;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.common.cache.Cacheability;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.fileresource.FileResourceRetentionStrategy;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.sms.config.SmsConfiguration;

import com.google.common.collect.ImmutableSet;

/**
 * @author Lars Helge Overland
 */
public enum SettingKey
{
    UI_LOCALE( "keyUiLocale", LocaleManager.DEFAULT_LOCALE, Locale.class ),
    DB_LOCALE( "keyDbLocale", Locale.class ),
    ANALYSIS_DISPLAY_PROPERTY( "keyAnalysisDisplayProperty", DisplayProperty.NAME, DisplayProperty.class ),
    ANALYSIS_DIGIT_GROUP_SEPARATOR( "keyAnalysisDigitGroupSeparator", DigitGroupSeparator.SPACE,
        DigitGroupSeparator.class ),
    CURRENT_DOMAIN_TYPE( "keyCurrentDomainType" ),
    TRACKER_DASHBOARD_LAYOUT( "keyTrackerDashboardLayout" ),
    APPLICATION_TITLE( "applicationTitle", "DHIS 2", String.class, false, true ),
    APPLICATION_INTRO( "keyApplicationIntro", true ),
    APPLICATION_NOTIFICATION( "keyApplicationNotification", true ),
    APPLICATION_FOOTER( "keyApplicationFooter", true ),
    APPLICATION_RIGHT_FOOTER( "keyApplicationRightFooter", true ),
    FLAG( "keyFlag", "dhis2", String.class ),
    FLAG_IMAGE( "keyFlagImage" ),
    START_MODULE( "startModule", "dhis-web-dashboard", String.class ),
    START_MODULE_ENABLE_LIGHT_WEIGHT( "startModuleEnableLightweight", Boolean.FALSE, Boolean.class ),
    FACTOR_OF_DEVIATION( "factorDeviation", 2d, Double.class ),
    EMAIL_HOST_NAME( "keyEmailHostName" ),
    EMAIL_PORT( "keyEmailPort", 587, Integer.class ),
    EMAIL_USERNAME( "keyEmailUsername" ),
    EMAIL_TLS( "keyEmailTls", Boolean.TRUE, Boolean.class ),
    EMAIL_SENDER( "keyEmailSender", "no-reply@dhis2.org", String.class ),
    EMAIL_PASSWORD( "keyEmailPassword", "", String.class, true, false ),
    MIN_PASSWORD_LENGTH( "minPasswordLength", 8, Integer.class ),
    MAX_PASSWORD_LENGTH( "maxPasswordLength", 40, Integer.class ),
    SMS_CONFIG( "keySmsSetting", new SmsConfiguration(), SmsConfiguration.class ),
    /**
     * Max SMS text message length, default 7 * 153 ( 7 parts , 153 is message
     * length for multi parts )
     **/
    SMS_MAX_LENGTH( "keySmsMaxLength", 1071, Integer.class ),
    CACHE_STRATEGY( "keyCacheStrategy", CacheStrategy.CACHE_1_MINUTE, CacheStrategy.class ),
    CACHEABILITY( "keyCacheability", Cacheability.PUBLIC, Cacheability.class ),
    ANALYTICS_FINANCIAL_YEAR_START( "analyticsFinancialYearStart",
        AnalyticsFinancialYearStartKey.FINANCIAL_YEAR_OCTOBER, AnalyticsFinancialYearStartKey.class ),
    PHONE_NUMBER_AREA_CODE( "phoneNumberAreaCode" ),
    MULTI_ORGANISATION_UNIT_FORMS( "multiOrganisationUnitForms", Boolean.FALSE, Boolean.class ),
    CONFIGURATION( "keyConfig", Configuration.class ),
    ACCOUNT_RECOVERY( "keyAccountRecovery", Boolean.FALSE, Boolean.class ),
    LOCK_MULTIPLE_FAILED_LOGINS( "keyLockMultipleFailedLogins", Boolean.FALSE, Boolean.class ),
    GOOGLE_ANALYTICS_UA( "googleAnalyticsUA" ),
    CREDENTIALS_EXPIRES( "credentialsExpires", 0, Integer.class ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlert", false, Boolean.class ),
    ACCOUNT_EXPIRES_IN_DAYS( "accountExpiresInDays", 7, Integer.class ),
    ACCOUNT_EXPIRY_ALERT( "accountExpiryAlert", false, Boolean.class ),
    SELF_REGISTRATION_NO_RECAPTCHA( "keySelfRegistrationNoRecaptcha", Boolean.FALSE, Boolean.class ),
    RECAPTCHA_SECRET( "recaptchaSecret", "6LcVwT0UAAAAAAtMWnPoerWwLx_DSwrcEncHCiWu", String.class, true, false ),
    RECAPTCHA_SITE( "recaptchaSite", "6LcVwT0UAAAAAAkO_EGPiYOiymIszZUeHfqWIYX5", String.class, true, false ),
    CAN_GRANT_OWN_USER_ROLES( "keyCanGrantOwnUserAuthorityGroups", Boolean.FALSE, Boolean.class ),
    IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD( "keyIgnoreAnalyticsApprovalYearThreshold", -1, Integer.class ),
    ANALYTICS_MAX_LIMIT( "keyAnalyticsMaxLimit", 100000, Integer.class ),
    INCLUDE_ZERO_VALUES_IN_ANALYTICS( "keyIncludeZeroValuesInAnalytics", Boolean.FALSE, Boolean.class ),
    SQL_VIEW_MAX_LIMIT( "keySqlViewMaxLimit", -1, Integer.class ),
    RESPECT_META_DATA_START_END_DATES_IN_ANALYTICS_TABLE_EXPORT(
        "keyRespectMetaDataStartEndDatesInAnalyticsTableExport", Boolean.FALSE, Boolean.class ),
    SKIP_DATA_TYPE_VALIDATION_IN_ANALYTICS_TABLE_EXPORT( "keySkipDataTypeValidationInAnalyticsTableExport",
        Boolean.FALSE, Boolean.class ),
    CUSTOM_LOGIN_PAGE_LOGO( "keyCustomLoginPageLogo", Boolean.FALSE, Boolean.class ),
    CUSTOM_TOP_MENU_LOGO( "keyCustomTopMenuLogo", Boolean.FALSE, Boolean.class ),
    ANALYTICS_MAINTENANCE_MODE( "keyAnalyticsMaintenanceMode", Boolean.FALSE, Boolean.class ),
    DATABASE_SERVER_CPUS( "keyDatabaseServerCpus", 0, Integer.class ),
    LAST_SUCCESSFUL_ANALYTICS_TABLES_RUNTIME( "keyLastSuccessfulAnalyticsTablesRuntime" ),
    LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_RUNTIME( "keyLastSuccessfulLatestAnalyticsPartitionRuntime" ),
    LAST_MONITORING_RUN( "keyLastMonitoringRun", Date.class ),
    LAST_SUCCESSFUL_DATA_VALUE_SYNC( "keyLastSuccessfulDataSynch", new Date( 0 ), Date.class ),
    LAST_SUCCESSFUL_EVENT_DATA_SYNC( "keyLastSuccessfulEventsDataSynch", new Date( 0 ), Date.class ),
    LAST_SUCCESSFUL_COMPLETE_DATA_SET_REGISTRATION_SYNC( "keyLastCompleteDataSetRegistrationSyncSuccess", new Date( 0 ),
        Date.class ),
    SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE( "syncSkipSyncForDataChangedBefore", new Date( 0 ), Date.class ),
    LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE( "keyLastSuccessfulAnalyticsTablesUpdate", Date.class ),
    LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE( "keyLastSuccessfulLatestAnalyticsPartitionUpdate", Date.class ),
    LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE( "keyLastSuccessfulResourceTablesUpdate", Date.class ),
    LAST_SUCCESSFUL_SYSTEM_MONITORING_PUSH( "keyLastSuccessfulSystemMonitoringPush", Date.class ),
    LAST_SUCCESSFUL_MONITORING( "keyLastSuccessfulMonitoring", Date.class ),
    NEXT_ANALYTICS_TABLE_UPDATE( "keyNextAnalyticsTableUpdate", Date.class ),
    HELP_PAGE_LINK( "helpPageLink", "https://dhis2.github.io/dhis2-docs/master/en/user/html/dhis2_user_manual_en.html",
        String.class ),
    ACCEPTANCE_REQUIRED_FOR_APPROVAL( "keyAcceptanceRequiredForApproval", Boolean.FALSE, Boolean.class ),
    SYSTEM_NOTIFICATIONS_EMAIL( "keySystemNotificationsEmail" ),
    ANALYSIS_RELATIVE_PERIOD( "keyAnalysisRelativePeriod", RelativePeriodEnum.LAST_12_MONTHS,
        RelativePeriodEnum.class ),
    REQUIRE_ADD_TO_VIEW( "keyRequireAddToView", Boolean.FALSE, Boolean.class ),
    ALLOW_OBJECT_ASSIGNMENT( "keyAllowObjectAssignment", Boolean.FALSE, Boolean.class ),
    USE_CUSTOM_LOGO_FRONT( "keyUseCustomLogoFront", Boolean.FALSE, Boolean.class ),
    USE_CUSTOM_LOGO_BANNER( "keyUseCustomLogoBanner", Boolean.FALSE, Boolean.class ),
    METADATA_REPO_URL( "keyMetaDataRepoUrl",
        "https://raw.githubusercontent.com/dhis2/dhis2-metadata-repo/master/repo/221/index.json", String.class ),
    DATA_IMPORT_STRICT_PERIODS( "keyDataImportStrictPeriods", Boolean.FALSE, Boolean.class ),
    DATA_IMPORT_STRICT_DATA_ELEMENTS( "keyDataImportStrictDataElements", Boolean.FALSE, Boolean.class ),
    DATA_IMPORT_STRICT_CATEGORY_OPTION_COMBOS( "keyDataImportStrictCategoryOptionCombos", Boolean.FALSE,
        Boolean.class ),
    DATA_IMPORT_STRICT_ORGANISATION_UNITS( "keyDataImportStrictOrganisationUnits", Boolean.FALSE, Boolean.class ),
    DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS( "keyDataImportStrictAttributeOptionCombos", Boolean.FALSE,
        Boolean.class ),
    DATA_IMPORT_REQUIRE_CATEGORY_OPTION_COMBO( "keyDataImportRequireCategoryOptionCombo", Boolean.FALSE,
        Boolean.class ),
    DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO( "keyDataImportRequireAttributeOptionCombo", Boolean.FALSE,
        Boolean.class ),
    CUSTOM_JS( "keyCustomJs" ),
    CUSTOM_CSS( "keyCustomCss" ),
    CALENDAR( "keyCalendar", "iso8601", String.class ),
    DATE_FORMAT( "keyDateFormat", "yyyy-MM-dd", String.class ),
    STYLE( "keyStyle", "light_blue/light_blue.css", String.class ),
    REMOTE_INSTANCE_URL( "keyRemoteInstanceUrl", "", String.class ),
    REMOTE_INSTANCE_USERNAME( "keyRemoteInstanceUsername", "", String.class ),
    REMOTE_INSTANCE_PASSWORD( "keyRemoteInstancePassword", "", String.class, true, false ),
    GOOGLE_MAPS_API_KEY( "keyGoogleMapsApiKey", "AIzaSyBjlDmwuON9lJbPMDlh_LI3zGpGtpK9erc", String.class, false, false ),
    BING_MAPS_API_KEY( "keyBingMapsApiKey", "AoifMs0zqvpAEuI6OX5Kk93rEM-oLrvQIJe_xdCv1BF4J3yquFnUozze-M7gEf0b",
        String.class, false, false ),
    LAST_SUCCESSFUL_METADATA_SYNC( "keyLastMetaDataSyncSuccess", Date.class ),
    METADATAVERSION_ENABLED( "keyVersionEnabled", Boolean.FALSE, Boolean.class ),
    METADATA_FAILED_VERSION( "keyMetadataFailedVersion", String.class ),
    METADATA_LAST_FAILED_TIME( "keyMetadataLastFailedTime", Date.class ),
    LAST_SUCCESSFUL_SCHEDULED_PROGRAM_NOTIFICATIONS( "keyLastSuccessfulScheduledProgramNotifications", Date.class ),
    LAST_SUCCESSFUL_SCHEDULED_DATASET_NOTIFICATIONS( "keyLastSuccessfulScheduledDataSetNotifications", Date.class ),
    REMOTE_METADATA_VERSION( "keyRemoteMetadataVersion", String.class ),
    SYSTEM_METADATA_VERSION( "keySystemMetadataVersion", String.class ),
    STOP_METADATA_SYNC( "keyStopMetadataSync", Boolean.FALSE, Boolean.class ),
    FILE_RESOURCE_RETENTION_STRATEGY( "keyFileResourceRetentionStrategy", FileResourceRetentionStrategy.NONE,
        FileResourceRetentionStrategy.class ),
    MAX_REMOTE_SERVER_AVAILABILITY_CHECK_ATTEMPTS( "syncMaxRemoteServerAvailabilityCheckAttempts", 3, Integer.class ),
    MAX_SYNC_ATTEMPTS( "syncMaxAttempts", 3, Integer.class ),
    DELAY_BETWEEN_REMOTE_SERVER_AVAILABILITY_CHECK_ATTEMPTS( "syncDelayBetweenRemoteServerAvailabilityCheckAttempts",
        500, Integer.class ),
    LAST_SUCCESSFUL_DATA_STATISTICS( "lastSuccessfulDataStatistics", Date.class ),
    ANALYTICS_HIDE_DAILY_PERIODS( "keyHideDailyPeriods", Boolean.FALSE, Boolean.class ),
    ANALYTICS_HIDE_WEEKLY_PERIODS( "keyHideWeeklyPeriods", Boolean.FALSE, Boolean.class ),
    ANALYTICS_HIDE_BIWEEKLY_PERIODS( "keyHideBiWeeklyPeriods", Boolean.FALSE, Boolean.class ),
    ANALYTICS_HIDE_MONTHLY_PERIODS( "keyHideMonthlyPeriods", Boolean.FALSE, Boolean.class ),
    ANALYTICS_HIDE_BIMONTHLY_PERIODS( "keyHideBiMonthlyPeriods", Boolean.FALSE, Boolean.class ),
    GATHER_ANALYTICAL_OBJECT_STATISTICS_IN_DASHBOARD_VIEWS( "keyGatherAnalyticalObjectStatisticsInDashboardViews",
        Boolean.FALSE, Boolean.class ),
    COUNT_PASSIVE_DASHBOARD_VIEWS_IN_USAGE_ANALYTICS( "keyCountPassiveDashboardViewsInUsageAnalytics",
        Boolean.FALSE, Boolean.class ),
    DASHBOARD_CONTEXT_MENU_ITEM_SWITCH_VIEW_TYPE( "keyDashboardContextMenuItemSwitchViewType", Boolean.TRUE,
        Boolean.class ),
    DASHBOARD_CONTEXT_MENU_ITEM_OPEN_IN_RELEVANT_APP( "keyDashboardContextMenuItemOpenInRelevantApp", Boolean.TRUE,
        Boolean.class ),
    DASHBOARD_CONTEXT_MENU_ITEM_SHOW_INTERPRETATIONS_AND_DETAILS(
        "keyDashboardContextMenuItemShowInterpretationsAndDetails", Boolean.TRUE, Boolean.class ),
    DASHBOARD_CONTEXT_MENU_ITEM_VIEW_FULLSCREEN( "keyDashboardContextMenuItemViewFullscreen", Boolean.TRUE,
        Boolean.class ),
    DEFAULT_BASE_MAP( "keyDefaultBaseMap" ),
    RULE_ENGINE_ASSIGN_OVERWRITE( "ruleEngineAssignOverwrite", Boolean.FALSE, Boolean.class ),

    /**
     * Progressive caching factor definition for Analytics. In order to enable
     * it, the {@link #ANALYTICS_CACHE_TTL_MODE} has to be set to PROGRESSIVE.
     */
    ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR( "keyAnalyticsCacheProgressiveTtlFactor", 160, Integer.class ),

    /**
     * The caching strategy enabled.
     */
    ANALYTICS_CACHE_TTL_MODE( "keyAnalyticsCacheTtlMode", AnalyticsCacheTtlMode.FIXED, AnalyticsCacheTtlMode.class );

    private final String name;

    private final Serializable defaultValue;

    private final Class<? extends Serializable> clazz;

    private final boolean confidential;

    private final boolean translatable;

    private static final ImmutableSet<String> NAMES = getNameSet();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    SettingKey( String name )
    {
        this( name, null, String.class, false, false );
    }

    SettingKey( String name, boolean translatable )
    {
        this( name, null, String.class, false, translatable );
    }

    SettingKey( String name, Class<? extends Serializable> clazz )
    {
        this( name, null, clazz, false, false );
    }

    <T extends Serializable> SettingKey( String name, T defaultValue, Class<T> clazz )
    {
        this( name, defaultValue, clazz, false, false );
    }

    <T extends Serializable> SettingKey( String name, T defaultValue, Class<T> clazz, boolean confidential,
        boolean translatable )
    {
        this.name = name;
        this.defaultValue = defaultValue;
        this.clazz = clazz;
        this.confidential = confidential;
        this.translatable = translatable;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public static Optional<SettingKey> getByName( String name )
    {
        for ( SettingKey setting : SettingKey.values() )
        {
            if ( setting.getName().equals( name ) )
            {
                return Optional.of( setting );
            }
        }

        return Optional.empty();
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public static Serializable getAsRealClass( String name, String value )
    {
        Optional<SettingKey> setting = getByName( name );

        if ( setting.isPresent() )
        {
            Class<?> settingClazz = setting.get().getClazz();

            if ( Double.class.isAssignableFrom( settingClazz ) )
            {
                return Double.valueOf( value );
            }
            else if ( Integer.class.isAssignableFrom( settingClazz ) )
            {
                return Integer.valueOf( value );
            }
            else if ( Boolean.class.isAssignableFrom( settingClazz ) )
            {
                return Boolean.valueOf( value );
            }
            else if ( Locale.class.isAssignableFrom( settingClazz ) )
            {
                return LocaleUtils.toLocale( value );
            }
            else if ( DigitGroupSeparator.class.isAssignableFrom( settingClazz ) )
            {
                return DigitGroupSeparator.valueOf( value );
            }
            else if ( Cacheability.class.isAssignableFrom( settingClazz ) )
            {
                return Cacheability.valueOf( value );
            }
            else if ( AnalyticsFinancialYearStartKey.class.isAssignableFrom( settingClazz ) )
            {
                return AnalyticsFinancialYearStartKey.valueOf( value );
            }
            else if ( FileResourceRetentionStrategy.class.isAssignableFrom( settingClazz ) )
            {
                return FileResourceRetentionStrategy.valueOf( value );
            }
            else if ( Date.class.isAssignableFrom( settingClazz ) )
            {
                // Accepts String with date in ISO_LOCAL_DATE_TIME format
                LocalDateTime dateTime = LocalDateTime.parse( value );

                return Date.from( dateTime.atZone( ZoneId.systemDefault() ).toInstant() );
            }
            else if ( Enum.class.isAssignableFrom( settingClazz ) )
            {
                return Enum.valueOf( (Class<? extends Enum>) settingClazz, value.toUpperCase() );
            }

            // TODO handle Dates
        }

        return value;
    }

    public boolean hasDefaultValue()
    {
        return defaultValue != null;
    }

    public static Set<String> getNames()
    {
        return NAMES;
    }

    private static ImmutableSet<String> getNameSet()
    {
        return ImmutableSet.copyOf( Stream.of( SettingKey.values() )
            .map( SettingKey::getName )
            .collect( toSet() ) );
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public Serializable getDefaultValue()
    {
        return defaultValue;
    }

    public Class<? extends Serializable> getClazz()
    {
        return clazz;
    }

    public boolean isConfidential()
    {
        return confidential;
    }

    public boolean isTranslatable()
    {
        return translatable;
    }

    @Override
    public String toString()
    {
        return new StringJoiner( ", ", SettingKey.class.getSimpleName() + "[", "]" )
            .add( "name='" + name + "'" )
            .add( "defaultValue=" + defaultValue )
            .add( "clazz=" + clazz )
            .add( "confidential=" + confidential )
            .add( "translatable=" + translatable ).toString();
    }
}
