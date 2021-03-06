package org.innovateuk.ifs.application.viewmodel.finance;

import org.innovateuk.ifs.finance.resource.cost.FinanceRowType;
import org.innovateuk.ifs.form.resource.FormInputType;

/**
 * View model for grant claim form input.
 */
public class GrantClaimCostViewModel extends AbstractCostViewModel {
    private long organisationGrantClaimPercentageId;
    private int maximumGrantClaimPercentage;
    private Integer organisationGrantClaimPercentage;


    @Override
    protected FormInputType formInputType() {
        return FormInputType.FINANCE;
    }

    @Override
    public FinanceRowType getFinanceRowType() {
        return FinanceRowType.FINANCE;
    }

    public long getOrganisationGrantClaimPercentageId() {
        return organisationGrantClaimPercentageId;
    }

    public void setOrganisationGrantClaimPercentageId(long organisationGrantClaimPercentageId) {
        this.organisationGrantClaimPercentageId = organisationGrantClaimPercentageId;
    }

    public int getMaximumGrantClaimPercentage() {
        return maximumGrantClaimPercentage;
    }

    public void setMaximumGrantClaimPercentage(int maximumGrantClaimPercentage) {
        this.maximumGrantClaimPercentage = maximumGrantClaimPercentage;
    }

    public Integer getOrganisationGrantClaimPercentage() {
        return organisationGrantClaimPercentage;
    }

    public void setOrganisationGrantClaimPercentage(Integer organisationGrantClaimPercentage) {
        this.organisationGrantClaimPercentage = organisationGrantClaimPercentage;
    }

    public boolean maximumGrantClaimPercentageIsSmallerThanOrganisationGrantClaimPercentage() {
        return maximumGrantClaimPercentage < (organisationGrantClaimPercentage == null ? 0 : organisationGrantClaimPercentage);
    }
}
