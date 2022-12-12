// ----------------------------------- //
// ----- How to call this method ----- //
//            wipeOut()                //
//                                     //
// steps {                             //
//    wipeOut()                        //
// }                                   //
// ----------------------------------- //

def call() {
    sh 'docker run --rm -v ${WORKSPACE}:/work -w /work -e L_UID=$(id -u) -e G_UID=$(id -g) ubuntu:20.04 bash -c \'chown -R ${L_UID}:${G_UID} . \''
    sh 'docker run --rm -v ${WORKSPACE}:/work -w /work -e L_UID=$(id -u) -e G_UID=$(id -g) ubuntu:20.04 bash -c \'chown -R ${L_UID}:${G_UID} .git \''
    deleteDir()
    cleanWs deleteDirs: true, disableDeferredWipeout: true
}