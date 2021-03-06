package org.innovateuk.ifs.project.bankdetails.documentation;

import org.innovateuk.ifs.BaseControllerMockMVCTest;
import org.innovateuk.ifs.commons.error.Error;
import org.innovateuk.ifs.commons.rest.RestErrorResponse;
import org.innovateuk.ifs.organisation.resource.OrganisationAddressResource;
import org.innovateuk.ifs.project.bankdetails.controller.ProjectBankDetailsController;
import org.innovateuk.ifs.project.bankdetails.resource.BankDetailsResource;
import org.innovateuk.ifs.project.bankdetails.resource.BankDetailsStatusResource;
import org.innovateuk.ifs.project.bankdetails.resource.ProjectBankDetailsStatusSummary;
import org.innovateuk.ifs.project.bankdetails.transactional.BankDetailsService;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static java.util.Arrays.asList;
import static org.innovateuk.ifs.commons.error.Error.fieldError;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.documentation.BankDetailsDocs.bankDetailsResourceFields;
import static org.innovateuk.ifs.documentation.BankDetailsDocs.projectBankDetailsStatusSummaryFields;
import static org.innovateuk.ifs.organisation.builder.OrganisationAddressResourceBuilder.newOrganisationAddressResource;
import static org.innovateuk.ifs.project.bankdetails.builder.BankDetailsResourceBuilder.newBankDetailsResource;
import static org.innovateuk.ifs.project.bankdetails.builder.BankDetailsStatusResourceBuilder.newBankDetailsStatusResource;
import static org.innovateuk.ifs.project.bankdetails.builder.ProjectBankDetailsStatusSummaryBuilder.newProjectBankDetailsStatusSummary;
import static org.innovateuk.ifs.project.constant.ProjectActivityStates.COMPLETE;
import static org.innovateuk.ifs.project.constant.ProjectActivityStates.PENDING;
import static org.innovateuk.ifs.util.JsonMappingUtil.toJson;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ProjectBankDetailsControllerDocumentation extends BaseControllerMockMVCTest<ProjectBankDetailsController> {

    @Mock
    private BankDetailsService bankDetailsServiceMock;

    @Override
    protected ProjectBankDetailsController supplyControllerUnderTest() {
        return new ProjectBankDetailsController();
    }

    @Test
    public void submitBankDetails() throws Exception {
        Long projectId = 1L;
        Long organisationId = 1L;
        OrganisationAddressResource organisationAddressResource = newOrganisationAddressResource().build();
        BankDetailsResource bankDetailsResource = newBankDetailsResource()
                .withProject(projectId).withSortCode("123456")
                .withAccountNumber("12345678")
                .withOrganisation(organisationId)
                .withOrganiationAddress(organisationAddressResource)
                .withCompanyName("Company name")
                .build();

        when(bankDetailsServiceMock.submitBankDetails(bankDetailsResource)).thenReturn(serviceSuccess());

        mockMvc.perform(
                put("/project/{projectId}/bank-details", projectId)
                        .contentType(APPLICATION_JSON)
                        .content(toJson(bankDetailsResource))
                        .header("IFS_AUTH_TOKEN", "123abc"))
                .andExpect(status().isOk())
                .andDo(document("project/{method-name}",
                        pathParameters(
                                parameterWithName("projectId").description("Id of the project to be updated with bank details")
                        ),
                        requestFields(bankDetailsResourceFields)
                ))
                .andReturn();
    }

    @Test
    public void submitInvalidBankDetails() throws Exception {
        Long projectId = 1L;
        Long organisationId = 1L;

        OrganisationAddressResource organisationAddressResource = newOrganisationAddressResource().build();
        BankDetailsResource bankDetailsResource = newBankDetailsResource()
                .withProject(projectId).withSortCode("123")
                .withAccountNumber("1234567")
                .withOrganisation(organisationId)
                .withOrganiationAddress(organisationAddressResource)
                .withCompanyName("Company name")
                .build();

        Error invalidSortCodeError = fieldError("sortCode", "123", "validation.standard.sortcode.format", "", "", "\\d{6}");
        Error invalidAccountNumberError = fieldError("accountNumber", "1234567", "validation.standard.accountnumber.format", "", "", "\\d{8}");

        RestErrorResponse expectedErrors = new RestErrorResponse(asList(invalidSortCodeError, invalidAccountNumberError));

        when(bankDetailsServiceMock.submitBankDetails(bankDetailsResource)).thenReturn(serviceSuccess());
        mockMvc.perform(
                post("/project/{projectId}/bank-details", projectId)
                        .header("IFS_AUTH_TOKEN", "123abc")
                        .contentType(APPLICATION_JSON)
                        .content(toJson(bankDetailsResource)))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().json(toJson(expectedErrors)))
                .andDo(document("project/{method-name}",
                        pathParameters(
                                parameterWithName("projectId").description("Id of the project to be updated with bank details")
                        ),
                        requestFields(bankDetailsResourceFields)
                ))
                .andReturn();
    }

    @Test
    public void getBankDetailsProjectSummary() throws Exception {
        Long projectId = 123L;
        List<BankDetailsStatusResource> bankDetailsStatusResources = newBankDetailsStatusResource().withOrganisationId(1L, 2L, 3L).withOrganisationName("ABC Ltd.", "XYZ Ltd.", "University of Sheffield").withBankDetailsStatus(PENDING, COMPLETE, COMPLETE).build(3);
        final ProjectBankDetailsStatusSummary bankDetailsStatusSummary = newProjectBankDetailsStatusSummary().withCompetitionId(1L).withCompetitionName("Galaxy Note 7 disaster case study").withProjectId(2L).withBankDetailsStatusResources(bankDetailsStatusResources).build();
        when(bankDetailsServiceMock.getProjectBankDetailsStatusSummary(projectId)).thenReturn(serviceSuccess(bankDetailsStatusSummary));
        mockMvc.perform(get("/project/{projectId}/bank-details/status-summary", projectId)
                .header("IFS_AUTH_TOKEN", "123abc"))
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(bankDetailsStatusSummary)))
                .andDo(document("project/{method-name}",
                        pathParameters(
                                parameterWithName("projectId").description("Id of project that bank details status summary is requested for")
                        ),
                        responseFields(projectBankDetailsStatusSummaryFields)
                ));
    }
}
