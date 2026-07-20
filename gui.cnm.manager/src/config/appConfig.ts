export interface GuiAppConfig {
  environment?: string;
  apis?: {
    cnmBaseUrl?: string;
    csaBaseUrl?: string;
    iidmBaseUrl?: string;
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
    cnmBaseUrl: import.meta.env.VITE_CNM_API_BASE_URL ?? '',
    iidmBaseUrl: import.meta.env.VITE_IIDM_API_BASE_URL ?? ''
  }
};

export async function loadAppConfig(moduleName = 'gui.cnm.manager'): Promise<GuiAppConfig> {
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
