package com.worth.ifs.competitionsetup.service.sectionupdaters;

import com.worth.ifs.application.service.CompetitionService;
import com.worth.ifs.application.service.MilestoneService;
import com.worth.ifs.commons.error.Error;
import com.worth.ifs.competition.resource.CompetitionResource;
import com.worth.ifs.competition.resource.CompetitionSetupSection;
import com.worth.ifs.competition.resource.MilestoneResource;
import com.worth.ifs.competition.resource.MilestoneType;
import com.worth.ifs.competitionsetup.form.CompetitionSetupForm;
import com.worth.ifs.competitionsetup.form.InitialDetailsForm;
import com.worth.ifs.competitionsetup.model.MilestoneEntry;
import com.worth.ifs.competitionsetup.service.CompetitionSetupMilestoneService;
import com.worth.ifs.competitionsetup.service.CompetitionSetupService;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * Competition setup section saver for the initial details section.
 */
@Service
public class InitialDetailsSectionSaver implements CompetitionSetupSectionSaver {

	private static Log LOG = LogFactory.getLog(InitialDetailsSectionSaver.class);
    public static String OPENINGDATE_FIELDNAME = "openingDate";

	@Autowired
	private CompetitionService competitionService;

	@Autowired
	private CompetitionSetupService competitionSetupService;

    @Autowired
    private MilestoneService milestoneService;

	@Autowired
	private CompetitionSetupMilestoneService competitionSetupMilestoneService;

	@Override
	public CompetitionSetupSection sectionToSave() {
		return CompetitionSetupSection.INITIAL_DETAILS;
	}

	@Override
	public List<Error> saveSection(CompetitionResource competition, CompetitionSetupForm competitionSetupForm) {
		
		InitialDetailsForm initialDetailsForm = (InitialDetailsForm) competitionSetupForm;

		Boolean isDiffCompType = competition.getCompetitionType() != initialDetailsForm.getCompetitionTypeId();

		competition.setName(initialDetailsForm.getTitle());
		competition.setExecutive(initialDetailsForm.getExecutiveUserId());

		try {
			LocalDateTime startDate = LocalDateTime.of(initialDetailsForm.getOpeningDateYear(),
					initialDetailsForm.getOpeningDateMonth(), initialDetailsForm.getOpeningDateDay(), 0, 0);
			competition.setStartDate(startDate);

            List<Error> errors = saveOpeningDateAsMilestone(startDate, competition.getId());
            if(!errors.isEmpty()) {
                return errors;
            }

		} catch (Exception e) {
			LOG.error(e.getMessage());

            return asList(Error.fieldError(OPENINGDATE_FIELDNAME, null, "Unable to save opening date"));
		}

		competition.setCompetitionType(initialDetailsForm.getCompetitionTypeId());
		competition.setLeadTechnologist(initialDetailsForm.getLeadTechnologistUserId());

		competition.setInnovationArea(initialDetailsForm.getInnovationAreaCategoryId());
		competition.setInnovationSector(initialDetailsForm.getInnovationSectorCategoryId());

		competitionService.update(competition);

        if(isDiffCompType) {
            competitionService.initApplicationFormByCompetitionType(competition.getId(), initialDetailsForm.getCompetitionTypeId());
        }

        return Collections.emptyList();
	}

	@Override
	public List<Error> autoSaveSectionField(CompetitionResource competitionResource, String fieldName, String value) {
		List<Error> errors = new ArrayList<>();

	    try {
            errors = updateCompetitionResourceWithAutosave(errors, competitionResource, fieldName, value);
        } catch (Exception e) {
            errors.add(new Error(e.getMessage(), HttpStatus.BAD_REQUEST));
        }

        if(!errors.isEmpty()) {
            return errors;
        }

        competitionService.update(competitionResource);

		return Collections.emptyList();
	}

	private List<Error> updateCompetitionResourceWithAutosave(List<Error> errors, CompetitionResource competitionResource, String fieldName, String value) {
        switch (fieldName) {
            case "title":
                competitionResource.setName(value);
                break;
            case "competitionTypeId":
                competitionResource.setCompetitionType(Long.parseLong(value));
                break;
            case "innovationSectorCategoryId":
                competitionResource.setInnovationSector(Long.parseLong(value));
                break;
            case "innovationAreaCategoryId":
                competitionResource.setInnovationArea(Long.parseLong(value));
                break;
            case "leadTechnologistUserId":
                competitionResource.setLeadTechnologist(Long.parseLong(value));
                break;
            case "executiveUserId":
                competitionResource.setExecutive(Long.parseLong(value));
                break;
            case "openingDate":
                try {
                    String[] dateParts = value.split("-");
                    LocalDateTime startDate = LocalDateTime.of(
                            Integer.parseInt(dateParts[2]),
                            Integer.parseInt(dateParts[1]),
                            Integer.parseInt(dateParts[0]),
                            0, 0, 0);
                    competitionResource.setStartDate(startDate);

                    errors.addAll(saveOpeningDateAsMilestone(startDate, competitionResource.getId()));
                    if(!errors.isEmpty()) {
                        return errors;
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                    return asList(Error.fieldError(OPENINGDATE_FIELDNAME, null, "Unable to save opening date"));
                }
                break;
            default:
                return asList(new Error("Field not found", HttpStatus.BAD_REQUEST));
        }

        return errors;
    }

	private List<Error> validateOpeningDate(LocalDateTime openingDate) {
        if (openingDate.isBefore(LocalDateTime.now())) {
            return asList(Error.fieldError(OPENINGDATE_FIELDNAME, openingDate.toString(), "Please enter a future date"));
        }

        return Collections.emptyList();
    }

	private List<Error> saveOpeningDateAsMilestone(LocalDateTime openingDate, Long competitionId) {
        List<Error> errors = validateOpeningDate(openingDate);
        if(!errors.isEmpty()) {
            return errors;
        }

	    MilestoneEntry milestoneEntry = new MilestoneEntry();
        milestoneEntry.setMilestoneType(MilestoneType.OPEN_DATE);
		milestoneEntry.setDay(openingDate.getDayOfMonth());
        milestoneEntry.setMonth(openingDate.getMonth().getValue());
        milestoneEntry.setYear(openingDate.getYear());

        List<MilestoneResource> milestones = milestoneService.getAllDatesByCompetitionId(competitionId);
        if(milestones.isEmpty()) {
            milestones = competitionSetupMilestoneService.createMilestonesForCompetition(competitionId);
        }
        milestones.sort((c1, c2) -> c1.getType().compareTo(c2.getType()));

		LinkedMap<String, MilestoneEntry> milestoneEntryMap = new LinkedMap<>();
		milestoneEntryMap.put(MilestoneType.OPEN_DATE.name(), milestoneEntry);

		return competitionSetupMilestoneService.updateMilestonesForCompetition(milestones, milestoneEntryMap, competitionId);
	}

	@Override
	public boolean supportsForm(Class<? extends CompetitionSetupForm> clazz) {
		return InitialDetailsForm.class.equals(clazz);
	}

}
