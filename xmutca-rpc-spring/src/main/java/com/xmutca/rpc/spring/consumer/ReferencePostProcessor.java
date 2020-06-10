package com.xmutca.rpc.spring.consumer;

import com.xmutca.rpc.core.config.RpcMetadata;
import com.xmutca.rpc.core.consumer.DefaultRemoteInvokerHandler;
import com.xmutca.rpc.core.consumer.RemoteInvokerHandler;
import com.xmutca.rpc.core.consumer.RemoteServiceFactory;
import com.xmutca.rpc.core.consumer.Reference;
import com.xmutca.rpc.core.rpc.exchange.ClientExchange;
import com.xmutca.rpc.spring.common.XmutcaRpcProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * RpcClientBeanPostProcessor
 *
 * @author pwh
 */
@Slf4j
public class ReferencePostProcessor implements BeanPostProcessor {


    private RemoteInvokerHandler remoteInvokerHandler;

    public ReferencePostProcessor(XmutcaRpcProperties xmutcaRpcProperties) {
        // 不存在配置
        if (Objects.isNull(xmutcaRpcProperties.getConsumer())) {
            return;
        }

        remoteInvokerHandler = new DefaultRemoteInvokerHandler(xmutcaRpcProperties.getConsumer());

        // 启动客户端
        ClientExchange.start(xmutcaRpcProperties.getConsumer(), xmutcaRpcProperties.getRegistry());
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)  {
        processRpcReference(bean);
        return bean;
    }

    private void processRpcReference(Object bean) {
        Class beanClass = bean.getClass();
        do {
            Field[] fields = beanClass.getDeclaredFields();
            for (Field field : fields) {
                Reference reference = field.getAnnotation(Reference.class);
                if (reference != null) {
                    // 生成元数据
                    RpcMetadata rpcMetadata = new RpcMetadata();
                    rpcMetadata.setGroup(reference.group());
                    rpcMetadata.setServiceName(reference.serviceName());
                    rpcMetadata.setVersion(reference.version());

                    // 生成引用
                    Object instance = RemoteServiceFactory
                            .factory(rpcMetadata, remoteInvokerHandler)
                            .getService(field.getType());
                    try {
                        field.setAccessible(true);
                        field.set(bean, instance);
                    } catch (IllegalAccessException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } while ((beanClass = beanClass.getSuperclass()) != null);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}