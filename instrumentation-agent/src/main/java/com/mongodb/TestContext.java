package com.mongodb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the description of the test context that will be passed to the agent.
 * It contains a list of test descriptions which has to be run.
 */
public class TestContext {
    @JsonProperty
    private List<TestDescription> testDescriptions;

    public TestContext() {
        testDescriptions = new ArrayList<>();
    }

    public static TestContext emptyContext() {
        return new TestContext();
    }

    public TestContext(TestDescription... testDescriptions) {
        this.testDescriptions = List.of(testDescriptions);
    }

    public TestContext(List<TestDescription> testDescriptions) {
        this.testDescriptions = testDescriptions;
    }

    public static class TestDescription {
        @JsonProperty
        private String description;
        @JsonProperty
        private String testFile;

        public TestDescription(@JsonProperty("description") String description, @JsonProperty("testFile") String testFile) {
            this.description = description;
            this.testFile = testFile;
        }

        public String getDescription() {
            return description;
        }

        public String getTestFile() {
            return testFile;
        }
    }

    public List<TestDescription> getTestDescriptions() {
        return testDescriptions;
    }
}
