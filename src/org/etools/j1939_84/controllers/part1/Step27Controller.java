/*
 * Copyright 2020 Equipment & Tool Institute
 */
package org.etools.j1939_84.controllers.part1;

import static org.etools.j1939_84.J1939_84.NL;
import static org.etools.j1939_84.controllers.QuestionListener.AnswerType.NO;
import static org.etools.j1939_84.controllers.ResultsListener.MessageType.QUESTION;
import static org.etools.j1939_84.controllers.ResultsListener.MessageType.WARNING;
import static org.etools.j1939_84.model.Outcome.ABORT;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.etools.j1939_84.controllers.QuestionListener;
import org.etools.j1939_84.controllers.StepController;
import org.etools.j1939_84.model.Outcome;
import org.etools.j1939_84.modules.BannerModule;
import org.etools.j1939_84.modules.DateTimeModule;
import org.etools.j1939_84.modules.EngineSpeedModule;
import org.etools.j1939_84.modules.VehicleInformationModule;

/**
 * @author Marianne Schaefer (marianne.m.schaefer@gmail.com)
 * <p>
 * The controller for 6.1.27 Part 1 to Part 2 Transition
 */
public class Step27Controller extends StepController {

    private static final int PART_NUMBER = 1;

    private static final int STEP_NUMBER = 27;

    private static final int TOTAL_STEPS = 3;

    Step27Controller() {
        this(Executors.newSingleThreadScheduledExecutor(),
                new EngineSpeedModule(),
                new BannerModule(),
                new VehicleInformationModule(),
                DateTimeModule.getInstance());
    }

    Step27Controller(Executor executor,
                     EngineSpeedModule engineSpeedModule,
                     BannerModule bannerModule,
                     VehicleInformationModule vehicleInformationModule,
                     DateTimeModule dateTimeModule) {
        super(executor,
                engineSpeedModule,
                bannerModule,
                vehicleInformationModule,
                dateTimeModule,
                PART_NUMBER,
                STEP_NUMBER,
                TOTAL_STEPS);
    }

    @Override
    protected void run() throws Throwable {
        incrementProgress("Part 1, Step 27 - Part 1 to Part 2 Transition");
        //  6.1.27.1 Actions:
        //  a. Testing may be stopped for vehicles with failed tests and for
        //  vehicles with the MIL on or a non-emissions related fault displayed
        //  in DM1. Vehicles with the MIL on will fail subsequent tests.
        displayQuestionMessage();

        //  b. The transition from part 1 to part 2 shall be as provided below.
        //        i. The engine shall be started without turning the key off.
        //       ii. Or, an electric drive or hybrid drive system shall be placed in the operating
        //           mode used to provide power to the drive system without moving the vehicle, if not
        //           automatically provided during the initial key off to key on operation.
        incrementProgress("Part 1, Step 27 b.i - Ensuring Key On, Engine On");
        ensureKeyOnEngineOn();

        //      iii. The engine shall be allowed to idle one minute
        incrementProgress("Part 1, Step 27 b.iii - Allowing engine to idle one minute");
        waitForOneMinute();
    }

    private void waitForOneMinute() throws InterruptedException {
        long secondsToGo = 60;
        getListener().onResult("Allowing engine to idle for " + secondsToGo + " seconds");
        long stopTime = getDateTimeModule().getTimeAsLong() + secondsToGo * 1000L;
        while (secondsToGo > 0) {
            secondsToGo = (stopTime - getDateTimeModule().getTimeAsLong()) / 1000;
            updateProgress("Allowing engine to idle for " + secondsToGo + " seconds");
            getDateTimeModule().pauseFor(1000);
        }
    }

    /**
     * This method determines if there was a failure in the previous tests.
     * Then, if there has been a failure; the method asks the user if they
     * would like to continue and responds accordingly.
     */
    private void displayQuestionMessage() {
        // Only display question if there was a failure otherwise assume continuing
        // First of all, let's figure out if we have a failure
        boolean hasFailure = getPartResult(PART_NUMBER).getStepResults()
                .stream()
                .anyMatch(s -> s.getOutcome() == Outcome.FAIL);
        if (hasFailure) {
            // We have a failure, display the question
            QuestionListener questionListener = answerType -> {
                // end test if user doesn't want to continue
                if (answerType == NO) {
                    getListener().addOutcome(1, 27, ABORT, "Aborting - user ended test");
                    try {
                        setEnding(Ending.ABORTED);
                    } catch (InterruptedException ignored) {
                    }
                }
            };
            //  a. Testing may be stopped for vehicles with failed tests and for vehicles with the MIL on
            //  or a non-emissions related fault displayed in DM1. Vehicles with the MIL on will fail subsequent tests.
            String message = "Ready to transition from Part 1 to Part 2 of the test" + NL;
            message += "a. Testing may be stopped for vehicles with failed tests and for vehicles with the MIL on or a non-emissions related fault displayed in DM1." + NL;
            message += "   Vehicles with the MIL on will fail subsequent tests." + NL + NL;
            message += "This vehicle has had failures and will likely fail subsequent tests.  Would you still like to continue?" + NL;
            getListener().onUrgentMessage(message, "Start Part 2", QUESTION, questionListener);
        }
    }

    /**
     * Ensures the Key is on with the Engine Off and prompts the user to make
     * the proper adjustments.
     *
     * @throws InterruptedException if the user cancels the operation
     */
    private void ensureKeyOnEngineOn() throws InterruptedException {
        try {
            if (!getEngineSpeedModule().isEngineRunning()) {
                getListener().onUrgentMessage("Please turn the Engine ON with Key ON", "Adjust Key Switch", WARNING);
                while (!getEngineSpeedModule().isEngineRunning()) {
                    updateProgress("Waiting for Key ON, Engine ON...");
                    getDateTimeModule().pauseFor(500);
                }
            }
        } catch (InterruptedException e) {
            getListener().addOutcome(getPartNumber(), getStepNumber(), ABORT, "User cancelled operation");
            throw e;
        }
    }
}
