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
package org.hisp.dhis.dataset.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.notification.SendStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zubair@dhis2.org on 28.07.17.
 */
class DataSetNotificationTemplateServiceTest extends DhisSpringTest
{

    @Autowired
    private DataSetNotificationTemplateService dsntService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserGroupService userGroupService;

    private Set<DataSet> dataSets = new HashSet<>();

    private Set<User> users = new HashSet<>();

    private Set<DeliveryChannel> channels = new HashSet<>();

    private DataSet dataSetA;

    private DataSet dataSetB;

    private DataSet dataSetC;

    private User userA;

    private User userB;

    private DeliveryChannel smsChannel = DeliveryChannel.SMS;

    private DeliveryChannel emailChannel = DeliveryChannel.EMAIL;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private UserGroup userGroupA;

    private UserGroup userGroupB;

    private DataSetNotificationRecipient notificationRecipient;

    private DataSetNotificationTrigger completion = DataSetNotificationTrigger.DATA_SET_COMPLETION;

    private DataSetNotificationTemplate templateA;

    private DataSetNotificationTemplate templateB;

    private String templateNameA = "TemplateA";

    private String templateNameB = "TemplateB";

    private String message = "message";

    private String subject = "subject";

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------
    @Override
    protected void setUpTest()
        throws Exception
    {
        ouA = createOrganisationUnit( "ouA" );
        ouB = createOrganisationUnit( "ouB" );
        ouC = createOrganisationUnit( "ouC" );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        dataSetA = createDataSet( 'A', new MonthlyPeriodType() );
        dataSetB = createDataSet( 'B', new MonthlyPeriodType() );
        dataSetC = createDataSet( 'C', new MonthlyPeriodType() );
        dataSetA.getSources().add( ouA );
        dataSetA.getSources().add( ouB );
        dataSetB.getSources().add( ouA );
        dataSetB.getSources().add( ouB );
        dataSetC.getSources().add( ouA );
        dataSetC.getSources().add( ouB );
        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );
        dataSetService.addDataSet( dataSetC );
        userA = makeUser( "A" );
        userB = makeUser( "B" );
        userService.addUser( userA );
        userService.addUser( userB );
        dataSets.addAll( Arrays.asList( dataSetA, dataSetB, dataSetC ) );
        users.addAll( Arrays.asList( userA, userB ) );
        channels.addAll( Arrays.asList( smsChannel, emailChannel ) );
        userGroupA = createUserGroup( 'A', users );
        userGroupB = createUserGroup( 'B', users );
        userGroupService.addUserGroup( userGroupA );
        userGroupService.addUserGroup( userGroupB );
        notificationRecipient = DataSetNotificationRecipient.ORGANISATION_UNIT_CONTACT;
    }

    @Test
    void testSaveGet()
    {
        templateA = new DataSetNotificationTemplate( dataSets, channels, message, notificationRecipient, completion,
            subject, userGroupA, 0, SendStrategy.SINGLE_NOTIFICATION );
        templateA.setAutoFields();
        templateA.setName( templateNameA );
        dsntService.save( templateA );
        DataSetNotificationTemplate fetched = dsntService.get( templateA.getUid() );
        assertNotNull( fetched );
        assertEquals( templateA.getUid(), fetched.getUid() );
    }

    @Test
    void testDelete()
    {
        templateA = new DataSetNotificationTemplate( dataSets, channels, message, notificationRecipient, completion,
            subject, userGroupA, 0, SendStrategy.SINGLE_NOTIFICATION );
        templateA.setAutoFields();
        templateA.setName( templateNameA );
        templateB = new DataSetNotificationTemplate( dataSets, channels, message, notificationRecipient, completion,
            subject, userGroupB, 0, SendStrategy.SINGLE_NOTIFICATION );
        templateB.setAutoFields();
        templateB.setName( templateNameB );
        dsntService.save( templateA );
        dsntService.save( templateB );
        DataSetNotificationTemplate fetchedA = dsntService.get( templateA.getUid() );
        DataSetNotificationTemplate fetchedB = dsntService.get( templateB.getUid() );
        assertNotNull( fetchedA );
        assertNotNull( fetchedB );
        dsntService.delete( templateA );
        DataSetNotificationTemplate deletedA = dsntService.get( templateA.getUid() );
        DataSetNotificationTemplate keptB = dsntService.get( templateB.getUid() );
        assertNull( deletedA );
        assertNotNull( keptB );
    }

    @Test
    void testGetAll()
    {
        templateA = new DataSetNotificationTemplate( dataSets, channels, message, notificationRecipient, completion,
            subject, userGroupA, 0, SendStrategy.SINGLE_NOTIFICATION );
        templateA.setAutoFields();
        templateA.setName( templateNameA );
        templateB = new DataSetNotificationTemplate( dataSets, channels, message, notificationRecipient, completion,
            subject, userGroupB, 0, SendStrategy.SINGLE_NOTIFICATION );
        templateB.setAutoFields();
        templateB.setName( templateNameB );
        dsntService.save( templateA );
        dsntService.save( templateB );
        List<DataSetNotificationTemplate> templates = dsntService.getAll();
        assertEquals( 2, templates.size() );
        assertTrue( templates.contains( templateA ) );
    }

    @Test
    void testGetNotificationsByTriggerType()
    {
        templateA = new DataSetNotificationTemplate( dataSets, channels, message, notificationRecipient, completion,
            subject, userGroupA, 0, SendStrategy.SINGLE_NOTIFICATION );
        templateA.setAutoFields();
        templateA.setName( templateNameA );
        templateB = new DataSetNotificationTemplate( dataSets, channels, message, notificationRecipient, completion,
            subject, userGroupB, 0, SendStrategy.SINGLE_NOTIFICATION );
        templateB.setAutoFields();
        templateB.setName( templateNameB );
        dsntService.save( templateA );
        dsntService.save( templateB );
        assertEquals( 2, dsntService.getCompleteNotifications( dataSetA ).size() );
    }
}
