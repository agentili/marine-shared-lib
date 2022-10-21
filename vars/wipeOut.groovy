
def call() {
    deleteDir()
    cleanWs deleteDirs: true, disableDeferredWipeout: true
}