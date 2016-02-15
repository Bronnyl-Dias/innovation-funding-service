*** Settings ***
Library  SauceLabs

*** Variables ***
${REMOTE_URL}     ${EMPTY}
${DESIRED_CAPABILITIES}    ${EMPTY}
${SERVER_AUTH}    ${EMPTY}
${TEST_TAGS}    ${EMPTY}
${FF_PROFILE}    ${CURDIR}/../firefox_config
*** Keywords ***
Login as user
    [Arguments]    ${email}    ${password}
    The guest user opens the browser
    The guest user inserts user email & password    ${email}    ${password}
    The guest user clicks the log-in button

The guest user inserts user email & password
    [Arguments]    ${USERNAME}    ${PSW}
    Input Text    id=id_email    ${USERNAME}
    Input Password    id=id_password    ${PSW}

The guest user clicks the log-in button
    Click Button    css=input.button

# The guest user opens the browser
#    Start Virtual Display       1920    1080
#    Open browser  http://google.com  ${BROWSER}
#    ...  ff_profile_dir=${FF_PROFILE}
#    ...  remote_url=${REMOTE_URL}
#    ...  desired_capabilities=${DESIRED_CAPABILITIES}
#    Run keyword if  '${SERVER_AUTH}' != ''    Go to    ${PROTOCOL}${SERVER_AUTH}@${SERVER_BASE}
#    Run keyword if  '${SERVER_AUTH}' == ''    Go to    ${PROTOCOL}${SERVER_BASE}

The guest user opens the browser
    # Start Virtual Display       1920        1080
    Run keyword if  '${SERVER_AUTH}' != ''        Open browser   ${PROTOCOL}${SERVER_AUTH}@${SERVER_BASE}    ${BROWSER}
                                                  ...  ff_profile_dir=${FF_PROFILE}
                                                  ...  remote_url=${REMOTE_URL}
                                                  ...  desired_capabilities=${DESIRED_CAPABILITIES}

    Run keyword if  '${SERVER_AUTH}' == ''        Open browser   ${PROTOCOL}${SERVER_BASE}                   ${BROWSER}
                                                  ...  ff_profile_dir=${FF_PROFILE}
                                                  ...  remote_url=${REMOTE_URL}
                                                  ...  desired_capabilities=${DESIRED_CAPABILITIES}

    run keyword and ignore error         Log into Shib

Log into Shib
    Input Text             name=j_username         steve.smith@empire.com
    Input Text             name=j_password         test
    Submit Form
    Wait Until Element Is Visible       id=id_email

TestTeardown User closes the browser
    Run keyword if  '${REMOTE_URL}' != ''    Report Sauce status  'IFS | ${PREV_TEST_NAME}'    ${PREV_TEST_STATUS}    ${TEST_TAGS}    ${REMOTE_URL}
    Close all browsers

User closes the browser
    Run keyword if  '${REMOTE_URL}' != ''    Report Sauce status  'IFS | ${SUITE_NAME}'    ${SUITE_STATUS}    ${SUITE_MESSAGE}    ${REMOTE_URL}
    Close all browsers

Logout as user
    Click Element    link=Logout
