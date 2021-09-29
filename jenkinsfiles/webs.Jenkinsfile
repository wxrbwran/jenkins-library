@Library('jenkins-libs-web@master') _

def gitlab = new com.xzlcorp.gitlab()
def tool = new com.xzlcorp.tools()
def webs = new com.xzlcorp.webs()

if (env.BUILD_USER != null) {
  currentBuild.description = "Trigger by ${env.BUILD_USER}"
} else {
  currentBuild.description = "Trigger by Gitlab"
}

def projects = []
def banned = ["shared"]
def PNPM_INSTALLED = false

def projectId = gitlab.GetProjectID("xzl-webs")
def packagesList = gitlab.GetProjectFileTree(projectId, env.BRANCH_NAME, "packages")
def converted = tool.JSON2Obj(packagesList);

converted.each({
  if (!banned.contains(it.name) && !it.name.startsWith("."))
  projects.add(it.name)
})

pipeline {
    agent {
        label 'YX_TEST'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        skipDefaultCheckout()
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
    }

    tools {
        nodejs 'NODEJS_14'
    }

    triggers {
        GenericTrigger (
            // 构建时的标题
            causeString: 'Triggered by $ref',
            // 获取POST参数中的变量，key指的是变量名，通过$ref来访问对应的值，value指的是JSON匹配值（参考Jmeter的JSON提取器）
            // ref指的是推送的分支，格式如：refs/heads/master
            genericVariables: [[key: 'ref', value: '$.ref']],
            // 打印获取的变量的key-value，此处会打印如：ref=refs/heads/master
            printContributedVariables: true,
            // 打印POST传递的参数
            printPostContent: true,
            // regexpFilterExpression与regexpFilterExpression成对使用
            // 当两者相等时，会触发对应分支的构建
            regexpFilterExpression: '^refs/heads/(cro|doctor|nurse|out|org)-(dev|test|master)$',
            regexpFilterText: '$ref',
            // 与webhook中配置的token参数值一致
            token: 'xzl-webs-token'
        )
    }

    stages {
        stage('clean') {
          steps {
            script {
              cleanWs()
            }
          }
        }
        stage('checkout') {
          steps {
            git branch: "${BRANCH_NAME}",
            credentialsId: 'gitlab-ssh-key',
            url: 'git@git.xzlcorp.com:UnitedFrontEnd/xzl-webs.git'
            sh 'ls -lat'
            script {
              env.GIT_CHANGE = sh(script: "git diff --name-status HEAD~0 HEAD~1", returnStdout: true).trim()
            }
          }
        }

        stage("install") {
          steps {
            script {
              projects.each({
                if (env.GIT_CHANGE.contains(it) && !PNPM_INSTALLED) {
                  sh 'npm i -g pnpm --registry=https://registry.npm.taobao.org'
                  sh "pnpm install --registry=https://registry.npm.taobao.org"
                  PNPM_INSTALLED = true
                }
              })

            }
          }
        }

        stage("build") {
          steps {
            script {
              projects.each({
                if (env.GIT_CHANGE.contains(it)) {
                  println("项目${it}已更改。")
                  webs.BuildAndDeployWebProject(it)
                }
              })
            }
          }
        }

    }

    post {
      always {
        echo "构建结束"
      }

      success {
        // echo "构建成功"
        script {
          if (env.BRANCH == "master") {
            tool.PrintMsg("打tag start","blue")
            String tagString = "v${new Date().format("yy.MMdd.HHmm")}"
            gitlab.CreateTag(projectId, tagString, env.BRANCH_NAME)
            tool.PrintMsg("打tag end","blue")
          }
        }
      }

      failure {
        echo "构建失败"
      }

      aborted {
        echo "构建中断"
      }
    }
}

