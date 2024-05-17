package com.mongodb.unified.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TestContext {
    private List<TestDescription> testDescriptions;

    public TestContext() {
        //NOOP
    }

    public TestContext(TestDescription ... testDescriptions) {
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
    }

    public List<TestDescription> getTestDescriptions() {
        return testDescriptions;
    }
}
