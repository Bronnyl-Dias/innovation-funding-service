*** Settings ***
Documentation     INFUND-887 : As an applicant I want the option to look up my business organisation's details using Companies House lookup so I don’t have to type it in manually as part of the registration process
Suite Setup       The guest user opens the browser
Suite Teardown    TestTeardown User closes the browser
Resource          ../../../resources/GLOBAL_LIBRARIES.robot
Resource          ../../../resources/variables/GLOBAL_VARIABLES.robot
Resource          ../../../resources/variables/User_credentials.robot
Resource          ../../../resources/keywords/Login_actions.robot
Resource          ../../../resources/keywords/Applicant_actions.robot

*** Test Cases ***
Search using valid company name
    [Documentation]    INFUND-887
    [Tags]    Applicant    Company house    HappyPath
    Given user navigates to the page    ${SEARCH_COMPANYHOUSE_URL}
    When user enters text to a text field    id=org-name    innovate
    And user clicks the button/link    id=org-search
    Then the valid company names matching the search criteria should be displayed

Search using invalid company name
    [Documentation]    INFUND-887
    [Tags]    Applicant    Company house
    Given user navigates to the page    ${SEARCH_COMPANYHOUSE_URL}
    When user enters text to a text field    id=org-name    innoavte
    And user clicks the button/link    id=org-search
    Then the search criteria should not fetch any result

Search using valid registration number
    [Documentation]    INFUND-887
    [Tags]    Applicant    Company house    HappyPath
    Given user navigates to the page    ${SEARCH_COMPANYHOUSE_URL}
    When user enters text to a text field    id=org-name    05493105
    And user clicks the button/link    id=org-search
    Then the valid company names matching the search criteria should be displayed

search using invalid registration number
    [Documentation]    INFUND-887
    [Tags]    Applicant    Company house
    Given user navigates to the page    ${SEARCH_COMPANYHOUSE_URL}
    When user enters text to a text field    id=org-name    64536
    And user clicks the button/link    id=org-search
    Then the search criteria should not fetch any result

Search for invalid characters
    [Documentation]    INFUND-887
    [Tags]    Applicant    Company house    Pending
    Given user navigates to the page    ${SEARCH_COMPANYHOUSE_URL}
    When user enters text to a text field    id=org-name    {}{}
    And user clicks the button/link    id=org-search
    Then the applicant should get a validation error for the company house

*** Keywords ***
the valid company names matching the search criteria should be displayed
    Page Should Contain    05493105 - Incorporated on 28 June 2005
    Click Link    INNOVATE LTD
    Page Should Contain    Business Organisation
    Page Should Contain    Organisation name
    Element Should Contain    css=.form-block p:nth-child(2)    INNOVATE LTD
    Page Should Contain    Registration number
    Page should contain    05493105
    # Page Should Contain    Address
    # Page Should Contain    M45 8QP

the search criteria should not fetch any result
    Page Should Contain    Sorry we couldn't find any results within Companies House.

the applicant should get a validation error for the company house
    Element Should Be Visible    css=.error-message
