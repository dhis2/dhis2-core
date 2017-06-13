package org.hisp.dhis.security.vote;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.sqlview.SqlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows certain type/uid combinations to be externally accessed (no login required).
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ExternalAccessVoter implements AccessDecisionVoter<FilterInvocation>
{
    private static final Log LOG = LogFactory.getLog( ExternalAccessVoter.class );

    // this should probably be moved somewhere else, but leaving it here for now
    private static final Map<String, Class<? extends IdentifiableObject>> externalClasses = new HashMap<>();

    static
    {
        externalClasses.put( "charts", Chart.class );
        externalClasses.put( "maps", org.hisp.dhis.mapping.Map.class );
        externalClasses.put( "reportTables", ReportTable.class );
        externalClasses.put( "reports", Report.class );
        externalClasses.put( "documents", Document.class );
        externalClasses.put( "sqlViews", SqlView.class );
    }

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private IdentifiableObjectManager manager;

    // -------------------------------------------------------------------------
    // AccessDecisionVoter Implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean supports( ConfigAttribute attribute )
    {
        return false;
    }

    @Override
    public boolean supports( Class<?> clazz )
    {
        return clazz.isAssignableFrom( FilterInvocation.class );
    }

    @Override
    public int vote( Authentication authentication, FilterInvocation filterInvocation, Collection<ConfigAttribute> attributes )
    {
        if ( authentication.getPrincipal().equals( "anonymousUser" ) && authentication.isAuthenticated() &&
            filterInvocation.getRequest().getMethod().equals( RequestMethod.GET.name() ) )
        {
            String requestUrl = filterInvocation.getRequestUrl();
            String[] urlSplit = requestUrl.split( "/" );

            if ( urlSplit.length > 3 )
            {
                String type = urlSplit[2];

                if ( urlSplit[1].equals( "api" ) && externalClasses.get( type ) != null )
                {
                    String uid = getUidPart( urlSplit[3] );

                    if ( CodeGenerator.isValidUid( uid ) )
                    {
                        IdentifiableObject identifiableObject = manager.get( externalClasses.get( type ), uid );

                        if ( identifiableObject != null && identifiableObject.getExternalAccess() )
                        {
                            LOG.debug( "ACCESS_GRANTED [" + filterInvocation.toString() + "]" );

                            return ACCESS_GRANTED;
                        }
                    }
                }
            }
        }

        LOG.debug( "ACCESS_ABSTAIN [" + filterInvocation.toString() + "]: No supported attributes." );

        return ACCESS_ABSTAIN;
    }

    private String getUidPart( String uidPath )
    {
        if ( uidPath.contains( "." ) )
        {
            return uidPath.substring( 0, uidPath.indexOf( "." ) );
        }

        return uidPath;
    }
}
