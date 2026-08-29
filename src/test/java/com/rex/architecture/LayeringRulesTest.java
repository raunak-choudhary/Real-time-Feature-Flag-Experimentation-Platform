package com.rex.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architectural constraints enforced as tests.
 *
 * <p>These exist so the layering survives future changes. A rule that is only written down in a
 * README decays; a rule that fails the build does not.
 */
class LayeringRulesTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.rex");
  }

  @Test
  @DisplayName("layers are respected: api depends on service, service on repository, never upward")
  void layeredArchitectureIsRespected() {
    Architectures.layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("Api")
        .definedBy("com.rex.api..")
        .layer("Service")
        .definedBy("com.rex.service..")
        .layer("Repository")
        .definedBy("com.rex.repository..")
        .whereLayer("Api")
        .mayNotBeAccessedByAnyLayer()
        .whereLayer("Service")
        .mayOnlyBeAccessedByLayers("Api")
        .whereLayer("Repository")
        .mayOnlyBeAccessedByLayers("Service")
        .allowEmptyShould(true)
        .check(classes);
  }

  @Test
  @DisplayName("controllers never reach a repository directly")
  void controllersDoNotUseRepositories() {
    noClasses()
        .that()
        .resideInAPackage("com.rex.api..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.rex.repository..")
        .allowEmptyShould(true)
        .check(classes);
  }

  @Test
  @DisplayName("the evaluation engine stays free of Spring so it remains a pure unit")
  void evaluationEngineHasNoFrameworkDependency() {
    noClasses()
        .that()
        .resideInAPackage("com.rex.evaluation..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
        .allowEmptyShould(true)
        .check(classes);
  }

  @Test
  @DisplayName("the statistics engine stays free of Spring for the same reason")
  void statisticsEngineHasNoFrameworkDependency() {
    noClasses()
        .that()
        .resideInAPackage("com.rex.statistics..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
        .allowEmptyShould(true)
        .check(classes);
  }

  @Test
  @DisplayName("repositories are interfaces, so persistence stays declarative")
  void repositoriesAreInterfaces() {
    classes()
        .that()
        .resideInAPackage("com.rex.repository..")
        .should()
        .beInterfaces()
        .allowEmptyShould(true)
        .check(classes);
  }

  @Test
  @DisplayName("nothing calls System.out or System.err; logging goes through slf4j")
  void noConsolePrinting() {
    com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
        .allowEmptyShould(true)
        .check(classes);
  }
}
