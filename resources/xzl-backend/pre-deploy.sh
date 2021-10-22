#!/bin/bash

for ARGUMENT in "$@"
do
    KEY=$(echo $ARGUMENT | cut -f1 -d=)
    VALUE=$(echo $ARGUMENT | cut -f2 -d=)   
    case "$KEY" in
        CURRENT_PRJ_NAME)   CURRENT_PRJ_NAME=${VALUE} ;;
        DOCKER_REGISTRY_IMAGE_TARGET)    DOCKER_REGISTRY_IMAGE_TARGET=${VALUE} ;;  
        *)   
    esac    
done

echo " -> （1）尝试清理原有运行资源"
echo $CURRENT_PRJ_NAME
echo $DOCKER_REGISTRY_IMAGE_TARGET
# docker stop ' + "${CURRENT_PRJ_NAME}" + ' || true 
# docker container rm -f ' + "${CURRENT_PRJ_NAME}" + ' || true 
# docker image rmi --force ' + "${DOCKER_REGISTRY_IMAGE_TARGET}" + ' || true 

