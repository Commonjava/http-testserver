/**
 * Copyright (C) 2011-2024 Red Hat, Inc. (https://github.com/Commonjava/http-testserver)
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
package org.commonjava.test.http.quarkus.internal;

import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.commonjava.test.http.quarkus.InjectStream;
import org.commonjava.test.http.stream.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StreamServerCallBack
        implements QuarkusTestAfterConstructCallback, QuarkusTestAfterEachCallback, QuarkusTestBeforeEachCallback
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private static final Map<Object, StreamServer> injectStreamServers = new ConcurrentHashMap<>();

    @Override
    public void afterConstruct( Object testInstance )
    {
        Class<?> current = testInstance.getClass();
        while ( current.getSuperclass() != null )
        {
            for ( Field field : current.getDeclaredFields() )
            {
                InjectStream injectMockAnnotation = field.getAnnotation( InjectStream.class );
                if ( injectMockAnnotation != null && field.getType().equals( StreamServer.class ) )
                {
                    final String baseResource = injectMockAnnotation.base();
                    logger.trace( "Found field with @InjectStream annotation, base resource is {}", baseResource );

                    StreamServer server = new StreamServer( baseResource );
                    try
                    {
                        logger.trace( "Injecting the field {} with server instance", field.getName() );
                        field.setAccessible( true );
                        field.set( testInstance, server );
                        injectStreamServers.putIfAbsent( testInstance, server );
                    }
                    catch ( IllegalAccessException e )
                    {
                        throw new RuntimeException( e );
                    }
                    return;
                }
            }
            current = current.getSuperclass();
        }
    }

    @Override
    public void beforeEach( QuarkusTestMethodContext context )
    {
        final StreamServer server = injectStreamServers.get( context.getTestInstance() );
        if ( server != null )
        {
            server.start();
        }
    }

    @Override
    public void afterEach( QuarkusTestMethodContext context )
    {
        final StreamServer server = injectStreamServers.get( context.getTestInstance() );
        if ( server != null )
        {
            server.stop();
        }
    }

}
