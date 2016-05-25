package com.worth.ifs;

import com.worth.ifs.application.AbstractApplicationController;
import com.worth.ifs.application.form.ApplicationForm;
import com.worth.ifs.application.resource.AppendixResource;
import com.worth.ifs.application.resource.ApplicationResource;
import com.worth.ifs.application.resource.SectionResource;
import com.worth.ifs.application.resource.SectionType;
import com.worth.ifs.application.service.ApplicationSummaryRestService;
import com.worth.ifs.application.service.CompetitionService;
import com.worth.ifs.commons.security.UserAuthenticationService;
import com.worth.ifs.competition.resource.CompetitionResource;
import com.worth.ifs.file.resource.FileEntryResource;
import com.worth.ifs.file.service.FileEntryRestService;
import com.worth.ifs.form.resource.FormInputResource;
import com.worth.ifs.form.resource.FormInputResponseResource;
import com.worth.ifs.form.service.FormInputResponseService;
import com.worth.ifs.form.service.FormInputRestService;
import com.worth.ifs.user.resource.ProcessRoleResource;
import com.worth.ifs.user.resource.UserResource;
import com.worth.ifs.user.resource.UserRoleType;
import com.worth.ifs.user.service.ProcessRoleService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/competition/{competitionId}/application")
public class ApplicationManagementController extends AbstractApplicationController {
    @SuppressWarnings("unused")
    private static final Log LOG = LogFactory.getLog(ApplicationManagementController.class);
    @Autowired
    protected UserAuthenticationService userAuthenticationService;
    @Autowired
    protected ProcessRoleService processRoleService;
    @Autowired
    protected FormInputRestService formInputRestService;
    @Autowired
    CompetitionService competitionService;
    @Autowired
    ApplicationSummaryRestService applicationSummaryRestService;
    @Autowired
    FormInputResponseService formInputResponseService;
    @Autowired
    FileEntryRestService fileEntryRestService;

    @RequestMapping(value = "/{applicationId}", method = RequestMethod.GET)
    public String displayApplicationForCompetitionAdministrator(@PathVariable("competitionId") final String competitionId,
                                                                @PathVariable("applicationId") final String applicationIdString,
                                                                @ModelAttribute("form") ApplicationForm form,
                                                                Model model,
                                                                HttpServletRequest request
    ) {
        UserResource user = getLoggedUser(request);
        Long applicationId = Long.valueOf(applicationIdString);
        form.setAdminMode(true);

        List<FormInputResponseResource> responses = formInputResponseService.getByApplication(applicationId);
        model.addAttribute("responses", formInputResponseService.mapFormInputResponsesToFormInput(responses));

        ApplicationResource application = applicationService.getById(applicationId);
        // so the mode is viewonly
        application.enableViewMode();

        CompetitionResource competition = competitionService.getById(application.getCompetition());
        addApplicationAndSections(application, competition, user.getId(), Optional.empty(), Optional.empty(), model, form);
        addOrganisationAndUserFinanceDetails(competition.getId(), applicationId, user, model, form);
        addAppendices(applicationId, responses, model);

        model.addAttribute("applicationReadyForSubmit", false);
        model.addAttribute("isCompManagementDownload", true);
        return "competition-mgt-application-overview";
    }

    @RequestMapping(value = "/{applicationId}/finances/{organisationId}", method = RequestMethod.GET)
    public String displayApplicationForCompetitionAdministrator(@PathVariable("competitionId") final String competitionId,
                                                                @PathVariable("applicationId") final String applicationIdString,
                                                                @PathVariable("organisationId") final String organisationId,
                                                                @ModelAttribute("form") ApplicationForm form,
                                                                Model model
    ) throws ExecutionException, InterruptedException {
        Long applicationId = Long.valueOf(applicationIdString);
        ApplicationResource application = applicationService.getById(applicationId);
        CompetitionResource competition = competitionService.getById(application.getCompetition());
        SectionResource financeSection = sectionService.getSectionsForCompetitionByType(application.getCompetition(), SectionType.ORGANISATION_FINANCES).get(0);
        List<FormInputResponseResource> responses = formInputResponseService.getByApplication(applicationId);
        UserResource impersonatingUser = getImpersonateUserByOrganisationId(organisationId, form, applicationId);

        // so the mode is viewonly
        form.setAdminMode(true);
        application.enableViewMode();
        model.addAttribute("responses", formInputResponseService.mapFormInputResponsesToFormInput(responses));

        addApplicationAndSections(application, competition, impersonatingUser.getId(), Optional.ofNullable(financeSection), Optional.empty(), model, form);
        addOrganisationAndUserFinanceDetails(competition.getId(), applicationId, impersonatingUser, model, form);

        model.addAttribute("applicationReadyForSubmit", false);
        return "comp-mgt-application-finances";
    }

    private UserResource getImpersonateUserByOrganisationId(@PathVariable("organisationId") String organisationId, @ModelAttribute("form") ApplicationForm form, Long applicationId) throws InterruptedException, ExecutionException {
        UserResource user;
        form.setImpersonateOrganisationId(Long.valueOf(organisationId));
        List<ProcessRoleResource> processRoles = processRoleService.findProcessRolesByApplicationId(applicationId);
        Optional<Long> userId = processRoles.stream()
                .filter(p -> p.getOrganisation().equals(Long.valueOf(organisationId)))
                .map(p -> p.getUser())
                .findAny();

        if (!userId.isPresent()) {
            LOG.error("Found no user to impersonate.");
            return null;
        }
        user = userService.retrieveUserById(userId.get()).getSuccessObject();
        return user;
    }

    @RequestMapping(value = "/{applicationId}/forminput/{formInputId}/download", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<ByteArrayResource> downloadQuestionFile(
            @PathVariable("applicationId") final Long applicationId,
            @PathVariable("formInputId") final Long formInputId,
            HttpServletRequest request) throws ExecutionException, InterruptedException {
        final UserResource user = userAuthenticationService.getAuthenticatedUser(request);
        ProcessRoleResource processRole;
        if (user.hasRole(UserRoleType.COMP_ADMIN)) {
            long processRoleId = formInputResponseService.getByFormInputIdAndApplication(formInputId, applicationId).getSuccessObjectOrThrowException().get(0).getUpdatedBy();
            processRole = processRoleService.getById(processRoleId).get();
        } else {
            processRole = processRoleService.findProcessRole(user.getId(), applicationId);
        }

        final ByteArrayResource resource = formInputResponseService.getFile(formInputId, applicationId, processRole.getId()).getSuccessObjectOrThrowException();
        return getPdfFile(resource);
    }

    private ResponseEntity<ByteArrayResource> getPdfFile(ByteArrayResource resource) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentLength(resource.contentLength());
        httpHeaders.setContentType(MediaType.parseMediaType("application/pdf"));
        return new ResponseEntity<>(resource, httpHeaders, HttpStatus.OK);
    }

    private void addAppendices(Long applicationId, List<FormInputResponseResource> responses, Model model) {
        final List<AppendixResource> appendices = responses.stream().filter(fir -> fir.getFileEntry() != null).
                map(fir -> {
                    FormInputResource formInputResource = formInputRestService.getById(fir.getFormInput()).getSuccessObject();
                    FileEntryResource fileEntryResource = fileEntryRestService.findOne(fir.getFileEntry()).getSuccessObject();
                    String title = formInputResource.getDescription() != null ? formInputResource.getDescription() : fileEntryResource.getName();
                    return new AppendixResource(applicationId, formInputResource.getId(), title, fileEntryResource);
                }).
                collect(Collectors.toList());
        model.addAttribute("appendices", appendices);
    }
}
