package org.innovateuk.ifs.user.transactional;

import org.innovateuk.ifs.address.mapper.AddressMapper;
import org.innovateuk.ifs.address.resource.AddressResource;
import org.innovateuk.ifs.authentication.service.IdentityProviderService;
import org.innovateuk.ifs.authentication.validator.PasswordPolicyValidator;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.competition.domain.Competition;
import org.innovateuk.ifs.competition.domain.Stakeholder;
import org.innovateuk.ifs.competition.domain.StakeholderInvite;
import org.innovateuk.ifs.competition.repository.StakeholderInviteRepository;
import org.innovateuk.ifs.competition.repository.StakeholderRepository;
import org.innovateuk.ifs.competition.transactional.TermsAndConditionsService;
import org.innovateuk.ifs.invite.domain.RoleInvite;
import org.innovateuk.ifs.invite.repository.RoleInviteRepository;
import org.innovateuk.ifs.profile.domain.Profile;
import org.innovateuk.ifs.profile.repository.ProfileRepository;
import org.innovateuk.ifs.registration.resource.InternalUserRegistrationResource;
import org.innovateuk.ifs.registration.resource.StakeholderRegistrationResource;
import org.innovateuk.ifs.registration.resource.UserRegistrationResource;
import org.innovateuk.ifs.transactional.BaseTransactionalService;
import org.innovateuk.ifs.user.domain.User;
import org.innovateuk.ifs.user.mapper.UserMapper;
import org.innovateuk.ifs.user.resource.Role;
import org.innovateuk.ifs.user.resource.UserResource;
import org.innovateuk.ifs.user.resource.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.innovateuk.ifs.commons.error.CommonErrors.notFoundError;
import static org.innovateuk.ifs.commons.error.CommonFailureKeys.NOT_AN_INTERNAL_USER_ROLE;
import static org.innovateuk.ifs.commons.error.Error.fieldError;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceFailure;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.user.resource.Role.APPLICANT;
import static org.innovateuk.ifs.user.resource.Role.IFS_ADMINISTRATOR;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleMap;
import static org.innovateuk.ifs.util.EntityLookupCallbacks.find;

/**
 * A service around Registration and general user-creation operations
 */
@Service
public class RegistrationServiceImpl extends BaseTransactionalService implements RegistrationService {

    @Autowired
    private IdentityProviderService idpService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private PasswordPolicyValidator passwordPolicyValidator;

    @Autowired
    private UserSurveyService userSurveyService;

    @Autowired
    private RoleInviteRepository roleInviteRepository;

    @Autowired
    private TermsAndConditionsService termsAndConditionsService;

    @Autowired
    private RegistrationNotificationService registrationEmailService;

    @Autowired
    private StakeholderInviteRepository stakeholderInviteRepository;

    @Autowired
    private StakeholderRepository stakeholderRepository;

    @Override
    @Transactional
    public ServiceResult<UserResource> createUser(UserRegistrationResource userRegistrationResource) {
        final UserResource userResource = userRegistrationResource.toUserResource();

        return validateUser(userResource).
                andOnSuccess(validUser -> {
                    final User user = userMapper.mapToDomain(userResource);
                    return createUserWithUid(user, userResource.getPassword(), userRegistrationResource.getAddress());
                });
    }

    @Override
    @Transactional
    public ServiceResult<UserResource> createUser(UserResource userResource) {
        return createOrganisationUser(Optional.empty(), Optional.empty(), userResource);
    }

    @Override
    @Transactional
    public ServiceResult<UserResource> createUserWithCompetitionContext(long competitionId, long organisationId, UserResource userResource) {
        return createOrganisationUser(Optional.of(competitionId), Optional.of(organisationId), userResource);
    }

    private ServiceResult<UserResource> createOrganisationUser(Optional<Long> competitionId, Optional<Long> organisationId,  UserResource userResource) {
        return validateUser(userResource).
                andOnSuccessReturn(validUser -> assembleUserFromResource(validUser)).
                andOnSuccess(newUser -> addApplicantRoleToUserIfNoRolesAssigned(userResource, newUser)).
                andOnSuccess(newUserWithRole -> markLatestSiteTermsAndConditionsAgreedToIfApplicant(newUserWithRole)).
                andOnSuccess(newUserWithRole -> createUserWithUid(newUserWithRole, userResource.getPassword(), null)).
                andOnSuccess(createdUser -> sendUserVerificationEmail(competitionId, organisationId, createdUser));
    }

    private ServiceResult<UserResource> validateUser(UserResource userResource) {
        return passwordPolicyValidator.validatePassword(userResource.getPassword(), userResource)
                .handleSuccessOrFailure(
                        failure -> serviceFailure(
                                simpleMap(
                                        failure.getErrors(),
                                        error -> fieldError("password", error.getFieldRejectedValue(), error.getErrorKey())
                                )
                        ),
                        success -> serviceSuccess(userResource)
                );
    }

    private User assembleUserFromResource(UserResource userResource) {
        User newUser = new User();
        newUser.setFirstName(userResource.getFirstName());
        newUser.setLastName(userResource.getLastName());
        newUser.setEmail(userResource.getEmail());
        newUser.setTitle(userResource.getTitle());
        newUser.setPhoneNumber(userResource.getPhoneNumber());
        newUser.setAllowMarketingEmails(userResource.getAllowMarketingEmails());
        newUser.setRoles(new HashSet<>(userResource.getRoles()));

        return newUser;
    }

    private ServiceResult<User> addApplicantRoleToUserIfNoRolesAssigned(UserResource userResource, User user) {
        return userResource.getRoles().isEmpty() ? addRoleToUser(user, APPLICANT) : serviceSuccess(user);
    }

    private ServiceResult<User> markLatestSiteTermsAndConditionsAgreedToIfApplicant(User userWithRole) {
        return userWithRole.hasRole(Role.APPLICANT) ?
                agreeLatestSiteTermsAndConditionsForUser(userWithRole) : serviceSuccess(userWithRole);
    }

    private ServiceResult<UserResource> createUserWithUid(User user, String password, AddressResource addressResource) {

        ServiceResult<String> uidFromIdpResult = idpService.createUserRecordWithUid(user.getEmail(), password);

        return uidFromIdpResult.andOnSuccessReturn(uidFromIdp -> {
            user.setUid(uidFromIdp);
            user.setStatus(UserStatus.INACTIVE);
            Profile profile = new Profile();
            if (addressResource != null) profile.setAddress(addressMapper.mapToDomain(addressResource));
            Profile savedProfile = profileRepository.save(profile);
            user.setProfileId(savedProfile.getId());
            User savedUser = userRepository.save(user);

            return userMapper.mapToResource(savedUser);
        });
    }

    private ServiceResult<UserResource> sendUserVerificationEmail(Optional<Long> competitionId, Optional<Long> organisationId, UserResource createdUser) {

        return registrationEmailService.sendUserVerificationEmail(createdUser, competitionId, organisationId).
                andOnSuccessReturn(() -> createdUser);
    }

    @Override
    @Transactional
    public ServiceResult<Void> activateUser(long userId) {
        return getUser(userId).andOnSuccessReturnVoid(this::activateUser);
    }

    private ServiceResult<User> activateUser(User user) {
        return idpService
                .activateUser(user.getUid())
                .andOnSuccessReturn(() -> {
                    user.setStatus(UserStatus.ACTIVE);
                    return userRepository.save(user);
                });
    }

    @Override
    @Transactional
    public ServiceResult<Void> deactivateUser(long userId) {
        return getUser(userId).andOnSuccessReturnVoid(this::deactivateUser);
    }

    private ServiceResult<User> deactivateUser(User user) {
        return idpService
                .deactivateUser(user.getUid())
                .andOnSuccessReturn(() -> {
                    user.setStatus(UserStatus.INACTIVE);
                    return userRepository.save(user);
                });
    }

    @Override
    @Transactional
    public ServiceResult<Void> activateApplicantAndSendDiversitySurvey(long userId) {
        return getUser(userId)
                .andOnSuccess(this::activateUser)
                .andOnSuccessReturnVoid(this::sendApplicantDiversitySurvey);
    }

    @Override
    @Transactional
    public ServiceResult<Void> activateAssessorAndSendDiversitySurvey(long userId) {
        return getUser(userId)
                .andOnSuccess(this::activateUser)
                .andOnSuccessReturnVoid(this::sendAssessorDiversitySurvey);
    }

    private ServiceResult<Void> sendApplicantDiversitySurvey(User user) {
        return userSurveyService.sendApplicantDiversitySurvey(user);
    }

    private ServiceResult<Void> sendAssessorDiversitySurvey(User user) {
        return userSurveyService.sendAssessorDiversitySurvey(user);
    }

    private ServiceResult<User> addRoleToUser(User user, Role role) {
        if (!user.hasRole(role)) {
            user.addRole(role);
        }
        return serviceSuccess(user);
    }

    private ServiceResult<User> agreeLatestSiteTermsAndConditionsForUser(User user) {
        return termsAndConditionsService.getLatestSiteTermsAndConditions().andOnSuccessReturn(termsAndConditions -> {
            if (user.getTermsAndConditionsIds() == null) {
                user.setTermsAndConditionsIds(new LinkedHashSet<>());
            }
            user.getTermsAndConditionsIds().add(termsAndConditions.getId());
            return user;
        });
    }

    @Override
    @Transactional
    public ServiceResult<Void> resendUserVerificationEmail(final UserResource user) {
        return registrationEmailService.resendUserVerificationEmail(user);
    }

    @Override
    @Transactional
    public ServiceResult<Void> createInternalUser(String inviteHash, InternalUserRegistrationResource internalUserRegistrationResource) {
        return getByHash(inviteHash).andOnSuccess(roleInvite ->
                getInternalRoleResources(roleInvite.getTarget()).andOnSuccess(roleResource -> {
                    internalUserRegistrationResource.setEmail(roleInvite.getEmail());
                    internalUserRegistrationResource.setRoles(roleResource);
                    return createUser(internalUserRegistrationResource)
                            .andOnSuccess(() -> updateInviteStatus(roleInvite))
                            .andOnSuccessReturnVoid();
                }));
    }

    @Override
    @Transactional
    public ServiceResult<Void> createStakeholder(String hash, StakeholderRegistrationResource stakeholderRegistrationResource) {
        return getStakeholderInviteByHash(hash)
                .andOnSuccess(stakeholderInvite -> createStakeholderUser(stakeholderRegistrationResource, stakeholderInvite)
                        .andOnSuccess(user -> associateUserWithCompetition(stakeholderInvite.getTarget(), user))
                        .andOnSuccess(() -> updateStakeholderInviteStatus(stakeholderInvite))
                        .andOnSuccessReturnVoid());
    }

    private ServiceResult<Void> associateUserWithCompetition(Competition competition, User user) {
        stakeholderRepository.save(new Stakeholder(competition, user));
        return serviceSuccess();
    }

    private ServiceResult<List<Role>> getInternalRoleResources(Role role) {
        if (role == IFS_ADMINISTRATOR){
            return getIFSAdminRoles(role); // IFS Admin has multiple roles
        } else {
            return serviceSuccess(singletonList(role));
        }
    }

    private ServiceResult<Void> createUser(InternalUserRegistrationResource internalUserRegistrationResource) {
        final UserResource userResource = internalUserRegistrationResource.toUserResource();

        return validateUser(userResource).
                andOnSuccess(validUser -> {
                    final User user = userMapper.mapToDomain(userResource);
                    return createUserWithUid(user, userResource.getPassword()).
                            andOnSuccess(this::activateUser).andOnSuccessReturnVoid();
                });
    }

    private ServiceResult<User> createStakeholderUser(StakeholderRegistrationResource stakeholderRegistrationResource, StakeholderInvite stakeholderInvite) {
        final UserResource userResource = stakeholderRegistrationResource.toUserResource();
        userResource.setEmail(stakeholderInvite.getEmail());
        userResource.setRoles(singletonList(Role.STAKEHOLDER));
        return validateUser(userResource).
                andOnSuccess(validUser -> {
                    final User user = userMapper.mapToDomain(userResource);
                    return createUserWithUid(user, userResource.getPassword()).
                            andOnSuccess(this::activateUser)
                            .andOnSuccessReturn(() -> user);
                });
    }

    private ServiceResult<Void> updateInviteStatus(RoleInvite roleInvite) {
        roleInvite.open();
        roleInviteRepository.save(roleInvite);
        return serviceSuccess();
    }

    private ServiceResult<Void> updateStakeholderInviteStatus(StakeholderInvite stakeholderInvite) {
        stakeholderInvite.open();
        stakeholderInviteRepository.save(stakeholderInvite);
        return serviceSuccess();
    }

    private ServiceResult<List<Role>> getIFSAdminRoles(Role roleType) {
        return serviceSuccess( asList(roleType, Role.PROJECT_FINANCE) );
    }

    private ServiceResult<RoleInvite> getByHash(String hash) {
        return find(roleInviteRepository.getByHash(hash), notFoundError(RoleInvite.class, hash));
    }

    private ServiceResult<StakeholderInvite> getStakeholderInviteByHash(String hash) {
        return find(stakeholderInviteRepository.getByHash(hash), notFoundError(RoleInvite.class, hash));
    }

    private ServiceResult<User> createUserWithUid(User user, String password) {
        ServiceResult<String> uidFromIdpResult = idpService.createUserRecordWithUid(user.getEmail(), password);

        return uidFromIdpResult.andOnSuccess(uidFromIdp -> {
            user.setUid(uidFromIdp);
            Profile profile = new Profile();
            Profile savedProfile = profileRepository.save(profile);
            user.setProfileId(savedProfile.getId());
            User createdUser = userRepository.save(user);
            return serviceSuccess(createdUser);
        });
    }

    @Override
    @Transactional
    public ServiceResult<Void> editInternalUser(UserResource userToEdit, Role userRoleType) {

        return validateInternalUserRole(userRoleType)
                .andOnSuccess(() -> ServiceResult.getNonNullValue(userRepository.findOne(userToEdit.getId()), notFoundError(User.class)))
                .andOnSuccess(user -> getInternalRoleResources(userRoleType)
                    .andOnSuccess(roleResources -> {
                        Set<Role> roleList = new HashSet<>(roleResources);
                        user.setFirstName(userToEdit.getFirstName());
                        user.setLastName(userToEdit.getLastName());
                        user.setRoles(roleList);
                        userRepository.save(user);
                        return serviceSuccess();
                    })
                );
    }

    private ServiceResult<Void> validateInternalUserRole(Role userRoleType) {

        return Role.internalRoles().contains(userRoleType) ?
                serviceSuccess() : serviceFailure(NOT_AN_INTERNAL_USER_ROLE);
    }
}