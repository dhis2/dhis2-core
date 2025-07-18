/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.imports.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.hisp.dhis.artemis.Message;
import org.hisp.dhis.artemis.MessageType;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;

/**
 * Class holding data necessary for implementation of notifications.
 *
 * @author Zubair Asghar
 */
@Data
@Builder(builderClassName = "TrackerNotificationBundleBuilder")
@JsonDeserialize(builder = TrackerNotificationDataBundle.TrackerNotificationBundleBuilder.class)
public class TrackerNotificationDataBundle implements Message {
  @JsonProperty private String uid;

  @JsonProperty private Class<? extends IdentifiableObject> klass;

  @JsonProperty private String object;

  @JsonProperty private JobConfiguration jobConfiguration;

  @JsonProperty private Program program;

  @JsonProperty private Enrollment enrollment;

  @JsonProperty private Event event;

  @JsonProperty private SingleEvent singleEvent;

  @JsonProperty private List<NotificationTrigger> triggers = new ArrayList<>();

  @JsonProperty @Builder.Default
  private List<Notification> enrollmentNotifications = new ArrayList<>();

  @JsonProperty @Builder.Default
  private List<Notification> trackerEventNotifications = new ArrayList<>();

  @JsonProperty @Builder.Default
  private List<Notification> singleEventNotifications = new ArrayList<>();

  @JsonProperty private TrackerImportStrategy importStrategy;

  @JsonProperty private String accessedBy;

  @JsonProperty private String jobId;

  @Override
  @JsonProperty
  public MessageType getMessageType() {
    return MessageType.TRACKER_SIDE_EFFECT;
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static final class TrackerNotificationBundleBuilder {}
}
