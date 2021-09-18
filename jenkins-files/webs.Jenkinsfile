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

    stages {
        stage('clean') {
          steps {
            script {
              cleanWs()
            }
          }
        }
        stage('prepare') {
          steps {
            sh 'node -v'
            script {
              list = env.BRANCH_NAME.split("-");
              env.PROJECT = list[0];
              env.BRANCH = list[1];
              // doctor cro nurse out org
              println("当前构建项目为： ${PROJECT}");
              // dev test master
              println("当前构建分支为： ${BRANCH}");

              if (BRANCH == 'master') {
                env.BUILD_SH = "pnpm dist:${PROJECT}"
                env.SERVER_PATH = "n";
              } else if (BRANCH == 'test') {
                env.BUILD_SH = "pnpm prerelease:${PROJECT}"
                env.SERVER_PATH = "n.test";
              } else if (BRANCH == 'dev') {
                env.BUILD_SH = "pnpm dev-dist:${PROJECT}"
                env.SERVER_PATH = "n.dev";
              }
              env.DIST = "xzl-web-${env.PROJECT}"
              if (env.PROJECT == "cro") {
                env.DIST = "clinical-cro"
              }
              if (env.PROJECT == "out") {
                env.DIST = "out-hospital-patient"
              }
              env.ROOT_PATH = "/Users/xinzhilici/homebrew/var/www/${SERVER_PATH}/${DIST}";
              env.TARGET_HOST_IP = '172.16.10.126'
            }
          }
        }

        stage('checkout') {
          steps {
            git branch: "${BRANCH_NAME}",
            credentialsId: 'gitlab-ssh-key',
            url: 'git@git.xzlcorp.com:UnitedFrontEnd/xzl-webs.git'
            sh 'ls -lat'
          }
        }

        stage('build') {
          steps {
            sh 'npm i -g pnpm --registry=https://registry.npm.taobao.org'
            sh "pnpm install --registry=https://registry.npm.taobao.org"
            sh "${BUILD_SH}"
          }
        }

        stage('deploy') {
          steps {
            script {
              if (env.BRANCH_NAME == 'master') {
                sh "AutoBuilder transfer  --rp ./${DIST} --wp *"
                } else {
                sshagent(credentials: ['jenkins-self-ssh-key']) {
                    sh 'ssh -o StrictHostKeyChecking=no -l xinzhilici ${TARGET_HOST_IP} "rm -rf ${ROOT_PATH} || true"'
                    sh 'ssh -o StrictHostKeyChecking=no -l xinzhilici ${TARGET_HOST_IP} "mkdir -p ${ROOT_PATH} || true"'
                    sh 'scp -o StrictHostKeyChecking=no -r ./${DIST}/* xinzhilici@${TARGET_HOST_IP}:"${ROOT_PATH}"'
                }
              }
            }
          }
        }
    }

    post {
      always {
        echo "构建结束"
      }

      success {
        echo "构建成功"
          git branch: "${BRANCH_NAME}",
           credentialsId: 'gitlab-ssh-key',
           url: 'git@git.xzlcorp.com:UnitedFrontEnd/xzl-webs.git'
          script{
            if (env.BRANCH == "master") {
              def GIT_TAG = new Date().format(“yyyyMMddHHmmss”);
              git tag -a -m “${GIT_TAG}” ${GIT_TAG}
              git push origin --tags
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

