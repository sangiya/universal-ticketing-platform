package com.ticketmesh.service;

import com.ticketmesh.dto.FamilyGroupResponse;
import com.ticketmesh.dto.FamilyMemberResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.FamilyGroup;
import com.ticketmesh.model.FamilyMember;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.FamilyGroupRepository;
import com.ticketmesh.repository.FamilyMemberRepository;
import com.ticketmesh.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Family groups: a tenant-scoped group of users sharing benefits. The owner
 * creates the group and becomes its first OWNER member; other members join and
 * can only be removed by the owner.
 */
@Service
public class FamilyGroupService {

    private final FamilyGroupRepository groupRepository;
    private final FamilyMemberRepository memberRepository;
    private final UserRepository userRepository;

    public FamilyGroupService(FamilyGroupRepository groupRepository,
                              FamilyMemberRepository memberRepository,
                              UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FamilyGroupResponse create(Long userId, Long tenantId, String name) {
        if (tenantId == null) {
            tenantId = 1L;
        }
        FamilyGroup group = groupRepository.save(
                new FamilyGroup(tenantId, name, userId));
        memberRepository.save(new FamilyMember(group.getId(), userId, FamilyMember.Role.OWNER));
        return toGroupResponse(group, 1L);
    }

    @Transactional
    public FamilyGroupResponse join(Long userId, Long tenantId, Long familyId) {
        if (tenantId == null) {
            tenantId = 1L;
        }
        FamilyGroup group = groupRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Family group not found: " + familyId));
        if (!group.getTenantId().equals(tenantId)) {
            throw new NotFoundException("Family group not found: " + familyId);
        }
        if (memberRepository.existsByFamilyIdAndUserId(familyId, userId)) {
            throw new ConflictException("User is already a member of this family group");
        }
        memberRepository.save(new FamilyMember(familyId, userId, FamilyMember.Role.MEMBER));
        return toGroupResponse(group, memberRepository.countByFamilyId(familyId));
    }

    @Transactional
    public FamilyGroupResponse removeMember(Long familyId, Long memberUserId, Long actorUserId) {
        FamilyGroup group = groupRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Family group not found: " + familyId));
        FamilyMember actor = memberRepository.findByFamilyIdAndUserId(familyId, actorUserId)
                .orElseThrow(() -> new NotFoundException("Actor is not a member of this group"));
        if (actor.getRole() != FamilyMember.Role.OWNER) {
            throw new ConflictException("Only the group owner can remove members");
        }
        if (memberUserId.equals(actorUserId)) {
            throw new ConflictException("The group owner cannot remove themselves");
        }
        FamilyMember target = memberRepository.findByFamilyIdAndUserId(familyId, memberUserId)
                .orElseThrow(() -> new NotFoundException("Member not found: " + memberUserId));
        if (target.getRole() == FamilyMember.Role.OWNER) {
            throw new ConflictException("The group owner cannot be removed");
        }
        memberRepository.delete(target);
        return toGroupResponse(group, memberRepository.countByFamilyId(familyId));
    }

    @Transactional(readOnly = true)
    public List<FamilyGroupResponse> myGroups(Long userId) {
        List<Long> familyIds = memberRepository.findByUserId(userId).stream()
                .map(FamilyMember::getFamilyId)
                .collect(Collectors.toList());
        if (familyIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Long familyId : familyIds) {
            counts.put(familyId, memberRepository.countByFamilyId(familyId));
        }
        return familyIds.stream()
                .map(groupRepository::findById)
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparing(FamilyGroup::getCreatedAt).reversed())
                .map(g -> toGroupResponse(g, counts.getOrDefault(g.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> members(Long familyId, Long actorUserId) {
        groupRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Family group not found: " + familyId));
        boolean isMember = memberRepository.existsByFamilyIdAndUserId(familyId, actorUserId);
        if (!isMember) {
            throw new NotFoundException("User is not a member of this group");
        }
        return memberRepository.findByFamilyId(familyId).stream()
                .sorted(Comparator.comparing(FamilyMember::getJoinedAt))
                .map(m -> new FamilyMemberResponse(m.getUserId(), usernameOf(m.getUserId()),
                        m.getRole().name(), m.getJoinedAt()))
                .collect(Collectors.toList());
    }

    private FamilyGroupResponse toGroupResponse(FamilyGroup group, long memberCount) {
        return new FamilyGroupResponse(group.getId(), group.getName(), group.getOwnerUserId(),
                memberCount, group.getCreatedAt());
    }

    private String usernameOf(Long userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse(null);
    }
}
