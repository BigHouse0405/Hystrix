/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.contrib.reactivesocket.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.metric.sample.HystrixCommandUtilization;
import com.netflix.hystrix.metric.sample.HystrixThreadPoolUtilization;
import com.netflix.hystrix.metric.sample.HystrixUtilization;
import org.agrona.LangUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SerialHystrixUtilization extends SerialHystrixMetric {

    public static byte[] toBytes(HystrixUtilization utilization) {
        byte[] retVal = null;

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            JsonGenerator json = cborFactory.createGenerator(bos);

            json.writeStartObject();
            json.writeStringField("type", "HystrixUtilization");
            json.writeObjectFieldStart("commands");
            for (Map.Entry<HystrixCommandKey, HystrixCommandUtilization> entry: utilization.getCommandUtilizationMap().entrySet()) {
                final HystrixCommandKey key = entry.getKey();
                final HystrixCommandUtilization commandUtilization = entry.getValue();
                writeCommandUtilizationJson(json, key, commandUtilization);

            }
            json.writeEndObject();

            json.writeObjectFieldStart("threadpools");
            for (Map.Entry<HystrixThreadPoolKey, HystrixThreadPoolUtilization> entry: utilization.getThreadPoolUtilizationMap().entrySet()) {
                final HystrixThreadPoolKey threadPoolKey = entry.getKey();
                final HystrixThreadPoolUtilization threadPoolUtilization = entry.getValue();
                writeThreadPoolUtilizationJson(json, threadPoolKey, threadPoolUtilization);
            }
            json.writeEndObject();
            json.writeEndObject();
            json.close();

            retVal = bos.toByteArray();
        } catch (Exception e) {
            LangUtil.rethrowUnchecked(e);
        }

        return retVal;
    }

    public static HystrixUtilization fromByteBuffer(ByteBuffer bb) {
        byte[] byteArray = new byte[bb.remaining()];
        bb.get(byteArray);

        Map<HystrixCommandKey, HystrixCommandUtilization> commandUtilizationMap = new HashMap<>();
        Map<HystrixThreadPoolKey, HystrixThreadPoolUtilization> threadPoolUtilizationMap = new HashMap<>();

        try {
            CBORParser parser = cborFactory.createParser(byteArray);
            JsonNode rootNode = mapper.readTree(parser);

            Iterator<Map.Entry<String, JsonNode>> commands = rootNode.path("commands").fields();
            Iterator<Map.Entry<String, JsonNode>> threadPools = rootNode.path("threadpools").fields();

            while (commands.hasNext()) {
                Map.Entry<String, JsonNode> command = commands.next();
                HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(command.getKey());
                HystrixCommandUtilization commandUtilization = new HystrixCommandUtilization(command.getValue().path("activeCount").asInt());
                commandUtilizationMap.put(commandKey, commandUtilization);
            }

            while (threadPools.hasNext()) {
                Map.Entry<String, JsonNode> threadPool = threadPools.next();
                HystrixThreadPoolKey threadPoolKey = HystrixThreadPoolKey.Factory.asKey(threadPool.getKey());
                HystrixThreadPoolUtilization threadPoolUtilization = new HystrixThreadPoolUtilization(
                        threadPool.getValue().path("activeCount").asInt(),
                        threadPool.getValue().path("corePoolSize").asInt(),
                        threadPool.getValue().path("poolSize").asInt(),
                        threadPool.getValue().path("queueSize").asInt()
                );
                threadPoolUtilizationMap.put(threadPoolKey, threadPoolUtilization);
            }
        } catch (IOException ioe) {
            System.out.println("IO Exception : " + ioe);
        }
        return new HystrixUtilization(commandUtilizationMap, threadPoolUtilizationMap);
    }

    private static void writeCommandUtilizationJson(JsonGenerator json, HystrixCommandKey key, HystrixCommandUtilization utilization) throws IOException {
        json.writeObjectFieldStart(key.name());
        json.writeNumberField("activeCount", utilization.getConcurrentCommandCount());
        json.writeEndObject();
    }

    private static void writeThreadPoolUtilizationJson(JsonGenerator json, HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolUtilization utilization) throws IOException {
        json.writeObjectFieldStart(threadPoolKey.name());
        json.writeNumberField("activeCount", utilization.getCurrentActiveCount());
        json.writeNumberField("queueSize", utilization.getCurrentQueueSize());
        json.writeNumberField("corePoolSize", utilization.getCurrentCorePoolSize());
        json.writeNumberField("poolSize", utilization.getCurrentPoolSize());
        json.writeEndObject();
    }
}
