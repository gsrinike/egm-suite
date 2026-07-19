package eu.egm.srv.cnm.services.rdf;

import eu.egm.data.cnm.cgmes.CgmesEquipmentProfile;
import eu.egm.data.cnm.cgmes.CgmesProfileEntity;
import eu.egm.data.cnm.cgmes.CgmesStateVariablesProfile;
import eu.egm.data.cnm.cgmes.CgmesSteadyStateHypothesisProfile;
import eu.egm.data.cnm.cgmes.CgmesTopologyProfile;
import eu.egm.data.cnm.common.ProfileFamily;
import eu.egm.data.cnm.common.ProfilePayload;
import java.util.List;
import java.util.Locale;

class CgmesProfileExtractionStrategy extends AbstractProfileExtractionStrategy {
    @Override
    public boolean supports(ProfileFamily family, String profileType) {
        String code = profileType == null ? "" : profileType.toUpperCase(Locale.ROOT);
        return family != ProfileFamily.NCP && List.of("EQ", "SSH", "SV", "TP").contains(code);
    }

    @Override
    public ProfilePayload<?> extract(
            ProfileFamily family,
            String profileType,
            String fileId,
            String objectId,
            List<RdfFact> facts) {
        Object profile = switch ((profileType == null ? "" : profileType).toUpperCase(Locale.ROOT)) {
            case "EQ" -> equipment(facts);
            case "SSH" -> ssh(facts);
            case "SV" -> sv(facts);
            case "TP" -> tp(facts);
            default -> entities(facts);
        };
        return new ProfilePayload<>(
                family,
                profileType,
                fileId,
                objectId,
                topologyObjects(facts, profileType),
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
}
