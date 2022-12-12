// ---------------------------------------------------------------------------------------------------------------//
// ---------------------------------------- How to call this method ----------------------------------------------//
//            githubCheckout(url: REPOSITORY_URL , sha: BRANCH/COMMIT/TAG, creds: CREDENTIAL_ID)                  //
//                                                                                                                //
// steps {                                                                                                        //
//     githubCheckout(url: 'git@github.com/MyCompany/MyRepo.git', sha: 'develop', creds: 'credentials_identity')  //
// }                                                                                                              //
// ---------------------------------------------------------------------------------------------------------------//

def call(Map params = [:]) {
    assert params.url != null
    assert params.url != ""
    assert (params.url).contains("git@")
    assert params.sha != null
    assert params.sha != ""
    assert params.creds != null
    assert params.creds != ""

    checkout([$class                     : 'GitSCM',
        branches                         : [[name: "${params.sha}"]],
        doGenerateSubmoduleConfigurations: false,
        extensions                       : [],
        gitTool                          : 'Default',
        submoduleCfg                     : [],
        userRemoteConfigs                : [[credentialsId: params.creds, url: params.url]]
    ])
}