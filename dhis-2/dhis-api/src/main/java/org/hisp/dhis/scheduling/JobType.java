package org.hisp.dhis.scheduling;

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
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.MockJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.scheduling.parameters.SmsJobParameters;

/**
 * Enum describing the different jobs in the system.
 * Each job has a key, class, configurable status and possibly a map containing relative endpoints for possible parameters.
 * <p>
 * The key must match the jobs bean name so that the {@link SchedulingManager} can fetch the correct job
 *
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob", false, null, null, true ),
    DATA_INTEGRITY( "dataIntegrityJob", true, null, null, false ),
    RESOURCE_TABLE( "resourceTableJob", true, null, null, false ),
    ANALYTICS_TABLE( "analyticsTableJob", true, AnalyticsJobParameters.class, ImmutableMap.of(
        "skipTableTypes", "/api/analytics/tableTypes"
    ), false ),
    DATA_SYNC( "dataSynchJob", true, null, null, false ),
    PROGRAM_DATA_SYNC( "programDataSyncJob", true, null, null, false ),
    FILE_RESOURCE_CLEANUP( "fileResourceCleanUpJob", false, null, null, false ),
    META_DATA_SYNC( "metadataSyncJob", true, null, null, false ),
    SMS_SEND( "sendSmsJob", false, SmsJobParameters.class, null, true ),
    SEND_SCHEDULED_MESSAGE( "sendScheduledMessageJob", true, null, null, false ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", true, null, null, false ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", false, null, null, false ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", false, null, null, true ),
    MONITORING( "monitoringJob", true, MonitoringJobParameters.class, ImmutableMap.of(
        "relativePeriods", "/api/periodTypes/relativePeriodTypes",
        "validationRuleGroups", "/api/validationRuleGroups"
    ), true ),
    PUSH_ANALYSIS( "pushAnalysisJob", true, PushAnalysisJobParameters.class, ImmutableMap.of(
        "pushAnalysis", "/api/pushAnalysis"
    ), true ),
    PREDICTOR( "predictorJob", true, PredictorJobParameters.class, ImmutableMap.of(
        "predictors", "/api/predictors",
        "predictorGroups", "/api/predictorGroups"
    ), true ),
    DATA_SET_NOTIFICATION( "dataSetNotificationJob", false, null, null, true ),
    REMOVE_EXPIRED_RESERVED_VALUES( "removeExpiredReservedValuesJob", false, null, null, true ),
    KAFKA_TRACKER( "kafkaTrackerJob", false, null, null, false ),

    // For tests
    MOCK( "mockJob", false, MockJobParameters.class, null, true ),

    // To satifisfy code that used the old enum TaskCategory
    DATAVALUE_IMPORT( null, false, null, null, true ),
    ANALYTICSTABLE_UPDATE( null, false, null, null, true ),
    METADATA_IMPORT( null, false, null, null, true ),
    GML_IMPORT( null, false, null, null, true ),
    DATAVALUE_IMPORT_INTERNAL( null, false, null, null, true ),
    EVENT_IMPORT( null, false, null, null, true ),
    ENROLLMENT_IMPORT( null, false, null, null, true ),
    TEI_IMPORT( null, false, null, null, true ),
    LEADER_ELECTION( "leaderElectionJob", false, null, null, false ),
    LEADER_RENEWAL( "leaderRenewalJob", false, null, null, false ),
    COMPLETE_DATA_SET_REGISTRATION_IMPORT( null, false, null, null, true );

    private final String key;

    private final Class<? extends JobParameters> jobParameters;

    private final boolean configurable;

    private final boolean multipleInstancesAllowed;

    ImmutableMap<String, String> relativeApiElements;

    JobType( String key, boolean configurable, Class<? extends JobParameters> jobParameters,
        ImmutableMap<String, String> relativeApiElements, boolean multipleInstancesAllowed )
    {
        this.key = key;
        this.jobParameters = jobParameters;
        this.configurable = configurable;
        this.relativeApiElements = relativeApiElements;
        this.multipleInstancesAllowed = multipleInstancesAllowed;
    }

    public String getKey()
    {
        return key;
    }

    public Class<? extends JobParameters> getJobParameters()
    {
        return jobParameters;
    }

    public boolean isConfigurable()
    {
        return configurable;
    }

    public boolean isMultipleInstancesAllowed() {
        return multipleInstancesAllowed;
    }

    public ImmutableMap<String, String> getRelativeApiElements()
    {
        return relativeApiElements;
    }
}
