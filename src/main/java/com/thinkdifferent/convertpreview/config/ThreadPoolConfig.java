package com.thinkdifferent.convertpreview.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.concurrent.*;

/**
 * 线程池添加异常输出信息
 *
 * @author ltian
 * @version 1.0
 * @date 2023/2/2 10:47
 */
@ConditionalOnClass({ThreadPoolTaskExecutor.class})
@Configuration(
        proxyBeanMethods = false
)
@EnableConfigurationProperties({TaskExecutionProperties.class})
@Slf4j
public class ThreadPoolConfig {

    @Bean(
            name = {"applicationTaskExecutor", "taskExecutor"}
    )
    public Executor applicationTaskExecutor(TaskExecutionProperties properties) {
        TaskExecutionProperties.Pool pool = properties.getPool();

        return new TaskExecutor(pool.getCoreSize(), pool.getMaxSize(),
                pool.getKeepAlive().getSeconds(), TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    static class TaskExecutor extends ThreadPoolExecutor {

        public TaskExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (Objects.nonNull(t)) {
                log.error("线程池执行异常", t);
            }
        }
    }
}
