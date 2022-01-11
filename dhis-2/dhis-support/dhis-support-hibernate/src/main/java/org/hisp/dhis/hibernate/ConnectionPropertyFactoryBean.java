/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.hibernate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author Lars Helge Overland
 */
public class ConnectionPropertyFactoryBean
    implements FactoryBean<String>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private HibernateConfigurationProvider hibernateConfigurationProvider;

    public void setHibernateConfigurationProvider( HibernateConfigurationProvider hibernateConfigurationProvider )
    {
        this.hibernateConfigurationProvider = hibernateConfigurationProvider;
    }

    private String hibernateProperty;

    public void setHibernateProperty( String hibernateProperty )
    {
        this.hibernateProperty = hibernateProperty;
    }

    private String defaultValue;

    public void setDefaultValue( String defaultValue )
    {
        this.defaultValue = defaultValue;
    }

    // -------------------------------------------------------------------------
    // FactoryBean implementation
    // -------------------------------------------------------------------------

    @Override
    public String getObject()
    {
        String value = hibernateConfigurationProvider.getConfiguration().getProperty( hibernateProperty );

        return StringUtils.defaultIfEmpty( value, defaultValue );
    }

    @Override
    public Class<String> getObjectType()
    {
        return String.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
