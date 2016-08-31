package org.hisp.dhis.dxf2.common;

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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class IdSchemes
{
    private IdentifiableProperty idScheme;

    private IdentifiableProperty dataElementIdScheme = IdentifiableProperty.UID;

    private IdentifiableProperty categoryOptionComboIdScheme = IdentifiableProperty.UID;

    private IdentifiableProperty orgUnitIdScheme = IdentifiableProperty.UID;

    private IdentifiableProperty programIdScheme = IdentifiableProperty.UID;

    private IdentifiableProperty programStageIdScheme = IdentifiableProperty.UID;

    public IdSchemes()
    {
    }

    public IdentifiableProperty getIdScheme()
    {
        return idScheme;
    }

    public IdentifiableProperty getIdentifiableProperty( IdentifiableProperty identifiableProperty )
    {
        return idScheme != null ? idScheme : identifiableProperty;
    }

    public void setIdScheme( IdentifiableProperty idScheme )
    {
        this.idScheme = idScheme;
    }

    public IdentifiableProperty getDataElementIdScheme()
    {
        return getIdentifiableProperty( dataElementIdScheme );
    }

    public void setDataElementIdScheme( IdentifiableProperty dataElementIdScheme )
    {
        this.dataElementIdScheme = dataElementIdScheme;
    }

    public IdentifiableProperty getCategoryOptionComboIdScheme()
    {
        return getIdentifiableProperty( categoryOptionComboIdScheme );
    }

    public void setCategoryOptionComboIdScheme( IdentifiableProperty categoryOptionComboIdScheme )
    {
        this.categoryOptionComboIdScheme = categoryOptionComboIdScheme;
    }

    public IdentifiableProperty getOrgUnitIdScheme()
    {
        return getIdentifiableProperty( orgUnitIdScheme );
    }

    public void setOrgUnitIdScheme( IdentifiableProperty orgUnitIdScheme )
    {
        this.orgUnitIdScheme = orgUnitIdScheme;
    }

    public IdentifiableProperty getProgramIdScheme()
    {
        return getIdentifiableProperty( programIdScheme );
    }

    public void setProgramIdScheme( IdentifiableProperty programIdScheme )
    {
        this.programIdScheme = programIdScheme;
    }

    public IdentifiableProperty getProgramStageIdScheme()
    {
        return getIdentifiableProperty( programStageIdScheme );
    }

    public void setProgramStageIdScheme( IdentifiableProperty programStageIdScheme )
    {
        this.programStageIdScheme = programStageIdScheme;
    }

    public static String getValue( String uid, String code, IdentifiableProperty identifiableProperty )
    {
        boolean idScheme = IdentifiableProperty.ID.equals( identifiableProperty ) || IdentifiableProperty.UID.equals( identifiableProperty );
        return idScheme ? uid : code;
    }

    public static String getValue( IdentifiableObject identifiableObject, IdentifiableProperty identifiableProperty )
    {
        boolean idScheme = IdentifiableProperty.ID.equals( identifiableProperty ) || IdentifiableProperty.UID.equals( identifiableProperty );

        if ( idScheme )
        {
            return identifiableObject.getUid();
        }
        else if ( IdentifiableProperty.CODE.equals( identifiableProperty ) )
        {
            return identifiableObject.getCode();
        }
        else if ( IdentifiableProperty.NAME.equals( identifiableProperty ) )
        {
            return identifiableObject.getName();
        }

        return null;
    }
}
