package com.xzlcorp

// webs项目

def BuildAndDeployWebProject(project) {
  // prepare
  // doctor cro nurse out org
  println("当前构建项目为： ${project}")
  // dev test master
  println("当前构建分支为： ${BRANCH_NAME}")

  // env.BRANCH_NAME = "dev"

  if (BRANCH_NAME == 'master') {
    env.BUILD_SH = "pnpm dist:${project}"
    env.SERVER_PATH = 'n'
  } else if (BRANCH_NAME == 'test') {
    env.BUILD_SH = "pnpm prerelease:${project}"
    env.SERVER_PATH = 'n.test'
  } else if (BRANCH_NAME == 'dev') {
    env.BUILD_SH = "pnpm dev-dist:${project}"
    env.SERVER_PATH = 'n.dev'
  }
  env.DIST = "xzl-web-${project}"
  if (project == 'cro') {
    env.DIST = 'clinical-cro'
  }
  if (project == 'out') {
    env.DIST = 'out-hospital-patient'
  }
  if (project == 'jiupin') {
    env.DIST = 'jiupin'
  }
  env.ROOT_PATH = "/Users/xinzhilici/homebrew/var/www/${SERVER_PATH}/${DIST}"
  env.TARGET_HOST_IP = '172.16.10.126'

  //and build
  sh "${BUILD_SH}"

  // deploy
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
