package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.common.CnmServiceType;
import eu.egm.data.cnm.common.GridTopologyObject;
import eu.egm.data.cnm.common.GridTopologyRelation;
import eu.egm.data.cnm.rdf.CimProfileFact;
import eu.egm.data.cnm.rdf.ProfileFragment;
import eu.egm.data.cnm.snapshot.CgmNetworkSnapshot;
import eu.egm.data.cnm.state.StateSnapshot;
import eu.egm.data.cnm.state.StateVariablePoint;
import eu.egm.data.cnm.topology.StaticTopologyModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Two-pass assembler that stitches profile fragments into one CGM snapshot.
 *
 * <p>Pass 1 indexes every fact by mRID. Pass 2 materializes static topology
 * objects, dynamic state values, and relation diagnostics using the completed
 * index so forward references across files can be checked consistently.</p>
 */
public class CgmSnapshotAssembler {
    public CgmNetworkSnapshot assemble(CnmServiceType serviceType, List<ProfileFragment> fragments) {
        if (fragments == null || fragments.isEmpty()) {
            throw new IllegalArgumentException("At least one profile fragment is required");
        }
        ProfileFragment first = fragments.getFirst();
        Map<String, CimProfileFact> index = new LinkedHashMap<>();
        for (ProfileFragment fragment : fragments) {
            for (CimProfileFact fact : fragment.facts()) {
                index.putIfAbsent(fact.mRID(), fact);
            }
        }

        List<GridTopologyObject> staticObjects = new ArrayList<>();
        List<GridTopologyRelation> relations = new ArrayList<>();
        List<StateVariablePoint> stateValues = new ArrayList<>();
        Set<String> unresolved = new LinkedHashSet<>();
        Set<String> sourceFileIds = new LinkedHashSet<>();
        List<String> diagnostics = new ArrayList<>();

        for (ProfileFragment fragment : fragments) {
            sourceFileIds.add(fragment.fileId());
            diagnostics.addAll(fragment.diagnostics());
            for (CimProfileFact fact : fragment.facts()) {
                if (isDynamicProfile(fact.profileType()) || isDynamicType(fact.cimType())) {
                    stateValues.add(new StateVariablePoint(
                            fact.mRID(),
                            fact.cimType(),
                            fact.profileType(),
                            fact.attributes(),
                            fact.references()));
                } else {
                    staticObjects.add(new GridTopologyObject(
                            fact.mRID(),
                            name(fact),
                            fact.cimType(),
                            fact.profileType(),
                            fact.attributes()));
                }
                fact.references().forEach((type, target) -> {
                    relations.add(new GridTopologyRelation(
                            fact.mRID() + ":" + type + ":" + target,
                            fact.mRID(),
                            target,
                            type,
                            Map.of("profileType", fact.profileType())));
                    if (!target.isBlank() && !index.containsKey(target)) {
                        unresolved.add(fact.mRID() + " -> " + target + " (" + type + ")");
                    }
                });
            }
        }
        unresolved.forEach(value -> diagnostics.add("Unresolved CGMES reference: " + value));
        String snapshotId = snapshotId(first.importId(), first.tsoName(), first.businessDay(), first.businessTime(), first.timeFrame());
        return new CgmNetworkSnapshot(
                snapshotId,
                first.importId(),
                serviceType,
                first.tsoName(),
                first.businessDay(),
                first.businessTime(),
                first.timeFrame(),
                new StaticTopologyModel(staticObjects, relations, List.copyOf(unresolved)),
                new StateSnapshot(snapshotId, first.businessDay(), first.businessTime(), first.timeFrame(), stateValues),
                List.copyOf(sourceFileIds),
                diagnostics,
                List.of());
    }

    private boolean isDynamicProfile(String profileType) {
        String normalized = normalized(profileType);
        return normalized.equals("SSH") || normalized.equals("SV") || normalized.endsWith("_SSH") || normalized.endsWith("_SV");
    }

    private boolean isDynamicType(String cimType) {
        String normalized = normalized(cimType);
        return normalized.startsWith("SV") || normalized.contains("STATE") || normalized.contains("SETPOINT");
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String name(CimProfileFact fact) {
        Object name = fact.attributes().get("name");
        return name == null ? "" : String.valueOf(name);
    }

    private String snapshotId(String importId, String tsoName, String businessDay, String businessTime, String timeFrame) {
        return String.join(":",
                valueOr(importId),
                valueOr(tsoName),
                valueOr(businessDay),
                valueOr(businessTime),
                valueOr(timeFrame));
    }

    private String valueOr(String value) {
        return value == null ? "" : value;
    }
}
