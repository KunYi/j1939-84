/*
 * Copyright 2020 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part1;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.etools.j1939_84.bus.j1939.BusResult;
import org.etools.j1939_84.bus.j1939.Lookup;
import org.etools.j1939_84.bus.j1939.packets.DM21DiagnosticReadinessPacket;
import org.etools.j1939_84.controllers.DataRepository;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.DiagnosticReadinessModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.VehicleInformationModule;

/**
 * @author Marianne Schaefer (marianne.m.schaefer@gmail.com)
 * <p>
 * The controller for 6.1.11 DM21: Diagnostic readiness 2
 */
public class Part01Step11Controller extends StepController {

    private static final int PART_NUMBER = 1;
    private static final int STEP_NUMBER = 11;
    private static final int TOTAL_STEPS = 0;

    private final DataRepository dataRepository;
    private final DiagnosticReadinessModule diagnosticReadinessModule;

    Part01Step11Controller(DataRepository dataRepository) {
        this(Executors.newSingleThreadScheduledExecutor(),
             new EngineSpeedModule(),
             new BannerModule(),
             new DiagnosticReadinessModule(),
             new VehicleInformationModule(),
             dataRepository,
             DateTimeModule.getInstance());
    }

    Part01Step11Controller(Executor executor,
                           EngineSpeedModule engineSpeedModule,
                           BannerModule bannerModule,
                           DiagnosticReadinessModule diagnosticReadinessModule,
                           VehicleInformationModule vehicleInformationModule,
                           DataRepository dataRepository,
                           DateTimeModule dateTimeModule) {
        super(executor,
              engineSpeedModule,
              bannerModule,
              vehicleInformationModule,
              dateTimeModule,
              PART_NUMBER,
              STEP_NUMBER,
              TOTAL_STEPS);
        this.diagnosticReadinessModule = diagnosticReadinessModule;
        this.dataRepository = dataRepository;
    }

    @Override
    protected void run() throws Throwable {
        diagnosticReadinessModule.setJ1939(getJ1939());
        // 6.1.11.1.a. Global DM21 (send Request (PGN 59904) for PGN 49408
        List<DM21DiagnosticReadinessPacket> globalPackets = diagnosticReadinessModule
                .requestDM21Packets(getListener(), true).getPackets();

        // 6.1.11.2.e. Fail if no OBD ECU provides a DM21 message.
        if (globalPackets.isEmpty()) {
            addFailure(1, 11, "6.1.11.1.e - No OBD ECU provided a DM21 message");
        } else {
            // 6.1.11.2.a. Fail if any ECU reports distance with MIL on (SPN 3069) is not zero.
            globalPackets.stream()
                    .filter(packet -> isNotZero(packet.getKmSinceDTCsCleared()))
                    .map(p -> Lookup.getAddressName(p.getSourceAddress()))
                    .forEach(moduleName -> addFailure("6.1.11.1.a - " + moduleName + " reported distance with MIL on (SPN 3069) is not zero"));

            // 6.1.11.2.b. Fail if any ECU reports distance SCC (SPN 3294) is not zero.
            globalPackets.stream()
                    .filter(packet -> isNotZero(packet.getKmWhileMILIsActivated()))
                    .map(p -> Lookup.getAddressName(p.getSourceAddress()))
                    .forEach(moduleName -> addFailure("6.1.11.1.b - " + moduleName + " reported distance SCC (SPN 3294) is not zero"));

            // 6.1.11.2.c. Fail if any ECU reports time with MIL on (SPN 3295) is not zero (if supported)
            globalPackets.stream()
                    .filter(packet -> isNotZero(packet.getMinutesWhileMILIsActivated()))
                    .map(p -> Lookup.getAddressName(p.getSourceAddress()))
                    .forEach(moduleName -> addFailure("6.1.11.1.c - " + moduleName + " reported time with MIL on (SPN 3295) is not zero"));

            // 6.1.11.2.d. Fail if any ECU reports time SCC (SPN 3296) > 1 minute (if supported).
            globalPackets.stream()
                    .filter(packet -> Double.valueOf(packet.getMinutesSinceDTCsCleared()).intValue() > 1)
                    .map(p -> Lookup.getAddressName(p.getSourceAddress()))
                    .forEach(moduleName -> addFailure("6.1.11.1.d - " + moduleName + " reported time SCC (SPN 3296) > 1 minute"));
        }

        // 6.1.11.3.a. DS DM21 to each OBD ECU
        List<Integer> obdModuleAddresses = dataRepository.getObdModuleAddresses();

        List<BusResult<DM21DiagnosticReadinessPacket>> dsResults = obdModuleAddresses
                .stream()
                .map(addr -> diagnosticReadinessModule.getDM21Packets(getListener(), true, addr))
                .collect(Collectors.toList());

        // ignore missing responses and NACKs
        List<DM21DiagnosticReadinessPacket> dsPackets = filterPackets(dsResults);

        // 6.1.11.4.a. Fail if any ECU reports distance with MIL on (SPN 3069) is not zero.
        dsPackets.stream()
                .filter(packet -> isNotZero(packet.getKmSinceDTCsCleared()))
                .map(p -> Lookup.getAddressName(p.getSourceAddress()))
                .forEach(moduleName -> addFailure("6.1.11.4.a - " + moduleName + " reported distance with MIL on (SPN 3069) is not zero"));

        // 6.1.11.4.b. Fail if any ECU reports distance SCC (SPN 3294) is not zero.
        dsPackets.stream()
                .filter(packet -> isNotZero(packet.getKmWhileMILIsActivated()))
                .map(p -> Lookup.getAddressName(p.getSourceAddress()))
                .forEach(moduleName -> addFailure("6.1.11.4.b - " + moduleName + " reported distance SCC (SPN 3294) is not zero"));

        // 6.1.11.4.c. Fail if any ECU reports time with MIL on (SPN 3295) is not zero (if supported)
        dsPackets.stream()
                .filter(packet -> isNotZero(packet.getMinutesWhileMILIsActivated()))
                .map(p -> Lookup.getAddressName(p.getSourceAddress()))
                .forEach(moduleName -> addFailure("6.1.11.4.c - " + moduleName + " reported time with MIL on (SPN 3295) is not zero"));

        // 6.1.11.4.d. Fail if any ECU reports time SCC (SPN 3296) > 1 minute (if supported).
        dsPackets.stream()
                .filter(packet -> isNotZero(packet.getMinutesSinceDTCsCleared()))
                .map(p -> Lookup.getAddressName(p.getSourceAddress()))
                .forEach(moduleName -> addFailure("6.1.11.4.d - " + moduleName + " reported time SCC (SPN 3296) > 1 minute"));

        // 6.1.11.4.e Fail if any difference compared to data received during global request.
        compareRequestPackets(globalPackets, dsPackets, "6.1.11.4.e");

        // 6.1.11.4.f Fail if NACK not received from OBD ECUs that did not respond to global query.
        checkForNACKs(globalPackets, filterAcks(dsResults), obdModuleAddresses, "6.1.11.4.f");
    }

    private static boolean isNotZero(double value) {
        return Double.valueOf(value).intValue() != 0;
    }

}
