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