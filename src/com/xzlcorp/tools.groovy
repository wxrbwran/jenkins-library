package com.xzlcorp

import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput
// import groovy.json.JsonGenerator.Options

//格式化输出
def PrintMsg(value,color){
    colors = ['red'   : "\033[40;31m >>>>>>>>>>>${value}<<<<<<<<<<< \033[0m",
              'blue'  : "\033[47;34m ${value} \033[0m",
              'green' : "[1;32m>>>>>>>>>>${value}>>>>>>>>>>[m",
              'green1' : "\033[40;32m >>>>>>>>>>>${value}<<<<<<<<<<< \033[0m" ]
    ansiColor('xterm') {
        println(colors[color])
    }
}

// JSON String 转为 Object

def JSON2Obj(jsonString) {
    return new JsonSlurperClassic().parseText(jsonString)
}

// Object 转为 JSON String 

def Obj2JSON(dataObject) {
    return JsonOutput.toJson(dataObject)
}

// 显示触发构建者
def ShowTriggerUser(env) {
    if (env.BUILD_USER != null) {
        currentBuild.description = "Trigger by ${env.BUILD_USER}"
    } else {
        currentBuild.description = "Trigger by Gitlab"
    }
}

def TagIt(projectId, branchName, tagString = "v${new Date().format("yy.MMdd.HHmmSSSSSS")}") {
    if (branchName == "master") {
        PrintMsg("打tag start","blue")
        gitlab.CreateTag(projectId, tagString, env.BRANCH_NAME)
        PrintMsg("打tag end","blue")
    } else {
        PrintMsg("不是master,不打了","blue")
    }
}

def DingItMarkdown(params, env) {
    text = params.type == "success" ? "成功" : "失败";
    dingtalk (
        robot: params.robotId,
        type:'MARKDOWN',
        atAll: params.atAll,
        text: [
            "# ${params.projectName}构建${text}",
            "---",
            "> 详情",
            "- 分支: ${env.BRANCH_NAME}",
            "- 持续时间: ${currentBuild.durationString}",
            "- 任务: #${env.BUILD_ID}",
            "- 构建地址: [点击查看](https://njenkins.xzlcorp.com/view/Web/job/xzl-webs/job/${env.BRANCH_NAME}/${env.BUILD_ID}/console)"
        ],
    )
}

def GetProjectId(projectName) {
    def projectIdMap = ["xzl-webs": 334];
    return projectIdMap[projectName];
}

def GetBackendEnv(projectName) {
    switch(projectName) {
        case "med-service":
            return [
                RUN_PORT: 8030,
                TARGET_HOST_IP: "10.0.2.130"
            ];
        case "console-service":
            return [
                RUN_PORT: 8020,
                TARGET_HOST_IP: "10.0.2.131"
            ];
    }
}

def HandleEnv(env) {
    env.RUN_PARAMS = "--spring.cloud.config.profile=${BRANCH_NAME} --spring.profiles.active=${BRANCH_NAME}"
    if (env.BRANCH_NAME == 'shanxi_dev') {
        env.TARGET_HOST_IP = "192.168.1.100"
        env.EUREKA_URL = "http://192.168.1.100:7990/eureka/"
        env.AGENT_LABEL = "SX_DEV"
    } else if (env.BRANCH_NAME == 'dev') {
        env.TARGET_HOST_IP = "172.16.10.129"
        env.EUREKA_URL = "http://172.16.10.129:7990/eureka/"
        env.AGENT_LABEL = "YX_DEV"
    } else if (env.BRANCH_NAME == 'test') {
        env.TARGET_HOST_IP = "172.16.10.128"
        env.EUREKA_URL = "http://172.16.10.128:7990/eureka/"
        env.AGENT_LABEL = "YX_DEV"
    } else if (env.BRANCH_NAME == 'master') {
        env.TARGET_HOST_IP = "172.16.10.122"
        env.EUREKA_URL = "http://172.16.10.122:7990/eureka/"
        env.AGENT_LABEL = "YX_DEV"
        env.RUN_PARAMS = "--spring.cloud.config.profile=prod --spring.profiles.active=prod"
    } else if (env.BRANCH_NAME == 'prod_master') {
        env.TARGET_HOST_IP = "10.0.2.130"
        env.EUREKA_URL = "http://10.0.2.129:7990/eureka/"
        env.AGENT_LABEL = "MSA"
        env.RUN_PARAMS = "--spring.cloud.config.profile=aliyun_prod --spring.profiles.active=aliyun_prod"
    }
    env.DOCKER_REGISTRY_HOST = "registry.cn-beijing.aliyuncs.com"
    env.DOCKER_REGISTRY_URL = "https://${DOCKER_REGISTRY_HOST}"
    env.DOCKER_REGISTRY_PREFIX = "${DOCKER_REGISTRY_HOST}"
    env.DOCKER_REGISTRY_IMAGE_TARGET = "${DOCKER_REGISTRY_PREFIX}/xzl-dev/${CURRENT_PRJ_NAME}"
    env.DOCKER_JRE_IMAGE = "${DOCKER_REGISTRY_PREFIX}/corp/jre:11u8"

    return env;
}