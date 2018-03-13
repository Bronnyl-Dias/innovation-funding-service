package org.innovateuk.ifs.application.transactional;

import org.innovateuk.ifs.application.domain.FormInputResponse;
import org.innovateuk.ifs.application.resource.FormInputResponseCommand;
import org.innovateuk.ifs.application.resource.FormInputResponseResource;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Transactional service for application form input responses.
 */
public interface FormInputResponseService {

    @PostFilter("hasPermission(filterObject, 'READ')")
    ServiceResult<List<FormInputResponseResource>> findResponsesByApplication(Long applicationId);

    @PostFilter("hasPermission(filterObject, 'READ')")
    ServiceResult<List<FormInputResponseResource>> findResponsesByFormInputIdAndApplicationId(Long formInputId, Long applicationId);

    @PostAuthorize("hasPermission(returnObject, 'READ')")
    ServiceResult<FormInputResponseResource> findResponseByApplicationIdAndQuestionName(long applicationId, String questionName);

    @PostFilter("hasPermission(filterObject, 'READ')")
    ServiceResult<List<FormInputResponseResource>> findResponseByApplicationIdAndQuestionId(long applicationId, long questionId);

    // TODO we need to have separate methods for save and update
    @PreAuthorize("hasPermission(#formInputResponseCommand, 'SAVE')")
    ServiceResult<FormInputResponse> saveQuestionResponse(@P("formInputResponseCommand") FormInputResponseCommand formInputResponseCommand);

}
