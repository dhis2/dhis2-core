package org.hisp.dhis.dataset;

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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DataSetDeletionHandler
    extends DeletionHandler
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DataSetService dataSetService;
    
    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return DataSet.class.getSimpleName();
    }

    @Override
    public void deleteDataElement( DataElement dataElement )
    {
        Iterator<DataSet> iterator = dataElement.getDataSets().iterator();
        
        while ( iterator.hasNext() )
        {
            DataSet dataSet = iterator.next();
            dataSet.removeDataElement( dataElement );
            idObjectManager.updateNoAcl( dataSet );
        }
        
        for ( DataSet dataSet : idObjectManager.getAllNoAcl( DataSet.class ) )
        {
            boolean update = false;
            
            Iterator<DataElementOperand> operands = dataSet.getCompulsoryDataElementOperands().iterator();
            
            while ( operands.hasNext() )
            {
                DataElementOperand operand = operands.next();
                
                if ( operand.getDataElement().equals( dataElement ) )
                {
                    operands.remove();
                    update = true;
                }
            }
            
            if ( update )
            {
                idObjectManager.updateNoAcl( dataSet );
            }
        }
    }
    
    @Override
    public void deleteIndicator( Indicator indicator )
    {
        Iterator<DataSet> iterator = indicator.getDataSets().iterator();
        
        while ( iterator.hasNext() )
        {
            DataSet dataSet = iterator.next();
            dataSet.getIndicators().remove( indicator );
            idObjectManager.updateNoAcl( dataSet );
        }
    }
    
    @Override
    public void deleteSection( Section section )
    {
        DataSet dataSet = section.getDataSet();
        
        if ( dataSet != null )
        {
            dataSet.getSections().remove( section );
            idObjectManager.updateNoAcl( dataSet );
        }
    }
    
    @Override
    public void deleteLegendSet( LegendSet legendSet )
    {
        Collection<DataSet> dataSets = idObjectManager.getAllNoAcl( DataSet.class );
        
        for ( DataSet dataSet : dataSets )
        {
            if ( legendSet.equals( dataSet.getLegendSet() ) )
            {
                dataSet.setLegendSet( null );
                idObjectManager.updateNoAcl( dataSet );
            }
        }
    }
    
    @Override
    public void deleteDataElementCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        Collection<DataSet> dataSets = idObjectManager.getAllNoAcl( DataSet.class );

        for ( DataSet dataSet : dataSets )
        {            
            if ( dataSet != null && categoryCombo.equals( dataSet.getCategoryCombo() ) )
            {
                dataSet.setCategoryCombo( null );
                idObjectManager.updateNoAcl( dataSet );
            }
        }        
    }

    @Override
    public void deleteOrganisationUnit( OrganisationUnit unit )
    {
        Iterator<DataSet> iterator = unit.getDataSets().iterator();
        
        while ( iterator.hasNext() )
        {
            DataSet dataSet = iterator.next();
            dataSet.getSources().remove( unit );
            idObjectManager.updateNoAcl( dataSet );
        }
    }

    @Override
    public void deleteDataEntryForm( DataEntryForm dataEntryForm )
    {
        List<DataSet> associatedDataSets = dataSetService.getDataSetsByDataEntryForm( dataEntryForm );

        for ( DataSet dataSet : associatedDataSets )
        {
            dataSet.setDataEntryForm( null );
            idObjectManager.updateNoAcl( dataSet );
        }
    }
    
    @Override
    public void deleteDataApprovalWorkflow( DataApprovalWorkflow workflow )
    {
        Iterator<DataSet> iterator = workflow.getDataSets().iterator();
        
        while ( iterator.hasNext() )
        {
            DataSet dataSet = iterator.next();
            dataSet.setWorkflow( null );
            idObjectManager.updateNoAcl( dataSet );
        }
    }    
}
