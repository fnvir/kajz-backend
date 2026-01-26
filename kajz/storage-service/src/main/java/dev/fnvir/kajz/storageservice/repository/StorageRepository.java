package dev.fnvir.kajz.storageservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.fnvir.kajz.storageservice.model.FileUpload;

public interface StorageRepository extends JpaRepository<FileUpload, Long> {

}
