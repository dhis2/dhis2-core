package org.hisp.dhis.de.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import com.opensymphony.xwork2.Action;

/**
 * @author Mike Nelushi
 */
public class GetGreyFieldsByOrgUnitAction
    implements Action
{
	
	private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }


	// -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------
	private String dataSetId;

    public void setDataSetId( String dataSetId )
    {
        this.dataSetId = dataSetId;
    }
    
	private String organisationUnitId;

	public void setOrganisationUnitId( String organisationUnitId )
    {
        this.organisationUnitId = organisationUnitId;
    }
    
    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------
    
	
	 private Map<String, Boolean> greyedFieldsByOrgUnit = new HashMap<>();
	 
	 public Map<String, Boolean> getGreyedFieldsByOrgUnit() {
		return greyedFieldsByOrgUnit;
	}
	
	
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

	
	@Override
    public String execute()
    {
    	DataSet dataSet = dataSetService.getDataSet( dataSetId );
    	
		for ( DataElementOperand operand : dataSet.getGreyedFields() )
        {
            if ( operand != null && operand.getDataElement() != null && operand.getOrganisationUnit() != null )
            {
            	if(operand.getOrganisationUnit().getUid().equals(organisationUnitId)){
            		greyedFieldsByOrgUnit.put( operand.getDataElement().getUid() + "-" + operand.getCategoryOptionCombo().getUid(), true );
            	}                	
            }
        }    	
    	
        return SUCCESS;
    }
}
