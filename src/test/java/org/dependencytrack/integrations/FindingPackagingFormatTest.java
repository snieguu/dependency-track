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
package org.dependencytrack.integrations;

import alpine.Config;
import org.dependencytrack.PersistenceCapableTest;
import org.dependencytrack.model.AnalysisState;
import org.dependencytrack.model.Finding;
import org.dependencytrack.model.Project;
import org.dependencytrack.model.Severity;
import org.dependencytrack.model.Vulnerability;
import org.dependencytrack.model.VulnerabilityAlias;
import org.dependencytrack.tasks.scanners.AnalyzerIdentity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

class FindingPackagingFormatTest extends PersistenceCapableTest {

    @Test
    @SuppressWarnings("unchecked")
    void wrapperTest() {
        Project project = qm.createProject(
                "Test", "Sample project", "1.0", null, null, null, true, false);
        FindingPackagingFormat fpf = new FindingPackagingFormat(
                project.getUuid(),
                Collections.EMPTY_LIST
        );
        JSONObject root = fpf.getDocument();

        JSONObject meta = root.getJSONObject("meta");
        Assertions.assertEquals(Config.getInstance().getApplicationName(), meta.getString("application"));
        Assertions.assertEquals(Config.getInstance().getApplicationVersion(), meta.getString("version"));
        Assertions.assertNotNull(meta.getString("timestamp"));

        JSONObject pjson = root.getJSONObject("project");
        Assertions.assertEquals(project.getName(), pjson.getString("name"));
        Assertions.assertEquals(project.getDescription(), pjson.getString("description"));
        Assertions.assertEquals(project.getVersion(), pjson.getString("version"));

        Assertions.assertEquals("1.2", root.getString("version"));
    }

    @Test
    void testFindingsVulnerabilityAndAliases() {
        Project project = qm.createProject(
                "Test", "Sample project", "1.0", null, null, null, true, false);

        Finding findingWithoutAlias = new Finding(project.getUuid(), "component-uuid-1", "component-name-1", "component-group",
                "component-version", "component-purl", "component-cpe", "vuln-uuid", Vulnerability.Source.GITHUB, "vuln-vulnId-1", "vuln-title",
                "vuln-subtitle", "vuln-description", "vuln-recommendation", Severity.CRITICAL, BigDecimal.valueOf(7.2), BigDecimal.valueOf(8.4), BigDecimal.valueOf(1.25), BigDecimal.valueOf(1.75), BigDecimal.valueOf(1.3),
                "0.5", "0.9", null, AnalyzerIdentity.OSSINDEX_ANALYZER, new Date(), null, null, AnalysisState.NOT_AFFECTED, true);

        Finding findingWithAlias = new Finding(project.getUuid(), "component-uuid-2", "component-name-2", "component-group",
                "component-version", "component-purl", "component-cpe", "vuln-uuid", Vulnerability.Source.NVD, "vuln-vulnId-2", "vuln-title",
                "vuln-subtitle", "vuln-description", "vuln-recommendation", Severity.HIGH, BigDecimal.valueOf(7.2), BigDecimal.valueOf(8.4), BigDecimal.valueOf(1.25), BigDecimal.valueOf(1.75), BigDecimal.valueOf(1.3),
                "0.5", "0.9", null, AnalyzerIdentity.INTERNAL_ANALYZER, new Date(), null, null, AnalysisState.NOT_AFFECTED, true);

        var alias = new VulnerabilityAlias();
        alias.setCveId("someCveId");
        alias.setSonatypeId("someSonatypeId");
        alias.setGhsaId("someGhsaId");
        alias.setOsvId("someOsvId");
        alias.setSnykId("someSnykId");
        alias.setGsdId("someGsdId");
        alias.setVulnDbId("someVulnDbId");
        alias.setInternalId("someInternalId");

        var other = new VulnerabilityAlias();
        other.setCveId("anotherCveId");
        other.setSonatypeId("anotherSonatypeId");
        other.setGhsaId("anotherGhsaId");
        other.setOsvId("anotherOsvId");
        other.setSnykId("anotherSnykId");
        other.setGsdId("anotherGsdId");
        other.setInternalId("anotherInternalId");
        other.setVulnDbId(null);

        findingWithoutAlias.addVulnerabilityAliases(List.of());
        findingWithAlias.addVulnerabilityAliases(List.of(alias, other));

        FindingPackagingFormat fpf = new FindingPackagingFormat(
                project.getUuid(),
                List.of(findingWithoutAlias, findingWithAlias)
        );

        JSONObject root = fpf.getDocument();

        JSONArray findings = root.getJSONArray("findings");

        Assertions.assertEquals("component-name-1", findings.getJSONObject(0).getJSONObject("component").getString("name"));
        Assertions.assertEquals("component-name-2", findings.getJSONObject(1).getJSONObject("component").getString("name"));

        Assertions.assertEquals(AnalyzerIdentity.OSSINDEX_ANALYZER, findings.getJSONObject(0).getJSONObject("attribution").get("analyzerIdentity"));
        Assertions.assertEquals(AnalyzerIdentity.INTERNAL_ANALYZER, findings.getJSONObject(1).getJSONObject("attribution").get("analyzerIdentity"));

        Assertions.assertEquals(Severity.CRITICAL.toString(), findings.getJSONObject(0).getJSONObject("vulnerability").get("severity"));
        Assertions.assertEquals(Severity.HIGH.toString(), findings.getJSONObject(1).getJSONObject("vulnerability").get("severity"));

        JSONArray aliases_1 =  findings.getJSONObject(0).getJSONObject("vulnerability").getJSONArray("aliases");
        Assertions.assertTrue(aliases_1.isEmpty());
        JSONArray aliases_2 =  findings.getJSONObject(1).getJSONObject("vulnerability").getJSONArray("aliases");
        Assertions.assertFalse(aliases_2.isEmpty());
        Assertions.assertEquals(2, aliases_2.length());
        Assertions.assertEquals("anotherCveId", aliases_2.getJSONObject(0).getString("cveId"));
        Assertions.assertEquals("anotherGhsaId", aliases_2.getJSONObject(0).getString("ghsaId"));
        Assertions.assertEquals("someCveId", aliases_2.getJSONObject(1).getString("cveId"));
        Assertions.assertEquals("someOsvId", aliases_2.getJSONObject(1).getString("osvId"));

        // negative test to see if technical id is not included
        Assertions.assertFalse(aliases_2.getJSONObject(0).has("id"));

        //final negative test to make sure the allBySource element is not included
        String finalJsonOutput = root.toString();
        Assertions.assertFalse(finalJsonOutput.contains("allBySource"));
    }
}
