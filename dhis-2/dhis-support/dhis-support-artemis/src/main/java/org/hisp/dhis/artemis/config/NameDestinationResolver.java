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
package org.hisp.dhis.artemis.config;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicSession;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class NameDestinationResolver implements DestinationResolver {
  @Override
  public Destination resolveDestinationName(
      Session session, String destinationName, boolean pubSubDomain) throws JMSException {
    if (pubSubDomain) {
      return resolveTopic(session, destinationName);
    } else {
      return resolveQueue(session, destinationName);
    }
  }

  private Destination resolveTopic(Session session, String topicName) throws JMSException {
    if (session instanceof TopicSession) {
      // Cast to TopicSession: will work on both JMS 1.1 and 1.0.2
      return session.createTopic(topicName);
    } else {
      // Fall back to generic JMS Session: will only work on JMS 1.1
      return session.createTopic(topicName);
    }
  }

  private Queue resolveQueue(Session session, String queueName) throws JMSException {
    if (session instanceof QueueSession) {
      // Cast to QueueSession: will work on both JMS 1.1 and 1.0.2
      return session.createQueue(queueName);
    } else {
      // Fall back to generic JMS Session: will only work on JMS 1.1
      return session.createQueue(queueName);
    }
  }
}
