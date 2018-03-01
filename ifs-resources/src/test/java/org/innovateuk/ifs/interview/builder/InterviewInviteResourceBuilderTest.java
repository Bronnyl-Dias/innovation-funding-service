package org.innovateuk.ifs.interview.builder;

import org.innovateuk.ifs.invite.resource.CompetitionInviteResource;
import org.innovateuk.ifs.invite.resource.InterviewInviteResource;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.innovateuk.ifs.interview.builder.InterviewInviteResourceBuilder.newInterviewInviteResource;
import static org.junit.Assert.assertEquals;

/**
 * Builder for {@link CompetitionInviteResource}
 */
public class InterviewInviteResourceBuilderTest {
    @Test
    public void buildOne() {
        String expectedCompetitionName = "Juggling craziness";
        long expectedCompId = 2L;
        String expectedEmail = "tom@poly.io";
        String expectedHash = "inviteHash";
        long expectedUserId = 1L;
        ZonedDateTime expectedInterviewDate = ZonedDateTime.now();

        InterviewInviteResource invite = newInterviewInviteResource()
                .withCompetitionName(expectedCompetitionName)
                .withCompetitionId(expectedCompId)
                .withEmail(expectedEmail)
                .withInviteHash(expectedHash)
                .withInterviewDate(expectedInterviewDate)
                .withUserId(expectedUserId)
                .build();

        assertEquals(expectedCompetitionName, invite.getCompetitionName());
        assertEquals(expectedCompId, invite.getCompetitionId());
        assertEquals(expectedEmail, invite.getEmail());
        assertEquals(expectedHash, invite.getHash());
        assertEquals(expectedInterviewDate, invite.getInterviewDate());
        assertEquals(expectedUserId, invite.getUserId());
    }

    @Test
    public void buildMany() {
        String[] expectedCompetitionNames = {"Juggling craziness", "Intermediate Juggling"};
        long[] expectedCompIds = {1L, 2L};
        String[] expectedEmails = {"tom@poly.io", "steve.smith@empire.com"};
        String[] expectedHashes = {"hash1", "hash2"};
        ZonedDateTime[] expectedInterviewDates = {ZonedDateTime.now(), ZonedDateTime.now().plusHours(1)};
        long[] expectedUserIds = {3L, 4L};

        List<InterviewInviteResource> invites = newInterviewInviteResource()
                .withCompetitionName(expectedCompetitionNames)
                .withCompetitionId(1L, 2L)
                .withEmail(expectedEmails)
                .withInviteHash(expectedHashes)
                .withInterviewDate(expectedInterviewDates)
                .withUserId(3L, 4L)
                .build(2);

        InterviewInviteResource first = invites.get(0);
        assertEquals(expectedCompetitionNames[0], first.getCompetitionName());
        assertEquals(expectedCompIds[0], first.getCompetitionId());
        assertEquals(expectedEmails[0], first.getEmail());
        assertEquals(expectedHashes[0], first.getHash());
        assertEquals(expectedInterviewDates[0], first.getInterviewDate());
        assertEquals(expectedUserIds[0], first.getUserId());

        InterviewInviteResource second = invites.get(1);
        assertEquals(expectedCompetitionNames[1], second.getCompetitionName());
        assertEquals(expectedCompIds[1], second.getCompetitionId());
        assertEquals(expectedEmails[1], second.getEmail());
        assertEquals(expectedHashes[1], second.getHash());
        assertEquals(expectedInterviewDates[1], second.getInterviewDate());
        assertEquals(expectedUserIds[1], second.getUserId());
    }
}
