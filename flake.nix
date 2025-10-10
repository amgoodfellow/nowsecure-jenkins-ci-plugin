{
  description = "Jenkins plugin flake";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    { self, nixpkgs, ... }@inputs:

    inputs.flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      {

        # Development environments
        devShells = pkgs.mkShell {
          default = pkgs.mkShell {

            packages = [
              pkgs.nixpkgs-fmt
              pkgs.nil

              pkgs.jdk17
              pkgs.maven
              pkgs.jdt-language-server

              pkgs.act
            ];

            # Environment variables
            env = {
              JAVA_HOME = "${pkgs.jdk17}";
            };
          };
        };
      }
    );
}
