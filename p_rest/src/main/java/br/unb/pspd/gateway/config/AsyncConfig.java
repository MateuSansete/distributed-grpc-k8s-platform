package br.unb.pspd.gateway.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Executor dedicado para as chamadas gRPC paralelas (A e B), evitando o
 * ForkJoinPool.commonPool compartilhado da JVM.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "packageSearchExecutor", destroyMethod = "shutdown")
    public ExecutorService packageSearchExecutor() {
        return Executors.newFixedThreadPool(8);
    }
}
