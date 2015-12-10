package com.worth.ifs.organisation.controller;

import com.worth.ifs.organisation.resource.CompanyHouseBusiness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CompanyHouseController exposes CompanyHouse data and operations through a REST API.
 */
@RestController
@RequestMapping("/companyhouse")
public class CompanyHouseController {
    @Autowired
    CompanyHouseApi companyHouseService;

    @RequestMapping("/searchCompanyHouse/{name}")
     public List<CompanyHouseBusiness> searchCompanyHouse(@PathVariable("name") final String name) {
        List<CompanyHouseBusiness> companies = companyHouseService.searchOrganisationsByName(name);
        return companies;
    }

    @RequestMapping("/getCompanyHouse/{id}")
    public CompanyHouseBusiness getCompanyHouse(@PathVariable("id") final String id) {
        CompanyHouseBusiness company = companyHouseService.getOrganisationById(id);
        return company;
    }
}
