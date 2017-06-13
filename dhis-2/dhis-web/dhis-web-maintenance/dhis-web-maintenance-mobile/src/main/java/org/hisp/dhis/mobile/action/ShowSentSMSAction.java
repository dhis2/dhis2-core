package org.hisp.dhis.mobile.action;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;

public class ShowSentSMSAction
    extends ActionPagingSupport<OutboundSms>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OutboundSmsService outboundSmsService;

    public void setOutboundSmsService( OutboundSmsService outboundSmsService )
    {
        this.outboundSmsService = outboundSmsService;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    public UserService getUserService()
    {
        return userService;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private List<OutboundSms> listOutboundSMS;

    public List<OutboundSms> getListOutboundSMS()
    {
        return listOutboundSMS;
    }

    private Integer filterStatusType;

    public Integer getFilterStatusType()
    {
        return filterStatusType;
    }

    public void setFilterStatusType( Integer filterStatusType )
    {
        this.filterStatusType = filterStatusType;
    }

    private List<String> recipientNames;

    public List<String> getRecipientNames()
    {
        return recipientNames;
    }

    private Integer total;

    public Integer getTotal()
    {
        return total;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        listOutboundSMS = new ArrayList<>();

        if ( filterStatusType != null && filterStatusType == 0 )
        {
            total = outboundSmsService.getOutboundSms( OutboundSmsStatus.OUTBOUND ).size();
            paging = createPaging( total );
            listOutboundSMS = outboundSmsService.getOutboundSms( OutboundSmsStatus.OUTBOUND, paging.getStartPos(),
                paging.getPageSize() );
        }
        if ( filterStatusType != null && filterStatusType == 1 )
        {
            total = outboundSmsService.getOutboundSms( OutboundSmsStatus.SENT ).size();
            paging = createPaging( total );
            listOutboundSMS = outboundSmsService.getOutboundSms( OutboundSmsStatus.SENT, paging.getStartPos(),
                paging.getPageSize() );
        }
        if ( filterStatusType != null && filterStatusType == 2 )
        {
            total = outboundSmsService.getOutboundSms( OutboundSmsStatus.ERROR ).size();
            paging = createPaging( total );
            listOutboundSMS = outboundSmsService.getOutboundSms( OutboundSmsStatus.ERROR, paging.getStartPos(),
                paging.getPageSize() );
        }
        if ( filterStatusType != null && filterStatusType == 3 || filterStatusType == null )
        {
            filterStatusType = 3;
            total = outboundSmsService.getAllOutboundSms().size();
            paging = createPaging( total );
            listOutboundSMS = outboundSmsService.getAllOutboundSms( paging.getStartPos(), paging.getPageSize() );
        }

        // Get the name of recipients
        recipientNames = new ArrayList<>();
        recipientNames.add( "" );
        String tempString;
        for ( OutboundSms outboundSms : listOutboundSMS )
        {
            tempString = "";
            for ( String phoneNumber : outboundSms.getRecipients() )
            {
                Collection<User> users = userService.getUsersByPhoneNumber( phoneNumber );
                if ( users == null || users.size() == 0 )
                {
                    tempString += "[unknown]";
                }
                else if ( users.size() > 0 )
                {

                    Iterator<User> usersIterator = users.iterator();
                    while ( usersIterator.hasNext() )
                    {
                        User user = usersIterator.next();
                        tempString += "[" + user.getUsername() + "]";
                    }
                }
            }
            recipientNames.add( tempString );
        }

        return SUCCESS;
    }

}
