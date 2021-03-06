package org.innovateuk.ifs.otherdocuments;

import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.file.resource.FileEntryResource;
import org.innovateuk.ifs.project.otherdocuments.service.OtherDocumentsRestService;
import org.innovateuk.ifs.project.service.ProjectRestService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;

import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.innovateuk.ifs.commons.rest.RestResult.restSuccess;
import static org.innovateuk.ifs.file.builder.FileEntryResourceBuilder.newFileEntryResource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OtherDocumentsServiceImplTest {

    @InjectMocks
    private OtherDocumentsServiceImpl otherDocumentsService;

    @Mock
    private ProjectRestService projectRestService;

    @Mock
    private OtherDocumentsRestService otherDocumentsRestService;

    @Test
    public void testAddCollaborationAgreement() {

        FileEntryResource createdFile = newFileEntryResource().build();

        when(otherDocumentsRestService.addCollaborationAgreementDocument(123L, "text/plain", 1000, "filename.txt", "My content!".getBytes())).
                thenReturn(restSuccess(createdFile));

        ServiceResult<FileEntryResource> result =
                otherDocumentsService.addCollaborationAgreementDocument(123L, "text/plain", 1000, "filename.txt", "My content!".getBytes());

        assertTrue(result.isSuccess());
        assertEquals(createdFile, result.getSuccess());
    }

    @Test
    public void testGetCollaborationAgreementFile() {

        Optional<ByteArrayResource> content = Optional.of(new ByteArrayResource("My content!".getBytes()));
        when(otherDocumentsRestService.getCollaborationAgreementFile(123L)).thenReturn(restSuccess(content));

        Optional<ByteArrayResource> result = otherDocumentsService.getCollaborationAgreementFile(123L);
        assertEquals(content, result);
    }

    @Test
    public void testGetCollaborationAgreementFileDetails() {

        FileEntryResource returnedFile = newFileEntryResource().build();

        Optional<FileEntryResource> response = Optional.of(returnedFile);
        when(otherDocumentsRestService.getCollaborationAgreementFileDetails(123L)).thenReturn(restSuccess(response));

        Optional<FileEntryResource> result = otherDocumentsService.getCollaborationAgreementFileDetails(123L);
        assertEquals(response, result);
    }

    @Test
    public void testRemoveCollaborationAgreement() {

        when(otherDocumentsRestService.removeCollaborationAgreementDocument(123L)).thenReturn(restSuccess());

        ServiceResult<Void> result = otherDocumentsService.removeCollaborationAgreementDocument(123L);

        assertTrue(result.isSuccess());

        verify(otherDocumentsRestService).removeCollaborationAgreementDocument(123L);
    }

    @Test
    public void testAddExploitationPlan() {

        FileEntryResource createdFile = newFileEntryResource().build();

        when(otherDocumentsRestService.addExploitationPlanDocument(123L, "text/plain", 1000, "filename.txt", "My content!".getBytes())).
                thenReturn(restSuccess(createdFile));

        ServiceResult<FileEntryResource> result =
                otherDocumentsService.addExploitationPlanDocument(123L, "text/plain", 1000, "filename.txt", "My content!".getBytes());

        assertTrue(result.isSuccess());
        assertEquals(createdFile, result.getSuccess());
    }

    @Test
    public void testGetCExploitationPlanFile() {

        Optional<ByteArrayResource> content = Optional.of(new ByteArrayResource("My content!".getBytes()));
        when(otherDocumentsRestService.getExploitationPlanFile(123L)).thenReturn(restSuccess(content));

        Optional<ByteArrayResource> result = otherDocumentsService.getExploitationPlanFile(123L);
        assertEquals(content, result);
    }

    @Test
    public void testGetExploitationPlanFileDetails() {

        FileEntryResource returnedFile = newFileEntryResource().build();

        Optional<FileEntryResource> response = Optional.of(returnedFile);
        when(otherDocumentsRestService.getExploitationPlanFileDetails(123L)).thenReturn(restSuccess(response));

        Optional<FileEntryResource> result = otherDocumentsService.getExploitationPlanFileDetails(123L);
        assertEquals(response, result);
    }

    @Test
    public void testRemoveExploitationPlan() {

        when(otherDocumentsRestService.removeExploitationPlanDocument(123L)).thenReturn(restSuccess());

        ServiceResult<Void> result = otherDocumentsService.removeExploitationPlanDocument(123L);

        assertTrue(result.isSuccess());

        verify(otherDocumentsRestService).removeExploitationPlanDocument(123L);
    }

    @Test
    public void testAcceptOrRejectOtherDocuments() {

        when(otherDocumentsRestService.acceptOrRejectOtherDocuments(123L, true)).thenReturn(restSuccess());

        ServiceResult<Void> result = otherDocumentsService.acceptOrRejectOtherDocuments(123L, true);

        assertTrue(result.isSuccess());

        verify(otherDocumentsRestService).acceptOrRejectOtherDocuments(123L, true);
    }

    @Test
    public void testOtherDocumentsSubmitAllowedWhenAllFilesUploaded() throws Exception {

        when(otherDocumentsRestService.isOtherDocumentsSubmitAllowed(123L)).thenReturn(restSuccess(true));

        Boolean submitAllowed = otherDocumentsService.isOtherDocumentSubmitAllowed(123L);

        assertTrue(submitAllowed);

        verify(otherDocumentsRestService).isOtherDocumentsSubmitAllowed(123L);
    }

    @Test
    public void testOtherDocumentsSubmitAllowedWhenNotAllFilesUploaded() throws Exception {

        when(otherDocumentsRestService.isOtherDocumentsSubmitAllowed(123L)).thenReturn(restSuccess(false));

        Boolean submitAllowed = otherDocumentsService.isOtherDocumentSubmitAllowed(123L);

        assertFalse(submitAllowed);

        verify(otherDocumentsRestService).isOtherDocumentsSubmitAllowed(123L);
    }
    @Test
    public void testSetPartnerDocumentsAsSubmitted()  throws Exception {

        when(otherDocumentsRestService.setPartnerDocumentsSubmitted(1L)).thenReturn(restSuccess());

        ServiceResult<Void> submitted = otherDocumentsService.setPartnerDocumentsSubmitted(1L);

        assertTrue(submitted.isSuccess());

        verify(otherDocumentsRestService).setPartnerDocumentsSubmitted(1L);
    }
}