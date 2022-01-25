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
package org.hisp.dhis.dxf2.pdfform;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialJulyPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.SixMonthlyAprilPeriodType;
import org.hisp.dhis.period.SixMonthlyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.util.DateUtils;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

/**
 * @author viet@dhis2.org
 */
public class DataSetPdfDocument extends PdfDocument<DataSet>
{
    public DataSetPdfDocument( Document document, PdfDataEntrySettings settings )
    {
        super( document, settings );
    }

    @Override
    public void write( PdfWriter writer, DataSet dataSet )
        throws DocumentException,
        IOException
    {
        setTitle( dataSet );

        document.add( Chunk.NEWLINE );

        createInputFields( writer, dataSet );

        // insertTable_TextRow( writer, mainTable, TEXT_BLANK );
        //
        // insertTable_DataSet( mainTable, writer, dataSet );

        mainTable.addToDocument( document );

        document.add( Chunk.NEWLINE );
        document.add( Chunk.NEWLINE );

        // insertSaveAsButton( document, writer,
        // PdfDataEntryFormUtil.LABELCODE_BUTTON_SAVEAS,
        // dataSet.getDisplayName() );
    }

    protected void setTitle( DataSet dataSet )
        throws DocumentException
    {
        document
            .add( new Paragraph( dataSet.getDisplayName(), settings.getFont( PdfFormFontSettings.FONTTYPE_TITLE ) ) );

        document.add( new Paragraph( dataSet.getDisplayDescription(),
            settings.getFont( PdfFormFontSettings.FONTTYPE_DESCRIPTION ) ) );
    }

    private void createInputFields( PdfWriter writer, DataSet dataSet )
        throws IOException,
        DocumentException
    {
        createOrgUnitAndPeriodInputs( writer, dataSet );
        createAttributeSelectionFields( writer, dataSet );
    }

    private void createOrgUnitAndPeriodInputs( PdfWriter writer, DataSet dataSet )
        throws DocumentException,
        IOException
    {
        new PdfTable( 2, Element.ALIGN_LEFT, new int[] { 1, 3 } )
            .addInputField( createOrgUnitInputField( writer ) )
            .addInputField( createPeriodSelectionField( writer, dataSet ) )
            .addToTable( mainTable );
    }

    private PdfSelectField createPeriodSelectionField( PdfWriter writer, DataSet dataSet )
    {
        List<Period> periods = getPeriods( dataSet.getPeriodType() );

        String[] periodsTitle = getPeriodTitles( periods, settings.getFormat() );
        String[] periodsValue = getPeriodValues( periods );

        return PdfSelectField.builder()
            .label( "Period" )
            .fieldName( PdfDataEntryFormUtil.LABELCODE_PERIODID )
            .font( settings.getFont( PdfFormFontSettings.FONTTYPE_BODY ) )
            .cellType( PdfCellType.DEFAULT )
            .optionValues( periodsValue )
            .optionLabels( periodsTitle )
            .writer( writer )
            .build();
    }

    private PdfTextInputField createOrgUnitInputField( PdfWriter writer )
    {
        return PdfTextInputField.builder()
            .label( "Organization unit identifier" )
            .fieldName( PdfDataEntryFormUtil.LABELCODE_ORGID )
            .writer( writer )
            .cellType( PdfCellType.ORGANISATION_UNIT )
            .font( settings.getFont( PdfFormFontSettings.FONTTYPE_BODY ) )
            .build();
    }

    // TODO need to check currentUser
    private void createAttributeSelectionFields( PdfWriter writer, DataSet dataset )
        throws IOException,
        DocumentException
    {
        PdfTable table = new PdfTable( 2, Element.ALIGN_LEFT, new int[] { 1, 3 } );
        CategoryCombo catCombo = dataset.getCategoryCombo();

        for ( Category category : catCombo.getCategories() )
        {
            if ( category.isDefault() )
                continue;

            String[] optionLabels = category.getCategoryOptions().stream()
                .map( option -> option.getDisplayName() )
                .toArray( String[]::new );

            String[] optionValues = category.getCategoryOptions().stream()
                .map( option -> option.getUid() )
                .toArray( String[]::new );

            table.addInputField( PdfSelectField.builder()
                .label( category.getDisplayName() )
                .fieldName( PdfDataEntryFormUtil.LABELCODE_ATTRIBUTE_OPTIONID + "_" + catCombo.getUid() + "_" +
                    category.getUid() )
                .font( settings.getFont( PdfFormFontSettings.FONTTYPE_BODY ) )
                .cellType( PdfCellType.DEFAULT )
                .optionValues( optionValues )
                .optionLabels( optionLabels )
                .writer( writer )
                .build() );
        }

        table.addToTable( mainTable );
    }

    private String[] getPeriodValues( List<Period> periods )
    {
        String[] periodValues = new String[periods.size()];

        for ( int i = 0; i < periods.size(); i++ )
        {
            periodValues[i] = periods.get( i ).getIsoDate();
        }

        return periodValues;
    }

    private String[] getPeriodTitles( List<Period> periods, I18nFormat format )
    {
        String[] periodTitles = new String[periods.size()];

        for ( int i = 0; i < periods.size(); i++ )
        {
            Period period = periods.get( i );
            periodTitles[i] = format.formatPeriod( period );

            periodTitles[i] += " ( " + DateUtils.getMediumDateString( period.getStartDate() )
                + " - " + DateUtils.getMediumDateString( period.getEndDate() ) + " )";
        }

        return periodTitles;
    }

    private List<Period> getPeriods( PeriodType periodType )
    {
        Period period = setPeriodDateRange( periodType );

        return ((CalendarPeriodType) periodType).generatePeriods( period.getStartDate(), period.getEndDate() );
    }

    private Period setPeriodDateRange( PeriodType periodType )
    {
        Period period = new Period();

        Calendar currentDate = Calendar.getInstance();

        int currYear = currentDate.get( Calendar.YEAR );
        int startYear = currYear - PdfDataEntrySettings.PERIODRANGE_PREVYEARS;
        int endYear = currYear + PdfDataEntrySettings.PERIODRANGE_FUTUREYEARS;

        if ( periodType.getName().equals( QuarterlyPeriodType.NAME )
            || periodType.getName().equals( SixMonthlyPeriodType.NAME )
            || periodType.getName().equals( SixMonthlyAprilPeriodType.NAME )
            || periodType.getName().equals( YearlyPeriodType.NAME )
            || periodType.getName().equals( FinancialAprilPeriodType.NAME )
            || periodType.getName().equals( FinancialJulyPeriodType.NAME )
            || periodType.getName().equals( FinancialOctoberPeriodType.NAME ) )
        {
            startYear = currYear - PdfDataEntrySettings.PERIODRANGE_PREVYEARS_YEARLY;
            endYear = currYear + PdfDataEntrySettings.PERIODRANGE_FUTUREYEARS_YEARLY;
        }

        period.setStartDate( DateUtils.getMediumDate( String.valueOf( startYear ) + "-01-01" ) );
        period.setEndDate( DateUtils.getMediumDate( String.valueOf( endYear ) + "-01-01" ) );

        return period;
    }

}
