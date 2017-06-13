package org.hisp.dhis.webapi.utils;

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

import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.pdfform.PdfDataEntryFormUtil;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PdfDataEntryFormImportUtil
{
    // -------------------------------------------------------------------------
    // DEPENDENCIES
    // -------------------------------------------------------------------------

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private TrackedEntityDataValueService dataValueService;

    // -------------------------------------------------------------------------
    // METHODS
    // -------------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    public void importProgramStage( InputStream in, I18nFormat format )
        throws Exception
    {
        int programStageInstanceId;
        ProgramStageInstance programStageInstance;

        PdfReader reader = new PdfReader( in, null );

        AcroFields form = reader.getAcroFields();

        String strOrgID = form.getField( PdfDataEntryFormUtil.LABELCODE_ORGID );
        int organisationUnitId = Integer.parseInt( strOrgID );

        String strPeriodID = form.getField( PdfDataEntryFormUtil.LABELCODE_PERIODID );
        Date executionDateInput = format.parseDate( strPeriodID );
        Calendar executeDateCal = Calendar.getInstance();
        executeDateCal.setTime( executionDateInput );

        int programStageId = Integer.valueOf( form.getField( PdfDataEntryFormUtil.LABELCODE_PROGRAMSTAGEIDTEXTBOX ) );

        ProgramStageInstanceDataManager programStageInstanceDataManager = new ProgramStageInstanceDataManager();

        // Loop Through the Fields and get data.
        Set<String> fldNames = form.getFields().keySet();

        // Create Organized data
        for ( String fldName : fldNames )
        {
            // If the value in the text field is not empty, proceed to add it.
            if ( !form.getField( fldName ).trim().isEmpty() )
            {
                if ( fldName.startsWith( PdfDataEntryFormUtil.LABELCODE_DATADATETEXTFIELD ) )
                {

                    String[] strArrFldName = fldName.split( "_" );
                    int rowNumber = Integer.parseInt( strArrFldName[1] );

                    programStageInstanceDataManager.addDateData( Integer.parseInt( form.getField( fldName ) ),
                        rowNumber );
                }
                else if ( fldName.startsWith( PdfDataEntryFormUtil.LABELCODE_DATAENTRYTEXTFIELD ) )
                {

                    String[] strArrFldName = fldName.split( "_" );
                    int dataElementId = Integer.parseInt( strArrFldName[1] );
                    int rowNumber = Integer.parseInt( strArrFldName[2] );

                    programStageInstanceDataManager.addDataElementData( dataElementId, form.getField( fldName ),
                        rowNumber );

                }
            }
        }

        // For each row, add new programStageInstance and add data elements to it.
        for ( Map.Entry<Integer, ProgramStageInstanceStorage> entry : programStageInstanceDataManager
            .getProgramStageInstanceData().entrySet() )
        {
            ProgramStageInstanceStorage programStageInstanceStorage = entry.getValue();

            int date = programStageInstanceStorage.getDate();
            executeDateCal.set( Calendar.DATE, date );
            Date executionDate = executeDateCal.getTime();

            // Step 2. Create Program Stage Instance - If does not exists
            // already.
            programStageInstanceId = addNewProgramStageInstance( programStageId, organisationUnitId, executionDate );

            programStageInstance = programStageInstanceService.getProgramStageInstance( programStageInstanceId );

            for ( Map.Entry<Integer, String> dataElementsEntry : programStageInstanceStorage.getDataElementsValue().entrySet() )
            {
                Integer dataElementId = dataElementsEntry.getKey();
                String value = dataElementsEntry.getValue();

                // Step 3. Insert Data
                insertValueProgramStageDataElement( programStageInstance, dataElementId, value );
            }
        }

        reader.close();
    }

    private int addNewProgramStageInstance( int programStageId, int organisationUnitId, Date executionDate )
        throws Exception
    {
        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( organisationUnitId );

        if ( organisationUnit == null )
        {
            throw new Exception( "Invalid organisation unit identifier: " + organisationUnitId );
        }

        ProgramStage programStage = programStageService.getProgramStage( programStageId );
        Program program = programStage.getProgram();

        ProgramInstance programInstance = programInstanceService.getProgramInstances( program ).iterator().next();

        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setDueDate( executionDate );
        programStageInstance.setExecutionDate( executionDate );
        programStageInstance.setOrganisationUnit( organisationUnit );

        return programStageInstanceService.addProgramStageInstance( programStageInstance );
    }

    private void insertValueProgramStageDataElement( ProgramStageInstance programStageInstance, int dataElementId, String value )
        throws Exception
    {

        DataElement dataElement = dataElementService.getDataElement( dataElementId );

        TrackedEntityDataValue dataValue = dataValueService.getTrackedEntityDataValue( programStageInstance,
            dataElement );

        value = StringUtils.trimToNull( value );

        // ---------------------------------------------------------------------
        // Save value
        // ---------------------------------------------------------------------

        if ( programStageInstance.getExecutionDate() == null )
        {
            programStageInstance.setExecutionDate( new Date() );
            programStageInstanceService.updateProgramStageInstance( programStageInstance );
        }

        // providedElsewhere = (providedElsewhere == null) ? false :
        // providedElsewhere;
        String storedBy = currentUserService.getCurrentUsername();

        if ( dataValue == null && value != null )
        {
            // LOG.debug( "Adding TrackedEntityDataValue, value added" );

            dataValue = new TrackedEntityDataValue( programStageInstance, dataElement, value );
            dataValue.setStoredBy( storedBy );
            // dataValue.setProvidedElsewhere( providedElsewhere );

            dataValueService.saveTrackedEntityDataValue( dataValue );
        }

        if ( dataValue != null && value == null )
        {
            dataValueService.deleteTrackedEntityDataValue( dataValue );
        }
        else if ( dataValue != null )
        {
            dataValue.setValue( value );
            dataValue.setLastUpdated( new Date() );
            dataValue.setStoredBy( storedBy );

            dataValueService.updateTrackedEntityDataValue( dataValue );
        }
    }

    private class ProgramStageInstanceDataManager
    {
        private Map<Integer, ProgramStageInstanceStorage> programStageInstanceData;

        private Map<Integer, ProgramStageInstanceStorage> getProgramStageInstanceData()
        {
            return programStageInstanceData;
        }

        private ProgramStageInstanceDataManager()
        {
            programStageInstanceData = new HashMap<>();
        }

        public void addDateData( int date, int rowNumber )
        {
            // Obsolete - get Date from strDateData

            // If it already exists, add to existing.
            // Otherwise, create one and add value in it.
            if ( programStageInstanceData.containsKey( rowNumber ) )
            {
                // add to existing
                programStageInstanceData.get( rowNumber ).setDate( date );
            }
            else
            {
                // Create and add
                ProgramStageInstanceStorage programStageInstanceStorage = new ProgramStageInstanceStorage();
                programStageInstanceStorage.setDate( date );

                programStageInstanceData.put( rowNumber, programStageInstanceStorage );
            }

        }

        public void addDataElementData( int dataElementId, String value, int rowNumber )
        {
            // If it already exists, add to existing.
            // Otherwise, create one and add value in it.
            if ( programStageInstanceData.containsKey( rowNumber ) )
            {
                // add to existing
                programStageInstanceData.get( rowNumber ).addDataElements( dataElementId, value );
            }
            else
            {
                // Create and add
                ProgramStageInstanceStorage programStageInstanceStorage = new ProgramStageInstanceStorage();
                programStageInstanceStorage.addDataElements( dataElementId, value );

                programStageInstanceData.put( rowNumber, programStageInstanceStorage );
            }
        }
    }

    private class ProgramStageInstanceStorage
    {
        private int date;

        private Map<Integer, String> dataElementsValue;

        // -------------------------------------------------------------------------
        // GET/SET
        // -------------------------------------------------------------------------

        public int getDate()
        {
            return date;
        }

        public void setDate( int date )
        {
            this.date = date;
        }

        public Map<Integer, String> getDataElementsValue()
        {
            return dataElementsValue;
        }

        // -------------------------------------------------------------------------
        // CONSTRUCTOR
        // -------------------------------------------------------------------------

        public ProgramStageInstanceStorage()
        {
            dataElementsValue = new HashMap<>();
        }

        // -------------------------------------------------------------------------
        // METHODS
        // -------------------------------------------------------------------------

        public void addDataElements( Integer dataElementId, String value )
        {
            dataElementsValue.put( dataElementId, value );
        }
    }

}
