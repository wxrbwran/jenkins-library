#!/bin/bash

for ARGUMENT in "$@"
do
    KEY=$(echo $ARGUMENT | cut -f1 -d=)
    VALUE=$(echo $ARGUMENT | cut -f2 -d=)   
    case "$KEY" in
        ALIYUN_DOCKER_REGISTRY_LOGIN_USR)   ALIYUN_DOCKER_REGISTRY_LOGIN_USR=${VALUE} ;;
        ALIYUN_DOCKER_REGISTRY_LOGIN_PSW)    ALIYUN_DOCKER_REGISTRY_LOGIN_PSW=${VALUE} ;;  
        *)   
    esac    
done

 echo " -> （2） 部署 Docker 镜像到目标服务器"
 docker -v
 echo $ALIYUN_DOCKER_REGISTRY_LOGIN_USR
 echo $ALIYUN_DOCKER_REGISTRY_LOGIN_PSW