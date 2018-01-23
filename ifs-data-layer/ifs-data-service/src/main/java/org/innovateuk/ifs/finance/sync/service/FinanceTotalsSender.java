package org.innovateuk.ifs.finance.sync.service;

import org.innovateuk.ifs.commons.security.SecuredBySpring;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FinanceTotalsSender {
    @PreAuthorize("hasPermission(#applicationId, 'READ')")
    ServiceResult<Void> sendFinanceTotalsForApplication(@P("applicationId") Long applicationId);

    @PreAuthorize("hasAnyAuthority('system_registrar')")
    @SecuredBySpring(value = "SEND_APPLICATION_TOTALS",
            description = "Only the system registrar can send all the totals.")
    ServiceResult<Void> sendFinanceTotalsForCompetition(Long competitionId);

    @PreAuthorize("hasAnyAuthority('system_registrar')")
    @SecuredBySpring(value = "SEND_APPLICATION_TOTALS",
            description = "Only the system registrar can send all the totals.")
    ServiceResult<Void> sendAllFinanceTotals();
}
