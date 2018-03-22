#!/usr/bin/env bash

set -e

PROJECT=$1
TARGET=$2
VERSION=$3

. $(dirname $0)/deploy-functions.sh
. $(dirname $0)/local-deploy-functions.sh

PROJECT=$(getProjectName $PROJECT $TARGET)
SVC_ACCOUNT_TOKEN=$(getSvcAccountToken)
HOST=$(getHost $TARGET)
ROUTE_DOMAIN=$(getRouteDomain $TARGET $HOST)
REGISTRY=$(getRegistry)
INTERNAL_REGISTRY=$(getInternalRegistry)
SVC_ACCOUNT_CLAUSE=$(getSvcAccountClause $TARGET $PROJECT $SVC_ACCOUNT_TOKEN)
REGISTRY_TOKEN=$SVC_ACCOUNT_TOKEN

function upgradeServices {
    # data-service
    oc apply -f $(getBuildLocation)/31-data-service.yml ${SVC_ACCOUNT_CLAUSE}
    rolloutStatus "data-service"

    # services
    oc apply -f $(getBuildLocation)/4-application-service.yml ${SVC_ACCOUNT_CLAUSE}
    oc apply -f $(getBuildLocation)/5-front-door-service.yml ${SVC_ACCOUNT_CLAUSE}
    oc apply -f $(getBuildLocation)/41-assessment-svc.yml ${SVC_ACCOUNT_CLAUSE}
    oc apply -f $(getBuildLocation)/42-competition-mgt-svc.yml ${SVC_ACCOUNT_CLAUSE}
    oc apply -f $(getBuildLocation)/43-project-setup-mgt-svc.yml ${SVC_ACCOUNT_CLAUSE}
    oc apply -f $(getBuildLocation)/44-project-setup-svc.yml ${SVC_ACCOUNT_CLAUSE}
    oc apply -f $(getBuildLocation)/45-registration-svc.yml ${SVC_ACCOUNT_CLAUSE}
    oc apply -f $(getBuildLocation)/46-prototypes-svc.yml ${SVC_ACCOUNT_CLAUSE}
    oc apply -f $(getBuildLocation)/shib/5-shib.yml ${SVC_ACCOUNT_CLAUSE}
    oc apply -f $(getBuildLocation)/shib/56-idp.yml ${SVC_ACCOUNT_CLAUSE}

    # The SIL stub is required in all environments, in one form or another, except for production
    if ! $(isProductionEnvironment ${TARGET}); then
        oc apply -f $(getBuildLocation)/sil-stub/80-sil-stub.yml ${SVC_ACCOUNT_CLAUSE}
    fi

    if ! $(isNamedEnvironment ${TARGET}); then
        oc apply -f $(getBuildLocation)/finance-data-service/32-finance-data-service.yml ${SVC_ACCOUNT_CLAUSE}
    fi


    watchStatus
}

function forceReload {
    oc rollout latest dc/data-service ${SVC_ACCOUNT_CLAUSE}
    rolloutStatus data-service

    oc rollout latest dc/application-svc ${SVC_ACCOUNT_CLAUSE}
    oc rollout latest dc/front-door-svc ${SVC_ACCOUNT_CLAUSE}
    oc rollout latest dc/assessment-svc ${SVC_ACCOUNT_CLAUSE}
    oc rollout latest dc/competition-mgt-svc ${SVC_ACCOUNT_CLAUSE}
    oc rollout latest dc/project-setup-mgt-svc ${SVC_ACCOUNT_CLAUSE}
    oc rollout latest dc/project-setup-svc ${SVC_ACCOUNT_CLAUSE}
    oc rollout latest dc/registration-svc ${SVC_ACCOUNT_CLAUSE}
    oc rollout latest dc/prototypes-svc ${SVC_ACCOUNT_CLAUSE}
    oc rollout latest dc/idp ${SVC_ACCOUNT_CLAUSE}
    oc rollout latest dc/shib ${SVC_ACCOUNT_CLAUSE}

    # The SIL stub is required in all environments, in one form or another, except for production
    if ! $(isProductionEnvironment ${TARGET}); then
        oc rollout latest dc/sil-stub ${SVC_ACCOUNT_CLAUSE}
    fi

    if ! $(isNamedEnvironment ${TARGET}); then
        oc rollout latest dc/finance-data-service ${SVC_ACCOUNT_CLAUSE}
    fi

    watchStatus
}

function watchStatus {
    rolloutStatus application-svc
    rolloutStatus front-door-svc
    rolloutStatus assessment-svc
    rolloutStatus competition-mgt-svc
    rolloutStatus project-setup-mgt-svc
    rolloutStatus project-setup-svc
    rolloutStatus registration-svc
    rolloutStatus prototypes-svc
    rolloutStatus idp
    rolloutStatus shib

    # The SIL stub is required in all environments, in one form or another, except for production
    if ! $(isProductionEnvironment ${TARGET}); then
        rolloutStatus sil-stub
    fi

    if ! $(isNamedEnvironment ${TARGET}); then
        rolloutStatus finance-data-service
    fi
}

function rolloutStatus {
    FINISHED=0
    while [  $FINISHED -ne 1 ]; do
        RESULT=$(oc rollout status  dc/$1 ${SVC_ACCOUNT_CLAUSE} 2>&1) || true # the || true is to eat up all non-zero exit codes
        echo "$RESULT"

        if [[ ${RESULT} != *"timed out"* ]]; then
          FINISHED=1
        fi
        if [[ ${RESULT} == *"failed progressing"* ]]; then
          echo "FAILED TO DEPLOY $1"
          echo "Try triggering the deploy again with oc rollout latest dc/application-svc in project $PROJECT and check oc logs and oc describe heavily"
          exit 1
        fi
    done
}

# Entry point
tailorAppInstance

if [[ ${TARGET} == "local" ]]
then
    replacePersistentFileClaim
fi

useContainerRegistry
upgradeServices

if [[ ${bamboo_openshift_force_reload} == "true" ]]
then
    forceReload
fi

if [[ ${TARGET} == "production" || ${TARGET} == "uat" || ${TARGET} == "perf" ]]
then
    # We only scale up data-service once data-service started up and performed the Flyway migrations on one thread
    scaleDataService
    # TODO: IFS-2657 Include scaleFinanceDataService once it is completed and ready for prdouction deploy (along with other script changes above)
    # Below is not required for now, it will be put back in when we start deploying it to production & UAT in future
    # scaleFinanceDataService
fi
