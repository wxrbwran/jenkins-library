package com.xzlcorp

import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput
import groovy.json.JsonGenerator.Options

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

def Obj2JSON(dataObject) {
    return JsonOutput.toJson(dataObject)
}