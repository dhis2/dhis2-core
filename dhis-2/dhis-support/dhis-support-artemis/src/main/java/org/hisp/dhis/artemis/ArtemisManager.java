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
package org.hisp.dhis.artemis;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMRegistry;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.remoting.Acceptor;
import org.hisp.dhis.artemis.config.ArtemisConfigData;
import org.hisp.dhis.artemis.config.ArtemisMode;
import org.springframework.stereotype.Service;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service
public class ArtemisManager {
  private final EmbeddedActiveMQ embeddedActiveMQ;

  private final ArtemisConfigData artemisConfigData;

  public ArtemisManager(EmbeddedActiveMQ embeddedActiveMQ, ArtemisConfigData artemisConfigData) {
    this.embeddedActiveMQ = embeddedActiveMQ;
    this.artemisConfigData = artemisConfigData;
  }

  @PostConstruct
  public void startArtemis() throws Exception {
    if (ArtemisMode.EMBEDDED == artemisConfigData.getMode()) {
      Acceptor existingAcceptor =
          InVMRegistry.instance.getAcceptor(ArtemisConfigData.EMBEDDED_ACCEPTOR_ID);
      if (existingAcceptor == null) {
        log.info(
            "Starting embedded Artemis ActiveMQ server with Acceptor ID {}",
            ArtemisConfigData.EMBEDDED_ACCEPTOR_ID);
        embeddedActiveMQ.start();
      } else {
        log.warn(
            "Acceptor with ID {} already exists, skip starting embedded Artemis ActiveMQ server.",
            ArtemisConfigData.EMBEDDED_ACCEPTOR_ID);
      }
    }
  }

  @PreDestroy
  public void stopArtemis() throws Exception {
    if (embeddedActiveMQ == null) {
      return;
    }

    embeddedActiveMQ.stop();
  }
}
