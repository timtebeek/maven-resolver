/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.resolver.examples;

import org.apache.maven.resolver.examples.util.Booter;
import org.junit.Test;

/**
 * Runs all demos at once as part of UT.
 */
public class AllResolverDemosTest {
    @Test
    public void serviceLocator() throws Exception {
        AllResolverDemos.main(new String[] {Booter.SERVICE_LOCATOR});
    }

    @Test
    public void supplier() throws Exception {
        AllResolverDemos.main(new String[] {Booter.SUPPLIER});
    }

    @Test
    public void guice() throws Exception {
        AllResolverDemos.main(new String[] {Booter.GUICE});
    }

    @Test
    public void sisu() throws Exception {
        AllResolverDemos.main(new String[] {Booter.SISU});
    }
}
