/*
 * Copyright © ${year} ${owner} (${email})
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
package com.jd.live.agent.plugin.router.kafka.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.kafka.v3.interceptor.PartitionInterceptor;
import com.jd.live.agent.plugin.router.kafka.v3.interceptor.SendInterceptor;

/**
 * ConsumerConfigDefinition
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "KafkaProducerDefinition_v3")
@ConditionalOnProperty(name = {
        GovernanceConfig.CONFIG_LIVE_ENABLED,
        GovernanceConfig.CONFIG_LANE_ENABLED
}, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_MQ_ENABLED)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_KAFKA_ENABLED)
@ConditionalOnClass(KafkaProducerDefinition.TYPE_CONSUMER_CONFIG)
public class KafkaProducerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_CONSUMER_CONFIG = "org.apache.kafka.clients.producer.KafkaProducer";

    private static final String METHOD_SEND = "send";

    private static final String METHOD_PARTITIONS_FOR = "partitionsFor";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public KafkaProducerDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_CONSUMER_CONFIG);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_SEND),
                        () -> new SendInterceptor(context)
                ),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_PARTITIONS_FOR),
                        () -> new PartitionInterceptor(context)
                )
        };
    }
}
