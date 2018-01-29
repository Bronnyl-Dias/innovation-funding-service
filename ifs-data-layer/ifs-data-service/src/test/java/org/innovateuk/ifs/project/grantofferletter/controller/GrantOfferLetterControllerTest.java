package org.innovateuk.ifs.project.grantofferletter.controller;

import org.innovateuk.ifs.BaseControllerMockMVCTest;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.file.resource.FileEntryResource;
import org.innovateuk.ifs.file.service.FileAndContents;
import org.innovateuk.ifs.file.service.FilesizeAndTypeFileValidator;
import org.innovateuk.ifs.project.grantofferletter.resource.GrantOfferLetterState;
import org.innovateuk.ifs.project.grantofferletter.transactional.GrantOfferLetterService;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.util.JsonMappingUtil.toJson;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class GrantOfferLetterControllerTest extends BaseControllerMockMVCTest<GrantOfferLetterController> {

    private static final long projectId = 123L;
    private static final long maxFilesize = 1234L;
    private static final List<String> mediaTypes = singletonList("application/pdf");

    @Mock(name = "fileValidator")
    private FilesizeAndTypeFileValidator<List<String>> fileValidatorMock;

    @Override
    protected GrantOfferLetterController supplyControllerUnderTest() {
        GrantOfferLetterController controller = new GrantOfferLetterController();
        ReflectionTestUtils.setField(controller, "maxFilesizeBytesForProjectSetupGrantOfferLetter", maxFilesize);
        ReflectionTestUtils.setField(controller, "validMediaTypesForProjectSetupGrantOfferLetter", mediaTypes);
        return controller;
    }

    @Test
    public void getGrantOfferLetterFileContents() throws Exception {

        Function<GrantOfferLetterService, ServiceResult<FileAndContents>> serviceCallToUpload =
                (service) -> service.getGrantOfferLetterFileAndContents(projectId);

        assertGetFileContents("/project/{projectId}/grant-offer", new Object[] {projectId},
                emptyMap(), grantOfferLetterServiceMock, serviceCallToUpload).
                andDo(documentFileGetContentsMethod("project/{method-name}"));
    }

    @Test
    public void getAdditionalContractFileContents() throws Exception {

        Function<GrantOfferLetterService, ServiceResult<FileAndContents>> serviceCallToUpload =
                (service) -> service.getAdditionalContractFileAndContents(projectId);

        assertGetFileContents("/project/{projectId}/additional-contract", new Object[] {projectId},
                emptyMap(), grantOfferLetterServiceMock, serviceCallToUpload).
                andDo(documentFileGetContentsMethod("project/{method-name}"));
    }

    @Test
    public void getGrantOfferLetterFileEntryDetails() throws Exception {

        Function<GrantOfferLetterService, ServiceResult<FileEntryResource>> serviceCallToUpload =
                (service) -> service.getSignedGrantOfferLetterFileEntryDetails(projectId);

        assertGetFileDetails("/project/{projectId}/signed-grant-offer/details", new Object[] {projectId},
                emptyMap(),
                grantOfferLetterServiceMock, serviceCallToUpload).
                andDo(documentFileGetDetailsMethod("project/{method-name}"));
    }

    @Test
    public void getAdditionalContractFileEntryDetails() throws Exception {

        Function<GrantOfferLetterService, ServiceResult<FileEntryResource>> serviceCallToUpload =
                (service) -> service.getAdditionalContractFileEntryDetails(projectId);

        assertGetFileDetails("/project/{projectId}/additional-contract/details", new Object[] {projectId},
                emptyMap(),
                grantOfferLetterServiceMock, serviceCallToUpload).
                andDo(documentFileGetDetailsMethod("project/{method-name}"));
    }

    @Test
    public void addGrantOfferLetter() throws Exception {

        BiFunction<GrantOfferLetterService, FileEntryResource, ServiceResult<FileEntryResource>> serviceCallToUpload =
                (service, fileToUpload) -> service.createSignedGrantOfferLetterFileEntry(eq(projectId), eq(fileToUpload), fileUploadInputStreamExpectations());

        assertFileUploadProcess("/project/" + projectId + "/signed-grant-offer/", fileValidatorMock, mediaTypes, grantOfferLetterServiceMock, serviceCallToUpload).
                andDo(documentFileUploadMethod("project/{method-name}"));
    }

    @Test
    public void addAdditionalContractFile() throws Exception {

        BiFunction<GrantOfferLetterService, FileEntryResource, ServiceResult<FileEntryResource>> serviceCallToUpload =
                (service, fileToUpload) -> service.createAdditionalContractFileEntry(eq(projectId), eq(fileToUpload), fileUploadInputStreamExpectations());

        assertFileUploadProcess("/project/" + projectId + "/additional-contract/", fileValidatorMock, mediaTypes, grantOfferLetterServiceMock, serviceCallToUpload).
                andDo(documentFileUploadMethod("project/{method-name}"));

    }

    @Test
    public void removeGrantOfferLetterFile() throws Exception {

        when(grantOfferLetterServiceMock.removeGrantOfferLetterFileEntry(projectId)).thenReturn(serviceSuccess());

        mockMvc.perform(delete("/project/{projectId}/grant-offer", projectId)).
                andExpect(status().isNoContent()).
                andDo(document("project/{method-name}"));
    }

    @Test
    public void removeSignedGrantOfferLetterFile() throws Exception {

        when(grantOfferLetterServiceMock.removeSignedGrantOfferLetterFileEntry(projectId)).thenReturn(serviceSuccess());

        mockMvc.perform(delete("/project/{projectId}/signed-grant-offer-letter", projectId)).
                andExpect(status().isNoContent()).
                andDo(document("project/{method-name}"));
    }

    @Test
    public void isSignedGrantOfferLetterRejected() throws Exception {

        when(grantOfferLetterServiceMock.isSignedGrantOfferLetterRejected(projectId)).thenReturn(serviceSuccess(true));

        mockMvc.perform(get("/project/{projectId}/signed-grant-offer-letter/is-rejected", 123L))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andReturn();

        verify(grantOfferLetterServiceMock).isSignedGrantOfferLetterRejected(projectId);
    }

    @Test
    public void getGrantOfferLetterWorkflowState() throws Exception {

        when(grantOfferLetterServiceMock.getGrantOfferLetterState(projectId)).thenReturn(serviceSuccess(GrantOfferLetterState.APPROVED));

        mockMvc.perform(get("/project/{projectId}/grant-offer-letter/state", 123L))
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(GrantOfferLetterState.APPROVED)))
                .andDo(document("project/{method-name}"))
                .andReturn();

        verify(grantOfferLetterServiceMock).getGrantOfferLetterState(projectId);
    }
}
