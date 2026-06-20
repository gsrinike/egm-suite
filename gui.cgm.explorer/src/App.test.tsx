import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, vi } from 'vitest';
import App from './App';

describe('App', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => []
    }));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders the CGMES explorer shell', async () => {
    render(<App />);

    expect(screen.getByText('CGMES grid explorer')).toBeInTheDocument();
    expect(screen.getByText('Choose CGMES files')).toBeInTheDocument();
    await waitFor(() => expect(fetch).toHaveBeenCalledWith('/api/cgm/imports'));
  });
});
