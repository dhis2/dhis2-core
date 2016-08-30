package org.hisp.dhis.validationrule.action;

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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Lars Helge Overland
 * @version $Id: GetFilteredDataElementsAction.java 6256 2008-11-10 17:10:30Z
 *          larshelg $
 */
public class GetFilteredDataElementsAction
    implements Action
{
    private static final int ALL = 0;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    // -------------------------------------------------------------------------
    // Parameters
    // -------------------------------------------------------------------------

    private Integer dataSetId;

    public void setDataSetId( Integer dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    private String periodTypeName;

    public void setPeriodTypeName( String periodTypeName )
    {
        this.periodTypeName = periodTypeName;
    }

    public String getPeriodTypeName()
    {
        return periodTypeName;
    }

    private String filter;

    public void setFilter( String filter )
    {
        this.filter = filter;
    }

    private List<DataElement> dataElements;

    public List<DataElement> getDataElements()
    {
        return dataElements;
    }

    private List<DataElementOperand> operands = new ArrayList<>();

    public List<DataElementOperand> getOperands()
    {
        return operands;
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {

        // ---------------------------------------------------------------------
        // Get periodType
        // ---------------------------------------------------------------------

        PeriodType periodType = periodService.getPeriodTypeByName( periodTypeName );

        // ---------------------------------------------------------------------
        // DataElementGroup filter
        // ---------------------------------------------------------------------

        if ( dataSetId == ALL )
        {
            // -----------------------------------------------------------------
            // Get datasets with the periodType
            // -----------------------------------------------------------------

            List<DataSet> dataSets = new ArrayList<>( dataSetService.getDataSetsByPeriodType( periodType ) );

            Collections.sort( dataSets, IdentifiableObjectNameComparator.INSTANCE );

            // -----------------------------------------------------------------
            // Get available dataelements into the dataSets
            // -----------------------------------------------------------------

            Set<DataElement> members = new HashSet<>();

            for ( DataSet dataSet : dataSets )
            {
                members.addAll( dataSet.getDataElements() );
            }

            dataElements = new ArrayList<>( getIntegerDataElements( members ) );
        }
        else
        {
            Collection<DataElement> members = dataSetService.getDataSet( dataSetId ).getDataElements();

            dataElements = new ArrayList<>( getIntegerDataElements( members ) );
        }

        Collections.sort( dataElements, new IdentifiableObjectNameComparator() );

        // ---------------------------------------------------------------------
        // Create Operands
        // ---------------------------------------------------------------------

        operands = new ArrayList<>( categoryService.getOperands( dataElements ) );

        // ---------------------------------------------------------------------
        // String filter
        // ---------------------------------------------------------------------

        Iterator<DataElementOperand> iterator = operands.iterator();

        while ( iterator.hasNext() )
        {
            DataElementOperand operand = iterator.next();

            if ( !operand.getOperandName().toLowerCase().contains( filter.toLowerCase() ) )
            {
                iterator.remove();
            }
        }

        return SUCCESS;
    }

    // -------------------------------------------------------------------------
    // TODO use predicate / commons instead
    // -------------------------------------------------------------------------

    private Collection<DataElement> getIntegerDataElements( Collection<DataElement> dataElements )
    {
        Iterator<DataElement> iterator = dataElements.iterator();

        while ( iterator.hasNext() )
        {
            if ( !iterator.next().getValueType().isNumeric() )
            {
                iterator.remove();
            }
        }

        return dataElements;
    }
}
