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
package org.apache.camel.processor.aggregate.jdbc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Test;

public class JdbcAggregateRecoverWithSedaTest extends AbstractJdbcAggregationTestSupport {

    private static AtomicInteger counter = new AtomicInteger();

    @Override
    void configureJdbcAggregationRepository() {
        // enable recovery
        repo.setUseRecovery(true);
        // check faster
        repo.setRecoveryInterval(500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testJdbcAggregateRecoverWithSeda() throws Exception {
        // should fail the first 2 times and then recover
        getMockEndpoint("mock:aggregated").expectedMessageCount(3);
        getMockEndpoint("mock:result").expectedBodiesReceived("ABCDE");
        // should be marked as redelivered
        getMockEndpoint("mock:result").message(0).header(Exchange.REDELIVERED).isEqualTo(Boolean.TRUE);
        // on the 2nd redelivery attempt we success
        getMockEndpoint("mock:result").message(0).header(Exchange.REDELIVERY_COUNTER).isEqualTo(2);

        template.sendBodyAndHeader("direct:start", "A", "id", 123);
        template.sendBodyAndHeader("direct:start", "B", "id", 123);
        template.sendBodyAndHeader("direct:start", "C", "id", 123);
        template.sendBodyAndHeader("direct:start", "D", "id", 123);
        template.sendBodyAndHeader("direct:start", "E", "id", 123);

        assertMockEndpointsSatisfied(30, TimeUnit.SECONDS);
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                        .aggregate(header("id"), new MyAggregationStrategy())
                        .completionSize(5).aggregationRepository(repo)
                        .log("aggregated exchange id ${exchangeId} with ${body}")
                        .to("mock:aggregated")
                        .to("seda:foo")
                        .end();

                // should be able to recover when we send over SEDA as its a OnCompletion
                // which confirms the exchange when its complete.
                from("seda:foo")
                        .delay(1000)
                        // simulate errors the first two times
                        .process(new Processor() {
                            public void process(Exchange exchange) {
                                int count = counter.incrementAndGet();
                                if (count <= 2) {
                                    throw new IllegalArgumentException("Damn");
                                }
                            }
                        })
                        .to("mock:result");
            }
        };
    }
}
