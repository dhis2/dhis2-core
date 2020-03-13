package org.hisp.dhis.dxf2.datavalueset;

import static org.hisp.dhis.security.acl.AccessStringHelper.DATA_READ;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.system.util.JacksonUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DataValueSetExportAccessControlTest
    extends DhisTest
{
    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private UserService _userService;

    private DataElement deA;
    private DataElement deB;

    private CategoryOption coA;
    private CategoryOption coB;
    private CategoryOption coC;
    private CategoryOption coD;

    private Category caA;
    private Category caB;

    private CategoryCombo ccA;

    private CategoryOptionCombo cocA;
    private CategoryOptionCombo cocB;
    private CategoryOptionCombo cocC;
    private CategoryOptionCombo cocD;

    private DataSet dsA;

    private Period peA;

    private OrganisationUnit ouA;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void setUpTest()
    {
        userService = _userService;

        // Metadata

        PeriodType ptA = periodService.getPeriodTypeByName( MonthlyPeriodType.NAME );

        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        idObjectManager.save( Lists.newArrayList( deA, deB ) );

        coA = createCategoryOption( 'A' );
        coB = createCategoryOption( 'B' );
        coC = createCategoryOption( 'C' );
        coD = createCategoryOption( 'D' );
        idObjectManager.save( Lists.newArrayList( coA, coB, coC, coD ) );

        caA = createCategory( 'A', coA, coB );
        caB = createCategory( 'B', coC, coD );
        idObjectManager.save( Lists.newArrayList( caA, caB ) );

        ccA = createCategoryCombo( 'A', caA, caB );
        idObjectManager.save( ccA );

        cocA = createCategoryOptionCombo( ccA, coA, coC );
        cocB = createCategoryOptionCombo( ccA, coA, coD );
        cocC = createCategoryOptionCombo( ccA, coB, coC );
        cocD = createCategoryOptionCombo( ccA, coB, coD );
        idObjectManager.save( Lists.newArrayList( cocA, cocB, cocC, cocD ) );

        dsA = createDataSet( 'A', ptA, ccA );
        dsA.addDataSetElement( deA );
        dsA.addDataSetElement( deB );
        idObjectManager.save( dsA );

        peA = createPeriod( "201901" );
        idObjectManager.save( peA );

        ouA = createOrganisationUnit( 'A' );
        idObjectManager.save( ouA );

        // Data values

        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocA, cocA, "1" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocA, cocB, "2" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocA, cocC, "3" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocA, cocD, "4" ) );

        // User

        User user = createUser( 'A' );
        user.setOrganisationUnits( Sets.newHashSet( ouA ) );
        userService.addUser( user );
        CurrentUserService currentUserService = new MockCurrentUserService( user );
        setDependency( dataValueSetService, "currentUserService", currentUserService );
        setDependency( organisationUnitService, "currentUserService", currentUserService );

        // Sharing

        enableDataSharing( user, coA, DATA_READ );
        enableDataSharing( user, coC, DATA_READ );
        enableDataSharing( user, coD, DATA_READ );
        enableDataSharing( user, dsA, DATA_READ );

        idObjectManager.update( coA );
        idObjectManager.update( coC );
        idObjectManager.update( coD );
        idObjectManager.update( dsA );
    }

    @Test
    public void testExportAttributeOptionComboAccess()
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setPeriods( Sets.newHashSet( peA ) )
            .setOrganisationUnits( Sets.newHashSet( ouA ) );

        dataValueSetService.writeDataValueSetJson( params, out );

        DataValueSet dvs = JacksonUtils.fromJson( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertNotNull( dvs.getDataSet() );
    }
}
