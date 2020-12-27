import { WebPlugin } from '@capacitor/core';
import { PhoneEventUsagePlugin } from './definitions';

export class PhoneEventUsageWeb extends WebPlugin implements PhoneEventUsagePlugin {
  constructor() {
    super({
      name: 'PhoneEventUsage',
      platforms: ['web'],
    });
  }

  async enable(): Promise<{}> {
    //No web enabled
    return {'enable': false}
  }

  async getPermissionStatus(): Promise<{}> {
    //No web enabled
    return {'getPermissionStatus': false}
  }

  async getAppUsage(duration: number): Promise<{}> {
    //No web enabled
    return {'getAppUsage': duration}
  }
}

// Instantiate the plugin
const PhoneEventUsage = new PhoneEventUsageWeb();

// Export the plugin
export { PhoneEventUsage };

// Register as a web plugin
import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(PhoneEventUsage);
