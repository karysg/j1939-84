/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.model;

import static org.etools.j1939_84.J1939_84.NL;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.etools.j1939tools.bus.RequestResult;
import org.etools.j1939tools.j1939.model.FuelType;
import org.etools.j1939tools.j1939.packets.AddressClaimPacket;
import org.etools.j1939tools.j1939.packets.ComponentIdentificationPacket;
import org.etools.j1939tools.j1939.packets.DM19CalibrationInformationPacket;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The Vehicle Information which is required in Part 1.
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class VehicleInformation implements Cloneable {

    private RequestResult<AddressClaimPacket> addressClaim;

    private int calIds;

    private List<DM19CalibrationInformationPacket> calIdsFound = Collections.emptyList();

    private String certificationIntent;

    private int emissionUnits;

    private Map<Integer, Optional<ComponentIdentificationPacket>> emissionUnitsFound = new HashMap<>();

    private int engineModelYear;

    private FuelType fuelType;

    private boolean usCarb;

    private int numberOfTripsForFaultBImplant;

    private int numberOfFaultAImplants;

    private int vehicleModelYear;

    private String vin = "";

    public RequestResult<AddressClaimPacket> getAddressClaim() {
        if (addressClaim == null) {
            addressClaim = new RequestResult<>(false, Collections.emptyList());
        }
        return addressClaim;
    }

    public void setAddressClaim(RequestResult<AddressClaimPacket> addressClaim) {
        this.addressClaim = addressClaim;
    }

    public int getCalIds() {
        return calIds;
    }

    public void setCalIds(int calIds) {
        this.calIds = calIds;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Not a concern in desktop app.")
    public List<DM19CalibrationInformationPacket> getCalIdsFound() {
        return calIdsFound;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Not a concern in desktop app.")
    public void setCalIdsFound(List<DM19CalibrationInformationPacket> calIdsFound) {
        this.calIdsFound = calIdsFound;
    }

    public String getCertificationIntent() {
        return certificationIntent;
    }

    public void setCertificationIntent(String certificationIntent) {
        this.certificationIntent = certificationIntent;
    }

    public int getEmissionUnits() {
        return emissionUnits;
    }

    public void setEmissionUnits(int emissionUnits) {
        this.emissionUnits = emissionUnits;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Not a concern in desktop app.")
    public Map<Integer, Optional<ComponentIdentificationPacket>> getEmissionUnitsFound() {
        return emissionUnitsFound;
    }

    public void setEmissionUnitsFound(Map<Integer, Optional<ComponentIdentificationPacket>> emissionUnitsFound) {
        this.emissionUnitsFound.clear();
        this.emissionUnitsFound.putAll(emissionUnitsFound);
    }

    public int getEngineModelYear() {
        return engineModelYear;
    }

    public void setEngineModelYear(int engineModelYear) {
        this.engineModelYear = engineModelYear;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public int getNumberOfTripsForFaultBImplant() {
        return numberOfTripsForFaultBImplant;
    }

    public void setNumberOfTripsForFaultBImplant(int numberOfTripsForFaultBImplant) {
        this.numberOfTripsForFaultBImplant = numberOfTripsForFaultBImplant;
    }

    public int getNumberOfFaultAImplants() { return numberOfFaultAImplants; }

    public void setNumberOfFaultAImplants(int numberOfFaultAImplants){
        this.numberOfFaultAImplants = numberOfFaultAImplants;
    }

    public int getVehicleModelYear() {
        return vehicleModelYear;
    }

    public void setVehicleModelYear(int vehicleModelYear) {
        this.vehicleModelYear = vehicleModelYear;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        if (vin == null) {
            vin = "";
        }
        this.vin = vin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(calIds,
                            calIdsFound,
                            certificationIntent,
                            emissionUnits,
                            emissionUnitsFound,
                            engineModelYear,
                            fuelType,
                            vehicleModelYear,
                            vin,
                            numberOfTripsForFaultBImplant,
                            numberOfFaultAImplants);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof VehicleInformation)) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        VehicleInformation that = (VehicleInformation) obj;

        return Objects.equals(certificationIntent, that.certificationIntent)
                && calIds == that.calIds
                && Objects.equals(calIdsFound, that.calIdsFound)
                && emissionUnits == that.emissionUnits
                && Objects.equals(emissionUnitsFound, that.emissionUnitsFound)
                && engineModelYear == that.engineModelYear && fuelType == that.fuelType
                && vehicleModelYear == that.vehicleModelYear && Objects.equals(vin, that.vin)
                && numberOfTripsForFaultBImplant == that.numberOfTripsForFaultBImplant
                && numberOfFaultAImplants == that.numberOfFaultAImplants;
    }

    @SuppressFBWarnings(value = "CN_IDIOM_NO_SUPER_CALL", justification = "Calling super.clone() will cause a crash")
    @Override
    public VehicleInformation clone() {

        VehicleInformation vehInfo = new VehicleInformation();
        vehInfo.setAddressClaim(getAddressClaim());
        vehInfo.setCalIds(getCalIds());
        vehInfo.setCalIdsFound(getCalIdsFound());
        vehInfo.setCertificationIntent(getCertificationIntent());
        vehInfo.setEmissionUnits(getEmissionUnits());
        vehInfo.setEmissionUnitsFound(getEmissionUnitsFound());
        vehInfo.setEngineModelYear(getEngineModelYear());
        vehInfo.setFuelType(getFuelType());
        vehInfo.setNumberOfTripsForFaultBImplant(getNumberOfTripsForFaultBImplant());
        vehInfo.setNumberOfFaultAImplants(getNumberOfFaultAImplants());
        vehInfo.setVehicleModelYear(getVehicleModelYear());
        vehInfo.setVin(getVin());
        vehInfo.setUsCarb(isUsCarb());

        return vehInfo;
    }

    @Override
    public String toString() {
        return "User Data Entry: " + NL + NL
                + "Engine Model Emissions Year: " + engineModelYear
                + (isUsCarb() ? " US/CARB" : " not US/CARB") + NL
                + "Number of Emissions ECUs Expected: " + emissionUnits + NL
                + "Number of CAL IDs Expected: " + calIds + NL
                + "Fuel Type: " + fuelType + NL
                + "Ignition Type: " + fuelType.ignitionType.name + NL
                + "Number of Trips for Fault B Implant: " + numberOfTripsForFaultBImplant + NL
                + "Number of Fault A Implants: " + numberOfFaultAImplants + NL
                + NL
                + "Vehicle Information:" + NL
                + "VIN: " + vin + NL
                + "Vehicle MY: " + vehicleModelYear + NL
                + "Engine MY: " + engineModelYear + NL
                + "Cert. Engine Family: " + certificationIntent + NL
                + "Number of OBD ECUs Found: " + emissionUnitsFound.size() + NL
                + emissionUnitsFound.keySet().stream()
                                    .map(addr -> "     Address: " + addr +
                                            emissionUnitsFound.get(addr).map(cid -> " Make: " + cid.getMake() + ", Model: " + cid.getModel() + ", Serial: "
                                                + cid.getSerialNumber()).orElse(""))
                                    .collect(Collectors.joining(NL))
                + NL
                + "Number of CAL IDs Found: "
                + calIdsFound.stream().mapToLong(dm19 -> dm19.getCalibrationInformation().size()).sum() + NL
                + calIdsFound.stream()
                             .map(DM19CalibrationInformationPacket::toString)
                             .flatMap(String::lines)
                             .map(s -> "     " + s)
                             .collect(Collectors.joining(NL))
                + NL;
    }

    public boolean isUsCarb() {
        return usCarb;
    }

    public void setUsCarb(boolean usCarb) {
        this.usCarb = usCarb;
    }
}
