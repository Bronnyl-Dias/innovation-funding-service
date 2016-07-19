*** Settings ***
Documentation     INFUND-43 As an applicant and I am on the application form on an open application, I will receive feedback if I my input is invalid, so I know how I should enter the question
Suite Setup       Run keywords    log in and create new application if there is not one already
...               AND    Applicant goes to the application details page of the Robot application
Suite Teardown    TestTeardown User closes the browser
Force Tags        Applicant
Resource          ../../../../resources/GLOBAL_LIBRARIES.robot
Resource          ../../../../resources/variables/GLOBAL_VARIABLES.robot
Resource          ../../../../resources/variables/User_credentials.robot
Resource          ../../../../resources/keywords/Login_actions.robot
Resource          ../../../../resources/keywords/User_actions.robot
Resource          ../../../../resources/keywords/SUITE_SET_UP_ACTIONS.robot

*** Test Cases ***
Title field client side
    [Documentation]    INFUND-43
    ...
    ...    INFUND-2843
    Given the user should see the text in the page    Application details
    When the user enters text to a text field    id=application_details-title    ${EMPTY}
    And the user should see an error    Please enter the full title of the project
    And the user enters text to a text field    id=application_details-title    Robot test application
    And the applicant should not see the validation error any more

Day field client side
    [Documentation]    INFUND-43
    ...
    ...    INFUND-2843
    [Tags]    HappyPath
    [Setup]    The applicant inserts a valid date
    When the user enters text to a text field    id=application_details-startdate_day    32
    Then the user should see an error    Please enter a valid date
    When the user enters text to a text field    id=application_details-startdate_day    0
    Then the user should see an error    Please enter a valid date
    When the user enters text to a text field    id=application_details-startdate_day    -1
    Then the user should see an error    Please enter a valid date
    When the user enters text to a text field    id=application_details-startdate_day    ${EMPTY}
    Then the user should see an error    Please enter a valid date
    When the applicant inserts a valid date
    Then the applicant should not see the validation error any more

Month field client side
    [Documentation]    INFUND-43
    ...
    ...    INFUND-2843
    [Tags]
    [Setup]    The applicant inserts a valid date
    When the user enters text to a text field    id=application_details-startdate_month    0
    Then the user should see an error    Please enter a valid date
    When the user enters text to a text field    id=application_details-startdate_month    13
    Then the user should see an error    Please enter a valid date
    When the user enters text to a text field    id=application_details-startdate_month    -1
    Then the user should see an error    Please enter a valid date
    When the user enters text to a text field    id=application_details-startdate_month    ${EMPTY}
    Then the user should see an error    Please enter a valid date
    When the applicant inserts a valid date
    Then the applicant should not see the validation error any more

Year field client side
    [Documentation]    INFUND-43
    ...
    ...    INFUND-2843
    [Tags]    HappyPath
    [Setup]    Run keywords    the user enters text to a text field    id=application_details-title    Robot test application
    ...    AND    the user enters text to a text field    id=application_details-duration    15
    When the applicant inserts an invalid date
    Then the user should see an error    Please enter a future date
    When the user enters text to a text field    id=application_details-startdate_year    ${EMPTY}
    Then the user should see an error    Please enter a future date
    When the applicant inserts a valid date
    Then the applicant should not see the validation error any more

Duration field client side
    [Documentation]    INFUND-43
    ...
    ...    INFUND-2843
    [Setup]    Run keywords    the user enters text to a text field    id=application_details-title    Robot test application
    ...    AND    the applicant inserts a valid date
    When the user enters text to a text field    id=application_details-duration    0
    Then the user should see an error    Please enter a valid duration between 1 and 36 months
    When the user enters text to a text field    id=application_details-duration    -1
    Then the user should see an error    Please enter a valid duration between 1 and 36 months
    When the user enters text to a text field    id=application_details-duration    ${EMPTY}
    Then the user should see an error    Please enter a valid value
    And the user enters text to a text field    id=application_details-duration    15
    And the applicant should not see the validation error of the duration any more

Application details server side
    [Documentation]    INFUND-2843
    [Tags]    Pending    HappyPath
    # TODO pending INFUND-3999
    Given the user should see the text in the page    Application details
    When the user enters text to a text field    id=application_details-title    ${EMPTY}
    Then the user enters text to a text field    id=application_details-startdate_day    ${EMPTY}
    And the user enters text to a text field    id=application_details-startdate_month    ${EMPTY}
    And the user enters text to a text field    id=application_details-startdate_year    ${EMPTY}
    And the user enters text to a text field    id=application_details-duration    ${EMPTY}
    When the user clicks the button/link    jQuery=button:contains("Mark as complete")
    Then The user should see an error    Please enter the full title of the project
    And the user should see an error    Please enter a future date
    And the user should see an error    Your project should last between 1 and 36 months
    And the user should see the element    css=.error-summary-list
    [Teardown]    And the user enters text to a text field    id=application_details-title    Robot test application

Empty text area
    [Documentation]    INFUND-43
    [Tags]
    Given the user clicks the button/link    css=.pagination-part-title
    When the applicant clears the text area of the "Project Summary"
    Then the user should see an error    Please enter some text
    And the user enters some text in the text area
    Then the applicant should not see the validation error any more

*** Keywords ***
the applicant should not see the validation error any more
    Focus    css=.app-submit-btn
    run keyword and ignore error    mouse out    css=input
    Run Keyword And Ignore Error    mouse out    css=.editor
    Focus    css=.app-submit-btn
    sleep    300ms
    wait until element is not visible    css=.error-message

the applicant inserts a valid date
    Clear Element Text    id=application_details-startdate_day
    Input Text    id=application_details-startdate_day    20
    Clear Element Text    id=application_details-startdate_month
    Input Text    id=application_details-startdate_month    11
    Clear Element Text    id=application_details-startdate_year
    Input Text    id=application_details-startdate_year    2020

the applicant inserts an invalid date
    Clear Element Text    id=application_details-startdate_day
    Input Text    id=application_details-startdate_day    18
    Clear Element Text    id=application_details-startdate_year
    Input Text    id=application_details-startdate_year    2015
    Clear Element Text    id=application_details-startdate_month
    Input Text    id=application_details-startdate_month    11

the applicant clears the text area of the "Project Summary"
    Clear Element Text    css=#form-input-11 .editor
    Press Key    css=#form-input-11 .editor    \\8
    Focus    css=.app-submit-btn
    Comment    Click Element    css=.fa-bold
    Sleep    300ms

Applicant goes to the application details page of the Robot application
    Given the user navigates to the page    ${DASHBOARD_URL}
    When the user clicks the button/link    link=Robot test application
    And the user clicks the button/link    link=Application details

the user enters some text in the text area
    Input Text    css=#form-input-11 .editor    Test text
    Mouse Out    css=#form-input-11 .editor

the applicant should not see the validation error of the duration any more
    Focus    css=.app-submit-btn
    run keyword and ignore error    mouse out    css=input
    Run Keyword And Ignore Error    mouse out    css=.editor
    Focus    css=.app-submit-btn
    sleep    300ms
    Wait Until Page Does Not Contain    Please enter a valid value
