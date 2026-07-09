package com.example.agv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgvInspectionApplicationTest {

    @Test
    void applicationClassShouldBeInstantiableForCoverage() {
        assertNotNull(new AgvInspectionApplication());
    }
}
