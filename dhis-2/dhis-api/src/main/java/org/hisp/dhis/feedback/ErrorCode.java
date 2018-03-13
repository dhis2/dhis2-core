package org.hisp.dhis.feedback;

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

/**
 * E3000 - E3999: Security Errors
 * E4000 - E4999: Metadata Validation Errors
 * E5000 - E5999: Preheat Errors
 * E6000 - E6999: Metadata Import Errors
 * E7000 - E7099: Scheduling errors
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum ErrorCode
{
    /* Security Errors */
    E3000( "User `{0}` is not allowed to create objects of type {1}." ),
    E3001( "User `{0}` is not allowed to update object `{1}`." ),
    E3002( "User `{0}` is not allowed to delete object `{1}`." ),
    E3003( "User `{0}` is not allowed to grant users access to user role `{1}`." ),
    E3004( "User `{0}` is not allowed to grant users access to user groups." ),
    E3005( "User `{0}` is not allowed to grant users access to user group `{1}`." ),
    E3006( "User `{0}` is not allowed to externalize objects of type `{1}`." ),
    E3008( "User `{0}` is not allowed to make public objects of type `{1}`." ),
    E3009( "User `{0}` is not allowed to make private objects of type `{1}`." ),
    E3010( "Invalid access string `{0}`." ),
    E3011( "Data sharing is not enabled for type `{0}`, but one or more access strings contains data sharing read or write." ),

    /* Metadata Validation Errors */
    E4000( "Missing required property `{0}`." ),
    E4001( "Maximum length of property `{0}`is {1}, but given length was {2}." ),
    E4002( "Allowed length range for property `{0}` is [{1} to {2}], but given length was {3}." ),
    E4003( "Property `{0}` requires a valid email address, was given `{1}`." ),
    E4004( "Property `{0}` requires a valid URL, was given `{1}`." ),
    E4005( "Property `{0}` requires a valid password, was given `{1}`." ),
    E4006( "Property `{0}` requires a valid HEX color, was given `{1}`." ),
    E4007( "Allowed size range for collection property `{0}` is [{1} to {2}], but size given was {3}." ),
    E4008( "Allowed range for numeric property `{0}` is [{1} to {2}], but number given was {3}." ),
    E4009( "Attribute `{0}` is unique, and value `{1}` already exist." ),
    E4010( "Attribute `{0}` is not supported for type `{1}`." ),
    E4011( "Attribute `{0}` is required, but no value was found." ),
    E4012( "Attribute `{0}` contains elements of different period type than the data set it was added to" ),
    E4013( "Invalid Closing date `{0}`, must be after Opening date `{1}`"),
    E4014( "Invalid UID `{0}` for property `{1}`"),
    E4015( "Property `{0}` refers to an object that does not exist, could not find `{1}`"),
    E4016( "Object referenced by the `{0}` property is already associated with another object, value: `{1}`"),
    E4017( "RenderingType `{0}` is not supported for ValueType `{1}`"),
    E4018( "Property `{0}` must be set when property `{1}` is `{2}`"),

    /* TextPattern Errors */
    E4019( "Failed to parse pattern `{0}`. {1}"),
    E4020( "The value `{0}` does not conform to the attribute pattern `{1}`"),

    /* TextPattern for ID generation errors */
    E4021( "ID-pattern is required to have 1 generated segment (RANDOM or SEQUENTIAL)." ),
    E4022( "Pattern `{0}` does not conform to the value type `{1}`." ),

    /* Scheduling errors */
    E7000( "Failed to add/update job configuration - Another job of the same job type is already scheduled with this cron expression" ),
    E7001( "Failed to add/update job configuration - Trying to add job with continuous exection while there already is a job with continuous exectution of the same job type." ),
    E7002( "Failed to add/update job configuration - Uid does not exist" ),
    E7003( "Failed to add/update job configuration - Only interval can be configured for non configurable job type `{0}`" ),
    E7004( "Failed to add/update job configuration - Cron Expression must not be null " ),
    E7005( "Failed to add/update job configuration - Failed to validate cron expression: `{0}` " ),
    E7006( "Failed to execute job `{0}`." ),

    /* Job specific scheduling errors */
    E7010( "Failed to validate job runtime - `{0}`" ),

    /* Preheat Errors */
    E5000( "Found matching object for given reference, but import mode is CREATE. Identifier was {0}, and object was {1}." ),
    E5001( "No matching object for given reference. Identifier was {0}, and object was {1}." ),
    E5002( "Invalid reference {0} on object {1} for association `{2}`." ),
    E5003( "Property `{0}` with value `{1}` on object {2} already exists on object {3}." ),
    E5004( "Id `{0}` for type `{1}` exists on more than 1 object in the payload, removing all but the first found." );

    private String message;

    ErrorCode( String message )
    {
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }

}
