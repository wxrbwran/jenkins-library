AGENT_LABEL = "MASTER"
pipeline {
    agent {
        label AGENT_LABEL
    }
    environment {
        CURRENT_PRJ_NAME = "apis-service"
        RUN_PORT = 8000
        ALIYUN_DOCKER_REGISTRY_LOGIN = credentials('aliyun-docker-registry-login-credential')
        YX_901_DOCKER_HARBOR = credentials('yx-901-harbor')
    }
    triggers {
        gitlab  triggerOnPush: true,
                triggerOnMergeRequest: false,
                branchFilterType: "All",
                secretToken: "${env.git_token}"
    }
    tools {
        gradle "GLOBAL_GRADLE_V6_6"
        jdk "JDK_11"
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        skipDefaultCheckout()
        disableConcurrentBuilds()
        timeout(time: 15, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
    }

    stages {
        stage('prepare') {
            steps {
                script {

                    env.RUN_PARAMS = "--spring.cloud.config.profile=${BRANCH_NAME} --spring.profiles.active=${BRANCH_NAME}"
                    if (env.BRANCH_NAME == 'shanxi_dev') {
                        env.TARGET_HOST_IP = "192.168.0.107"
                        env.EUREKA_URL = "http://192.168.0.107:7990/eureka/"

                        env.DOCKER_REGISTRY_HOST = "registry.cn-beijing.aliyuncs.com"
                        env.DOCKER_REGISTRY_URL = "https://${DOCKER_REGISTRY_HOST}"
                        env.DOCKER_REGISTRY_PREFIX = "${DOCKER_REGISTRY_HOST}"
                        env.DOCKER_REGISTRY_IMAGE_TARGET = "${DOCKER_REGISTRY_PREFIX}/xzl-dev/${CURRENT_PRJ_NAME}"
                        env.DOCKER_JRE_IMAGE = "${DOCKER_REGISTRY_PREFIX}/corp/jre:11u8"

                        AGENT_LABEL = "SX_DEV"
                    } else if (env.BRANCH_NAME == 'dev') {
                        env.TARGET_HOST_IP = "172.16.10.129"
                        env.EUREKA_URL = "http://172.16.10.129:7990/eureka/"

                        env.DOCKER_REGISTRY_HOST = "registry.cn-beijing.aliyuncs.com"
                        env.DOCKER_REGISTRY_URL = "https://${DOCKER_REGISTRY_HOST}"
                        env.DOCKER_REGISTRY_PREFIX = "${DOCKER_REGISTRY_HOST}"
                        env.DOCKER_REGISTRY_IMAGE_TARGET = "${DOCKER_REGISTRY_PREFIX}/xzl-dev/${CURRENT_PRJ_NAME}"
                        env.DOCKER_JRE_IMAGE = "${DOCKER_REGISTRY_PREFIX}/corp/jre:11u8"

                        AGENT_LABEL = "YX_DEV"
                    } else if (env.BRANCH_NAME == 'test') {
                        env.TARGET_HOST_IP = "172.16.10.128"
                        env.EUREKA_URL = "http://172.16.10.128:7990/eureka/"

                        env.DOCKER_REGISTRY_HOST = "registry.cn-beijing.aliyuncs.com"
                        env.DOCKER_REGISTRY_URL = "https://${DOCKER_REGISTRY_HOST}"
                        env.DOCKER_REGISTRY_PREFIX = "${DOCKER_REGISTRY_HOST}"
                        env.DOCKER_REGISTRY_IMAGE_TARGET = "${DOCKER_REGISTRY_PREFIX}/xzl-test/${CURRENT_PRJ_NAME}"
                        env.DOCKER_JRE_IMAGE = "${DOCKER_REGISTRY_PREFIX}/corp/jre:11u8"

                        AGENT_LABEL = "YX_DEV"
                    } else if (env.BRANCH_NAME == 'master') {
                        env.TARGET_HOST_IP = "172.16.10.122"
                        env.EUREKA_URL = "http://172.16.10.122:7990/eureka/"

                        env.DOCKER_REGISTRY_HOST = "registry.cn-beijing.aliyuncs.com"
                        env.DOCKER_REGISTRY_URL = "http://${DOCKER_REGISTRY_HOST}"
                        env.DOCKER_REGISTRY_PREFIX = "${DOCKER_REGISTRY_HOST}"
                        env.DOCKER_REGISTRY_IMAGE_TARGET = "${DOCKER_REGISTRY_PREFIX}/xzl-prod/${CURRENT_PRJ_NAME}"
                        env.DOCKER_JRE_IMAGE = "${DOCKER_REGISTRY_PREFIX}/corp/jre:11u8"

                        AGENT_LABEL = "YX_DEV"
                        env.RUN_PARAMS = "--spring.cloud.config.profile=prod --spring.profiles.active=prod"
                    } else if (env.BRANCH_NAME == 'prod_master') {
                        env.TARGET_HOST_IP = "10.0.2.129"
                        env.EUREKA_URL = "http://10.0.2.129:7990/eureka/"
                        env.DOCKER_REGISTRY_HOST = "registry-vpc.cn-beijing.aliyuncs.com"
                        env.DOCKER_REGISTRY_URL = "http://${DOCKER_REGISTRY_HOST}"
                        env.DOCKER_REGISTRY_PREFIX = "${DOCKER_REGISTRY_HOST}"
                        env.DOCKER_REGISTRY_IMAGE_TARGET = "${DOCKER_REGISTRY_PREFIX}/xzl-prod/${CURRENT_PRJ_NAME}"
                        env.DOCKER_JRE_IMAGE = "${DOCKER_REGISTRY_PREFIX}/corp/jre:11u8"
                        AGENT_LABEL = "MSA"
                        env.RUN_PARAMS = "--spring.cloud.config.profile=aliyun_prod --spring.profiles.active=aliyun_prod"
                    } else {

                        sh 'echo "???????????????????????????????????????!!!" && exit 1'
                    }
                }
            }
        }

        stage('select agent') {
            agent {
                label AGENT_LABEL as String
            }
            steps {
                sh 'curl -o- https://git.xzlcorp.com/OpenShared/auto-build/raw/master/install-gradle-plugin.sh | bash'
                sh 'gradle'
            }
        }

        stage('checkout code') {
            agent {
                label AGENT_LABEL as String
            }
            steps {
                git branch: "${BRANCH_NAME}",
                        credentialsId: "gitlab-ssh-key",
                        url: "git@git.xzlcorp.com:Backends/${CURRENT_PRJ_NAME}.git"
                sh "ls -lat"
            }
        }

        stage('build') {
            agent {
                label AGENT_LABEL as String
            }
            steps {
                script {
                    if (env.BRANCH_NAME != 'prod_master') {
                        sh 'gradle -Dorg.gradle.daemon=false clean'
                        sh '''
                            echo " ->???1??????????????? (Fat Jar)"
                            TASK=":publish"
                            if gradle tasks --all | grep "$TASK"
                            then
                                echo 'publish library artifact'
                                gradle -Dorg.gradle.daemon=false publish
                            else
                                echo 'no publish task'
                            fi
                            
                            if gradle tasks --all | grep "upgradeVersion"
                            then
                                echo 'upgradeVersion artifact to db'
                                gradle -Dorg.gradle.daemon=false upgradeVersion
                            else
                                gradle -Dorg.gradle.daemon=false build -x compileTestJava
                            fi
                        '''

                        sh '''
                            echo " ->???2?????????Docker ??????"
                            docker build \
                            --build-arg jre=${DOCKER_JRE_IMAGE} \
                            -t ${DOCKER_REGISTRY_IMAGE_TARGET} \
                            --pull=true \
                            ${WORKSPACE}
                        '''
                        sh '''
                            echo " -> ???3??? Docker ??????????????????"
                            docker login \
                            --username ${ALIYUN_DOCKER_REGISTRY_LOGIN_USR} \
                            --password ${ALIYUN_DOCKER_REGISTRY_LOGIN_PSW} \
                            ${DOCKER_REGISTRY_URL}
                            docker push ${DOCKER_REGISTRY_IMAGE_TARGET}
                        '''
                    }
                }
            }
        }

        stage('deploy') {
            agent {
                label AGENT_LABEL as String
            }
            steps {
                script {
                    sshagent(credentials: ['jenkins-self-ssh-key']) {
                        try {
                            writeFile file: "${CURRENT_PRJ_NAME}-pre-deploy.sh", text: '#!/bin/bash \n ' +
                                    'echo " -> ???1?????????????????????????????????" \n ' +
                                    'docker stop ' + "${CURRENT_PRJ_NAME}" + ' || true \n' +
                                    'docker container rm -f ' + "${CURRENT_PRJ_NAME}" + ' || true \n' +
                                    'docker image rmi --force ' + "${DOCKER_REGISTRY_IMAGE_TARGET}" + ' || true \n'
                            sh 'scp -o StrictHostKeyChecking=no ${CURRENT_PRJ_NAME}-pre-deploy.sh root@${TARGET_HOST_IP}:"~"'
                            sh 'ssh -o StrictHostKeyChecking=no -l root ${TARGET_HOST_IP} bash ~/${CURRENT_PRJ_NAME}-pre-deploy.sh'
                            sh 'ssh -o StrictHostKeyChecking=no -l root ${TARGET_HOST_IP} "rm -f ~/${CURRENT_PRJ_NAME}-pre-deploy.sh || true"'
                        } catch (exc) {
                            sh 'echo "?????????????????????????????????????????????!"'
                            //throw
                        } finally {
                            writeFile file: "${CURRENT_PRJ_NAME}-deploy.sh", text: '#!/bin/bash \n' +
                                    'echo " -> ???2??? ?????? Docker ????????????????????????"\n' +
                                    'docker login \\\n' +
                                    '--username ' + "${ALIYUN_DOCKER_REGISTRY_LOGIN_USR}"  + ' \\\n' +
                                    '--password ' + "${ALIYUN_DOCKER_REGISTRY_LOGIN_PSW}" + ' \\\n' +
                                    "${DOCKER_REGISTRY_URL}\n" +

                                    'docker run --log-opt max-size=10m --log-opt max-file=5 \\\n' +
                                    '-d --restart=always  \\\n' +
                                    '-e HOST_IP=$(echo $(hostname -I) | cut -d " " -f1) \\\n' +
                                    '-e EUREKA_URL=' + "${EUREKA_URL}" + ' \\\n' +
                                    '-e ZONE=' + "${BRANCH_NAME}" + ' \\\n' +
                                    '-p ' + "${RUN_PORT}" + ':' + "${RUN_PORT}" + ' \\\n' +
                                    '--name ' + "${CURRENT_PRJ_NAME}" + ' \\\n' +
                                    "${DOCKER_REGISTRY_IMAGE_TARGET}" + " ${RUN_PARAMS}"
                            sh 'scp -o StrictHostKeyChecking=no ${CURRENT_PRJ_NAME}-deploy.sh root@${TARGET_HOST_IP}:"~"'
                            sh 'ssh -o StrictHostKeyChecking=no -l root ${TARGET_HOST_IP} bash ~/${CURRENT_PRJ_NAME}-deploy.sh'
                            sh 'ssh -o StrictHostKeyChecking=no -l root ${TARGET_HOST_IP} "rm -f ~/${CURRENT_PRJ_NAME}-deploy.sh || true"'
//                        sh 'ssh -o StrictHostKeyChecking=no -l root 172.16.10.129 uname -a'
                        }
                    }
                }
            }
        }

        stage('clean') {
            agent {
                label AGENT_LABEL as String
            }
            steps {
//                TODO: clear docker image of local host
                sh 'echo "???????????????????????????"'
//                sh 'docker image rmi --force ${DOCKER_REGISTRY_IMAGE_TARGET} || true'
                cleanWs()
            }
        }
    }

    post {
        success {
            sh "echo suc!"
        }

        failure {
            sh "echo fail!"
        }
    }
}


