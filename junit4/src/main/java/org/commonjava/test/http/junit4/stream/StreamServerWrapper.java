/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/http-testserver)
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
package org.commonjava.test.http.junit4.stream;

import org.commonjava.test.http.common.CommonMethod;
import org.commonjava.test.http.stream.StreamResolver;
import org.commonjava.test.http.stream.StreamServer;
import org.commonjava.test.http.util.UrlUtils;
import org.junit.rules.ExternalResource;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings( "unused" )
public class StreamServerWrapper
        extends ExternalResource
{

    private final StreamServer server;

    public StreamServerWrapper( final StreamResolver resolver )
    {
        server = new StreamServer( resolver );
    }

    public StreamServerWrapper( final String resourceBase )
    {
        server = new StreamServer( resourceBase );
    }

    public StreamServer getServer()
    {
        return server;
    }

    @Override
    public void after()
    {
        server.stop();
    }

    @Override
    public void before()
    {
        server.start();
    }

    public int getPort()
    {
        return server.getPort();
    }

    public String formatUrl( final String... subpath )
    {
        return server.formatUrl( subpath );
    }

    public String formatPath( final String... subpath )
    {
        try
        {
            return UrlUtils.buildPath( "/", subpath );
        }
        catch ( final MalformedURLException e )
        {
            throw new IllegalArgumentException( "Failed to build url to: " + Arrays.toString( subpath ), e );
        }
    }

    public String getBaseUri()
    {
        return server.getBaseUri();
    }

    public String getUrlPath( final String url )
            throws MalformedURLException
    {
        return server.getUrlPath( url );
    }

    public Map<String, Integer> getAccessesByPathKey()
    {
        return server.getAccessesByPathKey();
    }

    public String getAccessKey( final CommonMethod method, final String path )
    {
        return server.getAccessKey( method, path );
    }

    public String getAccessKey( final String method, final String path )
    {
        return server.getAccessKey( method, path );
    }

    public Integer getAccessesFor( final String path )
    {
        return server.getAccessesFor( path );
    }

    public Integer getAccessesFor( final String method, final String path )
    {
        return server.getAccessesFor( method, path );
    }
}
