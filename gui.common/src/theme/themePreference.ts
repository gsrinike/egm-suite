const THEME_STORAGE_KEY = 'egm.light-theme';

export function applyThemePreference(lightTheme = readThemePreference()): boolean {
  document.body.classList.toggle('light-theme', lightTheme);
  return lightTheme;
}

export function toggleThemePreference(currentLightTheme: boolean): boolean {
  const nextLightTheme = !currentLightTheme;
  writeThemePreference(nextLightTheme);
  applyThemePreference(nextLightTheme);
  return nextLightTheme;
}

function readThemePreference(): boolean {
  try {
    return window.localStorage.getItem(THEME_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

function writeThemePreference(lightTheme: boolean) {
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, String(lightTheme));
  } catch {
    // Ignore storage failures; the active page still receives the theme class.
  }
}
