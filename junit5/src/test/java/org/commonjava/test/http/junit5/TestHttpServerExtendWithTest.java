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
package org.commonjava.test.http.junit5;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.commonjava.test.http.common.CommonMethod;
import org.commonjava.test.http.expect.ExpectationServer;
import org.commonjava.test.http.junit5.annotations.Expected;
import org.commonjava.test.http.junit5.expect.ExpectationServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith( ExpectationServerExtension.class )
public class TestHttpServerExtendWithTest
        extends AbstractExtensionTest
{
    //    @RegisterExtension
    @Expected( base = "repos", port = 9090 )
    private ExpectationServer server;

    @Override
    protected ExpectationServer getServer()
    {
        return this.server;
    }
}
