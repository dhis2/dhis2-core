package org.hisp.dhis.setting;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.apache.commons.lang3.LocaleUtils;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.sms.config.SmsConfiguration;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Lars Helge Overland
 */
public enum SettingKey
{
    MESSAGE_EMAIL_NOTIFICATION( "keyMessageEmailNotification", Boolean.FALSE, Boolean.class ),
    MESSAGE_SMS_NOTIFICATION( "keyMessageSmsNotification", Boolean.FALSE, Boolean.class ),
    UI_LOCALE( "keyUiLocale", Locale.class ),
    DB_LOCALE( "keyDbLocale", Locale.class ),
    ANALYSIS_DISPLAY_PROPERTY( "keyAnalysisDisplayProperty", "name", String.class ),
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
    EMAIL_SENDER( "keyEmailSender" ),
    EMAIL_PASSWORD( "keyEmailPassword", "", String.class, true ),
    INSTANCE_BASE_URL( "keyInstanceBaseUrl" ),
    SCHEDULED_TASKS( "keySchedTasks", ListMap.class ),
    SMS_CONFIG( "keySmsConfiguration", SmsConfiguration.class ),
    CACHE_STRATEGY( "keyCacheStrategy", "CACHE_6AM_TOMORROW", String.class ),
    PHONE_NUMBER_AREA_CODE( "phoneNumberAreaCode" ),
    MULTI_ORGANISATION_UNIT_FORMS( "multiOrganisationUnitForms", Boolean.FALSE, Boolean.class ),
    CONFIGURATION( "keyConfig", Configuration.class ),
    ACCOUNT_RECOVERY( "keyAccountRecovery", Boolean.FALSE, Boolean.class ),
    GOOGLE_ANALYTICS_UA( "googleAnalyticsUA" ),
    CREDENTIALS_EXPIRES( "credentialsExpires", 0, Integer.class ),
    SELF_REGISTRATION_NO_RECAPTCHA( "keySelfRegistrationNoRecaptcha", Boolean.FALSE, Boolean.class ),
    OPENID_PROVIDER( "keyOpenIdProvider" ),
    OPENID_PROVIDER_LABEL( "keyOpenIdProviderLabel" ),
    CAN_GRANT_OWN_USER_AUTHORITY_GROUPS( "keyCanGrantOwnUserAuthorityGroups", Boolean.FALSE, Boolean.class ),
    HIDE_UNAPPROVED_DATA_IN_ANALYTICS( "keyHideUnapprovedDataInAnalytics", Boolean.FALSE, Boolean.class ),
    ANALYTICS_MAX_LIMIT( "keyAnalyticsMaxLimit", 50000, Integer.class ),
    CUSTOM_LOGIN_PAGE_LOGO( "keyCustomLoginPageLogo", Boolean.FALSE, Boolean.class ),
    CUSTOM_TOP_MENU_LOGO( "keyCustomTopMenuLogo", Boolean.FALSE, Boolean.class ),
    ANALYTICS_MAINTENANCE_MODE( "keyAnalyticsMaintenanceMode", Boolean.FALSE, Boolean.class ),
    DATABASE_SERVER_CPUS( "keyDatabaseServerCpus", 0, Integer.class ),
    LAST_SUCCESSFUL_ANALYTICS_TABLES_RUNTIME( "keyLastSuccessfulAnalyticsTablesRuntime" ),
    LAST_MONITORING_RUN( "keyLastMonitoringRun", Date.class ),
    LAST_SUCCESSFUL_DATA_SYNC( "keyLastSuccessfulDataSynch", Date.class ),
    LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE( "keyLastSuccessfulAnalyticsTablesUpdate", Date.class ),
    LAST_SUCCESSFUL_RESOURCE_TABLES_UPDATE( "keyLastSuccessfulResourceTablesUpdate", Date.class ),
    LAST_SUCCESSFUL_MONITORING( "keyLastSuccessfulMonitoring", Date.class ),
    LAST_SUCCESSFUL_SMS_SCHEDULING( "lastSuccessfulSmsScheduling", Date.class ),
    LAST_SUCCESSFUL_DATA_STATISTIC( "lastSuccessfulDataStatistics", Date.class ),
    HELP_PAGE_LINK( "helpPageLink", "../dhis-web-commons-about/help.action", String.class ),
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
    REMOTE_INSTANCE_PASSWORD( "keyRemoteInstancePassword", "", String.class, true );
    
    private final String name;
    
    private final Serializable defaultValue;
    
    private final Class<?> clazz;

    private boolean confidential;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    private SettingKey( String name )
    {
        this.name = name;
        this.defaultValue = null;
        this.clazz = String.class;
        this.confidential = false;
    }
    
    private SettingKey( String name, Class<?> clazz )
    {
        this.name = name;
        this.defaultValue = null;
        this.clazz = clazz;
        this.confidential = false;
    }

    private SettingKey( String name, Serializable defaultValue, Class<?> clazz )
    {
        this.name = name;
        this.defaultValue = defaultValue;
        this.clazz = clazz;
        this.confidential = false;
    }

    private SettingKey( String name, Serializable defaultValue, Class<?> clazz, boolean confidential )
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
            
            //TODO handle Dates
        }
        
        return value;
    }
    
    public boolean hasDefaultValue()
    {
        return defaultValue != null;
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
