VERSION 0.7
FROM nixpkgs/nix-flakes:nixos-23.05
WORKDIR /app

nix-shell-scala:
  COPY flake.nix flake.lock /flake
  # warm up nix cache by running "echo"
  RUN nix develop "/flake#scalaOnly" --command echo


scala-deps:
  FROM +nix-shell-scala
  CACHE /root/.sbt
  CACHE /root/.ivy2
  CACHE /root/.cache/sbt
  CACHE /root/.cache/coursier/v1
  CACHE /root/.cache/scalablytyped
  CACHE /usr/local/share/.cache/yarn/v6

  # Copy project structure without any code.
  # For initializing scala compiler, depedencies, scalablytyped.
  COPY --dir build.sbt project .
  # copy only parts from subprojects, such that build.sbt is valid
  # COPY foo/package.json foo/yarn.lock foo/
  RUN nix develop "/flake#scalaOnly" --command bash -c 'sbt "update; npmInstallDependencies"' 


scala-compile:
  FROM +scala-deps
  CACHE /tmp/sbt-remote-cache
  COPY --dir foo .
  RUN nix develop "/flake#scalaOnly" --command bash -c 'sbt "pullRemoteCache; compile; Test/compile; pushRemoteCache"' 
