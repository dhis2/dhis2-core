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
package org.hisp.dhis.feedback;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum ErrorCode
{
    /* General */
    E1000( "API query must be specified" ),
    E1001( "API query contains an illegal string" ),
    E1002( "API version is invalid" ),

    /* Basic metadata */
    E1100( "Data element not found or not accessible: `{0}`" ),
    E1101( "Period is invalid: `{0}`" ),
    E1102( "Organisation unit not found or not accessible: `{0}`" ),
    E1103( "Category option combo not found or not accessible: `{0}`" ),
    E1104( "Attribute option combo not found or not accessible: `{0}`" ),
    E1105( "Data set not found or not accessible: `{0}`" ),
    E1106( "There are duplicate translation record for property `{0}` and locale `{1}`" ),
    E1107( "Object type `{0}` is not translatable" ),
    E1108( "Could not add item to collection: {0}" ),
    E1109( "Could not remove item from collection: {0}" ),
    E1110( "Category combo not found or not accessible: `{0}`" ),
    E1111( "Category option not found or not accessible: `{0}`" ),
    E1112( "Objects of type `{0}` not found or not accessible: `{1}`" ),
    E1113( "Object of type `{0}` not found or not accessible: `{1}`" ),
    E1114( "Data set form type must be custom: `{0}`" ),
    E1115( "Data element value type must match option set value type: `{0}`" ),
    E1116( "Data element of value type multi text must have an option set: `{0}`" ),
    E1117(
        "Data element `{0}` of value type multi text cannot use an option set `{1}` that uses the separator character in one of its codes: `{2}`" ),

    /* Org unit merge */
    E1500( "At least two source orgs unit must be specified" ),
    E1501( "Target org unit must be specified" ),
    E1502( "Target org unit cannot be a source org unit" ),
    E1503( "Source org unit does not exist: `{0}`" ),
    E1504( "Target org unit cannot be a descendant of a source org unit" ),

    /* Org unit split */
    E1510( "Source org unit must be specified" ),
    E1511( "At least two target org units must be specified" ),
    E1512( "Source org unit cannot be a target org unit" ),
    E1513( "Primary target must be specified" ),
    E1514( "Primary target must be a target org unit" ),
    E1515( "Target org unit does not exist: `{0}`" ),
    E1516( "Target org unit cannot be a descendant of the source org unit: `{0}`" ),

    /* Org unit move */
    E1520( "User `{0}` is not allowed to move organisation units" ),
    E1521( "User `{0}` is not allowed to move organisation `{1}`" ),
    E1522( "User `{0}` is not allowed to move organisation `{1}` unit from parent `{2}`" ),
    E1523( "User `{0}` is not allowed to move organisation `{1}` unit to parent `{2}`" ),

    /* Data */
    E2000( "Query parameters cannot be null" ),
    E2001( "At least one data element, data set or data element group must be specified" ),
    E2002( "At least one period, start/end dates, last updated or last updated duration must be specified" ),
    E2003( "Both periods and start/end date cannot be specified" ),
    E2004( "Start date must be before end date" ),
    E2005( "Duration is not valid: `{0}`" ),
    E2006( "At least one organisation unit or organisation unit group must be specified" ),
    E2007( "Organisation unit children cannot be included for organisation unit groups" ),
    E2008( "At least one organisation unit must be specified when children are included" ),
    E2009( "Limit cannot be less than zero: `{0}`" ),
    E2010( "User is not allowed to read data for data set: `{0}`" ),
    E2011( "User is not allowed to read data for attribute option combo: `{0}`" ),
    E2012( "User is not allowed to view org unit: `{0}`" ),
    E2013( "At least one data set must be specified" ),
    E2014( "Unable to parse filter `{0}`" ),
    E2015( "Unable to parse order param: `{0}`" ),
    E2016( "Unable to parse element `{0}` on filter `{1}`. The values available are: {2}" ),
    E2017( "Data set is locked" ),
    E2018( "Category option combo is required but not specified" ),
    E2019( "Organisation unit is closed for the selected period: `{0}`" ),
    E2020( "Organisation unit is not in the hierarchy of the current user: `{0}`" ),
    E2021( "Data set: `{0}` does not contain data element: `{1}`" ),
    E2022( "Period: `{0}` is after latest open future period: `{1}` for data element: `{2}`" ),
    E2023( "Period: `{0}` is before start date: {1} for attribute option: `{2}`" ),
    E2024( "Period: `{0}` is after start date: {1} for attribute option: `{2}`" ),
    E2025( "Period: `{0}` is not open for data set: `{1}`" ),
    E2026( "File resource already assigned or linked to a data value" ),
    E2027( "File resource is invalid: `{0}`" ),
    E2028( "Comment is invalid: `{0}`" ),
    E2029( "Data value is not a valid option of the data element option set: `{0}`" ),
    E2030( "Data value must match data element value type: `{0}`" ),
    E2031( "User does not have write access to category option combo: `{0}`" ),
    E2032( "Data value not found or not accessible" ),
    E2033( "Follow-up must be specified" ),
    E2034( "Filter not supported: `{0}`" ),
    E2035( "Operator not supported: `{0}`" ),
    E2036( "Combination not supported: `{0}`" ),
    E2037( "Order not supported: `{0}`" ),
    E2038( "Field not supported: `{0}`" ),
    E2039( "Stage offset is allowed only for repeatable stages (`{0}` is not repeatable)" ),
    E2040( "Both category combination and category options must be specified" ),
    E2041( "Attribute option combo does not exist for given category combo and category options" ),
    E2042( "Min value must be specified" ),
    E2043( "Max value must be specified" ),
    E2044( "Max value must be greater than min value" ),

    /* Outlier detection */
    E2200( "At least one data element must be specified" ),
    E2201( "Start date and end date must be specified" ),
    E2202( "Start date must be before end date" ),
    E2203( "At least one organisation unit must be specified" ),
    E2204( "Threshold must be a positive number" ),
    E2205( "Max results must be a positive number" ),
    E2206( "Max results exceeds the allowed max limit: `{0}`" ),
    E2207( "Data start date must be before data end date" ),
    E2208( "Non-numeric data values encountered during outlier value detection" ),

    /* Followup analysis */
    E2300( "At least one data element or data set must be specified" ),
    E2301( "Start date and end date must be specified directly or indirectly by specifying a period" ),

    /* Security */
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
    E3011( "Data sharing is not enabled for type `{0}`, but access strings contain data sharing read or write." ),
    E3012( "User `{0}` does not have read access for object `{1}`." ),
    E3013( "Sharing settings of system default metadata object of type `{0}` cannot be modified." ),
    E3014( "You do not have manage access to this object." ),
    E3015( "Invalid public access string: `{0}`" ),
    E3016( "Data sharing is not enabled for this object" ),
    E3017( "Invalid user group access string: `{0}`" ),
    E3018( "Invalid user access string: `{0}`" ),
    E3019( "Sharing is not enabled for this object `{0}`" ),

    /* Metadata Validation */
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
    E4013( "Invalid Closing date `{0}`, must be after Opening date `{1}`" ),
    E4014( "Invalid UID `{0}` for property `{1}`" ),
    E4015( "Property `{0}` refers to an object that does not exist, could not find `{1}`" ),
    E4016( "Object referenced by the `{0}` property is already associated with another object, value: `{1}`" ),
    E4017( "RenderingType `{0}` is not supported for ValueType `{1}`" ),
    E4018( "Property `{0}` must be set when property `{1}` is `{2}`" ),
    E4019( "Failed to parse pattern `{0}`. {1}" ),
    E4020( "The value `{0}` does not conform to the attribute pattern `{1}`" ),
    E4021( "ID-pattern is required to have 1 generated segment (RANDOM or SEQUENTIAL)." ),
    E4022( "Pattern `{0}` does not conform to the value type `{1}`." ),
    E4023( "Property `{0}` can not be set when property `{1}` is `{2}`. " ),
    E4024( "Property `{0}` must be set when property `{1}` is `{2}`. " ),
    E4025( "Properties `{0}` and `{1}` are mutually exclusive and cannot be used together." ),
    E4026( "One of the properties `{0}` and `{1}` is required when property `{2}` is `{3}`." ),
    E4027( "Value `{0}` is not a valid for property `{1}`" ),
    E4028( "Option set `{0}` already contains option `{1}`" ),
    E4029( "Job parameters cannot be null for job type: {0}" ),
    E4030( "Object could not be deleted because it is associated with another object: {0}" ),
    E4031( "Property `{0}` requires a valid JSON payload, was given `{1}`." ),
    E4032( "Patch path `{0}` is not supported." ),

    /* ProgramRuleAction validation */
    E4033( "A program rule action of type `{0}` associated with program rule name `{1}` is invalid" ),
    E4034( "ProgramNotificationTemplate `{0}` associated with program rule name `{1}` does not exist" ),
    E4035( "ProgramNotificationTemplate cannot be null for program rule name `{0}`" ),
    E4036( "ProgramStageSection cannot be null for program rule `{0}`" ),
    E4037( "ProgramStageSection `{0}` associated with program rule `{1}` does not exist" ),
    E4038( "ProgramStage cannot be null for program rule `{0}`" ),
    E4039( "ProgramStage `{0}` associated with program rule `{1}` does not exist" ),
    E4040( "Option cannot be null for program rule `{0}`" ),
    E4041( "Option `{0}` associated with program rule `{1}` does not exist" ),
    E4042( "OptionGroup cannot be null for program rule `{0}`" ),
    E4043( "OptionGroup `{0}` associated with program rule `{1}` does not exist" ),
    E4044( "DataElement or TrackedEntityAttribute cannot be null for program rule `{0}`" ),
    E4045( "DataElement `{0}` associated with program rule `{1}` does not exist" ),
    E4046( "TrackedEntityAttribute `{0}` associated with program rule `{1}` does not exist" ),
    E4047( "DataElement `{0}` is not linked to any ProgramStageDataElement for program rule `{1}`" ),
    E4048( "TrackedEntityAttribute `{0}` is not linked to ProgramTrackedEntityAttribute for program rule `{1}`" ),
    E4049( "Property `{0}` requires a valid username, was given `{1}`." ),
    E4054( "Property `{0}` already exists, was given `{1}`." ),
    E4056( "Property `{0}` can not be changed, was given `{1}`." ),
    E4055( "An user needs to have at least one user role associated with it." ),
    E4050( "One of DataElement, TrackedEntityAttribute or ProgramRuleVariable is required for program rule `{0}`" ),

    /* ProgramRuleVariable validation */
    E4051( "A program rule variable with name `{0}` and program uid `{1}` already exists" ),
    E4052( "For program rule variable with name `{0}` following keywords are forbidden : and , or , not" ),
    E4053( "Program stage `{0}` must reference a program." ),

    /* SQL views */
    E4300( "SQL query is null" ),
    E4301( "SQL query must be a select query" ),
    E4302( "SQL query can only contain a single semi-colon at the end of the query" ),
    E4303( "Variables contain null key" ),
    E4304( "Variables contain null value" ),
    E4305( "Variable params are invalid: `{0}`" ),
    E4306( "Variables are invalid: `{0}`" ),
    E4307( "SQL query contains variables not provided in request: `{0}`" ),
    E4308( "Criteria params are invalid: `{0}`" ),
    E4309( "Criteria values are invalid: `{0}`" ),
    E4310( "SQL query contains references to protected tables" ),
    E4311( "SQL query contains illegal keywords" ),
    E4312( "Current user is not authorised to read data from SQL view: `{0}`" ),
    E4313( "SQL query contains variable names that are invalid: `{0}`" ),
    E4314( "Provided `{0}`: (`{1}`) are not part of the selected `{2}`" ),
    E4315( "Provided Program: (`{0}`) is without registration" ),

    /* Preheat */
    E5000( "Found matching object for reference, but import mode is CREATE. Identifier was {0}, and object was {1}." ),
    E5001( "No matching object for reference. Identifier was {0}, and object was {1}." ),
    E5002( "Invalid reference {0} on object {1} for association `{2}`." ),
    E5003( "Property `{0}` with value `{1}` on object {2} already exists on object {3}." ),
    E5004( "Id `{0}` for type `{1}` exists on more than 1 object in the payload, removing all but the first found." ),
    E5005( "Properties `{0}` in objects `{1}` must be unique within the payload" ),
    E5006( "Non-owner reference {0} on object {1} for association `{2}` disallowed for payload for ERRORS_NOT_OWNER" ),

    /* Metadata import */
    E6000( "Program `{0}` has more than one Program Instances" ),
    E6001( "ProgramStage `{0}` has invalid next event scheduling property `{1}`. " +
        "This property need to be data element of value type date and belong the program stage." ),
    E6002( "Class name {0} is not supported." ),
    E6003( "Could not patch object with id {0}." ),
    E6004( "Attribute `{0}` has invalid GeoJson value." ),
    E6005( "Attribute `{0}` has unsupported GeoJson value." ),

    /* File resource */
    E6100( "Filename not present" ),
    E6101( "File type not allowed" ),

    /* Users */
    E6200( "Feedback message recipients user group not defined" ),
    E6201( "User account not found" ),
    E6202( "User account does not have a valid email address" ),
    E6203( "SMTP server/email sending is not available" ),
    E6204( "Username is already taken" ),
    E6205( "Restore token does not exist" ),
    E6206( "Restore type does not exist" ),
    E6207( "Restore token is not in valid format" ),
    E6208( "Restore token is incorrect" ),
    E6209( "Restore token is not set for user account" ),
    E6210( "Restore expiration date is not set for user account" ),
    E6211( "User account restore invitation has expired" ),

    /* Data exchange */
    E6300( "DHIS 2 client request failed: {0} {1}" ),
    E6301( "Analytics data exchange not found or not accessible: `{0}`" ),

    /* Scheduling */
    E7000( "Job of same type already scheduled with cron expression: `{0}`" ),
    E7003( "Only interval property can be configured for non configurable job type: `{0}`" ),
    E7004( "Cron expression must be not null for job with scheduling type CRON: `{0}`" ),
    E7005( "Cron expression is invalid for job: `{0}` " ),
    E7006( "Failed to execute job: `{0}`." ),
    E7007( "Delay must be not null for job with scheduling type FIXED_DELAY: `{0}`" ),
    E7010( "Failed to validate job runtime: `{0}`" ),

    /* Aggregate analytics */
    E7100( "Query parameters cannot be null" ),
    E7101( "At least one dimension must be specified" ),
    E7102( "At least one data dimension item or data element group set dimension item must be specified" ),
    E7103( "Dimensions cannot be specified as dimension and filter simultaneously: `{0}`" ),
    E7104( "At least one period as dimension or filter, or start and dates, must be specified" ),
    E7105( "Periods and start and end dates cannot be specified simultaneously" ),
    E7106( "Start date cannot be after end date" ),
    E7107( "Start and end dates cannot be specified for reporting rates" ),
    E7108( "Only a single indicator can be specified as filter" ),
    E7109( "Only a single reporting rate can be specified as filter" ),
    E7110( "Category option combos cannot be specified as filter" ),
    E7111( "Dimensions cannot be specified more than once: `{0}`" ),
    E7112( "Reporting rates can only be specified together with dimensions of type: `{0}`" ),
    E7113( "Assigned categories cannot be specified when data elements are not specified" ),
    E7114( "Assigned categories can only be specified together with data elements" ),
    E7115( "Data elements must be of a value and aggregation type that allow aggregation: `{0}`" ),
    E7116( "Indicator expressions cannot contain cyclic references: `{0}`" ),
    E7117( "A data dimension 'dx' must be specified when output format is DATA_VALUE_SET" ),
    E7118( "A period dimension 'pe' must be specified when output format is DATA_VALUE_SET" ),
    E7119( "An organisation unit dimension 'ou' must be specified when output format is DATA_VALUE_SET" ),
    E7120( "User: `{0}` is not allowed to view org unit: `{1}`" ),
    E7121( "User: `{0}` is not allowed to read data for `{1}`: `{2}`" ),
    E7122( "Data approval level does not exist: `{0}`" ),
    E7123( "Current user is constrained by a dimension but has access to no dimension items: `{0}`" ),
    E7124( "Dimension is present in query without any valid dimension options: `{0}`" ),
    E7125( "Dimension identifier does not reference any dimension: `{0}`" ),
    E7126( "Column must be present as dimension in query: `{0}`" ),
    E7127( "Row must be present as dimension in query: `{0}`" ),
    E7128( "Query result set exceeded max limit: `{0}`" ),
    E7129( "Program is specified but does not exist: `{0}`" ),
    E7130( "Program stage is specified but does not exist: `{0}`" ),
    E7131( "Query failed, likely because the query timed out" ),
    E7132( "An indicator expression caused division by zero operation" ),
    E7133( "Query cannot be executed, possibly because of invalid types or invalid operation" ),
    E7134( "Cannot retrieve total value for data elements with skip total category combination" ),
    E7135( "Date time is not parsable: `{0}`" ),

    /* Event analytics */
    E7200( "At least one organisation unit must be specified" ),
    E7201( "Dimensions cannot be specified more than once: `{0}`" ),
    E7202( "Query items cannot be specified more than once: `{0}`" ),
    E7203( "Value dimension cannot also be specified as an item or item filter" ),
    E7204( "Value dimension or aggregate data must be specified when aggregation type is specified" ),
    E7205( "Start and end date or at least one period must be specified" ),
    E7206( "Start date is after end date: `{0}`, `{1}`" ),
    E7207( "Page number must be a positive number: `{0}`" ),
    E7208( "Page size must be zero or a positive number: `{0}`" ),
    E7209( "Limit is larger than max limit: `{0}`, `{1}`" ),
    E7210( "Time field is invalid: `{0}`" ),
    E7211( "Org unit field is invalid: `{0}`" ),
    E7212( "Cluster size must be a positive number: `{0}`" ),
    E7213( "Bbox is invalid, must be on format: 'min-lng,min-lat,max-lng,max-lat': `{0}`" ),
    E7214( "Cluster field must be specified when bbox or cluster size are specified" ),
    E7215( "Query item cannot specify both legend set and option set: `{0}`" ),
    E7216( "Query item must be aggregateable when used in aggregate query: `{0}`" ),
    E7217( "User is not allowed to view event analytics data: `{0}`" ),
    E7218( "Spatial database support is not enabled" ),
    E7219( "Data element must be of value type coordinate or org unit to be used as coordinate field: `{0}`" ),
    E7220( "Attribute must be of value type coordinate or org unit to be used as coordinate field: `{0}`" ),
    E7221( "Coordinate field is invalid: `{0}`" ),
    E7222( "Query item or filter is invalid: `{0}`" ),
    E7223( "Value does not refer to a data element or attribute which are numeric and part of the program: `{0}`" ),
    E7224( "Item identifier does not reference any data element, attribute or indicator part of the program: `{0}`" ),
    E7225( "Program stage is mandatory for data element dimensions in enrollment analytics queries: `{0}`" ),
    E7226( "Dimension is not a valid query item: `{0}`" ),
    E7227( "Relationship entity type not supported: `{0}`" ),
    E7228( "Fallback coordinate field is invalid: `{0}` " ),
    E7229( "Operator `{0}` does not allow missing value" ),
    E7230( "Header param `{0}` does not exist" ),
    E7231( "Legacy `{0}` can be updated only through event visualizations" ),

    /* Org unit analytics */
    E7300( "At least one organisation unit must be specified" ),
    E7301( "At least one organisation unit group set must be specified" ),

    /* Debug analytics */
    E7400( "Debug query must contain at least one data element, one period and one organisation unit" ),

    /* Validation Results API */
    E7500( "Organisation unit does not exist: `{0}`" ),
    E7501( "Validation rule does not exist: `{0}`" ),
    E7502( "Filter for period is not valid: `{0}`" ),
    E7503( "Filter for created date period is not valid: `{0}`" ),

    /* Data import validation */
    // Data Set validation
    E7600( "Data set not found or not accessible: `{0}`" ),
    E7601( "User does not have write access for DataSet: `{0}`" ),
    E7602( "A valid dataset is required" ),
    E7603( "Org unit not found or not accessible: `{0}`" ),
    E7604( "Attribute option combo not found or not accessible: `{0}`" ),
    // Data Value validation
    E7610( "Data element not found or not accessible: `{0}`" ),
    E7611( "Period not valid: `{0}`" ),
    E7612( "Organisation unit not found or not accessible: `{0}`" ),
    E7613( "Category option combo not found or not accessible for writing data: `{0}`" ),
    E7614( "Category option combo: `{0}` option not accessible: `{1}`" ),
    E7615( "Attribute option combo not found or not accessible for writing data: `{0}`" ),
    E7616( "Attribute option combo: `{0}` option not accessible: `{1}`" ),
    E7617( "Organisation unit: `{0}` not in hierarchy of current user: `{1}`" ),
    E7618( "Data value or comment not specified for data element: `{0}`" ),
    E7619( "Value must match data element''s `{0}` type constraints: {1}" ),
    E7620( "Invalid comment: {0}" ),
    E7621( "Data value is not a valid option of the data element option set: `{0}`" ),
    // Data Value constraints
    E7630( "Category option combo is required but is not specified" ),
    E7631( "Attribute option combo is required but is not specified" ),
    E7632( "Period type of period: `{0}` not valid for data element: `{1}`" ),
    E7633( "Data element: `{0}` is not part of dataset: `{1}`" ),
    E7634( "Category option combo: `{0}` must be part of category combo of data element: `{1}`" ),
    E7635( "Attribute option combo: `{0}` must be part of category combo of data sets of data element: `{1}`" ),
    E7636( "Data element: `{1}` must be assigned through data sets to organisation unit: `{0}`" ),
    E7637( "Invalid storedBy: {0}" ),
    E7638( "Period: `{0}` is not within date range of attribute option combo: `{1}`" ),
    E7639( "Organisation unit: `{0}` is not valid for attribute option combo: `{1}`" ),
    E7640( "Current date is past expiry days for period: `{0}`  and data set: `{1}`" ),
    E7641( "Period: `{0}` is after latest open future period: `{2}` for data element: `{1}`" ),
    E7642( "Data already approved for data set: `{3}` period: `{1}` org unit: `{0}` attribute option combo: `{2}`" ),
    E7643( "Period: `{0}` is not open for this data set at this time: `{1}`" ),
    E7644( "Period: `{0}` does not conform to the open periods of associated data sets" ),
    E7645( "No data value for file resource exist for the given combination for data element: `{0}`" ),

    /* Data store query validation */
    E7650( "Not a valid path: `{0}`" ),
    E7651( "Illegal fields expression. Expected `,`, `[` or `]` at position {0} but found `{1}`" ),
    E7652( "Illegal filter expression `{0}`: {1}" ),
    E7653( "Illegal filter `{0}`: {1}" ),

    /* GeoJSON import validation and conflicts */
    E7700( "Error reading JSON input: {0}" ),
    E7701( "Input is not a valid GeoJSON document: {0}" ),
    E7702( "GeoJSON attribute does not exist: {0}" ),
    E7703( "GeoJSON attribute is not of type {0} but: {1}" ),
    E7704( "GeoJSON attribute is not applicable to organisation units" ),
    E7705( "GeoJSON feature lacks identifier property: `{0}`" ),
    E7706( "GeoJSON feature lacks geometry property" ),
    E7707( "GeoJSON geometry is not valid" ),
    E7708( "GeoJSON target organisation unit does not exist" ),
    E7709( "Organisation unit could not be updated with new GeoJSON geometry" ),
    E7710( "User is not allowed to update the target organisation unit" ),
    E7711( "Organisation unit cannot be uniquely identified by its name" );

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
