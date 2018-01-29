package org.innovateuk.ifs.finance.transactional;

import org.innovateuk.ifs.commons.BaseIntegrationTest;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.finance.domain.CostTotal;
import org.innovateuk.ifs.finance.repository.CostTotalRepository;
import org.innovateuk.ifs.finance.resource.cost.FinanceRowType;
import org.innovateuk.ifs.finance.resource.sync.FinanceCostTotalResource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.innovateuk.ifs.finance.builder.CostTotalBuilder.newCostTotal;
import static org.innovateuk.ifs.finance.resource.cost.FinanceRowType.LABOUR;
import static org.innovateuk.ifs.finance.resource.cost.FinanceRowType.MATERIALS;

public class CostTotalServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CostTotalService costTotalService;

    @Autowired
    private CostTotalRepository costTotalRepository;

    @Before
    public void cleanRepository() {
        costTotalRepository.deleteAll();
    }

    @Test
    public void saveCostTotal() {
        FinanceCostTotalResource costTotalResource = newApplicationFinanceCostTotalResource(
                10L,
                LABOUR,
                BigDecimal.valueOf(5000L)
        );

        ServiceResult<Void> result = costTotalService.saveCostTotal(costTotalResource);

        assertThat(result.isSuccess()).isTrue();

        assertThat(costTotalRepository.count()).isOne();
        assertThat(costTotalRepository.findByFinanceId(costTotalResource.getFinanceId()))
                .isEqualToComparingOnlyGivenFields(
                        newCostTotal()
                                .withFinanceId(costTotalResource.getFinanceId())
                                .withName(costTotalResource.getName())
                                .withType(costTotalResource.getFinanceType())
                                .withTotal(costTotalResource.getTotal())
                                .build()
                );
    }

    @Test
    public void saveCostTotals() {
        Long financeId = 20L;
        FinanceCostTotalResource costTotalResource1 = newApplicationFinanceCostTotalResource(
                financeId,
                LABOUR,
                BigDecimal.valueOf(5000L)
        );
        FinanceCostTotalResource costTotalResource2 = newApplicationFinanceCostTotalResource(
                financeId,
                MATERIALS,
                BigDecimal.valueOf(2500L)
        );
        List<FinanceCostTotalResource> costTotalResources = asList(costTotalResource1, costTotalResource2);

        ServiceResult<Void> result = costTotalService.saveCostTotals(costTotalResources);

        assertThat(result.isSuccess()).isTrue();

        assertThat(costTotalRepository.count()).isEqualTo(2);
        assertThat(costTotalRepository.findAllByFinanceId(financeId))
                .usingElementComparatorIgnoringFields("id")
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(
                        newCostTotal()
                                .withFinanceId(financeId)
                                .withName(costTotalResource1.getName(), costTotalResource2.getName())
                                .withType(costTotalResource1.getFinanceType(), costTotalResource2.getFinanceType())
                                .withTotal(costTotalResource1.getTotal(), costTotalResource2.getTotal())
                                .buildArray(2, CostTotal.class)
                );
    }

    private FinanceCostTotalResource newApplicationFinanceCostTotalResource(
            Long financeId,
            FinanceRowType type,
            BigDecimal total
    ) {
        FinanceCostTotalResource costTotalResource = new FinanceCostTotalResource();
        costTotalResource.setFinanceId(financeId);
        costTotalResource.setName(type.getName());
        costTotalResource.setFinanceType("APPLICATION");
        costTotalResource.setTotal(total);
        return costTotalResource;
    }
}
