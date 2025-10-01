/*
 * The MIT License
 *
 * Copyright (c) 2025, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.ec2;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import hudson.slaves.NodeProvisioner;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests to verify the availability of provisioner strategies for the EC2 plugin.
 *
 * This test ensures that the NoDelayProvisionerStrategy from jenkins-core
 * is available for use by the EC2 plugin.
 */
@WithJenkins
class ProvisionerStrategyAvailabilityTest {

    /**
     * Check if Jenkins version is 2.530 or higher.
     * The NoDelayProvisionerStrategy CloudProvisioningListener fixes were added in 2.530.
     */
    private boolean isJenkins2530OrHigher() {
        String version = Jenkins.VERSION;
        if (version == null) {
            return false;
        }

        // Parse version number (format: "2.530" or "2.530-SNAPSHOT")
        String[] parts = version.split("[.-]");
        if (parts.length < 2) {
            return false;
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return major > 2 || (major == 2 && minor >= 530);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Test
    void testStandardProvisionerStrategyAvailable(JenkinsRule r) {
        Jenkins jenkins = r.jenkins;
        List<NodeProvisioner.Strategy> strategies = jenkins.getExtensionList(NodeProvisioner.Strategy.class);

        // Verify the standard strategy is available (sanity check)
        boolean standardStrategyFound = strategies.stream()
                .anyMatch(strategy -> strategy.getClass().getSimpleName().contains("StandardStrategy")
                        || strategy.getClass().getSimpleName().contains("Standard"));

        assertTrue(
                standardStrategyFound, "StandardStrategyImpl should be available as a baseline provisioner strategy");
    }

    @Test
    void testNoDelayProvisionerStrategyAvailable(JenkinsRule r) {
        // NoDelayProvisionerStrategy with CloudProvisioningListener fixes requires Jenkins 2.530+
        assumeTrue(
                isJenkins2530OrHigher(),
                "NoDelayProvisionerStrategy CloudProvisioningListener fixes require Jenkins 2.530+. "
                        + "Current version: " + Jenkins.VERSION);

        Jenkins jenkins = r.jenkins;
        List<NodeProvisioner.Strategy> strategies = jenkins.getExtensionList(NodeProvisioner.Strategy.class);

        // Check for NoDelayProvisionerStrategy (should exist in 2.530+)
        boolean noDelayStrategyFound = strategies.stream().anyMatch(strategy -> "NoDelayProvisionerStrategy"
                .equals(strategy.getClass().getSimpleName()));

        assertTrue(noDelayStrategyFound, "NoDelayProvisionerStrategy should be available from jenkins-core 2.530+");

        NodeProvisioner.Strategy noDelayStrategy = strategies.stream()
                .filter(strategy ->
                        "NoDelayProvisionerStrategy".equals(strategy.getClass().getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("NoDelayProvisionerStrategy not found"));

        assertNotNull(noDelayStrategy, "NoDelayProvisionerStrategy should be instantiable");

        // Verify it's the correct class from jenkins-core
        assertEquals(
                "hudson.slaves.NoDelayProvisionerStrategy",
                noDelayStrategy.getClass().getName(),
                "NoDelayProvisionerStrategy should be from hudson.slaves package");
    }

    @Test
    void testProvisionerStrategiesCanBeInstantiated(JenkinsRule r) {
        Jenkins jenkins = r.jenkins;
        List<NodeProvisioner.Strategy> strategies = jenkins.getExtensionList(NodeProvisioner.Strategy.class);

        // Verify we have at least the baseline strategies
        assertFalse(strategies.isEmpty(), "At least one provisioner strategy should be available");

        // Verify all strategies can be instantiated and are not null
        for (NodeProvisioner.Strategy strategy : strategies) {
            assertNotNull(strategy, "Strategy should not be null");
            assertNotNull(strategy.getClass(), "Strategy class should not be null");
            assertNotNull(strategy.getClass().getSimpleName(), "Strategy class name should not be null");
        }

        // Verify StandardStrategyImpl is always present (baseline)
        boolean hasStandardStrategy = strategies.stream()
                .anyMatch(strategy -> strategy.getClass().getSimpleName().contains("StandardStrategy"));
        assertTrue(hasStandardStrategy, "StandardStrategyImpl should always be available");

        // If Jenkins 2.530+, verify NoDelayProvisionerStrategy is present
        if (isJenkins2530OrHigher()) {
            boolean hasNoDelayStrategy = strategies.stream().anyMatch(strategy -> "NoDelayProvisionerStrategy"
                    .equals(strategy.getClass().getSimpleName()));
            assertTrue(hasNoDelayStrategy, "NoDelayProvisionerStrategy should be available in Jenkins 2.530+");
        }
    }
}
