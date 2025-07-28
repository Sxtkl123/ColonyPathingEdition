package com.arxyt.colonypathingedition.core.api;

import java.util.List;

public interface BuildingCookExtra {

    List<Integer> getCustomers(int maxCount);

    void deleteCustomer(int customerId);

    void tryRegisterCustomer(int citizenId);

    boolean checkCustomerRegistry(int citizenId);

    int checkSize();
}
