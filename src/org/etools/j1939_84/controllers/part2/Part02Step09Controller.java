/*
 * Copyright 2021 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part2;

import static org.etools.j1939_84.bus.j1939.Lookup.getAddressName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.etools.j1939_84.bus.j1939.packets.AcknowledgmentPacket;
import org.etools.j1939_84.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import org.etools.j1939_84.bus.j1939.packets.ParsedPacket;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DTCModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.VehicleInformationModule;

/**
 * 6.2.9 DM21: Diagnostic readiness 2
 */
public class Part02Step09Controller extends StepController {

    private static final int PART_NUMBER = 2;
    private static final int STEP_NUMBER = 9;
    private static final int TOTAL_STEPS = 0;

    private final DataRepository dataRepository;
    private final DTCModule dtcModule;

    Part02Step09Controller(DataRepository dataRepository) {
        this(Executors.newSingleThreadScheduledExecutor(),
             new EngineSpeedModule(),
             new BannerModule(),
             new VehicleInformationModule(),
             dataRepository,
             DateTimeModule.getInstance(),
             new DTCModule());
    }

    Part02Step09Controller(Executor executor,
                           EngineSpeedModule engineSpeedModule,
                           BannerModule bannerModule,
                           VehicleInformationModule vehicleInformationModule,
                           DataRepository dataRepository,
                           DateTimeModule dateTimeModule,
                           DTCModule dtcModule) {
        super(executor,
              engineSpeedModule,
              bannerModule,
              vehicleInformationModule,
              dateTimeModule,
              PART_NUMBER,
              STEP_NUMBER,
              TOTAL_STEPS);
        this.dataRepository = dataRepository;
        this.dtcModule = dtcModule;
    }

    @Override
    protected void run() throws Throwable {
        dtcModule.setJ1939(getJ1939());

        // 6.2.9.1 a. Global DM21 (send Request (PGN 59904) for PGN 49408 (SPNs 3069, 3294-3296)).
        var globalResponse = dtcModule.requestDM21(getListener());
        var globalPackets = globalResponse.getPackets();

        // 6.2.9.2 a. Fail if any ECU reports > 0 distance SCC (SPN 3294).
        globalPackets.stream()
                .filter(p -> p.getKmSinceDTCsCleared() > 0)
                .forEach(p -> addFailure("6.2.9.2.a - " + getAddressName(p.getSourceAddress()) + " reported > 0 distance SCC (SPN 3294)"));

        // 6.2.9.2 b. Fail if no ECU reports time (SPN 3295) or distance (SPN 3069) with MIL on.
        boolean timeReported = globalPackets.stream()
                .map(DM21DiagnosticReadinessPacket::getMinutesWhileMILIsActivated)
                .anyMatch(v -> !ParsedPacket.isNotAvailable(v));

        boolean distanceReported = globalPackets.stream()
                .map(DM21DiagnosticReadinessPacket::getKmWhileMILIsActivated)
                .anyMatch(v -> !ParsedPacket.isNotAvailable(v));

        if (!timeReported && !distanceReported) {
            addFailure("6.2.9.2.b - No ECU reported time (SPN 3295) or distance (SPN 3069) with MIL on");
        }

        // 6.2.9.2 c. Fail if any ECU reports > 0 for time (if supported) or distance with MIL on.
        globalPackets.stream()
                .filter(p -> {
                    double value = p.getMinutesWhileMILIsActivated();
                    return value > 0 && value < 0xFF00;
                })
                .forEach(p -> addFailure("6.2.9.2.c - " + getAddressName(p.getSourceAddress()) + " reported > 0 time with MIL on"));

        globalPackets.stream()
                .filter(p -> {
                    double value = p.getKmWhileMILIsActivated();
                    return value > 0 && !ParsedPacket.isNotAvailable(value);
                })
                .forEach(p -> addFailure("6.2.9.2.c - " + getAddressName(p.getSourceAddress()) + " reported > 0 distance with MIL on"));

        // 6.2.9.2 d. Fail if any ECU reports zero time SCC (SPN 3296) (if supported).
        globalPackets.stream()
                .filter(p -> {
                    double timeScc = p.getMinutesSinceDTCsCleared();
                    return timeScc >= 0.0 && timeScc < 1.0;
                })
                .forEach(p -> addFailure("6.2.9.2.d - " + getAddressName(p.getSourceAddress()) + " reported zero time SCC (SPN 3296)"));

        // 6.2.9.2 e. Warn if no OBD ECU reports time (SPN 3296) for DM21.
        boolean timeNotSupported = globalPackets.stream()
                .filter(p -> dataRepository.isObdModule(p.getSourceAddress()))
                .map(DM21DiagnosticReadinessPacket::getMinutesSinceDTCsCleared)
                .allMatch(ParsedPacket::isNotAvailable);

        if (timeNotSupported) {
            addWarning("6.2.9.2.e - No OBD ECU reported time (SPN 3296) for DM21");
        }

        // 6.1.14.4.a. DS DM26 to each OBD ECU.
        List<Integer> obdModuleAddresses = dataRepository.getObdModuleAddresses();

        List<DM21DiagnosticReadinessPacket> dsPackets = new ArrayList<>();
        List<AcknowledgmentPacket> dsAcks = new ArrayList<>();

        obdModuleAddresses.forEach(address -> {
            var result = dtcModule.requestDM21(getListener(), address);
            dsPackets.addAll(result.getPackets());
            dsAcks.addAll(result.getAcks());
        });

        // 6.2.9.4.a. Fail if any difference compared to data received from global request.
        compareRequestPackets(globalResponse.getPackets(), dsPackets, "6.2.9.4.a");

        // 6.2.9.4.b. Fail if NACK not received from OBD ECUs that did not respond to global query.
        checkForNACKs(globalResponse.getPackets(), dsAcks, obdModuleAddresses, "6.2.9.4.b");
    }
}
