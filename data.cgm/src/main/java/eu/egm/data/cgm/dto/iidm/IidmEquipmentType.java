package eu.egm.data.cgm.dto.iidm;

/**
 * IIDM-oriented equipment categories used by mapping modules and service APIs.
 *
 * The enum intentionally stays lightweight. It mirrors the equipment concepts
 * that the suite needs to exchange internally without coupling data-only
 * modules to a specific PowSyBl runtime dependency.
 */
public enum IidmEquipmentType {
    SUBSTATION,
    VOLTAGE_LEVEL,
    BUS,
    LINE,
    TWO_WINDINGS_TRANSFORMER,
    GENERATOR,
    LOAD,
    SHUNT_COMPENSATOR,
    SWITCH,
    STATE_VARIABLE,
    UNKNOWN
}
