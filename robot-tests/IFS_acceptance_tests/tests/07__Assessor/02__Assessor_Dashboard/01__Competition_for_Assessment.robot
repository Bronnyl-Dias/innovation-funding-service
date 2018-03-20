*** Settings ***
Documentation     INFUND-5432 As an assessor I want to receive an alert to complete my profile when I log into my dashboard so that I can ensure that it is complete.
Suite Setup       The user logs-in in new browser  &{assessor_credentials}
Force Tags        Assessor
Resource          ../../../resources/defaultResources.robot

*** Test Cases ***
Assessment should not be visible when the deadline has passed
    [Documentation]  INFUND-1188
    [Tags]  MySQL
    ${assessorsDeadline} =  Save competition's current assessor deadline  ${IN_ASSESSMENT_COMPETITION}
    When The assessment deadline for the ${IN_ASSESSMENT_COMPETITION_NAME} changes to the past
    And the user reloads the page
#    Then The user should not see the element    link=${IN_ASSESSMENT_COMPETITION_NAME}
    [Teardown]  Execute sql string  UPDATE `${database_name}`.`milestone` SET `DATE`='${assessorsDeadline}' WHERE `competition_id`='${IN_ASSESSMENT_COMPETITION}' and `type`='ASSESSOR_DEADLINE';
# TODO update when IFS-3148

*** Keywords ***
Save competition's current assessor deadline
    [Arguments]  ${competitionId}
    Connect to Database  @{database}
    ${result} =  Query  SELECT DATE_FORMAT(`date`, '%Y-%l-%d %H:%i:%s') FROM `${database_name}`.`milestone` WHERE `competition_id`='${competitionId}' AND type='ASSESSOR_DEADLINE';
    ${result} =  get from list  ${result}  0
    ${assDeadline} =  get from list  ${result}  0
    [Return]  ${assDeadline}