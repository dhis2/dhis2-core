package org.hisp.dhis.dataset.comparator;

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

import java.util.Comparator;

import org.hisp.dhis.dataset.DataSet;

public class DataSetApprovalFrequencyComparator
    implements Comparator<DataSet>
{
    public static final DataSetApprovalFrequencyComparator INSTANCE = new DataSetApprovalFrequencyComparator();
    
    @Override
    public int compare( DataSet d1, DataSet d2 )
    {
        if ( d1 == null )
        {
            return -1;
        }
        
        if ( d2 == null )
        {
            return 1;
        }
        
        if ( d1.getWorkflow() != null && d2.getWorkflow() == null)
        {
            return -1;
        }
        
        if ( d1.getWorkflow() == null && d2.getWorkflow() != null )
        {
            return 1;
        }
        
        int frequencyOrder = Integer.valueOf( d1.getPeriodType().getFrequencyOrder() ).compareTo( Integer.valueOf( d2.getPeriodType().getFrequencyOrder() ) );
        
        if ( frequencyOrder != 0 )
        {
            return frequencyOrder;
        }
        
        return d1.compareTo( d2 );
    }
}
