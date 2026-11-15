package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.MappingInheritanceStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Shared by every mapper: an unmapped target property fails the build, so a renamed field can't silently become
 * null in a response. Ignore intended gaps explicitly with {@code @Mapping(target = ..., ignore = true)}.
 */
@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        mappingInheritanceStrategy = MappingInheritanceStrategy.AUTO_INHERIT_FROM_CONFIG)
public interface MtgMapperConfig {

    /**
     * Prototype inherited by every mapping to an entity: audit columns are set by JPA auditing, never mapped.
     */
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    Auditable toAuditable(Object source);
}
