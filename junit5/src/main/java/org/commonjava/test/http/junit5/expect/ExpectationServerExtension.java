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
package org.commonjava.test.http.junit5.expect;

import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.test.http.junit5.annotations.Expected;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.lang.reflect.Field;

public class ExpectationServerExtension
        implements AfterEachCallback, BeforeEachCallback, TestInstancePostProcessor
{
    private ExpectationServer server;

    @SuppressWarnings( "unused" )
    public ExpectationServerExtension()
    {
        this( null );
    }

    public ExpectationServerExtension( final String baseResource )
    {
        this.server = new ExpectationServer( baseResource );
    }

    @Override
    public void beforeEach( ExtensionContext context )
    {
        server.start();
    }

    @Override
    public void afterEach( ExtensionContext context )
    {
        server.stop();
    }

    public ExpectationServer getServer()
    {
        return server;
    }

    @Override
    public void postProcessTestInstance( Object testInstance, ExtensionContext context )
            throws Exception
    {
        for ( Field field : testInstance.getClass().getDeclaredFields() )
        {
            if ( field.isAnnotationPresent( Expected.class ) )
            {
                Expected expected = field.getAnnotation( Expected.class );
                String base = expected.base();
                this.server = new ExpectationServer( base );
                field.setAccessible( true );
                if ( field.getType().equals( ExpectationServer.class ) )
                {
                    field.set( testInstance, this.server );
                    return;
                }
            }
        }
    }
}
