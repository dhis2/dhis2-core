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
package org.hisp.dhis.message;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultMessageServiceTest {

  @Mock private DhisConfigurationProvider configurationProvider;

  @Mock private EmailMessageSender emailMessageSender;

  @Mock private List<MessageSender> messageSenders = new ArrayList<>();

  @InjectMocks private DefaultMessageService messageService;

  @ParameterizedTest
  @MethodSource("provideArgCombos")
  @SuppressWarnings("unchecked")
  void testFooter(String baseUrl, MessageType messageType, String uid) {
    // setup
    MessageConversationParams params =
        new MessageConversationParams.Builder()
            .withSubject("subject")
            .withText("text")
            .withMessageType(messageType)
            .build();
    MessageConversation messageConversation = params.createMessageConversation();
    messageConversation.setUid(uid);

    MessageConversationParams msgParams = Mockito.mock(MessageConversationParams.class);
    when(msgParams.createMessageConversation()).thenReturn(messageConversation);
    when(configurationProvider.getServerBaseUrl()).thenReturn(baseUrl);

    // mock an email sender so its args can be inspected
    Iterator<MessageSender> itr = Mockito.mock(Iterator.class);
    when(messageSenders.iterator()).thenReturn(itr);
    when(itr.hasNext()).thenReturn(true, false);
    when(itr.next()).thenReturn(emailMessageSender);

    ArgumentCaptor<String> footerCaptor = ArgumentCaptor.forClass(String.class);

    // condition
    messageService.sendMessage(msgParams);

    // checks
    verify(emailMessageSender)
        .sendMessageAsync(any(), any(), footerCaptor.capture(), any(), any(), anyBoolean());
    String footer = footerCaptor.getValue();
    assertTrue(
        footer.contains("https://dhis2.org/dhis-web-messaging/#/" + messageType + "/" + uid));
  }

  private static Stream<Arguments> provideArgCombos() {
    return Stream.of(
        Arguments.of("https://dhis2.org/", MessageType.SYSTEM, "Abcdefg0001"),
        Arguments.of("https://dhis2.org/", MessageType.PRIVATE, "Abcdefg0002"),
        Arguments.of("https://dhis2.org", MessageType.TICKET, "Abcdefg0003"),
        Arguments.of("https://dhis2.org", MessageType.SYSTEM_VERSION_UPDATE, "Abcdefg0004"),
        Arguments.of("https://dhis2.org", MessageType.VALIDATION_RESULT, "Abcdefg0005"));
  }
}
