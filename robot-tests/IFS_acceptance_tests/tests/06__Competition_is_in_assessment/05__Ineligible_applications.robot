*** Settings ***
Documentation     INFUND-8942 - Filter and sorting on 'Ineligible applications' dashboard
...
...               INFUND-7374 - As a member of the competitions team I can inform an applicant that their application is ineligible so that they know their application is not being sent for assessment
...
...               INFUND-7373 - As a member of the competitions team I can view a list of ineligible applications so that I know which applications have been marked as ineligible and which applicants have been informed
...
...               INFUND-9130 - Applicant dashboard: Application moved from 'Application in progress' section of dashboard to 'Previous applications' section
...
...               INFUND-7370 - As a member of the competitions team I can mark a submitted application as ineligible so that the application is not sent to be assessed
...
...               INFUND-8941 - As a member of the competitions team I can reinstate an application that as been marked as Ineligible
...
...               IFS-986 - Innovation Leads: Enable 'Mark as ineligible' for applications
...
...               IFS-1458 View unsuccessful applications after Inform state: initial navigation
...
...               IFS-1459 View unsuccessful applications after Inform state: list
...
...               IFS-1491 Inform Applicant - Ineligible page - couple of issues
...
...               IFS-3132 Email content templates for notifications
...
...               IFS-2994 New Stakeholder role and permissions
Suite Setup       The user logs-in in new browser  &{Comp_admin1_credentials}
Suite Teardown    the user closes the browser
Force Tags        CompAdmin
Resource          ../../resources/defaultResources.robot
Resource          ../02__Competition_Setup/CompAdmin_Commons.robot

*** Variables ***
${ineligibleApplication}  Living with Virtual Reality
${ineligibleApplicationOverview}  ${server}/management/competition/${IN_ASSESSMENT_COMPETITION}/application/${application_ids["${ineligibleApplication}"]}
${ineligibleApplications}  ${server}/management/competition/${IN_ASSESSMENT_COMPETITION}/applications/ineligible
${ineligibleMessage}  On checking your application we found that it did not meet these requirements.
# ${IN_ASSESSMENT_COMPETITION} is the Sustainable living models for the future
${submittedApplication}           Living with Digital Rights Management
${submittedApplicationNumber}     ${application_ids['${submittedApplication}']}
${ineligibleApplication}          Ineligible Vlogging
${ineligibleApplicationNumber}    ${application_ids['${ineligibleApplication}']}

*** Test Cases ***
A non submitted application cannot be marked as ineligible
    [Documentation]    INFUND-7370
    [Tags]
    Given the user navigates to the page           ${server}/management/competition/${IN_ASSESSMENT_COMPETITION}/applications/all
    Then the user should see the element           jQuery = td:contains("Rainfall") ~ td:contains("Started")
    When the user clicks the button/link           link = ${application_ids["Rainfall"]}
    Then the user should not see the element       jQuery = h2 button:contains("Mark application as ineligible")
    [Teardown]    the user clicks the button/link  jQuery = .govuk-back-link:contains("Back")

Ineligigle button is shown on submitted applications
    [Documentation]    INFUND-7370
    [Tags]
    Given the user should see the element  jQuery = td:contains("${ineligibleApplication}") ~ td:contains("Submitted")
    When the user clicks the button/link   link = ${application_ids["${ineligibleApplication}"]}
    Then the user should see the element   jQuery = h2 button:contains("Mark application as ineligible")

Clicking the ineligible button
    [Documentation]  INFUND-7370 IFS-986
    [Tags]  InnovationLead
    [Setup]  log in as a different user     &{innovation_lead_one}
    Given the user navigates to the page    ${ineligibleApplicationOverview}
    When the user clicks the button/link    jQuery = h2 button:contains("Mark application as ineligible")
    #There are 2 buttons with the same name so we need to be careful
    Then the user should see the element    css = [aria-hidden = "false"] [id = "ineligibleReason"]
    And browser validations have been disabled
    When the user clicks the button/link    css = button[name = "markAsIneligible"]
    Then the user should see a field and summary error  ${empty_field_warning_message}

Cancel marking the application as ineligible
    [Documentation]  INFUND-7370 IFS-986
    [Tags]  InnovationLead
    When the user clicks the button/link      jQuery = .button-clear:contains("Cancel")
    Then the user should not see the element  css = [aria-hidden = "false"] [id = "ineligibleReason"]

Client side validation - mark an application as ineligible
    [Documentation]  IFS-159
    [Tags]  InnovationLead
    Given the user clicks the button/link                   jQuery = h2 button:contains("Mark application as ineligible")
    And the user enters multiple strings into a text field  id = ineligibleReason  a${SPACE}  402
    Then the user should see a field error                  Maximum word count exceeded. Please reduce your word count to 400.
    When the user enters text to a text field               id = ineligibleReason  This is the reason of ineligibility.
    [Teardown]  the user clicks the button/link             jQuery = h2 button:contains("Mark application as ineligible")

Marking an application as ineligible moves it to the ineligible view
    [Documentation]  INFUND-7370 IFS-986
    [Tags]  InnovationLead
    Given the user clicks the button/link       jQuery = h2 button:contains("Mark application as ineligible")
    Then the user enters text to a text field   id = ineligibleReason  This is the reason of why this application is ineligible
    When the user clicks the button/link        jQuery = .govuk-button:contains("Mark application as ineligible")
    Then the user should be redirected to the correct page  ${ineligibleApplications}
    And the user should see the element         jQuery = td:contains("${ineligibleApplication}")
    And the user should not see the element     jQuery = td:contains("${ineligibleApplication}") ~ td > a:contains("Inform applicant")

Sort ineligible applications by lead
    [Documentation]  INFUND-8942  IFS-986
    [Tags]  InnovationLead
    When the application list is sorted by  Lead
    Then the applications should be sorted by column  3

Filter ineligible applications
    [Documentation]    INFUND-8942
    [Tags]
    [Setup]  log in as a different user        &{Comp_admin1_credentials}
    Given the user navigates to the page       ${ineligibleApplications}
    When the user selects the option from the drop-down menu  No  id = filterInform
    When the user clicks the button/link       jQuery = .govuk-button:contains("Filter")
    Then the user should see the element       jQuery = td:contains("${ineligibleApplication}") ~ td .govuk-button:contains("Inform applicant")
    And the user should not see the element    jQuery = td:contains("Informed ineligible application") ~ td span:contains("Informed")
    When the user clicks the button/link       jQuery = a:contains("Clear all filters")
    Then the user should see the element       jQuery = td:contains("Informed ineligible application") ~ td span:contains("Informed")

The Administrator should see the ineligible applications in unsuccessful list but he cannot reinstate it
    [Documentation]  IFS-1458 IFS-1459 IFS-50
    [Tags]  Administrator
    [Setup]  log in as a different user        &{ifs_admin_user_credentials}
    Given the user navigates to the page       ${server}/management/competition/${IN_ASSESSMENT_COMPETITION}/applications/previous
    Then the user should see the element       jQuery = td:contains("${ineligibleApplication}")
    And the user should not see the element    jQuery = td:contains("${ineligibleApplication}") ~ td a:contains("Mark as successful")

Inform a user their application is ineligible
    [Documentation]  INFUND-7374  IFS-1491  IFS-3132
    [Tags]  Applicant
    [Setup]  log in as a different user       &{internal_finance_credentials}
    Given the user navigates to the page      ${ineligibleApplications}
    And the user clicks the button/link       jQuery = td:contains("${ineligibleApplication}") ~ td > a:contains("Inform applicant")
    And the user clicks the button/link       jQuery = a:contains("Cancel")
    When the user clicks the button/link      jQuery = td:contains("${ineligibleApplication}") ~ td > a:contains("Inform applicant")
    And the user should see the element       jQuery = p:contains("${ineligibleMessage}")
    And the user clicks the button/link       jQuery = button:contains("Send")
    Then the user should see the element      jQuery = td:contains("${ineligibleApplication}") ~ td span:contains("Informed")

Applicant is informed that his application is not eligible
    [Documentation]  INFUND-7374  IFS-3132
    [Tags]  Applicant
    When the applicant can see his application in the right section    Previous applications
    Then the user reads his email  ${Ineligible_user["email"]}         Notification regarding your application  ${ineligibleMessage}

Innovation Lead is not able to reinstate an application
    [Documentation]  INFUND-8941 IFS-986
    [Tags]  InnovationLead
    Given log in as a different user          &{innovation_lead_one}
    When the user navigates to the page       ${ineligibleApplicationOverview}
    Then the user should not see the element  jQuery = a[role = "button"]:contains("Reinstate application")

Reinstate an application
    [Documentation]  INFUND-8941 IFS-986 IFS-1458 IFS-1459
    [Tags]
    Given Log in as a different user          &{Comp_admin1_credentials}
    When the user navigates to the page       ${ineligibleApplicationOverview}
    And the user clicks the button/link       jQuery = a:contains("Reinstate application")
    When the user clicks the button/link      jQuery = button:contains("Reinstate application")
    And the user navigates to the page        ${server}/management/competition/${IN_ASSESSMENT_COMPETITION}/applications/submitted
    Then the user should see the element      jQuery = td:contains("${ineligibleApplication}")
    And the reinstated application in no longer shown in the unsuccessful list
    And the applicant can see his application in the right section  Applications in progress

Stakeholders cannot mark applications as ineligible
    [Documentation]  IFS-2994
    [Tags]
    Given Log in as a different user            &{stakeholder_user}
    When the user navigates to the page         ${SERVER}/management/competition/${IN_ASSESSMENT_COMPETITION}/application/${submittedApplicationNumber}?origin = SUBMITTED_APPLICATIONS
    Then the user should see the element        jQuery = dt:contains("Competition name") ~ dd:contains("${IN_ASSESSMENT_COMPETITION_NAME}")
    And the user should not see the element     jQuery = button:contains("Mark application as ineligible")

Stakeholders cannot reinstate an application
    [Documentation]  IFS-2994
    [Tags]
    When the user navigates to the page        ${SERVER}/management/competition/${IN_ASSESSMENT_COMPETITION}/application/${ineligibleApplicationNumber}?origin = INELIGIBLE_APPLICATIONS
    Then the user should see the element       jQuery = dt:contains("Competition name") ~ dd:contains("${IN_ASSESSMENT_COMPETITION_NAME}")
    And the user should not see the element    css = a[data-js-modal = "modal-reinstate"]

*** Keywords ***
the applicant can see his application in the right section
    [Arguments]    ${section}
    Log in as a different user         &{Ineligible_user}
    the user should see the element    jQuery = h2:contains(${section}) ~ ul a:contains("Living with Virtual Reality")

the user navigates to ineligible applications
    the user clicks the button/link    link = ${IN_ASSESSMENT_COMPETITION_NAME}
    the user clicks the button/link    link = Applications: All, submitted, ineligible
    the user clicks the button/link    link = Ineligible applications

the reinstated application in no longer shown in the unsuccessful list
    the user navigates to the page       ${server}/management/competition/${IN_ASSESSMENT_COMPETITION}/applications/previous
    the user should not see the element  jQuery = td:contains("${ineligibleApplication}")

the user is required to enter a subject/message
    [Arguments]  ${fieldValidation}  ${field}  ${fieldContent}
    the user clicks the button/link         jQuery = button:contains("Send")
    the user should see a field error       ${fieldValidation}
    the user enters text to a text field    id = ${field}  ${fieldContent}