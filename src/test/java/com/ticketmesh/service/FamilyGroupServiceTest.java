package com.ticketmesh.service;

import com.ticketmesh.dto.FamilyGroupResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.model.FamilyGroup;
import com.ticketmesh.model.FamilyMember;
import com.ticketmesh.repository.FamilyGroupRepository;
import com.ticketmesh.repository.FamilyMemberRepository;
import com.ticketmesh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FamilyGroupServiceTest {

    private FamilyGroupRepository groupRepository;
    private FamilyMemberRepository memberRepository;
    private UserRepository userRepository;
    private FamilyGroupService familyService;

    private FamilyGroup group;

    @BeforeEach
    void setUp() {
        groupRepository = mock(FamilyGroupRepository.class);
        memberRepository = mock(FamilyMemberRepository.class);
        userRepository = mock(UserRepository.class);
        familyService = new FamilyGroupService(groupRepository, memberRepository, userRepository);

        group = new FamilyGroup(1L, "The Crowes", 7L);
        when(groupRepository.save(any(FamilyGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.save(any(FamilyMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
    }

    @Test
    void create_createsGroupWithOwnerMember() {
        FamilyGroupResponse response = familyService.create(7L, 1L, "The Crowes");

        assertEquals("The Crowes", response.name());
        assertEquals(7L, response.ownerUserId());
        assertEquals(1L, response.memberCount());
        verify(groupRepository).save(any(FamilyGroup.class));
        verify(memberRepository).save(any(FamilyMember.class));
    }

    @Test
    void create_usesDefaultTenantWhenNull() {
        FamilyGroupResponse response = familyService.create(7L, null, "Solo");

        assertEquals(7L, response.ownerUserId());
        verify(groupRepository).save(any(FamilyGroup.class));
    }

    @Test
    void join_addsMember() {
        when(memberRepository.existsByFamilyIdAndUserId(100L, 8L)).thenReturn(false);
        when(memberRepository.countByFamilyId(100L)).thenReturn(2L);

        FamilyGroupResponse response = familyService.join(8L, 1L, 100L);

        assertEquals(2L, response.memberCount());
        verify(memberRepository).save(any(FamilyMember.class));
    }

    @Test
    void join_rejectsDuplicateMember() {
        when(memberRepository.existsByFamilyIdAndUserId(100L, 8L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> familyService.join(8L, 1L, 100L));
        verify(memberRepository, never()).save(any(FamilyMember.class));
    }

    @Test
    void owner_removesMember() {
        FamilyMember actor = new FamilyMember(100L, 7L, FamilyMember.Role.OWNER);
        FamilyMember target = new FamilyMember(100L, 8L, FamilyMember.Role.MEMBER);
        when(memberRepository.findByFamilyIdAndUserId(100L, 7L)).thenReturn(Optional.of(actor));
        when(memberRepository.findByFamilyIdAndUserId(100L, 8L)).thenReturn(Optional.of(target));
        when(memberRepository.countByFamilyId(100L)).thenReturn(1L);

        FamilyGroupResponse response = familyService.removeMember(100L, 8L, 7L);

        assertEquals(1L, response.memberCount());
        verify(memberRepository).delete(target);
    }

    @Test
    void nonOwner_cannotRemoveMember() {
        FamilyMember actor = new FamilyMember(100L, 9L, FamilyMember.Role.MEMBER);
        when(memberRepository.findByFamilyIdAndUserId(100L, 9L)).thenReturn(Optional.of(actor));

        assertThrows(ConflictException.class,
                () -> familyService.removeMember(100L, 8L, 9L));
        verify(memberRepository, never()).delete(any(FamilyMember.class));
    }
}
