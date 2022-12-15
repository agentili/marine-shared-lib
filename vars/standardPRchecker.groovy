def call(Map pipelineParams) {

    // Asserts
    assert pipelineParams.gitRepoUrl != null
    // assert (pipelineParams.gitRepoUrl).contains("https://")
    assert pipelineParams.gitRepoSshUrl != null
    assert (pipelineParams.gitRepoSshUrl).contains("git@")
    assert pipelineParams.branch != null

    pipeline {
        agent {
            label pipelineParams.agent
        }
        triggers {
            githubPullRequests abortRunning: true, cancelQueued: true, events: [commitChanged(), Open()], preStatus: true, repoProviders: [githubPlugin(repoPermission: 'PULL')], spec: 'H/2 * * * *', triggerMode: 'CRON'
        }
        environment {
            NEXUS_HOST="https://nexus.navionics.it"
        }
        options {
            timeout(time: 1, unit: 'HOURS')
            disableConcurrentBuilds()
            ansiColor('xterm')
            buildDiscarder(logRotator(numToKeepStr: '10', daysToKeepStr: '10'))
            githubProjectProperty(displayName: '', projectUrlStr: pipelineParams.gitRepoUrl)
        }
        stages {
            stage('CleanUp') {
                steps {
                    sh 'docker run --rm -v ${WORKSPACE}:/work -w /work -e L_UID=$(id -u) -e G_UID=$(id -g) ubuntu:20.04 bash -c \'chown -R ${L_UID}:${G_UID} . \''
                    deleteDir()
                    cleanWs deleteDirs: true, disableDeferredWipeout: true
                }
            }
            stage('Checkout') {
                steps{
                    checkout([
                        $class                           : 'GitSCM',
                        branches                         : [[name: pipelineParams.branch]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions                       : [],
                        gitTool                          : 'Default',
                        submoduleCfg                     : [],
                        userRemoteConfigs                : [[credentialsId: '03ce9989-445b-437a-868c-64293e2c1de6', url: pipelineParams.gitRepoSshUrl ]]
                    ])
                }
            }
            stage("Build and Test"){
                steps{
                    withCredentials([
                        usernamePassword(credentialsId: '8c7b1b3b-651e-413b-be60-7715749bdce4', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USER'),
                        usernamePassword(credentialsId: 'd625b3f5-a463-4067-83fd-9053eb95e120', passwordVariable: 'NEXUS_PASSWORD', usernameVariable: 'NEXUS_USER')
                    ]) {
                        sh 'docker-compose -f environments/docker/docker-compose.yml down --rmi all -v'
                        sh 'docker-compose -f environments/docker/docker-compose.yml up --build java-builder-service'
                        junit '**/target/*reports/*.xml'
                        publishCoverage(
                            adapters: [
                                jacocoAdapter(path: "**/target/site/jacoco/*.xml")
                            ],
                            sourceFileResolver: sourceFiles('STORE_LAST_BUILD'),
                            failNoReports: false,
                            failUnhealthy: false,
                            failUnstable: false,
                            globalThresholds: [[
                                thresholdTarget: 'Line',
                                unhealthyThreshold: 50.0,
                                unstableThreshold: 50.0
                            ]]
                        )
                    }
                }
            }
        }
        post {
            failure {
                emailext( attachLog: true,
                    compressLog: true,
                    recipientProviders: [culprits()],
                    subject: "[${currentBuild.projectName}] Failed Pipeline: ${currentBuild.fullDisplayName}",
                    body: "Something is wrong with ${env.BUILD_URL} on branch ${pipelineParams.branch}, please check the log"
                )
            }
            always {
                step([
                    $class: 'GitHubCommitStatusSetter',
                    reposSource: [$class: "ManuallyEnteredRepositorySource", url: pipelineParams.gitRepoSshUrl],
                    commitShaSource: [$class: "ManuallyEnteredShaSource", sha: pipelineParams.branch],
                    errorHandlers: [[$class: 'ShallowAnyErrorHandler']],
                    statusResultSource: [
                        $class: 'ConditionalStatusResultSource',
                        results: [
                            [$class: 'BetterThanOrEqualBuildResult', result: 'UNSTABLE', state: 'SUCCESS', message: "Jenkins Job ${env.BUILD_NUMBER} - Result: ${currentBuild.currentResult} "],
                            [$class: 'BetterThanOrEqualBuildResult', result: 'FAILURE', state: 'FAILURE', message: "Jenkins Job ${env.BUILD_NUMBER} - Result: ${currentBuild.currentResult} "],
                            [$class: 'AnyBuildResult', state: 'FAILURE', message: 'Loophole']
                        ]
                    ]
                ])
                sh "docker-compose -f environments/docker/docker-compose.yml down --rmi local -v"
                sh "docker system prune -f"
                sh 'docker run --rm -v ${WORKSPACE}:/work -w /work -e L_UID=$(id -u) -e G_UID=$(id -g) ubuntu:20.04 bash -c \'chown -R ${L_UID}:${G_UID} . \''
                sh 'docker run --rm -v ${WORKSPACE}:/work -w /work -e L_UID=$(id -u) -e G_UID=$(id -g) ubuntu:20.04 bash -c \'chown -R ${L_UID}:${G_UID} .git \''
                deleteDir()
                cleanWs deleteDirs: true, disableDeferredWipeout: true
            }
        }
    }

}