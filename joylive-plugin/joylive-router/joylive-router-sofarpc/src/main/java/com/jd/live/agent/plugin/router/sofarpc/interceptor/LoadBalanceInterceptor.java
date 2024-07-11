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
package com.jd.live.agent.plugin.router.sofarpc.interceptor;

import com.alipay.sofa.rpc.client.AbstractCluster;
import com.alipay.sofa.rpc.client.ProviderInfo;
import com.alipay.sofa.rpc.client.SofaRpcCluster;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.sofarpc.instance.SofaRpcEndpoint;
import com.jd.live.agent.plugin.router.sofarpc.request.SofaRpcRequest.SofaRpcOutboundRequest;
import com.jd.live.agent.plugin.router.sofarpc.request.invoke.SofaRpcInvocation.SofaRpcOutboundInvocation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClusterInterceptor
 */
public class LoadBalanceInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final ObjectParser parser;

    private final Map<AbstractCluster, SofaRpcCluster> clusters = new ConcurrentHashMap<>();

    public LoadBalanceInterceptor(InvocationContext context, ObjectParser parser) {
        this.context = context;
        this.parser = parser;
    }

    /**
     * Enhanced logic before method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
        List<ProviderInfo> invoked = (List<ProviderInfo>) arguments[1];
        SofaRpcCluster cluster = clusters.computeIfAbsent((AbstractCluster) ctx.getTarget(),
                c -> new SofaRpcCluster(c, parser));
        SofaRpcOutboundRequest request = new SofaRpcOutboundRequest((SofaRequest) arguments[0], cluster);
        if (!request.isSystem() && !request.isDisabled()) {
            SofaRpcOutboundInvocation invocation = new SofaRpcOutboundInvocation(request, new SofaRpcInvocationContext(context));
            try {
                invoked.forEach(p -> request.addAttempt(p.getHost() + ":" + p.getPort()));
                SofaRpcEndpoint endpoint = context.route(invocation);
                mc.setResult(endpoint.getProvider());
            } catch (RejectException e) {
                mc.setThrowable(cluster.createRejectException(e, request));
            }
            mc.setSkip(true);
        }
    }

}
