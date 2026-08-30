package com.ticketmesh.repository;

import com.ticketmesh.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    List<OtpCode> findByUser_IdAndPurposeOrderByCreatedAtDesc(
            Long userId, OtpCode.Purpose purpose);
}
