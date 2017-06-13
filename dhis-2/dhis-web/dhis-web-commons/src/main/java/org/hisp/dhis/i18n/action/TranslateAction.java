package org.hisp.dhis.i18n.action;

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

import com.opensymphony.xwork2.Action;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.translation.ObjectTranslation;
import org.hisp.dhis.translation.TranslationProperty;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static org.hisp.dhis.common.IdentifiableObjectUtils.CLASS_ALIAS;

/**
 * @author Oyvind Brucker
 * @modifier Dang Duy Hieu
 */
public class TranslateAction
    implements Action
{
    private static final Log log = LogFactory.getLog( TranslateAction.class );

    private String className;

    private String uid;

    private String loc;

    private String returnUrl;

    private String message;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private IdentifiableObjectManager identifiableObjectManager;

    public void setIdentifiableObjectManager( IdentifiableObjectManager identifiableObjectManager )
    {
        this.identifiableObjectManager = identifiableObjectManager;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    public void setClassName( String className )
    {
        this.className = className;
    }

    public void setUid( String uid )
    {
        this.uid = uid;
    }

    public void setLoc( String locale )
    {
        this.loc = locale;
    }

    public void setReturnUrl( String returnUrl )
    {
        this.returnUrl = returnUrl;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    public String getClassName()
    {
        return className;
    }

    public String getUid()
    {
        return uid;
    }

    public String getLocale()
    {
        return loc;
    }

    public String getReturnUrl()
    {
        return returnUrl;
    }

    public String getMessage()
    {
        return message;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        className = className != null && CLASS_ALIAS.containsKey( className ) ? CLASS_ALIAS.get( className ) : className;
        
        log.info( "Classname: " + className + ", uid: " + uid + ", loc: " + loc );

        IdentifiableObject object = identifiableObjectManager.getObject( uid , className );

        HttpServletRequest request = ServletActionContext.getRequest();

        Set<ObjectTranslation> listObjectTranslation = new HashSet<>(object.getTranslations());

        for ( TranslationProperty p :  TranslationProperty.values()  )
        {
            Enumeration<String> paramNames = request.getParameterNames();

            Collections.list( paramNames ).forEach( paramName -> {

                if ( paramName.equalsIgnoreCase( p.getName() ) )
                {
                    String[] paramValues = request.getParameterValues( paramName );

                    if ( !ArrayUtils.isEmpty( paramValues ) && StringUtils.isNotEmpty( paramValues[0]) )
                    {
                        listObjectTranslation.removeIf( o -> o.getProperty().equals( p ) && o.getLocale().equalsIgnoreCase( loc )  );

                        listObjectTranslation.add( new ObjectTranslation( loc, p, paramValues[0] ) );
                    }
                }
            });
        }

        identifiableObjectManager.updateTranslations( object, listObjectTranslation );

        return SUCCESS;
    }
}
