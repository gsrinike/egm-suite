package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.cgmes.CgmesDiagramLayoutProfile;
import eu.egm.data.cnm.cgmes.CgmesCoordinateSystem;
import eu.egm.data.cnm.cgmes.CgmesEquipmentProfile;
import eu.egm.data.cnm.cgmes.CgmesGeographicalLocationProfile;
import eu.egm.data.cnm.cgmes.CgmesLocation;
import eu.egm.data.cnm.cgmes.CgmesPositionPoint;
import eu.egm.data.cnm.cgmes.CgmesProfileEntity;
import eu.egm.data.cnm.cgmes.CgmesProfileKind;
import eu.egm.data.cnm.cgmes.CgmesStateVariablesProfile;
import eu.egm.data.cnm.cgmes.CgmesSteadyStateHypothesisProfile;
import eu.egm.data.cnm.cgmes.CgmesTopologyProfile;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import java.util.List;

class CgmesProfileExtractionStrategy extends AbstractProfileExtractionStrategy {
    @Override
    public boolean supports(ProfileFamily family, String profileType) {
        return family != ProfileFamily.NCP && CgmesProfileKind.isKnown(profileType);
    }

    @Override
    public ProfilePayload<?> extract(
            ProfileProcessingContext context,
            List<RdfFact> facts) {
        CgmesProfileKind kind = CgmesProfileKind.fromCode(context.profileType());
        Object profile = switch (kind) {
            case EQUIPMENT, BOUNDARY_EQUIPMENT, EQUIPMENT_OPERATION, EQUIPMENT_SHORT_CIRCUIT, EQUIPMENT_CONTINGENCY -> equipment(facts);
            case STEADY_STATE_HYPOTHESIS -> ssh(facts);
            case STATE_VARIABLES -> sv(facts);
            case TOPOLOGY, BOUNDARY_TOPOLOGY -> tp(facts);
            case DIAGRAM_LAYOUT -> dl(facts);
            case GEOGRAPHICAL_LOCATION -> gl(facts);
            default -> entities(facts);
        };
        return new ProfilePayload<>(
                context.profileFamily() == ProfileFamily.Unknown ? ProfileFamily.CGMES : context.profileFamily(),
                kind == CgmesProfileKind.UNKNOWN ? context.profileType() : kind.code(),
                context.fileId(),
                context.objectId(),
                topologyObjects(facts, context.profileType()),
                topologyRelations(facts),
                List.of(),
                profile);
    }

    private CgmesEquipmentProfile equipment(List<RdfFact> facts) {
        List<CgmesProfileEntity> terminals = entities(facts, "Terminal");
        return new CgmesEquipmentProfile(
                entities(facts, "Substation"),
                entities(facts, "VoltageLevel"),
                facts.stream()
                        .filter(fact -> !matches(fact.type(), "Substation", "VoltageLevel", "Terminal", "RegulatingControl"))
                        .map(fact -> new CgmesProfileEntity(fact.mRID(), name(fact), fact.type(), fact.attributes()))
                        .toList(),
                terminals,
                entities(facts, "RegulatingControl"));
    }

    private CgmesSteadyStateHypothesisProfile ssh(List<RdfFact> facts) {
        return new CgmesSteadyStateHypothesisProfile(
                entities(facts, "Switch", "Equipment", "ConductingEquipment"),
                entities(facts, "Control", "Setpoint", "TapChanger"),
                entities(facts, "Terminal"));
    }

    private CgmesStateVariablesProfile sv(List<RdfFact> facts) {
        return new CgmesStateVariablesProfile(
                entities(facts, "SvVoltage"),
                entities(facts, "SvPowerFlow"),
                entities(facts, "SvInjection"));
    }

    private CgmesTopologyProfile tp(List<RdfFact> facts) {
        return new CgmesTopologyProfile(
                entities(facts, "TopologicalNode"),
                entities(facts, "ConnectivityNode"),
                entities(facts, "Terminal"));
    }

    private CgmesDiagramLayoutProfile dl(List<RdfFact> facts) {
        return new CgmesDiagramLayoutProfile(
                entities(facts, "Diagram"),
                entities(facts, "DiagramObject"),
                entities(facts, "DiagramObjectPoint"),
                entities(facts, "VisibilityLayer"));
    }

    private CgmesGeographicalLocationProfile gl(List<RdfFact> facts) {
        return new CgmesGeographicalLocationProfile(
                facts.stream()
                        .filter(fact -> matches(fact.type(), "Location"))
                        .map(fact -> new CgmesLocation(
                                fact.mRID(),
                                name(fact),
                                fact.type(),
                                reference(fact, "PowerSystemResource"),
                                reference(fact, "CoordinateSystem"),
                                fact.attributes()))
                        .toList(),
                facts.stream()
                        .filter(fact -> matches(fact.type(), "PositionPoint"))
                        .map(fact -> new CgmesPositionPoint(
                                fact.mRID(),
                                name(fact),
                                fact.type(),
                                reference(fact, "Location"),
                                attribute(fact, "sequenceNumber"),
                                attribute(fact, "xPosition"),
                                attribute(fact, "yPosition"),
                                attribute(fact, "zPosition"),
                                fact.attributes()))
                        .toList(),
                facts.stream()
                        .filter(fact -> matches(fact.type(), "CoordinateSystem"))
                        .map(fact -> new CgmesCoordinateSystem(fact.mRID(), name(fact), fact.type(), fact.attributes()))
                        .toList());
    }

    private Object attribute(RdfFact fact, String key) {
        return fact.attributes().entrySet().stream()
                .filter(entry -> key.equalsIgnoreCase(entry.getKey()) || entry.getKey().endsWith("." + key))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }

    private String reference(RdfFact fact, String key) {
        return fact.references().entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().contains(key.toLowerCase()))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }
}
