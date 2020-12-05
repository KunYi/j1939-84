/**
 * Copyright 2019 Equipment & Tool Institute
 */
package org.etools.j1939_84.bus.j1939.packets;

import static org.etools.j1939_84.J1939_84.NL;

import org.etools.j1939_84.bus.Packet;
import org.etools.j1939_84.bus.j1939.J1939DaRepository;

/**
 * Parses the DM21 Diagnostic Readiness Packet
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class DM21DiagnosticReadinessPacket extends GenericPacket {

    public static final int PGN = 49408;

    public static final String TSCC_LINE = "Time Since DTCs Cleared:";

    public DM21DiagnosticReadinessPacket(Packet packet) {
        super(packet, new J1939DaRepository().findPgnDefinition(PGN));
    }

    private String getDistanceSinceDTCsClearedAsString() {
        return getValuesWithUnits(getKmSinceDTCsCleared(), "km", getMilesSinceDTCsCleared(), "mi");
    }

    private String getDistanceWithMILActiveAsString() {
        return getValuesWithUnits(getKmWhileMILIsActivated(), "km", getMilesWhileMILIsActivated(), "mi");
    }

    /**
     * Returns the total number of kilometers the vehicle has traveled since the
     * Diagnostic Trouble Codes were last cleared
     *
     * @return kilometers as a double
     */
    public double getKmSinceDTCsCleared() {
        return getScaledShortValue(2, 1);
    }

    /**
     * Returns the total number of kilometers the vehicle has traveled while the
     * Malfunction Indicator Lamp has been active
     *
     * @return kilometers as a double
     */
    public double getKmWhileMILIsActivated() {
        return getScaledShortValue(0, 1);
    }

    /**
     * Returns the total number of miles the vehicle has traveled since the
     * Diagnostic Trouble Codes were last cleared
     *
     * @return miles as a double
     */
    public double getMilesSinceDTCsCleared() {
        return getKmSinceDTCsCleared() * KM_TO_MILES_FACTOR;
    }

    /**
     * Returns the total number of miles the vehicle has traveled while the
     * Malfunction Indicator Lamp has been active
     *
     * @return miles as a double
     */
    public double getMilesWhileMILIsActivated() {
        return getKmWhileMILIsActivated() * KM_TO_MILES_FACTOR;
    }

    /**
     * Returns the total number of minutes the engine has been running since the
     * Diagnostic Trouble Codes were last cleared.
     *
     * @return minutes as a double
     */
    public double getMinutesSinceDTCsCleared() {
        return getScaledShortValue(6, 1);
    }

    /**
     * Returns the total number of minutes the engine has been running while the
     * Malfunction Indicator Lamp has been active
     *
     * @return minutes as a double
     */
    public double getMinutesWhileMILIsActivated() {
        return getScaledShortValue(4, 1);
    }

    @Override
    public String getName() {
        return "DM21";
    }

    private String getTimeSinceDTCsClearedAsString() {
        return getValueWithUnits(getMinutesSinceDTCsCleared(), "minutes");
    }

    private String getTimeWithMILActiveAsString() {
        return getValueWithUnits(getMinutesWhileMILIsActivated(), "minutes");
    }

    @Override
    public String toString() {
        String result = getStringPrefix() + "[" + NL;
        result += "  Distance Traveled While MIL is Activated:     " + getDistanceWithMILActiveAsString() + NL;
        result += "  Time Run by Engine While MIL is Activated:    " + getTimeWithMILActiveAsString() + NL;
        result += "  Distance Since DTCs Cleared:                  " + getDistanceSinceDTCsClearedAsString() + NL;
        result += "  " + TSCC_LINE + "                      " + getTimeSinceDTCsClearedAsString() + NL;
        result += "]";
        return result;
    }
}
