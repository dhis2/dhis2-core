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
package org.hisp.dhis.dxf2.pdf;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.pdfform.PdfDataEntryFormGenerator;
import org.hisp.dhis.dxf2.pdfform.PdfDataEntrySettings;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.lowagie.text.DocumentException;

/**
 * @author viet@dhis2.org
 */
public class PdfDataEntryFormGeneratorTest
    extends SingleSetupIntegrationTestBase
{
    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private PdfDataEntryFormGenerator formGenerator;

    @Autowired
    private PeriodService periodService;

    @Test
    void testExport()
        throws DocumentException,
        IOException
    {
        PeriodType periodType = periodService.getPeriodTypeByName( MonthlyPeriodType.NAME );
        DataElement dataElement = createDataElement( 'A' );
        manager.save( dataElement );
        DataSet dataSet = createDataSet( 'A', periodType );
        manager.save( dataSet );

        Section section = new Section( "SectionA", dataSet, Lists.newArrayList( dataElement ), Collections.emptySet() );
        manager.save( section );
        CategoryOption optionA = createCategoryOption( 'A' );
        manager.save( optionA );
        Category categoryA = createCategory( 'A', optionA );
        manager.save( categoryA );

        CategoryOption optionB = createCategoryOption( 'B' );
        manager.save( optionB );
        Category categoryB = createCategory( 'B', optionB );
        manager.save( categoryB );
        CategoryCombo categoryCombo = createCategoryCombo( 'A', categoryA, categoryB );
        manager.save( categoryCombo );

        CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo( categoryCombo, optionA, optionB );
        manager.save( categoryOptionCombo );

        dataSet.setCategoryCombo( categoryCombo );

        manager.save( dataSet );

        PdfDataEntrySettings settings = PdfDataEntrySettings.builder()
            .format( i18nManager.getI18nFormat() )
            .serverName( "test server" )
            .headerText( "header text" )
            .build();

        ByteArrayOutputStream baos = formGenerator.generateDataEntry( dataSet, settings );

        assertNotNull( baos );
    }
}
