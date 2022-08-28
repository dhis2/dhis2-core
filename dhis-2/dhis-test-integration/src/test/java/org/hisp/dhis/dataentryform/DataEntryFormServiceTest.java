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
package org.hisp.dhis.dataentryform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Bharath
 */
class DataEntryFormServiceTest extends NonTransactionalIntegrationTest
{

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataEntryFormService dataEntryFormService;

    @Autowired
    private DataElementService dataElementService;

    private PeriodType periodType;

    private DataElement dataElement;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
        throws Exception
    {
        periodType = new MonthlyPeriodType();
        dataElement = createDataElement( 'A' );
        dataElementService.addDataElement( dataElement );
    }

    // -------------------------------------------------------------------------
    // DataEntryForm
    // -------------------------------------------------------------------------
    @Test
    void testAddDataEntryForm()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        dataSetService.addDataSet( dataSetA );
        DataEntryForm dataEntryFormA = createDataEntryForm( 'A' );
        long dataEntryFormAid = dataEntryFormService.addDataEntryForm( dataEntryFormA );
        dataEntryFormA = dataEntryFormService.getDataEntryForm( dataEntryFormAid );
        assertEquals( dataEntryFormAid, dataEntryFormA.getId() );
        assertEquals( "DataEntryFormA", dataEntryFormA.getName() );
    }

    @Test
    void testUpdateDataEntryForm()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        dataSetService.addDataSet( dataSetA );
        DataEntryForm dataEntryForm = createDataEntryForm( 'A' );
        long id = dataEntryFormService.addDataEntryForm( dataEntryForm );
        dataEntryForm = dataEntryFormService.getDataEntryForm( id );
        assertEquals( "DataEntryFormA", dataEntryForm.getName() );
        dataEntryForm.setName( "DataEntryFormX" );
        dataEntryFormService.updateDataEntryForm( dataEntryForm );
        dataEntryForm = dataEntryFormService.getDataEntryForm( id );
        assertEquals( dataEntryForm.getName(), "DataEntryFormX" );
    }

    @Test
    void testDeleteAndGetDataEntryForm()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        dataSetService.addDataSet( dataSetA );
        DataEntryForm dataEntryForm = createDataEntryForm( 'A' );
        long id = dataEntryFormService.addDataEntryForm( dataEntryForm );
        dataEntryForm = dataEntryFormService.getDataEntryForm( id );
        assertNotNull( dataEntryFormService.getDataEntryForm( id ) );
        dataEntryFormService.deleteDataEntryForm( dataEntryFormService.getDataEntryForm( id ) );
        assertNull( dataEntryFormService.getDataEntryForm( id ) );
    }

    @Test
    void testGetDataEntryFormByName()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        dataSetService.addDataSet( dataSetA );
        DataEntryForm dataEntryForm = createDataEntryForm( 'A' );
        long id = dataEntryFormService.addDataEntryForm( dataEntryForm );
        dataEntryForm = dataEntryFormService.getDataEntryForm( id );
        assertEquals( dataEntryFormService.getDataEntryFormByName( "DataEntryFormA" ), dataEntryForm );
        assertNull( dataEntryFormService.getDataEntryFormByName( "DataEntryFormX" ) );
    }

    @Test
    void testGetAllDataEntryForms()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );
        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );
        DataEntryForm dataEntryFormA = createDataEntryForm( 'A' );
        DataEntryForm dataEntryFormB = createDataEntryForm( 'B' );
        dataEntryFormService.addDataEntryForm( dataEntryFormA );
        dataEntryFormService.addDataEntryForm( dataEntryFormB );
        List<DataEntryForm> dataEntryForms = dataEntryFormService.getAllDataEntryForms();
        assertEquals( dataEntryForms.size(), 2 );
        assertTrue( dataEntryForms.contains( dataEntryFormA ) );
        assertTrue( dataEntryForms.contains( dataEntryFormB ) );
    }

    @Test
    void testPrepareForSave()
    {
        String html = "<table><tr><td><input id=\"1434-11-val\" style=\"width:4em;text-align:center\" title=\"[ 1434 - Expected Births - 11 - (default) - int ]\" value=\"[ Expected Births - (default) ]\" /></td></tr></table>";
        String expected = "<table><tr><td><input id=\"1434-11-val\" style=\"width:4em;text-align:center\" title=\"\" value=\"\" /></td></tr></table>";
        String actual = dataEntryFormService.prepareDataEntryFormForSave( html );
        assertEquals( expected, actual );
    }
}
