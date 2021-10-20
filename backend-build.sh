#!/bin/bash


for ARGUMENT in "$@"
do
    KEY=$(echo $ARGUMENT | cut -f1 -d=)
    VALUE=$(echo $ARGUMENT | cut -f2 -d=)   
    case "$KEY" in
        DOCKER_JRE_IMAGE)   DOCKER_JRE_IMAGE=${VALUE} ;;
        DOCKER_REGISTRY_IMAGE_TARGET)    DOCKER_REGISTRY_IMAGE_TARGET=${VALUE} ;;  
        WORKSPACE) WORKSPACE=${VALUE} ;;
        DOCKER_REGISTRY_URL) DOCKER_REGISTRY_URL=${VALUE} ;;
        ALIYUN_DOCKER_REGISTRY_LOGIN_USR) ALIYUN_DOCKER_REGISTRY_LOGIN_USR=${VALUE} ;;
        ALIYUN_DOCKER_REGISTRY_LOGIN_PSW) ALIYUN_DOCKER_REGISTRY_LOGIN_PSW=${VALUE} ;;
        *)   
    esac    
done

# gradle -Dorg.gradle.daemon=false clean

echo " ->（1）构建打包 (Fat Jar)"
gradle -v
# TASK=":publish"
# if gradle tasks --all | grep "$TASK"
# then
#     echo 'publish library artifact'
#     gradle -Dorg.gradle.daemon=false publish
# else
#     echo 'no publish task'
# fi

# if gradle tasks --all | grep "upgradeVersion"
# then
#     echo 'upgradeVersion artifact to db'
#     gradle -Dorg.gradle.daemon=false upgradeVersion
# else
#     gradle -Dorg.gradle.daemon=false build -x compileTestJava
# fi

echo " ->（2）构建Docker 镜像"
echo $DOCKER_JRE_IMAGE
echo $DOCKER_REGISTRY_IMAGE_TARGET
echo $WORKSPACE
echo $DOCKER_REGISTRY_URL
echo $ALIYUN_DOCKER_REGISTRY_LOGIN_USR
echo $ALIYUN_DOCKER_REGISTRY_LOGIN_PSW


#   docker build \
#   --build-arg jre=${DOCKER_JRE_IMAGE} \
#   -t ${DOCKER_REGISTRY_IMAGE_TARGET} \
#   --pull=true \
#   ${WORKSPACE}

# echo " -> （3） Docker 镜像上传入库"
docker -v
#   docker login \
#   --username ${ALIYUN_DOCKER_REGISTRY_LOGIN_USR} \
#   --password ${ALIYUN_DOCKER_REGISTRY_LOGIN_PSW} \
#   ${DOCKER_REGISTRY_URL}
#   docker push ${DOCKER_REGISTRY_IMAGE_TARGET}