/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.exec;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.apache.camel.util.UnsafeUriCharactersEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.apache.camel.component.exec.ExecEndpoint.NO_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the configuration of {@link ExecEndpoint}
 */
@CamelSpringTest
@ContextConfiguration
public class ExecEndpointTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecEndpointTest.class);

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ExecBinding customBinding;

    @Autowired
    private ExecCommandExecutor customExecutor;

    private Component component;

    @BeforeEach
    public void setUp() throws Exception {
        component = camelContext.getComponent("exec");
    }

    @Test
    @DirtiesContext
    public void testValidComponentDescriptor() {
        assertNotNull(component, "The Camel Exec component can not be resolved");
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointDefaultConf() throws Exception {
        ExecEndpoint e = createExecEndpoint("exec:test");
        assertTrue(e instanceof ExecEndpoint,
                "The Camel Exec component must create instances of " + ExecEndpoint.class.getSimpleName());
        assertNull(e.getArgs());
        assertNull(e.getWorkingDir());
        assertNull(e.getOutFile());

        assertEquals(NO_TIMEOUT, e.getTimeout());
        assertEquals("test", e.getExecutable());
        assertNotNull(e.getBinding());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointDefaultNoTimeout() throws Exception {
        ExecEndpoint e = createExecEndpoint("exec:test");
        assertEquals(ExecEndpoint.NO_TIMEOUT, e.getTimeout());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointCustomBinding() throws Exception {
        ExecEndpoint e = createExecEndpoint("exec:test?binding=#customBinding");
        assertSame(customBinding, e.getBinding(),
                "Expected is the custom customBinding reference from the application context");
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointCustomCommandExecutor() throws Exception {
        ExecEndpoint e = createExecEndpoint("exec:test?commandExecutor=#customExecutor");
        assertSame(customExecutor, e.getCommandExecutor(),
                "Expected is the custom customExecutor reference from the application context");
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointWithArgs() throws Exception {
        String args = "arg1 arg2 arg3";
        // can use space or %20
        ExecEndpoint e = createExecEndpoint("exec:test?args=" + args.replaceAll(" ", "%20"));
        assertEquals(args, e.getArgs());
        ExecEndpoint e2 = createExecEndpoint("exec:test?args=" + args);
        assertEquals(args, e2.getArgs());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointWithArgs2() throws Exception {
        String args = "arg1 \"arg2 \" arg3";
        ExecEndpoint e = createExecEndpoint("exec:test?args=" + UnsafeUriCharactersEncoder.encode(args));
        assertEquals(args, e.getArgs());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointWithArgs3() throws Exception {
        String args = "RAW(arg1+arg2 arg3)";
        // Just avoid URI encoding by using the RAW()
        ExecEndpoint e = createExecEndpoint("exec:test?args=" + args);
        assertEquals("arg1+arg2 arg3", e.getArgs());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointWithTimeout() throws Exception {
        long timeout = 1999999L;
        ExecEndpoint e = createExecEndpoint("exec:test?timeout=" + timeout);
        assertEquals(timeout, e.getTimeout());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointWithExitValues() throws Exception {
        ExecEndpoint e = createExecEndpoint("exec:test?exitValues=1,2,3");
        assertEquals("1,2,3", e.getExitValues());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointWithEmptyExitValues() throws Exception {
        ExecEndpoint e = createExecEndpoint("exec:test?exitValues=");
        assertEquals("", e.getExitValues());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointWithOutFile() throws Exception {
        String outFile = "output.txt";
        ExecEndpoint e = createExecEndpoint("exec:test?outFile=" + outFile);
        assertEquals(outFile, e.getOutFile());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointWithWorkingDir() throws Exception {
        String workingDir = "/workingdir";
        ExecEndpoint e = createExecEndpoint("exec:test?workingDir=" + workingDir);
        assertEquals(workingDir, e.getWorkingDir());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointEscapedWorkingDir() throws Exception {
        String cmd = "temp.exe";
        String dir = "\"c:/program files/wokr/temp\"";
        String uri = "exec:" + cmd + "?workingDir=" + dir;

        ExecEndpoint endpoint = createExecEndpoint(UnsafeUriCharactersEncoder.encode(uri));
        assertEquals(cmd, endpoint.getExecutable());
        assertNull(endpoint.getArgs());

        assertEquals(dir, endpoint.getWorkingDir());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointEscapedCommand() throws Exception {
        String executable = "C:/Program Files/test/text.exe";
        String uri = "exec:" + executable;

        ExecEndpoint endpoint = createExecEndpoint(UnsafeUriCharactersEncoder.encode(uri));

        assertNull(endpoint.getArgs());
        assertNull(endpoint.getWorkingDir());

        assertEquals(executable, endpoint.getExecutable());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointComposite() throws Exception {
        String workingDir = "/workingdir";
        String argsEscaped = "arg1 arg2 \"arg 3\"";
        long timeout = 10000L;

        String uri = "exec:executable.exe?workingDir=" + workingDir + "&timeout=" + timeout + "&args=" + argsEscaped;
        ExecEndpoint e = createExecEndpoint(UnsafeUriCharactersEncoder.encode(uri));
        assertEquals(workingDir, e.getWorkingDir());
        assertEquals(argsEscaped, e.getArgs());
        assertEquals(timeout, e.getTimeout());
    }

    @Test
    @DirtiesContext
    public void testCreateEndpointComposite2() throws Exception {
        String workingDir = "/workingdir";
        String outFile = "target/outfile.xml";
        long timeout = 10000;
        StringBuilder builder = new StringBuilder();
        builder.append("exec:executable.exe").append("?workingDir=" + workingDir).append("&timeout=" + timeout);
        builder.append("&outFile=" + outFile);
        builder.append("&commandExecutor=#customExecutor&binding=#customBinding");

        ExecEndpoint e = createExecEndpoint(UnsafeUriCharactersEncoder.encode(builder.toString()));
        assertEquals(workingDir, e.getWorkingDir());
        assertEquals(timeout, e.getTimeout());
        assertEquals(outFile, e.getOutFile());
        assertSame(customBinding, e.getBinding());
        assertSame(customExecutor, e.getCommandExecutor());
    }

    private ExecEndpoint createExecEndpoint(String uri) throws Exception {
        LOGGER.debug("Using Exec endpoint URI " + uri);
        return (ExecEndpoint) component.createEndpoint(uri);
    }
}
