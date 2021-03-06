*** Settings ***
Documentation   IFS-984 Innovation Leads user journey navigation
...
...             IFS-191 Innovation Lead Stakeholder view filtered dashboard
...
...             IFS-1308 Innovation Leads: Project Setup
Suite Setup     The user logs-in in new browser  &{innovation_lead_one}
Suite Teardown  the user closes the browser
Force Tags      InnovationLead  HappyPath
Resource        ../../resources/defaultResources.robot
Resource        ../02__Competition_Setup/CompAdmin_Commons.robot
Resource        ../10__Project_setup/PS_Common.robot

*** Test Cases ***
Innovation Lead should see Submitted and Ineligible Applications
    [Documentation]  IFS-984
    [Tags]
    Given the user navigates to the page       ${CA_Live}
    Then the user should see all live competitions
    When the user navigates to the page        ${server}/management/competition/${IN_ASSESSMENT_COMPETITION}
    Then the user should not see the element   jQuery = a:contains("View and update competition setup")
    When the user clicks the button/link       link = Applications: Submitted, ineligible
    And the user clicks the button/link        link = Submitted applications
    Then the user should see the element       jQuery = td:contains("${IN_ASSESSMENT_APPLICATION_4_TITLE}") ~ td:contains("57,803")
    When the user navigates to the page        ${server}/management/competition/${IN_ASSESSMENT_COMPETITION}/applications/ineligible
    Then the user should see the element       css = #application-list
    When the user clicks the button/link       jQuery = a:contains(${application_ids["Ineligible Virtualisation"]})
    Then the user should not see the element   jQuery = .govuk-button:contains("Reinstate application")
    When the user clicks the button/link       jQuery = a:contains("Back")
    Then the user should not see the element   jQuery = .govuk-button:contains("Inform applicant")

Innovation lead cannot access CompSetup, Invite Assessors, Manage assessments, Funding decision, All Applictions
    [Documentation]  IFS-984, IFS-1414
    [Tags]
    The user should see permission error on page  ${server}/management/competition/setup/${openCompetitionRTO}
    The user should see permission error on page  ${server}/management/competition/${IN_ASSESSMENT_COMPETITION}/assessors/find
    The user should see permission error on page  ${server}/management/assessment/competition/${CLOSED_COMPETITION}
    The user should see permission error on page  ${server}/management/competition/${FUNDERS_PANEL_COMPETITION_NUMBER}/funding
    The user should see permission error on page  ${server}/management/competition/${IN_ASSESSMENT_COMPETITION}/applications/all

Innnovation lead can see competitions assigned to him only
    [Documentation]  IFS-191  IFS-1308
    [Tags]  CompAdmin
    [Setup]  log in as a different user   &{Comp_admin1_credentials}
    Given The Competition Admin assigns the Innovation Lead to a competition  ${COMP_MANAGEMENT_UPDATE_COMP}/manage-innovation-leads/find
    And The Competition Admin assigns the Innovation Lead to a competition    ${server}/management/competition/setup/${PROJECT_SETUP_COMPETITION}/manage-innovation-leads/find
    When Log in as a different user       &{innovation_lead_two}
    Then the user should see the element  link = ${openGenericCompetition}
    And the user should see the element   link = ${openCompetitionRTO_name}
    And the user should not see the text in the page  ${openCompetitionBusinessRTO_name}
    When the user clicks the button/link  css = #section-4 a  #Project setup tab
    Then the user should see the element  link = ${PROJECT_SETUP_COMPETITION_NAME}
    [Teardown]  The user clicks the button/link  link = Dashboard

Innovation lead can only search for applications assigned to them
    [Documentation]  IFS-4564
    Given the user enters text to a text field    searchQuery  ${FUNDERS_PANEL_APPLICATION_1_NUMBER}
    When the user clicks the button/link          id = searchsubmit
    And the user clicks the button/link           link = ${FUNDERS_PANEL_APPLICATION_1_NUMBER}
    Then the user should see the element          jQuery = span:contains("${FUNDERS_PANEL_APPLICATION_1_TITLE}")
    [Teardown]  The user clicks the button/link   link = Dashboard

Innovation lead cannot search for unassigned applications
    [Documentation]  IFS-4564
    Given the user enters text to a text field    searchQuery  ${INFORM_COMPETITION_NAME_1_NUMBER}
    When the user clicks the button/link          id = searchsubmit
    Then the user should see the element          jQuery = p:contains("0") strong:contains("${INFORM_COMPETITION_NAME_1_NUMBER}")
    [Teardown]  The user clicks the button/link   link = Dashboard

*** Keywords ***
The user should see permission error on page
    [Arguments]  ${page}
    The user navigates to the page and gets a custom error message  ${page}  ${403_error_message}

The Competition Admin assigns the Innovation Lead to a competition
    [Arguments]  ${path}
    the user navigates to the page       ${path}
    the user clicks the button/link      jQuery = td:contains("Peter Freeman") button:contains("Add")
    the user should not see the element  jQuery = td:contains("Peter Freeman")