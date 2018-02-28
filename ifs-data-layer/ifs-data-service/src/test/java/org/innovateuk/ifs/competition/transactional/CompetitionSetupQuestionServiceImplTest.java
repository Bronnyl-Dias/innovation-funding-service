package org.innovateuk.ifs.competition.transactional;

import org.innovateuk.ifs.BaseServiceUnitTest;
import org.innovateuk.ifs.application.domain.GuidanceRow;
import org.innovateuk.ifs.application.domain.Question;
import org.innovateuk.ifs.application.repository.GuidanceRowRepository;
import org.innovateuk.ifs.application.repository.QuestionRepository;
import org.innovateuk.ifs.commons.error.CommonErrors;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.competition.domain.Competition;
import org.innovateuk.ifs.competition.resource.CompetitionSetupQuestionResource;
import org.innovateuk.ifs.competition.resource.CompetitionSetupQuestionType;
import org.innovateuk.ifs.competition.resource.GuidanceRowResource;
import org.innovateuk.ifs.file.resource.FileTypeCategory;
import org.innovateuk.ifs.form.domain.FormInput;
import org.innovateuk.ifs.form.mapper.GuidanceRowMapper;
import org.innovateuk.ifs.form.repository.FormInputRepository;
import org.innovateuk.ifs.form.resource.FormInputScope;
import org.innovateuk.ifs.form.resource.FormInputType;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.innovateuk.ifs.application.builder.GuidanceRowBuilder.newFormInputGuidanceRow;
import static org.innovateuk.ifs.application.builder.GuidanceRowResourceBuilder.newFormInputGuidanceRowResourceBuilder;
import static org.innovateuk.ifs.application.builder.QuestionBuilder.newQuestion;
import static org.innovateuk.ifs.commons.error.CommonFailureKeys.COMPETITION_NOT_EDITABLE;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceFailure;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.competition.builder.CompetitionBuilder.newCompetition;
import static org.innovateuk.ifs.competition.builder.CompetitionSetupQuestionResourceBuilder.newCompetitionSetupQuestionResource;
import static org.innovateuk.ifs.form.builder.FormInputBuilder.newFormInput;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests the CompetitionSetupQuestionServiceImpl with mocked repositories/mappers.
 */
public class CompetitionSetupQuestionServiceImplTest extends BaseServiceUnitTest<CompetitionSetupQuestionServiceImpl> {

    @Override
    protected CompetitionSetupQuestionServiceImpl supplyServiceUnderTest() {
        return new CompetitionSetupQuestionServiceImpl();
    }

    private static String number = "number";
    private static String shortTitle = CompetitionSetupQuestionType.SCOPE.getShortName();
    private static String newShortTitle = "CannotBeSet";
    private static String title = "title";
    private static String subTitle = "subTitle";
    private static String guidanceTitle = "guidanceTitle";
    private static String guidance = "guidance";
    private static String fileUploadGuidance = "fileUploadGuidance";
    private static Integer maxWords = 1;
    private static String assessmentGuidanceAnswer = "assessmentGuidance";
    private static String assessmentGuidanceTitle = "assessmentGuidanceTitle";
    private static Integer assessmentMaxWords = 2;
    private static Integer scoreTotal = 10;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private FormInputRepository formInputRepository;

    @Mock
    private GuidanceRowMapper guidanceRowMapper;

    @Mock
    private GuidanceRowRepository guidanceRowRepository;

    @Mock
    private CompetitionSetupTemplateService competitionSetupTemplateService;

    @Test
    public void test_getByQuestionId() {
        Long questionId = 1L;
        List<GuidanceRow> guidanceRows = newFormInputGuidanceRow().build(1);
        Question question = newQuestion().
                withFormInputs(asList(
                        newFormInput()
                                .withType(FormInputType.FILEUPLOAD)
                                .withScope(FormInputScope.APPLICATION)
                                .withGuidanceAnswer(fileUploadGuidance)
                                .build(),
                        newFormInput()
                                .withType(FormInputType.TEXTAREA)
                                .withScope(FormInputScope.APPLICATION)
                                .withWordCount(maxWords)
                                .withGuidanceTitle(guidanceTitle)
                                .withGuidanceAnswer(guidance)
                                .build(),
                        newFormInput()
                                .withType(FormInputType.TEXTAREA)
                                .withScope(FormInputScope.ASSESSMENT)
                                .withWordCount(assessmentMaxWords)
                                .withGuidanceTitle(assessmentGuidanceTitle)
                                .withGuidanceAnswer(assessmentGuidanceAnswer)
                                .withGuidanceRows(guidanceRows)
                                .build(),
                        newFormInput()
                                .withType(FormInputType.ASSESSOR_SCORE)
                                .withScope(FormInputScope.ASSESSMENT)
                                .build(),
                        newFormInput()
                                .withType(FormInputType.ASSESSOR_RESEARCH_CATEGORY)
                                .withScope(FormInputScope.ASSESSMENT)
                                .build(),
                        newFormInput()
                                .withType(FormInputType.ASSESSOR_APPLICATION_IN_SCOPE)
                                .withScope(FormInputScope.ASSESSMENT)
                                .build()
                        )

                )
                .withQuestionNumber(number)
                .withAssessorMaximumScore(scoreTotal)
                .withDescription(subTitle)
                .withShortName(shortTitle)
                .withName(title)
                .withId(questionId)
                .build();


        when(questionRepository.findOne(questionId)).thenReturn(question);
        when(guidanceRowMapper.mapToResource(guidanceRows)).thenReturn(new ArrayList<>());

        ServiceResult<CompetitionSetupQuestionResource> result = service.getByQuestionId(questionId);

        assertTrue(result.isSuccess());

        CompetitionSetupQuestionResource resource = result.getSuccess();

        assertEquals(resource.getAppendix(), true);
        assertEquals(resource.getScored(), true);
        assertEquals(resource.getWrittenFeedback(), true);
        assertEquals(resource.getScope(), true);
        assertEquals(resource.getResearchCategoryQuestion(), true);
        assertEquals(resource.getAssessmentGuidance(), assessmentGuidanceAnswer);
        assertEquals(resource.getAssessmentGuidanceTitle(), assessmentGuidanceTitle);
        assertEquals(resource.getAssessmentMaxWords(), assessmentMaxWords);
        assertEquals(resource.getGuidanceTitle(), guidanceTitle);
        assertEquals(resource.getMaxWords(), maxWords);
        assertEquals(resource.getScoreTotal(), scoreTotal);
        assertEquals(resource.getNumber(), number);
        assertEquals(resource.getQuestionId(), questionId);
        assertEquals(resource.getSubTitle(), subTitle);
        assertEquals(resource.getShortTitle(), shortTitle);
        assertEquals(resource.getTitle(), title);
        assertEquals(resource.getGuidance(), guidance);
        assertEquals(resource.getType(), CompetitionSetupQuestionType.SCOPE);
        assertEquals(resource.getShortTitleEditable(), false);
        assertEquals(resource.getAppendixGuidance(), fileUploadGuidance);

        verify(guidanceRowMapper).mapToResource(guidanceRows);
    }

    @Test
    public void test_update() {
        long questionId = 1L;

        List<GuidanceRowResource> guidanceRows = newFormInputGuidanceRowResourceBuilder().build(1);
        when(guidanceRowMapper.mapToDomain(guidanceRows)).thenReturn(new ArrayList<>());

        CompetitionSetupQuestionResource resource = newCompetitionSetupQuestionResource()
                .withAppendix(false)
                .withGuidance(guidance)
                .withGuidanceTitle(guidanceTitle)
                .withMaxWords(maxWords)
                .withNumber(number)
                .withTitle(title)
                .withShortTitle(newShortTitle)
                .withSubTitle(subTitle)
                .withQuestionId(questionId)
                .withAssessmentGuidance(assessmentGuidanceAnswer)
                .withAssessmentGuidanceTitle(assessmentGuidanceTitle)
                .withAssessmentMaxWords(assessmentMaxWords)
                .withGuidanceRows(guidanceRows)
                .withScored(true)
                .withScoreTotal(scoreTotal)
                .withWrittenFeedback(true)
                .build();

        Question question = newQuestion().
                withShortName(CompetitionSetupQuestionType.SCOPE.getShortName()).build();

        FormInput questionFormInput = newFormInput().build();
        FormInput appendixFormInput = newFormInput().build();
        FormInput researchCategoryQuestionFormInput = newFormInput().build();
        FormInput scopeQuestionFormInput = newFormInput().build();
        FormInput scoredQuestionFormInput = newFormInput().build();
        FormInput writtenFeedbackFormInput = newFormInput().build();

        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.APPLICATION, FormInputType.TEXTAREA)).thenReturn(questionFormInput);
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.APPLICATION, FormInputType.FILEUPLOAD)).thenReturn(appendixFormInput);
        when(questionRepository.findOne(questionId)).thenReturn(question);
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.ASSESSMENT, FormInputType.ASSESSOR_RESEARCH_CATEGORY)).thenReturn(researchCategoryQuestionFormInput);
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.ASSESSMENT, FormInputType.ASSESSOR_APPLICATION_IN_SCOPE)).thenReturn(scopeQuestionFormInput);
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.ASSESSMENT, FormInputType.ASSESSOR_SCORE)).thenReturn(scoredQuestionFormInput);
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.ASSESSMENT, FormInputType.TEXTAREA)).thenReturn(writtenFeedbackFormInput);

        doNothing().when(guidanceRowRepository).delete(writtenFeedbackFormInput.getGuidanceRows());
        when(guidanceRowRepository.save(writtenFeedbackFormInput.getGuidanceRows())).thenReturn(writtenFeedbackFormInput.getGuidanceRows());

        ServiceResult<CompetitionSetupQuestionResource> result = service.update(resource);

        assertTrue(result.isSuccess());
        assertNotEquals(question.getQuestionNumber(), number);
        assertEquals(question.getDescription(), subTitle);
        assertEquals(question.getName(), title);
        assertEquals(questionFormInput.getGuidanceTitle(), guidanceTitle);
        assertEquals(questionFormInput.getGuidanceAnswer(), guidance);
        assertEquals(questionFormInput.getWordCount(), maxWords);
        assertEquals(writtenFeedbackFormInput.getGuidanceAnswer(), assessmentGuidanceAnswer);
        assertEquals(writtenFeedbackFormInput.getGuidanceTitle(), assessmentGuidanceTitle);
        //Short name shouldn't be set on SCOPE question.
        assertNotEquals(question.getShortName(), newShortTitle);
        assertEquals(question.getShortName(), shortTitle);

        assertEquals(appendixFormInput.getActive(), false);
        assertEquals(appendixFormInput.getGuidanceAnswer(), null);

        assertEquals(researchCategoryQuestionFormInput.getActive(), true);
        assertEquals(scopeQuestionFormInput.getActive(), true);
        assertEquals(scoredQuestionFormInput.getActive(), true);
        assertEquals(writtenFeedbackFormInput.getActive(), true);

        verify(guidanceRowMapper).mapToDomain(guidanceRows);
    }

    @Test
    public void test_updateShouldNotChangeAppendixFormInputWhenItCantBeFound() {
        setMocksForSuccessfulUpdate();
        CompetitionSetupQuestionResource resource = createValidQuestionResourceWithoutAppendixOptions();

        resource.setAppendix(false);
        resource.setAllowedFileTypesEnum(asSet(FileTypeCategory.PDF));
        resource.setAppendixGuidance(fileUploadGuidance);


        boolean appendixEnabled = true;
        String guidanceAnswer = "Only excel files with spaghetti VB macros allowed";
        String allowedFileTypes = "XLSX";

        FormInput appendixFormInput = newFormInput()
                .withActive(appendixEnabled)
                .withGuidanceAnswer(guidanceAnswer)
                .withAllowedFileTypes(allowedFileTypes)
                .build();
        //Override repository response set in setMocksForSuccessfulUpdate test prep function
        when(formInputRepository.findByQuestionIdAndScopeAndType(
                1L,
                FormInputScope.APPLICATION,
                FormInputType.FILEUPLOAD
        )).thenReturn(appendixFormInput);

        ServiceResult<CompetitionSetupQuestionResource> result = service.update(resource);

        assertEquals(true, result.isSuccess());
        assertNotEquals(appendixEnabled, appendixFormInput.getActive());
        assertNotEquals(guidanceAnswer, appendixFormInput.getAllowedFileTypes());
        assertNotEquals(allowedFileTypes, appendixFormInput.getGuidanceAnswer());
    }

    @Test
    public void test_updateShouldNotChangeAppendixFormInputWhenOptionIsNull() {
        setMocksForSuccessfulUpdate();
        CompetitionSetupQuestionResource resource = createValidQuestionResourceWithoutAppendixOptions();

        resource.setAppendix(false);
        resource.setAllowedFileTypesEnum(asSet(FileTypeCategory.PDF));
        resource.setAppendixGuidance(fileUploadGuidance);


        boolean appendixEnabled = true;
        String guidanceAnswer = "Only excel files with spaghetti VB macros allowed";
        String allowedFileTypes = "XLSX";

        FormInput appendixFormInput = newFormInput()
                .withActive(appendixEnabled)
                .withGuidanceAnswer(guidanceAnswer)
                .withAllowedFileTypes(allowedFileTypes)
                .build();
        //Override repository response set in prerequisites test prep function
        when(formInputRepository.findByQuestionIdAndScopeAndType(
                1L,
                FormInputScope.APPLICATION,
                FormInputType.FILEUPLOAD
        )).thenReturn(appendixFormInput);

        ServiceResult<CompetitionSetupQuestionResource> result = service.update(resource);

        assertEquals(true, result.isSuccess());
        assertNotEquals(appendixEnabled, appendixFormInput.getActive());
        assertNotEquals(guidanceAnswer, appendixFormInput.getAllowedFileTypes());
        assertNotEquals(allowedFileTypes, appendixFormInput.getGuidanceAnswer());
    }

    @Test
    public void test_updateShouldResetAppendixOptionsFormInputWhenItsNotSelected() {
        setMocksForSuccessfulUpdate();
        CompetitionSetupQuestionResource resource = createValidQuestionResourceWithoutAppendixOptions();

        resource.setAppendix(false);
        resource.setAllowedFileTypesEnum(asSet(FileTypeCategory.PDF));
        resource.setAppendixGuidance(fileUploadGuidance);

        FormInput appendixFormInput = newFormInput()
                .withActive(true)
                .withGuidanceAnswer("Only excel files with spaghetti VB macros allowed")
                .withAllowedFileTypes("XLSX")
                .build();

        //Override repository response set in prerequisites test prep function
        when(formInputRepository.findByQuestionIdAndScopeAndType(
                1L,
                FormInputScope.APPLICATION,
                FormInputType.FILEUPLOAD
        )).thenReturn(appendixFormInput);

        ServiceResult<CompetitionSetupQuestionResource> result = service.update(resource);

        assertEquals(true, result.isSuccess());
        assertFalse(appendixFormInput.getActive());
        assertNull(appendixFormInput.getAllowedFileTypes());
        assertNull(appendixFormInput.getGuidanceAnswer());
    }

    @Test
    public void test_updateShouldSetAppendixOptionsFormInputWhenSelected() {
        setMocksForSuccessfulUpdate();
        CompetitionSetupQuestionResource resource = createValidQuestionResourceWithoutAppendixOptions();

        resource.setAppendix(true);
        resource.setAllowedFileTypesEnum(asSet(FileTypeCategory.PDF));
        resource.setAppendixGuidance(fileUploadGuidance);

        FormInput appendixFormInput = newFormInput().build();
        //Override repository response set in prerequisites test prep function
        when(formInputRepository.findByQuestionIdAndScopeAndType(
                1L,
                FormInputScope.APPLICATION,
                FormInputType.FILEUPLOAD
        )).thenReturn(appendixFormInput);

        ServiceResult<CompetitionSetupQuestionResource> result = service.update(resource);

        assertEquals(true, result.isSuccess());
        assertTrue(appendixFormInput.getActive());
        assertEquals(FileTypeCategory.PDF.getDisplayName(), appendixFormInput.getAllowedFileTypes());
        assertEquals(fileUploadGuidance, appendixFormInput.getGuidanceAnswer());
    }

    @Test
    public void test_updateShouldAppendFileTypeSeparatedByComma() {
        Long questionId = 1L;

        setMocksForSuccessfulUpdate();
        CompetitionSetupQuestionResource resource = createValidQuestionResourceWithoutAppendixOptions();

        resource.setAppendix(true);
        resource.setAllowedFileTypesEnum(asSet(FileTypeCategory.PDF, FileTypeCategory.SPREADSHEET));
        resource.setAppendixGuidance(fileUploadGuidance);

        FormInput appendixFormInput = newFormInput().build();
        //Override repository response set in prerequisites test prep function
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.APPLICATION, FormInputType.FILEUPLOAD)).thenReturn(appendixFormInput);

        ServiceResult<CompetitionSetupQuestionResource> result = service.update(resource);

        assertTrue(appendixFormInput.getAllowedFileTypes().contains(FileTypeCategory.PDF.getDisplayName()));
        assertTrue(appendixFormInput.getAllowedFileTypes().contains(FileTypeCategory.SPREADSHEET.getDisplayName()));
    }

    private void setMocksForSuccessfulUpdate() {
        long questionId = 1L;
        when(guidanceRowMapper.mapToDomain(anyList())).thenReturn(new ArrayList<>());

        Question question = newQuestion().
                withShortName(CompetitionSetupQuestionType.SCOPE.getShortName()).build();

        FormInput questionFormInput = newFormInput().build();
        FormInput appendixFormInput = newFormInput().build();
        FormInput researchCategoryQuestionFormInput = newFormInput().build();
        FormInput scopeQuestionFormInput = newFormInput().build();
        FormInput scoredQuestionFormInput = newFormInput().build();
        FormInput writtenFeedbackFormInput = newFormInput().build();

        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.APPLICATION, FormInputType.TEXTAREA)).thenReturn(questionFormInput);
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.APPLICATION, FormInputType.FILEUPLOAD)).thenReturn(appendixFormInput);
        when(questionRepository.findOne(questionId)).thenReturn(question);
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.ASSESSMENT, FormInputType.ASSESSOR_RESEARCH_CATEGORY)).thenReturn(researchCategoryQuestionFormInput);
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.ASSESSMENT, FormInputType.ASSESSOR_APPLICATION_IN_SCOPE)).thenReturn(scopeQuestionFormInput);
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.ASSESSMENT, FormInputType.ASSESSOR_SCORE)).thenReturn(scoredQuestionFormInput);
        when(formInputRepository.findByQuestionIdAndScopeAndType(questionId, FormInputScope.ASSESSMENT, FormInputType.TEXTAREA)).thenReturn(writtenFeedbackFormInput);

        doNothing().when(guidanceRowRepository).delete(writtenFeedbackFormInput.getGuidanceRows());
        when(guidanceRowRepository.save(writtenFeedbackFormInput.getGuidanceRows())).thenReturn(writtenFeedbackFormInput.getGuidanceRows());
    }

    @Test
    public void test_delete() {
        long questionId = 1L;
        when(competitionSetupTemplateService.deleteAssessedQuestionInCompetition(questionId)).thenReturn(serviceSuccess());
        ServiceResult<Void> result = service.delete(questionId);
        assertTrue(result.isSuccess());
    }

    @Test
    public void test_createByCompetitionId() {
        Long competitionId = 22L;
        Long questionId = 33L;
        Competition competition = newCompetition().build();
        Question newlyCreatedQuestion = newQuestion().withId(questionId).build();
        when(competitionRepositoryMock.findById(competitionId)).thenReturn(competition);
        when(competitionSetupTemplateService.addDefaultAssessedQuestionToCompetition(competition)).thenReturn(serviceSuccess(newlyCreatedQuestion));
        when(questionRepository.findOne(questionId)).thenReturn(newlyCreatedQuestion);

        ServiceResult<CompetitionSetupQuestionResource> result = service.createByCompetitionId(competitionId);
        assertTrue(result.isSuccess());

        CompetitionSetupQuestionResource resource = result.getSuccess();
        assertEquals(questionId, resource.getQuestionId());
    }

    @Test
    public void test_createByCompetitionIdWithNonExistentCompId() {
        Long competitionId = 22L;
        when(competitionRepositoryMock.findById(competitionId)).thenReturn(null);

        ServiceResult<CompetitionSetupQuestionResource> result = service.createByCompetitionId(competitionId);
        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(CommonErrors.notFoundError(Competition.class, competitionId)));
    }

    @Test
    public void test_createByCompetitionIdWhenDefaultCreationFails() {
        Long competitionId = 22L;
        Competition competition = newCompetition().build();
        when(competitionRepositoryMock.findById(competitionId)).thenReturn(competition);
        when(competitionSetupTemplateService.addDefaultAssessedQuestionToCompetition(competition)).thenReturn(serviceFailure(COMPETITION_NOT_EDITABLE));

        ServiceResult<CompetitionSetupQuestionResource> result = service.createByCompetitionId(competitionId);
        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(COMPETITION_NOT_EDITABLE));
    }

    private CompetitionSetupQuestionResource createValidQuestionResourceWithoutAppendixOptions() {
        CompetitionSetupQuestionResource resource = newCompetitionSetupQuestionResource()
                .withAppendix(false)
                .withGuidance(guidance)
                .withGuidanceTitle(guidanceTitle)
                .withMaxWords(maxWords)
                .withNumber(number)
                .withTitle(title)
                .withShortTitle(newShortTitle)
                .withSubTitle(subTitle)
                .withQuestionId(1L)
                .withAssessmentGuidance(assessmentGuidanceAnswer)
                .withAssessmentGuidanceTitle(assessmentGuidanceTitle)
                .withAssessmentMaxWords(assessmentMaxWords)
                .withGuidanceRows(newFormInputGuidanceRowResourceBuilder().build(1))
                .withScored(true)
                .withScoreTotal(scoreTotal)
                .withWrittenFeedback(true)
                .build();

        return resource;
    }
}
