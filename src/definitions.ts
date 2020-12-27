declare module '@capacitor/core' {
  interface PluginRegistry {
    PhoneEventUsage: PhoneEventUsagePlugin;
  }
}

export interface PhoneEventUsagePlugin {
  enable(): Promise<{}>;
  getPermissionStatus(): Promise<{}>;
  getAppUsage(duration: number): Promise<{}>;
}
