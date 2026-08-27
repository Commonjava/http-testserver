/*
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
package org.commonjava.test.http.util;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.Objects;
import java.util.stream.Stream;

public final class PortFinder
{

    private PortFinder()
    {
    }

    public static <T> T findPortFor( final int maxTries, final PortConsumer<T> consumer )
    {
        Exception lastException = null;

        for ( int i = 0; i < maxTries; i++ )
        {
            final int port = findOpenPort( maxTries );

            try
            {
                return consumer.call( port );
            }
            catch ( final IOException e )
            {
                lastException = e;
            }
            catch ( final RuntimeException e )
            {
                if ( !isBindException( e ) )
                {
                    throw e;
                }

                lastException = e;
            }
        }

        throw new IllegalStateException( "Cannot find open port after " + maxTries + " attempts.", lastException );
    }

    public static int findOpenPort( final int maxTries )
    {
        IOException lastException = null;

        for ( int i = 0; i < maxTries; i++ )
        {
            try ( ServerSocket socket = new ServerSocket( 0 ) )
            {
                return socket.getLocalPort();
            }
            catch ( final IOException e )
            {
                lastException = e;
            }
        }

        throw new IllegalStateException( "Cannot find open port after " + maxTries + " attempts.", lastException );
    }

    private static boolean isBindException( final Throwable t )
    {
        return Stream.iterate( t, Objects::nonNull, Throwable::getCause )
                     .anyMatch( BindException.class::isInstance );
    }

    public interface PortConsumer<T>
    {
        T call( int port ) throws IOException;
    }
}
