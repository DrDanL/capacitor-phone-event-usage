declare module '@capacitor/core' {
  interface PluginRegistry {
    PhoneEventUsage: PhoneEventUsagePlugin;
  }
}

export interface PhoneEventUsagePlugin {
  enable(): Promise<void>;
  echo(options: { value: string }): Promise<{ value: string }>;
  getPermissionStatus(): Promise<{}>;
  getAppUsage(duration: number): Promise<{}>;
}
