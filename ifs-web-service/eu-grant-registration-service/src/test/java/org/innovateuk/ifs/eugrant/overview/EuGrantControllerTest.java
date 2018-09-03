package org.innovateuk.ifs.eugrant.overview;

import org.innovateuk.ifs.BaseControllerMockMVCTest;
import org.innovateuk.ifs.eugrant.overview.controller.EuGrantController;
import org.junit.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

public class EuGrantControllerTest extends BaseControllerMockMVCTest<EuGrantController> {

    @Override
    protected EuGrantController supplyControllerUnderTest() {
        return new EuGrantController();
    }

    @Test
    public void viewOverview() throws Exception {

        mockMvc.perform(get("/overview"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("eugrant/overview"));
    }
}
