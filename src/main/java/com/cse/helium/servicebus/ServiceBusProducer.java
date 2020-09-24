package com.cse.helium.servicebus;

import com.microsoft.azure.servicebus.ITopicClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class ServiceBusProducer {

  @Autowired
  private ITopicClient iTopicClient;

  @EventListener(ApplicationReadyEvent.class)
  public void produce() throws InterruptedException, ServiceBusException {
      this.iTopicClient.send(new Message("Hello @ " + Instant.now().toString()));
  }

}
