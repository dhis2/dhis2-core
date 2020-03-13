package org.hisp.dhis.dxf2.datavalueset;

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.User;

public class DataValueSetExportAccessControlTest
    extends DhisTest
{
    private DataElement deA;
    private DataElement deB;
    private DataElement deC;

    private CategoryCombo ccA;

    private CategoryOptionCombo cocA;
    private CategoryOptionCombo cocB;

    private Attribute atA;

    private AttributeValue avA;
    private AttributeValue avB;
    private AttributeValue avC;
    private AttributeValue avD;

    private DataSet dsA;
    private DataSet dsB;

    private Period peA;
    private Period peB;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;

    private User user;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

}
