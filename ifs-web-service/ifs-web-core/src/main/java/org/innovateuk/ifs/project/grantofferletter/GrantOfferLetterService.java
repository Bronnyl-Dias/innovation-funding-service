package org.innovateuk.ifs.project.grantofferletter;

import org.innovateuk.ifs.commons.ZeroDowntime;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.file.resource.FileEntryResource;
import org.innovateuk.ifs.project.grantofferletter.resource.GrantOfferLetterState;
import org.innovateuk.ifs.project.grantofferletter.resource.GrantOfferLetterStateResource;
import org.innovateuk.ifs.project.resource.ApprovalType;
import org.springframework.core.io.ByteArrayResource;

import java.util.Optional;

/**
 * A service for dealing with a project's grant offer functionality
 */
public interface GrantOfferLetterService {

    Optional<ByteArrayResource> getSignedGrantOfferLetterFile(Long projectId);

    Optional<FileEntryResource> getSignedGrantOfferLetterFileDetails(Long projectId);

    Optional<ByteArrayResource> getGrantOfferFile(Long projectId);

    Optional<FileEntryResource> getGrantOfferFileDetails(Long projectId);

    ServiceResult<FileEntryResource> addSignedGrantOfferLetter(Long projectId, String contentType, long fileSize, String originalFilename, byte[] bytes);

    ServiceResult<FileEntryResource> addGrantOfferLetter(Long projectId, String contentType, long fileSize, String originalFilename, byte[] bytes);

    ServiceResult<Void> removeGrantOfferLetter(Long projectId);

    ServiceResult<Void> removeSignedGrantOfferLetter(Long projectId);

    ServiceResult<Void> submitGrantOfferLetter(Long projectId);

    @ZeroDowntime(reference = "IFS-2579", description = "Remove in Sprint 19 - replaced with usage of getGrantOfferLetterState()")
    ServiceResult<Boolean> isSendGrantOfferLetterAllowed(Long projectId);

    ServiceResult<Void> sendGrantOfferLetter(Long projectId);

    @ZeroDowntime(reference = "IFS-2579", description = "Remove in Sprint 19 - replaced with usage of getGrantOfferLetterState()")
    ServiceResult<Boolean> isGrantOfferLetterAlreadySent(Long projectId);

    ServiceResult<Void> approveOrRejectSignedGrantOfferLetter(Long projectId, ApprovalType approvalType);

    @ZeroDowntime(reference = "IFS-2579", description = "Remove in Sprint 19 - replaced with usage of getGrantOfferLetterState()")
    ServiceResult<Boolean> isSignedGrantOfferLetterApproved(Long projectId);

    @ZeroDowntime(reference = "IFS-2579", description = "Remove in Sprint 19 - replaced with usage of getGrantOfferLetterState()")
    ServiceResult<Boolean> isSignedGrantOfferLetterRejected(Long projectId);

    @ZeroDowntime(reference = "IFS-2579", description = "Remove in Sprint 19 - replaced with usage of getGrantOfferLetterState()")
    ServiceResult<GrantOfferLetterState> getGrantOfferLetterWorkflowState(Long projectId);

    ServiceResult<GrantOfferLetterStateResource> getGrantOfferLetterState(Long projectId);

    Optional<ByteArrayResource> getAdditionalContractFile(Long projectId);

    Optional<FileEntryResource> getAdditionalContractFileDetails(Long projectId);

    ServiceResult<FileEntryResource> addAdditionalContractFile(Long projectId, String contentType, long fileSize, String originalFilename, byte[] bytes);

}
