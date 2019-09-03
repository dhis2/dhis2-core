package org.hisp.dhis.dxf2.dataset;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.system.callable.CategoryOptionComboCallable;
import org.hisp.dhis.system.callable.IdentifiableObjectCallable;
import org.hisp.dhis.system.callable.PeriodCallable;

class MetaDataCallables
{
    private final IdentifiableObjectCallable<DataSet> dataSetCallable;

    private final IdentifiableObjectCallable<OrganisationUnit> orgUnitCallable;

    private final IdentifiableObjectCallable<CategoryOptionCombo> optionComboCallable;

    private final IdentifiableObjectCallable<Period> periodCallable;

    MetaDataCallables( ImportConfig config, IdentifiableObjectManager idObjManager, PeriodService periodService,
        CategoryService categoryService )
    {
        dataSetCallable = new IdentifiableObjectCallable<>( idObjManager, DataSet.class, config.getDsScheme(), null );
        orgUnitCallable = new IdentifiableObjectCallable<>( idObjManager, OrganisationUnit.class, config.getOuScheme(),
            null );
        optionComboCallable = new CategoryOptionComboCallable( categoryService, config.getAocScheme(), null );
        periodCallable = new PeriodCallable( periodService, null, null );
    }

    IdentifiableObjectCallable<DataSet> getDataSetCallable()
    {
        return dataSetCallable;
    }

    IdentifiableObjectCallable<OrganisationUnit> getOrgUnitCallable()
    {
        return orgUnitCallable;
    }

    IdentifiableObjectCallable<CategoryOptionCombo> getOptionComboCallable()
    {
        return optionComboCallable;
    }

    IdentifiableObjectCallable<Period> getPeriodCallable()
    {
        return periodCallable;
    }
}
