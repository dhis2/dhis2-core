<<<<<<< HEAD
package org.hisp.dhis.mobile.action;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.sms.outbound.OutboundSmsService;

import com.opensymphony.xwork2.Action;

/**
 * @author Nguyen Kim Lai
 *
 * @version $ DeleteSentSMSAction.java Oct 16, 2012 $
 */
public class DeleteSentSMSAction implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    
    private OutboundSmsService outboundSmsService;
    
    public void setOutboundSmsService( OutboundSmsService outboundSmsService )
    {
        this.outboundSmsService = outboundSmsService;
    }    
    
    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------
    
    private Integer[] ids;

    public void setIds( Integer[] ids )
    {
        this.ids = ids;
    }
    
    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }
    
    @Override
    public String execute()
        throws Exception
    {
        if ( ids != null && ids.length > 0 )
        {
            for ( Integer each : ids )
            {
                outboundSmsService.deleteById( each );
            }
        }
        if ( id != null )
        {
            outboundSmsService.deleteById( id );
        }
        return SUCCESS;

    }

    public Integer[] getIds()
    {
        return ids;
    }

    public Integer getId()
=======
/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.mobile.action;

import org.hisp.dhis.sms.outbound.OutboundSmsService;

import com.opensymphony.xwork2.Action;

/**
 * @author Nguyen Kim Lai
 *
 * @version $ DeleteSentSMSAction.java Oct 16, 2012 $
 */
public class DeleteSentSMSAction implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OutboundSmsService outboundSmsService;

    public void setOutboundSmsService( OutboundSmsService outboundSmsService )
    {
        this.outboundSmsService = outboundSmsService;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Long[] ids;

    public void setIds( Long[] ids )
    {
        this.ids = ids;
    }

    private Long id;

    public void setId( Long id )
    {
        this.id = id;
    }

    @Override
    public String execute()
        throws Exception
    {
        if ( ids != null && ids.length > 0 )
        {
            for ( long each : ids )
            {
                outboundSmsService.delete( each );
            }
        }
        if ( id != null )
        {
            outboundSmsService.delete( id );
        }
        return SUCCESS;

    }

    public Long[] getIds()
    {
        return ids;
    }

    public Long getId()
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
        return id;
    }
}
