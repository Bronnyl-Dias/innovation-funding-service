package com.worth.ifs.project.documentation;

import com.worth.ifs.BaseControllerMockMVCTest;
import com.worth.ifs.commons.service.ServiceResult;
import com.worth.ifs.file.resource.FileEntryResource;
import com.worth.ifs.file.service.FileAndContents;
import com.worth.ifs.project.controller.ProjectGrantOfferController;
import com.worth.ifs.project.transactional.ProjectGrantOfferService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;

import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static org.mockito.Matchers.eq;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

/**
 * Api documentation for grant offer letter using ASCII docs
 **/
public class ProjectGrantOfferControllerDocumentation extends BaseControllerMockMVCTest<ProjectGrantOfferController> {

    private RestDocumentationResultHandler document;

    @Before
    public void setup() {
        this.document = document("project/{method-name}",
                preprocessResponse(prettyPrint()));
    }

    @Override
    protected ProjectGrantOfferController supplyControllerUnderTest() {
        return new ProjectGrantOfferController();
    }

    @Test
    public void addGrantOfferLetter() throws Exception {

        Long projectId = 123L;

        BiFunction<ProjectGrantOfferService, FileEntryResource, ServiceResult<FileEntryResource>> serviceCallToUpload =
                (service, fileToUpload) -> service.createSignedGrantOfferLetterFileEntry(eq(projectId), eq(fileToUpload), fileUploadInputStreamExpectations());

        assertFileUploadProcess("/project/" + projectId + "/signed-grant-offer", projectGrantOfferServiceMock, serviceCallToUpload).
                andDo(documentFileUploadMethod(document));
    }

    @Test
    public void addGeneratedGrantOfferLetter() throws Exception {

        Long projectId = 111L;

        BiFunction<ProjectGrantOfferService, FileEntryResource, ServiceResult<FileEntryResource>> serviceCallToUpload =
                (service, fileToUpload) -> service.createGrantOfferLetterFileEntry(eq(projectId), eq(fileToUpload), fileUploadInputStreamExpectations());

        assertFileUploadProcess("/project/" + projectId + "/grant-offer", projectGrantOfferServiceMock, serviceCallToUpload).
                andDo(documentFileUploadMethod(document));
    }

    @Test
    public void addAdditionalContractFile() throws Exception {

        Long projectId = 123L;

        BiFunction<ProjectGrantOfferService, FileEntryResource, ServiceResult<FileEntryResource>> serviceCallToUpload =
                (service, fileToUpload) -> service.createAdditionalContractFileEntry(eq(projectId), eq(fileToUpload), fileUploadInputStreamExpectations());

        assertFileUploadProcess("/project/" + projectId + "/additional-contract", projectGrantOfferServiceMock, serviceCallToUpload).
                andDo(documentFileUploadMethod(document));
    }

    @Test
    public void getGrantOfferLetterFileEntryDetails() throws Exception {

        Long projectId = 123L;

        BiFunction<ProjectGrantOfferService, FileEntryResource, ServiceResult<FileEntryResource>> serviceCallToUpload =
                (service, fileToUpload) -> service.getSignedGrantOfferLetterFileEntryDetails(projectId);

        assertGetFileDetails("/project/{projectId}/signed-grant-offer/details", new Object[]{projectId}, emptyMap(),
                projectGrantOfferServiceMock, serviceCallToUpload).
                andDo(documentFileGetDetailsMethod(document));
    }

    @Test
    public void getGeneratedGrantOfferLetterFileEntryDetails() throws Exception {

        Long projectId = 123L;

        BiFunction<ProjectGrantOfferService, FileEntryResource, ServiceResult<FileEntryResource>> serviceCallToUpload =
                (service, fileToUpload) -> service.getGrantOfferLetterFileEntryDetails(projectId);

        assertGetFileDetails("/project/{projectId}/grant-offer/details", new Object[]{projectId}, emptyMap(),
                projectGrantOfferServiceMock, serviceCallToUpload).
                andDo(documentFileGetDetailsMethod(document));
    }


    @Test
    public void getAdditionalContractFileEntryDetails() throws Exception {

        Long projectId = 123L;

        BiFunction<ProjectGrantOfferService, FileEntryResource, ServiceResult<FileEntryResource>> serviceCallToUpload =
                (service, fileToUpload) -> service.getAdditionalContractFileEntryDetails(projectId);

        assertGetFileDetails("/project/{projectId}/additional-contract/details", new Object[]{projectId}, emptyMap(),
                projectGrantOfferServiceMock, serviceCallToUpload).
                andDo(documentFileGetDetailsMethod(document));
    }

    @Test
    public void getAdditionalContractFileContents() throws Exception {

        Long projectId = 123L;

        Function<ProjectGrantOfferService, ServiceResult<FileAndContents>> serviceCallToUpload =
                (service) -> service.getAdditionalContractFileAndContents(projectId);

        assertGetFileContents("/project/{projectId}/additional-contract", new Object[]{projectId},
                emptyMap(), projectGrantOfferServiceMock, serviceCallToUpload).
                andDo(documentFileGetContentsMethod(document));
    }

    @Test
    public void getGrantOfferLetterFileContents() throws Exception {

        Long projectId = 123L;

        Function<ProjectGrantOfferService, ServiceResult<FileAndContents>> serviceCallToUpload =
                (service) -> service.getSignedGrantOfferLetterFileAndContents(projectId);


        assertGetFileContents("/project/{projectId}/signed-grant-offer", new Object[]{projectId},
                emptyMap(), projectGrantOfferServiceMock, serviceCallToUpload).
                andDo(documentFileGetContentsMethod(document));
    }

    @Test
    public void getGeneratedGrantOfferLetterFileContents() throws Exception {

        Long projectId = 123L;

        Function<ProjectGrantOfferService, ServiceResult<FileAndContents>> serviceCallToUpload =
                (service) -> service.getGrantOfferLetterFileAndContents(projectId);


        assertGetFileContents("/project/{projectId}/grant-offer", new Object[]{projectId},
                emptyMap(), projectGrantOfferServiceMock, serviceCallToUpload).
                andDo(documentFileGetContentsMethod(document));
    }

}
