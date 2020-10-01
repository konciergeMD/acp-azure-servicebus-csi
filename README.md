# Demo and Deployment Walk Through

## Background

Walkthrough demonstrating usage of CSI Driver and Azure Key Vault. Based on [Helium AKS](https://github.com/retaildevcrews/helium/tree/main/docs/aks) and [CSI Driver](https://github.com/Azure/secrets-store-csi-driver-provider-azure)

### Azure Components in Use

- Azure Container Registry
- Azure Kubernetes Service
- Azure Key Vault
- Azure Service Bus
- Azure Managed Identity

### Prerequisites

- Azure subscription with permissions to create:
  - Resource Groups, Service Principals, Keyvault, Cosmos DB, AKS, Azure Container Registry, Azure Monitor
- Bash shell (tested on Mac, Ubuntu, Windows with WSL2)
  - Will not work in Cloud Shell or WSL1
- Azure CLI ([download](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest))
- Docker CLI ([download](https://docs.docker.com/install/))
- Visual Studio Code (optional) ([download](https://code.visualstudio.com/download))
- kubectl (install by using `sudo az aks install-cli`)
- Helm v3 ([Install Instructions](https://helm.sh/docs/intro/install/))
  - Using helm version > 3.3 is recommended (setup has been tested successfully with 3.3.4)
- Java 11
- Maven 1.6+

## Setup

Fork this repo and clone to your local machine

```bash

git clone https://github.com/alfredoihernandez/servicebus-csi.git

cd servicebus-csi

```

### Login to Azure and select subscription

```bash

az login

# show your Azure accounts
az account list -o table

# select the Azure account
az account set -s {subscription name or Id}

```

### Choose a unique DNS name

```bash

# this will be the prefix for all resources
# do not include punctuation - only use a-z and 0-9
# must be at least 5 characters long
# must start with a-z (only lowercase)
export Name=[your unique name]

export Location=eastus

### if nslookup doesn't fail to resolve, change Name
nslookup ${Name}.vault.azure.net
nslookup ${Name}.azurecr.io

```

### Create Resource Group

> When experimenting with this sample, you should create new resource groups to avoid accidentally deleting resources
>
> If you use an existing resource group, please make sure to apply resource locks to avoid accidentally deleting resources

```bash

# create the resource groups
az group create -n ${Name}-rg -l ${Location}

```

### Create Azure Key Vault

- All secrets are stored in Azure Key Vault for security

```bash

## create the KV
az keyvault create -g ${Name}-rg -n ${Name}-kv

```

### Setup Azure Container Registry

- Create the Container Registry with admin access `disabled`

```bash

# create the ACR
az acr create --sku Standard --admin-enabled false -g ${Name}-rg -n $Name

```

### Create the AKS Cluster

```bash

# note: if you see the following failure, navigate to your .azure\ directory
# and delete the file "aksServicePrincipal.json":
#    Waiting for AAD role to propagate[################################    ]  90.0000%Could not create a
#    role assignment for ACR. Are you an Owner on this subscription?

az aks create --name ${Name}-aks --resource-group ${Name}-rg --location ${Location} --enable-cluster-autoscaler --min-count 3 --max-count 6 --node-count 3 --kubernetes-version 1.17.11 --attach-acr $Name  --no-ssh-key --enable-managed-identity

az aks get-credentials -n ${Name}-aks -g ${Name}-rg

# Test if you can get nodes
kubectl get nodes

```

### Create Service Bus

```bash

# Create SB Namespace

az servicebus namespace create -g ${Name}-rg -n ${Name}-sb-ns

# Create SB Topic

az servicebus topic create -g ${Name}-rg --namespace-name ${Name}-sb-ns -n ${Name}-sb-topic

# Create SB Subscription

az servicebus topic subscription create -g $Name-rg --namespace-name $Name-sb-ns --topic-name ${Name}-sb-topic -n ${Name}-sb-sub

# Get SB Access Key

export SB_Access_Key='az servicebus namespace authorization-rule keys list -g $Name-rg --namespace-name $Name-sb-ns --name RootManageSharedAccessKey -o tsv --query primaryConnectionString'

```

### Save Service Bus Config to Key Vault

```bash

az keyvault secret set -o table --vault-name ${Name}-kv --name "ServiceBusConn" --value $(eval $SB_Access_Key)
az keyvault secret set -o table --vault-name ${Name}-kv --name "ServiceBusTopic" --value ${Name}-sb-topic

```

### Set Up Managed Identity

```bash

helm repo add aad-pod-identity https://raw.githubusercontent.com/Azure/aad-pod-identity/master/charts

# Note: This is sourcing the file so that the exported variables in the script can be used below when installing the service bus app.
. ./helm/servicebus/aad-podid.sh -a ${Name}-aks -r ${Name}-rg -m ${Name}-mi -k ${Name}-kv

```

## Build and Push Docker Image

```bash

docker build . -t ${Name}.azurecr.io/sbus:latest

az acr login -n $Name

docker push ${Name}.azurecr.io/sbus:latest

```

### Modify Values in YAML

```bash

# MacOS users: sed inplace isn't compatible
# Mac Users: brew install gsed
# Mac Users: alias sed='gsed'

sed -i "s/%%Name%%/${Name}/g" helm/servicebus/helm-config.yaml && \
sed -i "s/%%KV_TenantID%%/$(az account show --query id -o tsv)/g" helm/servicebus/helm-config.yaml && \
sed -i "s/%%MI_Client_ID%%/${MI_ClientID}/g" helm/servicebus/helm-config.yaml && \
sed -i "s|%%MI_Resource_ID%%|${MI_ResID}|g" helm/servicebus/helm-config.yaml
# Notes:
#   - This last replacement us using "|" as a separator because $MI_ResID has "/" characters.
#   - MI_ClientID and MI_ResID are set when running ". ./helm/servicebus/aad-podid.sh ..."

```

### Helm Install CSI Driver and Service Bus

```bash

helm repo add csi-secrets-store-provider-azure https://raw.githubusercontent.com/Azure/secrets-store-csi-driver-provider-azure/master/charts

helm install csi-provider csi-secrets-store-provider-azure/csi-secrets-store-provider-azure

helm install servicebus helm/servicebus -f helm/servicebus/helm-config.yaml

```

### Checking Install

```bash

# Check whether the servicebus pods are running
# Example output:
# servicebus-6b7956d7bf-hsj9h  1/1   Running   0    2m12s
kubectl get pods | grep servicebus

# Check whether kubernetes secrets are created
kubectl get secrets | grep sb

# Exec into pods to see secrets
kubectl exec -it `kubectl get pods | grep servicebus | awk '{print $1}'` -- /bin/sh
ls /mnt/secrets-store

```

### Clean up

```bash

az group delete --no-wait -y -n ${Name}-rg

```
