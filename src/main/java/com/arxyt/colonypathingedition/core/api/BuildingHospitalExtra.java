package com.arxyt.colonypathingedition.core.api;

public interface BuildingHospitalExtra {
    int getOnDutyCitizen(int citizenId);
    void setCitizenInactive();
    void citizenShouldWork();
    void citizenShouldNotWork();
    boolean IsThisOnDutyCitizenID(int citizenId);
    boolean checkCitizenOnDuty(int citizenId);
    boolean noHealerCuringPlayer(int workerID);
    void resetHealerCuringPlayer();
    boolean noHealerWandering(int workerID);
    void resetHealerWandering();
}
