package com.arxyt.colonypathingedition.core.api;

public interface BuildingHospitalExtra {
    int getOnDutyCitizen(int citizenId);
    void setCitizenInactive();
    void citizenShouldWork();
    void citizenShouldNotWork();
    boolean checkCitizenOnDuty(int citizenId);
}
