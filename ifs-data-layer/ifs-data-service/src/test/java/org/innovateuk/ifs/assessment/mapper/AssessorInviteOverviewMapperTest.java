package org.innovateuk.ifs.assessment.mapper;

import org.innovateuk.ifs.category.domain.InnovationArea;
import org.innovateuk.ifs.category.mapper.InnovationAreaMapper;
import org.innovateuk.ifs.category.resource.InnovationAreaResource;
import org.innovateuk.ifs.invite.constant.InviteStatus;
import org.innovateuk.ifs.invite.domain.ParticipantStatus;
import org.innovateuk.ifs.invite.domain.competition.CompetitionAssessmentParticipant;
import org.innovateuk.ifs.invite.domain.competition.CompetitionParticipant;
import org.innovateuk.ifs.invite.mapper.ParticipantStatusMapper;
import org.innovateuk.ifs.invite.resource.AssessorInviteOverviewResource;
import org.innovateuk.ifs.invite.resource.ParticipantStatusResource;
import org.innovateuk.ifs.profile.domain.Profile;
import org.innovateuk.ifs.profile.repository.ProfileRepository;
import org.innovateuk.ifs.user.resource.BusinessType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.ZonedDateTime;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.innovateuk.ifs.assessment.builder.CompetitionAssessmentParticipantBuilder.newCompetitionAssessmentParticipant;
import static org.innovateuk.ifs.assessment.interview.builder.AssessmentInterviewPanelInviteBuilder.newAssessmentInterviewPanelInvite;
import static org.innovateuk.ifs.assessment.interview.builder.AssessmentInterviewPanelParticipantBuilder.newAssessmentInterviewPanelParticipant;
import static org.innovateuk.ifs.category.builder.InnovationAreaBuilder.newInnovationArea;
import static org.innovateuk.ifs.category.builder.InnovationAreaResourceBuilder.newInnovationAreaResource;
import static org.innovateuk.ifs.invite.builder.AssessorInviteOverviewResourceBuilder.newAssessorInviteOverviewResource;
import static org.innovateuk.ifs.invite.builder.CompetitionAssessmentInviteBuilder.newCompetitionAssessmentInvite;
import static org.innovateuk.ifs.invite.builder.RejectionReasonBuilder.newRejectionReason;
import static org.innovateuk.ifs.profile.builder.ProfileBuilder.newProfile;
import static org.innovateuk.ifs.user.builder.UserBuilder.newUser;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AssessorInviteOverviewMapperTest {

    @Mock
    private ParticipantStatusMapper participantStatusMapperMock;
    @Mock
    private ProfileRepository profileRepositoryMock;
    @Mock
    private InnovationAreaMapper innovationAreaMapperMock;

    private AssessorInviteOverviewMapper assessorInviteOverviewMapper;

    @Before
    public void setUp() throws Exception {
        assessorInviteOverviewMapper = new AssessorInviteOverviewMapper(
                participantStatusMapperMock,
                profileRepositoryMock,
                innovationAreaMapperMock
        );

        when(participantStatusMapperMock.mapToResource(ParticipantStatus.PENDING))
                .thenReturn(ParticipantStatusResource.PENDING);
        when(participantStatusMapperMock.mapToResource(ParticipantStatus.REJECTED))
                .thenReturn(ParticipantStatusResource.REJECTED);
    }

    @Test
    public void mapFromCompetitionAssessmentParticipant_newUser() {
        InnovationArea innovationArea = newInnovationArea().build();
        InnovationAreaResource innovationAreaResource = newInnovationAreaResource().build();

        when(innovationAreaMapperMock.mapToResource(innovationArea)).thenReturn(innovationAreaResource);

        CompetitionAssessmentParticipant competitionAssessmentParticipant =
                createCompetitionAssessmentParticipant(innovationArea);

        AssessorInviteOverviewResource assessorInviteOverviewResource =
                assessorInviteOverviewMapper.mapToResource(competitionAssessmentParticipant);

        verify(participantStatusMapperMock).mapToResource(ParticipantStatus.PENDING);
        verify(innovationAreaMapperMock).mapToResource(innovationArea);

        assertThat(assessorInviteOverviewResource).isEqualToComparingFieldByField(
                newAssessorInviteOverviewResource()
                        .withName("Joe Bloggs")
                        .withInviteId(competitionAssessmentParticipant.getInvite().getId())
                        .withDetails("Invite sent: 28 Feb 2018")
                        .withInnovationAreas(singletonList(innovationAreaResource))
                        .withStatus(ParticipantStatusResource.PENDING)
                        .build()
        );
    }

    @Test
    public void mapFromCompetitionAssessmentParticipant_existingUser() {
        InnovationArea innovationArea = newInnovationArea().build();
        InnovationAreaResource innovationAreaResource = newInnovationAreaResource().build();

        Profile profile = newProfile()
                .withBusinessType(BusinessType.BUSINESS)
                .withInnovationArea(innovationArea)
                .build();

        when(profileRepositoryMock.findOne(profile.getId())).thenReturn(profile);
        when(innovationAreaMapperMock.mapToResource(innovationArea)).thenReturn(innovationAreaResource);

        CompetitionAssessmentParticipant competitionAssessmentParticipant =
                createCompetitionAssessmentParticipant(newInnovationArea().build());

        competitionAssessmentParticipant.setUser(
                newUser()
                        .withProfileId(profile.getId())
                        .build()
        );

        AssessorInviteOverviewResource assessorInviteOverviewResource =
                assessorInviteOverviewMapper.mapToResource(competitionAssessmentParticipant);

        verify(participantStatusMapperMock).mapToResource(ParticipantStatus.PENDING);
        verify(profileRepositoryMock).findOne(profile.getId());
        verify(innovationAreaMapperMock).mapToResource(innovationArea);

        assertThat(assessorInviteOverviewResource).isEqualToComparingFieldByField(
                newAssessorInviteOverviewResource()
                        .withId(competitionAssessmentParticipant.getUser().getId())
                        .withName("Joe Bloggs")
                        .withInviteId(competitionAssessmentParticipant.getInvite().getId())
                        .withBusinessType(profile.getBusinessType())
                        .withDetails("Invite sent: 28 Feb 2018")
                        .withInnovationAreas(singletonList(innovationAreaResource))
                        .withStatus(ParticipantStatusResource.PENDING)
                        .build()
        );
    }

    private CompetitionAssessmentParticipant createCompetitionAssessmentParticipant(InnovationArea innovationArea) {
        return newCompetitionAssessmentParticipant()
                    .withInvite(
                            newCompetitionAssessmentInvite()
                                    .withName("Joe Bloggs")
                                    .withEmail("test@test.com")
                                    .withSentOn(ZonedDateTime.parse("2018-02-28T12:00:00Z"))
                                    .withStatus(InviteStatus.SENT)
                                    .withInnovationArea(innovationArea)
                                    .build()
                    )
                    .withStatus(ParticipantStatus.PENDING)
                    .build();
    }

    @Test
    public void mapFromCompetitionParticipant() {
        InnovationArea innovationArea = newInnovationArea().build();
        Profile profile = newProfile()
                .withBusinessType(BusinessType.BUSINESS)
                .withInnovationArea(innovationArea)
                .build();

        when(profileRepositoryMock.findOne(profile.getId())).thenReturn(profile);

        InnovationAreaResource innovationAreaResource = newInnovationAreaResource().build();

        when(innovationAreaMapperMock.mapToResource(innovationArea)).thenReturn(innovationAreaResource);

        CompetitionParticipant competitionAssessmentParticipant = newAssessmentInterviewPanelParticipant()
                .withInvite(
                        newAssessmentInterviewPanelInvite()
                                .withName("Joe Bloggs")
                                .withEmail("test@test.com")
                                .withSentOn(ZonedDateTime.parse("2018-02-28T12:00:00Z"))
                                .withStatus(InviteStatus.SENT)
                                .build()
                )
                .withUser(
                        newUser()
                                .withProfileId(profile.getId())
                                .build()
                )
                .withStatus(ParticipantStatus.PENDING)
                .withRejectionReason()
                .build();

        AssessorInviteOverviewResource assessorInviteOverviewResource =
                assessorInviteOverviewMapper.mapToResource(competitionAssessmentParticipant);

        verify(participantStatusMapperMock).mapToResource(ParticipantStatus.PENDING);
        verify(profileRepositoryMock).findOne(profile.getId());
        verify(innovationAreaMapperMock).mapToResource(innovationArea);

        assertThat(assessorInviteOverviewResource).isEqualToComparingFieldByField(
                newAssessorInviteOverviewResource()
                        .withId(competitionAssessmentParticipant.getUser().getId())
                        .withName("Joe Bloggs")
                        .withInviteId(competitionAssessmentParticipant.getInvite().getId())
                        .withBusinessType(profile.getBusinessType())
                        .withDetails("Invite sent: 28 Feb 2018")
                        .withInnovationAreas(singletonList(innovationAreaResource))
                        .withStatus(ParticipantStatusResource.PENDING)
                        .build()
        );
    }

    @Test
    public void mapFromCompetitionParticipant_inviteRejectedWithReason() {
        InnovationArea innovationArea = newInnovationArea().build();
        Profile profile = newProfile()
                .withInnovationArea(innovationArea)
                .build();

        when(profileRepositoryMock.findOne(profile.getId())).thenReturn(profile);

        when(innovationAreaMapperMock.mapToResource(innovationArea)).thenReturn(newInnovationAreaResource().build());

        CompetitionParticipant competitionAssessmentParticipant = newAssessmentInterviewPanelParticipant()
                .withInvite(newAssessmentInterviewPanelInvite().build())
                .withUser(
                        newUser()
                                .withProfileId(profile.getId())
                                .build()
                )
                .withStatus(ParticipantStatus.REJECTED)
                .withRejectionReason(
                        newRejectionReason()
                                .withReason("Could not attend")
                                .build()
                )
                .build();

        AssessorInviteOverviewResource assessorInviteOverviewResource =
                assessorInviteOverviewMapper.mapToResource(competitionAssessmentParticipant);

        assertThat(assessorInviteOverviewResource.getStatus()).isEqualTo(ParticipantStatusResource.REJECTED);
        assertThat(assessorInviteOverviewResource.getDetails()).isEqualTo("Invite declined as could not attend");
    }
}
