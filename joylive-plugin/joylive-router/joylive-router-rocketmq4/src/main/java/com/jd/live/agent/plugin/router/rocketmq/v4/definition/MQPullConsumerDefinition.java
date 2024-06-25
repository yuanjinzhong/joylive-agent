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
package com.jd.live.agent.plugin.router.rocketmq.v4.definition;

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
import com.jd.live.agent.plugin.router.rocketmq.v4.interceptor.SubscribeInterceptor;

/**
 * DefaultMQPushConsumerDefinition
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "MQPullConsumerDefinition_v4")
@ConditionalOnProperty(name = {
        GovernanceConfig.CONFIG_LIVE_ENABLED,
        GovernanceConfig.CONFIG_LANE_ENABLED
}, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_MQ_ENABLED)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ROCKETMQ_ENABLED, matchIfMissing = true)
@ConditionalOnClass(MQPullConsumerDefinition.TYPE_MQ_PULL_CONSUMER)
@ConditionalOnClass(PullAPIWrapperDefinition.TYPE_CLIENT_LOGGER)
public class MQPullConsumerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_MQ_PULL_CONSUMER = "org.apache.rocketmq.client.consumer.MQPullConsumer";

    private static final String METHOD_FETCH_MESSAGE_QUEUES_IN_BALANCE = "fetchMessageQueuesInBalance";

    private static final String METHOD_FETCH_SUBSCRIBE_MESSAGE_QUEUES = "fetchSubscribeMessageQueues";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public MQPullConsumerDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_MQ_PULL_CONSUMER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.in(METHOD_FETCH_MESSAGE_QUEUES_IN_BALANCE, METHOD_FETCH_SUBSCRIBE_MESSAGE_QUEUES),
                        () -> new SubscribeInterceptor(context)
                )
        };
    }
}
