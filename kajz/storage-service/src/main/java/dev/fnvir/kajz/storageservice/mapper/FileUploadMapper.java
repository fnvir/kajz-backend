package dev.fnvir.kajz.storageservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.factory.Mappers;

import dev.fnvir.kajz.storageservice.dto.res.CompleteUploadResponse;
import dev.fnvir.kajz.storageservice.dto.res.FileUploadResponse;
import dev.fnvir.kajz.storageservice.model.FileUpload;

@Mapper(componentModel = ComponentModel.SPRING)
public interface FileUploadMapper {
    
    FileUploadMapper INSTANCE = Mappers.getMapper(FileUploadMapper.class);
    
    @Mapping(target = "fileId", source = "id")
    @Mapping(target = "accessLevel", source = "access")
    @Mapping(target = "contentType", source = "mimeType")
    @Mapping(target = "startedAt", source = "createdAt")
    @Mapping(target = "contentLength", source = "contentSize")
    CompleteUploadResponse fileUploadToResponse(FileUpload fileUpload);
    
    @Mapping(target = "fileId", source = "id")
    FileUploadResponse toResponseDto(FileUpload fileUpload);

}
