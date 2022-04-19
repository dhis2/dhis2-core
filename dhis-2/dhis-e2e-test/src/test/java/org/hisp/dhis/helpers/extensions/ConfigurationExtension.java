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
package org.hisp.dhis.helpers.extensions;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.FailureConfig;
import io.restassured.config.JsonConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.helpers.config.TestConfiguration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ConfigurationExtension
    implements BeforeAllCallback
{
    private static Logger logger = LogManager.getRootLogger();

    private static PrintStream loggingProxy()
    {
        return new PrintStream( System.out )
        {
            @Override
            public void print( final String string )
            {
                logger.warn( string );
            }
        };
    }

    @Override
    public void beforeAll( ExtensionContext context )
    {
        RestAssured.baseURI = TestConfiguration.get().baseUrl();

        RestAssured.config = RestAssuredConfig.config()
            .failureConfig( FailureConfig.failureConfig().failureListeners( new OnFailureLogAppender() ) )
            .logConfig( new LogConfig().defaultStream( loggingProxy() )
                .urlEncodeRequestUri( false )
                .enableLoggingOfRequestAndResponseIfValidationFails() )
            .jsonConfig( new JsonConfig().numberReturnType( JsonPathConfig.NumberReturnType.BIG_DECIMAL ) );

        RestAssured.defaultParser = Parser.JSON;
        RestAssured.requestSpecification = defaultRequestSpecification();
    }

    private RequestSpecification defaultRequestSpecification()
    {
        RequestSpecBuilder requestSpecification = new RequestSpecBuilder();

        requestSpecification.addFilters( Arrays.asList(
            new CookieFilter(),
            new SessionFilter(),
            new AuthFilter()
        ) )
            .setContentType( ContentType.JSON );

        return requestSpecification.build();
    }
}
