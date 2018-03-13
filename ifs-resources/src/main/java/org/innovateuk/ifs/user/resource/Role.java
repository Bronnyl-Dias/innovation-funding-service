package org.innovateuk.ifs.user.resource;

import org.innovateuk.ifs.util.enums.Identifiable;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Role defines database relations and a model to use client side and server side.
 */
public enum Role implements Identifiable {

    LEADAPPLICANT               ( 1, "leadapplicant",   "Lead Applicant",   "applicant/dashboard"),
    COLLABORATOR                ( 2, "collaborator",    "Collaborator",     "applicant/dashboard"),
    ASSESSOR                    ( 3, "assessor",        "Assessor",         "assessment/assessor/dashboard"),
    APPLICANT                   ( 4, "applicant",       "Applicant",        "applicant/dashboard"),

    COMP_ADMIN                  ( 5, "comp_admin",              "Competition Administrator",    "management/dashboard"),
    SYSTEM_REGISTRATION_USER    ( 6, "system_registrar",        "System Registration User"),
    SYSTEM_MAINTAINER           ( 7, "system_maintainer",       "System Maintainer"),
    PROJECT_FINANCE             ( 8, "project_finance",         "Project Finance",              "management/dashboard"),
    FINANCE_CONTACT             ( 9, "finance_contact",         "Finance Contact"),
    PARTNER                     (10, "partner",                 "Partner"),
    PROJECT_MANAGER             (11, "project_manager",         "Project Manager"),
    COMP_EXEC                   (12, "competition_executive",   "Portfolio Manager"),

    INNOVATION_LEAD             (13, "innovation_lead",     "Innovation Lead",      "management/dashboard"),
    IFS_ADMINISTRATOR           (14, "ifs_administrator",   "IFS Administrator",    "management/dashboard"),
    SUPPORT                     (15, "support",             "IFS Support User",     "management/dashboard"),

    PANEL_ASSESSOR              (16, "panel_assessor",              "Panel Assessor"),
    INTERVIEW_ASSESSOR          (17, "interview_assessor",          "Interview Assessor"),
    INTERVIEW_LEAD_APPLICANT    (18, "interview_lead_applicant",    "Interview Lead Applicant");

    final long id;
    final String name;
    final String displayName;
    final String dashboardUrl;

    Role(final long id, final String name, final String displayName) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.dashboardUrl = null;
    }

    Role(final long id, final String name, final String displayName, final String dashboardUrl) {
        assert dashboardUrl != null;

        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.dashboardUrl = dashboardUrl;
    }

    public static Role getByName(String name) {
        return Stream.of(values()).filter(role -> role.name.equals(name)).findFirst().get();
    }

    public static List<Role> getByNameIn(List<String> names) {
        return Stream.of(values()).filter(role -> names.contains(role.getName())).collect(toList());
    }

    public static Role getById (long id) {
        return Stream.of(values()).filter(role -> role.id == id).findFirst().get();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUrl() {
        return dashboardUrl;
    }

    public boolean isOfType(UserRoleType roleType) {
        return roleType.getName().equals(name);
    }

    public boolean isPartner() {
        return this == PARTNER;
    }

    public boolean isLeadApplicant() {
        return this == LEADAPPLICANT;
    }

    public boolean isCollaborator() {
        return this == COLLABORATOR;
    }

    public boolean isProjectManager() {
        return this == PROJECT_MANAGER;
    }
}