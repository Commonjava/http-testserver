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
package org.commonjava.test.http.junit4.expect;

import org.commonjava.test.http.common.CommonMethod;
import org.commonjava.test.http.expect.ContentResponse;
import org.commonjava.test.http.expect.ExpectationHandler;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.rules.ExternalResource;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;

@SuppressWarnings( "unused" )
public class ExpectationServerWrapper
        extends ExternalResource
{
    private final ExpectationServer server;

    public ExpectationServerWrapper()
    {
        this( null );
    }

    public ExpectationServerWrapper( final String baseResource )
    {
        this.server = new ExpectationServer( baseResource );
    }

    public ExpectationServerWrapper( final String baseResource, final int port )
    {
        this.server = new ExpectationServer( baseResource, port );
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
        return server.formatPath( subpath );
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

    public Map<String, ContentResponse> getRegisteredErrors()
    {
        return server.getRegisteredErrors();
    }

    public void registerException( final String url, final String error )
    {
        server.registerException( url, error );
    }

    public void registerException( final String method, final String url, final String error )
    {
        server.registerException( method, url, error );
    }

    public void registerException( final String url, final String error, final int responseCode )
    {
        server.registerException( url, error, responseCode );
    }

    public void registerException( final String method, final String url, final int responseCode, final String error )
    {
        server.registerException( method, url, responseCode, error );
    }

    public void expect( final String testUrl, final int responseCode, final String body )
            throws Exception
    {
        server.expect( testUrl, responseCode, body );
    }

    public void expect( final String method, final String testUrl, final int responseCode, final String body )
            throws Exception
    {
        server.expect( method, testUrl, responseCode, body );
    }

    public void expect( final String testUrl, final int responseCode, final InputStream bodyStream )
            throws Exception
    {
        server.expect( testUrl, responseCode, bodyStream );
    }

    public void expect( final String method, final String testUrl, final int responseCode,
                        final InputStream bodyStream )
            throws Exception
    {
        server.expect( method, testUrl, responseCode, bodyStream );
    }

    public void expect( final String method, final String testUrl, ExpectationHandler handler )
    {
        server.expect( method, testUrl, handler );
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

    public ExpectationServer getServer()
    {
        return server;
    }

}
