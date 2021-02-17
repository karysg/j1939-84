/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part07;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.DiagnosticMessageModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.VehicleInformationModule;

/**
 * 6.7.4 DM12: Emissions Related Active DTCs
 */
public class Part07Step04Controller extends StepController {
    private static final int PART_NUMBER = 7;
    private static final int STEP_NUMBER = 4;
    private static final int TOTAL_STEPS = 0;

    Part07Step04Controller() {
        this(Executors.newSingleThreadScheduledExecutor(),
             new BannerModule(),
             DateTimeModule.getInstance(),
             DataRepository.getInstance(),
             new EngineSpeedModule(),
             new VehicleInformationModule(),
             new DiagnosticMessageModule());
    }

    Part07Step04Controller(Executor executor,
                           BannerModule bannerModule,
                           DateTimeModule dateTimeModule,
                           DataRepository dataRepository,
                           EngineSpeedModule engineSpeedModule,
                           VehicleInformationModule vehicleInformationModule,
                           DiagnosticMessageModule diagnosticMessageModule) {
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
    }

    @Override
    protected void run() throws Throwable {
        // 6.7.4.1.a DS DM12 [(send Request (PGNPG 59904) for PGNPG 65236 (SPNSPs 1213-1215, 1706, and 3038)]) to each OBD ECU.
        // 6.7.4.2.a Fail if any OBD ECU reports an active DTC.
        // 6.7.4.2.b Fail if any OBD ECU does not report MIL off.
        // 6.7.4.2.c Fail if no OBD ECU supports DM12.
        // 6.7.4.1.d Fail if NACK not received from OBD ECUs that did not provide a DM12 message.
    }

}