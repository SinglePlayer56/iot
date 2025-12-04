package com.viktor.failed_events_processor.mapper;

import com.viktor.avro.DltError;
import com.viktor.failed_events_processor.dto.ErrorDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ErrorMapper {

    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "errorSource", source = "errorSource")
    @Mapping(target = "stackTrace", source = "stackTrace")
    @Mapping(target = "traceId", source = "traceId")
    @Mapping(target = "receivedAt", source = "receivedAt")
    ErrorDto toErrorDto(DltError dltError);
}
