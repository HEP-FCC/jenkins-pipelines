#!groovy

@Library('root-pipelines')
import cern.root.pipeline.*

properties([
    parameters([
        string(name: 'ghprbPullId', defaultValue: '1'),
        string(name: 'ghprbGhRepository', defaultValue: 'root/root'),
        string(name: 'ghprbCommentBody', defaultValue: '@phsft-bot build'),
        string(name: 'sha1', defaultValue: '48214f30056e120818ea73b9fadf7b72268bc7de'),
        string(name: 'MODE', defaultValue: 'experimental'),
        string(name: 'VERSION', defaultValue: 'master', description: 'Branch to be built'),
        string(name: 'EXTERNALS', defaultValue: 'ROOT-latest', description: ''),
        string(name: 'EMPTY_BINARY', defaultValue: 'false', description: 'Boolean to empty the binary directory (i.e. to force a full re-build)'),
        string(name: 'ExtraCMakeOptions', defaultValue: '-Dvc=OFF -Dimt=OFF -Dccache=ON', description: 'Additional CMake configuration options of the form "-Doption1=value1 -Doption2=value2"'),
        string(name: 'MODE', defaultValue: 'experimental', description: 'The build mode')
    ])
])

GitHub gitHub = new GitHub(this, 'Regular PR', params.ghprbGhRepository, ghprbPullId, params.sha1)
BotParser parser = new BotParser(this, gitHub, params.ExtraCMakeOptions)
GenericBuild build = new GenericBuild(this)

if (parser.isParsableComment(params.ghprbCommentBody.trim())) {
    parser.parse()
}

parser.postStatusComment()
parser.configure(this, build)

stage('Building') {
    build.build()

    if (currentBuild.result == Result.SUCCESS) {
        gitHub.setSucceedCommitStatus('Build passed', currentBuild)
    } else {
        gitHub.setFailedCommitStatus('Build failed', currentBuild)
    }
}

stage('Publish reports') {
    build.sendEmails()
}
