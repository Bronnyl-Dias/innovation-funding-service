package org.innovateuk.ifs.application.populator.section;


import org.innovateuk.ifs.applicant.resource.ApplicantSectionResource;
import org.innovateuk.ifs.application.populator.ApplicationNavigationPopulator;
import org.innovateuk.ifs.application.populator.forminput.FormInputViewModelGenerator;
import org.innovateuk.ifs.application.service.SectionService;
import org.innovateuk.ifs.application.viewmodel.forminput.AbstractFormInputViewModel;
import org.innovateuk.ifs.application.viewmodel.section.YourOrganisationSectionViewModel;
import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.form.ApplicationForm;
import org.innovateuk.ifs.form.resource.FormInputType;
import org.innovateuk.ifs.organisation.resource.OrganisationResource;
import org.innovateuk.ifs.organisation.resource.OrganisationTypeEnum;
import org.innovateuk.ifs.user.service.OrganisationRestService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.innovateuk.ifs.applicant.builder.ApplicantResourceBuilder.newApplicantResource;
import static org.innovateuk.ifs.applicant.builder.ApplicantSectionResourceBuilder.newApplicantSectionResource;
import static org.innovateuk.ifs.application.builder.ApplicationResourceBuilder.newApplicationResource;
import static org.innovateuk.ifs.commons.rest.RestResult.restSuccess;
import static org.innovateuk.ifs.competition.builder.CompetitionResourceBuilder.newCompetitionResource;
import static org.innovateuk.ifs.form.builder.FormInputResourceBuilder.newFormInputResource;
import static org.innovateuk.ifs.form.builder.SectionResourceBuilder.newSectionResource;
import static org.innovateuk.ifs.organisation.builder.OrganisationResourceBuilder.newOrganisationResource;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link YourOrganisationSectionPopulator}
 */
@RunWith(MockitoJUnitRunner.class)
public class YourOrganisationSectionPopulatorTest {

    @InjectMocks
    private YourOrganisationSectionPopulator yourOrganisationSectionPopulator;

    @Mock
    private ApplicationNavigationPopulator navigationPopulator;

    @Mock
    private SectionService sectionService;

    @Mock
    private FormInputViewModelGenerator formInputViewModelGenerator;

    @Mock
    private OrganisationRestService organisationRestService;

    @Test
    public void populate() {
        OrganisationResource organisation = newOrganisationResource().withOrganisationType(OrganisationTypeEnum.BUSINESS.getId()).build();

        CompetitionResource competition = newCompetitionResource()
                .withStateAid(true)
                .build();

        ApplicantSectionResource section = newApplicantSectionResource()
                .withCurrentApplicant(newApplicantResource().withOrganisation(organisation).build())
                .withCurrentUser(newUserResource().build())
                .withCompetition(competition)
                .withApplication(newApplicationResource().build())
                .withSection(newSectionResource().build())
                .build();
        ApplicationForm form = mock(ApplicationForm.class);
        Model model = mock(Model.class);
        BindingResult bindingResult = mock(BindingResult.class);

        AbstractFormInputViewModel organisationSize = mock(AbstractFormInputViewModel.class);
        AbstractFormInputViewModel financialYearEnd = mock(AbstractFormInputViewModel.class);
        AbstractFormInputViewModel overviewRow = mock(AbstractFormInputViewModel.class);

        List<AbstractFormInputViewModel> formInputViewModels = asList(organisationSize, financialYearEnd, overviewRow);

        when(organisationSize.getFormInput()).thenReturn(newFormInputResource().withType(FormInputType.ORGANISATION_SIZE).build());
        when(financialYearEnd.getFormInput()).thenReturn(newFormInputResource().withType(FormInputType.FINANCIAL_YEAR_END).build());
        when(overviewRow.getFormInput()).thenReturn(newFormInputResource().withType(FormInputType.FINANCIAL_OVERVIEW_ROW).build());
        when(sectionService.getCompleted(section.getApplication().getId(), section.getCurrentApplicant().getOrganisation().getId())).thenReturn(emptyList());
        when(formInputViewModelGenerator.fromSection(section, section, form, false)).thenReturn(formInputViewModels);
        when(organisationRestService.getOrganisationById(organisation.getId())).thenReturn(restSuccess(organisation));

        YourOrganisationSectionViewModel viewModel = yourOrganisationSectionPopulator.populate(section, form, model, bindingResult, false, Optional.of(2L), true);

        assertThat(viewModel.isSection(), is(true));
        assertThat(viewModel.isComplete(), is(false));
        assertThat(viewModel.getFormInputViewModels(), equalTo(formInputViewModels));
        assertThat(viewModel.getOrganisationSizeFormInputViewModel(), equalTo(organisationSize));
        assertThat(viewModel.getFinancialEndYearFormInputViewModel(), equalTo(financialYearEnd));
        assertThat(viewModel.getFinanceOverviewRows(), equalTo(singletonList(overviewRow)));
        assertThat(viewModel.getStandardInputViewModels(), equalTo(emptyList()));
        assertThat(viewModel.getApplicantOrganisationId(), equalTo(2L));
        assertThat(viewModel.isReadOnlyAllApplicantApplicationFinances(), is(true));
        assertThat(viewModel.isStateAidEligibility(), is(true));
    }
}