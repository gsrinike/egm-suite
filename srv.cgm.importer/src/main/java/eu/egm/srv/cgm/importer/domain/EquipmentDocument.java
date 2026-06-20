package eu.egm.srv.cgm.importer.domain;

import eu.egm.com.data.cgmes.EquipmentType;
import eu.egm.com.data.cgmes.EquipmentView;
import eu.egm.com.data.cgmes.CgmesProcess;
import eu.egm.com.data.cgmes.CgmesRegion;
import eu.egm.com.data.cgmes.ImportMetadata;

import java.util.Map;

/**
 * Storage document for one searchable network element.
 *
 * The class stays persistence-annotation free; technology-specific mapping is
 * handled by com.infra through the repository adapter.
 */
public class EquipmentDocument {
    private String documentId;
    private String equipmentId;
    private String networkId;
    private String businessDay;
    private String timestamp;
    private CgmesRegion region;
    private CgmesProcess process;
    private String timeFrame;
    private String tsoName;
    private String cgmesProfileType;
    private String versionNumber;
    private String extension;
    private String name;
    private EquipmentType type;
    private String containerId;
    private double nominalVoltage;
    private Map<String, Object> attributes;

    public EquipmentDocument() {
    }

    public EquipmentDocument(String documentId, String equipmentId, String networkId, String businessDay, String timestamp,
                             CgmesRegion region, CgmesProcess process, String timeFrame, String tsoName,
                             String cgmesProfileType, String versionNumber, String extension, String name, EquipmentType type,
                             String containerId, double nominalVoltage, Map<String, Object> attributes) {
        this.documentId = documentId;
        this.equipmentId = equipmentId;
        this.networkId = networkId;
        this.businessDay = businessDay;
        this.timestamp = timestamp;
        this.region = region;
        this.process = process;
        this.timeFrame = timeFrame;
        this.tsoName = tsoName;
        this.cgmesProfileType = cgmesProfileType;
        this.versionNumber = versionNumber;
        this.extension = extension;
        this.name = name;
        this.type = type;
        this.containerId = containerId;
        this.nominalVoltage = nominalVoltage;
        this.attributes = attributes;
    }

    public static EquipmentDocument fromView(EquipmentView view) {
        return new EquipmentDocument(
                view.networkId() + ":" + view.type() + ":" + view.id(),
                view.id(),
                view.networkId(),
                view.metadata().businessDay().toString(),
                view.metadata().timestamp().toString(),
                view.metadata().region(),
                view.metadata().process(),
                view.metadata().timeFrame(),
                view.metadata().tsoName(),
                view.metadata().cgmesProfileType(),
                view.metadata().versionNumber(),
                view.metadata().extension(),
                view.name(),
                view.type(),
                view.containerId(),
                view.nominalVoltage(),
                view.attributes());
    }

    public EquipmentView toView() {
        return new EquipmentView(equipmentId, networkId,
                ImportMetadata.of(java.time.LocalDate.parse(businessDay), timestamp, region, process, timeFrame, tsoName, cgmesProfileType, versionNumber, extension),
                name, type, containerId, nominalVoltage, attributes);
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public String getNetworkId() {
        return networkId;
    }

    public String getBusinessDay() {
        return businessDay;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public CgmesRegion getRegion() {
        return region;
    }

    public CgmesProcess getProcess() {
        return process;
    }

    public String getTimeFrame() {
        return timeFrame;
    }

    public String getTsoName() {
        return tsoName;
    }

    public String getCgmesProfileType() {
        return cgmesProfileType;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getExtension() {
        return extension;
    }

    public String getName() {
        return name;
    }

    public EquipmentType getType() {
        return type;
    }

    public String getContainerId() {
        return containerId;
    }

    public double getNominalVoltage() {
        return nominalVoltage;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
