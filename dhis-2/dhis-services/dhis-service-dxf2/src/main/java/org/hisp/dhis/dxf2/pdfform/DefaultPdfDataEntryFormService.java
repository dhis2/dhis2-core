package org.hisp.dhis.dxf2.pdfform;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.awt.Color;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialJulyPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.SixMonthlyAprilPeriodType;
import org.hisp.dhis.period.SixMonthlyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfAppearance;
import com.lowagie.text.pdf.PdfBorderDictionary;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfFormField;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RadioCheckField;
import com.lowagie.text.pdf.TextField;

/**
 * @author James Chang
 */
public class DefaultPdfDataEntryFormService
    implements PdfDataEntryFormService
{
    private static final Color COLOR_BACKGROUDTEXTBOX = Color.getHSBColor( 0.0f, 0.0f, 0.961f );

    private static final String TEXT_BLANK = " ";

    private static final int TEXTBOXWIDTH_NUMBERTYPE = 35;

    private static final int TEXTBOXWIDTH = 160;

    private static final int PERIODRANGE_PREVYEARS = 1;
    private static final int PERIODRANGE_FUTUREYEARS = 2;

    private static final int PERIODRANGE_PREVYEARS_YEARLY = 5;
    private static final int PERIODRANGE_FUTUREYEARS_YEARLY = 6;

    private static final Integer MAX_OPTIONS_DISPLAYED = 30;

    private static final Integer PROGRAM_FORM_ROW_NUMBER = 10;

    private PdfFormFontSettings pdfFormFontSettings;

    private I18nFormat format;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private OptionService optionService;

    // -------------------------------------------------------------------------
    // PdfDataEntryFormService implementation
    // -------------------------------------------------------------------------

    @Override
    public void generatePDFDataEntryForm( Document document, PdfWriter writer, String dataSetUid, int typeId,
        Rectangle pageSize, PdfFormFontSettings pdfFormFontSettings, I18nFormat format )
    {
        try
        {
            this.pdfFormFontSettings = pdfFormFontSettings;
            this.format = format;

            document.setPageSize( pageSize );

            document.open();

            if ( typeId == PdfDataEntryFormUtil.DATATYPE_DATASET )
            {
                setDataSet_DocumentContent( document, writer, dataSetUid );
            }
            else if ( typeId == PdfDataEntryFormUtil.DATATYPE_PROGRAMSTAGE )
            {
                setProgramStage_DocumentContent( document, writer, dataSetUid );
            }
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( ex );
        }
        finally
        {
            document.close();
        }
    }

    private void setDataSet_DocumentContent( Document document, PdfWriter writer, String dataSetUid )
        throws Exception
    {
        DataSet dataSet = dataSetService.getDataSet( dataSetUid );

        if ( dataSet == null )
        {
            throw new RuntimeException( "Error - DataSet not found for UID " + dataSetUid );
        }

        setDataSet_DocumentTopSection( document, dataSet );

        document.add( Chunk.NEWLINE );

        List<Period> periods = getPeriods_DataSet( dataSet.getPeriodType() );

        PdfPTable mainTable = new PdfPTable( 1 ); // Table with 1 cell.
        setMainTable( mainTable );

        insertTable_OrgAndPeriod( mainTable, writer, periods );

        insertTable_TextRow( writer, mainTable, TEXT_BLANK );

        insertTable_DataSet( mainTable, writer, dataSet );

        document.add( mainTable );


        document.add( Chunk.NEWLINE );
        document.add( Chunk.NEWLINE );

        insertSaveAsButton( document, writer, PdfDataEntryFormUtil.LABELCODE_BUTTON_SAVEAS, dataSet.getDisplayName() );
    }

    private void setDataSet_DocumentTopSection( Document document, DataSet dataSet )
        throws DocumentException
    {
        document.add( new Paragraph( dataSet.getDisplayName(), pdfFormFontSettings
            .getFont( PdfFormFontSettings.FONTTYPE_TITLE ) ) );

        document.add( new Paragraph( dataSet.getDisplayDescription(), pdfFormFontSettings
            .getFont( PdfFormFontSettings.FONTTYPE_DESCRIPTION ) ) );
    }

    private List<Period> getPeriods_DataSet( PeriodType periodType )
        throws ParseException
    {
        Period period = setPeriodDateRange( periodType );

        return ((CalendarPeriodType) periodType).generatePeriods( period.getStartDate(), period.getEndDate() );
    }

    private void setMainTable( PdfPTable mainTable )
    {
        mainTable.setWidthPercentage( 100.0f );
        mainTable.setHorizontalAlignment( Element.ALIGN_LEFT );
    }

    private void insertTable_DataSet( PdfPTable mainTable, PdfWriter writer, DataSet dataSet )
        throws IOException, DocumentException
    {
        Rectangle rectangle = new Rectangle( TEXTBOXWIDTH, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

        if ( dataSet.getSections().size() > 0 )
        {
            for ( Section section : dataSet.getSections() )
            {
                insertTable_DataSetSections( mainTable, writer, rectangle, section.getDataElements(), section.getDisplayName() );
            }
        }
        else
        {
            insertTable_DataSetSections( mainTable, writer, rectangle, dataSet.getDataElements(), "" );
        }
    }

    private void insertTable_DataSetSections( PdfPTable mainTable, PdfWriter writer, Rectangle rectangle,
        Collection<DataElement> dataElements, String sectionName )
        throws IOException, DocumentException
    {
        boolean hasBorder = true;

        // Add Section Name and Section Spacing
        insertTable_TextRow( writer, mainTable, TEXT_BLANK );

        if ( sectionName != null && !sectionName.isEmpty() )
        {
            insertTable_TextRow( writer, mainTable, sectionName,
                pdfFormFontSettings.getFont( PdfFormFontSettings.FONTTYPE_SECTIONHEADER ) );
        }

        // Create A Table To Add For Each Section
        PdfPTable table = new PdfPTable( 2 );

        table.setWidths( new int[]{ 2, 1 } );
        table.setWidthPercentage( 100.0f );
        table.setHorizontalAlignment( Element.ALIGN_LEFT );


        // For each DataElement and Category Combo of the dataElement, create
        // row.
        for ( DataElement dataElement : dataElements )
        {
            for ( DataElementCategoryOptionCombo categoryOptionCombo : dataElement.getSortedCategoryOptionCombos() )
            {
                String categoryOptionComboDisplayName = "";

                // Hide Default category option combo name
                if ( !categoryOptionCombo.isDefault() )
                {
                    categoryOptionComboDisplayName = categoryOptionCombo.getDisplayName();
                }

                addCell_Text( table, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), dataElement.getFormNameFallback() + " " +
                    categoryOptionComboDisplayName, Element.ALIGN_RIGHT );

                String strFieldLabel = PdfDataEntryFormUtil.LABELCODE_DATAENTRYTEXTFIELD + dataElement.getUid() + "_"
                    + categoryOptionCombo.getUid();

                ValueType valueType = dataElement.getValueType();

                // Yes Only case - render as check-box
                if ( ValueType.TRUE_ONLY == valueType )
                {
                    addCell_WithCheckBox( table, writer, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), strFieldLabel );
                }
                else if ( ValueType.BOOLEAN == valueType )
                {
                    // Create Yes - true, No - false, Select..
                    String[] optionList = new String[]{ "[No Value]", "Yes", "No" };
                    String[] valueList = new String[]{ "", "true", "false" };

                    // addCell_WithRadioButton(table, writer, strFieldLabel);
                    addCell_WithDropDownListField( table, rectangle, writer, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), strFieldLabel, optionList, valueList );
                }
                else if ( valueType.isNumeric() )
                {
                    Rectangle rectNum = new Rectangle( TEXTBOXWIDTH_NUMBERTYPE, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

                    addCell_WithTextField( table, rectNum, writer, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), strFieldLabel, PdfFieldCell.TYPE_TEXT_NUMBER );
                }
                else
                {
                    addCell_WithTextField( table, rectangle, writer, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), strFieldLabel );
                }
            }
        }

        PdfPCell cell_withInnerTable = new PdfPCell( table );
        cell_withInnerTable.setBorder( Rectangle.NO_BORDER );

        mainTable.addCell( cell_withInnerTable );
    }

    private void setProgramStage_DocumentContent( Document document, PdfWriter writer, String programStageUid )
        throws Exception
    {
        ProgramStage programStage = programStageService.getProgramStage( programStageUid );

        if ( programStage == null )
        {
            throw new RuntimeException( "Error - ProgramStage not found for UID " + programStageUid );
        }
        else
        {
            // Get Rectangle with TextBox Width to be used
            Rectangle rectangle = new Rectangle( 0, 0, TEXTBOXWIDTH, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

            // Create Main Layout table and set the properties
            PdfPTable mainTable = getProgramStageMainTable();

            // Generate Period List for ProgramStage
            List<Period> periods = getProgramStagePeriodList();

            // Add Org Unit, Period, Hidden ProgramStageID Field
            insertTable_OrgAndPeriod( mainTable, writer, periods );

            insertTable_TextRow( writer, mainTable, TEXT_BLANK );

            // Add ProgramStage Field - programStage.getId();
            insertTable_HiddenValue( mainTable, rectangle, writer,
                PdfDataEntryFormUtil.LABELCODE_PROGRAMSTAGEIDTEXTBOX, String.valueOf( programStage.getId() ) );

            // Add ProgramStage Content to PDF - [The Main Section]
            insertTable_ProgramStage( mainTable, writer, programStage );

            // Add the mainTable to document
            document.add( mainTable );
        }
    }

    private void insertTable_ProgramStage( PdfPTable mainTable, PdfWriter writer, ProgramStage programStage )
        throws IOException, DocumentException
    {
        Rectangle rectangle = new Rectangle( TEXTBOXWIDTH, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

        // Add Program Stage Sections
        if ( programStage.getProgramStageSections().size() > 0 )
        {
            // Sectioned Ones
            for ( ProgramStageSection section : programStage.getProgramStageSections() )
            {
                insertTable_ProgramStageSections( mainTable, rectangle, writer, section.getDataElements() );
            }
        }
        else
        {
            // Default one
            insertTable_ProgramStageSections( mainTable, rectangle, writer, programStage.getAllDataElements() );
        }
    }

    private void insertTable_ProgramStageSections( PdfPTable mainTable, Rectangle rectangle, PdfWriter writer,
        Collection<DataElement> dataElements )
        throws IOException, DocumentException
    {
        boolean hasBorder = false;

        // Add one to column count due to date entry + one hidden height set
        // field.
        int colCount = dataElements.size() + 1 + 1;

        PdfPTable table = new PdfPTable( colCount ); // Code 1

        float totalWidth = 800f;
        float firstCellWidth_dateEntry = PdfDataEntryFormUtil.UNITSIZE_DEFAULT * 3;
        float lastCellWidth_hidden = PdfDataEntryFormUtil.UNITSIZE_DEFAULT;
        float dataElementCellWidth = (totalWidth - firstCellWidth_dateEntry - lastCellWidth_hidden)
            / dataElements.size();

        // Create 2 types of Rectangles, one for Date field, one for data
        // elements - to be used when rendering them.
        Rectangle rectangleDate = new Rectangle( 0, 0, PdfDataEntryFormUtil.UNITSIZE_DEFAULT * 2,
            PdfDataEntryFormUtil.UNITSIZE_DEFAULT );
        Rectangle rectangleDataElement = new Rectangle( 0, 0, dataElementCellWidth,
            PdfDataEntryFormUtil.UNITSIZE_DEFAULT );

        // Cell Width Set
        float[] cellWidths = new float[colCount];

        // Date Field Settings
        cellWidths[0] = firstCellWidth_dateEntry;

        for ( int i = 1; i < colCount - 1; i++ )
        {
            cellWidths[i] = dataElementCellWidth;
        }

        cellWidths[colCount - 1] = lastCellWidth_hidden;

        table.setWidths( cellWidths );

        // Create Header
        addCell_Text( table, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), "Date", Element.ALIGN_CENTER );

        // Add Program Data Elements Columns
        for ( DataElement dataElement : dataElements )
        {
            addCell_Text( table, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), dataElement.getFormNameFallback(), Element.ALIGN_CENTER );
        }

        addCell_Text( table, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), TEXT_BLANK, Element.ALIGN_CENTER );

        // ADD A HIDDEN INFO FOR ProgramStageID
        // Print rows, having the data elements repeating on each column.

        for ( int rowNo = 1; rowNo <= PROGRAM_FORM_ROW_NUMBER; rowNo++ )
        {
            // Add Date Column
            String strFieldDateLabel = PdfDataEntryFormUtil.LABELCODE_DATADATETEXTFIELD + Integer.toString( rowNo );

            addCell_WithTextField( table, rectangleDate, writer, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), strFieldDateLabel );

            // Add Program Data Elements Columns
            for ( DataElement dataElement : dataElements )
            {
                OptionSet optionSet = dataElement.getOptionSet();

                String strFieldLabel = PdfDataEntryFormUtil.LABELCODE_DATAENTRYTEXTFIELD
                    + Integer.toString( dataElement.getId() )
                    // + "_" + Integer.toString(programStageId) + "_" +
                    // Integer.toString(rowNo);
                    + "_" + Integer.toString( rowNo );

                if ( optionSet != null )
                {
                    String query = ""; // Get All Option

                    // TODO: This gets repeated <- Create an array of the
                    // options. and apply only once.
                    List<Option> options = optionService.getOptions( optionSet.getId(), query, MAX_OPTIONS_DISPLAYED );

                    addCell_WithDropDownListField( table, rectangleDataElement, writer, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), strFieldLabel, options.toArray( new String[0] ),
                        options.toArray( new String[0] ) );
                }
                else
                {
                    // NOTE: When Rendering for DataSet, DataElement's OptionSet
                    // does not get rendered.
                    // Only for events, it gets rendered as dropdown list.
                    addCell_WithTextField( table, rectangleDataElement, writer, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), strFieldLabel );
                }
            }

            addCell_Text( table, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), TEXT_BLANK, Element.ALIGN_LEFT );
        }

        PdfPCell cell_withInnerTable = new PdfPCell( table );
        cell_withInnerTable.setBorder( Rectangle.NO_BORDER );

        mainTable.addCell( cell_withInnerTable );

    }

    private List<Period> getProgramStagePeriodList()
        throws ParseException
    {
        PeriodType periodType = PeriodType.getPeriodTypeByName( MonthlyPeriodType.NAME );

        Period period = setPeriodDateRange( periodType );

        return ((CalendarPeriodType) periodType).generatePeriods( period.getStartDate(), period.getEndDate() );
    }

    private PdfPTable getProgramStageMainTable()
    {
        PdfPTable mainTable = new PdfPTable( 1 ); // Code 1

        mainTable.setTotalWidth( 800f );
        mainTable.setLockedWidth( true );
        mainTable.setHorizontalAlignment( Element.ALIGN_LEFT );

        return mainTable;
    }

    private void insertTable_OrgAndPeriod( PdfPTable mainTable, PdfWriter writer, List<Period> periods )
        throws IOException, DocumentException
    {
        boolean hasBorder = false;
        float width = 220.0f;

        // Input TextBox size
        Rectangle rectangle = new Rectangle( width, PdfDataEntryFormUtil.CONTENT_HEIGHT_DEFAULT );

        // Add Organization ID/Period textfield
        // Create A table to add for each group AT HERE
        PdfPTable table = new PdfPTable( 2 ); // Code 1
        table.setWidths( new int[]{ 1, 3 } );
        table.setHorizontalAlignment( Element.ALIGN_LEFT );


        addCell_Text( table, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), "Organization unit identifier", Element.ALIGN_RIGHT );
        addCell_WithTextField( table, rectangle, writer, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), PdfDataEntryFormUtil.LABELCODE_ORGID,
            PdfFieldCell.TYPE_TEXT_ORGUNIT );

        String[] periodsTitle = getPeriodTitles( periods, format );
        String[] periodsValue = getPeriodValues( periods );

        addCell_Text( table, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), "Period", Element.ALIGN_RIGHT );
        addCell_WithDropDownListField( table, rectangle, writer, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), PdfDataEntryFormUtil.LABELCODE_PERIODID, periodsTitle, periodsValue );

        // Add to the main table
        PdfPCell cell_withInnerTable = new PdfPCell( table );
        // cell_withInnerTable.setPadding(0);
        cell_withInnerTable.setBorder( Rectangle.NO_BORDER );

        cell_withInnerTable.setHorizontalAlignment( Element.ALIGN_LEFT );

        mainTable.addCell( cell_withInnerTable );
    }

    private void insertTable_HiddenValue( PdfPTable mainTable, Rectangle rectangle, PdfWriter writer, String fieldName,
        String value )
        throws IOException, DocumentException
    {
        boolean hasBorder = false;

        // Add Organization ID/Period textfield
        // Create A table to add for each group AT HERE
        PdfPTable table = new PdfPTable( 1 ); // Code 1

        addCell_WithTextField( table, rectangle, writer, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), fieldName, value );

        // Add to the main table
        PdfPCell cell_withInnerTable = new PdfPCell( table );
        // cell_withInnerTable.setPadding(0);
        cell_withInnerTable.setBorder( Rectangle.NO_BORDER );
        mainTable.addCell( cell_withInnerTable );
    }

    private void insertTable_TextRow( PdfWriter writer, PdfPTable mainTable, String text )
    {
        insertTable_TextRow( writer, mainTable, text,
            pdfFormFontSettings.getFont( PdfFormFontSettings.FONTTYPE_BODY ) );
    }

    private void insertTable_TextRow( PdfWriter writer, PdfPTable mainTable, String text, Font font )
    {
        boolean hasBorder = false;

        // Add Organization ID/Period textfield
        // Create A table to add for each group AT HERE
        PdfPTable table = new PdfPTable( 1 );
        table.setHorizontalAlignment( Element.ALIGN_LEFT );

        addCell_Text( table, PdfDataEntryFormUtil.getPdfPCell( hasBorder ), text, Element.ALIGN_LEFT, font );

        // Add to the main table
        PdfPCell cell_withInnerTable = new PdfPCell( table );

        cell_withInnerTable.setBorder( Rectangle.NO_BORDER );

        mainTable.addCell( cell_withInnerTable );
    }

    // Insert 'Save As' button to document.
    //@SuppressWarnings( "unused" )
    private void insertSaveAsButton( Document document, PdfWriter writer, String name, String dataSetName )
        throws DocumentException
    {
        boolean hasBorder = false;

        // Button Table
        PdfPTable tableButton = new PdfPTable( 1 );

        tableButton.setWidthPercentage( 20.0f );
        float buttonHeight = PdfDataEntryFormUtil.UNITSIZE_DEFAULT + 5;

        tableButton.setHorizontalAlignment( Element.ALIGN_CENTER );

        //FIXME
        
        String jsAction = "var newFileName = this.getField(\"" + PdfDataEntryFormUtil.LABELCODE_PERIODID + "\").value + ' ' + "
            + "  this.getField(\"" + PdfDataEntryFormUtil.LABELCODE_ORGID + "\").value + ' ' + "
            + "  \"" + dataSetName + ".pdf\";"
            + "var returnVal = app.alert('This will save this PDF file as ' + newFileName + '.  Do you want to Continue?', 1, 2);"
            + "if(returnVal == 4) { "
            + "  var aMyPath = this.path.split(\"/\");"
            + "  aMyPath.pop();"
            + "  aMyPath.push(newFileName);"
            + "  this.saveAs(aMyPath.join(\"/\"));"
            + "  this.saveAs({cPath:cMyPath, bPromptToOverwrite:true});"
            + "  app.alert('File Saved.', 1);"
            + "} ";

        addCell_WithPushButtonField( tableButton, writer, PdfDataEntryFormUtil.getPdfPCell( buttonHeight, PdfDataEntryFormUtil.CELL_COLUMN_TYPE_ENTRYFIELD, hasBorder ), name, jsAction );

        document.add( tableButton );
    }

    private void addCell_Text( PdfPTable table, PdfPCell cell, String text, int horizontalAlignment )
    {
        addCell_Text( table, cell, text, horizontalAlignment, pdfFormFontSettings.getFont( PdfFormFontSettings.FONTTYPE_BODY ) );
    }

    private void addCell_Text( PdfPTable table, PdfPCell cell, String text, int horizontalAlignment, Font font )
    {
        cell.setHorizontalAlignment( horizontalAlignment );

        cell.setPhrase( new Phrase( text, font ) );

        table.addCell( cell ); // TODO: change this with cellEvent?
    }

    private void addCell_WithTextField( PdfPTable table, Rectangle rect, PdfWriter writer, PdfPCell cell, String strfldName )
        throws IOException, DocumentException
    {
        addCell_WithTextField( table, rect, writer, cell, strfldName, PdfFieldCell.TYPE_DEFAULT, "" );
    }

    private void addCell_WithTextField( PdfPTable table, Rectangle rect, PdfWriter writer, PdfPCell cell, String strfldName,
        int fieldCellType )
        throws IOException, DocumentException
    {
        addCell_WithTextField( table, rect, writer, cell, strfldName, fieldCellType, "" );
    }

    private void addCell_WithTextField( PdfPTable table, Rectangle rect, PdfWriter writer, PdfPCell cell, String strfldName,
        String value )
        throws IOException, DocumentException
    {
        addCell_WithTextField( table, rect, writer, cell, strfldName, PdfFieldCell.TYPE_DEFAULT, value );
    }

    private void addCell_WithTextField( PdfPTable table, Rectangle rect, PdfWriter writer, PdfPCell cell, String strfldName,
        int fieldCellType, String value )
        throws IOException, DocumentException
    {
        TextField nameField = new TextField( writer, rect, strfldName );

        nameField.setBorderWidth( 1 );
        nameField.setBorderColor( Color.BLACK );
        nameField.setBorderStyle( PdfBorderDictionary.STYLE_SOLID );
        nameField.setBackgroundColor( COLOR_BACKGROUDTEXTBOX );

        nameField.setText( value );

        nameField.setAlignment( Element.ALIGN_RIGHT );
        nameField.setFont( pdfFormFontSettings.getFont( PdfFormFontSettings.FONTTYPE_BODY ).getBaseFont() );

        cell.setCellEvent( new PdfFieldCell( nameField.getTextField(), rect.getWidth(), rect.getHeight(), fieldCellType, writer ) );

        table.addCell( cell );
    }

    private void addCell_WithDropDownListField( PdfPTable table, Rectangle rect, PdfWriter writer, PdfPCell cell, String strfldName, String[] optionList,
        String[] valueList ) throws IOException, DocumentException
    {
        TextField textList = new TextField( writer, rect, strfldName );

        textList.setChoices( optionList );
        textList.setChoiceExports( valueList );

        textList.setBorderWidth( 1 );
        textList.setBorderColor( Color.BLACK );
        textList.setBorderStyle( PdfBorderDictionary.STYLE_SOLID );
        textList.setBackgroundColor( COLOR_BACKGROUDTEXTBOX );

        PdfFormField dropDown = textList.getComboField();

        cell.setCellEvent( new PdfFieldCell( dropDown, rect.getWidth(), rect.getHeight(), writer ) );

        table.addCell( cell );
    }

    private void addCell_WithCheckBox( PdfPTable table, PdfWriter writer, PdfPCell cell, String strfldName )
        throws IOException, DocumentException
    {
        float sizeDefault = PdfDataEntryFormUtil.UNITSIZE_DEFAULT;

        RadioCheckField checkbox = new RadioCheckField( writer, new Rectangle( sizeDefault, sizeDefault ), "Yes", "On" );

        checkbox.setBorderWidth( 1 );
        checkbox.setBorderColor( Color.BLACK );

        PdfFormField checkboxfield = checkbox.getCheckField();
        checkboxfield.setFieldName( strfldName + "_" + PdfFieldCell.TPYEDEFINE_NAME + PdfFieldCell.TYPE_CHECKBOX );

        setCheckboxAppearance( checkboxfield, writer.getDirectContent(), sizeDefault );

        cell.setCellEvent( new PdfFieldCell( checkboxfield, sizeDefault, sizeDefault, PdfFieldCell.TYPE_CHECKBOX, writer ) );

        table.addCell( cell );
    }

    @SuppressWarnings( "unused" )
    private void addCell_WithRadioButton( PdfPTable table, PdfWriter writer, PdfPCell cell, String strfldName )
    {
        PdfFormField radiogroupField = PdfFormField.createRadioButton( writer, true );
        radiogroupField.setFieldName( strfldName );

        cell.setCellEvent( new PdfFieldCell( radiogroupField, new String[]{ "Yes", "No", "null" }, new String[]{
            "true", "false", "" }, "", 30.0f, PdfDataEntryFormUtil.UNITSIZE_DEFAULT, PdfFieldCell.TYPE_RADIOBUTTON, writer ) );

        table.addCell( cell );

        writer.addAnnotation( radiogroupField );
    }

    private void addCell_WithPushButtonField( PdfPTable table, PdfWriter writer, PdfPCell cell, String strfldName, String jsAction )
    {
        cell.setCellEvent( new PdfFieldCell( null, jsAction, "BTN_SAVEPDF", "Save PDF", PdfFieldCell.TYPE_BUTTON,
            writer ) );

        table.addCell( cell );
    }

    public String[] getPeriodValues( List<Period> periods )
    {
        String[] periodValues = new String[periods.size()];

        for ( int i = 0; i < periods.size(); i++ )
        {
            periodValues[i] = periods.get( i ).getIsoDate();
        }

        return periodValues;
    }

    public String[] getPeriodTitles( List<Period> periods, I18nFormat format )
    {
        String[] periodTitles = new String[periods.size()];

        for ( int i = 0; i < periods.size(); i++ )
        {
            Period period = periods.get( i );
            periodTitles[i] = format.formatPeriod( period );

            periodTitles[i] += " - " + DateUtils.getMediumDateString( period.getStartDate() )
                + " - " + DateUtils.getMediumDateString( period.getEndDate() );
        }

        return periodTitles;
    }

    private Period setPeriodDateRange( PeriodType periodType )
        throws ParseException
    {
        Period period = new Period();

        Calendar currentDate = Calendar.getInstance();

        int currYear = currentDate.get( Calendar.YEAR );
        int startYear = currYear - PERIODRANGE_PREVYEARS;
        int endYear = currYear + PERIODRANGE_FUTUREYEARS;

        if ( periodType.getName().equals( QuarterlyPeriodType.NAME )
            || periodType.getName().equals( SixMonthlyPeriodType.NAME )
            || periodType.getName().equals( SixMonthlyAprilPeriodType.NAME )
            || periodType.getName().equals( YearlyPeriodType.NAME )
            || periodType.getName().equals( FinancialAprilPeriodType.NAME )
            || periodType.getName().equals( FinancialJulyPeriodType.NAME )
            || periodType.getName().equals( FinancialOctoberPeriodType.NAME ) )
        {
            startYear = currYear - PERIODRANGE_PREVYEARS_YEARLY;
            endYear = currYear + PERIODRANGE_FUTUREYEARS_YEARLY;
        }

        period.setStartDate( DateUtils.getMediumDate( String.valueOf( startYear ) + "-01-01" ) );
        period.setEndDate( DateUtils.getMediumDate( String.valueOf( endYear ) + "-01-01" ) );

        return period;
    }

    private void setCheckboxAppearance( PdfFormField checkboxfield, PdfContentByte canvas, float width )
    {
        PdfAppearance[] onOff = new PdfAppearance[2];
        onOff[0] = canvas.createAppearance( width + 2, width + 2 );
        onOff[0].rectangle( 1, 1, width, width );
        onOff[0].stroke();
        onOff[1] = canvas.createAppearance( width + 2, width + 2 );
        onOff[1].setRGBColorFill( 255, 128, 128 );
        onOff[1].rectangle( 1, 1, width, width );
        onOff[1].fillStroke();
        onOff[1].moveTo( 1, 1 );
        onOff[1].lineTo( width + 1, width + 1 );
        onOff[1].moveTo( 1, width + 1 );
        onOff[1].lineTo( width + 1, 1 );
        onOff[1].stroke();

        checkboxfield.setAppearance( PdfAnnotation.APPEARANCE_NORMAL, "Off", onOff[0] );
        checkboxfield.setAppearance( PdfAnnotation.APPEARANCE_NORMAL, "On", onOff[1] );
    }
}
