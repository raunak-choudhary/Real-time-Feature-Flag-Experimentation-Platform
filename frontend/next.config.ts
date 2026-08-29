import type { NextConfig } from "next";

const config: NextConfig = {
  reactStrictMode: true,
  // The SDK is a workspace package shipped as TypeScript source rather than a built bundle,
  // so Next has to compile it alongside the app.
  transpilePackages: ["@rex/sdk"],
};

export default config;
