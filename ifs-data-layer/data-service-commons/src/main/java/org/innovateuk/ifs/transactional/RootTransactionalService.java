package org.innovateuk.ifs.transactional;

import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.user.domain.ProcessRole;
import org.innovateuk.ifs.user.resource.Role;
import org.innovateuk.ifs.user.domain.User;
import org.innovateuk.ifs.user.repository.ProcessRoleRepository;
import org.innovateuk.ifs.user.repository.UserRepository;
import org.innovateuk.ifs.user.resource.UserResource;
import org.innovateuk.ifs.user.resource.UserRoleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

import static org.innovateuk.ifs.commons.error.CommonErrors.forbiddenError;
import static org.innovateuk.ifs.commons.error.CommonErrors.notFoundError;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceFailure;
import static org.innovateuk.ifs.util.EntityLookupCallbacks.find;

/**
 * This class represents the base class for transactional services.  Method calls within this service will have
 * transaction boundaries provided to allow for safe atomic operations and persistence cascading.
 */
@Transactional(readOnly = true)
public abstract class RootTransactionalService {
    @Autowired
    protected ProcessRoleRepository processRoleRepository;

    @Autowired
    protected UserRepository userRepository;

    protected Supplier<ServiceResult<ProcessRole>> processRole(Long processRoleId) {
        return () -> getProcessRole(processRoleId);
    }

    protected ServiceResult<ProcessRole> getProcessRole(Long processRoleId) {
        return find(processRoleRepository.findOne(processRoleId), notFoundError(ProcessRole.class, processRoleId));
    }

    protected ServiceResult<List<ProcessRole>> getProcessRoles(Long applicationId, UserRoleType roleType) {
        return getRole(roleType).andOnSuccess(role -> find(processRoleRepository.findByApplicationIdAndRoleId(applicationId, role.getId()), notFoundError(ProcessRole.class, applicationId, role.getId())));
    }

    protected Supplier<ServiceResult<User>> user(final Long id) {
        return () -> getUser(id);
    }

    protected ServiceResult<User> getUser(final Long id) {
        return find(userRepository.findOne(id), notFoundError(User.class, id));
    }

    protected Supplier<ServiceResult<Role>> role(UserRoleType roleType) {
        return () -> getRole(roleType);
    }

    protected Supplier<ServiceResult<Role>> role(String roleName) {
        return () -> getRole(roleName);
    }

    protected ServiceResult<Role> getRole(UserRoleType roleType) {
        return getRole(roleType.getName());
    }

    protected ServiceResult<Role> getRole(String roleName) {
        return find(Role.getByName(roleName), notFoundError(Role.class, roleName));
    }


    protected ServiceResult<User> getCurrentlyLoggedInUser() {
        UserResource currentUser = (UserResource) SecurityContextHolder.getContext().getAuthentication().getDetails();

        if (currentUser == null) {
            return serviceFailure(forbiddenError());
        }

        return getUser(currentUser.getId());
    }

}
