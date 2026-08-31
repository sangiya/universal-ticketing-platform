package com.ticketmesh.service;

import com.ticketmesh.model.KycSubmission;
import com.ticketmesh.repository.KycRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class KycService {

    private final KycRepository kycRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final PiiEncryptor encryptor;

    public KycService(KycRepository kycRepository, UserRepository userRepository,
                      CurrentUser currentUser, PiiEncryptor encryptor) {
        this.kycRepository = kycRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.encryptor = encryptor;
    }

    @Transactional
    public KycSubmission submit(Map<String,String> req) {
        var user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        String docNumber = req.get("documentNumber");
        String docType = req.get("documentType");
        String docUrl = req.get("documentUrl");
        String selfieUrl = req.get("selfieUrl");
        String businessReg = req.get("businessReg");
        Long shopId = req.get("shopId") != null ? Long.valueOf(req.get("shopId")) : null;
        var enc = docNumber != null ? encryptor.encrypt(docNumber) : null;
        var kyc = new KycSubmission(user.getTenantId() != null ? user.getTenantId() : 1L,
                user.getId(), shopId, docType, enc, docUrl, selfieUrl, businessReg);
        return kycRepository.save(kyc);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> mySubmissions() {
        var user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        return kycRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::masked).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> pendingForAdmin() {
        return kycRepository.findByStatusOrderByCreatedAtDesc(KycSubmission.Status.SUBMITTED).stream()
                .map(this::maskedAdmin).toList();
    }

    @Transactional
    public KycSubmission review(Long id, String action, String internalReason, String customerReason) {
        var kyc = kycRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("KYC not found: " + id));
        var reviewer = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        KycSubmission.Status target;
        try { target = KycSubmission.Status.valueOf(action); } catch (Exception e) { throw new ConflictException("Invalid action: " + action); }
        kyc.setStatus(target);
        kyc.setInternalReason(internalReason);
        kyc.setCustomerReason(customerReason);
        kyc.setReviewedAt(java.time.Instant.now());
        kyc.setReviewedBy(reviewer.getId());
        return kycRepository.save(kyc);
    }

    private Map<String,Object> masked(KycSubmission k) {
        String maskedDoc = null;
        if (k.getDocumentNumberEnc() != null) {
            try { String plain = encryptor.decrypt(k.getDocumentNumberEnc()); maskedDoc = plain.length()<=4? "***" : "*".repeat(plain.length()-4)+plain.substring(plain.length()-4); } catch (Exception e) { maskedDoc="***"; }
        }
        return Map.of("id", k.getId(), "status", k.getStatus().name(), "documentType", k.getDocumentType()!=null?k.getDocumentType():"-", "documentNumberMasked", maskedDoc!=null?maskedDoc:"-", "createdAt", k.getCreatedAt().toString(), "customerReason", k.getCustomerReason()!=null?k.getCustomerReason():"-");
    }
    private Map<String,Object> maskedAdmin(KycSubmission k) {
        var base = new java.util.HashMap<>(masked(k));
        base.put("userId", k.getUserId());
        base.put("documentUrl", k.getDocumentUrl()!=null?k.getDocumentUrl():"-");
        base.put("selfieUrl", k.getSelfieUrl()!=null?k.getSelfieUrl():"-");
        // never expose raw document number in list
        return base;
    }
}
