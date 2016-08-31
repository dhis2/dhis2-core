package org.hisp.dhis.dd.action.dataelement;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;

import com.opensymphony.xwork2.Action;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: GetDataElementListAction.java 4378 2008-01-02 10:13:39Z abyot $
 */
public class GetDataElementGroupMembersAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private List<DataElement> dataElements;

    public List<DataElement> getDataElements()
    {
        return dataElements;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer dataElementGroupId;

    public void setDataElementGroupId( Integer dataElementGroupId )
    {
        this.dataElementGroupId = dataElementGroupId;
    }

    private List<DataElementOperand> operands = new ArrayList<>();

    public List<DataElementOperand> getOperands()
    {
        return operands;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        if ( dataElementGroupId > 0 )
        {
            DataElementGroup group = dataElementService.getDataElementGroup( dataElementGroupId.intValue() );

            dataElements = new ArrayList<>( group.getMembers() );
        }
        else
        {
            dataElements = new ArrayList<>( dataElementService.getAllDataElements() );
        }

        Collections.sort( dataElements, new IdentifiableObjectNameComparator() );

        for ( DataElement dataElement : dataElements )
        {
            DataElementCategoryCombo categoryCombo = dataElement.getCategoryCombo();
            
            Set<DataElementCategoryOptionCombo> optionCombos = categoryCombo.getOptionCombos();

            if ( optionCombos.size() > 1 )
            {
                for ( DataElementCategoryOptionCombo optionCombo : optionCombos )
                {
                    DataElementOperand operand = new DataElementOperand( dataElement.getUid(), 
                        optionCombo.getUid(), dataElement.getName() + optionCombo.getName() );
                    operands.add( operand );
                }
            }
            else
            {
                DataElementOperand operand = new DataElementOperand( dataElement.getUid(), 
                    optionCombos.iterator().next().getUid(), dataElement.getName() );
                operands.add( operand );
            }
        }

        return SUCCESS;
    }
}
