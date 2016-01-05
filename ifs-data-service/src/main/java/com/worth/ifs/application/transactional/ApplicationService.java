package com.worth.ifs.application.transactional;

import com.worth.ifs.application.domain.Application;
import com.worth.ifs.application.resource.FormInputResponseFileEntryId;
import com.worth.ifs.application.resource.FormInputResponseFileEntryResource;
import com.worth.ifs.form.domain.FormInputResponse;
import com.worth.ifs.transactional.ServiceFailure;
import com.worth.ifs.util.Either;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.File;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Transactional and secure service for Application processing work
 */
public interface ApplicationService {

    @PreAuthorize("hasAuthority('applicant')")
    Application createApplicationByApplicationNameForUserIdAndCompetitionId(String applicationName, final Long competitionId, final Long userId);

    @PreAuthorize("hasPermission(#fileEntry, 'UPDATE')")
    Either<ServiceFailure, Pair<File, FormInputResponseFileEntryResource>> createFormInputResponseFileUpload(@P("fileEntry") FormInputResponseFileEntryResource fileEntry, Supplier<InputStream> inputStreamSupplier);

    @PreAuthorize("hasPermission(#fileEntry, 'UPDATE')")
    Either<ServiceFailure, Pair<File, FormInputResponseFileEntryResource>> updateFormInputResponseFileUpload(@P("fileEntry") FormInputResponseFileEntryResource fileEntry, Supplier<InputStream> inputStreamSupplier);

    @PreAuthorize("hasPermission(#fileEntry, 'com.worth.ifs.application.resource.FormInputResponseFileEntryResource', 'UPDATE')")
    Either<ServiceFailure, FormInputResponse> deleteFormInputResponseFileUpload(@P("fileEntry") FormInputResponseFileEntryId fileEntryId);

    @PreAuthorize("hasPermission(#fileEntry, 'com.worth.ifs.application.resource.FormInputResponseFileEntryResource', 'READ')")
    Either<ServiceFailure, Pair<FormInputResponseFileEntryResource, Supplier<InputStream>>> getFormInputResponseFileUpload(@P("fileEntry") FormInputResponseFileEntryId fileEntryId);
}
