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
package org.commonjava.test.http.junit4.expect;

import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.rules.ExternalResource;

public class ExpectationServerRule
        extends ExternalResource
{
    private final ExpectationServer server;

    public ExpectationServerRule()
    {
        this( null );
    }

    public ExpectationServerRule( final String baseResource )
    {
        this.server = new ExpectationServer( baseResource );
    }

    @Override
    public void after()
    {
        server.stop();
    }

    @Override
    public void before()
            throws Exception
    {
        server.start();
    }

    public ExpectationServer getServer()
    {
        return server;
    }

}
