package se.vandmo.dependencylock.maven;

import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.apache.maven.model.Dependency;

public final class Artifacts implements Iterable<Artifact> {

  public final List<Artifact> artifacts;

  private Artifacts(Collection<Artifact> artifacts) {
    ArrayList<Artifact> copy = new ArrayList<>(artifacts);
    sort(copy);
    this.artifacts = unmodifiableList(copy);
  }

  public static Artifacts fromMavenArtifacts(
      Collection<org.apache.maven.artifact.Artifact> artifacts, boolean enableIntegrityChecking) {
    return new Artifacts(
        artifacts.stream().map(a -> Artifact.from(a, enableIntegrityChecking)).collect(toList()));
  }

  public static Artifacts fromArtifacts(
      Collection<Artifact> artifacts, boolean enableIntegrityChecking) {
    return new Artifacts(artifacts);
  }

  public static Artifacts fromDependencies(Collection<Dependency> dependencies) {
    return new Artifacts(dependencies.stream().map(Artifact::from).collect(toList()));
  }

  public Optional<Artifact> by(ArtifactIdentifier identifier) {
    for (Artifact artifact : artifacts) {
      if (identifier.equals(artifact.identifier)) {
        return Optional.of(artifact);
      }
    }
    return Optional.empty();
  }

  @Override
  public Iterator<Artifact> iterator() {
    return artifacts.iterator();
  }
}
