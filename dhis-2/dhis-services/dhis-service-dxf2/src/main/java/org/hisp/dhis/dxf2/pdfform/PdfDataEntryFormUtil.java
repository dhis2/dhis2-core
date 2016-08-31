package org.hisp.dhis.dxf2.pdfform;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfReader;

/**
 * @author James Chang
 */
public class PdfDataEntryFormUtil
{
    public static final int DATATYPE_DATASET = 0;

    public static final int DATATYPE_PROGRAMSTAGE = 1;

    public static final float UNITSIZE_DEFAULT = 10;

    // Label Names

    public static final String LABELCODE_TEXTFIELD = "TXFD_";

    public static final String LABELCODE_BUTTON = "BTNFD_";

    public static final String LABELCODE_ORGID = LABELCODE_TEXTFIELD + "OrgID";

    public static final String LABELCODE_PERIODID = LABELCODE_TEXTFIELD + "PeriodID";

    public static final String LABELCODE_BUTTON_SAVEAS = LABELCODE_BUTTON + "SaveAs";

    public static final String LABELCODE_DATADATETEXTFIELD = "TXFDDT_";

    public static final String LABELCODE_DATAENTRYTEXTFIELD = "TXFDDV_";

    public static final String LABELCODE_PROGRAMSTAGEIDTEXTBOX = "TXPSTGID_";

    // Cell Related

    private static final Color COLOR_CELLBORDER = Color.getHSBColor( 0.0f, 0.0f, 0.863f);

    public static final float CELL_MIN_HEIGHT_DEFAULT = 14;

    public static final float CONTENT_HEIGHT_DEFAULT = 12;

    public static final int CELL_COLUMN_TYPE_LABEL = 0;

    public static final int CELL_COLUMN_TYPE_ENTRYFIELD = 1;

    public static final int CELL_COLUMN_TYPE_HEADER = 2;

    private static final String DATAVALUE_IMPORT_STOREBY = "admin";

    private static final String DATAVALUE_IMPORT_COMMENT = "Imported by PDF Data Entry Form";

    private static final String FOOTERTEXT_DEFAULT = "PDF Template [v 1.00] generated from DHIS %s on %s";

    private static final String ERROR_INVALID_PERIOD = "Invalid period: ";

    private static final String ERROR_EMPTY_ORG_UNIT = "The organisation unit was not specified";

    private static final String ERROR_EMPTY_PERIOD = "The period was not specified.";

    // -------------------------------------------------------------------------
    // METHODS
    // -------------------------------------------------------------------------

    public static void setFooterOnDocument( Document document, String footerText, Font font )
    {
        boolean isNumbered = true;

        HeaderFooter footer = new HeaderFooter( new Phrase( footerText, font ), isNumbered );
        footer.setBorder( Rectangle.NO_BORDER );
        footer.setAlignment( Element.ALIGN_RIGHT );
        document.setFooter( footer );
    }

    public static void setDefaultFooterOnDocument( Document document, String serverName, Font font )
    {
        String strFooterText = String.format( FOOTERTEXT_DEFAULT, serverName, DateUtils.getMediumDateString() );

        setFooterOnDocument( document, strFooterText, font );
    }

    public static Rectangle getDefaultPageSize( int typeId )
    {
        if ( typeId == PdfDataEntryFormUtil.DATATYPE_PROGRAMSTAGE )
        {
            return new Rectangle( PageSize.A4.getLeft(), PageSize.A4.getBottom(), PageSize.A4.getTop(),
                PageSize.A4.getRight() );
        }
        else
        {
            return PageSize.A4;
        }
    }

    public static PdfPCell getPdfPCell( boolean hasBorder )
    {
        return getPdfPCell( CELL_MIN_HEIGHT_DEFAULT, CELL_COLUMN_TYPE_ENTRYFIELD, hasBorder );
    }

    public static PdfPCell getPdfPCell( float minHeight, int cellContentType, boolean hasBorder )
    {
        PdfPCell cell = new PdfPCell();
        cell.setMinimumHeight( minHeight );
        
        if( hasBorder )
        {
            cell.setBorderWidth( 0.1f );
            cell.setBorderColor( COLOR_CELLBORDER );            
        }
        else
        {
            cell.setBorder( Rectangle.NO_BORDER );
        }
        
        cell.setPadding( 2.0f );

        switch ( cellContentType )
        {
            case CELL_COLUMN_TYPE_ENTRYFIELD:
                cell.setHorizontalAlignment( Element.ALIGN_CENTER );
                cell.setVerticalAlignment( Element.ALIGN_MIDDLE );
    
                break;
    
            case CELL_COLUMN_TYPE_HEADER:
                cell.setHorizontalAlignment( Element.ALIGN_CENTER );
                cell.setVerticalAlignment( Element.ALIGN_MIDDLE );
    
                break;
    
            case CELL_COLUMN_TYPE_LABEL:
                cell.setHorizontalAlignment( Element.ALIGN_RIGHT );
                cell.setVerticalAlignment( Element.ALIGN_TOP );
    
            default:
                break;
        }

        return cell;
    }

    /**
     * Creates data value set from Input Stream (PDF) for PDF data import
     */
    public static DataValueSet getDataValueSet( InputStream in )
    {
        PdfReader reader = null;

        DataValueSet dataValueSet = new DataValueSet();

        List<org.hisp.dhis.dxf2.datavalue.DataValue> dataValueList = new ArrayList<>();

        try
        {
            reader = new PdfReader( in );

            AcroFields form = reader.getAcroFields();

            if ( form != null )
            {
                // Process OrgUnitUID and PeriodID from the PDF Form

                String orgUnitUid = form.getField( PdfDataEntryFormUtil.LABELCODE_ORGID ).trim();
                String periodId = form.getField( PdfDataEntryFormUtil.LABELCODE_PERIODID ).trim();

                if ( periodId == null || periodId.isEmpty() )
                {
                    throw new InvalidIdentifierReferenceException( ERROR_EMPTY_PERIOD );
                }

                if ( orgUnitUid == null || orgUnitUid.isEmpty() )
                {
                    throw new InvalidIdentifierReferenceException( ERROR_EMPTY_ORG_UNIT );
                }

                Period period = PeriodType.getPeriodFromIsoString( periodId );
                
                if ( period == null )
                {
                    throw new InvalidIdentifierReferenceException( ERROR_INVALID_PERIOD + periodId );
                }
                
                // Loop Through the Fields and get data.

                @SuppressWarnings( "unchecked" )
                Set<String> fldNames = form.getFields().keySet();

                for ( String fldName : fldNames )
                {
                    
                    if ( fldName.startsWith( PdfDataEntryFormUtil.LABELCODE_DATAENTRYTEXTFIELD ) )
                    {
                        String[] strArrFldName = fldName.split( "_" );

                        org.hisp.dhis.dxf2.datavalue.DataValue dataValue = new org.hisp.dhis.dxf2.datavalue.DataValue();

                        dataValue.setDataElement( strArrFldName[1] );
                        dataValue.setCategoryOptionCombo( strArrFldName[2] );
                        dataValue.setOrgUnit( orgUnitUid );
                        dataValue.setPeriod( period.getIsoDate() );
                        
                        dataValue.setValue( fieldValueFormat( strArrFldName, form.getField( fldName ) ) );     
                        
                        dataValue.setStoredBy( DATAVALUE_IMPORT_STOREBY );
                        dataValue.setComment( DATAVALUE_IMPORT_COMMENT );
                        dataValue.setFollowup( false );
                        dataValue.setLastUpdated( DateUtils.getMediumDateString() );

                        dataValueList.add( dataValue );
                    }
                }

                dataValueSet.setDataValues( dataValueList );
            }
            else
            {
                throw new RuntimeException( "Could not generate PDF AcroFields form from input" );
            }
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( ex );
        }
        finally
        {
            if ( reader != null )
            {
                reader.close();
            }
        }

        return dataValueSet;
    }
    
    private static String fieldValueFormat( String[] strArrFldName, String fldValue )
    {                
        // For checkbox, we need to change value from Off/On --> false/true
        if( strArrFldName.length == 4 
            && strArrFldName[3].substring( 0, 1 ).compareTo( PdfFieldCell.TPYEDEFINE_NAME ) == 0 )
        {            
            int fieldType = Integer.parseInt( strArrFldName[3].substring( 1 ) );
                                        
            if( fieldType == PdfFieldCell.TYPE_CHECKBOX )
            {       
                if( fldValue.compareTo( "On" ) == 0 )
                {
                    fldValue = "true";
                }
                else if( fldValue.compareTo( "Off" ) == 0 )
                {
                    fldValue = null;                                    
                }                                
            }            
        }
                
        return fldValue;
    }

}
