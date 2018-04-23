package org.innovateuk.ifs.project.security;

import org.innovateuk.ifs.BasePermissionRulesTest;
import org.innovateuk.ifs.application.resource.ApplicationResource;
import org.innovateuk.ifs.project.domain.Project;
import org.innovateuk.ifs.user.resource.UserResource;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.innovateuk.ifs.application.builder.ApplicationResourceBuilder.newApplicationResource;
import static org.innovateuk.ifs.invite.domain.ProjectParticipantRole.PROJECT_PARTNER;
import static org.innovateuk.ifs.project.builder.ProjectBuilder.newProject;
import static org.innovateuk.ifs.project.builder.ProjectUserBuilder.newProjectUser;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectApplicationPermissionRulesTest extends BasePermissionRulesTest<ProjectApplicationPermissionRules> {

    @Override
    protected ProjectApplicationPermissionRules supplyPermissionRulesUnderTest() {
        return new ProjectApplicationPermissionRules();
    }

    @Test
    public void testProjectPartnerCanViewApplicationsLinkedToTheirProjects() {

        UserResource user = newUserResource().build();
        ApplicationResource application = newApplicationResource().build();
        Project linkedProject = newProject().build();

        when(projectRepositoryMock.findOneByApplicationId(application.getId())).thenReturn(linkedProject);
        when(projectUserRepositoryMock.findByProjectIdAndUserIdAndRole(linkedProject.getId(), user.getId(), PROJECT_PARTNER)).
                thenReturn(newProjectUser().build(1));

        assertTrue(rules.projectPartnerCanViewApplicationsLinkedToTheirProjects(application, user));

        verify(projectRepositoryMock).findOneByApplicationId(application.getId());
        verify(projectUserRepositoryMock).findByProjectIdAndUserIdAndRole(linkedProject.getId(), user.getId(), PROJECT_PARTNER);
    }

    @Test
    public void testProjectPartnerCanViewApplicationsLinkedToTheirProjectsButNoProjectForApplication() {

        UserResource user = newUserResource().build();
        ApplicationResource application = newApplicationResource().build();
        Project linkedProject = newProject().build();

        when(projectRepositoryMock.findOneByApplicationId(application.getId())).thenReturn(null);

        assertFalse(rules.projectPartnerCanViewApplicationsLinkedToTheirProjects(application, user));

        verify(projectRepositoryMock).findOneByApplicationId(application.getId());
        verify(projectUserRepositoryMock, never()).findByProjectIdAndUserIdAndRole(linkedProject.getId(), user.getId(), PROJECT_PARTNER);
    }

    @Test
    public void testProjectPartnerCanViewApplicationsLinkedToTheirProjectsButNotPartnerOnLinkedProject() {

        UserResource user = newUserResource().build();
        ApplicationResource application = newApplicationResource().build();
        Project linkedProject = newProject().build();

        when(projectRepositoryMock.findOneByApplicationId(application.getId())).thenReturn(linkedProject);
        when(projectUserRepositoryMock.findByProjectIdAndUserIdAndRole(linkedProject.getId(), user.getId(), PROJECT_PARTNER)).
                thenReturn(emptyList());

        assertFalse(rules.projectPartnerCanViewApplicationsLinkedToTheirProjects(application, user));

        verify(projectRepositoryMock).findOneByApplicationId(application.getId());
        verify(projectUserRepositoryMock).findByProjectIdAndUserIdAndRole(linkedProject.getId(), user.getId(), PROJECT_PARTNER);
    }


}
