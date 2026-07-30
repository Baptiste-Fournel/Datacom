package com.datacom.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.datacom", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule leDomaineNeDependQueDeLuiMemeEtDesLibrairiesDeBase = classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "java..", "jakarta.persistence..", "jakarta.validation..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule leDomaineIgnoreSpring = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule lApplicationIgnoreLesAdaptateurs = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..web..", "..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule leWebIgnoreLInfrastructure = noClasses()
            .that().resideInAPackage("..web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule lesPaquetsSontSansCycle = slices()
            .matching("com.datacom.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);
}
