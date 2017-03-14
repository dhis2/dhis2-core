package org.hisp.dhis.common;

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

import org.hisp.dhis.util.ObjectUtils;

import com.google.common.base.MoreObjects;

/**
 * Identifier schemes used to map meta data. The general identifier
 * scheme can be overridden by id schemes specific to individual
 * object types. The default id scheme is UID.
 * 
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class IdSchemes
{
    private IdScheme idScheme = IdScheme.UID;

    private IdScheme dataElementIdScheme;

    private IdScheme categoryOptionComboIdScheme;
    
    private IdScheme categoryOptionIdScheme;

    private IdScheme orgUnitIdScheme;

    private IdScheme programIdScheme;

    private IdScheme programStageIdScheme;

    private IdScheme trackedEntityIdScheme;

    private IdScheme trackedEntityAttributeIdScheme;

    private IdScheme dataSetIdScheme;

    private IdScheme attributeOptionComboIdScheme;

    private IdScheme programStageInstanceIdScheme;

    public IdSchemes()
    {
    }

    public IdScheme getScheme( IdScheme idScheme )
    {
        return IdScheme.from( ObjectUtils.firstNonNull( idScheme, this.idScheme ) );
    }

    public IdScheme getIdScheme()
    {
        return IdScheme.from( idScheme );
    }

    public IdSchemes setIdScheme( String idScheme )
    {
        this.idScheme = IdScheme.from( idScheme );
        return this;
    }

    //--------------------------------------------------------------------------
    // Object type id schemes
    //--------------------------------------------------------------------------

    public IdScheme getDataElementIdScheme()
    {
        return getScheme( dataElementIdScheme );
    }

    public IdSchemes setDataElementIdScheme( String idScheme )
    {
        this.dataElementIdScheme = IdScheme.from( idScheme );
        return this;
    }

    public IdScheme getCategoryOptionComboIdScheme()
    {
        return getScheme( categoryOptionComboIdScheme );
    }

    public IdSchemes setCategoryOptionComboIdScheme( String idScheme )
    {
        this.categoryOptionComboIdScheme = IdScheme.from( idScheme );
        return this;
    }

    public IdScheme getCategoryOptionIdScheme()
    {
        return getScheme( categoryOptionIdScheme );
    }

    public IdSchemes setCategoryOptionIdScheme( String idScheme )
    {
        this.categoryOptionIdScheme = IdScheme.from( idScheme );
        return this;
    }

    public IdScheme getAttributeOptionComboIdScheme()
    {
        return getScheme( attributeOptionComboIdScheme );
    }

    public IdSchemes setAttributeOptionComboIdScheme( String idScheme )
    {
        this.attributeOptionComboIdScheme = IdScheme.from( idScheme );
        return this;
    }

    public IdScheme getDataSetIdScheme()
    {
        return getScheme( dataSetIdScheme );
    }

    public IdSchemes setDataSetIdScheme( String idScheme )
    {
        this.dataSetIdScheme = IdScheme.from( idScheme );
        return this;
    }

    public IdScheme getOrgUnitIdScheme()
    {
        return getScheme( orgUnitIdScheme );
    }

    public IdSchemes setOrgUnitIdScheme( String idScheme )
    {
        this.orgUnitIdScheme = IdScheme.from( idScheme );
        return this;
    }

    public IdScheme getProgramIdScheme()
    {
        return getScheme( programIdScheme );
    }

    public IdSchemes setProgramIdScheme( String idScheme )
    {
        this.programIdScheme = IdScheme.from( idScheme );
        return this;
    }

    public IdScheme getProgramStageIdScheme()
    {
        return getScheme( programStageIdScheme );
    }

    public IdSchemes setProgramStageIdScheme( String idScheme )
    {
        this.programStageIdScheme = IdScheme.from( idScheme );
        return this;
    }

    public IdScheme getProgramStageInstanceIdScheme()
    {
        return getScheme( programStageInstanceIdScheme );
    }

    public IdSchemes setProgramStageInstanceIdScheme( String idScheme )
    {
        this.programStageInstanceIdScheme = IdScheme.from( idScheme );
        return this;
    }

    public IdScheme getTrackedEntityIdScheme()
    {
        return getScheme( trackedEntityIdScheme );
    }

    public IdSchemes setTrackedEntityIdScheme( String idScheme )
    {
        this.trackedEntityIdScheme = IdScheme.from( idScheme );
        return this;
    }

    public IdScheme getTrackedEntityAttributeIdScheme()
    {
        return getScheme( trackedEntityAttributeIdScheme );
    }

    public IdSchemes setTrackedEntityAttributeIdScheme( String idScheme )
    {
        this.trackedEntityAttributeIdScheme = IdScheme.from( idScheme );
        return this;
    }

    //--------------------------------------------------------------------------
    // Get value methods
    //--------------------------------------------------------------------------

    public static String getValue( String uid, String code, IdentifiableProperty identifiableProperty )
    {
        return getValue( uid, code, IdScheme.from( identifiableProperty ) );
    }

    public static String getValue( String uid, String code, IdScheme idScheme )
    {
        boolean isId = idScheme.is( IdentifiableProperty.ID ) || idScheme.is( IdentifiableProperty.UID );

        return isId ? uid : code;
    }

    public static String getValue( IdentifiableObject identifiableObject, IdentifiableProperty identifiableProperty )
    {
        return getValue( identifiableObject, IdScheme.from( identifiableProperty ) );
    }

    public static String getValue( IdentifiableObject identifiableObject, IdScheme idScheme )
    {
        boolean isId = idScheme.is( IdentifiableProperty.ID ) || idScheme.is( IdentifiableProperty.UID );

        if ( isId )
        {
            return identifiableObject.getUid();
        }
        else if ( idScheme.is( IdentifiableProperty.CODE ) )
        {
            return identifiableObject.getCode();
        }
        else if ( idScheme.is( IdentifiableProperty.NAME ) )
        {
            return identifiableObject.getName();
        }

        return null;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "idScheme", idScheme )
            .add( "dataElementIdScheme", dataElementIdScheme )
            .add( "categoryOptionComboIdScheme", categoryOptionComboIdScheme )
            .add( "categoryOptionIdScheme", categoryOptionIdScheme )
            .add( "orgUnitIdScheme", orgUnitIdScheme )
            .add( "programIdScheme", programIdScheme )
            .add( "programStageIdScheme", programStageIdScheme )
            .add( "trackedEntityIdScheme", trackedEntityIdScheme )
            .add( "trackedEntityAttributeIdScheme", trackedEntityAttributeIdScheme )
            .add( "dataSetIdScheme", dataSetIdScheme )
            .add( "attributeOptionComboIdScheme", attributeOptionComboIdScheme )
            .add( "programStageInstanceIdScheme", programStageInstanceIdScheme )
            .toString();
    }
}
