def call (Map params = [:]) {

    step([
        $class: 'GitHubCommitStatusSetter',
        reposSource: [$class: "ManuallyEnteredRepositorySource", url: ${params.repo}],
        commitShaSource: [$class: "ManuallyEnteredShaSource", sha: "${params.sha}"],
        errorHandlers: [[$class: 'ShallowAnyErrorHandler']],
        statusResultSource: [
            $class: 'ConditionalStatusResultSource',
            results: [
            [$class: 'BetterThanOrEqualBuildResult', result: 'SUCCESS', state: 'SUCCESS', message: ${params.description}],
            [$class: 'BetterThanOrEqualBuildResult', result: 'UNSTABLE', state: 'SUCCESS', message: ${params.description}],
            [$class: 'BetterThanOrEqualBuildResult', result: 'FAILURE', state: 'FAILURE', message: ${params.description}],
            [$class: 'AnyBuildResult', state: 'FAILURE', message: 'Loophole']
            ]
        ]
    ])
}