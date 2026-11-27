{
  pkgs ? import <nixpkgs> { },
}:

with pkgs;

let
  sbt = pkgs.sbt.override { jre = openjdk17; };
in
mkShell (
  {
    buildInputs = [
      sbt
      openjdk17
      nodejs
      yarn
      clang
      graalvmPackages.graalvm-ce

      # keep this line if you use bash
      bashInteractive
    ];
  }
  // lib.optionalAttrs stdenv.isLinux {
    # Maven's native protoc plugins need the C++ runtime on NixOS.
    LD_LIBRARY_PATH = lib.makeLibraryPath [ stdenv.cc.cc.lib ];
  }
)
