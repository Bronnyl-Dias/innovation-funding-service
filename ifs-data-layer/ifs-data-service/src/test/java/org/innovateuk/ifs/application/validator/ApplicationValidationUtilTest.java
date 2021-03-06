package org.innovateuk.ifs.application.validator;

import org.innovateuk.ifs.BaseUnitTestMocksTest;
import org.innovateuk.ifs.application.validation.ApplicationValidationUtil;
import org.innovateuk.ifs.application.validation.ApplicationValidatorService;
import org.innovateuk.ifs.commons.error.ValidationMessages;
import org.innovateuk.ifs.finance.handler.item.MaterialsHandler;
import org.innovateuk.ifs.finance.resource.cost.Materials;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ApplicationValidationUtil}
 */

public class ApplicationValidationUtilTest extends BaseUnitTestMocksTest {

    @Mock
    private ApplicationValidatorService applicationValidatorServiceMock;

    @InjectMocks
    private ApplicationValidationUtil validationUtil;

    @Test
    public void validateProjectCostItem(){
        Materials material = new Materials();
        material.setCost(BigDecimal.valueOf(100));
        material.setItem("");
        material.setQuantity(5);

        when(applicationValidatorServiceMock.getProjectCostHandler(material)).thenReturn(new MaterialsHandler());

        ValidationMessages validationMessages = validationUtil.validateProjectCostItem(material);

        assertNotNull(validationMessages);
        assertEquals(validationMessages.getErrors().size(), 1);
        assertEquals(validationMessages.getFieldErrors("item").get(0).getErrorKey(), "validation.field.must.not.be.blank");
        assertEquals(validationMessages.getFieldErrors("item").get(0).getStatusCode(), HttpStatus.NOT_ACCEPTABLE);
    }
}
