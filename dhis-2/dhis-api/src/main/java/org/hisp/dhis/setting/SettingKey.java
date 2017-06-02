package org.hisp.dhis.setting;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.LocaleUtils;
import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.common.DigitGroupSeparator;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.cache.Cacheability;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.sms.config.SmsConfiguration;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
public enum SettingKey
{
    MESSAGE_EMAIL_NOTIFICATION( "keyMessageEmailNotification", Boolean.FALSE, Boolean.class ),
    MESSAGE_SMS_NOTIFICATION( "keyMessageSmsNotification", Boolean.FALSE, Boolean.class ),
    UI_LOCALE( "keyUiLocale", LocaleManager.DEFAULT_LOCALE, Locale.class ),
    DB_LOCALE( "keyDbLocale", Locale.class ),
    ANALYSIS_DISPLAY_PROPERTY( "keyAnalysisDisplayProperty", "name", String.class ),
    ANALYSIS_DIGIT_GROUP_SEPARATOR( "keyAnalysisDigitGroupSeparator", DigitGroupSeparator.SPACE, DigitGroupSeparator.class ),
    CURRENT_DOMAIN_TYPE( "keyCurrentDomainType" ),
    AUTO_SAVE_CASE_ENTRY_FORM( "keyAutoSaveCaseEntryForm", Boolean.FALSE, Boolean.class ),
    AUTO_SAVE_TRACKED_ENTITY_REGISTRATION_ENTRY_FORM( "keyAutoSavetTrackedEntityForm", Boolean.FALSE, Boolean.class ),
    AUTO_SAVE_DATA_ENTRY_FORM( "keyAutoSaveDataEntryForm", Boolean.FALSE, Boolean.class ),
    TRACKER_DASHBOARD_LAYOUT( "keyTrackerDashboardLayout" ),
    APPLICATION_TITLE( "applicationTitle", "DHIS 2", String.class ),
    APPLICATION_INTRO( "keyApplicationIntro" ),
    APPLICATION_NOTIFICATION( "keyApplicationNotification" ),
    APPLICATION_FOOTER( "keyApplicationFooter" ),
    APPLICATION_RIGHT_FOOTER( "keyApplicationRightFooter" ),
    FLAG( "keyFlag", "dhis2", String.class ),
    FLAG_IMAGE( "keyFlagImage" ),
    START_MODULE( "startModule", "dhis-web-dashboard-integration", String.class ),
    FACTOR_OF_DEVIATION( "factorDeviation", 2d, Double.class ),
    EMAIL_HOST_NAME( "keyEmailHostName" ),
    EMAIL_PORT( "keyEmailPort", 587, Integer.class ),
    EMAIL_USERNAME( "keyEmailUsername" ),
    EMAIL_TLS( "keyEmailTls", Boolean.TRUE, Boolean.class ),
    EMAIL_SENDER( "keyEmailSender", "no-reply@dhis2.org", String.class ),
    EMAIL_PASSWORD( "keyEmailPassword", "", String.class, true ),
    MIN_PASSWORD_LENGTH( "minPasswordLength", 8, Integer.class ),
    MAX_PASSWORD_LENGTH( "maxPasswordLength", 40, Integer.class ),
    INSTANCE_BASE_URL( "keyInstanceBaseUrl" ),
    SCHEDULED_TASKS( "keySchedTasks", ListMap.class ),
    SMS_CONFIG( "keySmsSetting", SmsConfiguration.class ),
    CACHE_STRATEGY( "keyCacheStrategy", "CACHE_6AM_TOMORROW", String.class ),
    CACHEABILITY( "keyCacheability", Cacheability.PUBLIC, Cacheability.class ),
    CACHE_ANALYTICS_DATA_YEAR_THRESHOLD( "keyCacheAnalyticsDataYearThreshold", 0, Integer.class ),
    ANALYTICS_FINANCIAL_YEAR_START( "analyticsFinancialYearStart", AnalyticsFinancialYearStartKey.FINANCIAL_PERIOD_OCTOBER, AnalyticsFinancialYearStartKey.class ),
    PHONE_NUMBER_AREA_CODE( "phoneNumberAreaCode" ),
    MULTI_ORGANISATION_UNIT_FORMS( "multiOrganisationUnitForms", Boolean.FALSE, Boolean.class ),
    CONFIGURATION( "keyConfig", Configuration.class ),
    ACCOUNT_RECOVERY( "keyAccountRecovery", Boolean.FALSE, Boolean.class ),
    LOCK_MULTIPLE_FAILED_LOGINS( "keyLockMultipleFailedLogins", Boolean.FALSE, Boolean.class ),
    GOOGLE_ANALYTICS_UA( "googleAnalyticsUA" ),
    CREDENTIALS_EXPIRES( "credentialsExpires", 0, Integer.class ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlert", false, Boolean.class ),
    SELF_REGISTRATION_NO_RECAPTCHA( "keySelfRegistrationNoRecaptcha", Boolean.FALSE, Boolean.class ),
    OPENID_PROVIDER( "keyOpenIdProvider" ),
    OPENID_PROVIDER_LABEL( "keyOpenIdProviderLabel" ),
    CAN_GRANT_OWN_USER_AUTHORITY_GROUPS( "keyCanGrantOwnUserAuthorityGroups", Boolean.FALSE, Boolean.class ),
    IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD( "keyIgnoreAnalyticsApprovalYearThreshold", -1, Integer.class ),
    ANALYTICS_MAX_LIMIT( "keyAnalyticsMaxLimit", 100000, Integer.class ),
    RESPECT_META_DATA_START_END_DATES_IN_ANALYTICS_TABLE_EXPORT( "keyRespectMetaDataStartEndDatesInAnalyticsTableExport", Boolean.FALSE, Boolean.class ),
    SKIP_DATA_TYPE_VALIDATION_IN_ANALYTICS_TABLE_EXPORT( "keySkipDataTypeValidationInAnalyticsTableExport", Boolean.FALSE, Boolean.class ),
    CUSTOM_LOGIN_PAGE_LOGO( "keyCustomLoginPageLogo", Boolean.FALSE, Boolean.class ),
    CUSTOM_TOP_MENU_LOGO( "keyCustomTopMenuLogo", Boolean.FALSE, Boolean.class ),
    ANALYTICS_MAINTENANCE_MODE( "keyAnalyticsMaintenanceMode", Boolean.FALSE, Boolean.class ),
    DATABASE_SERVER_CPUS( "keyDatabaseServerCpus", 0, Integer.class ),
    LAST_SUCCESSFUL_ANALYTICS_TABLES_RUNTIME( "keyLastSuccessfulAnalyticsTablesRuntime" ),
    LAST_MONITORING_RUN( "keyLastMonitoringRun", Date.class ),
    LAST_SUCCESSFUL_DATA_SYNC( "keyLastSuccessfulDataSynch", Date.class ),
    LAST_SUCCESSFUL_EVENT_DATA_SYNC( "keyLastSuccessfulEventsDataSynch", Date.class ),
    LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE( "keyLastSuccessfulAnalyticsTablesUpdate", Date.class ),
    LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE( "keyLastSuccessfulResourceTablesUpdate", Date.class ),
    LAST_SUCCESSFUL_MONITORING( "keyLastSuccessfulMonitoring", Date.class ),
    LAST_SUCCESSFUL_DATA_STATISTIC( "lastSuccessfulDataStatistics", Date.class ),
    HELP_PAGE_LINK( "helpPageLink", "https://dhis2.github.io/dhis2-docs/master/en/user/html/dhis2_user_manual_en.html", String.class ),
    ACCEPTANCE_REQUIRED_FOR_APPROVAL( "keyAcceptanceRequiredForApproval", Boolean.FALSE, Boolean.class ),
    SYSTEM_NOTIFICATIONS_EMAIL( "keySystemNotificationsEmail" ),
    ANALYSIS_RELATIVE_PERIOD( "keyAnalysisRelativePeriod", "LAST_12_MONTHS", String.class ),
    REQUIRE_ADD_TO_VIEW( "keyRequireAddToView", Boolean.FALSE, Boolean.class ),
    ALLOW_OBJECT_ASSIGNMENT( "keyAllowObjectAssignment", Boolean.FALSE, Boolean.class ),
    USE_CUSTOM_LOGO_FRONT( "keyUseCustomLogoFront", Boolean.FALSE, Boolean.class ),
    USE_CUSTOM_LOGO_BANNER( "keyUseCustomLogoBanner", Boolean.FALSE, Boolean.class ),
    METADATA_REPO_URL( "keyMetaDataRepoUrl", "https://raw.githubusercontent.com/dhis2/dhis2-metadata-repo/master/repo/221/index.json", String.class ),
    DATA_IMPORT_STRICT_PERIODS( "keyDataImportStrictPeriods", Boolean.FALSE, Boolean.class ),
    DATA_IMPORT_STRICT_CATEGORY_OPTION_COMBOS( "keyDataImportStrictCategoryOptionCombos", Boolean.FALSE, Boolean.class ),
    DATA_IMPORT_STRICT_ORGANISATION_UNITS( "keyDataImportStrictOrganisationUnits", Boolean.FALSE, Boolean.class ),
    DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS( "keyDataImportStrictAttributeOptionCombos", Boolean.FALSE, Boolean.class ),
    DATA_IMPORT_REQUIRE_CATEGORY_OPTION_COMBO( "keyDataImportRequireCategoryOptionCombo", Boolean.FALSE, Boolean.class ),
    DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO( "keyDataImportRequireAttributeOptionCombo", Boolean.FALSE, Boolean.class ),
    CUSTOM_JS( "keyCustomJs" ),
    CUSTOM_CSS( "keyCustomCss" ),
    CALENDAR( "keyCalendar", "iso8601", String.class ),
    DATE_FORMAT( "keyDateFormat", "yyyy-MM-dd", String.class ),
    APP_STORE_URL( "appStoreUrl", "https://www.dhis2.org/appstore", String.class ),
    APP_STORE_INDEX_URL( "appStoreIndexUrl", "https://s3-eu-west-1.amazonaws.com/dhis2-appstore/appstore.json", String.class ),
    STYLE( "keyStyle", "light_blue/light_blue.css", String.class ),
    REMOTE_INSTANCE_URL( "keyRemoteInstanceUrl", "", String.class ),
    REMOTE_INSTANCE_USERNAME( "keyRemoteInstanceUsername", "", String.class ),
    REMOTE_INSTANCE_PASSWORD( "keyRemoteInstancePassword", "", String.class, true ),
    SYSTEM_MONITORING_URL( "keySystemMonitoringUrl", "", String.class ),
    SYSTEM_MONITORING_USERNAME( "keySystemMonitoringUsername", "", String.class ),
    SYSTEM_MONITORING_PASSWORD( "keySystemMonitoringPassword", "", String.class, true ),
    MAPZEN_SEARCH_API_KEY( "keyMapzenSearchApiKey", "search-Se1CFzK", String.class ),
    GOOGLE_MAPS_API_KEY( "keyGoogleMapsApiKey", "AIzaSyBjlDmwuON9lJbPMDlh_LI3zGpGtpK9erc", String.class ),
    LAST_SUCCESSFUL_METADATA_SYNC( "keyLastMetaDataSyncSuccess", Date.class ),
    METADATAVERSION_ENABLED( "keyVersionEnabled", Boolean.FALSE, Boolean.class ),
    METADATA_FAILED_VERSION( "keyMetadataFailedVersion", String.class ),
    METADATA_LAST_FAILED_TIME( "keyMetadataLastFailedTime", Date.class ),
    METADATA_SYNC_CRON( "metaDataSyncCron", String.class ),
    DATA_SYNC_CRON( "dataSyncCron", String.class ),
    LAST_SUCCESSFUL_SCHEDULED_PROGRAM_NOTIFICATIONS( "keyLastSuccessfulScheduledProgramNotifications", Date.class ),
    REMOTE_METADATA_VERSION( "keyRemoteMetadataVersion", String.class ),
    SYSTEM_METADATA_VERSION( "keySystemMetadataVersion", String.class ),
    STOP_METADATA_SYNC( "keyStopMetadataSync", Boolean.FALSE, Boolean.class );

    private final String name;

    private final Serializable defaultValue;

    private final Class<?> clazz;

    private boolean confidential;

    private static final ImmutableSet<String> NAMES = getNameSet();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    SettingKey( String name )
    {
        this.name = name;
        this.defaultValue = null;
        this.clazz = String.class;
        this.confidential = false;
    }

    SettingKey( String name, Class<?> clazz )
    {
        this.name = name;
        this.defaultValue = null;
        this.clazz = clazz;
        this.confidential = false;
    }

    SettingKey( String name, Serializable defaultValue, Class<?> clazz )
    {
        this.name = name;
        this.defaultValue = defaultValue;
        this.clazz = clazz;
        this.confidential = false;
    }

    SettingKey( String name, Serializable defaultValue, Class<?> clazz, boolean confidential )
    {
        this.name = name;
        this.defaultValue = defaultValue;
        this.clazz = clazz;
        this.confidential = confidential;
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

            //TODO handle Dates
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
        Set<String> names = Sets.newHashSet();
        Sets.newHashSet( SettingKey.values() ).forEach( s -> names.add( s.getName() ) );
        return ImmutableSet.copyOf( names );
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

    public Class<?> getClazz()
    {
        return clazz;
    }

    public boolean isConfidential()
    {
        return confidential;
    }
}
