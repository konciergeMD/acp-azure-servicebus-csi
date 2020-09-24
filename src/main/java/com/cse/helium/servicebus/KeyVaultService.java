package com.cse.helium.servicebus;

import java.text.MessageFormat;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KeyVaultService {

  private TokenCredential credential;
  private SecretClient secretClient;

  public KeyVaultService(@Value("${keyvault.name}") String keyVaultName) {
    credential = new AzureCliCredentialBuilder().build();
    secretClient = new SecretClientBuilder()
        .vaultUrl("https://" + keyVaultName + ".vault.azure.net")
        .credential(credential)
        .buildClient();
    log.info(MessageFormat.format("KeyVaultUrl is {0}", secretClient.getVaultUrl()));
  }

  public String getSecret(String key){
    return secretClient.getSecret(key).getValue();
  }
  
}
