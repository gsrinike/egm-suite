export interface GuiAppConfig {
  environment?: string;
  apis?: {
    csaBaseUrl?: string;
    cnmBaseUrl?: string;
  };
}

declare global {
  interface Window {
    EGM_CONFIG?: GuiAppConfig;
  }
}

const DEFAULT_CONFIG: GuiAppConfig = {
  environment: 'local',
  apis: {
    csaBaseUrl: import.meta.env.VITE_CSA_API_BASE_URL ?? '',
    cnmBaseUrl: import.meta.env.VITE_CNM_API_BASE_URL ?? ''
  }
};

export async function loadAppConfig(moduleName = 'gui.rcc.manager'): Promise<GuiAppConfig> {
  const environment = import.meta.env.VITE_APP_ENV ?? 'local';
  const baseConfig = await fetchConfig(`/config/base/${moduleName}-application.json`);
  const environmentConfig = await fetchConfig(`/config/${environment}/${moduleName}-application.json`);
  const merged = mergeConfig(DEFAULT_CONFIG, baseConfig, environmentConfig);
  window.EGM_CONFIG = mergeConfig(window.EGM_CONFIG ?? {}, merged);
  return window.EGM_CONFIG;
}

async function fetchConfig(url: string): Promise<GuiAppConfig> {
  try {
    const response = await fetch(url, { cache: 'no-store' });
    return response.ok ? response.json() : {};
  } catch {
    return {};
  }
}

function mergeConfig(...configs: GuiAppConfig[]): GuiAppConfig {
  return configs.reduce<GuiAppConfig>((merged, config) => ({
    ...merged,
    ...config,
    apis: {
      ...merged.apis,
      ...config.apis
    }
  }), {});
}
