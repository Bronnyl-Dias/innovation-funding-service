*** Variables ***
&{lead_applicant_credentials}             email=steve.smith@empire.com    password=Passw0rd
&{collaborator1_credentials}              email=jessica.doe@ludlow.co.uk    password=Passw0rd
&{collaborator2_credentials}              email=pete.tom@egg.com    password=Passw0rd
&{worth_test_credentials}                 email=${test_mailbox_one}+submit@gmail.com    password=Passw0rd
&{Comp_admin1_credentials}                email=john.doe@innovateuk.test    password=Passw0rd
&{successful_applicant_credentials}       email=${test_mailbox_one}+fundsuccess@gmail.com    password=Passw0rd
&{successful_released_credentials}        email=${test_mailbox_two}+releasefeedback@gmail.com    password=Passw0rd
&{unsuccessful_applicant_credentials}     email=${test_mailbox_two}+fundfailure@gmail.com    password=Passw0rd
&{unsuccessful_released_credentials}      email=james.lewis@example.com    password=Passw0rd
&{assessor_credentials}                   email=paul.plum@gmail.com    password=Passw0rd
&{assessor2_credentials}                  email=felix.wilson@gmail.com    password=Passw0rd
&{existing_assessor1_credentials}         email=${test_mailbox_one}+jeremy.alufson@gmail.com    password=Passw0rd
&{nonregistered_assessor2_credentials}    email=${test_mailbox_one}+david.peters@gmail.com    password=Passw0rd
&{nonregistered_assessor3_credentials}    email=${test_mailbox_one}+thomas.fister@gmail.com    password=Passw0rd123
&{internal_finance_credentials}           email=lee.bowman@innovateuk.test    password=Passw0rd
&{innovation_lead_one}                    email=ian.cooper@innovateuk.test    password=Passw0rd
&{innovation_lead_two}                    email=peter.freeman@innovateuk.test    password=Passw0rd
&{Multiple_user_credentials}              email=jo.peters@ntag.example.com    password=Passw0rd
&{Ineligible_user}                        email=nancy.peterson@gmail.com    password=Passw0rd
&{support_user_credentials}               email=support@innovateuk.test    password=Passw0rd
&{ifs_admin_user_credentials}             email=arden.pimenta@innovateuk.test    password=Passw0rd
&{Assessor_e2e}                           email=${test_mailbox_one}+AJE2E@gmail.com    password=Passw0rd123
&{lead_applicant_alternative_user_credentials}    email=${test_mailbox_one}+mario@gmail.com    password=Passw0rd
&{collaborator1_alternative_user_credentials}     email=kevin.summers@ludlow.co.uk    password=Passw0rd
&{collaborator2_alternative_user_credentials}     email=casey.evans@egg.com    password=Passw0rd
&{RTO_lead_applicant_credentials}                 email=dave.adams@gmail.com   password=Passw0rd
&{Research_lead_applicant_credentials}            email=heather.ross@example.com   password=Passw0rd
&{PublicSector_lead_applicant_credentials}        email=becky.mason@gmail.com   password=Passw0rd

${Comp_admin1_credentials}                        john.doe@innovateuk.test
${innovation_lead_one}                            ian.cooper@innovateuk.test