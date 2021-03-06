package com.xmutca.rpc.core.consumer.cluster;

import com.xmutca.rpc.core.config.RpcClientConfig;
import com.xmutca.rpc.core.config.RpcMetadata;
import com.xmutca.rpc.core.consumer.ClusterInvoker;
import com.xmutca.rpc.core.consumer.LoadBalancer;
import com.xmutca.rpc.core.exception.RpcException;
import com.xmutca.rpc.core.rpc.RpcRequest;
import com.xmutca.rpc.core.rpc.RpcResponse;
import com.xmutca.rpc.core.transport.Client;
import com.xmutca.rpc.core.transport.ClientGroup;
import com.xmutca.rpc.core.transport.ClientPool;

/**
 * @version Revision: 0.0.1
 * @author: weihuang.peng
 * @Date: 2019-11-08
 */
public abstract class AbstractClusterInvoker implements ClusterInvoker {

    /**
     * 元数据
     */
    private RpcMetadata rpcMetadata;

    /**
     * 负载均衡
     */
    private LoadBalancer loadBalancer;

    /**
     * 本地配置
     */
    private RpcClientConfig rpcClientConfig;

    /**
     * 初始化相关对象
     * @param rpcMetadata
     * @param loadBalancer
     * @param rpcClientConfig
     */
    @Override
    public void init(RpcMetadata rpcMetadata, LoadBalancer loadBalancer, RpcClientConfig rpcClientConfig) {
        this.rpcMetadata = rpcMetadata;
        this.loadBalancer = loadBalancer;
        this.rpcClientConfig = rpcClientConfig;
    }

    @Override
    public Object invoke(RpcRequest rpcRequest) {
        ClientGroup groups = filter();
        if (groups.isEmpty()) {
            throw new RpcException("Failed to invoke the method, No provider available for the service " + rpcRequest.getFullName());
        }
        return doInvoke(rpcRequest, rpcClientConfig, groups).getResult();
    }

    @Override
    public Object invoke(String serviceName, String methodName, String methodSign, Object[] args) {
        RpcRequest rpcRequest = RpcRequest
                .RpcRequestBuilder
                .rpcRequest()
                .className(serviceName)
                .methodName(methodName)
                .methodSign(methodSign)
                .arguments(defaultArguments(args))
                .build();
        return invoke(rpcRequest);
    }

    /**
     * 处理空参数问题
     * @param args
     * @return
     */
    private Object[] defaultArguments(Object[] args) {
        if (null == args || args.length == 0) {
            return new Object[]{};
        }
        return args;
    }

    /**
     * 执行
     * @param rpcRequest
     * @param rpcClientConfig
     * @param groups
     * @return
     */
    protected abstract RpcResponse doInvoke(RpcRequest rpcRequest, RpcClientConfig rpcClientConfig, ClientGroup groups);

    /**
     * 目录搜索
     *
     * @return
     */
    protected ClientGroup filter() {
        return ClientPool.filter(rpcMetadata);
    }

    /**
     * 负载均衡
     *
     * @return
     */
    protected Client select(ClientGroup groups) {
        // 负载均衡
        Client client = loadBalancer.select(groups);
        if (null == client) {
            throw new RpcException("fail to send, not channel active");
        }
        return client;
    }
}
