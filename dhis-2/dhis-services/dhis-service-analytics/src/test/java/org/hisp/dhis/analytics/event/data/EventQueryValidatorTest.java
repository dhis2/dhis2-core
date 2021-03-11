/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class EventQueryValidatorTest
    extends DhisSpringTest
{
    private Program prA;

    private Program prB;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataElement deD;

    private DataElement deE;

    private TrackedEntityAttribute atA;

    private TrackedEntityAttribute atB;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private LegendSet lsA;

    private OptionSet osA;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private QueryValidator aggregateQueryValidator;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private EventQueryValidator queryValidator;

    @Override
    public void setUpTest()
    {
        queryValidator = new DefaultEventQueryValidator( aggregateQueryValidator, systemSettingManager );

        prA = createProgram( 'A' );
        prB = createProgram( 'B' );

        idObjectManager.save( prA );
        idObjectManager.save( prB );

        deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.TRACKER );
        deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.SUM, DataElementDomain.TRACKER );
        deC = createDataElement( 'C', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT,
            DataElementDomain.TRACKER );
        deD = createDataElement( 'D', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT,
            DataElementDomain.TRACKER );
        deE = createDataElement( 'E', ValueType.COORDINATE, AggregationType.NONE, DataElementDomain.TRACKER );

        idObjectManager.save( deA );
        idObjectManager.save( deB );
        idObjectManager.save( deC );
        idObjectManager.save( deD );
        idObjectManager.save( deE );

        atA = createTrackedEntityAttribute( 'A' );
        atB = createTrackedEntityAttribute( 'B' );

        idObjectManager.save( atA );
        idObjectManager.save( atB );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        ouC = createOrganisationUnit( 'C', ouA );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );

        lsA = createLegendSet( 'A' );

        idObjectManager.save( lsA );

        osA = new OptionSet( "OptionSetA", ValueType.TEXT );

        idObjectManager.save( osA );
    }

    @Test
    public void validateSuccesA()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) ).build();

        queryValidator.validate( params );
    }

    @Test
    public void validateValidTimeField()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) )
            .withTimeField( TimeField.INCIDENT_DATE.name() ).build();

        queryValidator.validate( params );
    }

    @Test
    public void validateSingleDataElementMultipleProgramsQueryItemSuccess()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) )
            .addItem( new QueryItem( deA, prA, null, ValueType.TEXT, AggregationType.NONE, null ) )
            .addItem( new QueryItem( deA, prB, null, ValueType.TEXT, AggregationType.NONE, null ) )
            .build();

        queryValidator.validate( params );
    }

    @Test
    public void validateDuplicateQueryItems()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) )
            .addItem( new QueryItem( deA, prA, null, ValueType.TEXT, AggregationType.NONE, null ) )
            .addItem( new QueryItem( deA, prA, null, ValueType.TEXT, AggregationType.NONE, null ) )
            .build();

        ErrorMessage error = queryValidator.validateForErrorMessage( params );

        assertEquals( ErrorCode.E7202, error.getErrorCode() );
    }

    @Test
    public void validateFailureNoStartEndDatePeriods()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withOrganisationUnits( Lists.newArrayList( ouB ) ).build();

        assertValidatonError( ErrorCode.E7205, params );
    }

    @Test
    public void validateErrorNoStartEndDatePeriods()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withOrganisationUnits( Lists.newArrayList( ouB ) ).build();

        ErrorMessage error = queryValidator.validateForErrorMessage( params );

        assertEquals( ErrorCode.E7205, error.getErrorCode() );
    }

    @Test
    public void validateInvalidQueryItemBothLegendSetAndOptionSet()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouB ) )
            .addItem( new QueryItem( deA, lsA, ValueType.TEXT, AggregationType.NONE, osA ) ).build();

        assertValidatonError( ErrorCode.E7215, params );
    }

    @Test
    public void validateInvalidTimeField()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) )
            .withTimeField( "notAUidOrTimeField" ).build();

        assertValidatonError( ErrorCode.E7210, params );
    }

    @Test
    public void validateInvalidOrgUnitField()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouA ) )
            .withOrgUnitField( "notAUid" ).build();

        assertValidatonError( ErrorCode.E7211, params );
    }

    @Test
    public void validateErrorPage()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouB ) )
            .withPage( -2 ).build();

        ErrorMessage error = queryValidator.validateForErrorMessage( params );

        assertEquals( ErrorCode.E7207, error.getErrorCode() );
    }

    @Test
    public void validateErrorPageSize()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouB ) )
            .withPageSize( -1 ).build();

        ErrorMessage error = queryValidator.validateForErrorMessage( params );

        assertEquals( ErrorCode.E7208, error.getErrorCode() );
    }

    @Test
    public void validateErrorMaxLimit()
    {
        when( systemSettingManager.getSystemSetting( SettingKey.ANALYTICS_MAX_LIMIT ) )
            .thenReturn( 100 );

        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouB ) )
            .withLimit( 200 ).build();

        ErrorMessage error = queryValidator.validateForErrorMessage( params );

        assertEquals( ErrorCode.E7209, error.getErrorCode() );
    }

    @Test
    public void validateErrorClusterSize()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .withStartDate( new DateTime( 2010, 6, 1, 0, 0 ).toDate() )
            .withEndDate( new DateTime( 2012, 3, 20, 0, 0 ).toDate() )
            .withOrganisationUnits( Lists.newArrayList( ouB ) )
            .withCoordinateField( deE.getUid() )
            .withClusterSize( -3L ).build();

        ErrorMessage error = queryValidator.validateForErrorMessage( params );

        assertEquals( ErrorCode.E7212, error.getErrorCode() );
    }

    /**
     * Asserts whether the given error code is thrown by the query validator for
     * the given query.
     *
     * @param errorCode the {@link ErrorCode}.
     * @param params the {@link DataQueryParams}.
     */
    private void assertValidatonError( final ErrorCode errorCode, final EventQueryParams params )
    {
        ThrowingRunnable runnable = () -> queryValidator.validate( params );
        IllegalQueryException ex = assertThrows( "Error code mismatch", IllegalQueryException.class, runnable );
        assertEquals( errorCode, ex.getErrorCode() );
    }
}
