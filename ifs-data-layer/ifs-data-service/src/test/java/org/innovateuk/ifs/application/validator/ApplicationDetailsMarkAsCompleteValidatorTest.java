package org.innovateuk.ifs.application.validator;

import org.innovateuk.ifs.application.domain.Application;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Validator;

import java.time.LocalDate;

import static org.innovateuk.ifs.category.builder.InnovationAreaBuilder.newInnovationArea;
import static org.innovateuk.ifs.category.builder.ResearchCategoryBuilder.newResearchCategory;
import static org.innovateuk.ifs.competition.builder.CompetitionBuilder.newCompetition;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleFilter;
import static org.junit.Assert.*;

/**
 * Mark as complete validator test class for application details section
 */
public class ApplicationDetailsMarkAsCompleteValidatorTest {

    private Validator validator;

    BindingResult bindingResult;
    LocalDate currentDate;
    Application application;

    @Before
    public void setUp() {
        validator = new ApplicationDetailsMarkAsCompleteValidator();
        currentDate = LocalDate.now();
    }

    @Test
    public void testInvalid() throws Exception {

        application  = new Application();

        application.setName("");
        application.setStartDate(currentDate.minusDays(1));
        application.setDurationInMonths(-5L);
        application.setResubmission(null);
        application.setCompetition(newCompetition()
                .withMinProjectDuration(10)
                .withMaxProjectDuration(20).build());

        DataBinder binder = new DataBinder(application);
        bindingResult = binder.getBindingResult();
        validator.validate(application, bindingResult);

        assertTrue(bindingResult.hasErrors());
        assertEquals(5, bindingResult.getErrorCount());

        application.setName(null);
        application.setStartDate(currentDate.minusDays(1));
        application.setDurationInMonths(0L);
        application.setResubmission(true);
        application.setPreviousApplicationNumber(null);
        application.setPreviousApplicationTitle(null);


        binder = new DataBinder(application);
        bindingResult = binder.getBindingResult();

        validator.validate(application, bindingResult);
        assertTrue(bindingResult.hasErrors());
        assertEquals(6, bindingResult.getErrorCount());

        application.setDurationInMonths(37L);
        application.setResubmission(false);

        binder = new DataBinder(application);
        bindingResult = binder.getBindingResult();

        validator.validate(application, bindingResult);
        assertTrue(bindingResult.hasErrors());
        assertEquals(4, bindingResult.getErrorCount());
    }

    @Test
    public void testValidate_applicationInnovationAreaIsNotSetButApplicableShouldResultInError() {
        application  = new Application();
        application.setCompetition(newCompetition()
                .withMinProjectDuration(10)
                .withMaxProjectDuration(20).build());

        application.setNoInnovationAreaApplicable(false);

        DataBinder binder = new DataBinder(application);
        bindingResult = binder.getBindingResult();
        validator.validate(application, bindingResult);

        assertTrue(bindingResult.hasErrors());
        assertEquals(bindingResult.getFieldError("innovationArea").getDefaultMessage(), "validation.application.innovationarea.category.required");
    }

    @Test
    public void testValidate_applicationInnovationAreaIsApplicableButNotSetShouldResultInError() {
        application  = new Application();
        application.setCompetition(newCompetition()
                .withMinProjectDuration(10)
                .withMaxProjectDuration(20).build());

        DataBinder binder = new DataBinder(application);
        bindingResult = binder.getBindingResult();
        validator.validate(application, bindingResult);

        assertTrue(bindingResult.hasErrors());
        assertEquals(bindingResult.getFieldError("innovationArea").getDefaultMessage(), "validation.application.innovationarea.category.required");
    }

    @Test
    public void testValid() throws Exception {

        application  = new Application();

        application.setName("IFS TEST DEV Project");
        application.setStartDate(currentDate.plusDays(1));
        application.setDurationInMonths(18L);
        application.setResubmission(true);
        application.setPreviousApplicationNumber("A Number");
        application.setPreviousApplicationTitle("Failed Application");
        application.setNoInnovationAreaApplicable(true);
        application.setResearchCategory(newResearchCategory().build());
        application.setCompetition(newCompetition()
                .withMinProjectDuration(10)
                .withMaxProjectDuration(20).build());

        DataBinder binder = new DataBinder(application);
        bindingResult = binder.getBindingResult();
        validator.validate(application, bindingResult);

        assertFalse(bindingResult.hasErrors());
    }

    @Test
    public void testValid_applicationInnovationAreaIsApplicableAndSet() {
        application  = new Application();

        application.setName("IFS TEST DEV Project");
        application.setStartDate(currentDate.plusDays(1));
        application.setDurationInMonths(18L);
        application.setResubmission(true);
        application.setPreviousApplicationNumber("A Number");
        application.setPreviousApplicationTitle("Failed Application");
        application.setNoInnovationAreaApplicable(false);
        application.setInnovationArea(newInnovationArea().build());
        application.setResearchCategory(newResearchCategory().build());
        application.setCompetition(newCompetition()
                .withMinProjectDuration(10)
                .withMaxProjectDuration(20).build());

        DataBinder binder = new DataBinder(application);
        bindingResult = binder.getBindingResult();
        validator.validate(application, bindingResult);

        assertFalse(bindingResult.hasErrors());
    }

    @Test
    public void testValid_applicationDurationExceedsMaxDurationShouldResultInError() {
        application  = new Application();
        application.setDurationInMonths(21L);
        application.setCompetition(newCompetition()
                .withMinProjectDuration(10)
                .withMaxProjectDuration(20).build());

        DataBinder binder = new DataBinder(application);
        bindingResult = binder.getBindingResult();
        validator.validate(application, bindingResult);

        assertFalse(simpleFilter(
                bindingResult.getFieldErrors(),
                error -> error.getField().equals("durationInMonths")
                        && error.getDefaultMessage().equals("validation.project.duration.input.invalid")
                        && (Integer) error.getArguments()[0] == 10
                        && (Integer) error.getArguments()[1] == 20)
                .isEmpty());
    }

    @Test
    public void testValid_applicationDurationBeneathMinDurationShouldResultInError() {
        application  = new Application();
        application.setDurationInMonths(9L);
        application.setCompetition(newCompetition()
                .withMinProjectDuration(10)
                .withMaxProjectDuration(20).build());

        DataBinder binder = new DataBinder(application);
        bindingResult = binder.getBindingResult();
        validator.validate(application, bindingResult);

        assertFalse(simpleFilter(
                bindingResult.getFieldErrors(),
                error -> error.getField().equals("durationInMonths")
                        && error.getDefaultMessage().equals("validation.project.duration.input.invalid")
                        && (Integer) error.getArguments()[0] == 10
                        && (Integer) error.getArguments()[1] == 20)
                .isEmpty());
    }

    @Test
    public void testValid_applicationDurationIsEqualToMaxAndMinDurationShouldNotResultInError() {
        application  = new Application();
        application.setDurationInMonths(10L);
        application.setCompetition(newCompetition()
                .withMinProjectDuration(10)
                .withMaxProjectDuration(10).build());

        DataBinder binder = new DataBinder(application);
        bindingResult = binder.getBindingResult();
        validator.validate(application, bindingResult);

        assertTrue(simpleFilter(
                bindingResult.getFieldErrors(),
                error -> error.getField().equals("durationInMonths"))
                .isEmpty());
    }

    @Test
    public void testSupportsApplicationAndSubclasses() {
        assertTrue(validator.supports(Application.class));
        assertTrue(validator.supports(new Application() {
            //empty extension of application;
        }.getClass()));
    }
}
