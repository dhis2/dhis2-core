package org.hisp.dhis.dxf2.dataset;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

public class MetaDataCaches
{
    private CachingMap<String, DataSet> dataSets = new CachingMap<>();

    private CachingMap<String, OrganisationUnit> orgUnits = new CachingMap<>();

    private CachingMap<String, Period> periods = new CachingMap<>();

    private CachingMap<String, CategoryOptionCombo> attrOptionCombos = new CachingMap<>();

    private CachingMap<String, Boolean> orgUnitInHierarchyMap = new CachingMap<>();

    private CachingMap<String, Boolean> attrOptComboOrgUnitMap = new CachingMap<>();

    public void preheat( IdentifiableObjectManager manager,
        final ImportConfig config )
    {
        dataSets.load( manager.getAll( DataSet.class ), ds -> ds.getPropertyValue( config.getDsScheme() ) );
        orgUnits.load( manager.getAll( OrganisationUnit.class ), ou -> ou.getPropertyValue( config.getOuScheme() ) );
        attrOptionCombos.load( manager.getAll( CategoryOptionCombo.class ),
            oc -> oc.getPropertyValue( config.getAocScheme() ) );
    }

    public CachingMap<String, DataSet> getDataSets()
    {
        return dataSets;
    }

    public CachingMap<String, OrganisationUnit> getOrgUnits()
    {
        return orgUnits;
    }

    public CachingMap<String, Period> getPeriods()
    {
        return periods;
    }

    public CachingMap<String, CategoryOptionCombo> getAttrOptionCombos()
    {
        return attrOptionCombos;
    }

    public CachingMap<String, Boolean> getOrgUnitInHierarchyMap()
    {
        return orgUnitInHierarchyMap;
    }

    public CachingMap<String, Boolean> getAttrOptComboOrgUnitMap()
    {
        return attrOptComboOrgUnitMap;
    }
}
