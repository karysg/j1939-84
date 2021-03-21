/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part08;

import static org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket.Response.ACK;
import static org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket.Response.NACK;
import static org.etools.j1939_84.bus.j1939.packets.DM22IndividualClearPacket.ControlByte.CLR_ACT_ACK;
import static org.etools.j1939_84.bus.j1939.packets.DM22IndividualClearPacket.ControlByte.CLR_ACT_NACK;
import static org.etools.j1939_84.bus.j1939.packets.DM22IndividualClearPacket.ControlByte.CLR_ACT_REQ;
import static org.etools.j1939_84.bus.j1939.packets.DM22IndividualClearPacket.ControlByte.CLR_PA_ACK;
import static org.etools.j1939_84.bus.j1939.packets.DM22IndividualClearPacket.ControlByte.CLR_PA_NACK;
import static org.etools.j1939_84.bus.j1939.packets.DM22IndividualClearPacket.ControlByte.CLR_PA_REQ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.etools.j1939_84.bus.j1939.Lookup;
import org.etools.j1939_84.bus.j1939.packets.DM12MILOnEmissionDTCPacket;
import org.etools.j1939_84.bus.j1939.packets.DiagnosticTroubleCode;
import org.etools.j1939_84.bus.j1939.packets.ParsedPacket;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.SectionA5Verifier;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.DiagnosticMessageModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.VehicleInformationModule;

/**
 * 6.8.12 DM22: Individual Clear/Reset of Active and Previously Active DTC
 */
public class Part08Step12Controller extends StepController {
    private static final int PART_NUMBER = 8;
    private static final int STEP_NUMBER = 12;
    private static final int TOTAL_STEPS = 0;

    private final SectionA5Verifier verifier;

    Part08Step12Controller() {
        this(Executors.newSingleThreadScheduledExecutor(),
             new BannerModule(),
             DateTimeModule.getInstance(),
             DataRepository.getInstance(),
             new EngineSpeedModule(),
             new VehicleInformationModule(),
             new DiagnosticMessageModule(),
             new SectionA5Verifier(PART_NUMBER, STEP_NUMBER));
    }

    Part08Step12Controller(Executor executor,
                           BannerModule bannerModule,
                           DateTimeModule dateTimeModule,
                           DataRepository dataRepository,
                           EngineSpeedModule engineSpeedModule,
                           VehicleInformationModule vehicleInformationModule,
                           DiagnosticMessageModule diagnosticMessageModule,
                           SectionA5Verifier verifier) {
        super(executor,
              bannerModule,
              dateTimeModule,
              dataRepository,
              engineSpeedModule,
              vehicleInformationModule,
              diagnosticMessageModule,
              PART_NUMBER,
              STEP_NUMBER,
              TOTAL_STEPS);
        this.verifier = verifier;
    }

    @Override
    protected void run() throws Throwable {
        verifier.setJ1939(getJ1939());

        // 6.8.12.1.a DS DM22 (PGN 49920) to OBD ECU(s) without a DM12 MIL on DTC stored
        // using the MIL On DTC SPN and FMI and control byte = 17, Request to Clear/Reset Active DTC.
        List<Integer> addresses = getDataRepository().getObdModuleAddresses()
                                                     .stream()
                                                     .filter(a -> getDTCs(a).isEmpty())
                                                     .collect(Collectors.toList());

        var dsResults = addresses.stream()
                                 .map(a -> getDiagnosticMessageModule().requestDM22(getListener(),
                                                                                    a,
                                                                                    CLR_ACT_REQ,
                                                                                    0x7FFFF,
                                                                                    0x31))
                                 .collect(Collectors.toList());

        var packets = filterPackets(dsResults);
        var acks = filterAcks(dsResults);

        // 6.8.12.2.a. Fail if the ECU provides CLR_PA_ACK (as described in SAE J1939-73 paragraph 5.7.22).
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_PA_ACK)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.2.a - " + moduleName + " provided CLR_PA_ACK");
               });

        // 6.8.12.2.a. Fail if the ECU provides CLR_ACT_ACK (as described in SAE J1939-73 paragraph 5.7.22).
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_ACT_ACK)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.2.a - " + moduleName + " provided CLR_ACT_ACK");
               });

        // 6.8.12.2.b. Fail if the ECU provides J1939-21 ACK for PGN 49920.
        acks.stream()
            .filter(p -> p.getResponse() == ACK)
            .map(ParsedPacket::getModuleName)
            .forEach(moduleName -> {
                addFailure("6.8.12.2.b - " + moduleName
                        + " provided J1939-21 ACK for PGN 49920");
            });

        // 6.8.12.2.c. Fail if the ECU provides CLR_ACT_NACK with an acknowledgement code greater than 0.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_ACT_NACK && p.getAcknowledgementCode() > 0)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.2.c - " + moduleName
                           + " provided CLR_ACT_NACK with an acknowledgement code greater than 0");
               });

        // 6.8.12.2.c. Fail if the ECU provides CLR_PA_NACK with an acknowledgement code greater than 0.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_PA_NACK && p.getAcknowledgementCode() > 0)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.2.c - " + moduleName
                           + " provided CLR_PA_NACK with an acknowledgement code greater than 0");
               });

        // 6.8.12.3.a. Info: if DM22 (PGN 49920) [CLR]_PA_NACK or [CLR]_ACT_NACK is not received with an acknowledgement
        // code of 0.
        for (int address : addresses) {
            boolean found = packets.stream()
                                   .filter(p -> p.getSourceAddress() == address)
                                   .anyMatch(p -> (p.getControlByte() == CLR_PA_NACK
                                           || p.getControlByte() == CLR_ACT_NACK) && p.getAcknowledgementCode() == 0);
            if (!found) {
                addInfo("6.8.12.3.a - " + Lookup.getAddressName(address)
                        + " did not provide DM22 CLR_PA_NACK or CLR_ACT_NACK with acknowledgement code of 0");
            }
        }

        // 6.8.12.3.b. Info: if J1939-21 NACK for PGN 49920 is received.
        acks.stream()
            .filter(p -> p.getResponse() == NACK)
            .map(ParsedPacket::getModuleName)
            .forEach(moduleName -> {
                addInfo("6.8.12.3.b - " + moduleName + " provided J1939-21 NACK for PGN 49920");
            });

        // 6.8.12.4.a. DS DM22 to OBD ECU with a DM12 MIL on DTC stored using the DM12 MIL On DTC SPN and FMI and
        // control byte = 1, Request to Clear/Reset Previously Active DTC.
        addresses = getDataRepository().getObdModuleAddresses()
                                       .stream()
                                       .filter(a -> !getDTCs(a).isEmpty())
                                       .collect(Collectors.toList());

        dsResults = new ArrayList<>();
        for (int address : addresses) {
            for (DiagnosticTroubleCode dtc : getDTCs(address)) {
                dsResults.add(getDiagnosticMessageModule().requestDM22(getListener(),
                                                                       address,
                                                                       CLR_PA_REQ,
                                                                       dtc.getSuspectParameterNumber(),
                                                                       dtc.getFailureModeIndicator()));
            }
        }

        packets = filterPackets(dsResults);
        acks = filterAcks(dsResults);

        // 6.8.12.5.a. Fail if the ECU provides DM22 with CLR_PA_ACK .
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_PA_ACK)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.5.a - " + moduleName + " provided CLR_PA_ACK");
               });

        // 6.8.12.5.a. Fail if the ECU provides DM22 with CLR_ACT_ACK.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_ACT_ACK)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.5.a - " + moduleName + " provided CLR_ACT_ACK");
               });

        // 6.8.12.5.b. Fail if the ECU provides J1939-21 ACK for PGN 49920.
        acks.stream()
            .filter(p -> p.getResponse() == ACK)
            .map(ParsedPacket::getModuleName)
            .forEach(moduleName -> {
                addFailure("6.8.12.5.b - " + moduleName + " provided J1939-21 ACK for PGN 49920");
            });

        // 6.8.12.5.c. Fail if the ECU provides CLR_ACT_NACK with an acknowledgement code greater than 0.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_ACT_NACK && p.getAcknowledgementCode() > 0)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.5.c - " + moduleName
                           + " provided CLR_ACT_NACK with an acknowledgement code greater than 0");
               });

        // 6.8.12.6.a. Warn if DM22 (PGN 49920) [CLR]_PA_NACK or [CLR]_ACT_NACK is not received with an acknowledgement
        // code of 0.
        for (int address : addresses) {
            boolean found = packets.stream()
                                   .filter(p -> p.getSourceAddress() == address)
                                   .anyMatch(p -> (p.getControlByte() == CLR_PA_NACK
                                           || p.getControlByte() == CLR_ACT_NACK) && p.getAcknowledgementCode() == 0);
            if (!found) {
                addWarning("6.8.12.6.a - " + Lookup.getAddressName(address)
                        + " did not provide DM22 CLR_PA_NACK or CLR_ACT_NACK with acknowledgement code of 0");
            }
        }

        // 6.8.12.6.b. Warn if J1939-21 NACK for PGN 49920 is received.
        acks.stream()
            .filter(p -> p.getResponse() == NACK)
            .map(ParsedPacket::getModuleName)
            .forEach(moduleName -> {
                addWarning("6.8.12.6.b - " + moduleName + " provided J1939-21 NACK for PGN 49920");
            });

        // 6.8.12.7.a. Global DM22 using DM12 MIL On DTC SPN and FMI with control byte = 1, Request to Clear/Reset
        // Previously Active DTC.
        var globalResults = getDiagnosticMessageModule().requestDM22(getListener(), CLR_PA_REQ, 0x7FFFF, 0x31);
        packets = globalResults.getPackets();
        acks = globalResults.getAcks();

        // 6.8.12.8.a. Fail if any ECU provides DM22 with CLR_PA_ACK.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_PA_ACK)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.8.a - " + moduleName + " provided DM22 with CLR_PA_ACK");
               });

        // 6.8.12.8.a. Fail if any ECU provides DM22 with CLR_ACT_ACK.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_ACT_ACK)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.8.a - " + moduleName + " provided DM22 with CLR_ACT_ACK");
               });

        // 6.8.12.8.b. Fail if any ECU provides J1939-21 ACK for PGN 49920.
        acks.stream()
            .filter(p -> p.getResponse() == ACK)
            .map(ParsedPacket::getModuleName)
            .forEach(moduleName -> {
                addFailure("6.8.12.8.b - " + moduleName + " provided J1939-21 ACK for PGN 49920");
            });

        // 6.8.12.8.c. Fail if any ECU provides CLR_ACT_NACK or CLR_PA_NACK with an acknowledgement code greater than 0.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_ACT_NACK && p.getAcknowledgementCode() > 0)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.8.c - " + moduleName
                           + " provided CLR_ACT_NACK with an acknowledgement code greater than 0");
               });

        // 6.8.12.8.c. Fail if any ECU provides CLR_PA_NACK with an acknowledgement code greater than 0.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_PA_NACK && p.getAcknowledgementCode() > 0)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.8.c - " + moduleName
                           + " provided CLR_PA_NACK with an acknowledgement code greater than 0");
               });

        // 6.8.12.9.a. Global DM22 using DM12 MIL On DTC SPN and FMI with control byte = 17, Request to Clear/Reset
        // Active DTC.
        globalResults = getDiagnosticMessageModule().requestDM22(getListener(), CLR_ACT_REQ, 0x7FFFF, 0x31);
        packets = globalResults.getPackets();
        acks = globalResults.getAcks();

        // 6.8.12.10.a. Fail if any ECU provides CLR_PA_ACK.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_PA_ACK)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.10.a - " + moduleName + " provided DM22 with CLR_PA_ACK");
               });

        // 6.8.12.10.a. Fail if any ECU provides CLR_ACT_ACK.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_ACT_ACK)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.10.a - " + moduleName + " provided DM22 with CLR_ACT_ACK");
               });

        // 6.8.12.10.b. Fail if any ECU provides J1939-21 ACK for PGN 49920.
        acks.stream()
            .filter(p -> p.getResponse() == ACK)
            .map(ParsedPacket::getModuleName)
            .forEach(moduleName -> {
                addFailure("6.8.12.10.b - " + moduleName + " provided J1939-21 ACK for PGN 49920");
            });

        // 6.8.12.10.c. Fail if any ECU provides CLR_ACT_NACK with an acknowledgement code greater than 0.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_ACT_NACK && p.getAcknowledgementCode() > 0)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.10.c - " + moduleName
                           + " provided CLR_ACT_NACK with an acknowledgement code greater than 0");
               });

        // 6.8.12.10.c. Fail if any ECU provides CLR_PA_NACK with an acknowledgement code greater than 0.
        packets.stream()
               .filter(p -> p.getControlByte() == CLR_PA_NACK && p.getAcknowledgementCode() > 0)
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.8.12.10.c - " + moduleName
                           + " provided CLR_PA_NACK with an acknowledgement code greater than 0");
               });

        // 6.8.12.10.d. Fail if any OBD ECU erases any diagnostic information. See Section A.5 for more information.
        verifier.verifyDataNotErased(getListener(), "6.8.12.10.d");
    }

    private List<DiagnosticTroubleCode> getDTCs(int address) {
        return getDTCs(DM12MILOnEmissionDTCPacket.class, address, 8);
    }

}
