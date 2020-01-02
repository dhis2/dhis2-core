package org.hisp.dhis.dxf2.dataset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;

class ImportConfig
{

    private IdScheme dsScheme;

    private IdScheme ouScheme;

    private IdScheme aocScheme;

    private ImportStrategy strategy;

    private boolean dryRun;

    private boolean skipExistingCheck;

    private boolean strictPeriods;

    private boolean strictAttrOptionCombos;

    private boolean strictOrgUnits;

    private boolean requireAttrOptionCombos;

    private boolean skipNotifications;

    private CategoryOptionCombo fallbackCatOptCombo;

    private static final Log log = LogFactory.getLog( ImportConfig.class );

    ImportConfig(SystemSettingManager systemSettingManager, CategoryService categoryService,
                 CompleteDataSetRegistrations cdsr, ImportOptions options)
    {
        dsScheme = IdScheme.from( cdsr.getDataSetIdSchemeProperty() );
        ouScheme = IdScheme.from( cdsr.getOrgUnitIdSchemeProperty() );
        aocScheme = IdScheme.from( cdsr.getAttributeOptionComboIdSchemeProperty() );

        log.info( String.format( "Data set scheme: %s, org unit scheme: %s, attribute option combo scheme: %s",
            dsScheme, ouScheme, aocScheme ) );

        strategy = cdsr.getStrategy() != null ? ImportStrategy.valueOf( cdsr.getStrategy() )
            : options.getImportStrategy();

        dryRun = cdsr.getDryRun() != null ? cdsr.getDryRun() : options.isDryRun();

        skipNotifications = options.isSkipNotifications();

        skipExistingCheck = options.isSkipExistingCheck();

        strictPeriods = options.isStrictPeriods()
            || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_PERIODS );

        strictAttrOptionCombos = options.isStrictAttributeOptionCombos()
            || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ATTRIBUTE_OPTION_COMBOS );

        strictOrgUnits = options.isStrictOrganisationUnits()
            || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS );

        requireAttrOptionCombos = options.isRequireAttributeOptionCombo()
            || (Boolean) systemSettingManager.getSystemSetting( SettingKey.DATA_IMPORT_REQUIRE_ATTRIBUTE_OPTION_COMBO );

        fallbackCatOptCombo = categoryService.getDefaultCategoryOptionCombo();
    }

    IdScheme getDsScheme()
    {
        return dsScheme;
    }

    IdScheme getOuScheme()
    {
        return ouScheme;
    }

    IdScheme getAocScheme()
    {
        return aocScheme;
    }

    ImportStrategy getStrategy()

    {
        return strategy;
    }

    boolean isDryRun()
    {
        return dryRun;
    }

    boolean isSkipExistingCheck()
    {
        return skipExistingCheck;
    }

    boolean isStrictPeriods()
    {
        return strictPeriods;
    }

    boolean isStrictAttrOptionCombos()
    {
        return strictAttrOptionCombos;
    }

    boolean isStrictOrgUnits()
    {
        return strictOrgUnits;
    }

    boolean isRequireAttrOptionCombos()
    {
        return requireAttrOptionCombos;
    }

    boolean isSkipNotifications()
    {
        return skipNotifications;
    }

    CategoryOptionCombo getFallbackCatOptCombo()
    {
        return fallbackCatOptCombo;
    }
}
