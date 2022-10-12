package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.JsonUtils.getBooleanOrDefault;
import static se.vandmo.dependencylock.maven.JsonUtils.getStringValue;
import static se.vandmo.dependencylock.maven.JsonUtils.possiblyGetStringValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public final class LockedDependency implements Comparable<LockedDependency>, Predicate<Artifact> {

  public final ArtifactIdentifier identifier;
  public final String version;
  public final String scope;
  public final boolean optional;
  public final Optional<String> checksum;

  private LockedDependency(
      ArtifactIdentifier identifier,
      String version,
      String scope,
      boolean optional,
      Optional<String> checksum) {
    this.identifier = requireNonNull(identifier);
    this.version = requireNonNull(version);
    this.scope = requireNonNull(scope);
    this.optional = optional;
    this.checksum = requireNonNull(checksum);
    this.checksum.ifPresent(
        value ->
            Checksum.checkAlgorithmHeader(
                value,
                "Encountered unsupported checksum format, consider using a later version of this plugin"));
  }

  public static LockedDependency fromJson(JsonNode json, boolean enableIntegrityChecking) {
    return new LockedDependency(
        ArtifactIdentifier
            .builder()
            .groupId(getStringValue(json, "groupId"))
            .artifactId(getStringValue(json, "artifactId"))
            .classifier(possiblyGetStringValue(json, "classifier"))
            .type(possiblyGetStringValue(json, "type"))
            .build(),
        getStringValue(json, "version"),
        getStringValue(json, "scope"),
        getBooleanOrDefault(json, "optional", false),
        enableIntegrityChecking ? possiblyGetStringValue(json, "checksum") : Optional.empty());
  }

  public static LockedDependency from(Artifact artifact, boolean integrityCheck) {
    return new LockedDependency(
        artifact.identifier,
        artifact.version,
        artifact.scope,
        artifact.optional,
        integrityCheck ? artifact.checksum : Optional.empty());
  }

  public JsonNode asJson() {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.put("groupId", identifier.groupId);
    json.put("artifactId", identifier.artifactId);
    json.put("version", version);
    json.put("scope", scope);
    json.put("type", identifier.type);
    json.put("optional", optional);
    checksum.ifPresent(sum -> json.put("checksum", sum));
    identifier.classifier.ifPresent(actualClassifier -> json.put("classifier", actualClassifier));
    return json;
  }

  public Artifact toArtifact() {
    return Artifact.builder()
        .artifactIdentifier(identifier)
        .version(version)
        .scope(scope)
        .optional(optional)
        .integrity(checksum)
        .build();
  }

  @Override
  public boolean test(Artifact artifact) {
    return identifier.equals(artifact.identifier)
        && version.matches(artifact.version)
        && scope.equals(artifact.scope)
        && optional == artifact.optional
        && checksum.equals(artifact.checksum);
  }

  public boolean differsOnlyByChecksum(Artifact artifact) {
    return identifier.equals(artifact.identifier)
        && version.matches(artifact.version)
        && scope.equals(artifact.scope)
        && optional == artifact.optional;
  }

  @Override
  public int compareTo(LockedDependency other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    return toStringWithVersion(version);
  }
  private String toStringWithVersion(String version) {
    return new StringBuilder()
        .append(identifier)
        .append(':')
        .append(version)
        .append(':')
        .append(scope)
        .append(":optional=")
        .append(optional)
        .append('@')
        .append(checksum.orElse("NO_CHECKSUM"))
        .toString();
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.identifier);
    hash = 17 * hash + Objects.hashCode(this.version);
    hash = 17 * hash + Objects.hashCode(this.scope);
    hash = 17 * hash + Objects.hashCode(this.optional);
    hash = 17 * hash + Objects.hashCode(this.checksum);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final LockedDependency other = (LockedDependency) obj;
    if (!Objects.equals(this.identifier, other.identifier)) {
      return false;
    }
    if (!Objects.equals(this.version, other.version)) {
      return false;
    }
    if (!Objects.equals(this.scope, other.scope)) {
      return false;
    }
    if (!Objects.equals(this.optional, other.optional)) {
      return false;
    }
    if (!Objects.equals(this.checksum, other.checksum)) {
      return false;
    }
    return true;
  }

  public WithMyVersion withMyVersion(String myVersion) {
    return new WithMyVersion(myVersion);
  }

  public final class WithMyVersion implements Predicate<Artifact> {

    private final String myVersion;

    private WithMyVersion(String myVersion) {
      this.myVersion = myVersion;
    }

    public String toString() {
      return toStringWithVersion(myVersion);
    }

    @Override
    public boolean test(Artifact artifact) {
      return identifier.equals(artifact.identifier)
          && version.matches(myVersion)
          && scope.equals(artifact.scope)
          && optional == artifact.optional
          && checksum.equals(artifact.checksum);
    }
  }

}
