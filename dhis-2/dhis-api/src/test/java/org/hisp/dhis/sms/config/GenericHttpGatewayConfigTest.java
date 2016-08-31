package org.hisp.dhis.sms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Before;
import org.junit.Test;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

public class GenericHttpGatewayConfigTest
{
    private String urlTemplate = "http://bulksms.vsms.net:5567/eapi/submission/send_sms/2/2.0?username={username}&password={password}&source_id={sender}&message={message}&msisdn={recipient}";

    private String bulk = "<bulksms><name>bulk</name><username>username</username><password>password</password></bulksms>";

    private String click = "<clickatell><name>click</name><username>storset</username><password>dust2001</password><apiId>3304014</apiId></clickatell>";

    private JAXBContext context;

    private String urlString = "<urlTemplate>http://bulksms.vsms.net:5567/eapi/submission/send_sms/2/2.0?username={username}&amp;password={password}&amp;source_id={sender}&amp;message={message}&amp;msisdn={recipient}</urlTemplate>";

    private String http = "<http><name>http</name>" + urlString + "<parameters>"
        + "<parameter key=\"username\" value=\"storset\" /><parameter key=\"password\" value=\"dust2001\" />"
        + "</parameters>" + "</http>";

    @Before
    public void setup()
        throws JAXBException
    {
        context = JAXBContext.newInstance( SmsConfiguration.class );
    }

    @Test
    public void testMarshalling()
        throws IOException, JAXBException
    {
        Writer writer = new StringWriter();
        Map<String, String> parameters = new HashMap<>();
        parameters.put( "username", "u1" );
        parameters.put( "password", "p1" );
        parameters.put( "sender", "s1" );

        SmsGatewayConfig config = new GenericHttpGatewayConfig(parameters );
        config.setUrlTemplate( urlTemplate );
        SmsConfiguration smsConfiguration = new SmsConfiguration();
        smsConfiguration.setGateways( Collections.singletonList( config ) );
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
        marshaller.marshal( smsConfiguration, writer );

        writer.flush();
        
        //assertTrue(writer.toString().contains( "<parameter key=\"username\" value=\"u1\"" ));
    }

    @Test
    public void testUnmarshalling()
        throws JAXBException
    {
        String xml = "<smsConfiguration xmlns=\"http://dhis2.org/schema/dxf/2.0\"><enabled>true</enabled><longNumber>DHIS2</longNumber>";
        xml += "<gateways>" + bulk + click + http + "</gateways></smsConfiguration>";

        Unmarshaller unmarshaller = context.createUnmarshaller();

        SmsConfiguration config = (SmsConfiguration) unmarshaller.unmarshal( new StringReader( xml ) );

        assertNotNull( config );
        List<SmsGatewayConfig> gateways = config.getGateways();
        assertNotNull( gateways );
        assertEquals( 3, gateways.size() );
        assertTrue( ((GenericHttpGatewayConfig)gateways.get( 2 )).getUrlTemplate().contains( "http://bulksms.vsms.net:5567/eapi/submission/send_sms/2/2.0" ) );
    }
}
