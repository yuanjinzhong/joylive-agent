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
package com.jd.live.agent.plugin.router.dubbo.v2_6.interceptor;

import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.filter.ExceptionFilter;
import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.dubbo.v2_6.request.DubboRequest.DubboInboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v2_6.request.invoke.DubboInvocation.DubboInboundInvocation;

/**
 * ClassLoaderFilterInterceptor
 */
public class ExceptionFilterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public ExceptionFilterInterceptor(InvocationContext context) {
        this.context = context;
    }

    /**
     * Enhanced logic before method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see ExceptionFilter#invoke(Invoker, Invocation)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Invocation invocation = (Invocation) mc.getArguments()[1];
        try {
            context.inbound(new DubboInboundInvocation(new DubboInboundRequest(invocation), context));
        } catch (RejectException e) {
            Result result = new RpcResult(new RpcException(RpcException.FORBIDDEN_EXCEPTION, e.getMessage()));
            mc.setResult(result);
            mc.setSkip(true);
        }
    }

}
