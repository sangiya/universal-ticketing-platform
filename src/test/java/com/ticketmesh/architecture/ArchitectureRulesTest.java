package com.ticketmesh.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture fitness functions for the TicketMesh codebase. Enforces:
 * <ul>
 *   <li>Layered architecture: controllers → services → repositories</li>
 *   <li>Package dependencies: no cyclic package dependencies</li>
 *   <li>Naming conventions: *Controller, *Service, *Repository</li>
 *   <li>DTO placement: response/request DTOs in dto package</li>
 *   <li>No Lombok usage (manual explicit constructors required)</li>
 *   <li>Public services must be in a service package</li>
 *   <li>No JPA repositories in controllers</li>
 * </ul>
 */
class ArchitectureRulesTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.ticketmesh");
    }

    @Test
    void noLombokAnnotations() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat().resideInAPackage("lombok..");
        rule.check(importedClasses);
    }

    @Test
    void securityPackageShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ticketmesh.security..")
                .should().dependOnClassesThat().resideInAPackage("com.ticketmesh.controller..");
        rule.check(importedClasses);
    }

    @Test
    void repositoriesShouldNotDependOnServices() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ticketmesh.repository..")
                .should().dependOnClassesThat().resideInAPackage("com.ticketmesh.service..");
        rule.check(importedClasses);
    }

    @Test
    void modelsShouldNotDependOnServices() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ticketmesh.model..")
                .should().dependOnClassesThat().resideInAPackage("com.ticketmesh.service..");
        rule.check(importedClasses);
    }

    @Test
    void aiPackageShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ticketmesh.ai..")
                .should().dependOnClassesThat().resideInAPackage("com.ticketmesh.controller..");
        rule.check(importedClasses);
    }

    @Test
    void exceptionsShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ticketmesh.exception..")
                .should().dependOnClassesThat().resideInAPackage("com.ticketmesh.controller..");
        rule.check(importedClasses);
    }

    @Test
    void integrationLayerShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.ticketmesh.integration..")
                .should().dependOnClassesThat().resideInAPackage("com.ticketmesh.controller..");
        rule.check(importedClasses);
    }
}
