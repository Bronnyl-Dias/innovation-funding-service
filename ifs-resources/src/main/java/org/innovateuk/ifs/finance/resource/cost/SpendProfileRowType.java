package org.innovateuk.ifs.finance.resource.cost;

import org.innovateuk.ifs.form.resource.FormInputType;

import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * FinanceRow types are used to identify the different categories that costs can have
 */
public enum SpendProfileRowType implements CostCategoryGenerator<SpendProfileRowType> {
    LABOUR("labour", true, "Labour"),
    OVERHEADS("overheads", true, "Overheads"),
    MATERIALS("materials", true, "Materials"),
    CAPITAL_USAGE("capital_usage", true, "Capital usage"),
    SUBCONTRACTING_COSTS("subcontracting", true, "Subcontracting"),
    TRAVEL("travel", true, "Travel and subsistence"),
    OTHER_COSTS("other_costs", true, "Other costs"),
    YOUR_FINANCE("your_finance"),
    FINANCE("finance", false, "Finance"),
    OTHER_FUNDING("other_funding", false, "Other Funding"),
    ACADEMIC("academic");

    private String type;
    private boolean includedInGeneratedSpendProfile;
    private String name;

    SpendProfileRowType(String type) {
        this(type, false, null);
    }

    SpendProfileRowType(String type, boolean includedInGeneratedSpendProfile, String name) {
        this.type = type;
        this.includedInGeneratedSpendProfile = includedInGeneratedSpendProfile;
        this.name = name;
    }

    public static SpendProfileRowType fromType(FormInputType formInputType) {
        if (formInputType != null) {
            for (SpendProfileRowType costType : values()) {
                if (formInputType == FormInputType.findByName(costType.getType())) {
                    return costType;
                }
            }
        }
        throw new IllegalArgumentException("No valid FinanceRowType found for FormInputType: " + formInputType);
    }

    public String getType() {
        return type;
    }

    public FormInputType getFormInputType() {
        return FormInputType.findByName(getType());
    }

    public boolean isIncludedInGeneratedSpendProfile() {
        return includedInGeneratedSpendProfile;
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    public static Optional<SpendProfileRowType> getByTypeName(String typeName) {
        return asList(SpendProfileRowType.values()).stream().filter(frt -> frt.getType().equals(typeName)).findFirst();
    }
}

