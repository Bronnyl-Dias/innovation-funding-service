#!/bin/bash
set -e

. $(dirname $0)/deploy-functions.sh
. $(dirname $0)/local-deploy-functions.sh

PROJECT=$(oc project -q)
shift 1
ROBOT_COMMAND=$@
HOST=$(getClusterAddress)
ROUTE_DOMAIN=apps.${HOST}
REGISTRY=docker-registry-default.apps.$(getClusterAddress)
INTERNAL_REGISTRY=172.30.80.28:5000


echo "Deploying tests to the current oc project ($PROJECT)"

function tailorToAppInstance() {
    rm -rf $(getBuildLocation)
    echo $(getBuildLocation)
    echo "Starting to copy os-files to $getBuildLocation"
    cp -r os-files $(getBuildLocation)
    sed -i.bak "s#innovateuk/#${INTERNAL_REGISTRY}/${PROJECT}/#g" $(getBuildLocation)/robot-tests/*.yml
    sed -i.bak "s/<<SHIB-ADDRESS>>/$PROJECT.$ROUTE_DOMAIN/g" $(getBuildLocation)/robot-tests/*.yml

    cp -r robot-tests robot-tests-tmp
    sed -i.bak "s/<<SHIB-ADDRESS>>/$PROJECT.$ROUTE_DOMAIN/g" robot-tests-tmp/openshift/*.sh
    sed -i.bak "s/<<SHIB-ADDRESS>>/$PROJECT.$ROUTE_DOMAIN/g" robot-tests-tmp/os_run_tests.sh
    sed -i.bak "s#\[\"./os_run_tests.sh\", \"-q\"\]#[\"./os_run_tests.sh\", \"-q\", \"$ROBOT_COMMAND\"]#g" robot-tests-tmp/Dockerfile

}

function cleanUp() {
    rm -rf robot-tests-tmp/
    rm -rf $(getBuildLocation)
}

function buildAndPushTestImages() {
    docker build -t ${REGISTRY}/${PROJECT}/robot-framework:1.0-SNAPSHOT robot-tests-tmp/
    docker login -p $(oc whoami -t) -u unused ${REGISTRY}
    docker push ${REGISTRY}/${PROJECT}/robot-framework:1.0-SNAPSHOT
}

function deployTests() {
    oc create -f $(getBuildLocation)/robot-tests/7-chrome.yml
    sleep 5
    oc create -f $(getBuildLocation)/robot-tests/8-robot.yml
    sleep 2
}

function fileFixtures() {
    chmod +x robot-tests/openshift/addtestFiles.sh
    ./robot-tests/openshift/addtestFiles.sh
}

function copyNecessaryFiles() {
    cp -r ifs-data-layer/ifs-data-service/docker-build.gradle robot-tests-tmp/docker-build.gradle
}

function navigateToRoot(){
    scriptDir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

    cd ${scriptDir}
    cd ../../..
}

navigateToRoot
cleanUp
rm -rf robot-tests/target && mkdir robot-tests/target
fileFixtures
tailorToAppInstance
copyNecessaryFiles
buildAndPushTestImages
deployTests
cleanUp

sleep 5

echo ""
echo "Tests are running now. You can follow the progress with the following command:"
echo "oc logs -f $(oc get pods | grep robot-framework-1- | grep -v deploy | awk '{ print $1 }')"
