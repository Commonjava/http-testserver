/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (http://github.com/Commonjava/http-testserver)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.test.http.junit5.stream;

import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.test.http.junit5.annotations.Expected;
import org.commonjava.test.http.stream.StreamResolver;
import org.commonjava.test.http.stream.StreamServer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.lang.reflect.Field;

public class StreamServerExtension
        implements AfterEachCallback, BeforeEachCallback, TestInstancePostProcessor
{

    private final StreamServer server;

    public StreamServerExtension( final StreamResolver resolver )
    {
        server = new StreamServer( resolver );
    }

    public StreamServerExtension( final String resourceBase )
    {
        server = new StreamServer( resourceBase );
    }

    @Override
    public void afterEach( ExtensionContext extensionContext )
            throws Exception
    {
        server.stop();
    }

    @Override
    public void beforeEach( ExtensionContext extensionContext )
            throws Exception
    {
        server.start();
    }

    @Override
    public void postProcessTestInstance( Object testInstance, ExtensionContext context )
            throws Exception
    {
        for ( Field field : testInstance.getClass().getDeclaredFields() )
        {
            if ( field.isAnnotationPresent( Expected.class ) )
            {
                field.setAccessible( true );
                if ( field.getType().equals( StreamServer.class ) )
                {
                    field.set( testInstance, this.server );
                    return;
                }
            }
        }
    }
}
