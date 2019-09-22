/**
 * Copyright (c) 2019. Equipment & Tool Institute
 */
package org.etools.j1939_84.model;

/**
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public enum Outcome {
    ABORT("ABORT"), FAIL("FAIL"), INCOMPLETE("INCOMPLETE"), PASS("PASS"), WARN("WARN");

    private final String string;

    private Outcome(String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }
}
