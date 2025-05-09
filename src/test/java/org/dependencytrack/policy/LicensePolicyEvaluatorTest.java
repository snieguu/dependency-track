/*
 * This file is part of Dependency-Track.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) OWASP Foundation. All Rights Reserved.
 */
package org.dependencytrack.policy;

import org.dependencytrack.PersistenceCapableTest;
import org.dependencytrack.model.Component;
import org.dependencytrack.model.License;
import org.dependencytrack.model.Policy;
import org.dependencytrack.model.PolicyCondition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

class LicensePolicyEvaluatorTest extends PersistenceCapableTest {

    private PolicyEvaluator evaluator;

    @BeforeEach
    public void initEvaluator() {
        evaluator = new LicensePolicyEvaluator();
        evaluator.setQueryManager(qm);
    }

    @Test
    void hasMatch() {
        License license = new License();
        license.setName("Apache 2.0");
        license.setLicenseId("Apache-2.0");
        license.setUuid(UUID.randomUUID());
        license = qm.persist(license);

        Policy policy = qm.createPolicy("Test Policy", Policy.Operator.ANY, Policy.ViolationState.INFO);
        PolicyCondition condition = qm.createPolicyCondition(policy, PolicyCondition.Subject.LICENSE, PolicyCondition.Operator.IS, license.getUuid().toString());
        Component component = new Component();
        component.setResolvedLicense(license);
        List<PolicyConditionViolation> violations = evaluator.evaluate(policy, component);
        Assertions.assertEquals(1, violations.size());
        PolicyConditionViolation violation = violations.get(0);
        Assertions.assertEquals(component, violation.getComponent());
        Assertions.assertEquals(condition, violation.getPolicyCondition());
    }

    @Test
    void noMatch() {
        License license = new License();
        license.setName("Apache 2.0");
        license.setLicenseId("Apache-2.0");
        license.setUuid(UUID.randomUUID());
        license = qm.persist(license);

        License otherLicense = new License();
        otherLicense.setName("WTFPL");
        otherLicense.setLicenseId("WTFPL");
        otherLicense.setUuid(UUID.randomUUID());
        otherLicense = qm.persist(otherLicense);

        Policy policy = qm.createPolicy("Test Policy", Policy.Operator.ANY, Policy.ViolationState.INFO);
        qm.createPolicyCondition(policy, PolicyCondition.Subject.LICENSE, PolicyCondition.Operator.IS,
                otherLicense.getUuid().toString());
        Component component = new Component();
        component.setResolvedLicense(license);
        List<PolicyConditionViolation> violations = evaluator.evaluate(policy, component);
        Assertions.assertEquals(0, violations.size());
    }

    @Test
    void wrongSubject() {
        License license = new License();
        license.setName("Apache 2.0");
        license.setLicenseId("Apache-2.0");
        license.setUuid(UUID.randomUUID());
        license = qm.persist(license);

        Policy policy = qm.createPolicy("Test Policy", Policy.Operator.ANY, Policy.ViolationState.INFO);
        qm.createPolicyCondition(policy, PolicyCondition.Subject.COORDINATES, PolicyCondition.Operator.IS, license.getUuid().toString());
        Component component = new Component();
        component.setResolvedLicense(license);
        List<PolicyConditionViolation> violations = evaluator.evaluate(policy, component);
        Assertions.assertEquals(0, violations.size());
    }

    @Test
    void wrongOperator() {
        License license = new License();
        license.setName("Apache 2.0");
        license.setLicenseId("Apache-2.0");
        license.setUuid(UUID.randomUUID());
        license = qm.persist(license);

        Policy policy = qm.createPolicy("Test Policy", Policy.Operator.ANY, Policy.ViolationState.INFO);
        qm.createPolicyCondition(policy, PolicyCondition.Subject.LICENSE, PolicyCondition.Operator.MATCHES, license.getUuid().toString());
        Component component = new Component();
        component.setResolvedLicense(license);
        List<PolicyConditionViolation> violations = evaluator.evaluate(policy, component);
        Assertions.assertEquals(0, violations.size());
    }

    @Test
    void valueIsUnresolved() {
        License license = new License();
        license.setName("Apache 2.0");
        license.setLicenseId("Apache-2.0");
        license.setUuid(UUID.randomUUID());
        license = qm.persist(license);

        Policy policy = qm.createPolicy("Test Policy", Policy.Operator.ANY, Policy.ViolationState.INFO);
        qm.createPolicyCondition(policy, PolicyCondition.Subject.LICENSE, PolicyCondition.Operator.IS, "unresolved");

        Component componentWithLicense = new Component();
        componentWithLicense.setResolvedLicense(license);

        Component componentWithoutLicense = new Component();

        List<PolicyConditionViolation> violations = evaluator.evaluate(policy, componentWithLicense);
        Assertions.assertEquals(0, violations.size());

        violations = evaluator.evaluate(policy, componentWithoutLicense);
        Assertions.assertEquals(1, violations.size());
    }

}
