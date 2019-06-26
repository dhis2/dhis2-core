package org.hisp.dhis.sms.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// TODO: This will be moved to the compression library
public enum SMSResponse
{
    SUCCESS( 0, "Submission has been processed successfully" ),

    // Warnings (< 100)
    WARN_DVERR( 1, "There was an error with some of the data values in the submission" ),
    WARN_DVEMPTY( 2, "The submission did not include any data values" ),
    WARN_AVEMPTY( 3, "The submission did not include any attribute values" ),

    // Errors (> 100)
    UNKNOWN_ERROR( 101, "An unknown error occurred" ),
    HEADER_ERROR( 102, "An unknown error occurred reading the header of the SMS" ),
    READ_ERROR( 103, "An unknown error occurred reading the SMS submission" ),

    INVALID_USER( 201, "User [%s] does not exist" ),
    INVALID_ORGUNIT( 202, "Organisation unit [%s] does not exist" ),
    INVALID_PROGRAM( 203, "Program [%s] does not exist" ),
    INVALID_TETYPE( 204, "Tracked Entity Type [%s] does not exist" ),
    INVALID_DATASET( 205, "DataSet [%s] does not exist" ),
    INVALID_PERIOD( 206, "Period [%s] is invalid" ),
    INVALID_AOC( 207, "Attribute Option Combo [%s] does not exist" ),
    INVALID_TEI( 208, "Tracked Entity Instance [%s] does not exist" ),
    INVALID_STAGE( 209, "Program stage [%s] does not exist" ),
    INVALID_EVENT( 210, "Event [%s] does not exist" ),
    INVALID_RELTYPE( 211, "Relationship Type [%s] does not exist" ),
    INVALID_ENROLL( 212, "Enrollment [%s] does not exist" ),
    INVALID_ATTRIB( 213, "Attribute [%s] does not exist" ),

    USER_NOTIN_OU( 301, "User [%s] does not not belong to organisation unit [%s]" ),
    OU_NOTIN_PROGRAM( 302, "Organisation unit [%s] is not assigned to program [%s]" ),
    OU_NOTIN_DATASET( 303, "Organisation unit [%s] is not assigned to dataSet [%s]" ),
    ENROLL_FAILED( 304, "Enrollment of TEI [%s] in program [%s] failed" ),
    DATASET_LOCKED( 305, "Dataset [%s] is locked for period [%s]" ),
    MULTI_PROGRAMS( 306, "Multiple active program instances exists for program [%s]" ),
    MULTI_STAGES( 307, "Multiple program stages found for event capture program [%s]" ),
    NO_ENROLL( 308, "No enrollment was found for tracked entity instance [%s] in program stage [%s]" ),
    NULL_ATTRIBVAL( 309, "Value for attribute [%s] was null" ),

    ;

    private final int code;

    private String description;

    private List<String> uids;

    private SMSResponse( int code, String description )
    {
        this.code = code;
        this.description = description;
        this.uids = new ArrayList<String>();
    }

    public SMSResponse set( String... uids )
    {
        this.uids = Arrays.asList( uids );
        this.description = String.format( description, (Object[]) uids );
        return this;
    }

    public SMSResponse set( List<String> uids )
    {
        this.uids = uids;
        this.description = String.format( description, uids );
        return this;
    }

    public String getDescription()
    {
        return description;
    }

    public int getCode()
    {
        return code;
    }

    @Override
    public String toString()
    {
        String uidDelim = uids.stream().collect( Collectors.joining( "," ) );
        return code + ":" + uidDelim + ":" + description;
    }
}
