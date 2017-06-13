package org.hisp.dhis.sms.outbound;

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

/**
 * Zubair <rajazubair.asghar@gmail.com>
 */

public enum GatewayResponse
{
    SUCCESS( "success", "" ), 
    SENT( "success", "" ), 
    FAILED( "failed", "" ), 
    PENDING( "pending", "" ), 
    SERVICE_NOT_AVAILABLE( "service not available", "" ), 
    ENCODING_FAILURE( "encoding failed", "" ), 
    PROCESSING( "processing", "" ), 
    QUEUED( "queued" ,"" ), 
    NO_GATEWAY_CONFIGURATION( "no gateway configuration found", "" ),
    NO_DEFAULT_GATEWAY( "no gateway is set to default", "" ),
    AUTHENTICATION_FAILED( "authentication failed", "" ),
    
    // -------------------------------------------------------------------------
    // BulkSms response codes
    // -------------------------------------------------------------------------
    
    RESULT_CODE_0( "success", "" ),
    RESULT_CODE_1( "scheduled", "" ),
    RESULT_CODE_22( "internal fatal error", "" ),
    RESULT_CODE_23( "authentication failure", "" ),
    RESULT_CODE_24( "data validation failed", "" ),
    RESULT_CODE_25( "you do not have sufficient credits", "" ),
    RESULT_CODE_26( "upstream credits not available", "" ),
    RESULT_CODE_27( "you have exceeded your daily quota", "" ),
    RESULT_CODE_28( "pstream quota exceeded", "" ),
    RESULT_CODE_40( "temporarily unavailable", "" ),
    RESULT_CODE_201( "maximum batch size exceeded", "" ),
    
    // -------------------------------------------------------------------------
    // Clickatell response codes
    // -------------------------------------------------------------------------
    
    RESULT_CODE_200( "success", "The request was successfully completed" ),
    RESULT_CODE_202( "accepted", "The message(s) will be processed" ),
    RESULT_CODE_207( "multi-status", "More than  one message was submitted to the API; however, not all messages have the same status" ),
    RESULT_CODE_400( "bad request", "Validation failure (such as missing/invalid parameters or headers)" ),
    RESULT_CODE_401( "unauthorized", "Authentication failure. This can also be caused by IP lockdown settings" ),
    RESULT_CODE_402( "payment required", "Not enough credit to send message" ),
    RESULT_CODE_404( "not found", "Resource does not exist" ),
    RESULT_CODE_405( "method not allowed", "Http method is not support on the resource" ),
    RESULT_CODE_410( "gone", "Mobile number is blocked" ),
    RESULT_CODE_429( "too many requests", "Generic rate limiting error" ),
    RESULT_CODE_503( "service unavailable", "A temporary error has occurred on our platform, please retry" ),
    RESULT_CODE_504( "Internal server exception", "Internal server exception" );

    private final String responseMessage;

    private final String responseMessageDetail;
    
    private String batchId;

    GatewayResponse( String responseMessage, String responseMessageDetail )
    {
        this.responseMessage = responseMessage;
        this.responseMessageDetail = responseMessageDetail;
    }

    public String getResponseMessage()
    {
        return responseMessage;
    }
    
    public String getResponseMessageDetail()
    {
        return responseMessageDetail;
    }
    
    public String getBatchId()
    {
        return batchId;
    }

    public void setBatchId( String batchId )
    {
        this.batchId = batchId;
    }
}

