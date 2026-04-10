{
  description = "Jenkins plugin flake";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    flake-parts.url = "github:hercules-ci/flake-parts";
    devshell.url = "github:numtide/devshell";
  };

  outputs =
    inputs@{ self, ... }:
    inputs.flake-parts.lib.mkFlake { inherit inputs; } {
      imports = [
        inputs.devshell.flakeModule
      ];
      systems = [
        "x86_64-linux"
        "aarch64-darwin"
        "aarch64-linux"
      ];
      perSystem =
        { pkgs, ... }:
        {
          devshells.default = {
            env = [
              {
                name = "JAVA_HOME";
                value = "${pkgs.jdk21}";
              }
            ];
            commands = [
              {
                name = "run";
                help = "start a local Jenkins instance";
                command = "mvn hpi:run";
              }
              {
                name = "format";
                help = "apply spotless formatting";
                command = "mvn spotless:apply";
              }
            ];
            packages = [
              pkgs.jdk21
              pkgs.maven
              pkgs.jdt-language-server
              pkgs.nixpkgs-fmt
              pkgs.nil
            ];
          };
        };
    };
}
