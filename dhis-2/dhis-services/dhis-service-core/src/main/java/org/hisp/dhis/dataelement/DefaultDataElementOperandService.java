package org.hisp.dhis.dataelement;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.expression.ExpressionService;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@Transactional
public class DefaultDataElementOperandService
    implements DataElementOperandService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementOperandStore dataElementOperandStore;

    public void setDataElementOperandStore( DataElementOperandStore dataElementOperandStore )
    {
        this.dataElementOperandStore = dataElementOperandStore;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    // -------------------------------------------------------------------------
    // Operand
    // -------------------------------------------------------------------------

    @Override
    public int addDataElementOperand( DataElementOperand dataElementOperand )
    {
        return dataElementOperandStore.save( dataElementOperand );
    }

    @Override
    public void deleteDataElementOperand( DataElementOperand dataElementOperand )
    {
        dataElementOperandStore.delete( dataElementOperand );
    }

    @Override
    public DataElementOperand getDataElementOperand( int id )
    {
        return dataElementOperandStore.get( id );
    }

    @Override
    public DataElementOperand getDataElementOperandByUid( String uid )
    {
        if ( StringUtils.isEmpty( uid ) )
        {
            return null;
        }

        Matcher matcher = ExpressionService.OPERAND_UID_PATTERN.matcher( uid );

        matcher.find();

        String deUid = matcher.group( 1 );
        String cocUid = matcher.group( 2 );

        DataElement dataElement = dataElementService.getDataElement( deUid );

        if ( dataElement == null )
        {
            return null;
        }

        DataElementCategoryOptionCombo categoryOptionCombo = null;

        if ( cocUid != null )
        {
            categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( cocUid );
        }

        return new DataElementOperand( dataElement, categoryOptionCombo );
    }

    @Override
    public List<DataElementOperand> getDataElementOperandsByUid( Collection<String> uids )
    {
        List<DataElementOperand> list = new ArrayList<>();

        for ( String uid : uids )
        {
            list.add( getDataElementOperandByUid( uid ) );
        }

        return list;
    }
    
    @Override
    public List<DataElementOperand> getAllDataElementOperands()
    {
        return dataElementOperandStore.getAllOrderedName();
    }

    @Override
    public List<DataElementOperand> getAllDataElementOperands( int first, int max )
    {
        return dataElementOperandStore.getAllOrderedName( first, max );
    }
    
    @Override
    public DataElementOperand getDataElementOperand( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo )
    {
        return dataElementOperandStore.get( dataElement, categoryOptionCombo );
    }
    
    @Override
    public DataElementOperand getDataElementOperand( String dataElementUid, String categoryOptionComboUid )
    {
        DataElement dataElement = dataElementService.getDataElement( dataElementUid );
        DataElementCategoryOptionCombo categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( categoryOptionComboUid );
        
        if ( dataElement == null || categoryOptionCombo == null )
        {
            return null;
        }
        
        return new DataElementOperand( dataElement, categoryOptionCombo );
    }

    @Override
    public DataElementOperand getOrAddDataElementOperand( DataElement dataElement, DataElementCategoryOptionCombo categoryOptionCombo )
    {
        DataElementOperand operand = getDataElementOperand( dataElement, categoryOptionCombo );
        
        if ( operand == null )
        {
            operand = new DataElementOperand( dataElement, categoryOptionCombo );
            
            addDataElementOperand( operand );
        }
        
        return operand;
    }

    @Override
    public DataElementOperand getOrAddDataElementOperand( String dataElementUid, String categoryOptionComboUid )
    {
        DataElement dataElement = dataElementService.getDataElement( dataElementUid );
        DataElementCategoryOptionCombo categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( categoryOptionComboUid );
        
        if ( dataElement == null || categoryOptionCombo == null )
        {
            return null;
        }
        
        return getOrAddDataElementOperand( dataElement, categoryOptionCombo );
    }
}
