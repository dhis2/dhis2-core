/*
 * Copyright (c) 2004-2023, University of Oslo
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

import java.util.Set;
import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetStore;
import org.hisp.dhis.notification.SendStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataSetNotificationTemplateStoreTest extends TransactionalIntegrationTest {

  @Autowired private DataSetNotificationTemplateStore store;
  @Autowired private DataSetStore dataSetStore;

  @Test
  void testGetNotificationsByTriggerTypeAndDataSet() {
    DataSet dataSet = createDataSet('A');
    dataSetStore.save(dataSet);

    DataSetNotificationTemplate template =
        createDataSetNotificationTemplate(
            "A",
            DataSetNotificationRecipient.USER_GROUP,
            DataSetNotificationTrigger.DATA_SET_COMPLETION,
            1,
            SendStrategy.SINGLE_NOTIFICATION);
    template.setDataSets(Set.of(dataSet));
    store.save(template);

    assertEquals(
        1,
        store
            .getNotificationsByTriggerType(dataSet, DataSetNotificationTrigger.DATA_SET_COMPLETION)
            .size());
  }

  @Test
  void testGetScheduledNotificationsValid() {
    DataSetNotificationTemplate template =
        createDataSetNotificationTemplate(
            "A",
            DataSetNotificationRecipient.USER_GROUP,
            DataSetNotificationTrigger.DATA_SET_COMPLETION,
            1,
            SendStrategy.SINGLE_NOTIFICATION);
    store.save(template);

    Assertions.assertEquals(
        1, store.getScheduledNotifications(DataSetNotificationTrigger.DATA_SET_COMPLETION).size());
  }

  @Test
  void testGetScheduledNotificationsNotExist() {
    DataSetNotificationTemplate template =
        createDataSetNotificationTemplate(
            "A",
            DataSetNotificationRecipient.USER_GROUP,
            DataSetNotificationTrigger.DATA_SET_COMPLETION,
            1,
            SendStrategy.SINGLE_NOTIFICATION);
    store.save(template);

    Assertions.assertEquals(
        0, store.getScheduledNotifications(DataSetNotificationTrigger.SCHEDULED_DAYS).size());
  }
}
