package org.hisp.dhis.sms.listener;

import java.util.function.Consumer;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.ImmutableMap;

@Transactional
public abstract class AbstractSMSListener implements IncomingSmsListener
{
    /**
     * TODO:
     * Include the user service
     * Include the program service
     *
     */

    private static final Log log = LogFactory.getLog( AbstractSMSListener.class );

    private static final String DEFAULT_PATTERN =  "([^\\s|=]+)\\s*\\=\\s*([^|=]+)\\s*(\\=|$)*\\s*";
    private static final String NO_SMS_CONFIG = "No sms configuration found";

    protected static final int INFO = 1;
    protected static final int WARNING = 2;
    protected static final int ERROR = 3;

    private static final ImmutableMap<Integer, Consumer<String>> LOGGER = new ImmutableMap.Builder<Integer, Consumer<String>>()
            .put( 1, log::info )
            .put( 2, log::warn )
            .put( 3, log::error )
            .build();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    protected OrganisationUnitService organizationUnitService;

    @Autowired
    protected CategoryService dataElementCategoryService;

    @Autowired
    private IncomingSmsService incomingSmsService;

    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;


    @Override
    public boolean accept( IncomingSms sms )
    {
        if ( sms == null )
        {
            return false;
        }


        return isAcceptable(sms);
    }

    public abstract boolean isAcceptable (IncomingSms message);

    @Override
    public void receive( IncomingSms sms )
    {
        postProcess( sms );

        //markCompleteDataSet( sms, orgUnit, parsedMessage, smsCommand, date );
        //sendSuccessFeedback( senderPhoneNumber, smsCommand, parsedMessage, date, orgUnit );

        //update( sms, SmsMessageStatus.PROCESSED, true );
    }

    protected abstract void postProcess( IncomingSms sms );

    protected void sendFeedback( String message, String sender, int logType )
    {
        LOGGER.getOrDefault( logType, log::info ).accept( message );

        if( smsSender.isConfigured() )
        {
            smsSender.sendMessage( null, message, sender );
            return;
        }

        LOGGER.getOrDefault( WARNING, log::info ).accept(  NO_SMS_CONFIG );
    }



    protected void update( IncomingSms sms, SmsMessageStatus status, boolean parsed )
    {
        sms.setStatus( status );
        sms.setParsed( parsed );

        incomingSmsService.update( sms );
    }

    /**
     * @param organizationUnitService the organizationUnitService to set
     */
    public void setOrganizationUnitService(OrganisationUnitService organizationUnitService) {
        this.organizationUnitService = organizationUnitService;
    }

    /**
     * @param dataElementCategoryService the dataElementCategoryService to set
     */
    public void setDataElementCategoryService(CategoryService dataElementCategoryService) {
        this.dataElementCategoryService = dataElementCategoryService;
    }

    /**
     * @param incomingSmsService the incomingSmsService to set
     */
    public void setIncomingSmsService(IncomingSmsService incomingSmsService) {
        this.incomingSmsService = incomingSmsService;
    }

}
