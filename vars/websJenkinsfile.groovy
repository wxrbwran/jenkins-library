def call(params) {
  def env = params.env

  def gitlab = new com.xzlcorp.gitlab()
  def tool = new com.xzlcorp.tools()
  def webs = new com.xzlcorp.webs()

  tool.ShowTriggerUser(env)

  def projects = []
  def PNPM_INSTALLED = false

  def projectId = gitlab.GetProjectID('xzl-webs')
  def packagesList = gitlab.GetProjectFileTree(projectId, env.BRANCH_NAME, 'packages')
  def converted = tool.JSON2Obj(packagesList)

  converted.each({
    if (!it.name.startsWith('.')) {
      projects.add(it.name)
    }
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
          nodejs 'NODEJS_16'
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
              regexpFilterExpression: '^refs/heads/(dev|test|master)$',
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
                env.GIT_CHANGE = sh(script: 'git diff --name-status HEAD~0 HEAD~1', returnStdout: true).trim()
              }
            }
          }

          stage('install') {
            steps {
              script {
                projects.each({
                  if (env.GIT_CHANGE.contains(it) && !PNPM_INSTALLED) {
                    // pnpm@7.rc 版本有问题
                    sh 'npm i -g pnpm@6.32.4 --registry=https://registry.npmmirror.com'
                    // sh 'npm i -g cnpm --registry=https://registry.npmmirror.com'
                    sh 'pnpm install --registry=https://registry.npmmirror.com'
                    PNPM_INSTALLED = true
                  }
                })
              }
            }
          }

          stage('build') {
            steps {
              script {
                if (env.GIT_CHANGE.contains('shared')) {
              projects = projects - 'shared'
              println('shared已更改,进行全部构建!')
              projects.each({
                    webs.BuildAndDeployWebProject(it)
              })
                } else {
              projects.each({
                    if (env.GIT_CHANGE.contains(it)) {
                  println("项目 === ${it} ===已更改,进行构建。")
                  webs.BuildAndDeployWebProject(it)
                    } else {
                  println("项目 === ${it} === 业务内容未更改,不再构建。")
                    }
              })
                }
              }
            }
          }
      }

      post {
        success {
          script {
            // tool.TagIt(projectId, env.BRANCH_NAME);
            try {
            if (env.BRANCH_NAME == 'master') {
                tool.PrintMsg('打tag start', 'blue')
                gitlab.TagIt(projectId, env.BRANCH_NAME)
                tool.PrintMsg('打tag end', 'blue')
              } else {
                tool.PrintMsg('不是master, 不打了', 'blue')
              }
            tool.DingItMarkdown([
                robotId: 'b2229249-b5ad-4d51-8788-f77706aba44c',
                atAll: false,
                projectName: 'xzl-webs',
                type: 'success'
              ],env)
            } catch (e) {
              tool.PrintMsg(e, 'red')
            }
          }
        }

        failure {
          script {
            try {
            tool.DingItMarkdown([
                robotId: 'b2229249-b5ad-4d51-8788-f77706aba44c',
                atAll: true,
                projectName: 'xzl-webs',
                type: 'failure'
              ],env)
            } catch (e) {
            PrintMsg(e, 'red')
            }
          }
        }

        aborted {
          echo '构建中断'
        }

        always {
          echo '构建结束'
        }
      }
  }
}
