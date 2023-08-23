{
  description = "A nix flake";

  inputs = {
    # disabled until we use it, to minimize download size.
    # don't hesitate to enable this again if you want a package from stable!
    # nixpkgs-stable.url = "github:nixos/nixpkgs/nixos-23.05";

    nixpkgs-unstable.url = "github:nixos/nixpkgs/nixpkgs-unstable";

    # to enable interoperability with `shell.nix`
    flake-compat = {
      url = "github:edolstra/flake-compat";
      flake = false;
    };

    # for `flake-utils.lib.eachSystem`
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs-unstable, flake-utils, ... }@inputs:
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          pkgs = import nixpkgs-unstable {
            inherit system;
            config.allowUnfree = false;
          };
          # enable again when we use stable packages, or add as an overlay to `pkgs`
          # pkgs-stable = import inputs.nixpkgs-stable {
          #   inherit system;
          #   config.allowUnfree = true;
          # };
          basePkgs = with pkgs; [ git zip jq earthly ];
          nodePkgs = with pkgs; [
            nodejs_18
            # explicitly run `yarn` with our node-version, see https://stackoverflow.com/a/74409706
            (yarn.override { nodejs = nodejs_18; })
          ];
          # sbt with headless jdk to reduce closure-/download-size
          ourSbt = pkgs.sbt.override { jre = pkgs.jdk19_headless; };
          scalaPkgs = [ ourSbt ] ++ nodePkgs;
          rustPkgs = with pkgs; [ rustup cargo-watch ];
          scalaOnlyShell = pkgs.mkShellNoCC
            {
              nativeBuildInputs = basePkgs ++ scalaPkgs;
              installPhase = "";
              shellHook = ''
                export JAVA_HOME="$('grep' -e '^-java-home' ${ourSbt}/share/sbt/conf/sbtopts | cut -d ' ' -f 2)"
                echo "sbt jdk path = $JAVA_HOME"
              '';
            };
        in
        {
          devShells = rec {
            default = scalaOnlyShell;

            scalaOnly = scalaOnlyShell;
          };
        }
      );
}
