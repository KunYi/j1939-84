/*
 * Copyright (c) 2021. Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part11;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.etools.j1939_84.bus.j1939.packets.DM26TripDiagnosticReadinessPacket;
import org.etools.j1939_84.bus.j1939.packets.ParsedPacket;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.DiagnosticMessageModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.VehicleInformationModule;

/**
 * 6.11.11 DM26: Diagnostic Readiness 3
 */
public class Part11Step11Controller extends StepController {
    private static final int PART_NUMBER = 11;
    private static final int STEP_NUMBER = 11;
    private static final int TOTAL_STEPS = 0;

    Part11Step11Controller() {
        this(Executors.newSingleThreadScheduledExecutor(),
             new BannerModule(),
             DateTimeModule.getInstance(),
             DataRepository.getInstance(),
             new EngineSpeedModule(),
             new VehicleInformationModule(),
             new DiagnosticMessageModule());
    }

    Part11Step11Controller(Executor executor,
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
        // 6.11.11.1.a. DS DM26 [(send Request (PGN 59904) for PGN 64952 (SPNs 3301-3305)]) to each OBD ECU.
        var dsResults = getDataRepository().getObdModuleAddresses()
                                           .stream()
                                           .map(a -> getDiagnosticMessageModule().requestDM26(getListener(), a))
                                           .collect(Collectors.toList());

        var packets = filterRequestResultPackets(dsResults);

        // 6.11.11.2.a. Fail if response indicates time since engine start (SPN 3301) differs by more than ±10 seconds
        // from expected value (calculated by software using original DM26 response in this part plus accumulated time
        // since then);.
        // i.e., Fail if ABS[(Time Since Engine StartB - Time Since Engine StartA) - Delta Time] > 10 seconds.
        packets.stream()
               .filter(p -> !areTimeConsistent(p))
               .map(ParsedPacket::getModuleName)
               .forEach(moduleName -> {
                   addFailure("6.11.11.2.a - " + moduleName
                           + " reported time since engine start differs by more than ±10 seconds from expected value");
               });

        // 6.11.11.2.b. Fail if NACK not received from OBD ECUs that did not provide a DM26 message.
        checkForNACKsDS(packets, filterRequestResultAcks(dsResults), "6.11.11.2.b");

        // 6.11.11.1.b. Record all monitor readiness this trip data (i.e., which supported monitors are complete this
        // trip or supported and not complete this trip).
        // This is out of order to prevent overwriting previous data before use.
        packets.forEach(this::save);
    }

    private boolean areTimeConsistent(DM26TripDiagnosticReadinessPacket currentPacket) {
        var previousPacket = get(DM26TripDiagnosticReadinessPacket.class, currentPacket.getSourceAddress());
        if (previousPacket == null) {
            return false;
        }

        var currentTime = currentPacket.getPacket().getTimestamp();
        var previousTime = previousPacket.getPacket().getTimestamp();
        var timeDifference = previousTime.until(currentTime, ChronoUnit.SECONDS);

        var currentWarmUpSeconds = currentPacket.getTimeSinceEngineStart();
        var previousWarmUpSeconds = previousPacket.getTimeSinceEngineStart();
        var warmUpDifference = currentWarmUpSeconds - previousWarmUpSeconds;

        return Math.abs(timeDifference - warmUpDifference) < 10; // seconds
    }

}
