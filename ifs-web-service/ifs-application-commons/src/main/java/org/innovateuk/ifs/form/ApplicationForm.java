package org.innovateuk.ifs.form;

import org.hibernate.validator.constraints.Length;
import org.innovateuk.ifs.application.resource.ApplicationResource;
import org.innovateuk.ifs.commons.validation.constraints.WordCount;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MultipartFile;

/**
 * This class is used to setup and submit the form input values. On submit the values are converted into an Form object.
 * http://stackoverflow.com/a/4511716
 */
public class ApplicationForm extends Form {

    private ApplicationResource application;

    private boolean adminMode = false;

    private boolean termsAgreed;

    private boolean stateAidAgreed;

    private Long impersonateOrganisationId;

    @Length(max = 5000, message = "{validation.field.too.many.characters}")
    @WordCount(max = 400, message = "{validation.field.max.word.count}")
    private String ineligibleReason;

    private MultipartFile overheadfile;
    private MultipartFile jesFileUpload;

    public ApplicationForm() {
        super();
    }

    public ApplicationResource getApplication() {
        return application;
    }

    public void setApplication(ApplicationResource application) {
        this.application = application;
    }

    /** Placeholder function for mapping errors to field in Thymeleaf */
    public String getOrganisationSize() {
        return "";
    }

    /** Placeholder function for mapping errors to field in Thymeleaf */
    public void setOrganisationSize(String organisationSize) {
        // does nothing
    }

    /** Placeholder function for mapping errors to field in Thymeleaf */
    public String getProjectLocation() {
        return "";
    }

    /** Placeholder function for mapping errors to field in Thymeleaf */
    public void setProjectLocation(String organisationSize) {
        // does nothing
    }

    public boolean isAdminMode() {
        return adminMode;
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
    }

    public Long getImpersonateOrganisationId() {
        return impersonateOrganisationId;
    }

    public void setImpersonateOrganisationId(Long impersonateOrganisationId) {
        this.impersonateOrganisationId = impersonateOrganisationId;
    }

    public boolean isTermsAgreed() {
        return termsAgreed;
    }

    public void setTermsAgreed(boolean termsAgreed) {
        this.termsAgreed = termsAgreed;
    }

    public boolean isStateAidAgreed() {
        return stateAidAgreed;
    }

    public void setStateAidAgreed(boolean stateAidAgreed) {
        this.stateAidAgreed = stateAidAgreed;
    }

    public String getRejectedValue(String fieldId){
        FieldError fieldError = getBindingResult().getFieldError("formInput[" + fieldId + "]");
        Object rejectedValue = fieldError != null ? fieldError.getRejectedValue() : null;
        return rejectedValue != null ? rejectedValue.toString() : null;
    }

    public String getIneligibleReason() {
        return ineligibleReason;
    }

    public void setIneligibleReason(String ineligibleReason) {
        this.ineligibleReason = ineligibleReason;
    }

    public MultipartFile getOverheadfile() {
        return overheadfile;
    }

    public void setOverheadfile(MultipartFile overheadfile) {
        this.overheadfile = overheadfile;
    }

    public MultipartFile getJesFileUpload() {
        return jesFileUpload;
    }

    public void setJesFileUpload(MultipartFile jesFileUpload) {
        this.jesFileUpload = jesFileUpload;
    }
}