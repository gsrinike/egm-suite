export interface GuiLfsaConfig {
  environment?: string;
  apis?: {
    lfsaBaseUrl?: string;
  };
}

declare global {
  interface Window {
    EGM_CONFIG?: GuiLfsaConfig;
  }
}

const DEFAULT_CONFIG: GuiLfsaConfig = {
  environment: 'local',
  apis: {
    lfsaBaseUrl: import.meta.env.VITE_LFSA_API_BASE_URL ?? ''
  }
};

export async function loadAppConfig(moduleName = 'gui.lfsa.manager'): Promise<GuiLfsaConfig> {
  const environment = import.meta.env.VITE_APP_ENV ?? 'local';
  const baseConfig = await fetchConfig(`/config/base/${moduleName}-application.json`);
  const environmentConfig = await fetchConfig(`/config/${environment}/${moduleName}-application.json`);
  const merged = mergeConfig(DEFAULT_CONFIG, baseConfig, environmentConfig);
  window.EGM_CONFIG = mergeConfig(window.EGM_CONFIG ?? {}, merged);
  return window.EGM_CONFIG;
}

async function fetchConfig(url: string): Promise<GuiLfsaConfig> {
  try {
    const response = await fetch(url, { cache: 'no-store' });
    return response.ok ? response.json() : {};
  } catch {
    return {};
  }
}

function mergeConfig(...configs: GuiLfsaConfig[]): GuiLfsaConfig {
  return configs.reduce<GuiLfsaConfig>((merged, config) => ({
    ...merged,
    ...config,
    apis: {
      ...merged.apis,
      ...config.apis
    }
  }), {});
}
