package com.cse.helium.servicebus;

import com.microsoft.azure.servicebus.TopicClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ServiceBusConfig {

  @Autowired
  private KeyVaultService keyVaultService;

  @Bean
  public TopicClient topicClient() throws InterruptedException, ServiceBusException {

    String conn = keyVaultService.getSecret("ServiceBusConn");
    String topic = keyVaultService.getSecret("ServiceBusTopic");

    log.info("conn: " + conn);
    log.info("topic: " + topic);
    
    return new TopicClient(new ConnectionStringBuilder(conn, topic));
  }
}
